package com.example.todolist.util

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import com.example.todolist.databinding.LayoutStylishAlertBinding

object StylishAlert {
    fun show(activity: Activity, message: String, isError: Boolean = true) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
        val binding = LayoutStylishAlertBinding.inflate(LayoutInflater.from(activity), root, false)
        
        root.addView(binding.root)

        // Renk ve ikon ayarı
        if (!isError) {
            binding.alertIcon.setImageResource(android.R.drawable.ic_dialog_info)
            binding.alertIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#4CAF50")
            )
            binding.alertCard.strokeColor = android.graphics.Color.parseColor("#334CAF50")
        }

        binding.alertMessage.text = message

        // Giriş animasyonu
        binding.alertCard.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Çıkış ve kaldırma
        binding.alertCard.postDelayed({
            binding.alertCard.animate()
                .translationY(-400f)
                .alpha(0f)
                .setDuration(400)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    root.removeView(binding.root)
                }
                .start()
        }, 3000)
    }
}
