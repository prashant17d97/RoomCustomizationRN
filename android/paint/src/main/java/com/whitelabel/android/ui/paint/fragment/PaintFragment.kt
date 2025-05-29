package com.whitelabel.android.ui.paint.fragment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.whitelabel.android.BuildConfig
import com.whitelabel.android.data.model.ImageMaskColor
import com.whitelabel.android.databinding.FragmentPaintBinding
import com.whitelabel.android.interfaces.ColorProvider
import com.whitelabel.android.interfaces.ImageProvider
import com.whitelabel.android.interfaces.PaintButtonClickListener
import com.whitelabel.android.interfaces.PaintInterfaceRegistry
import com.whitelabel.android.utils.ActivityLoader
import com.whitelabel.android.utils.CommonUtils
import com.whitelabel.android.utils.CommonUtils.createTakePhotoDialog
import com.whitelabel.android.utils.CommonUtils.getCorrectlyOrientedBitmap
import com.whitelabel.android.utils.CoroutineUtils.backgroundScope
import com.whitelabel.android.utils.Utils
import com.whitelabel.android.utils.Utils.createImageFile
import com.whitelabel.android.utils.Utils.hasCameraPermission
import com.whitelabel.android.utils.Utils.hexToColor
import com.whitelabel.android.view.RecolourImageView
import com.whitelabel.android.view.RecolourImageView.Tool
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Objects
import kotlin.math.roundToInt

class PaintFragment : Fragment(), RecolourImageView.Listener, ColorProvider {
    private lateinit var mContext: Context
    private var buttonClickListener: PaintButtonClickListener? = null

    private var imageUri = Uri.EMPTY

    companion object {
        private const val TAG = "PaintFragment"
    }

    private var _binding: FragmentPaintBinding? = null
    private val binding get() = _binding!!

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { result ->
        if (result) {
            processImage(imageUri)
        }
    }


