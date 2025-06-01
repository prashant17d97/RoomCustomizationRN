package com.roomcustomizationrn

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.whitelabel.android.data.model.ColorProperty
import com.whitelabel.android.interfaces.ColorProvider
import com.whitelabel.android.interfaces.ImageProvider
import com.whitelabel.android.interfaces.PaintInterfaceRegistry
import com.whitelabel.android.ui.paint.fragment.BottomSheetClickEvent
import com.whitelabel.android.utils.ActivityLoader.showPaintFragment
import com.whitelabel.android.utils.Utils.convertJsonToColorList
import java.io.ByteArrayOutputStream

/**
 * React Native module that provides access to the Paint functionality.
 */
class PaintModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule() {

    companion object {
        private const val NAME = "PaintModule"
        private var instance: PaintModule? = null

        fun getInstance(): PaintModule? = instance
    }

    init {
        instance = this
    }

    override fun getName(): String = NAME

    @ReactMethod
    fun showPaintFragment(
        color: String,
        imageUri: String,
        id: Int = -1,
        colorCatalogue: String = "",
        colorName: String = "Default Color",
        colorOptionsListGson: String = "",
    ) {
            val activity = reactContext.currentActivity as? MainActivity ?: return
            val fragmentContainerId = activity.fragmentContainerId

            // Create a ColorProperty object with the provided values
            val colorProperty = ColorProperty(
                colorName = colorName,
                colorCode = color,
                id = id,
                colorCatalogue = colorCatalogue
            )

            // Parse the colorOptionsListGson to a list of ColorProperty if provided
            val colorOptionsList = if (colorOptionsListGson.isNotEmpty()) {
                try {
                    colorOptionsListGson.convertJsonToColorList()
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            // Call the showPaintFragment extension function from ActivityLoader
            activity.showPaintFragment(
                colorProperty = colorProperty,
                imageUri = Uri.parse(imageUri),
                colorOptionsListGson = colorOptionsList,
                fragmentContainerId = fragmentContainerId
            )
        }

    /**
     * Handles paint click events from the native side and forwards them to React Native.
     *
     * @param event The PaintClickEvent to handle and forward to React Native.
     */
    fun handlePaintClickEvent(event: BottomSheetClickEvent) {
        val params = Arguments.createMap()

        when (event) {
            is BottomSheetClickEvent.SaveToProjectClick -> {
                params.putString("type", "saveToProjectClick")
                // Convert bitmap to base64 string
                val base64Image = bitmapToBase64(event.bitmap)
                params.putString("image", base64Image)
                emitEvent("paintButtonClicked", params)
            }

            is BottomSheetClickEvent.SendToColorConsultationClick -> {
                params.putString("type", "sendToColorConsultationClick")
                // Convert bitmap to base64 string
                val base64Image = bitmapToBase64(event.bitmap)
                params.putString("image", base64Image)
                emitEvent("paintButtonClicked", params)
            }

            is BottomSheetClickEvent.SaveColorClick -> {
                params.putString("type", "saveColorClick")
                params.putString("color", event.hexColorString)
                emitEvent("paintButtonClicked", params)
            }
        }
    }

    /**
     * Emits an event to React Native.
     *
     * @param eventName The name of the event to emit.
     * @param params The parameters to pass with the event.
     */
    private fun emitEvent(eventName: String, params: WritableMap) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    /**
     * Converts a Bitmap to a base64 encoded string.
     *
     * @param bitmap The Bitmap to convert.
     * @return The base64 encoded string representation of the Bitmap.
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Gets the current color from the ColorProvider.
     *
     * @return A map containing the color information.
     */
    @ReactMethod(isBlockingSynchronousMethod = true)
    fun getCurrentColor(): WritableMap {
        val activity = reactContext.currentActivity as? MainActivity ?: return Arguments.createMap()
        val colorProvider = activity.getColorProvider()
        val currentColor = colorProvider.getCurrentColor()

        return Arguments.createMap().apply {
            putString("colorName", currentColor.colorName)
            putString("colorCode", currentColor.colorCode)
            putInt("colorValue", currentColor.colorValue)
            putInt("id", currentColor.id)
            putString("colorCatalogue", currentColor.colorCatalogue)
        }
    }

    /**
     * Updates the current color in the ColorProvider.
     *
     * @param colorMap A map containing the color information.
     */
    @ReactMethod
    fun updateColor(colorMap: ReadableMap) {
        val activity = reactContext.currentActivity as? MainActivity ?: return
        val colorProvider = activity.getColorProvider()

        val colorName = colorMap.getString("colorName") ?: ""
        val colorCode = colorMap.getString("colorCode") ?: ""
        val colorValue = colorMap.getInt("colorValue")
        val id = colorMap.getInt("id")
        val colorCatalogue = colorMap.getString("colorCatalogue") ?: ""

        // Extract RGB values from colorValue
        val r = (colorValue shr 16) and 0xFF
        val g = (colorValue shr 8) and 0xFF
        val b = colorValue and 0xFF

        val colorProperty = ColorProperty(
            colorName = colorName,
            colorCode = colorCode,
            id = id,
            roomTypeId = -1,
            colorCatalogue = colorCatalogue,
            r = r,
            g = g,
            b = b
        )

        colorProvider.updateColor(colorProperty)
    }

    /**
     * Loads an image from the provided URI using the ImageProvider.
     *
     * @param imageUri The URI of the image to be loaded.
     */
    @ReactMethod
    fun loadImageUri(imageUri: String) {
        val activity = reactContext.currentActivity as? MainActivity ?: return
        val imageProvider = activity.getImageProvider()
        val uri = Uri.parse(imageUri)
        imageProvider.loadImageUri(uri)
    }

    /**
     * Loads an image from a base64 encoded string using the ImageProvider.
     *
     * @param base64Image The base64 encoded string representation of the image.
     */
    @ReactMethod
    fun loadImageBase64(base64Image: String) {
        val activity = reactContext.currentActivity as? MainActivity ?: return
        val imageProvider = activity.getImageProvider()

        try {
            val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            imageProvider.loadImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
