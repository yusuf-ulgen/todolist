package com.example.todolist.util

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.example.todolist.databinding.LayoutTutorialTooltipBinding

class TutorialOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#CC000000") // 80% black
    }
    private val eraserPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        isAntiAlias = true
    }
    private val strokePaint = Paint().apply {
        color = Color.parseColor("#00BFA5") // accent color
        style = Paint.Style.STROKE
        strokeWidth = 10f
        isAntiAlias = true
    }

    private var targetView: View? = null
    private var targetRect = RectF()
    private val binding: LayoutTutorialTooltipBinding

    private var onNextClick: (() -> Unit)? = null
    private var onSkipClick: (() -> Unit)? = null

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_HARDWARE, null)
        binding = LayoutTutorialTooltipBinding.inflate(LayoutInflater.from(context), this, true)
        
        setOnClickListener { onNextClick?.invoke() }
        binding.btnNext.setOnClickListener { onNextClick?.invoke() }
        binding.btnSkip.setOnClickListener { onSkipClick?.invoke() }
        binding.tooltipCard.setOnClickListener { /* stop propagation */ }
    }

    fun setTarget(
        view: View?,
        title: String,
        description: String,
        stepIndex: Int,
        totalSteps: Int,
        actionText: String? = null,
        actionUrl: String? = null,
        onNext: () -> Unit,
        onSkip: () -> Unit
    ) {
        this.targetView = view
        this.onNextClick = onNext
        this.onSkipClick = onSkip

        binding.tvTitle.text = title
        binding.tvDescription.text = description
        binding.tvProgress.text = "${stepIndex + 1}/$totalSteps"
        
        binding.btnSkip.visibility = if (totalSteps > 1) View.VISIBLE else View.GONE
        binding.btnNext.text = if (stepIndex == totalSteps - 1) "Anladım" else "Sonraki"

        if (!actionUrl.isNullOrEmpty()) {
            binding.btnAction.visibility = View.VISIBLE
            binding.btnAction.text = actionText ?: "Git"
            binding.btnAction.setOnClickListener {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(actionUrl))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } else {
            binding.btnAction.visibility = View.GONE
        }

        if (view != null) {
            view.post { 
                updateTargetRect()
                positionTooltip()
                invalidate()
            }
        } else {
            targetRect.set(0f, 0f, 0f, 0f)
            positionTooltip()
            invalidate()
        }
    }

    private fun updateTargetRect() {
        targetView?.let { view ->
            val location = IntArray(2)
            view.getLocationInWindow(location)
            targetRect.set(
                location[0].toFloat() - 20,
                location[1].toFloat() - 20,
                (location[0] + view.width).toFloat() + 20,
                (location[1] + view.height).toFloat() + 20
            )
        }
    }

    private fun positionTooltip() {
        binding.tooltipCard.post {
            val screenHeight = resources.displayMetrics.heightPixels
            val screenWidth = resources.displayMetrics.widthPixels
            val tooltipHeight = binding.tooltipCard.height
            val tooltipWidth = binding.tooltipCard.width
            
            val margin = 80f
            var top: Float

            if (targetRect.isEmpty) {
                top = (screenHeight - tooltipHeight) / 2f
            } else {
                top = targetRect.bottom + margin
                if (top + tooltipHeight > screenHeight - 200) {
                    top = targetRect.top - tooltipHeight - margin
                }
            }
            
            top = top.coerceIn(margin, screenHeight - tooltipHeight - margin)
            
            binding.tooltipCard.translationY = top
            binding.tooltipCard.translationX = (screenWidth - tooltipWidth) / 2f
            binding.tooltipCard.alpha = 1f
            binding.tooltipCard.visibility = View.VISIBLE
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        if (targetView != null) {
            canvas.drawRoundRect(targetRect, 32f, 32f, eraserPaint)
            canvas.drawRoundRect(targetRect, 32f, 32f, strokePaint)
        }
    }
}
