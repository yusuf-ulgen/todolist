package com.example.todolist.util

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

data class TutorialStep(
    val targetViewId: Int?,
    val title: String,
    val description: String,
    val actionText: String? = null,
    val actionUrl: String? = null
)

class TutorialManager(private val activity: Activity) {
    private val sharedPrefs = activity.getSharedPreferences("tutorials_v2", Context.MODE_PRIVATE)
    private var currentOverlay: TutorialOverlayView? = null

    fun showTutorial(pageKey: String, steps: List<TutorialStep>) {
        if (sharedPrefs.getBoolean(pageKey, false)) return // Already shown

        showStep(pageKey, steps, 0)
    }

    private fun showStep(pageKey: String, steps: List<TutorialStep>, index: Int) {
        if (index >= steps.size) {
            dismissTutorial(pageKey)
            return
        }

        val step = steps[index]
        val targetView = if (step.targetViewId != null) activity.findViewById<View>(step.targetViewId) else null
        
        if (targetView == null && step.targetViewId != null) {
            if (index == 0) {
                activity.window.decorView.postDelayed({
                    showStep(pageKey, steps, index)
                }, 500)
            } else {
                showStep(pageKey, steps, index + 1)
            }
            return
        }

        if (currentOverlay == null) {
            currentOverlay = TutorialOverlayView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            (activity.window.decorView as ViewGroup).addView(currentOverlay)
        }

        currentOverlay?.setTarget(
            targetView,
            step.title,
            step.description,
            index,
            steps.size,
            step.actionText,
            step.actionUrl,
            onNext = { showStep(pageKey, steps, index + 1) },
            onSkip = { dismissTutorial(pageKey) }
        )
    }

    private fun dismissTutorial(pageKey: String) {
        currentOverlay?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            currentOverlay = null
        }
        sharedPrefs.edit().putBoolean(pageKey, true).apply()
    }
}
