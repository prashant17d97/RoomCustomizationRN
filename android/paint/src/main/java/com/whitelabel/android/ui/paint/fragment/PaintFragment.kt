package com.whitelabel.android.ui.paint.fragment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.whitelabel.android.R
import com.whitelabel.android.data.model.ColorProperty
import com.whitelabel.android.databinding.FragmentPaintBinding
import com.whitelabel.android.interfaces.ColorProvider
import com.whitelabel.android.interfaces.ImageProvider
import com.whitelabel.android.interfaces.PaintButtonClickListener
import com.whitelabel.android.interfaces.PaintInterfaceRegistry
import com.whitelabel.android.ui.adapter.ColorAdapter
import com.whitelabel.android.ui.bsd.ShareBottomSheetDialog
import com.whitelabel.android.utils.ActivityLoader
import com.whitelabel.android.utils.ActivityLoader.COLOR_PROPERTIES_LIST
import com.whitelabel.android.utils.CommonUtils.getCorrectlyOrientedBitmap
import com.whitelabel.android.utils.CoroutineUtils.backgroundScope
import com.whitelabel.android.utils.Utils
import com.whitelabel.android.utils.Utils.convertJsonToColorList
import com.whitelabel.android.utils.Utils.saveToGallery
import com.whitelabel.android.utils.Utils.shareBitmapFromFragment
import com.whitelabel.android.utils.Utils.toColorProperty
import com.whitelabel.android.utils.Utils.toTextColor
import com.whitelabel.android.view.RecolourImageView
import com.whitelabel.android.view.RecolourImageView.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

class PaintFragment : Fragment(), RecolourImageView.Listener {
    private lateinit var mContext: Context
    private val buttonClickListener: PaintButtonClickListener by lazy {
        PaintInterfaceRegistry.getButtonClickListener()
    }

    private var imageUri = Uri.EMPTY

    private var pickedColorHex: String? = null

    companion object {
        private const val TAG = "PaintFragment"
    }

    private var _binding: FragmentPaintBinding? = null
    private val binding get() = _binding!!

