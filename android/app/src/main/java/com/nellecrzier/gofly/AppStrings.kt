package com.nellecrzier.gofly

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Akses string untuk kode yang tidak punya Context (ViewModel, lapisan
 * jaringan, pengurai). Selalu memakai bahasa pilihan pengguna lewat
 * [LocaleSupport.wrap], jadi pesan galat dan notifikasi singkat ikut berganti
 * bahasa bersama UI. Kode Compose tetap memakai `stringResource`.
 */
object AppStrings {
    @Volatile
    private var appContext: Context? = null

    /** Pengganti untuk unit test, yang tidak punya Context Android. */
    @Volatile
    var resolver: ((Int, Array<out Any>) -> String)? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun get(@StringRes id: Int, vararg args: Any): String {
        resolver?.let { return it(id, args) }
        val base = appContext ?: return ""
        return LocaleSupport.wrap(base).getString(id, *args)
    }

    fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String {
        val base = appContext ?: return ""
        return LocaleSupport.wrap(base).resources.getQuantityString(id, count, *args)
    }
}
