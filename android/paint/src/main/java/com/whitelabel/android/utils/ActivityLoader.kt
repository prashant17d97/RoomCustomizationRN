package com.whitelabel.android.utils

import android.R
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import com.whitelabel.android.data.model.ColorProperty
import com.whitelabel.android.ui.paint.fragment.PaintFragment
import com.whitelabel.android.utils.Utils.toJson


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
    internal const val COLOR_JSON = "color"

    internal const val COLOR_PROPERTIES_LIST = "color_option_list"


    /**
     * Shows the [PaintFragment] in the specified [FragmentActivity].
     *
     * This function handles the transaction to display the [PaintFragment]
     * with the provided image and color configurations.
     *
     * @param colorProperty The initial [ColorProperty] to be selected in the PaintFragment.
     * @param imageUri The URI of the image to be loaded and displayed in the PaintFragment.
     * @param colorOptionsListGson A list of [ColorProperty] objects representing the color palette
     *                              available to the user. Defaults to an empty list, meaning no
     *                              predefined color options will be shown beyond the initial color.
     * @param fragmentContainerId The ID of the ViewGroup container where the fragment will be placed.
     *                            If 0 (or not provided), it defaults to `android.R.id.content`.
     *                            Ensure this container exists in the activity's layout.
     * @return The instance of the shown [PaintFragment], allowing for further interaction if needed.
     *
     *
     *
     * Example:
     * ``` kotlin
     * showPaintFragment(
     *             colorProperty = ColorProperty(
     *                 colorName = "DARK GRAY",
     *                 colorCode = Color.DKGRAY.toHexString()
     *             ),
     *             imageUri = imageUri,
     *             fragmentContainerId = fragmentContainerId,
     *             colorOptionsListGson = listOf(
     *                 ColorProperty(colorName = "DARK GRAY", colorCode = Color.DKGRAY.toHexString()),
     *                 ColorProperty(colorName = "GRAY", colorCode = Color.GRAY.toHexString()),
     *                 ColorProperty(colorName = "YELLOW", colorCode = Color.YELLOW.toHexString()),
     *                 ColorProperty(colorName = "Green", colorCode = Color.GREEN.toHexString()),
     *                 ColorProperty(colorName = "MAGENTA", colorCode = Color.MAGENTA.toHexString()),
     *                 ColorProperty(colorName = "CYAN", colorCode = Color.CYAN.toHexString()),
     *                 ColorProperty(colorName = "LTGRAY", colorCode = Color.LTGRAY.toHexString()),
     *                 ColorProperty(colorName = "RED", colorCode = Color.RED.toHexString()),
     *             )
     *         )
     * ```
     */
    fun FragmentActivity.showPaintFragment(
        colorProperty: ColorProperty,
        imageUri: Uri,
        colorOptionsListGson: List<ColorProperty> = emptyList(),
        fragmentContainerId: Int = 0
    ) {
        val fragment = PaintFragment().apply {
            arguments = Bundle().apply {
                putString(IMAGE_URI, imageUri.toString())
                putString(COLOR_JSON, colorProperty.toJson())
                if (colorOptionsListGson.isNotEmpty()) {
                    putString(COLOR_PROPERTIES_LIST, colorOptionsListGson.toJson())
                }
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun AppCompatActivity.loadPaintFragment(): Int {
        var fragmentContainerId = 0
        val rootView = findViewById<ViewGroup>(R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val fragmentContainer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            fragmentContainerId = View.generateViewId()
            id = fragmentContainerId
        }
        rootView.addView(fragmentContainer)
        return fragmentContainerId
    }

}