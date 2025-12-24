package alie.info.newmultichoice.utils

import android.content.Context
import android.widget.ImageView
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.size.Scale
import coil.transform.RoundedCornersTransformation

/**
 * Optimized image loading utility using Coil
 */
object ImageLoader {

    private var imageLoader: ImageLoader? = null

    /**
     * Get or create optimized ImageLoader instance
     */
    fun getImageLoader(context: Context): ImageLoader {
        return imageLoader ?: ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // 2% of available storage
                    .build()
            }
            // SVG support removed for simplicity
            // .components { add(SvgDecoder.Factory()) }
            .build()
            .also { imageLoader = it }
    }

    /**
     * Load image with optimizations
     */
    fun loadImage(
        context: Context,
        imageView: ImageView,
        resourceId: Int? = null,
        url: String? = null,
        placeholder: Int? = null,
        error: Int? = null,
        roundedCorners: Float = 0f,
        scale: Scale = Scale.FIT
    ) {
        val request = ImageRequest.Builder(context)
            .data(resourceId ?: url)
            .target(imageView)
            .scale(scale)
            .apply {
                placeholder?.let { placeholder(it) }
                error?.let { error(it) }
                if (roundedCorners > 0) {
                    transformations(RoundedCornersTransformation(roundedCorners))
                }
            }
            .build()

        getImageLoader(context).enqueue(request)
    }

    /**
     * Preload image into cache
     */
    fun preloadImage(context: Context, resourceId: Int) {
        val request = ImageRequest.Builder(context)
            .data(resourceId)
            .build()

        getImageLoader(context).enqueue(request)
    }

    /**
     * Clear all caches
     */
    fun clearCache(context: Context) {
        getImageLoader(context).apply {
            memoryCache?.clear()
            diskCache?.clear()
        }
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(context: Context): Map<String, Any> {
        val loader = getImageLoader(context)
        return mapOf(
            "memory_cache_size" to (loader.memoryCache?.size ?: 0),
            "memory_cache_max" to (loader.memoryCache?.maxSize ?: 0),
            "disk_cache_size" to (loader.diskCache?.size ?: 0),
            "disk_cache_max" to (loader.diskCache?.maxSize ?: 0)
        )
    }
}
