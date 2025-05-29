package com.whitelabel.android.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.View
import android.view.ViewConfiguration
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ScaleGestureDetectorCompat
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class ZoomableImageView : View, GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener,
    OnScaleGestureListener {
    private var mDoubleTapDebounce = false
    private var mDoubleTapOccurred = false
    private val mDoubleTapToZoomEnabled: Boolean
    private var mDownFocusX = 0f
    private var mDownFocusY = 0f
    private var mDrawMatrix: Matrix? = null
    var drawable: Drawable? = null
        private set
    private var mEdgeInset: Int
    private var mExternalClickListener: OnClickListener? = null
    private var mFixedHeight: Int
    private var mGestureDetector: GestureDetector? = null
    private var mHaveLayout = false
    private var mIsDoubleTouch = false
    private var mListener: Listener? = null
    private val mMatrix: Matrix
    private var mMaxInitialScaleFactor = 0f
    private var mMaxScale = 0f
    private var mMinScale = 0f
    private val mOriginalMatrix: Matrix
    private var mQuickScaleEnabled = false
    private var mRotateRunnable: RotateRunnable? = null
    private var mRotation = 0f
    private var mScaleGetureDetector: ScaleGestureDetector? = null
    private var mScaleRunnable: ScaleRunnable? = null
    private var mScrollMinNumberOfPointers: Int
    private var mSnapRunnable: SnapRunnable? = null
    private val mTempDst: RectF
    private val mTempSrc: RectF
    private var mTransformsEnabled = false
    private val mTranslateRect: RectF
    private var mTranslateRunnable: TranslateRunnable? = null
    private val mValues: FloatArray

    /* loaded from: /storage/emulated/0/Documents/jadec/sources/com.whitelabel.upgcolor/dex-files/1.dex */
    interface Listener {
        fun zoomableImageViewDidUpdate(zoomableImageView: ZoomableImageView?)
    }

    /* loaded from: /storage/emulated/0/Documents/jadec/sources/com.whitelabel.upgcolor/dex-files/1.dex */
    interface OnClickListener {
        fun onZoomableImageViewClick(motionEvent: MotionEvent?)

        fun onZoomableImageViewLongPress(motionEvent: MotionEvent?)
    }

    // android.view.GestureDetector.OnGestureListener
    override fun onShowPress(motionEvent: MotionEvent) {
    }

    // android.view.GestureDetector.OnGestureListener
    override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
        return false
    }

    constructor(context: Context?) : super(context) {
        this.mMatrix = Matrix()
        this.mOriginalMatrix = Matrix()
        this.mFixedHeight = -1
        this.mDoubleTapToZoomEnabled = false
        this.mScrollMinNumberOfPointers = 1
        this.mTempSrc = RectF()
        this.mTempDst = RectF()
        this.mTranslateRect = RectF()
        this.mValues = FloatArray(9)
        this.mEdgeInset = 0
        initialize()
    }

    constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet) {
        this.mMatrix = Matrix()
        this.mOriginalMatrix = Matrix()
        this.mFixedHeight = -1
        this.mDoubleTapToZoomEnabled = false
        this.mScrollMinNumberOfPointers = 1
        this.mTempSrc = RectF()
        this.mTempDst = RectF()
        this.mTranslateRect = RectF()
        this.mValues = FloatArray(9)
        this.mEdgeInset = 0
        initialize()
    }

    constructor(context: Context?, attributeSet: AttributeSet?, i: Int) : super(
        context,
        attributeSet,
        i
    ) {
        this.mMatrix = Matrix()
        this.mOriginalMatrix = Matrix()
        this.mFixedHeight = -1
        this.mDoubleTapToZoomEnabled = false
        this.mScrollMinNumberOfPointers = 1
        this.mTempSrc = RectF()
        this.mTempDst = RectF()
        this.mTranslateRect = RectF()
        this.mValues = FloatArray(9)
        this.mEdgeInset = 0
        initialize()
    }

    // android.view.View
    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        val scaleGestureDetector = this.mScaleGetureDetector
        if (scaleGestureDetector == null || this.mGestureDetector == null) {
            return false
        }
        scaleGestureDetector.onTouchEvent(motionEvent)
        mGestureDetector!!.onTouchEvent(motionEvent)
        val action = motionEvent.action
        if ((action == 1 || action == 3) && !mTranslateRunnable!!.mRunning) {
            snap()
        }
        return true
    }

    // android.view.GestureDetector.OnDoubleTapListener
    override fun onDoubleTap(motionEvent: MotionEvent): Boolean {
        this.mDoubleTapOccurred = true
        if (this.mQuickScaleEnabled) {
            return false
        }
        return scale(motionEvent)
    }

    // android.view.GestureDetector.OnDoubleTapListener
    override fun onDoubleTapEvent(motionEvent: MotionEvent): Boolean {
        val action = motionEvent.action
        if (action == 0) {
            if (this.mQuickScaleEnabled) {
                this.mDownFocusX = motionEvent.x
                this.mDownFocusY = motionEvent.y
                return false
            }
            return false
        } else if (action == 1) {
            if (this.mQuickScaleEnabled) {
                return scale(motionEvent)
            }
            return false
        } else if (action == 2 && this.mQuickScaleEnabled && this.mDoubleTapOccurred) {
            val x = (motionEvent.x - this.mDownFocusX).toInt()
            val y = (motionEvent.y - this.mDownFocusY).toInt()
            if ((x * x) + (y * y) > sTouchSlopSquare) {
                this.mDoubleTapOccurred = false
                return false
            }
            return false
        } else {
            return false
        }
    }

    private fun scale(motionEvent: MotionEvent): Boolean {
        val z: Boolean
        val min: Float
        val min2: Float
        val f: Float
        if (this.mDoubleTapToZoomEnabled && this.mTransformsEnabled && this.mDoubleTapOccurred) {
            if (this.mDoubleTapDebounce) {
                z = false
            } else {
                val scale = scale
                var f2 = this.mMinScale
                if (scale > f2) {
                    val f3 = f2 / scale
                    val f4 = 1.0f - f3
                    f = ((width.toFloat() / 2) - (mTranslateRect.centerX() * f3)) / f4
                    min2 = ((height.toFloat() / 2) - (f3 * mTranslateRect.centerY())) / f4
                } else {
                    f2 = min(
                        mMaxScale.toDouble(),
                        max(f2.toDouble(), (scale * DOUBLE_TAP_SCALE_FACTOR).toDouble())
                    ).toFloat()
                    val f5 = f2 / scale
                    val width = (width - mTranslateRect.width()) / f5
                    val height = (height - mTranslateRect.height()) / f5
                    min =
                        if (mTranslateRect.width() <= width * DOUBLE_TAP_SCALE_FACTOR) {
                            mTranslateRect.centerX()
                        } else {
                            min(
                                max(
                                    (mTranslateRect.left + width).toDouble(),
                                    motionEvent.x.toDouble()
                                ), (mTranslateRect.right - width).toDouble()
                            ).toFloat()
                        }
                    min2 =
                        if (mTranslateRect.height() <= DOUBLE_TAP_SCALE_FACTOR * height) {
                            mTranslateRect.centerY()
                        } else {
                            min(
                                max(
                                    (mTranslateRect.top + height).toDouble(),
                                    motionEvent.y.toDouble()
                                ), (mTranslateRect.bottom - height).toDouble()
                            ).toFloat()
                        }
                    f = min
                }
                mScaleRunnable!!.start(scale, f2, f, min2)
                z = true
            }
            this.mDoubleTapDebounce = false
        } else {
            z = false
        }
        this.mDoubleTapOccurred = false
        return z
    }

    // android.view.GestureDetector.OnDoubleTapListener
    override fun onSingleTapConfirmed(motionEvent: MotionEvent): Boolean {
        val onClickListener = this.mExternalClickListener
        if (onClickListener != null && !this.mIsDoubleTouch) {
            onClickListener.onZoomableImageViewClick(motionEvent)
        }
        this.mIsDoubleTouch = false
        return true
    }

    // android.view.GestureDetector.OnGestureListener
    override fun onLongPress(motionEvent: MotionEvent) {
        val onClickListener = this.mExternalClickListener
        onClickListener?.onZoomableImageViewLongPress(motionEvent)
    }

    // android.view.GestureDetector.OnGestureListener
    override fun onScroll(
        motionEvent: MotionEvent?,
        motionEvent2: MotionEvent,
        f: Float,
        f2: Float
    ): Boolean {
        if (!this.mTransformsEnabled || mScaleRunnable!!.mRunning || motionEvent2.pointerCount < this.mScrollMinNumberOfPointers) {
            return false
        }
        translate(-f, -f2)
        return true
    }

    // android.view.GestureDetector.OnGestureListener
    override fun onDown(motionEvent: MotionEvent): Boolean {
        if (this.mTransformsEnabled) {
            mTranslateRunnable!!.stop()
            mSnapRunnable!!.stop()
            return true
        }
        return true
    }

    // android.view.GestureDetector.OnGestureListener
    override fun onFling(
        motionEvent: MotionEvent?,
        motionEvent2: MotionEvent,
        f: Float,
        f2: Float
    ): Boolean {
        if (!this.mTransformsEnabled || mScaleRunnable!!.mRunning || motionEvent2.pointerCount < this.mScrollMinNumberOfPointers) {
            return false
        }
        mTranslateRunnable!!.start(f, f2)
        return true
    }

    // android.view.ScaleGestureDetector.OnScaleGestureListener
    override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
        if (!this.mTransformsEnabled || mScaleRunnable!!.mRunning) {
            return true
        }
        this.mIsDoubleTouch = false
        scale(
            scale * scaleGestureDetector.scaleFactor,
            scaleGestureDetector.focusX,
            scaleGestureDetector.focusY
        )
        return true
    }

    // android.view.ScaleGestureDetector.OnScaleGestureListener
    override fun onScaleBegin(scaleGestureDetector: ScaleGestureDetector): Boolean {
        if (this.mTransformsEnabled && !mScaleRunnable!!.mRunning) {
            mScaleRunnable!!.stop()
            this.mIsDoubleTouch = true
        }
        return true
    }

    // android.view.ScaleGestureDetector.OnScaleGestureListener
    override fun onScaleEnd(scaleGestureDetector: ScaleGestureDetector) {
        if (this.mTransformsEnabled && this.mIsDoubleTouch) {
            this.mDoubleTapDebounce = true
            resetTransformations()
        }
    }

    fun setOnClickListener(onClickListener: OnClickListener?) {
        this.mExternalClickListener = onClickListener
    }

    fun clear() {
        this.mGestureDetector = null
        this.mScaleGetureDetector = null
        this.drawable = null
        mScaleRunnable!!.stop()
        this.mScaleRunnable = null
        mTranslateRunnable!!.stop()
        this.mTranslateRunnable = null
        mSnapRunnable!!.stop()
        this.mSnapRunnable = null
        mRotateRunnable!!.stop()
        this.mRotateRunnable = null
        this.mExternalClickListener = null
        this.mDoubleTapOccurred = false
    }

    fun bindDrawable(drawable: Drawable?) {
        val z: Boolean
        var drawable2: Drawable? =null
        if (drawable == null || drawable === (this.drawable.also { drawable2 = it })) {
            z = false
        } else {
            if (drawable2 != null) {
                drawable2!!.callback = null
            }
            this.drawable = drawable
            this.mMinScale = 0.0f
            drawable.callback = this
            z = true
        }
        configureBounds(z)
        invalidate()
    }

    fun bindPhoto(bitmap: Bitmap?) {
        val drawable = this.drawable
        val z = drawable is BitmapDrawable
        var z2 = !z
        if (drawable != null && z) {
            if (bitmap == (drawable as BitmapDrawable).bitmap) {
                return
            }
            z2 =
                bitmap != null && (this.drawable!!.intrinsicWidth != bitmap.width || this.drawable!!.intrinsicHeight != bitmap.height)
            this.mMinScale = 0.0f
            this.drawable = null
        }
        if (this.drawable == null && bitmap != null) {
            this.drawable = bitmap.toDrawable(resources)
        }
        configureBounds(z2)
        invalidate()
    }

    val photo: Bitmap?
        get() {
            val drawable = drawable as? BitmapDrawable ?: return null
            return drawable.bitmap
        }

    val isPhotoBound: Boolean
        get() = this.drawable != null

    fun resetTransformations() {
        mMatrix.set(this.mOriginalMatrix)
        invalidate()
        val listener = this.mListener
        listener?.zoomableImageViewDidUpdate(this)
    }

    fun rotateClockwise() {
        rotate(90.0f, true)
    }

    fun rotateCounterClockwise() {
        rotate(-90.0f, true)
    }

    // android.view.View
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (this.drawable != null) {
            val saveCount = canvas.saveCount
            canvas.save()
            val matrix = this.mDrawMatrix
            if (matrix != null) {
                canvas.concat(matrix)
            }
            drawable!!.draw(canvas)
            canvas.restoreToCount(saveCount)
            mTranslateRect.set(drawable!!.bounds)
            val matrix2 = this.mDrawMatrix
            matrix2?.mapRect(this.mTranslateRect)
        }
    }

    // android.view.View
    override fun onLayout(z: Boolean, i: Int, i2: Int, i3: Int, i4: Int) {
        super.onLayout(z, i, i2, i3, i4)
        this.mHaveLayout = true
        configureBounds(z)
    }

    // android.view.View
    override fun onMeasure(i: Int, i2: Int) {
        val i3 = this.mFixedHeight
        if (i3 >= 0) {
            super.onMeasure(i, MeasureSpec.makeMeasureSpec(i3, MeasureSpec.AT_MOST))
            setMeasuredDimension(measuredWidth, this.mFixedHeight)
            return
        }
        super.onMeasure(i, i2)
    }

    // android.view.View
    public override fun verifyDrawable(drawable: Drawable): Boolean {
        return this.drawable === drawable || super.verifyDrawable(drawable)
    }

    // android.view.View, android.graphics.drawable.Drawable.Callback
    override fun invalidateDrawable(drawable: Drawable) {
        if (this.drawable === drawable) {
            invalidate()
        } else {
            super.invalidateDrawable(drawable)
        }
    }

    fun setFixedHeight(i: Int) {
        val z = i != this.mFixedHeight
        this.mFixedHeight = i
        setMeasuredDimension(measuredWidth, this.mFixedHeight)
        if (z) {
            configureBounds(true)
            requestLayout()
        }
    }

    fun enableImageTransforms(z: Boolean) {
        this.mTransformsEnabled = z
        if (z) {
            return
        }
        resetTransformations()
    }

    private fun configureBounds(z: Boolean) {
        val drawable = this.drawable
        if (drawable == null || !this.mHaveLayout) {
            return
        }
        val intrinsicWidth = drawable.intrinsicWidth
        val intrinsicHeight = this.drawable!!.intrinsicHeight
        val z2 =
            (intrinsicWidth < 0 || width == intrinsicWidth) && (intrinsicHeight < 0 || height == intrinsicHeight)
        this.drawable!!.setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        if (z || (this.mMinScale == 0.0f && this.drawable != null && this.mHaveLayout)) {
            generateMatrix()
            generateScale()
        }
        if (z2 || mMatrix.isIdentity) {
            this.mDrawMatrix = null
        } else {
            this.mDrawMatrix = this.mMatrix
        }
    }

    private fun generateMatrix() {
        val intrinsicWidth = drawable!!.intrinsicWidth
        val intrinsicHeight = drawable!!.intrinsicHeight
        val width = width.toFloat()
        val height = height.toFloat()
        val f2 = intrinsicHeight.toFloat()
        val min =
            min((width / intrinsicWidth.toFloat()).toDouble(), (height / f2).toDouble()).toFloat()
        mMatrix.reset()
        mTempSrc[0.0f, 0.0f, intrinsicWidth.toFloat()] = f2
        mTempDst[0.0f, 0.0f, width] = height
        val f3 = width * 0.5f
        val f4 = intrinsicWidth.toFloat() * 0.5f * min
        val f5 = height * 0.5f
        val f6 = f2 * 0.5f * min
        val rectF = RectF(f3 - f4, f5 - f6, f3 + f4, f5 + f6)
        if (mTempDst.contains(rectF)) {
            mMatrix.setRectToRect(this.mTempSrc, rectF, Matrix.ScaleToFit.CENTER)
        } else {
            mMatrix.setRectToRect(this.mTempSrc, this.mTempDst, Matrix.ScaleToFit.CENTER)
        }
        mOriginalMatrix.set(this.mMatrix)
        val listener = this.mListener
        listener?.zoomableImageViewDidUpdate(this)
    }

    private fun generateScale() {
        val intrinsicWidth = drawable!!.intrinsicWidth
        val intrinsicHeight = drawable!!.intrinsicHeight
        val width = width
        val height = height
        val i = this.mEdgeInset
        this.mMinScale = min(
            (width / (intrinsicWidth - (i * 2))).toDouble(),
            (height / (intrinsicHeight - (i * 2))).toDouble()
        ).toFloat()
        this.mMaxScale = 1.8f
    }

    val scale: Float
        get() {
            mMatrix.getValues(this.mValues)
            return mValues[0]
        }

    fun scale(f: Float, f2: Float, f3: Float) {
        mMatrix.postRotate(-this.mRotation, width.toFloat() / 2, height.toFloat() / 2)
        val min = min(
            max(f.toDouble(), mMinScale.toDouble()),
            (this.mMaxScale * SCALE_OVERZOOM_FACTOR).toDouble()
        ).toFloat()
        val scale = scale
        val f4 = this.mMaxScale
        if (min > f4 && scale <= f4) {
            postDelayed({
                val scale2 = this@ZoomableImageView.scale
                if (scale2 > this@ZoomableImageView.mMaxScale) {
                    val f5 =
                        1.0f / (1.0f - (this@ZoomableImageView.mMaxScale / scale2))
                    val f6 = 1.0f - f5
                    val width =
                        this@ZoomableImageView.width.toFloat() / 2
                    val height =
                        this@ZoomableImageView.height.toFloat() / 2
                    val f7 =
                        mTranslateRect.left * f6
                    val f8 =
                        mTranslateRect.top * f6
                    val width2 =
                        (this@ZoomableImageView.width * f5) + (mTranslateRect.right * f6)
                    val height2 =
                        (this@ZoomableImageView.height * f5) + (mTranslateRect.bottom * f6)
                    mScaleRunnable!!.start(
                        scale2,
                        mMaxScale,
                        if (width2 > f7) (width2 + f7) / DOUBLE_TAP_SCALE_FACTOR else min(
                            max(width2.toDouble(), width.toDouble()),
                            f7.toDouble()
                        ).toFloat(),
                        if (height2 > f8) (height2 + f8) / DOUBLE_TAP_SCALE_FACTOR else min(
                            max(height2.toDouble(), height.toDouble()),
                            f8.toDouble()
                        ).toFloat()
                    )
                }
            }, ZOOM_CORRECTION_DELAY)
        }
        val f5 = min / scale
        mMatrix.postScale(f5, f5, f2, f3)
        mMatrix.postRotate(this.mRotation, width.toFloat() / 2, height.toFloat() / 2)
        invalidate()
        val listener = this.mListener
        listener?.zoomableImageViewDidUpdate(this)
    }

    fun translate(f: Float, f2: Float): Int {
        mTranslateRect.set(this.mTempSrc)
        mMatrix.mapRect(this.mTranslateRect)
        val width = width.toFloat()
        val f3 = mTranslateRect.left
        val f4 = mTranslateRect.right
        val f5 = width - 0.0f
        val max = if (f4 - f3 < f5) ((f5 - (f4 + f3)) / DOUBLE_TAP_SCALE_FACTOR) + 0.0f else max(
            (width - f4).toDouble(),
            min((0.0f - f3).toDouble(), f.toDouble())
        ).toFloat()
        val height = height.toFloat()
        val f6 = mTranslateRect.top
        val f7 = mTranslateRect.bottom
        val f8 = height - 0.0f
        val max2 = if (f7 - f6 < f8) ((f8 - (f7 + f6)) / DOUBLE_TAP_SCALE_FACTOR) + 0.0f else max(
            (height - f7).toDouble(),
            min((0.0f - f6).toDouble(), f2.toDouble())
        ).toFloat()
        mMatrix.postTranslate(max, max2)
        invalidate()
        val listener = this.mListener
        listener?.zoomableImageViewDidUpdate(this)
        val z = max == f
        val z2 = max2 == f2
        if (z && z2) {
            return 3
        }
        if (z) {
            return 1
        }
        return if (z2) 2 else 0
    }

    fun snap() {
        mTranslateRect.set(this.mTempSrc)
        mMatrix.mapRect(this.mTranslateRect)
        val width = width.toFloat()
        val f = mTranslateRect.left
        val f2 = mTranslateRect.right
        var f3 = 0.0f
        val f4 = width - 0.0f
        val f5 =
            if (f2 - f < f4) ((f4 - (f2 + f)) / DOUBLE_TAP_SCALE_FACTOR) + 0.0f else if (f > 0.0f) 0.0f - f else if (f2 < width) width - f2 else 0.0f
        val height = height.toFloat()
        val f6 = mTranslateRect.top
        val f7 = mTranslateRect.bottom
        val f8 = height - 0.0f
        if (f7 - f6 < f8) {
            f3 = 0.0f + ((f8 - (f7 + f6)) / DOUBLE_TAP_SCALE_FACTOR)
        } else if (f6 > 0.0f) {
            f3 = 0.0f - f6
        } else if (f7 < height) {
            f3 = height - f7
        }
        if (abs(f5.toDouble()) > SNAP_THRESHOLD || abs(f3.toDouble()) > SNAP_THRESHOLD) {
            mSnapRunnable!!.start(f5, f3)
            return
        }
        mMatrix.postTranslate(f5, f3)
        invalidate()
        val listener = this.mListener
        listener?.zoomableImageViewDidUpdate(this)
    }

    fun rotate(f: Float, z: Boolean) {
        if (z) {
            mRotateRunnable!!.start(f)
            return
        }
        this.mRotation += f
        mMatrix.postRotate(f, width.toFloat() / 2, height.toFloat() / 2)
        invalidate()
        val listener = this.mListener
        listener?.zoomableImageViewDidUpdate(this)
    }

    private fun initialize() {
        val context = context
        if (!sInitialized) {
            sInitialized = true
            val scaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
            sTouchSlopSquare = scaledTouchSlop * scaledTouchSlop
        }
        this.mGestureDetector = GestureDetector(context, this, null)
        val scaleGestureDetector = ScaleGestureDetector(context, this)
        this.mScaleGetureDetector = scaleGestureDetector
        this.mQuickScaleEnabled =
            ScaleGestureDetectorCompat.isQuickScaleEnabled(scaleGestureDetector)
        this.mScaleRunnable = ScaleRunnable(this)
        this.mTranslateRunnable = TranslateRunnable(this)
        this.mSnapRunnable = SnapRunnable(this)
        this.mRotateRunnable = RotateRunnable(this)
    }

    class ScaleRunnable(private val mHeader: ZoomableImageView) : Runnable {
        private var mCenterX = 0f
        private var mCenterY = 0f
        var mRunning: Boolean = false
        private var mStartScale = 0f
        private var mStartTime: Long = 0
        private var mStop = false
        private var mTargetScale = 0f
        private var mVelocity = 0f
        private var mZoomingIn = false

        fun start(f: Float, f2: Float, f3: Float, f4: Float): Boolean {
            if (this.mRunning) {
                return false
            }
            this.mCenterX = f3
            this.mCenterY = f4
            this.mTargetScale = f2
            this.mStartTime = System.currentTimeMillis()
            this.mStartScale = f
            val f5 = this.mTargetScale
            this.mZoomingIn = f5 > f
            this.mVelocity = (f5 - f) / 200.0f
            this.mRunning = true
            this.mStop = false
            mHeader.post(this)
            return true
        }

        fun stop() {
            this.mRunning = false
            this.mStop = true
        }


        override fun run() {
            if (mStop) return

            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - mStartTime
            val scale = mStartScale + mVelocity * elapsedTime.toFloat()

            mHeader.scale(scale, mCenterX, mCenterY)

            val reachedTarget = scale == mTargetScale
            val passedTarget = if (mZoomingIn) scale > mTargetScale else scale < mTargetScale

            if (reachedTarget || passedTarget) {
                mHeader.scale(mTargetScale, mCenterX, mCenterY)
                stop()
            } else if (!mStop) {
                mHeader.post(this)
            }
        }

    }

    class TranslateRunnable(private val mHeader: ZoomableImageView) : Runnable {
        private var mDecelerationX = 0f
        private var mDecelerationY = 0f
        private var mLastRunTime: Long = -1
        var mRunning: Boolean = false
        private var mStop = false
        private var mVelocityX = 0f
        private var mVelocityY = 0f

        fun start(f: Float, f2: Float): Boolean {
            if (this.mRunning) {
                return false
            }
            this.mLastRunTime = -1L
            this.mVelocityX = f
            this.mVelocityY = f2
            val atan2 = atan2(f2.toDouble(), f.toDouble()).toFloat().toDouble()
            this.mDecelerationX = (cos(atan2) * 20000.0).toFloat()
            this.mDecelerationY = (sin(atan2) * 20000.0).toFloat()
            this.mStop = false
            this.mRunning = true
            mHeader.post(this)
            return true
        }

        fun stop() {
            this.mRunning = false
            this.mStop = true
        }

        // java.lang.Runnable
        override fun run() {
            if (this.mStop) {
                return
            }
            val currentTimeMillis = System.currentTimeMillis()
            val j = this.mLastRunTime
            val f = if (j != -1L) ((currentTimeMillis - j).toFloat()) / 1000.0f else 0.0f
            val translate =
                mHeader.translate(this.mVelocityX * f, this.mVelocityY * f)
            this.mLastRunTime = currentTimeMillis
            val f2 = this.mDecelerationX * f
            if (abs(mVelocityX.toDouble()) > abs(f2.toDouble())) {
                this.mVelocityX -= f2
            } else {
                this.mVelocityX = 0.0f
            }
            val f3 = this.mDecelerationY * f
            if (abs(mVelocityY.toDouble()) > abs(f3.toDouble())) {
                this.mVelocityY -= f3
            } else {
                this.mVelocityY = 0.0f
            }
            val f4 = this.mVelocityX
            if ((f4 == 0.0f && this.mVelocityY == 0.0f) || translate == 0) {
                stop()
                mHeader.snap()
            } else {
                var f5 = DECELERATION_RATE
                if (translate == 1) {
                    if (f4 <= 0.0f) {
                        f5 = -20000.0f
                    }
                    this.mDecelerationX = f5
                    this.mDecelerationY = 0.0f
                    this.mVelocityY = 0.0f
                } else if (translate == 2) {
                    this.mDecelerationX = 0.0f
                    if (this.mVelocityY <= 0.0f) {
                        f5 = -20000.0f
                    }
                    this.mDecelerationY = f5
                    this.mVelocityX = 0.0f
                }
            }
            if (this.mStop) {
                return
            }
            mHeader.post(this)
        }

        companion object {
            private const val DECELERATION_RATE = 20000.0f
            private const val NEVER: Long = -1
        }
    }

    class SnapRunnable(private val mHeader: ZoomableImageView) : Runnable {
        private var mRunning = false
        private var mStartRunTime: Long = -1
        private var mStop = false
        private var mTranslateX = 0f
        private var mTranslateY = 0f

        fun start(f: Float, f2: Float): Boolean {
            if (this.mRunning) {
                return false
            }
            this.mStartRunTime = -1L
            this.mTranslateX = f
            this.mTranslateY = f2
            this.mStop = false
            this.mRunning = true
            mHeader.postDelayed(this, SNAP_DELAY)
            return true
        }

        fun stop() {
            this.mRunning = false
            this.mStop = true
        }

        // java.lang.Runnable
        override fun run() {
            val f: Float
            val f2: Float
            if (this.mStop) {
                return
            }
            val currentTimeMillis = System.currentTimeMillis()
            val j = this.mStartRunTime
            val f3 = if (j != -1L) (currentTimeMillis - j).toFloat() else 0.0f
            if (j == -1L) {
                this.mStartRunTime = currentTimeMillis
            }
            if (f3 >= 100.0f) {
                f2 = this.mTranslateX
                f = this.mTranslateY
            } else {
                val f4 = 100.0f - f3
                var f5 = (this.mTranslateX / f4) * 10.0f
                var f6 = (this.mTranslateY / f4) * 10.0f
                if (abs(f5.toDouble()) > abs(mTranslateX.toDouble()) || java.lang.Float.isNaN(f5)) {
                    f5 = this.mTranslateX
                }
                if (abs(f6.toDouble()) > abs(mTranslateY.toDouble()) || java.lang.Float.isNaN(f6)) {
                    f6 = this.mTranslateY
                }
                val f7 = f5
                f = f6
                f2 = f7
            }
            mHeader.translate(f2, f)
            val f8 = this.mTranslateX - f2
            this.mTranslateX = f8
            val f9 = this.mTranslateY - f
            this.mTranslateY = f9
            if (f8 == 0.0f && f9 == 0.0f) {
                stop()
            }
            if (this.mStop) {
                return
            }
            mHeader.post(this)
        }

        companion object {
            private const val NEVER: Long = -1
        }
    }

    class RotateRunnable(private val mHeader: ZoomableImageView) : Runnable {
        private var mAppliedRotation = 0f
        private var mLastRuntime: Long = 0
        private var mRunning = false
        private var mStop = false
        private var mTargetRotation = 0f
        private var mVelocity = 0f

        fun start(f: Float) {
            if (this.mRunning) {
                return
            }
            this.mTargetRotation = f
            this.mVelocity = f / 500.0f
            this.mAppliedRotation = 0.0f
            this.mLastRuntime = -1L
            this.mStop = false
            this.mRunning = true
            mHeader.post(this)
        }

        fun stop() {
            this.mRunning = false
            this.mStop = true
        }

        // java.lang.Runnable
        override fun run() {
            if (this.mStop) {
                return
            }
            if (this.mAppliedRotation != this.mTargetRotation) {
                val currentTimeMillis = System.currentTimeMillis()
                val j = this.mLastRuntime
                var f = this.mVelocity * ((if (j != -1L) currentTimeMillis - j else 0L).toFloat())
                val f2 = this.mAppliedRotation
                val f3 = this.mTargetRotation
                if ((f2 < f3 && f2 + f > f3) || (f2 > f3 && f2 + f < f3)) {
                    f = f3 - f2
                }
                mHeader.rotate(f, false)
                val f4 = this.mAppliedRotation + f
                this.mAppliedRotation = f4
                if (f4 == this.mTargetRotation) {
                    stop()
                }
                this.mLastRuntime = currentTimeMillis
            }
            if (this.mStop) {
                return
            }
            mHeader.post(this)
        }

        companion object {
            private const val NEVER: Long = -1
        }
    }

    fun setMaxInitialScale(f: Float) {
        this.mMaxInitialScaleFactor = f
    }

    fun setScrollMinNumberOfPointers(i: Int) {
        this.mScrollMinNumberOfPointers = i
    }

    // android.view.View
    override fun getMatrix(): Matrix {
        return this.mMatrix
    }

    fun setListener(listener: Listener?) {
        this.mListener = listener
    }

    fun setEdgeInset(i: Int) {
        this.mEdgeInset = i
    }

    companion object {
        private const val DOUBLE_TAP_SCALE_FACTOR = 2.0f
        private const val ROTATE_ANIMATION_DURATION: Long = 500
        private const val SCALE_OVERZOOM_FACTOR = 1.3f
        private const val SNAP_DELAY: Long = 250
        private const val SNAP_DURATION: Long = 100
        private const val SNAP_THRESHOLD = 20.0f
        private const val TAG = "ZoomableImageView"
        const val TRANSLATE_BOTH: Int = 3
        const val TRANSLATE_NONE: Int = 0
        const val TRANSLATE_X_ONLY: Int = 1
        const val TRANSLATE_Y_ONLY: Int = 2
        private const val ZOOM_ANIMATION_DURATION: Long = 200
        private const val ZOOM_CORRECTION_DELAY: Long = 600
        private var sInitialized = false
        private var sTouchSlopSquare = 0
    }
}
