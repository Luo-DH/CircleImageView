package com.cheat.lib_ui_circle

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.applyCanvas

class CircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val mPaintIn = Paint().apply {
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }

    private val mPaintIn2 = Paint().apply {
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)
    }

    private val mPaintOut = Paint().apply {
        isAntiAlias = true
    }

    private val mPaintBlur = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val mPath = Path()
    private lateinit var mTranBitmap: Bitmap

    private var layerId: Int = 0

    private var mCenterX: Float = 0f
    private var mCenterY: Float = 0f
    private var mRadius: Float = 0f

    private var mRotationDuration: Long = 5_000

    private var mRingRadius = 0f


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        mCenterX = width / 2f
        mCenterY = height / 2f
        mRadius = (width.coerceAtMost(height) / 2f)

        // 创建图层
        layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // 绘制圆1（黑色圆）
        mPath.reset()
        mPath.addCircle(mCenterX, mCenterY, mRadius, Path.Direction.CCW)
        canvas.drawPath(mPath, mPaintOut)

        if (!::mTranBitmap.isInitialized) {
            // 创建黑色圆形 Bitmap
            mTranBitmap = transform(createCircleBitmap())
        }

        val scale: Float
        val translationX: Float
        val translationY: Float

        if (mTranBitmap.width.toFloat() * height.toFloat() > width.toFloat() * mTranBitmap.height.toFloat()) {
            scale = height.toFloat() / mTranBitmap.height.toFloat()
            translationX = (width.toFloat() - mTranBitmap.width.toFloat() * scale) * 0.5f
            translationY = 0f
        } else {
            scale = width.toFloat() / mTranBitmap.width.toFloat()
            translationX = 0f
            translationY = (height.toFloat() - mTranBitmap.height.toFloat() * scale) * 0.5f
        }

        canvas.save()
        canvas.rotate(mRotationAngle, mCenterX, mCenterY)
        canvas.translate(translationX, translationY)
        canvas.scale(scale, scale)


        canvas.drawBitmap(mTranBitmap, 0f, 0f, mPaintIn)

        canvas.restore()

        mPath.reset()
        mPath.addCircle(mCenterX, mCenterY, mRingRadius, Path.Direction.CW)
        canvas.drawPath(mPath, mPaintIn2)

        // 恢复图层
        canvas.restoreToCount(layerId)
    }

    private var mRotationAngle = 0f

    private var mRotationAnim = ValueAnimator.ofFloat(mRotationAngle, mRotationAngle + 360f)

    private fun createRotationAnim(): ValueAnimator {
        mRotationAnim = ValueAnimator.ofFloat(mRotationAngle, (mRotationAngle) + 360).also {
            it.duration = mRotationDuration
            it.removeUpdateListener(mUpdateListener)
            it.addUpdateListener(mUpdateListener)
            it.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }

            })
        }
        return mRotationAnim
    }

    private val mUpdateListener = ValueAnimator.AnimatorUpdateListener { animation ->
        val value = animation.animatedValue as Float
        changeRotationAngle(value)
    }

    fun startRotationAnimation() {
        createRotationAnim().start()
    }

    fun pauseRotationAnimation() {
        mRotationAnim.pause()
    }

    fun goOnRotationAnimation() {
        startRotationAnimation()
    }

    fun changeRotationAngle(angle: Float) {
        mRotationAngle = angle
        invalidate()
    }

    private fun createCircleBitmap(): Bitmap {

        val res: Resources = context.resources
        val bitmap: Bitmap = BitmapFactory.decodeResource(res, R.drawable.test)

        return bitmap
    }

    fun setBitmap(bitmap: Bitmap) {
        mTranBitmap = transform(bitmap)

        invalidate()
    }

    fun setResource(resId: Int) {
        val res: Resources = context.resources
        val bitmap: Bitmap = BitmapFactory.decodeResource(res, resId)
        setBitmap(bitmap)
    }

    /**
     * 设置中间圆环的半径
     */
    fun setRingRadius(radius: Float) {
        mRingRadius = if (radius in 0f .. mRadius) {
            radius
        } else {
            0f
        }
        invalidate()
    }


    /**
     * 设置边距的大小
     */
    fun setBorderLength(len: Float) {
        if (len in 0f .. mRadius) {
            setRingRadius(mRadius - len)
        } else {
            setRingRadius(0f)
        }
    }

    fun setBlurConfig(blurRadius: Float, sampling: Float) {
        this.blurRadius = blurRadius
        this.sampling = sampling

        invalidate()
    }


    private var sampling = 1f
    private var blurRadius = 0f

    private fun transform(input: Bitmap): Bitmap {

        if (blurRadius < 1f) {
            return input
        }

        val scaledWidth = (input.width / sampling).toInt()
        val scaledHeight = (input.height / sampling).toInt()
        val output = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
        output.applyCanvas {
            scale(1 / sampling, 1 / sampling)
            drawBitmap(input, 0f, 0f, mPaintBlur)
        }

        var script: RenderScript? = null
        var tmpInt: Allocation? = null
        var tmpOut: Allocation? = null
        var blur: ScriptIntrinsicBlur? = null
        try {
            script = RenderScript.create(context)
            tmpInt = Allocation.createFromBitmap(
                script,
                output,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT
            )
            tmpOut = Allocation.createTyped(script, tmpInt.type)
            blur = ScriptIntrinsicBlur.create(script, Element.U8_4(script))
            blur.setRadius(blurRadius)
            blur.setInput(tmpInt)
            blur.forEach(tmpOut)
            tmpOut.copyTo(output)
        } finally {
            script?.destroy()
            tmpInt?.destroy()
            tmpOut?.destroy()
            blur?.destroy()
        }

        return output
    }

}