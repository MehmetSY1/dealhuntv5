package com.dealhunt.app.ui

import android.view.*
import androidx.recyclerview.widget.*
import com.bumptech.glide.Glide
import com.dealhunt.app.R
import com.dealhunt.app.data.Stores
import com.dealhunt.app.data.TLRate
import com.dealhunt.app.databinding.*
import com.dealhunt.app.model.*

class GameAdapter(private val onClick: (gameId: String, title: String, thumb: String) -> Unit) :
    ListAdapter<GameSearchResult, GameAdapter.VH>(object : DiffUtil.ItemCallback<GameSearchResult>() {
        override fun areItemsTheSame(a: GameSearchResult, b: GameSearchResult) = a.gameId == b.gameId
        override fun areContentsTheSame(a: GameSearchResult, b: GameSearchResult) = a == b
    }) {
    inner class VH(val b: ItemSearchResultBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(g: GameSearchResult) {
            b.tvGameTitle.text = g.title
            b.tvPrice.text = TLRate.fmt(g.cheapest.toDoubleOrNull() ?: 0.0)
            Glide.with(b.root).load(g.thumbnail).placeholder(R.drawable.placeholder_game).centerCrop().into(b.ivThumbnail)
            b.root.setOnClickListener {
                val id = g.gameId.ifBlank { g.cheapestDealId }
                if (id.isNotBlank()) onClick(id, g.title, g.thumbnail)
            }
        }
    }
    override fun onCreateViewHolder(p: ViewGroup, v: Int) = VH(ItemSearchResultBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, i: Int) = h.bind(getItem(i))
}

class DealAdapter(private val onClick: (dealId: String, title: String, thumb: String) -> Unit) :
    ListAdapter<DealDetail, DealAdapter.VH>(object : DiffUtil.ItemCallback<DealDetail>() {
        override fun areItemsTheSame(a: DealDetail, b: DealDetail) = a.dealId == b.dealId
        override fun areContentsTheSame(a: DealDetail, b: DealDetail) = a == b
    }) {
    inner class VH(val b: ItemFeaturedDealBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(d: DealDetail) {
            b.tvTitle.text = d.title.ifBlank { "Oyun" }
            b.tvSalePrice.text = TLRate.fmt(d.salePrice.toDoubleOrNull() ?: 0.0)
            b.tvNormalPrice.text = TLRate.fmt(d.normalPrice.toDoubleOrNull() ?: 0.0)
            val sav = d.savings.toDoubleOrNull() ?: 0.0
            b.tvSavings.text = "-%${sav.toInt()}"
            b.tvSavings.visibility = if (sav > 0) View.VISIBLE else View.GONE
            b.tvPlatformBadge.text = Stores.name(d.storeId)
            Glide.with(b.root).load(d.thumbnail).placeholder(R.drawable.placeholder_game).centerCrop().into(b.ivThumb)
            b.root.setOnClickListener {
                if (d.dealId.isNotBlank()) onClick(d.dealId, d.title, d.thumbnail)
            }
        }
    }
    override fun onCreateViewHolder(p: ViewGroup, v: Int) = VH(ItemFeaturedDealBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, i: Int) = h.bind(getItem(i))
}

class PriceAdapter(private val onBuy: (PlatformPrice) -> Unit) :
    ListAdapter<PlatformPrice, PriceAdapter.VH>(object : DiffUtil.ItemCallback<PlatformPrice>() {
        override fun areItemsTheSame(a: PlatformPrice, b: PlatformPrice) = a.dealId == b.dealId
        override fun areContentsTheSame(a: PlatformPrice, b: PlatformPrice) = a == b
    }) {
    inner class VH(val b: ItemPlatformPriceBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(p: PlatformPrice) {
            b.tvStoreName.text = p.storeName
            b.tvCurrentPrice.text = TLRate.fmt(p.currentPrice)
            if (p.originalPrice > p.currentPrice && p.savingsPercent > 0) {
                b.tvOriginalPrice.text = TLRate.fmt(p.originalPrice)
                b.tvOriginalPrice.visibility = View.VISIBLE
                b.tvDiscount.text = "-%${p.savingsPercent.toInt()}"
                b.tvDiscount.visibility = View.VISIBLE
            } else {
                b.tvOriginalPrice.visibility = View.GONE
                b.tvDiscount.visibility = View.GONE
            }
            b.badgeBest.visibility = if (p.isBestDeal) View.VISIBLE else View.GONE
            b.tvCurrentPrice.setTextColor(b.root.context.getColor(
                if (p.isBestDeal) R.color.accent_green else R.color.text_primary))
            b.root.setBackgroundResource(
                if (p.isBestDeal) R.drawable.bg_best_deal_card else R.drawable.bg_platform_card)
            if (p.logoUrl.isNotEmpty()) Glide.with(b.root).load(p.logoUrl).into(b.ivStoreLogo)
            b.btnBuy.setOnClickListener { onBuy(p) }
        }
    }
    override fun onCreateViewHolder(p: ViewGroup, v: Int) = VH(ItemPlatformPriceBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, i: Int) = h.bind(getItem(i))
}
