package com.nellecrzier.gofly.vm

import com.nellecrzier.gofly.R
import com.nellecrzier.gofly.AppStrings
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nellecrzier.gofly.GSession
import com.nellecrzier.gofly.GoFileException
import com.nellecrzier.gofly.GofileApi
import com.nellecrzier.gofly.GofileRepository
import com.nellecrzier.gofly.SessionStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val session: GSession = GSession(),
    val step: String = "email",
    val email: String = "",
    val tokenInput: String = "",
    val busy: Boolean = false,
    val checking: Boolean = false,
)

sealed interface SessionEvent {
    data class Message(val text: String, val error: Boolean = true) : SessionEvent
    data object LoggedIn : SessionEvent
    data object LoggedOut : SessionEvent
}

/**
 * Satu sumber kebenaran untuk sesi dan alur masuk. Compose hanya mengamati
 * [state]; efek samping (pesan, login, logout) dikirim lewat [events].
 */
class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GofileRepository()
    private val context = application.applicationContext

    private val stored = SessionStore.load(context)
    private val _state = MutableStateFlow(
        SessionUiState(
            session = stored,
            step = if (stored.isLoggedIn) "ready" else "email",
            checking = stored.isLoggedIn,
        ),
    )
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    init {
        if (stored.isLoggedIn) refreshSession()
    }

    fun setEmail(value: String) = _state.update { it.copy(email = value) }

    fun setTokenInput(value: String) = _state.update { it.copy(tokenInput = value) }

    fun backToEmail() = _state.update { it.copy(step = "email") }

    /**
     * Verifikasi sesi tersimpan. Saat offline sesi lokal tetap dipakai supaya
     * pengguna bisa membuka data yang sudah di-cache.
     */
    fun refreshSession() {
        val token = _state.value.session.token
        if (token.isBlank()) {
            _state.update { it.copy(checking = false) }
            return
        }
        viewModelScope.launch {
            try {
                val fresh = repository.getAccount(token)
                enter(fresh.copy(token = token), notify = false)
            } catch (error: Exception) {
                if (error is GoFileException.AuthExpired) {
                    logout(expired = true)
                } else {
                    _state.update { it.copy(checking = false) }
                    _events.tryEmit(SessionEvent.LoggedIn)
                }
            }
        }
    }

    fun sendLoginLink(email: String) {
        val clean = email.trim()
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                repository.sendLoginLink(clean)
                _state.update { it.copy(busy = false, step = "token", email = clean) }
                _events.tryEmit(
                    SessionEvent.Message(
                        AppStrings.get(R.string.session_link_sent),
                        error = false,
                    ),
                )
            } catch (error: Exception) {
                fail(error)
            }
        }
    }

    fun loginWithToken(raw: String) {
        val token = GofileApi.extractToken(raw)
        if (token == null) {
            _events.tryEmit(
                SessionEvent.Message(AppStrings.get(R.string.session_link_invalid)),
            )
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val account = repository.getAccount(token)
                enter(account.copy(token = token))
            } catch (error: Exception) {
                fail(error)
            }
        }
    }

    fun loginAsGuest() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                enter(repository.createGuest())
            } catch (error: Exception) {
                fail(error)
            }
        }
    }

    /** Dipakai oleh App Link `gofile.io/login/...` yang dibuka aplikasi. */
    fun adoptLoginLink(token: String) {
        if (token.isBlank() || _state.value.session.isLoggedIn) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val account = repository.getAccount(token)
                enter(account.copy(token = token))
                _events.tryEmit(SessionEvent.Message(AppStrings.get(R.string.session_login_via_link), error = false))
            } catch (error: Exception) {
                fail(error)
            }
        }
    }

    fun logout(expired: Boolean = false) {
        SessionStore.clear(context)
        _state.value = SessionUiState(step = "email")
        _events.tryEmit(SessionEvent.LoggedOut)
        if (expired) {
            _events.tryEmit(SessionEvent.Message(AppStrings.get(R.string.err_session_expired)))
        }
    }

    private fun enter(session: GSession, notify: Boolean = true) {
        SessionStore.save(context, session)
        _state.update {
            it.copy(session = session, step = "ready", busy = false, checking = false)
        }
        _events.tryEmit(SessionEvent.LoggedIn)
        if (notify) _events.tryEmit(SessionEvent.Message(AppStrings.get(R.string.session_signed_in), error = false))
    }

    private fun fail(error: Exception) {
        _state.update { it.copy(busy = false) }
        if (error is GoFileException.AuthExpired) {
            logout(expired = true)
        } else {
            _events.tryEmit(SessionEvent.Message(error.message ?: AppStrings.get(R.string.err_generic)))
        }
    }
}
