package com.example.todolist.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import com.example.todolist.BuildConfig
import com.example.todolist.PreferenceManager
import com.example.todolist.R

object ReleaseNotes {

        data class ReleaseNote(val version: String, val features: List<String>)

        // Manuel olarak buradan sürüm ve not girişi yapabilirsiniz
        private val notes =
                listOf(
                        ReleaseNote(
                                version = "1.7",
                                features =
                                        listOf(
                                                "1) Google Play politika uyumluluğu için galeri izinleri kaldırıldı.",
                                                "2) Uygulama stabilitesi ve performans iyileştirmeleri yapıldı."
                                        )
                        ),
                        ReleaseNote(
                                version = "1.6",
                                features =
                                        listOf(
                                                "1) İstatistikler ekranında ufak düzenlemeler yapıldı.",
                                                "2) Yeni sürüm notları sistemi eklendi.",
                                                "3) Yapımcının diğer içerikleri butonu eklendi."
                                        )
                        ),
                        ReleaseNote(
                                version = "1.5",
                                features =
                                        listOf(
                                                "Firebase senkronizasyon altyapısı güçlendirildi.",
                                                "Arayüz geliştirmeleri ve hata düzeltmeleri yapıldı."
                                        )
                        )
                )

        fun checkAndShow(context: Context) {
                val currentVersion = BuildConfig.VERSION_NAME
                val lastSeenVersion = PreferenceManager.getLastSeenVersionName(context)

                // Eğer mevcut sürüm daha önce gösterilmediyse göster
                if (currentVersion != lastSeenVersion) {
                        val note = notes.find { it.version == currentVersion }
                        if (note != null) {
                                showReleaseNotesDialog(context, note)
                        }
                        // Sürümü "görüldü" olarak işaretle
                        PreferenceManager.setLastSeenVersionName(context, currentVersion)
                }
        }

        private fun showReleaseNotesDialog(context: Context, note: ReleaseNote) {
                val dialog = Dialog(context)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                val view = LayoutInflater.from(context).inflate(R.layout.dialog_release_notes, null)
                dialog.setContentView(view)

                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog.window?.setLayout(
                        (context.resources.displayMetrics.widthPixels * 0.90).toInt(),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )

                val titleText = view.findViewById<TextView>(R.id.releaseNotesTitle)
                val contentText = view.findViewById<TextView>(R.id.releaseNotesContent)
                val closeButton = view.findViewById<android.view.View>(R.id.closeButton)

                titleText.text = "Sürüm Notları (v${note.version})"

                val fullNotesText = StringBuilder()
                note.features.forEach { line -> fullNotesText.append("• $line\n") }
                contentText.text = fullNotesText.toString().trim()

                closeButton.setOnClickListener { dialog.dismiss() }

                dialog.show()
        }
}
