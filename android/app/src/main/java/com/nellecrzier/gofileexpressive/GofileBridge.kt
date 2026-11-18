package com.nellecrzier.gofileexpressive

import android.webkit.JavascriptInterface

/**
 * Jembatan yang dipanggil WebView sebagai `window.AndroidBridge`.
 * Semua method harus berjalan di main thread saat menyentuh Activity/UI,
 * karena @JavascriptInterface dipanggil dari thread WebView.
 */
class GofileBridge(private val activity: MainActivity) {

    @JavascriptInterface
    fun isAvailable(): Boolean = true

    @JavascriptInterface
    fun hasNotificationPermission(): Boolean = activity.hasNotificationPermission()

    @JavascriptInterface
    fun requestNotificationPermission() {
        activity.runOnUiThread { activity.requestNotificationPermission() }
    }

    @JavascriptInterface
    fun isBackgroundAllowed(): Boolean = activity.isIgnoringBatteryOptimizations()

    @JavascriptInterface
    fun requestBackgroundPermission() {
        activity.runOnUiThread { activity.requestIgnoreBatteryOptimizations() }
    }

    @JavascriptInterface
    fun pickAndUpload(folderId: String, token: String) {
        activity.runOnUiThread { activity.pickFileForUpload(folderId, token) }
    }

    @JavascriptInterface
    fun download(url: String, fileName: String) {
        activity.runOnUiThread { activity.startDownload(url, fileName) }
    }

    /** Buka file/share link di browser atau aplikasi eksternal (window.open tidak jalan di WebView). */
    @JavascriptInterface
    fun openExternal(url: String) {
        activity.runOnUiThread { activity.openExternal(url) }
    }

    /** Kembalikan JSON array transfer terakhir supaya UI bisa dipulihkan. */
    @JavascriptInterface
    fun getTransfers(): String = TransferStore.toJson()

    @JavascriptInterface
    fun toast(message: String) {
        activity.runOnUiThread { activity.toast(message) }
    }
}
