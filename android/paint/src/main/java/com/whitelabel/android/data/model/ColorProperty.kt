package com.whitelabel.android.data.model

import android.graphics.Color
import android.util.Log
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.whitelabel.android.utils.Utils.hexToColor
import kotlinx.serialization.Serializable

@Serializable
data class ColorProperty(
    val colorName: String = "",
    val colorCode: String = "",
    val id: Int = -1,
    val roomTypeId: Int = -1,
    val colorCatalogue: String = "",
    val r: Int = Color.DKGRAY.red,
    val g: Int = Color.DKGRAY.green,
    val b: Int = Color.DKGRAY.blue
) {
    val colorValue: Int
        get() {
            val isTrue = r != Color.DKGRAY.red || g != Color.DKGRAY.green || b != Color.DKGRAY.blue
            // First try to use R, G, B values if they are not default
            Log.e("ColorProperty", "convertColorCodeToColorValue: $isTrue $colorCode")
            if (isTrue) {
                return Color.rgb(r, g, b)
            }
            // Then try with colorCode
            try {
                return colorCode.hexToColor()
            } catch (e: IllegalArgumentException) {
                Log.e(
                    "ColorProperty",
                    "convertColorCodeToColorValue: $isTrue ${e.localizedMessage}"
                )
                return Color.WHITE // At the end return WHITE
            }
        }
}