package com.whitelabel.android.ui.paint.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.whitelabel.android.data.model.ColorProperty
import com.whitelabel.android.utils.CommonUtils
import com.whitelabel.android.utils.CoroutineUtils.backgroundScope
import com.whitelabel.android.view.RecolourImageView
import java.io.IOException

class PaintFragmentUtil {
    // LiveData for UI state
    private val _imageProcessingState = MutableLiveData<ImageProcessingState>()
    val imageProcessingState: LiveData<ImageProcessingState> = _imageProcessingState

    private val _currentTool = MutableLiveData<RecolourImageView.Tool>()
    val currentTool: LiveData<RecolourImageView.Tool> = _currentTool

    private val _activeColor = MutableLiveData<ColorProperty>()
    val activeColor: LiveData<ColorProperty> = _activeColor

    private val _fillThreshold = MutableLiveData<Float>()
    val fillThreshold: LiveData<Float> = _fillThreshold

    private val _coverage = MutableLiveData<Float>()
    val coverage: LiveData<Float> = _coverage

    // Initialize with default values
    init {
        _currentTool.value = RecolourImageView.Tool.FILL
        _activeColor.value = ColorProperty()
        _fillThreshold.value = 0.5f
        _coverage.value = 0.5f
    }
    suspend fun processImageUri(context: Context, uri: Uri?) {
        if (uri == null) return
        _imageProcessingState.value = ImageProcessingState.Loading
        val imageBitmap = processImage(context = context, uri)
        val response =
            imageBitmap?.let { ImageProcessingState.Success(it) } ?: ImageProcessingState.Error(
                "Something went wrong"
            )
        _imageProcessingState.value = response

    }


    fun setActiveTool(tool: RecolourImageView.Tool) {
        _currentTool.value = tool
    }

    fun setActiveColor(colorProperty: ColorProperty) {
        _activeColor.value = colorProperty
    }

    fun updateFillThreshold(threshold: Float) {
        _fillThreshold.value = threshold
    }

    fun updateCoverage(coverage: Float) {
        _coverage.value = coverage
    }

    // Sealed class for image processing state
    sealed class ImageProcessingState {
        data object Loading : ImageProcessingState()
        data class Success(val bitmap: Bitmap) : ImageProcessingState()
        data class Error(val message: String) : ImageProcessingState()
    }

    private suspend fun processImage(context: Context, uri: Uri): Bitmap? = backgroundScope {
        return@backgroundScope try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bufferedStream = inputStream?.buffered()?.apply { mark(available()) }
            return@backgroundScope bufferedStream?.let { CommonUtils.getCorrectlyOrientedBitmap(it) }
        } catch (e: IOException) {
            Log.e("ExifUtils", "processImage: $e", )
            null
        }
    }
}
