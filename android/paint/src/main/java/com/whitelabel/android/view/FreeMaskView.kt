package com.whitelabel.android.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class FreeMaskView : View {
    private var mBrushSize: Int
    private var mLastX = 0f
    private var mLastY = 0f
    private var mListener: Listener?
    private val mMatrix: Matrix
    private var mPaint: Paint? = null
    private var mPath: Path
    private val mPathPoints: MutableList<PointF?>

    interface Listener {
        fun onFreeMaskViewDidFinishDrawing(freeMaskView: FreeMaskView?)
    }

    constructor(context: Context?) : super(context) {
        this.mMatrix = Matrix()
        this.mBrushSize = 1
        this.mPath = Path()
        this.mPathPoints = ArrayList()
        this.mListener = null
        initFreeMaskView()
    }

    constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet) {
        this.mMatrix = Matrix()
        this.mBrushSize = 1
        this.mPath = Path()
        this.mPathPoints = ArrayList()
        this.mListener = null
        initFreeMaskView()
    }

    constructor(context: Context?, attributeSet: AttributeSet?, i: Int) : super(
        context,
        attributeSet,
        i
    ) {
        this.mMatrix = Matrix()
        this.mBrushSize = 1
        this.mPath = Path()
        this.mPathPoints = ArrayList()
        this.mListener = null
        initFreeMaskView()
    }

    constructor(context: Context?, attributeSet: AttributeSet?, i: Int, i2: Int) : super(
        context,
        attributeSet,
        i,
        i2
    ) {
        this.mMatrix = Matrix()
        this.mBrushSize = 1
        this.mPath = Path()
        this.mPathPoints = ArrayList()
        this.mListener = null
        initFreeMaskView()
    }

    private fun initFreeMaskView() {
        val paint = Paint()
        this.mPaint = paint
        paint.color = -16776961
        mPaint!!.style = Paint.Style.STROKE
        mPaint!!.strokeJoin = Paint.Join.ROUND
        mPaint!!.strokeCap = Paint.Cap.ROUND
        setBrushSize(this.mBrushSize)
    }

    fun setMatrix(matrix: Matrix?) {
        mMatrix.set(matrix)
        invalidate()
    }

    fun reset() {
        mPath.reset()
        mPathPoints.clear()
        invalidate()
    }

    // android.view.View
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.setMatrix(this.mMatrix)
        canvas.drawPath(this.mPath, mPaint!!)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)

        val inverseMatrix = Matrix()
        mMatrix.invert(inverseMatrix)
        val touchPoint = floatArrayOf(event.x, event.y)
        inverseMatrix.mapPoints(touchPoint)
        val x = touchPoint[0]
        val y = touchPoint[1]

        val action = event.actionMasked

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mPath.moveTo(x, y)
                mPathPoints.add(PointF(x, y))
            }

            MotionEvent.ACTION_MOVE -> {
                mPath.quadTo(mLastX, mLastY, (x + mLastX) / 2f, (y + mLastY) / 2f)
                mPathPoints.add(PointF(x, y))
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                mPath.quadTo(mLastX, mLastY, (x + mLastX) / 2f, (y + mLastY) / 2f)
                mPathPoints.add(PointF(x, y))
                finalizeDraw()
            }

            MotionEvent.ACTION_CANCEL -> reset()
        }

        mLastX = x
        mLastY = y

        return true
    }


    private fun finalizeDraw() {
        // Smooth the drawn points with a tolerance (e.g., 8.0)
        val reducedPoints: List<PointF> = reduce(mPathPoints.toList().filterNotNull(), 8.0)
        val path = getPath(reducedPoints)

        // If the gesture forms a short loop, close the path
        var shouldClosePath = false
        if (mPathPoints.size > 2) {
            val start = mPathPoints[0]
            val end = mPathPoints[mPathPoints.size - 1]
            val distance = distanceBetween(start!!, end!!)

            val closeThresholdPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                50.0f,
                resources.displayMetrics
            )

            if (distance <= closeThresholdPx) {
                shouldClosePath = true
            }
        }

        if (shouldClosePath) {
            path.close()
        }

        mPathPoints.clear()
        mPath = path
        invalidate()

        if (mListener != null) {
            mListener!!.onFreeMaskViewDidFinishDrawing(this)
        }
    }

    fun setBrushSize(i: Int) {
        this.mBrushSize = i
        mPaint!!.strokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            (i * 10).toFloat(),
            resources.displayMetrics
        )
    }

    fun setListener(listener: Listener?) {
        this.mListener = listener
    }

    fun getBitmap(i: Int, i2: Int): Bitmap {
        val createBitmap = createBitmap(i, i2, Bitmap.Config.ARGB_8888)
        Canvas(createBitmap).drawPath(
            this.mPath,
            mPaint!!
        )
        return createBitmap
    }

    companion object {
        private fun getPath(reducedPoints: List<PointF>): Path {
            val path = Path()

            var previous: PointF? = null
            for (point in reducedPoints) {
                if (previous == null) {
                    // Start the path
                    path.moveTo(point.x, point.y)

                    // Handle the case of a single-point stroke
                    if (reducedPoints.size == 1) {
                        path.quadTo(point.x, point.y, point.x, point.y)
                    }
                } else {
                    val controlX = (point.x + previous.x) / 2.0f
                    val controlY = (point.y + previous.y) / 2.0f
                    path.quadTo(previous.x, previous.y, controlX, controlY)
                }
                previous = point
            }
            return path
        }


        private fun reduce(list: List<PointF>, d: Double): List<PointF> {
            if (d <= 0.0 || list.size < 3) {
                return list
            }
            val arrayList: ArrayList<PointF> = ArrayList()
            arrayList.add(list[0])
            douglasPeuckerReduction(d, list, arrayList, 0, list.size - 1)
            arrayList.add(list[list.size - 1])
            return arrayList
        }

        private fun douglasPeuckerReduction(
            d: Double,
            list: List<PointF>,
            list2: MutableList<PointF>,
            i: Int,
            i2: Int
        ) {
            val pointF = list[i]
            val pointF2 = list[i2]
            var d2 = 0.0
            var i3 = 0
            for (i4 in i + 1..<i2) {
                val orthogonalDistance = orthogonalDistance(list[i4], pointF, pointF2)
                if (orthogonalDistance > d2) {
                    i3 = i4
                    d2 = orthogonalDistance
                }
            }
            if (d2 > d) {
                douglasPeuckerReduction(d, list, list2, i, i3)
                douglasPeuckerReduction(d, list, list2, i3, i2)
                return
            }
            list2.add(pointF)
            list2.add(pointF2)
        }

        private fun distanceBetween(pointF: PointF, pointF2: PointF): Double {
            val f = pointF.x - pointF2.x
            val f2 = pointF.y - pointF2.y
            return sqrt(((f * f) + (f2 * f2)).toDouble())
        }

        private fun orthogonalDistance(pointF: PointF, pointF2: PointF, pointF3: PointF): Double {
            return (abs(((((((pointF2.x * pointF3.y) + (pointF3.x * pointF.y)) + (pointF.x * pointF2.y)) - (pointF3.x * pointF2.y)) - (pointF.x * pointF3.y)) - (pointF2.x * pointF.y)) / 2.0) / sqrt(
                (pointF2.x - pointF3.x).toDouble().pow(2.0) + (pointF2.y - pointF3.y).toDouble()
                    .pow(2.0)
            )) * 2.0
        }
    }
}