    private val pickVisualMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                processImage(uri)
                Log.d(TAG, "Selected URI: $uri")
            } else {
                Log.d(TAG, "No media selected")
            }
        }
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                requestCameraImage()
            } else {
                requestGalleryImage()
            }
        }


    private val imageProvider: ImageProvider = object : ImageProvider {
        override fun loadImageUri(imageUri: Uri) {
            processImage(imageUri)
        }

        override fun loadImageBitmap(imageBitmap: Bitmap) {
            processImage(imageBitmap)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaintBinding.inflate(inflater, container, false)
        mContext = binding.root.context
        PaintInterfaceRegistry.registerColorProvider(this)
        PaintInterfaceRegistry.registerImageProvider(imageProvider)
        buttonClickListener = PaintInterfaceRegistry.getButtonClickListener()
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        PaintInterfaceRegistry.unregisterColorProvider()
        PaintInterfaceRegistry.unregisterImageProvider()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleArguments()
        with(binding) {
            updateActiveColor()
            updateButtonState()
            colorSensitivitySeekbar.max = 1000
            colorIntensitySeekbar.max = 1000
            selectedPhotoImageView
            choosePhotoBtn.setOnClickListener {
                handleClicks(PaintClickEvent.ImageRequest)
            }
            reChoosePhotoBtn.setOnClickListener {
                handleClicks(PaintClickEvent.NewImageRequest)
            }
            paintRoll.setOnClickListener {
                handleClicks(PaintClickEvent.PaintRoll)
            }
            eraseButton.setOnClickListener {
                handleClicks(PaintClickEvent.EraserClick)
            }
            undoButton.setOnClickListener {
                handleClicks(PaintClickEvent.UndoClick)
            }
            shareButton.setOnClickListener {
                lifecycleScope.launch {
                    with(binding.selectedPhotoImageView) {
                        sharingImage(
                            this,
                            context = mContext
                        )
                    }?.let { buttonClickListener?.onPaintButtonClicked(PaintClickEvent.ShareClick(it)) }
                }
            }
            colorPalette.setOnClickListener {
                handleClicks(PaintClickEvent.ColorPalette)
            }
            colorSensitivitySeekbar.setupSeekBar()
            colorIntensitySeekbar.setupSeekBar()
            selectedPhotoImageView.setListener(this@PaintFragment)
            setActiveTool()
        }
    }

    private fun handleArguments() {
        arguments?.let { bundle ->
            updateColor(
                ImageMaskColor(
                    colorName = bundle.getString(ActivityLoader.COLOR_NAME) ?: "",
                    colorCode = bundle.getString(ActivityLoader.COLOR_HEX) ?: "#FF0000",
                    colorValue = bundle.getString(ActivityLoader.COLOR_HEX)?.hexToColor()
                        ?: Color.BLACK,
                    fandeckId = bundle.getInt(ActivityLoader.FANDECK_ID, -1),
                    fandeckName = bundle.getString(ActivityLoader.FANDECK_NAME) ?: "Default Fandeck"
                )
            )
            bundle.getString(ActivityLoader.IMAGE_URI)?.toUri()?.let {
                processImage(it)
            }
        }
    }

    private fun SeekBar.setupSeekBar() {
        setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (binding.selectedPhotoImageView.coverage >= 0.0f) {
                    binding.selectedPhotoImageView.coverage = progress / 1000.0f
                    Log.d(
                        TAG,
                        "onProgressChanged: ${binding.selectedPhotoImageView.coverage}, ${binding.selectedPhotoImageView.fillThreshold}"
                    )
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

//                seekBar?.progress = seekBar?.progress ?: 0
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Optional: Same as above
//                seekBar?.progress = seekBar?.progress ?: 0
            }
        })
    }


    private fun handleClicks(paintClickEvent: PaintClickEvent) {
        lifecycleScope.launch {
            when (paintClickEvent) {
                is PaintClickEvent.EraserClick -> {
                    setActiveTool(Tool.ERASER)
                }

                is PaintClickEvent.PaintRoll -> {
                    setActiveTool()
                }

                is PaintClickEvent.ImageRequest,
                is PaintClickEvent.NewImageRequest -> showPhotoPicker()

                is PaintClickEvent.UndoClick -> binding.selectedPhotoImageView.undo()

                else -> Unit // Do nothing
            }
            buttonClickListener?.onPaintButtonClicked(paintClickEvent)
            binding.updateButtonState()
        }
    }

    private fun FragmentPaintBinding.updateActiveColor() {
        val activeColor = getCurrentColor()
        selectedPhotoImageView.setColor(activeColor)
        val isColorLight: Boolean = CommonUtils.isColorLight(activeColor)
        buttonsLayout.setBackgroundColor(CommonUtils.getARGB(activeColor))
        with(binding) {
            CommonUtils.setImageViewColor(shareButton, isColorLight)
            CommonUtils.setImageViewColor(colorPalette, isColorLight)
            CommonUtils.setImageViewColor(paintRoll, isColorLight)
            CommonUtils.setImageViewColor(eraseButton, isColorLight)
            CommonUtils.setImageViewColor(undoButton, isColorLight)
            updateButtonState()
        }
    }

    private fun requestCameraImage() {
        if (::mContext.isInitialized && mContext.hasCameraPermission()) {
            imageUri = generateImageFile()
            if (imageUri != null) {
                cameraLauncher.launch(imageUri)
            }
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun showPhotoPicker() {
        val dialog = createTakePhotoDialog(
            context = mContext,
            onTakePhotoClickListener = { dialog, i ->
                requestCameraImage()
                dialog.dismiss()
            }, onChooseFromAlbumClickListener = { dialog, i ->
                requestGalleryImage()
                dialog.dismiss()
            })
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.MATCH_PARENT
        dialog.window!!.setLayout(width, height)
        dialog.show()

    }

    private fun requestGalleryImage() {
        pickVisualMedia.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.SingleMimeType(
                    "image/*"
                )
            )
        )
    }

    private fun generateImageFile(): Uri? {
        return FileProvider.getUriForFile(
            Objects.requireNonNull(mContext),
            BuildConfig.LIBRARY_PACKAGE_NAME + ".provider",
            mContext.createImageFile()
        )
    }

    private fun processImage(uri: Uri) {
        val selectedPhotoImageView = binding.selectedPhotoImageView
        safeUndoAll()

        // Coroutine delay replaces Handler.postDelayed
        lifecycleScope.launch {
            delay(100) // delay 100ms
            val bitmap = backgroundScope {
                try {
                    val inputStream = mContext.contentResolver.openInputStream(uri)
                        ?: return@backgroundScope null
                    val bufferedStream = inputStream.buffered().apply { mark(available()) }

                    val originalBitmap = getCorrectlyOrientedBitmap(bufferedStream)
                    scaleImage(bitmap = originalBitmap)

                } catch (e: IOException) {
                    Log.e(TAG, "processImage: $e")
                    null
                } catch (e: OutOfMemoryError) {
                    Log.e(TAG, "processImage: $e")
                    Utils.showOutOfMemoryToast(mContext)
                    null
                } catch (e: Exception) {
                    Log.e(TAG, "processImage: $e")
                    null
                }
            }

            if (bitmap != null) {
                selectedPhotoImageView.setImage(bitmap)
                finaliseSetup()
            }
        }
    }

    private fun processImage(bitmap: Bitmap) {
        safeUndoAll()
        // Coroutine delay replaces Handler.postDelayed
        lifecycleScope.launch {
            delay(100) // delay 100ms
            binding.selectedPhotoImageView.setImage(scaleImage(bitmap = bitmap))
            finaliseSetup()
        }
    }


    private fun safeUndoAll() {
        try {
            binding.selectedPhotoImageView.undoAll()
        } catch (exception: Exception) {
            Log.i(TAG, "PaintFragment in processImage undoAll Exception", exception)
        }
    }

    private suspend fun scaleImage(bitmap: Bitmap): Bitmap = backgroundScope {
        val originalWidth = bitmap.width.toDouble()
        val originalHeight = bitmap.height.toDouble()
        val width = binding.selectedPhotoImageView.width
        val height = binding.selectedPhotoImageView.height
        val scale = minOf(1.0, minOf(width / originalWidth, height / originalHeight))
        return@backgroundScope bitmap.scale(
            (originalWidth * scale).toInt(), (originalHeight * scale).toInt()
        )
    }


    private fun finaliseSetup() {
        binding.updateActiveColor()
        setActiveTool()
    }

    private fun setActiveTool(tool: Tool = Tool.FILL) {
        binding.selectedPhotoImageView.currentTool = tool
        binding.updateButtonState()
    }

    private fun FragmentPaintBinding.updateButtonState() {
        val hasImage = selectedPhotoImageView.hasImage
        paintRoll.isEnabled = hasImage
        paintRoll.alpha = if (hasImage) 1.0f else 0.4f
        eraseButton.isEnabled = hasImage
        eraseButton.alpha = if (hasImage) 1.0f else 0.4f
        undoButton.isEnabled = hasImage
        undoButton.alpha = if (hasImage) 1.0f else 0.4f
        shareButton.isEnabled = hasImage
        shareButton.alpha = if (hasImage) 1.0f else 0.4f
        paintMyRoomNoContent.visibility = if (hasImage) View.GONE else View.VISIBLE
        updateSelectedButtonTint()
    }

    private fun FragmentPaintBinding.updateSelectedButtonTint() {
        val hasImage = selectedPhotoImageView.hasImage
        val tool = selectedPhotoImageView.currentTool
        val isPaintRollerSelected = hasImage && tool == Tool.FILL
        if (isPaintRollerSelected) {
            paintRoll.isSelected = true
            CommonUtils.setImageViewColor(paintRoll, true)
            CommonUtils.setImageViewColor(eraseButton, false)
        } else {
            eraseButton.isSelected = true
            CommonUtils.setImageViewColor(paintRoll, false)
            CommonUtils.setImageViewColor(eraseButton, true)
        }
    }

    override fun recolourImageViewDidUpdateFillThreshold(recolourImageView: RecolourImageView?) {
        binding.colorSensitivitySeekbar.progress =
            (binding.selectedPhotoImageView.fillThreshold * 1000.0f).roundToInt()
    }

    override fun recolourImageViewDidUpdateCoverage(recolourImageView: RecolourImageView?) {
        with(binding) {
            colorIntensitySeekbar.progress =
                (selectedPhotoImageView.coverage * 1000.0f).roundToInt()
        }
    }

    override fun getCurrentColor(): ImageMaskColor = binding.selectedPhotoImageView.getColor()

    override fun updateColor(color: ImageMaskColor) {
        Log.d(TAG, "updateColor: $color")
        binding.selectedPhotoImageView.setColor(color)
        binding.updateActiveColor()
    }

}
