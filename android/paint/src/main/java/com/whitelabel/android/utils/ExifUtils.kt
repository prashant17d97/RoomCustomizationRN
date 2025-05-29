package com.whitelabel.android.utils

import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.IOException

internal object ExifUtils {
    /* JADX INFO: Access modifiers changed from: package-private */
    @JvmStatic
    fun getImageRotation(str: String): Int {
        try {
            val attributeInt = ExifInterface(str).getAttributeInt("Orientation", 1)
            if (attributeInt != 3) {
                if (attributeInt != 6) {
                    return if (attributeInt != 8) 0 else 270
                }
                return 90
            }
            return 180
        } catch (e: IOException) {
            Log.e("ExifUtils", "processImage: $e", )
            return 0
        } catch (e2: Exception) {
            Log.e("ExifUtils", "processImage: $e2", )
            return 0
        }
    }
}
