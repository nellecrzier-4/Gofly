package com.nellecrzier.gofileexpressive

import android.content.Context

data class GSession(
    val token: String = "",
    val accountId: String = "",
    val rootFolder: String = "",
    val email: String = "",
    val tier: String = "",
) {
    val isLoggedIn: Boolean get() = token.isNotBlank()
}

/** Sesi disimpan lokal supaya aplikasi native tidak perlu login ulang. */
object SessionStore {
    private const val NAME = "gofile-session"
    private const val TOKEN = "token"
    private const val ACCOUNT_ID = "accountId"
    private const val ROOT_FOLDER = "rootFolder"
    private const val EMAIL = "email"
    private const val TIER = "tier"

    fun load(context: Context): GSession {
        val p = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        return GSession(
            token = p.getString(TOKEN, "").orEmpty(),
            accountId = p.getString(ACCOUNT_ID, "").orEmpty(),
            rootFolder = p.getString(ROOT_FOLDER, "").orEmpty(),
            email = p.getString(EMAIL, "").orEmpty(),
            tier = p.getString(TIER, "").orEmpty(),
        )
    }

    fun save(context: Context, session: GSession) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().apply {
            putString(TOKEN, session.token)
            putString(ACCOUNT_ID, session.accountId)
            putString(ROOT_FOLDER, session.rootFolder)
            putString(EMAIL, session.email)
            putString(TIER, session.tier)
            apply()
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
