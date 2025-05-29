package com.whitelabel.android.view

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout

class TouchFeedbackView : FrameLayout {
    private fun initTouchFeedbackView() {
    }

    constructor(context: Context) : super(context) {
        initTouchFeedbackView()
    }

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        initTouchFeedbackView()
    }

    constructor(context: Context, attributeSet: AttributeSet?, i: Int) : super(
        context,
        attributeSet,
        i
    ) {
        initTouchFeedbackView()
    }

    constructor(context: Context, attributeSet: AttributeSet?, i: Int, i2: Int) : super(
        context,
        attributeSet,
        i,
        i2
    ) {
        initTouchFeedbackView()
    }

    fun displayFeedback(f: Float, f2: Float) {
        val applyDimension = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40.0f, resources.displayMetrics).toInt()
        val view = View(context)
        val shapeDrawable = ShapeDrawable(OvalShape())
        val layoutParams = LayoutParams(applyDimension, applyDimension)
        val f3 = applyDimension / 2.0f
        layoutParams.leftMargin = Math.round(f - f3)
        layoutParams.topMargin = Math.round(f2 - f3)
        shapeDrawable.paint.color = -1
        view.background = shapeDrawable
        addView(view, layoutParams)
        view.scaleX = 0.2f
        view.scaleY = 0.2f
        view.alpha = 0.8f
        val ofFloat = ObjectAnimator.ofFloat(view, "scaleX", 1.0f)
        val ofFloat2 = ObjectAnimator.ofFloat(view, "scaleY", 1.0f)
        val ofFloat3 = ObjectAnimator.ofFloat(view, "alpha", 0.0f)
        val animatorSet = AnimatorSet()
        animatorSet.play(ofFloat).with(ofFloat2).with(ofFloat3)
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.setDuration(500L)
        animatorSet.addListener(object : Animator.AnimatorListener {
            // from class: com.whitelabel.view.android.TouchFeedbackView.1
            // android.animation.Animator.AnimatorListener
            override fun onAnimationRepeat(animator: Animator) {
            }

            // android.animation.Animator.AnimatorListener
            override fun onAnimationStart(animator: Animator) {
            }

            // android.animation.Animator.AnimatorListener
            override fun onAnimationEnd(animator: Animator) {
                this@TouchFeedbackView.removeView(view)
            }

            // android.animation.Animator.AnimatorListener
            override fun onAnimationCancel(animator: Animator) {
                this@TouchFeedbackView.removeView(view)
            }
        })
        animatorSet.start()
    }
}