    private val pickVisualMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                processImage(uri)
                Log.d(TAG, "Selected URI: $uri")
            } else {
                Log.d(TAG, "No media selected")
            }
        }

    lateinit var backPressedCallback: OnBackPressedCallback

    private val imageProvider: ImageProvider = object : ImageProvider {
        override fun loadImageUri(imageUri: Uri) {
            processImage(imageUri)
        }

        override fun loadImageBitmap(imageBitmap: Bitmap) {
            processImage(imageBitmap)
        }
    }

    private val colorProvider: ColorProvider = object : ColorProvider {
        override fun getCurrentColor(): ColorProperty = binding.selectedPhotoImageView.getColor()

        override fun updateColor(color: ColorProperty) {
            binding.selectedPhotoImageView.setColor(color)
            with(binding) {
                updateButtonState()
            }
        }
    }

    private var tempFileToDelete: File? = null

    private val shareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Cleanup after user shares or cancels
        Log.d(TAG, "shareLauncher: ${result.data}, ${result.resultCode}")
        tempFileToDelete?.delete()
        tempFileToDelete = null
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaintBinding.inflate(inflater, container, false)
        mContext = binding.root.context
        PaintInterfaceRegistry.registerColorProvider(colorProvider)
        PaintInterfaceRegistry.registerImageProvider(imageProvider)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        PaintInterfaceRegistry.unregisterColorProvider()
        PaintInterfaceRegistry.unregisterImageProvider()
        backPressedCallback.remove()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleArguments()
        with(binding) {
            updateActiveColor()
            updateButtonState()
            onBackPressed()
            colorPickerBtn.setOnClickListener {
                handleClicks(PaintClickEvent.ColorPicker)
            }
            backButton.setOnClickListener { handleArrowAndBackPress() }
            activatedPaintBrushBtn.setOnClickListener {
                handleClicks(PaintClickEvent.PaintBrush)
            }
            saveColorBtn.setOnClickListener {
                pickedColorHex?.let { hexColorString ->
                    buttonClickListener.onPaintButtonClicked(
                        BottomSheetClickEvent.SaveColorClick(hexColorString)
                    )
                }
            }
            imageGalleryBtn.setOnClickListener {
                handleClicks(PaintClickEvent.GalleryRequest)
            }
            shareImageBtn.setOnClickListener {
                showBottomSheet()
            }
            selectedPhotoImageView.setListener(this@PaintFragment)
            setActiveTool()
        }
    }

    private fun showBottomSheet() {
        val dialog = ShareBottomSheetDialog(
            onSaveToGalleryClick = {
                getImageBitmap {
                    saveToGallery(it, mContext)
                }
            },
            onSaveToProjectClick = {
                getImageBitmap {
                    buttonClickListener.onPaintButtonClicked(
                        BottomSheetClickEvent.SaveToProjectClick(
                            it
                        )
                    )
                }
            },
            onSendToColorConsultationClick = {
                getImageBitmap {
                    buttonClickListener.onPaintButtonClicked(
                        BottomSheetClickEvent.SendToColorConsultationClick(
                            it
                        )
                    )
                }
            },
            onSharePhotoClick = {
                getImageBitmap {
                    shareLauncher.shareBitmapFromFragment(it, mContext) {
                        tempFileToDelete = it
                    }
                }
            }
        )

        dialog.show(childFragmentManager, "ShareBottomSheetDialog")
    }


    private fun getImageBitmap(bitmapCallback: (Bitmap) -> Unit) {
        lifecycleScope.launch {
            with(binding.selectedPhotoImageView) {
                sharingImage(
                    this
                )?.let {
                    bitmapCallback(it)
                }
            }
        }
    }

    private fun handleArguments() {
        arguments?.let { bundle ->
            colorProvider.updateColor(
                bundle.getString(ActivityLoader.COLOR_JSON).toColorProperty()
            )
            bundle.getString(ActivityLoader.IMAGE_URI)?.toUri()?.let {
                imageUri = it
                lifecycleScope.launch(Dispatchers.IO) {
                    processImage(imageUri)
                }

            }
            initializeColorList(bundle.getString(COLOR_PROPERTIES_LIST).convertJsonToColorList())
            updateCurrentColorText(
                colorName = colorProvider.getCurrentColor().colorName,
                colorValue = colorProvider.getCurrentColor().colorValue
            )
        }
    }

    private fun initializeColorList(colorOptionsList: List<ColorProperty>) {
        binding.colorOptionList.visibility =
            View.VISIBLE.takeIf { colorOptionsList.isNotEmpty() } ?: View.GONE
        binding.colorOptionList.layoutManager =
            StaggeredGridLayoutManager(
                (colorOptionsList.size / 5.0).roundToInt(),
                RecyclerView.HORIZONTAL
            )
        binding.colorOptionList.adapter = ColorAdapter(
            colors = colorOptionsList,
            onColorSelected = {
                updateCurrentColorText(it.colorName, it.colorValue)
                colorProvider.updateColor(it)
                binding.selectedPhotoImageView.apply {
                    currentTool = Tool.FILL
                    this.onTap(
                        MotionEvent.obtain(
                            System.currentTimeMillis(),
                            System.currentTimeMillis(),
                            MotionEvent.ACTION_DOWN,
                            0.0f,
                            0.0f,
                            0
                        )
                    )
                }
            }
        )
    }

    private fun updateCurrentColorText(colorName: String, colorValue: Int) {
        binding.selectedColorDetail.apply {
            text = colorName
            setTextColor(colorValue.toTextColor())
        }

        binding.saveColorBtn.setTextColor(colorValue.toTextColor())
        binding.currentColorDetail.setBackgroundColor(colorValue)
    }

    private fun handleClicks(paintClickEvent: PaintClickEvent) {
        lifecycleScope.launch {
            when (paintClickEvent) {
                is PaintClickEvent.PaintBrush -> setActiveTool()
                is PaintClickEvent.GalleryRequest -> requestGalleryImage()
                is PaintClickEvent.ColorPicker -> showColorPicker()
                else -> Unit // Do nothing
            }
            binding.updateButtonState(paintClickEvent = paintClickEvent)
        }
    }

    private fun FragmentPaintBinding.updateActiveColor() {
        val activeColor = colorProvider.getCurrentColor()
        selectedPhotoImageView.setColor(activeColor)

    }

    private fun requestGalleryImage() {
        setActiveTool()
        pickVisualMedia.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.SingleMimeType(
                    "image/*"
                )
            )
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onColorPicked(color: Int, movementX: Float, movementY: Float) {
        val hex = String.format("#%06X", 0xFFFFFF and color)
        Log.d(TAG, "onColorPicked: RGB: $color, HEX: $hex")
        if (binding.selectedPhotoImageView.currentTool == Tool.COLOR_PICKER) {
            pickedColorHex = hex
            updateCurrentColorText(
                colorName = hex,
                colorValue = color
            )
            binding.saveColorBtn.setTextColor(color.toTextColor())
            binding.currentColorDetail.setBackgroundColor(color)
            binding.eyeDropTint.setColorFilter(color)
            binding.selectedPhotoImageView.setColor(
                ColorProperty(
                    colorName = hex,
                    colorCode = hex,
                )
            )

            with(binding.eyeDrop) {
                x = movementX
                y = movementY
            }

        }
    }


    private fun onBackPressed() {
        backPressedCallback = object : OnBackPressedCallback(
            true // default to enabled
        ) {
            override fun handleOnBackPressed() {
                handleArrowAndBackPress()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            backPressedCallback
        )

    }

    fun handleArrowAndBackPress() {
        if (binding.selectedPhotoImageView.currentTool == Tool.COLOR_PICKER) {
            showAllUI()
        } else {
            parentFragmentManager.popBackStack()
        }
    }

    private fun showAllUI() {
        setActiveTool()
        with(binding) {
            colorOptionList.visibility = View.VISIBLE
            buttonsLayout.visibility = View.VISIBLE
            saveColorBtn.visibility = View.GONE
            binding.eyeDrop.visibility = View.GONE
        }
    }

    private fun showColorPicker() {
        binding.selectedPhotoImageView.undoAll()
        setActiveTool(Tool.COLOR_PICKER)
        with(binding) {
            colorOptionList.visibility = View.GONE
            buttonsLayout.visibility = View.GONE
            saveColorBtn.visibility = View.VISIBLE
            binding.eyeDrop.visibility = View.VISIBLE
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

    private fun FragmentPaintBinding.updateButtonState(
        paintClickEvent: PaintClickEvent = PaintClickEvent.PaintBrush
    ) {
        val hasImage = selectedPhotoImageView.hasImage
        if (!hasImage || paintClickEvent is PaintClickEvent.None) return

        val allButtons = listOf(
            activatedPaintBrushBtn,
            imageGalleryBtn,
            colorPickerBtn,
            shareImageBtn
        )

        val selectedButton = when (paintClickEvent) {
            is PaintClickEvent.ShareClick -> shareImageBtn
            PaintClickEvent.ColorPicker -> colorPickerBtn
            PaintClickEvent.GalleryRequest -> imageGalleryBtn
            else -> activatedPaintBrushBtn
        }

        binding.eyeDrop.visibility =
            View.VISIBLE.takeIf { binding.selectedPhotoImageView.currentTool == Tool.COLOR_PICKER }
                ?: View.GONE
        allButtons.forEach { button ->
            if (button == selectedButton) {
                button.setSelectedColor()
            } else {
                button.setUnSelectedColor()
            }
        }
    }

    private fun ImageButton.setSelectedColor() {
        setColorFilter(
            ContextCompat.getColor(mContext, R.color.selected_button_color),
            PorterDuff.Mode.SRC_IN
        )
    }

    private fun ImageButton.setUnSelectedColor() {
        setColorFilter(
            ContextCompat.getColor(mContext, R.color.unselected_button_color),
            PorterDuff.Mode.SRC_IN
        )
    }

    override fun recolourImageViewDidUpdateFillThreshold(recolourImageView: RecolourImageView?) {

    }

    override fun recolourImageViewDidUpdateCoverage(recolourImageView: RecolourImageView?) {

    }


}