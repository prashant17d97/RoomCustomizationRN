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
import com.whitelabel.android.data.model.ImageMaskColor
import com.whitelabel.android.interfaces.ColorProvider
import com.whitelabel.android.interfaces.ImageProvider
import com.whitelabel.android.interfaces.PaintInterfaceRegistry
import com.whitelabel.android.ui.paint.fragment.PaintClickEvent
import com.whitelabel.android.utils.ActivityLoader.loadPaintActivity
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

    /**
     * Launches the PaintActivity with the specified color and image URI.
     *
     * @param color The hex string representing the color to use (e.g., "#FFFFFF").
     * @param imageUri The URI of the image to be used in the activity.
     * @param fandeckId Optional ID representing the fandeck. Defaults to -1 if not provided.
     * @param fandeckName Optional name of the fandeck. Defaults to an empty string if not provided.
     * @param colorName Optional name of the color. Defaults to "Default Color" if not provided.
     */
    @ReactMethod
    fun startPaintActivity(
        color: String,
        imageUri: String,
        fandeckId: Int = -1,
        fandeckName: String = "",
        colorName: String = "Default Color"
    ) {
        val activity = reactContext.currentActivity ?: return
        val uri = Uri.parse(imageUri)
        activity.loadPaintActivity(color, uri, fandeckId, fandeckName, colorName)
    }

    /**
     * Handles paint click events from the native side and forwards them to React Native.
     *
     * @param event The PaintClickEvent to handle and forward to React Native.
     */
    fun handlePaintClickEvent(event: PaintClickEvent) {
        val params = Arguments.createMap()

        when (event) {
            is PaintClickEvent.ColorPalette -> {
                params.putString("type", "colorPalette")
                emitEvent("paintButtonClicked", params)
            }
            is PaintClickEvent.UndoClick -> {
                params.putString("type", "undoClick")
                emitEvent("paintButtonClicked", params)
            }
            is PaintClickEvent.EraserClick -> {
                params.putString("type", "eraserClick")
                emitEvent("paintButtonClicked", params)
            }
            is PaintClickEvent.ShareClick -> {
                params.putString("type", "shareClick")
                // Convert bitmap to base64 string
                val base64Image = bitmapToBase64(event.bitmap)
                params.putString("image", base64Image)
                emitEvent("paintButtonClicked", params)
            }
            is PaintClickEvent.PaintRoll -> {
                params.putString("type", "paintRoll")
                emitEvent("paintButtonClicked", params)
            }
            is PaintClickEvent.ImageRequest -> {
                params.putString("type", "imageRequest")
                emitEvent("paintButtonClicked", params)
            }
            is PaintClickEvent.NewImageRequest -> {
                params.putString("type", "newImageRequest")
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
            putInt("fandeckId", currentColor.fandeckId)
            putString("fandeckName", currentColor.fandeckName)
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
        val fandeckId = colorMap.getInt("fandeckId")
        val fandeckName = colorMap.getString("fandeckName") ?: ""

        val imageMaskColor = ImageMaskColor(
            colorName = colorName,
            colorCode = colorCode,
            colorValue = colorValue,
            fandeckId = fandeckId,
            fandeckName = fandeckName
        )

        colorProvider.updateColor(imageMaskColor)
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
