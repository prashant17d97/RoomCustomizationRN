package com.whitelabel.android.interfaces

import android.graphics.Bitmap
import android.net.Uri

/**
 * Interface for providing image loading functionality to the paint module.
 * This interface allows loading images from different sources, including URIs and Bitmaps,
 * to be used within the paint module.
 *
 * The implementation of this interface should define how images are fetched or generated.
 * Examples include fetching an image from a gallery, camera, or a remote source.
 * It is meant to be registered and accessed via the `PaintInterfaceRegistry`.
 */
interface ImageProvider {

    /**
     * Loads an image from the provided URI.
     * This method is intended to handle the image loading process,
     * including fetching and preparing the image for use within
     * the paint module.
     *
     * @param imageUri The URI of the image to be loaded.
     */
    fun loadImageUri(imageUri: Uri)

    /**
     * Loads an image represented as a Bitmap into the paint module.
     * This method allows the paint module to process or display images
     * provided as a Bitmap.
     *
     * @param imageBitmap The Bitmap source of the image to be loaded.
     */
    fun loadImageBitmap(imageBitmap: Bitmap)
}