package com.nellecrzier.gofly

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder

/**
 * ImageLoader aplikasi: thumbnail gambar, GIF, dan bingkai video untuk tampilan
 * grid. Tidak ada konten web yang dimuat — hanya URL file GoFile.
 *
 * Di sini juga dipasang pencatat crash lokal (tanpa layanan pihak ketiga).
 */
class GofileApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        AppLog.install(this)
        LocaleSupport.applySystemLocales(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components {
            add(VideoFrameDecoder.Factory())
            add(GifDecoder.Factory())
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                add(ImageDecoderDecoder.Factory())
            }
        }
        .crossfade(true)
        .respectCacheHeaders(false)
        .build()
}
