package alie.info.newmultichoice.utils

import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat

/**
 * Adapter for optimized image loading with fallbacks
 */
object ImageAdapter {

    /**
     * Load drawable resource with optimization
     */
    fun loadOptimizedImage(
        imageView: ImageView,
        @DrawableRes resourceId: Int,
        useCoil: Boolean = true
    ) {
        if (useCoil) {
            // Use Coil for better performance
            ImageLoader.loadImage(
                context = imageView.context,
                imageView = imageView,
                resourceId = resourceId,
                scale = coil.size.Scale.FIT
            )
        } else {
            // Fallback to standard loading
            imageView.setImageResource(resourceId)
        }
    }

    /**
     * Preload critical images on app start
     */
    fun preloadCriticalImages(context: android.content.Context) {
        val criticalImages = listOf(
            alie.info.newmultichoice.R.drawable.klcp_logo,
            alie.info.newmultichoice.R.drawable.menu_icon,
            alie.info.newmultichoice.R.mipmap.ic_launcher
        )

        criticalImages.forEach { resourceId ->
            ImageLoader.preloadImage(context, resourceId)
        }
    }

    /**
     * Get optimized drawable with caching
     */
    fun getOptimizedDrawable(
        context: android.content.Context,
        @DrawableRes resourceId: Int
    ): android.graphics.drawable.Drawable? {
        return ContextCompat.getDrawable(context, resourceId)?.apply {
            // Apply optimizations if possible
            if (this is android.graphics.drawable.BitmapDrawable) {
                // Could add bitmap optimizations here
            }
        }
    }

    /**
     * Clear image caches when needed
     */
    fun clearImageCache(context: android.content.Context) {
        ImageLoader.clearCache(context)
    }
}
