package com.nellecrzier.gofileexpressive

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

/**
 * ImageLoader aplikasi: thumbnail gambar dan bingkai video untuk tampilan
 * grid. Tidak ada konten web yang dimuat — hanya URL file GoFile.
 */
class GofileApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(VideoFrameDecoder.Factory()) }
        .crossfade(true)
        .respectCacheHeaders(false)
        .build()
}
