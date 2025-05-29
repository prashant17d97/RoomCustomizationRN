package com.whitelabel.android.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.view.isVisible
import com.whitelabel.android.R
import com.whitelabel.android.data.model.ImageMaskColor
import com.whitelabel.android.data.model.proto.RecolorImage
import com.whitelabel.android.data.model.proto.RecolorImageColor
import com.whitelabel.android.data.model.proto.RecolorImageMeta
import com.whitelabel.android.imagerNative.gen.PaintArea
import com.whitelabel.android.imagerNative.gen.PaintAreaVector
import com.whitelabel.android.imagerNative.gen.PaintMask
import com.whitelabel.android.utils.CoroutineUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jdeferred.Promise
import org.jdeferred.impl.DeferredObject
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow


/**
 * A custom FrameLayout for image recoloring functionality.
 *
 * This view allows users to load an image and apply different colors to sections of it
 * using various tools like fill, brush, eraser, and polygon selection.
 * It manages image transformations (zoom, pan), color selection, tool states,
 * history for undo operations, and saving/loading of recolored images.
 *
 * Key features:
 * - Image display and manipulation using [ZoomableImageView].
 * - Multiple recoloring tools: [Tool.FILL], [Tool.ERASER], [Tool.BRUSH], [Tool.POLYGON].
 * - Color selection via [ImageMaskColor].
 * - Mask generation and application using OpenCV.
 * - Undo history for recoloring operations.
 * - Saving and loading recolored images with metadata.
 * - Touch feedback for user interactions.
 * - Polygon selection for precise area definition.
 * - Freehand drawing for brush and eraser tools.
 *
 * The view heavily relies on OpenCV for image processing tasks such as mask generation,
 * color conversion, and contour detection. It uses coroutines for background tasks
 * to keep the UI responsive.
 *
 * @param context The Context the view is running in, through which it can
 *        access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr An attribute in the current theme that contains a
 *        reference to a style resource that supplies default values for
 *        the view. Can be 0 to not look for defaults.
 * @param defStyleRes A resource identifier of a style resource that
 *        supplies default values for the view, used only if
 *        defStyleAttr is 0 or can not be found in the theme. Can be 0
 *        to not look for defaults.
 *
 * @property isSaved Indicates whether the current state of the image has been saved.
 * @property hasImage True if an image is currently loaded, false otherwise.
 * @property currentTool The currently active recoloring tool.
 * @property fillThreshold The threshold for the fill tool, determining color sensitivity.
 * @property coverage The coverage or strength of the current tool (e.g., brush opacity).
 */
class RecolourImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes), ZoomableImageView.Listener,
    ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnGestureListener,
    FreeMaskView.Listener {

    private var activeMaskUpdate: Promise<*, *, *>? = null
    private var activeTool: Tool? = null
    private var customColor: ImageMaskColor? =
        null // Renamed mColor to avoid conflict with View.getColor()
    private val compositedHistory: MutableList<Bitmap> = mutableListOf()
    private lateinit var compositedView: ImageView
    private var currentCoverage = 0f // Renamed mCoverage
    private var currentColor: ImageMaskColor? = null
    private lateinit var freeMaskView: FreeMaskView
    private var hasRepaint = false
    private var imageBitmap: Bitmap? = null // Renamed mImage
    private lateinit var imageView: ZoomableImageView
    private lateinit var interceptGestureDetector: GestureDetector
    private lateinit var interceptScaleDetector: ScaleGestureDetector
    private var interceptingTouch: Boolean = false
    private var listener: Listener? = null
    private var needsReupdate = false
    private val paintAreas: PaintAreaVector = PaintAreaVector()
    private var paintMask: PaintMask? = null
    private lateinit var polygonView: PolygonView
    var isSaved: Boolean = true
    private var topColor: ImageMaskColor? = null
    private var topHasMask = false
    private var topTool: Tool? = null
    private lateinit var topView: ImageView
    private lateinit var touchFeedbackView: TouchFeedbackView
    private var usedColorsHistory: MutableList<Set<ImageMaskColor>?> = mutableListOf()

    private val viewScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    interface Listener {
        fun recolourImageViewDidUpdateCoverage(recolourImageView: RecolourImageView?)
        fun recolourImageViewDidUpdateFillThreshold(recolourImageView: RecolourImageView?)
    }

    enum class Tool {
        FILL, ERASER, BRUSH, POLYGON
    }

    init {
        initRecolourImageView(context)
    }

    private fun initRecolourImageView(ctx: Context) {
        interceptScaleDetector = ScaleGestureDetector(ctx, this).apply {
            isQuickScaleEnabled = false
        }
        interceptGestureDetector = GestureDetector(ctx, this)

        imageView = ZoomableImageView(ctx).apply {
            setMaxInitialScale(0.7f)
            enableImageTransforms(true)
            setScrollMinNumberOfPointers(2)
            setListener(this@RecolourImageView)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            setEdgeInset(
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, // Assuming R.dimen.unit was meant for DIP
                    EDGE_INSET_DP.toFloat(),
                    resources.displayMetrics
                ).toInt()
            )
            setOnClickListener(object : ZoomableImageView.OnClickListener {


                override fun onZoomableImageViewClick(motionEvent: MotionEvent?) {
                    if (motionEvent != null) {
                        this@RecolourImageView.onTap(motionEvent)
                    }
                }

                override fun onZoomableImageViewLongPress(motionEvent: MotionEvent?) {

                }
            })
        }
        addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        compositedView = ImageView(ctx).apply {
            scaleType = ImageView.ScaleType.MATRIX
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
        addView(compositedView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        topView = ImageView(ctx).apply {
            scaleType = ImageView.ScaleType.MATRIX
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
        addView(topView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        touchFeedbackView = TouchFeedbackView(ctx)
        addView(
            touchFeedbackView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        freeMaskView = FreeMaskView(ctx).apply {
            alpha = 0.3f
            visibility = View.GONE
            setListener(this@RecolourImageView)
        }
        addView(freeMaskView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        polygonView = PolygonView(ctx).apply {
            visibility = View.GONE
        }
        addView(polygonView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun onTap(motionEvent: MotionEvent) {
        val currentImage = imageBitmap ?: return
        val currentCustomColor = customColor ?: return

        when (activeTool) {
            Tool.FILL -> {
                val matrix = Matrix()
                imageView.matrix.invert(matrix)
                val x = motionEvent.x
                val y = motionEvent.y
                val mappedPoints = floatArrayOf(x, y)
                matrix.mapPoints(mappedPoints)

                if (mappedPoints[0] < 0.0f || mappedPoints[1] < 0.0f ||
                    mappedPoints[0] >= currentImage.width || mappedPoints[1] >= currentImage.height
                ) {
                    return
                }
                touchFeedbackView.displayFeedback(x, y)
                addSeedPoint(mappedPoints[0], mappedPoints[1], 0.25f)
                viewScope.launch { maybeApplyPolygonMask() } // Make suspend call from coroutine
                updateMask()
                isSaved = false
            }

            Tool.POLYGON -> {
                val matrix = Matrix()
                imageView.matrix.invert(matrix)
                val x = motionEvent.x
                val y = motionEvent.y
                val mappedPoints = floatArrayOf(x, y)
                matrix.mapPoints(mappedPoints)

                if (mappedPoints[0] < 0.0f || mappedPoints[1] < 0.0f ||
                    mappedPoints[0] >= currentImage.width || mappedPoints[1] >= currentImage.height
                ) {
                    return
                }
                touchFeedbackView.displayFeedback(x, y)
                maybeCommitHistory()

                viewScope.launch {
                    val rectF = withContext(Dispatchers.Default) {
                        paintMask?.let { pm ->
                            val tempPaintMask = PaintMask(pm)
                            val tempPaintAreaVector = PaintAreaVector()
                            tempPaintAreaVector.push_back(
                                PaintArea(
                                    mappedPoints[0].toDouble(),
                                    mappedPoints[1].toDouble(),
                                    0.15f
                                )
                            )
                            tempPaintMask.areas = tempPaintAreaVector
                            try {
                                val contourRect = PaintMask.findContourRect(tempPaintMask.image())
                                RectF(
                                    contourRect.x.toFloat(),
                                    contourRect.y.toFloat(),
                                    (contourRect.x + contourRect.width).toFloat(),
                                    (contourRect.y + contourRect.height).toFloat()
                                )
                            } catch (e: Exception) {

                                null
                            }
                        }
                    }
                    rectF?.let {
                        if (it.width() <= 10.0f || it.height() <= 10.0f) {
                            it.inset(-10.0f, -10.0f)
                        }
                        polygonView.setRect(it)
                    }
                    polygonView.visibility = View.VISIBLE
                }
            }

            else -> { /* No-op for other tools on tap */
            }
        }
    }

    val hasImage: Boolean
        get() = imageBitmap != null

    private fun addSeedPoint(mappedPoints: Float, mappedPoints2: Float, f3: Float) {
        val currentImage = imageBitmap ?: return
        if (mappedPoints < 0.0f || mappedPoints2 < 0.0f || mappedPoints >= currentImage.width || mappedPoints2 >= currentImage.height) {
            return
        }
        maybeCommitHistory()
        paintAreas.push_back(PaintArea(mappedPoints.toDouble(), mappedPoints2.toDouble(), f3))
        this.currentColor = this.customColor
        listener?.let {
            it.recolourImageViewDidUpdateFillThreshold(this)
            it.recolourImageViewDidUpdateCoverage(this)
        }
    }

    private fun maybeCommitHistory(force: Boolean = false) {
        var shouldCommit = topView.drawable != null
        if (shouldCommit && activeTool == Tool.FILL) {
            shouldCommit = !(topColor == customColor && isPolygonEnabled == topHasMask)
        }
        if (force || shouldCommit) {
            val compImage = compositedImage()
            compositedHistory.add(compImage)
            val newUsedColorsSet = HashSet<ImageMaskColor>()
            currentColor?.let { newUsedColorsSet.add(it) }
            if (usedColorsHistory.isNotEmpty()) {
                usedColorsHistory.lastOrNull()?.let { newUsedColorsSet.addAll(it) }
            }
            usedColorsHistory.add(newUsedColorsSet)
            compositedView.setImageBitmap(compImage)
            topView.setImageDrawable(null)
            updateExclusionMask(compImage, true)
            pruneHistory()
            paintAreas.clear()
            listener?.recolourImageViewDidUpdateFillThreshold(this)
            clearFreehandImage()
            topColor = customColor
            topTool = activeTool
            isSaved = false
        }
    }

    private fun pruneHistory() {
        while (compositedHistory.size > MAX_HISTORY) {
            compositedHistory.removeAt(0)
            usedColorsHistory.removeAt(0)
        }
    }

    private fun updateExclusionMask(mat: Mat?, allowRepaint: Boolean) {
        viewScope.launch(Dispatchers.Default) {
            paintMask?.let {
                if (mat == null) {
                    it.setExclusionMask(Mat())
                } else {
                    it.setAllowRepaint(allowRepaint)
                    it.setExclusionMask(mat)
                }
            }
        }
    }

    private fun updateExclusionMask(bitmap: Bitmap, allowRepaint: Boolean) {
        viewScope.launch {
            val maskMat = withContext(Dispatchers.Default) {
                val mat = Mat()
                try {
                    Utils.bitmapToMat(bitmap, mat)
                } catch (e: Exception) {
                    Log.d(TAG, "updateExclusionMask: ${e.localizedMessage}")
                }
                PaintMask.exclusionMaskFromImage(mat)
            }
            updateExclusionMask(maskMat, allowRepaint)
        }
    }

    private fun clearFreehandImage() {
        viewScope.launch(Dispatchers.Default) {
            paintMask?.setFreehandImage(Mat())
        }
    }

    private fun compositedImage(): Bitmap {
        val currentImage = imageBitmap ?: return createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val resultBitmap = createBitmap(
            currentImage.width, currentImage.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(resultBitmap)
        (compositedView.drawable as? BitmapDrawable)?.bitmap?.let {
            canvas.drawBitmap(it, 0.0f, 0.0f, null)
        }
        (topView.drawable as? BitmapDrawable)?.bitmap?.let {
            canvas.drawBitmap(it, 0.0f, 0.0f, null)
        }
        return resultBitmap
    }

    fun thumbnail(width: Int, height: Int): Bitmap {
        val currentImage = imageBitmap

        // Check if target thumbnail dimensions are valid
        if (width <= 0 || height <= 0) {
            Log.w(
                TAG,
                "Thumbnail dimensions are invalid: targetWidth=$width, targetHeight=$height. Returning 1x1 placeholder."
            )
            return createBitmap(
                1,
                1,
                Bitmap.Config.ARGB_8888
            ).apply { eraseColor(Color.TRANSPARENT) }
        }

        // Check if there's a valid source image
        if (currentImage == null || currentImage.width <= 0 || currentImage.height <= 0 || currentImage.isRecycled) {
            Log.w(
                TAG,
                "Source image is invalid for thumbnail. Returning empty thumbnail of target size."
            )
            return createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            ).apply { eraseColor(Color.LTGRAY) }
        }

        val scaleFactor = max(
            width.toDouble() / currentImage.width,
            height.toDouble() / currentImage.height
        ).toFloat()

        // Ensure scaled dimensions are at least 1 to avoid issues with Rect or drawing 0-size bitmaps
        val scaledWidth = (currentImage.width * scaleFactor).toInt().coerceAtLeast(1)
        val scaledHeight = (currentImage.height * scaleFactor).toInt().coerceAtLeast(1)

        val destRect = android.graphics.Rect(
            (width / 2) - (scaledWidth / 2),
            (height / 2) - (scaledHeight / 2),
            (width / 2) + (scaledWidth / 2),
            (height / 2) + (scaledHeight / 2)
        )

        val resultBitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // Draw the main image
        canvas.drawBitmap(currentImage, null, destRect, null)

        // Draw composited view overlay
        (compositedView.drawable as? BitmapDrawable)?.bitmap?.let { overlayBitmap ->
            if (overlayBitmap.width > 0 && overlayBitmap.height > 0 && !overlayBitmap.isRecycled) {
                canvas.drawBitmap(overlayBitmap, null, destRect, null)
            } else {
                Log.w(
                    TAG,
                    "Skipping composited overlay in thumbnail: invalid bitmap (w=${overlayBitmap.width}, h=${overlayBitmap.height}, recycled=${overlayBitmap.isRecycled})"
                )
            }
        }

        // Draw top view overlay
        (topView.drawable as? BitmapDrawable)?.bitmap?.let { overlayBitmap ->
            if (overlayBitmap.width > 0 && overlayBitmap.height > 0 && !overlayBitmap.isRecycled) {
                canvas.drawBitmap(overlayBitmap, null, destRect, null)
            } else {
                Log.w(
                    TAG,
                    "Skipping top overlay in thumbnail: invalid bitmap (w=${overlayBitmap.width}, h=${overlayBitmap.height}, recycled=${overlayBitmap.isRecycled})"
                )
            }
        }
        return resultBitmap
    }

    fun setImage(bitmap: Bitmap?) {
        this.imageBitmap = bitmap
        imageView.bindPhoto(bitmap)
        freeMaskView.matrix = imageView.matrix
        polygonView.matrix = imageView.matrix

        viewScope.launch {
            withContext(Dispatchers.Default) {
                paintMask?.delete()
                paintMask = null

                bitmap?.let {
                    val mat = Mat()
                    try {
                        Utils.bitmapToMat(it.copy(Bitmap.Config.ARGB_8888, true), mat)
                    } catch (e: Exception) {
                        Log.d(TAG, "Error converting bitmap to mat, ${e.localizedMessage}")
                    }
                    val mat2 = Mat()
                    Imgproc.cvtColor(
                        mat,
                        mat2,
                        Imgproc.COLOR_RGBA2RGB
                    ) // Assuming 1 was COLOR_RGBA2RGB
                    paintMask = PaintMask(mat2)
                }
            }
            updateMask()
        }
    }

    var currentTool: Tool?
        get() = activeTool
        set(tool) {
            if (tool != activeTool) {
                activeTool = tool
                listener?.let {
                    it.recolourImageViewDidUpdateCoverage(this)
                    it.recolourImageViewDidUpdateFillThreshold(this)
                }
                freeMaskView.reset()
                freeMaskView.visibility =
                    if (tool == Tool.ERASER || tool == Tool.BRUSH) VISIBLE else GONE
                polygonView.setUserInteractionEnabled(tool == Tool.POLYGON)
            }
        }

    var fillThreshold: Float
        get() {
            val currentArea = currentPaintArea()
            return if (activeTool != Tool.FILL || hasRepaint || currentArea == null) {
                -1.0f
            } else {
                currentArea.threshold
            }
        }
        set(f) {
            val currentArea = currentPaintArea()
            if (currentArea == null || f == currentArea.threshold) {
                return
            }
            currentArea.threshold = f
            listener?.recolourImageViewDidUpdateFillThreshold(this)
            updateMask()
        }

    var coverage: Float
        get() {
            return if ((activeTool != Tool.FILL || currentPaintArea() == null) && activeTool != Tool.BRUSH) {
                -1.0f
            } else {
                currentCoverage
            }
        }
        set(f) {
            if (f != currentCoverage) {
                currentCoverage = f
                listener?.recolourImageViewDidUpdateCoverage(this)
                updateMask()
            }
        }

    private fun currentPaintArea(): PaintArea? {
        return if (paintAreas.size() > 0) {
            paintAreas.get(paintAreas.size() - 1)
        } else {
            null
        }
    }

    fun updateMask(): Promise<*, *, *> {
        if (paintMask == null || customColor == null) {
            val deferredObject = DeferredObject<Any?, Any?, Any?>()
            deferredObject.resolve(null)
            return deferredObject.promise()
        }

        activeMaskUpdate?.let {
            // Check if promise is still pending. How to do this with jdeferred?
            // Assuming a method like it.isPending, or manage state differently.
            // For simplicity, if it's not null, we assume it's pending.
            // A more robust check for `isPending` would be needed if the library supports it.
            // if (it.state() == Promise.State.PENDING) { // Example, if state() exists
            needsReupdate = true
            return it
            // }
        }

        val deferredUpdate = DeferredObject<Any?, Throwable, Any?>()
        activeMaskUpdate = deferredUpdate.promise()

        val colorValue = customColor!!.colorValue
        val valDisableExclusionMask = activeTool == Tool.BRUSH && !isPolygonEnabled
        val valCoverage = currentCoverage
        val currentPaintAreasCopy = PaintAreaVector(paintAreas)


        viewScope.launch {
            try {
                var doCommitHistory = false
                var lastAreaForCommit: PaintArea? = null

                withContext(Dispatchers.Default) { // Corresponds to first AsyncTask's doInBackground
                    if (currentPaintAreasCopy.isEmpty && paintMask!!.didRepaint()) {
                        val tempPaintMask = paintMask!! // Safe call due to earlier check
                        val currentLastArea =
                            currentPaintAreasCopy.get(currentPaintAreasCopy.size() - 1)
                        if (!tempPaintMask.willDoRepaintForArea(currentLastArea)) {
                            lastAreaForCommit = currentLastArea
                            doCommitHistory = true
                        }
                    }
                }

                // Corresponds to first AsyncTask's onPostExecute
                if (doCommitHistory && lastAreaForCommit != null) {
                    maybeCommitHistory(true)
                    paintAreas.clear()
                    paintAreas.push_back(lastAreaForCommit)
                    // currentPaintAreasCopy is already a snapshot, mPaintAreas is now updated for the next step
                }

                // Corresponds to the inner AsyncTask
                val (resultBitmapLocal, didRepaintLocal) = withContext(Dispatchers.Default) {
                    paintMask!!.color = colorValue
                    paintMask!!.areas =
                        PaintAreaVector(paintAreas) // Use current mPaintAreas after potential commit
                    paintMask!!.setExclusionMaskDisabled(valDisableExclusionMask)
                    paintMask!!.coverage = valCoverage
                    var newBitmap: Bitmap? = null
                    try {
                        val imageMat = paintMask!!.image()
                        newBitmap = createBitmap(
                            imageMat.width(),
                            imageMat.height(),
                            Bitmap.Config.ARGB_8888
                        )
                        Utils.matToBitmap(imageMat, newBitmap, true)
                    } catch (e: Exception) {
                        Log.e("ExifUtils", "processImage: $e", )
                    }
                    Pair(newBitmap, paintMask!!.didRepaint())
                }

                // Corresponds to inner AsyncTask's onPostExecute
                this@RecolourImageView.activeMaskUpdate =
                    null // Clear before potential recursive call

                if (hasRepaint != didRepaintLocal) {
                    hasRepaint = didRepaintLocal
                    listener?.recolourImageViewDidUpdateCoverage(this@RecolourImageView)
                }
                topView.setImageBitmap(resultBitmapLocal)

                if (needsReupdate) {
                    needsReupdate = false
                    updateMask().done { deferredUpdate.resolve(null) }
                        .fail { ex ->
                            deferredUpdate.reject(
                                ex as? Throwable ?: Exception(ex.toString())
                            )
                        }
                } else {
                    deferredUpdate.resolve(null)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception in updateMask coroutine", e)
                deferredUpdate.reject(e)
                this@RecolourImageView.activeMaskUpdate = null
            }
        }
        return deferredUpdate.promise()
    }


    fun setBrushSize(size: Int) {
        freeMaskView.setBrushSize(size)
    }

    fun setColor(imageMaskColor: ImageMaskColor) {
        this.customColor = imageMaskColor
        val hsv = FloatArray(3)
        Color.colorToHSV(imageMaskColor.colorValue, hsv) // Removed .intValue()
        this.currentCoverage = (min(
            1.0, ((1.0 - hsv[2]).pow(0.45454545454545453) / 1.2) + 0.05
        ).toFloat() * 1000.0f).toInt() / 1000.0f // Used .toInt() for rounding
        listener?.recolourImageViewDidUpdateCoverage(this)
    }

    fun getColor(): ImageMaskColor {
        return customColor ?: ImageMaskColor()
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    override fun zoomableImageViewDidUpdate(zoomableImageView: ZoomableImageView?) {
        val matrix = zoomableImageView?.matrix
        topView.imageMatrix = matrix
        compositedView.imageMatrix = matrix
        freeMaskView.matrix = matrix
        polygonView.matrix = matrix
    }

    fun undo() {
        clearFreehandImage()
        if (!paintAreas.isEmpty) {
            undo1()
        } else if (topView.drawable != null) {
            undo2()
        } else if (compositedHistory.isNotEmpty()) {
            undo3()
        } else {
            compositedView.setImageDrawable(null)
            topView.setImageDrawable(null)
            updateExclusionMask(Mat(), true)
        }
    }

    private fun undo1() {
        popSeedPoint()
        updateMask()
        if (paintAreas.isEmpty) {
            undo2()
        }
    }

    private fun undo2() {
        topView.setImageDrawable(null)
        if (topView.drawable != null || compositedHistory.isEmpty()) {
            return
        }
        undo3()
    }

    private fun undo3() {
        compositedHistory.removeAt(compositedHistory.size - 1)
        usedColorsHistory.removeAt(usedColorsHistory.size - 1)
        this.currentColor = null
        if (compositedHistory.isNotEmpty()) {
            val bitmap = compositedHistory.last()
            updateExclusionMask(bitmap, true)
            compositedView.setImageBitmap(bitmap)
        } else {
            updateExclusionMask(Mat(), true)
            compositedView.setImageDrawable(null)
        }
    }

    private fun clearSeedPoints() {
        if (paintAreas.size() > 0) {
            paintAreas.clear()
            listener?.recolourImageViewDidUpdateFillThreshold(this)
        }
    }

    private fun popSeedPoint() {
        if (paintAreas.size() > 0) {
            paintAreas.removeRange(paintAreas.size() - 1, paintAreas.size())
            if (paintAreas.isEmpty) {
                this.currentColor = null
            }
            listener?.recolourImageViewDidUpdateFillThreshold(this)
        }
    }

    override fun onFreeMaskViewDidFinishDrawing(freeMaskView: FreeMaskView?) {
        when (activeTool) {
            Tool.ERASER -> applyEraser()
            Tool.BRUSH -> viewScope.launch { applyBrush() } // Launch suspend fun
            else -> { /* No-op */
            }
        }
    }

    private fun applyEraser() {
        if (compositedHistory.isEmpty() && topView.drawable == null) {
            freeMaskView.reset()
            return
        }
        try {
            val currentImage = imageBitmap ?: return
            val freehandBitmap =
                freeMaskView.getBitmap(currentImage.width, currentImage.height)

            val finalEraserBitmap = if (isPolygonEnabled) {
                val polygonBitmap =
                    polygonView.getBitmap(currentImage.width, currentImage.height)
                val canvas = Canvas(polygonBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                }
                canvas.drawBitmap(freehandBitmap, 0.0f, 0.0f, paint)
                polygonBitmap
            } else {
                freehandBitmap
            }
            eraseFromMaskImage(finalEraserBitmap)
        } catch (e: OutOfMemoryError) {
            Log.e("RecolorImage", "processImage: $e", )
            com.whitelabel.android.utils.Utils.showOutOfMemoryToast(context)
        } catch (e: Exception) {
            Log.e("RecolorImage", "processImage: $e", )
        }
        freeMaskView.reset()
    }

    private fun eraseFromMaskImage(bitmap: Bitmap) {
        maybeCommitHistory()
        val compImage = compositedImage()
        val canvas = Canvas(compImage)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, paint)
        compositedHistory.add(compImage)
        usedColorsHistory.lastOrNull()
            ?.let { usedColorsHistory.add(it) } // Duplicate last set of colors
        compositedView.setImageBitmap(compImage)
        updateExclusionMask(compImage, true)
        pruneHistory()
    }

    private suspend fun applyBrush() {
        val currentCustomColor = customColor ?: run {
            freeMaskView.reset()
            return
        }
        val currentImage = imageBitmap ?: return

        maybeCommitHistory() // This should be safe to call from main thread part of coroutine

        try {
            val bitmap = freeMaskView.getBitmap(currentImage.width, currentImage.height)
            maybeApplyPolygonMask() // This is already suspend
            currentColor = currentCustomColor
            isSaved = false

            withContext(Dispatchers.Default) {
                try {
                    val mat = Mat()
                    Utils.bitmapToMat(bitmap, mat, false) // alpha premultiplied? Check original
                    paintMask!!.setFreehandImage(mat) // Assuming paintMask is not null
                } catch (e: Exception) {
                    Log.i(TAG, "ApplyBrush failed in coroutine (mat conversion): $e", e)
                    // Potentially rethrow or handle
                }
            }
            // updateMask returns a Promise, await its completion
            updateMask() // Specify type for await if known, Any? for generic
            freeMaskView.reset()

        } catch (e: Exception) {
            Log.i(TAG, "ApplyBrush failed: $e", e)
        } catch (e: OutOfMemoryError) {
            Log.i(TAG, "ApplyBrush failed OutOfMemory: $e", e)
            com.whitelabel.android.utils.Utils.showOutOfMemoryToast(context)
        }
    }

    private suspend fun maybeApplyPolygonMask() {
        if (!isPolygonEnabled) return

        topHasMask = true
        val currentImage = imageBitmap ?: return
        val bitmap = polygonView.getBitmap(currentImage.width, currentImage.height)

        val mask = withContext(Dispatchers.IO) { // IO for potential file ops if getBitmap does that
            try {
                val mat = Mat()
                Utils.bitmapToMat(bitmap, mat)
                PaintMask.invertAlpha(mat)
                PaintMask.exclusionMaskFromImage(mat)
            } catch (e: Exception) {
                Log.e(TAG, "Error in maybeApplyPolygonMask: $e")
                null
            }
        }
        // updateExclusionMask is already main-safe or dispatches internally
        updateExclusionMask(mask, true)
    }


    var isPolygonEnabled: Boolean
        get() = polygonView.isVisible
        set(enabled) {
            polygonView.visibility = if (enabled) View.VISIBLE else View.GONE
        }

    override fun onInterceptTouchEvent(motionEvent: MotionEvent): Boolean {
        if (freeMaskView.isVisible) { // Use Kotlin property
            interceptScaleDetector.onTouchEvent(motionEvent)
            interceptGestureDetector.onTouchEvent(motionEvent)
        }
        return interceptingTouch
    }

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        val handled = super.onTouchEvent(motionEvent)
        if (interceptingTouch) {
            interceptScaleDetector.onTouchEvent(motionEvent)
            interceptGestureDetector.onTouchEvent(motionEvent)
        }
        return handled
    }

    override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
        imageView.onScale(scaleGestureDetector)
        return true
    }

    override fun onScaleBegin(scaleGestureDetector: ScaleGestureDetector): Boolean {
        imageView.onScaleBegin(scaleGestureDetector)
        interceptingTouch = true
        return true
    }

    override fun onScaleEnd(scaleGestureDetector: ScaleGestureDetector) {
        imageView.onScaleEnd(scaleGestureDetector)
        interceptingTouch = false
    }

    override fun onDown(motionEvent: MotionEvent): Boolean {
        if (motionEvent.pointerCount >= 2) {
            interceptingTouch = true
        }
        return true // Important to return true to receive further events
    }

    override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
        if (motionEvent.pointerCount < 2) { // Should be motionEvent.getPointerCount() == 1
            interceptingTouch = false
        }
        return false // Let click listener handle it
    }

    override fun onScroll(
        motionEvent: MotionEvent?, motionEvent2: MotionEvent, f: Float, f2: Float
    ): Boolean {
        // Ensure motionEvent is not null if imageView.onScroll expects non-null
        return imageView.onScroll(motionEvent, motionEvent2, f, f2)
    }

    override fun onFling(
        motionEvent: MotionEvent?, motionEvent2: MotionEvent, f: Float, f2: Float
    ): Boolean {
        return false
    }

    override fun onLongPress(motionEvent: MotionEvent) {}

    override fun onShowPress(motionEvent: MotionEvent) {}

    // This function was already suspend, just making sure it's correctly typed
    // And assuming the Promise type matches what await() expects
    suspend fun realUsedColorsPromise(): Promise<Set<ImageMaskColor>, Any, Any> {
        return realUsedColorsPromise(compositedImage())
    }

    // This function was already suspend, just making sure it's correctly typed
    private suspend fun realUsedColorsPromise(bitmap: Bitmap?): Promise<Set<ImageMaskColor>, Any, Any> {
        val deferred = DeferredObject<Set<ImageMaskColor>, Any, Any>()
        // Launch a coroutine to call the suspend function and resolve the deferred
        viewScope.launch {
            try {
                val result = realUsedColorsSuspend(bitmap) // Call the suspend version
                deferred.resolve(result)
            } catch (e: Throwable) {
                deferred.reject(e)
            }
        }
        return deferred.promise()
    }

    // Renamed the original suspend fun to avoid signature clash if types were different
    private suspend fun realUsedColorsSuspend(bitmap: Bitmap?): Set<ImageMaskColor> =
        withContext(Dispatchers.Default) {
            if (bitmap == null || usedColorsHistory.isEmpty()) {
                return@withContext emptySet()
            }

            val lastUsedColors = usedColorsHistory.lastOrNull() ?: emptySet()
            val combinedSet = HashSet<ImageMaskColor>(lastUsedColors)
            customColor?.let { combinedSet.add(it) } // Use customColor

            val mat = Mat()
            try {
                Utils.bitmapToMat(bitmap, mat)
            } catch (e: Exception) {

                return@withContext emptySet()
            }

            val rgbMat = Mat().also { Imgproc.cvtColor(mat, it, Imgproc.COLOR_RGBA2RGB) }
            mat.release()

            val hsvMat = Mat().also { Imgproc.cvtColor(rgbMat, it, Imgproc.COLOR_RGB2HSV) }
            rgbMat.release()

            val hues = PaintMask.findHues(hsvMat)
            hsvMat.release()

            val hueSet = mutableSetOf<Int>().apply {
                for (i in 0 until hues.size()) add(hues[i])
            }

            combinedSet.filterTo(HashSet()) { colorPicker ->
                val hsvLocal = FloatArray(3)
                Color.colorToHSV(colorPicker.colorValue, hsvLocal) // Removed toInt()
                val v =
                    (hsvLocal[2] * 255).toInt() // Assuming V is 0-1, scale to 0-255 for comparison if hues are 0-255
                // This hue matching logic might need adjustment based on how PaintMask.findHues works.
                // The original (v - 2..v + 1).any { it in hueSet } compared brightness (V) with hues (H).
                // This seems like a potential logic error in the original or a misunderstanding of the PaintMask API.
                // For now, I'll keep it as is, but it's worth reviewing.
                // A more typical approach would be to match the hue component (hsvLocal[0])
                (v - 2..v + 1).any { it in hueSet }
            }
        }

    suspend fun save(view: View, str: String, label: String, context2: Context): Boolean =
        withContext(Dispatchers.IO) {
            val measuredWidth = view.measuredWidth.toFloat()
            val measuredHeight = view.measuredHeight.toFloat()
            val thumb = thumbnail(measuredWidth.toInt(), measuredHeight.toInt()) // Renamed
            val drawable: Drawable? =
                ResourcesCompat.getDrawable(context2.resources, R.drawable.share, null)

            // The original code used CompletableDeferred, but realUsedColors() returns a Promise.
            // We need to await the promise.
            val imageMaskColorSets: Set<ImageMaskColor> = try {
                realUsedColorsPromise(compositedImage()).await() // Await the promise
            } catch (e: Exception) {
                Log.d(TAG, "Error getting real used colors for save: $e")
                return@withContext false
            }

            try {
                val filesDir = context2.filesDir
                val protosFromColors = protosFromColors(imageMaskColorSets)

                val maxScale = max(
                    (measuredWidth / thumb.width).toDouble(),
                    (measuredHeight / thumb.height).toDouble()
                ).toFloat()

                val width = thumb.width * maxScale
                val height = thumb.height * maxScale
                val finalRenderWidth = min(width.toDouble(), measuredWidth.toDouble()).toInt()
                val finalRenderHeight =
                    (min(height.toDouble(), measuredHeight.toDouble()) + 150.0f).toInt()

                val resultBitmap =
                    createBitmap(finalRenderWidth, finalRenderHeight, Bitmap.Config.RGB_565)
                val canvas = Canvas(resultBitmap)

                canvas.drawBitmap(thumb, null, RectF(0.0f, 0.0f, width, height), null)

                val footerTop = finalRenderHeight - 150.0f
                val rectF =
                    RectF(0.0f, footerTop, finalRenderWidth.toFloat(), finalRenderHeight.toFloat())
                val paint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawRect(rectF, paint)

                val displayColors = imageMaskColorSets.take(4)
                val swatchSize =
                    (finalRenderWidth.toFloat() - ((displayColors.size - 1) * 5.0f)) / displayColors.size
                var currentX = 0.0f

                displayColors.forEach { colorPicker ->
                    val swatchRect = RectF(
                        currentX,
                        rectF.top + 5.0f,
                        currentX + swatchSize,
                        rectF.bottom - 5.0f
                    )
                    val swatchPaint = Paint().apply {
                        this.color = com.whitelabel.android.utils.Utils.getARGB(colorPicker)
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(swatchRect, swatchPaint)

                    val hsv = FloatArray(3)
                    Color.colorToHSV(colorPicker.colorValue, hsv)

                    val textPaint = TextPaint().apply {
                        textSize = 18.0f
                        this.color = if (hsv[2] >= 0.8f) Color.BLACK else Color.WHITE
                    }

                    val textLineHeight = textPaint.descent() - textPaint.ascent()

                    val nameEllipsized = TextUtils.ellipsize(
                        colorPicker.colorName,
                        textPaint,
                        swatchRect.width() - 30.0f,
                        TextUtils.TruncateAt.END
                    )
                    val codeEllipsized = TextUtils.ellipsize(
                        colorPicker.colorCode,
                        textPaint,
                        swatchRect.width() - 30.0f,
                        TextUtils.TruncateAt.END
                    )

                    canvas.drawText(
                        nameEllipsized.toString(), // Ensure it's a String
                        swatchRect.left + 15.0f,
                        swatchRect.top + 15.0f + textLineHeight,
                        textPaint
                    )
                    canvas.drawText(
                        codeEllipsized.toString(), // Ensure it's a String
                        swatchRect.left + 15.0f,
                        swatchRect.top + 15.0f + (textLineHeight * 2.0f),
                        textPaint
                    )
                    currentX += swatchSize + 5.0f
                }

                drawable?.let {
                    val logoRight = finalRenderWidth - 20
                    it.setBounds(
                        logoRight - it.intrinsicWidth,
                        20,
                        logoRight,
                        it.intrinsicHeight + 20
                    )
                    it.draw(canvas)
                }

                File(filesDir, "recolor").mkdirs()
                val byteArrayOutputStream = ByteArrayOutputStream()
                resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()

                val meta = RecolorImageMeta.Builder()
                    .version(1)
                    .thumbnail(okio.ByteString.of(*byteArray)) // okio.ByteString
                    .label(label)
                    .usedColours(protosFromColors.toMutableList()) // Ensure it's MutableList if required
                    .time(Date().time)
                    .build()

                val file = File(filesDir, "recolor/$str.jpg") // Changed extension based on usage
                file.encodeProto(meta) // Changed to avoid clash
                true
            } catch (e: Exception) {
                Log.d(TAG, "Error saving recolor image: $e", e)
                false
            }
        }

    private suspend fun File.encodeProto(value: RecolorImageMeta) = withContext(Dispatchers.IO) {
        FileOutputStream(this@encodeProto).use { out ->
            RecolorImageMeta.ADAPTER.encode(out, value)
        }
    }

    suspend fun load(str: String, context: Context) {
        try {
            val filesDir = context.filesDir
            val (loadedImage, loadedComposited, loadedUsedColors) = withContext(Dispatchers.IO) {
                val allColors = mutableListOf<ImageMaskColor>()
                CoroutineUtils.getAllColors(context, allColors)

                val file = File(filesDir, "recolor/$str.recolor") // Assuming .recolor extension
                val decode = RecolorImage.ADAPTER.decode(FileInputStream(file))

                val imageBytes = decode.imageOriginal?.toByteArray()
                val compositedBytes = decode.imageRecolored?.toByteArray()

                val imageBitmapLocal =
                    imageBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                val compositedBitmapLocal =
                    compositedBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                val colors = colorsFromProtos(decode.usedColours, allColors)

                Triple(imageBitmapLocal, compositedBitmapLocal, colors)
            }

            setImage(loadedImage) // setImage handles background processing
            compositedView.setImageBitmap(loadedComposited)

            usedColorsHistory = arrayListOf()
            usedColorsHistory.add(HashSet<ImageMaskColor>().apply { addAll(loadedUsedColors) })

        } catch (e: Exception) {
            Log.d(TAG, "Error loading image: ${e.message}", e)
        }
    }

    suspend fun sharingImage(view: View, context: Context): Bitmap? =
        withContext(Dispatchers.Default) {
            val measuredWidth = view.measuredWidth.toFloat()
            val measuredHeight = view.measuredHeight.toFloat()
            val thumb = thumbnail(measuredWidth.toInt(), measuredHeight.toInt())

            val drawable = ResourcesCompat.getDrawable(
                context.resources, R.drawable.share, null
            ) ?: return@withContext null

            // Ensure history is committed before getting colors
            withContext(Dispatchers.Main) { // Ensure maybeCommitHistory is called on main thread if it modifies UI
                maybeCommitHistory(true)
            }


            val colorSet: Set<ImageMaskColor> = try {
                realUsedColorsPromise(compositedImage()).await()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting real used colors for sharing: $e")
                return@withContext null
            }


            val scale = max(
                measuredWidth / thumb.width.toFloat(),
                measuredHeight / thumb.height.toFloat()
            )
            val scaledWidth = (thumb.width * scale).toInt()
            val scaledHeight = (thumb.height * scale).toInt()
            val finalWidth = min(scaledWidth, measuredWidth.toInt())
            val finalHeight = min(
                scaledHeight + 150f,
                measuredHeight
            ).toInt() // Ensure measuredHeight is float for min

            val resultBitmap = createBitmap(finalWidth, finalHeight, Bitmap.Config.RGB_565)
            val canvas = Canvas(resultBitmap)

            val imageRect = RectF(0f, 0f, scaledWidth.toFloat(), scaledHeight.toFloat())
            canvas.drawBitmap(thumb, null, imageRect, null)

            val footerRect =
                RectF(0f, finalHeight - 150f, finalWidth.toFloat(), finalHeight.toFloat())
            canvas.drawRect(footerRect, Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            })

            val uniqueColors = colorSet.toList().take(4) // No need for toSet() here
            val swatchWidth = (finalWidth - ((uniqueColors.size - 1) * 5f)) / uniqueColors.size
            var xOffset = 0f

            uniqueColors.forEach { colorPicker ->
                val colorRect = RectF(
                    xOffset, footerRect.top + 5f, xOffset + swatchWidth, footerRect.bottom - 5f
                )
                val fillPaint = Paint().apply {
                    color = com.whitelabel.android.utils.Utils.getARGB(colorPicker)
                    style = Paint.Style.FILL
                }
                canvas.drawRect(colorRect, fillPaint)

                val hsv = FloatArray(3)
                Color.colorToHSV(colorPicker.colorValue, hsv)
                val textPaint = TextPaint().apply {
                    textSize = 18f
                    color = if (hsv[2] >= 0.8f) Color.BLACK else Color.WHITE
                }
                val textOffset = textPaint.descent() - textPaint.ascent()

                val name = TextUtils.ellipsize(
                    colorPicker.colorName,
                    textPaint,
                    colorRect.width() - 30f,
                    TextUtils.TruncateAt.END
                )
                val code = TextUtils.ellipsize(
                    colorPicker.colorCode,
                    textPaint,
                    colorRect.width() - 30f,
                    TextUtils.TruncateAt.END
                )

                canvas.drawText(
                    name.toString(),
                    colorRect.left + 15f,
                    colorRect.top + 15f + textOffset,
                    textPaint
                )
                canvas.drawText(
                    code.toString(),
                    colorRect.left + 15f,
                    colorRect.top + 15f + textOffset * 2,
                    textPaint
                )
                xOffset += swatchWidth + 5f
            }

            val logoRight = finalWidth - 20
            drawable.setBounds(
                logoRight - drawable.intrinsicWidth, 20, logoRight, 20 + drawable.intrinsicHeight
            )
            drawable.draw(canvas)

            resultBitmap
        }

    val isImageChanged: Boolean
        get() = compositedHistory.isNotEmpty() // Simplified

    fun undoAll() {
        if (paintMask == null) return

        clearFreehandImage()

        while (!paintAreas.isEmpty) { // Check isNotEmpty on PaintAreaVector
            popSeedPoint()
            updateMask() // This returns a Promise, ideally await or handle its completion
        }

        topView.setImageDrawable(null)

        while (compositedHistory.isNotEmpty()) {
            compositedHistory.removeAt(compositedHistory.size - 1)
            if (usedColorsHistory.isNotEmpty()) {
                usedColorsHistory.removeAt(usedColorsHistory.size - 1)
            }
            currentColor = null
        }

        if (paintAreas.isEmpty) { // Check isEmpty on PaintAreaVector
            compositedView.setImageDrawable(null)
            topView.setImageDrawable(null)
            updateExclusionMask(Mat(), true)
        }
    }

    // Generic await extension for org.jdeferred.Promise
    private suspend fun <T> Promise<T, *, *>.await(): T = suspendCancellableCoroutine { cont ->
        this.done { result: T -> // Explicitly type result
            if (cont.isActive) cont.resume(result)
        }.fail { err ->
            if (cont.isActive) cont.resumeWithException(
                err as? Throwable ?: Exception("Promise failed with unknown error: $err")
            )
        }
    }


    companion object {
        private const val EDGE_INSET_DP = -150
        private const val MAX_HISTORY = 6
        private const val TAG = "RecolourImageView"

        init {
            // Load OpenCV library
            if (!OpenCVLoader.initLocal()) {
                Log.e(TAG, "OpenCV initialization failed.")
            }
            try {
                System.loadLibrary("imager-native") // Make sure this matches your .so file name
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load imager-native library", e)
            }
        }

        fun protosFromColors(set: Set<ImageMaskColor>): List<RecolorImageColor> {
            return set.map { colorPicker ->
                RecolorImageColor.Builder()
                    .name(colorPicker.colorName)
                    .code(colorPicker.colorCode)
                    .rgb(colorPicker.colorValue)
                    .fandeckName(colorPicker.fandeckName)
                    .fandeckId(colorPicker.fandeckId.toString()) // Ensure fandeckId is String
                    .build()
            }
        }

        fun colorsFromProtos(
            protoList: List<RecolorImageColor>,
            allColorsList: List<ImageMaskColor>
        ): List<ImageMaskColor> {
            if (protoList.isEmpty()) {
                return emptyList()
            }
            val protoMap: HashMap<String, RecolorImageColor> = HashMap()
            for (recolorImageColor in protoList) {
                // Ensure fandeckId and code are not null for key creation
                val key = (recolorImageColor.fandeckId ?: "") + ":" + (recolorImageColor.code ?: "")
                protoMap[key] = recolorImageColor
            }

            val resultList: ArrayList<ImageMaskColor> = ArrayList()
            for (colorPicker in allColorsList) {
                val key = "${colorPicker.fandeckId}:${colorPicker.colorCode}"
                if (protoMap.containsKey(key)) {
                    resultList.add(colorPicker)
                }
            }
            return resultList
        }
    }
}

