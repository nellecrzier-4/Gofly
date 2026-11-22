package com.nellecrzier.gofileexpressive.ui.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.ui.Hyper
import com.nellecrzier.gofileexpressive.ui.HyperButton
import com.nellecrzier.gofileexpressive.ui.rememberReduceMotion

/**
 * Kunci aplikasi biometrik/PIN.
 *
 * [lockRevision] naik setiap kali aplikasi kembali dari latar belakang; saat
 * naik dan [enabled] benar, isi aplikasi ditutup sampai pengguna terverifikasi.
 * Kalau perangkat tidak punya biometrik, kredensial perangkat (PIN/pola/sandi)
 * tetap dipakai lewat opsi DEVICE_CREDENTIAL.
 */
@Composable
fun AppLockGate(
    enabled: Boolean,
    lockRevision: Int,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var unlocked by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var prompting by remember { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotion()

    fun prompt() {
        if (activity == null || prompting) return
        prompting = true
        error = null
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                prompting = false
                unlocked = true
                error = null
            }

            override fun onAuthenticationError(code: Int, message: CharSequence) {
                prompting = false
                error = message.toString()
            }

            override fun onAuthenticationFailed() {
                // Biometrik tidak cocok: dialog tetap terbuka, beri kesempatan lagi.
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.lock_title))
            .setSubtitle(context.getString(R.string.lock_subtitle))
            .setAllowedAuthenticators(allowedAuthenticators(context))
            .setConfirmationRequired(false)
            .build()
        try {
            prompt.authenticate(info)
        } catch (exception: Exception) {
            prompting = false
            error = context.getString(R.string.lock_unavailable)
        }
    }

    LaunchedEffect(lockRevision) {
        if (lockRevision > 0) {
            unlocked = false
            prompt()
        }
    }

    Box(Modifier.fillMaxSize().background(Hyper.Bg)) {
        if (unlocked) {
            content()
        } else {
            Column(
                Modifier.fillMaxSize().padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Hyper.Accent,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.lock_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Hyper.Text,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = error ?: stringResource(R.string.lock_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (error != null) Hyper.Accent else Hyper.Text2,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
                Box(Modifier.widthIn(max = 320.dp)) {
                    HyperButton(
                        text = stringResource(R.string.lock_unlock),
                        onClick = { prompt() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = unlocked && !reduceMotion,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(120)),
        ) { Box(Modifier) }
    }
}

private fun allowedAuthenticators(context: Context): Int {
    val manager = BiometricManager.from(context)
    val biometricOnly = BiometricManager.Authenticators.BIOMETRIC_STRONG
    val credential = BiometricManager.Authenticators.DEVICE_CREDENTIAL
    return when (manager.canAuthenticate(biometricOnly or credential)) {
        BiometricManager.BIOMETRIC_SUCCESS -> biometricOnly or credential
        else -> when (manager.canAuthenticate(biometricOnly)) {
            BiometricManager.BIOMETRIC_SUCCESS -> biometricOnly
            else -> credential
        }
    }
}

/** Apakah perangkat punya jalur autentikasi apa pun untuk kunci aplikasi. */
fun canUseAppLock(context: Context): Boolean {
    val manager = BiometricManager.from(context)
    return manager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL,
    ) == BiometricManager.BIOMETRIC_SUCCESS
}
