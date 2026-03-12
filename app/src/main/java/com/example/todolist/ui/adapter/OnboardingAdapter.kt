package com.example.todolist

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.OnboardingPageBinding

class OnboardingAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(val binding: OnboardingPageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = OnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val page = pages[position]
        Log.d("OnboardingAdapter", "Binding position=$position, featureImageRes=${page.featureImageRes}")

        holder.binding.pageIcon.setImageResource(page.iconRes)
        holder.binding.pageTitle.text = page.title
        holder.binding.pageDescription.text = page.description

        // Yeni görseli burada atıyoruz:
        holder.binding.pageFeatureImage.setImageResource(page.featureImageRes)
        holder.binding.pageFeatureImage.visibility = View.VISIBLE
    }

    override fun getItemCount() = pages.size
}