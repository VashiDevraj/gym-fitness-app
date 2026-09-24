package com.example.gymapplication

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target

/**
 * Centralised GIF loader with multi-tier fallback:
 *   1. Try the primary gifUrl
 *   2. On failure, try the backup URL (media2 → media, or strip query params)
 *   3. On all failure, show the emoji placeholder drawn on a coloured background
 */
object GifLoader {

    // Category → solid background colour for emoji placeholder
    private val categoryColors = mapOf(
        "Cardio"    to "#FF6B35",
        "Chest"     to "#4A90D9",
        "Back"      to "#7B68EE",
        "Legs"      to "#2ECC71",
        "Abs"       to "#F39C12",
        "Arms"      to "#E74C3C",
        "Shoulders" to "#9B59B6",
        "Full Body" to "#1ABC9C"
    )

    fun load(
        context: Context,
        imageView: ImageView,
        gifUrl: String,
        emoji: String,
        category: String,
        onLoadStart: () -> Unit = {},
        onLoadEnd: () -> Unit = {}
    ) {
        onLoadStart()

        val options = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .placeholder(R.drawable.gif_placeholder)
            .error(R.drawable.gif_placeholder)
            .centerCrop()

        Glide.with(context)
            .load(gifUrl)
            .apply(options)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    // Try backup URL — swap media2 to media or vice-versa
                    val backupUrl = buildBackupUrl(gifUrl)
                    if (backupUrl != null) {
                        imageView.post {
                            Glide.with(imageView.context)
                                .load(backupUrl)
                                .apply(options)
                                .listener(object : RequestListener<Drawable> {

                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<Drawable>,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        showEmojiPlaceholder(context, imageView, emoji, category)
                                        onLoadEnd()
                                        return true
                                    }

                                    override fun onResourceReady(
                                        resource: Drawable,
                                        model: Any,
                                        target: Target<Drawable>,
                                        dataSource: DataSource,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        onLoadEnd()
                                        return false
                                    }
                                })
                                .into(imageView)
                        }
                    } else {
                        showEmojiPlaceholder(context, imageView, emoji, category)
                        onLoadEnd()
                    }

                    return true
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    onLoadEnd()
                    return false
                }
            })
            .into(imageView)
    }

    private fun buildBackupUrl(original: String): String? {
        return when {
            original.contains("media2.giphy.com") ->
                original.replace("media2.giphy.com", "i.giphy.com")
            original.contains("i.giphy.com") ->
                original.replace("i.giphy.com", "media.giphy.com")
            original.contains("media.giphy.com") && !original.contains("media2") ->
                original.replace("media.giphy.com", "media2.giphy.com")
            else -> null
        }
    }

    private fun showEmojiPlaceholder(
        context: Context,
        imageView: ImageView,
        emoji: String,
        category: String
    ) {
        val bgColor = categoryColors[category] ?: "#3949AB"
        val drawable = EmojiPlaceholderDrawable(context, emoji, bgColor)
        imageView.setImageDrawable(drawable)
        imageView.scaleType = ImageView.ScaleType.FIT_XY
    }
}