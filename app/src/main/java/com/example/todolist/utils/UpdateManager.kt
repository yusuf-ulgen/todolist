package com.example.todolist.utils

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import com.example.todolist.BuildConfig
import com.example.todolist.R

object UpdateManager {

    // Buradaki değeri Play Store'daki en güncel sürümün koduyla değiştirebilirsiniz
    private const val REMOTE_VERSION_CODE = 8 // Mevcut sürümünüz 8 olduğu için uyarı kesilecektir

    fun checkUpdates(context: Context) {
        val currentVersionCode = BuildConfig.VERSION_CODE

        // Eğer Play Store'daki sürüm mevcut sürümden büyükse bildirimi göster
        if (REMOTE_VERSION_CODE > currentVersionCode) {
            showUpdateDialog(context)
        }
    }

    private fun showUpdateDialog(context: Context) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_update_notification, null)
        dialog.setContentView(view)

        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (context.resources.displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        dialog.setCancelable(true)

        val btnUpdate = view.findViewById<android.view.View>(R.id.btnUpdate)
        val btnClose = view.findViewById<android.view.View>(R.id.btnClose)

        btnUpdate.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                context.startActivity(intent)
            } catch (e: Exception) {
                // Play Store açılmazsa tarayıcıdan dene
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                context.startActivity(intent)
            }
            dialog.dismiss()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
