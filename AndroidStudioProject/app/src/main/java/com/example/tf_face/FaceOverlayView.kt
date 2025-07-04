package com.example.tf_face

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val testPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }
    private var faces: List<RectF> = emptyList()
    private var viewWidth: Int = 0
    private var viewHeight: Int = 0

    fun setFaces(faces: List<RectF>, bitmapWidth: Int, bitmapHeight: Int) {
        this.faces = faces
        this.viewWidth = bitmapWidth
        this.viewHeight = bitmapHeight
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw test rectangle
        val testRect = RectF(
            viewWidth * 0.25f, viewHeight * 0.25f,
            viewWidth * 0.75f, viewHeight * 0.75f
        )
        canvas.drawRect(testRect, testPaint)
        Log.d("FaceDetection", "Overlay drew test rectangle: $testRect")

        // Adjust for aspect ratio
        val modelSize = 128f
        val viewAspect = viewWidth.toFloat() / viewHeight
        val modelAspect = 1f // 128/128
        val scaleX: Float
        val scaleY: Float
        val offsetX: Float
        val offsetY: Float

        if (viewAspect > modelAspect) {
            // View is wider than model
            scaleY = viewHeight.toFloat() / modelSize
            scaleX = scaleY
            offsetX = (viewWidth - modelSize * scaleX) / 2
            offsetY = 0f
        } else {
            // View is taller than model
            scaleX = viewWidth.toFloat() / modelSize
            scaleY = scaleX
            offsetX = 0f
            offsetY = (viewHeight - modelSize * scaleY) / 2
        }

        Log.d("FaceDetection", "Overlay scale factors: scaleX=$scaleX, scaleY=$scaleY, offsetX=$offsetX, offsetY=$offsetY")

        for (face in faces) {
            val scaledRect = RectF(
                (face.left * scaleX + offsetX).coerceIn(0f, viewWidth.toFloat()),
                (face.top * scaleY + offsetY).coerceIn(0f, viewHeight.toFloat()),
                (face.right * scaleX + offsetX).coerceIn(0f, viewWidth.toFloat()),
                (face.bottom * scaleY + offsetY).coerceIn(0f, viewHeight.toFloat())
            )
            Log.d("FaceDetection", "Overlay drawing scaled rect: left=${scaledRect.left}, top=${scaledRect.top}, right=${scaledRect.right}, bottom=${scaledRect.bottom}")
            canvas.drawRect(scaledRect, paint)
        }
    }
}