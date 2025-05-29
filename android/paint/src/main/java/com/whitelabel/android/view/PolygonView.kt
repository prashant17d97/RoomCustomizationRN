package com.whitelabel.android.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.sqrt

class PolygonView : View, GestureDetector.OnGestureListener {
    private var mDotPaint: Paint? = null
    private var mDotRadius = 0
    private var mDraggingOrigin: PointF?
    private var mDraggingPoint: PointF?
    private var mFillPaint: Paint? = null
    private var mGestureDetector: GestureDetector? = null
    private var mLinePaint: Paint? = null
    private val mMatrix: Matrix
    private var mPoints: List<PointF>
    private var mUserInteractionEnabled: Boolean

    // android.view.GestureDetector.OnGestureListener
    override fun onFling(
        motionEvent: MotionEvent?,
        motionEvent2: MotionEvent,
        f: Float,
        f2: Float
    ): Boolean {
        return false
    }

    // android.view.GestureDetector.OnGestureListener
    override fun onLongPress(motionEvent: MotionEvent) {
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
        this.mPoints = ArrayList()
        this.mDraggingPoint = null
        this.mDraggingOrigin = null
        this.mUserInteractionEnabled = false
        initPolygonView()
    }

    constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet) {
        this.mMatrix = Matrix()
        this.mPoints = ArrayList()
        this.mDraggingPoint = null
        this.mDraggingOrigin = null
        this.mUserInteractionEnabled = false
        initPolygonView()
    }

    constructor(context: Context?, attributeSet: AttributeSet?, i: Int) : super(
        context,
        attributeSet,
        i
    ) {
        this.mMatrix = Matrix()
        this.mPoints = ArrayList()
        this.mDraggingPoint = null
        this.mDraggingOrigin = null
        this.mUserInteractionEnabled = false
        initPolygonView()
    }

    constructor(context: Context?, attributeSet: AttributeSet?, i: Int, i2: Int) : super(
        context,
        attributeSet,
        i,
        i2
    ) {
        this.mMatrix = Matrix()
        this.mPoints = ArrayList()
        this.mDraggingPoint = null
        this.mDraggingOrigin = null
        this.mUserInteractionEnabled = false
        initPolygonView()
    }

    private fun initPolygonView() {
        this.mGestureDetector = GestureDetector(context, this)
        val paint = Paint()
        this.mLinePaint = paint
        paint.strokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            3.0f,
            resources.displayMetrics
        )
        mLinePaint!!.color = -16749587
        mLinePaint!!.style = Paint.Style.STROKE
        val paint2 = Paint()
        this.mFillPaint = paint2
        paint2.color = 436235245
        mFillPaint!!.style = Paint.Style.FILL
        val paint3 = Paint()
        this.mDotPaint = paint3
        paint3.color = -16749587
        this.mDotRadius =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4.0f, resources.displayMetrics)
                .toInt()
    }

    // android.view.View
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val createPath = createPath(this.mMatrix)
        canvas.drawPath(createPath, mFillPaint!!)
        canvas.drawPath(createPath, mLinePaint!!)
        val i = this.mDotRadius
        for (pointF in this.mPoints) {
            val mapPoint = mapPoint(pointF, this.mMatrix)
            val f = i.toFloat()
            val f2 = (i * 2).toFloat()
            canvas.drawOval(
                RectF(
                    (mapPoint.x - f) - 1.0f,
                    (mapPoint.y - f) - 1.0f,
                    mapPoint.x + f2 + 1.0f,
                    mapPoint.y + f2 + 1.0f
                ),
                mDotPaint!!
            )
        }
    }

    private fun createPath(matrix: Matrix): Path {
        val path = Path()
        var z = true
        for (pointF in this.mPoints) {
            if (z) {
                path.moveTo(pointF.x, pointF.y)
                z = false
            } else {
                path.lineTo(pointF.x, pointF.y)
            }
        }
        path.close()
        path.transform(matrix)
        return path
    }

    fun setRect(rectF: RectF) {
        val arrayList: ArrayList<PointF> = ArrayList()
        arrayList.add(PointF(rectF.left, rectF.top))
        arrayList.add(PointF(rectF.right, rectF.top))
        arrayList.add(PointF(rectF.right, rectF.bottom))
        arrayList.add(PointF(rectF.left, rectF.bottom))
        this.mPoints = arrayList
        invalidate()
    }

    fun setMatrix(matrix: Matrix?) {
        mMatrix.set(matrix)
        invalidate()
    }

    // android.view.View
    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        return super.onTouchEvent(motionEvent) || mGestureDetector!!.onTouchEvent(motionEvent)
    }

    // android.view.GestureDetector.OnGestureListener
    override fun onDown(motionEvent: MotionEvent): Boolean {
        if (this.mUserInteractionEnabled) {
            if (motionEvent.pointerCount != 1) {
                this.mDraggingPoint = null
                return false
            }
            val pointNear = pointNear(
                invertMapPoint(
                    PointF(motionEvent.x, motionEvent.y),
                    this.mMatrix
                )
            )
            this.mDraggingPoint = pointNear
            if (pointNear != null) {
                this.mDraggingOrigin = invertMapPoint(
                    PointF(motionEvent.x, motionEvent.y),
                    this.mMatrix
                )
            }
            return this.mDraggingPoint != null
        }
        return false
    }

    // android.view.GestureDetector.OnGestureListener
    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (this.mDraggingPoint == null || e2.pointerCount != 1) {
            return false
        }
        val invertMapPoint = invertMapPoint(
            PointF(e1?.x ?: 0f, e1?.y ?: 0f),
            this.mMatrix
        )
        val invertMapPoint2 = invertMapPoint(
            PointF(e2.x, e2.y),
            this.mMatrix
        )
        mDraggingPoint!!.x = (invertMapPoint2.x - invertMapPoint.x) + mDraggingOrigin!!.x
        mDraggingPoint!!.y = (invertMapPoint2.y - invertMapPoint.y) + mDraggingOrigin!!.y
        invalidate()
        return true
    }

    private fun pointIndexNear(pointF: PointF): Int {
        val applyDimension =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80.0f, resources.displayMetrics)
        var i = -1
        var d = 3.4028234663852886E38
        for ((i2, pointF2) in this.mPoints.withIndex()) {
            val distance = distance(pointF2, pointF)
            if (distance <= applyDimension && distance < d) {
                i = i2
                d = distance
            }
        }
        return i
    }

    fun getBitmap(i: Int, i2: Int): Bitmap {
        val paint = Paint()
        paint.style = Paint.Style.FILL
        paint.color = -1
        val createBitmap = createBitmap(i, i2, Bitmap.Config.ARGB_8888)
        Canvas(createBitmap).drawPath(createPath(Matrix()), paint)
        return createBitmap
    }

    private fun pointNear(pointF: PointF): PointF? {
        val pointIndexNear = pointIndexNear(pointF)
        if (pointIndexNear == -1) {
            return null
        }
        return mPoints[pointIndexNear]
    }

    fun setUserInteractionEnabled(z: Boolean) {
        this.mUserInteractionEnabled = z
    }

    companion object {
        private const val TAG = "PolygonView"
        private fun mapPoint(pointF: PointF, matrix: Matrix): PointF {
            val fArr = floatArrayOf(pointF.x, pointF.y)
            matrix.mapPoints(fArr)
            return PointF(fArr[0], fArr[1])
        }

        private fun invertMapPoint(pointF: PointF, matrix: Matrix): PointF {
            val matrix2 = Matrix()
            matrix.invert(matrix2)
            val fArr = floatArrayOf(pointF.x, pointF.y)
            matrix2.mapPoints(fArr)
            return PointF(fArr[0], fArr[1])
        }

        private fun distance(pointF: PointF, pointF2: PointF): Double {
            val f = pointF.x - pointF2.x
            val f2 = pointF.y - pointF2.y
            return sqrt(((f * f) + (f2 * f2)).toDouble())
        }
    }
}
