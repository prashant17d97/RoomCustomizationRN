package com.whitelabel.android.utils

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import com.whitelabel.android.PaintActivity

typealias HexColorString = String

/**
 * Object responsible for managing and handling activities related to loading the PaintActivity.
 */
object ActivityLoader {
    /**
     * A constant that represents the key used to identify or store image URIs.
     *
     * This key is commonly used for passing or retrieving the location of images
     * within the application, such as through intents, bundles, or storage mechanisms.
     */
    internal const val IMAGE_URI = "image_uri"
    /**
     * Represents the key or identifier used to denote a color value in hexadecimal format.
     * This constant is often utilized when working with color-related data or configurations.
     */
    internal const val COLOR_HEX = "color"
    /**
     * Constant representing the key for identifying a fandeck.
     * Used primarily in contexts where fandeck identification or mapping is required.
     */
    internal const val FANDECK_ID = "fandeck_id"
    /**
     * Represents the constant key used for referencing or storing the fandeck name.
     * Typically utilized in contexts where the fandeck's identification or label
     * is required for operations, such as saving or retrieving from storage or preferences.
     */
    internal const val FANDECK_NAME = "fandeck_name"
    /**
     * Represents the key used to identify a color name in various contexts.
     * This constant might be used for serialization, database keys,
     * or identifying resources related to colors.
     */
    internal const val COLOR_NAME = "color_name"


    /**
     * Launches the PaintActivity with the supplied configuration parameters.
     *
     * @param color The hex string representing the color to use (e.g., "#FFFFFF").
     * @param imageUri The URI of the image to be used in the activity.
     * @param fandeckId Optional ID representing the fandeck. Defaults to -1 if not provided.
     * @param fandeckName Optional name of the fandeck. Defaults to an empty string if not provided.
     * @param colorName Optional name of the color. Defaults to "Default Color" if not provided.
     */
    fun Activity.loadPaintActivity(
        color: HexColorString,
        imageUri: Uri,
        fandeckId: Int = -1,
        fandeckName: String = "",
        colorName: String = Color.CYAN.toString()
    ) {
        val intent = Intent(this, PaintActivity::class.java)
        intent.putExtra(IMAGE_URI, imageUri.toString())
        intent.putExtra(COLOR_HEX, color)
        intent.putExtra(FANDECK_ID, fandeckId)
        intent.putExtra(FANDECK_NAME, fandeckName)
        intent.putExtra(COLOR_NAME, colorName)
        startActivity(intent)
    }

}