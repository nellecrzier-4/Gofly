package com.nellecrzier.gofileexpressive

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class GSession(
    val token: String = "",
    val accountId: String = "",
    val rootFolder: String = "",
    val email: String = "",
    val tier: String = "",
) {
    val isLoggedIn: Boolean get() = token.isNotBlank()
}

/**
 * Sesi disimpan lokal dengan [EncryptedSharedPreferences] (kunci di Keystore).
 *
 * Pengguna lama masih punya sesi di SharedPreferences biasa; [load] memigrasikan
 * token itu ke penyimpanan terenkripsi lalu menghapus versi lama, sehingga
 * tidak ada yang ter-logout setelah pembaruan.
 */
object SessionStore {
    private const val LEGACY_NAME = "gofile-session"
    private const val SECURE_NAME = "gofile-session-secure"

    private const val TOKEN = "token"
    private const val ACCOUNT_ID = "accountId"
    private const val ROOT_FOLDER = "rootFolder"
    private const val EMAIL = "email"
    private const val TIER = "tier"

    fun load(context: Context): GSession {
        val legacy = legacy(context)
        val secure = secure(context)

        if (secure != null) {
            val stored = read(secure)
            if (stored.isLoggedIn) return stored

            val old = read(legacy)
            if (old.isLoggedIn) {
                write(secure, old)
                legacy.edit().clear().apply()
                return old
            }
            return stored
        }

        // Keystore tidak tersedia: tetap bisa dipakai, walau tanpa enkripsi.
        return read(legacy)
    }

    fun save(context: Context, session: GSession) {
        val secure = secure(context)
        if (secure != null) {
            write(secure, session)
            legacy(context).edit().clear().apply()
        } else {
            write(legacy(context), session)
        }
    }

    fun clear(context: Context) {
        secure(context)?.edit()?.clear()?.apply()
        legacy(context).edit().clear().apply()
    }

    private fun secure(context: Context): SharedPreferences? {
        createSecure(context)?.let { return it }
        // Kunci bisa rusak setelah restore/backup; buang lalu coba sekali lagi.
        return runCatching {
            context.deleteSharedPreferences(SECURE_NAME)
            createSecure(context)
        }.getOrNull()
    }

    private fun createSecure(context: Context): SharedPreferences? = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            SECURE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrNull()

    private fun legacy(context: Context): SharedPreferences =
        context.getSharedPreferences(LEGACY_NAME, Context.MODE_PRIVATE)

    private fun read(prefs: SharedPreferences): GSession = GSession(
        token = prefs.getString(TOKEN, "").orEmpty(),
        accountId = prefs.getString(ACCOUNT_ID, "").orEmpty(),
        rootFolder = prefs.getString(ROOT_FOLDER, "").orEmpty(),
        email = prefs.getString(EMAIL, "").orEmpty(),
        tier = prefs.getString(TIER, "").orEmpty(),
    )

    private fun write(prefs: SharedPreferences, session: GSession) {
        prefs.edit().apply {
            putString(TOKEN, session.token)
            putString(ACCOUNT_ID, session.accountId)
            putString(ROOT_FOLDER, session.rootFolder)
            putString(EMAIL, session.email)
            putString(TIER, session.tier)
        }.apply()
    }
}
