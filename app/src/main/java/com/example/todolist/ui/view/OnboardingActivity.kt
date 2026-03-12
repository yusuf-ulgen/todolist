package com.example.todolist

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.todolist.databinding.ActivityOnboardingBinding
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Eğer daha önce gösterildiyse atla
        if (!PreferenceManager.isFirstLaunch(this)) {
            startActivity(Intent(this, Giris::class.java))
            finish()
            return
        }

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --------------- Burayı güncelliyoruz ---------------

        val onboardingPages = listOf(
            OnboardingPage(
                iconRes = R.drawable.ic_onboard_task,
                featureImageRes = R.drawable.onboard_task_feature,   // 1. ölçekli görsel
                title = "Görev Ekleme",
                description = "Adım adım kolayca yeni görevler ekleyin. Görev ekleyebilmek için sağ alttaki '+' ikonunu kullanın sonrasında görev bilgilerini girin (Saat bilgisi opsiyoneldir)."
            ),
            OnboardingPage(
                iconRes = R.drawable.ic_onboard_list,
                featureImageRes = R.drawable.onboard_list_feature,   // 2. ölçekli görsel
                title = "Listeleriniz",
                description = "Farklı listeler oluşturarak işlerinizi kategorilere ayırın, yönetimi basitleştirin. Sağ üstten de menüye ulaşabilirsiniz."
            ),
            OnboardingPage(
                iconRes = R.drawable.ic_onboard_stats,
                featureImageRes = R.drawable.onboard_stats_feature,   // 3. ölçekli görsel
                title = "İstatistikler",
                description = "Günlük ve haftalık istatistiklerinizi görüntüleyerek performansınızı analiz edin. Bunu kullanabilmek için resetleme zamanını ayarlamayı unutmayın! Resetleme zamanı vakti geldiğinde görevleri 'yapılmamış' durumuna getirir tekrardan."
            )
        )

        val onboardingAdapter = OnboardingAdapter(onboardingPages)
        binding.viewPager.adapter = onboardingAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ -> }.attach()

        binding.skipButton.setOnClickListener { completeOnboarding() }
        binding.nextButton.setOnClickListener {
            if (binding.viewPager.currentItem + 1 < onboardingAdapter.itemCount) {
                binding.viewPager.currentItem += 1
            } else {
                completeOnboarding()
            }
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            @SuppressLint("SetTextI18n")
            override fun onPageSelected(position: Int) {
                if (position == onboardingAdapter.itemCount - 1) {
                    binding.nextButton.text = "Başla"
                } else {
                    binding.nextButton.text = "İleri"
                }
            }
        })
    }

    private fun completeOnboarding() {
        PreferenceManager.setLaunched(this)
        startActivity(Intent(this, Giris::class.java))
        finish()
    }
}