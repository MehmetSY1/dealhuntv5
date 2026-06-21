package com.dealhunt.app.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.*
import com.dealhunt.app.R
import com.dealhunt.app.databinding.*
import com.dealhunt.app.ui.*
import com.dealhunt.app.util.WishlistManager

private fun Fragment.goByGameId(id: String, title: String, thumb: String) {
    if (id.isBlank()) return
    try {
        startActivity(
            Intent(requireContext(), DetailActivity::class.java).also {
                it.putExtra("GAME_ID", id)
                it.putExtra("GAME_TITLE", title)
                it.putExtra("GAME_THUMB", thumb)
            }
        )
    } catch (e: Throwable) {
        Toast.makeText(context, "Açılamadı", Toast.LENGTH_SHORT).show()
    }
}

private fun Fragment.goByDealId(id: String, title: String, thumb: String) {
    if (id.isBlank()) return
    try {
        startActivity(
            Intent(requireContext(), DetailActivity::class.java).also {
                it.putExtra("DEAL_ID", id)
                it.putExtra("GAME_TITLE", title)
                it.putExtra("GAME_THUMB", thumb)
            }
        )
    } catch (e: Throwable) {
        Toast.makeText(context, "Açılamadı", Toast.LENGTH_SHORT).show()
    }
}

class HomeFragment : Fragment() {
    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!
    private val vm: MainViewModel by viewModels()

    private val searchAd by lazy { GameAdapter { id, t, th -> goByGameId(id, t, th) } }
    private val dealsAd  by lazy { DealAdapter { id, t, th -> goByDealId(id, t, th) } }
    private val aiAd     by lazy { DealAdapter { id, t, th -> goByDealId(id, t, th) } }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentHomeBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(v: View, s: Bundle?) {
        b.rvSearch.layoutManager = LinearLayoutManager(context)
        b.rvSearch.adapter = searchAd
        b.rvDeals.layoutManager = GridLayoutManager(context, 2)
        b.rvDeals.adapter = dealsAd
        b.rvAiStrip.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        b.rvAiStrip.adapter = aiAd

        b.swipeRefresh.setOnRefreshListener { vm.loadDeals() }

        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b2: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b2: Int, c: Int) {}
            override fun afterTextChanged(e: Editable?) {
                val q = e?.toString()?.trim() ?: ""
                if (q.isEmpty()) vm.clearSearch() else vm.search(q)
                b.btnClear.visibility = if (q.isNotEmpty()) View.VISIBLE else View.GONE
            }
        })
        b.btnClear.setOnClickListener { b.etSearch.text?.clear(); vm.clearSearch() }

        vm.deals.observe(viewLifecycleOwner) { st ->
            b.swipeRefresh.isRefreshing = false
            if (st is S.Ok) {
                dealsAd.submitList(st.data)
                aiAd.submitList(st.data.take(8))
            }
        }

        vm.search.observe(viewLifecycleOwner) { st ->
            when (st) {
                is S.Idle -> { b.homeScroll.visibility = View.VISIBLE; b.rvSearch.visibility = View.GONE; b.tvSearchError.visibility = View.GONE; b.progressSearch.visibility = View.GONE }
                is S.Loading -> { b.homeScroll.visibility = View.GONE; b.rvSearch.visibility = View.GONE; b.tvSearchError.visibility = View.GONE; b.progressSearch.visibility = View.VISIBLE }
                is S.Ok -> { b.homeScroll.visibility = View.GONE; b.rvSearch.visibility = View.VISIBLE; b.tvSearchError.visibility = View.GONE; b.progressSearch.visibility = View.GONE; searchAd.submitList(st.data) }
                is S.Err -> { b.homeScroll.visibility = View.GONE; b.rvSearch.visibility = View.GONE; b.tvSearchError.visibility = View.VISIBLE; b.progressSearch.visibility = View.GONE; b.tvSearchError.text = st.msg }
            }
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

class ExploreFragment : Fragment() {
    private var _b: FragmentExploreBinding? = null
    private val b get() = _b!!
    private val vm: MainViewModel by viewModels()
    private lateinit var ad: GameAdapter
    private val genres = listOf("Trend","RPG","Aksiyon","Strateji","Indie","Yarış","Spor","FPS","Açık Dünya","Korku")
    private val queries = listOf("top","rpg","action","strategy","indie","racing","sports","fps","open world","horror")

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentExploreBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(v: View, s: Bundle?) {
        ad = GameAdapter { id, t, th -> goByGameId(id, t, th) }
        b.rvExplore.layoutManager = LinearLayoutManager(context)
        b.rvExplore.adapter = ad

        genres.forEachIndexed { i, label ->
            b.chipGroup.addView(TextView(context).apply {
                text = label; textSize = 13f; setPadding(28, 12, 28, 12)
                setTextColor(if (i == 0) Color.parseColor("#0A0C10") else Color.parseColor("#B0B8C8"))
                background = context?.getDrawable(if (i == 0) R.drawable.bg_chip_active else R.drawable.bg_chip)
                val lp = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.marginEnd = 8; layoutParams = lp
                setOnClickListener { pick(i) }
            })
        }

        vm.search.observe(viewLifecycleOwner) { st ->
            b.progressExplore.visibility = if (st is S.Loading) View.VISIBLE else View.GONE
            if (st is S.Ok) ad.submitList(st.data)
        }
        pick(0)
    }

    private fun pick(idx: Int) {
        for (i in 0 until b.chipGroup.childCount) {
            val chip = b.chipGroup.getChildAt(i) as? TextView ?: continue
            chip.setTextColor(Color.parseColor(if (i == idx) "#0A0C10" else "#B0B8C8"))
            chip.background = requireContext().getDrawable(if (i == idx) R.drawable.bg_chip_active else R.drawable.bg_chip)
        }
        vm.searchGenre(queries[idx])
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

class WishlistFragment : Fragment() {
    private var _b: FragmentWishlistBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentWishlistBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(v: View, s: Bundle?) {
        val ad = GameAdapter { id, t, th -> goByGameId(id, t, th) }
        b.rvWishlist.layoutManager = LinearLayoutManager(context)
        b.rvWishlist.adapter = ad
        val list = WishlistManager.getAll(requireContext())
        if (list.isEmpty()) { b.emptyView.visibility = View.VISIBLE; b.rvWishlist.visibility = View.GONE }
        else { b.emptyView.visibility = View.GONE; b.rvWishlist.visibility = View.VISIBLE; ad.submitList(list) }
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

class AiFragment : Fragment() {
    private var _b: FragmentAiBinding? = null
    private val b get() = _b!!
    private val vm: MainViewModel by viewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentAiBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(v: View, s: Bundle?) {
        val ad = DealAdapter { id, t, th -> goByDealId(id, t, th) }
        b.rvAi.layoutManager = GridLayoutManager(context, 2)
        b.rvAi.adapter = ad
        vm.deals.observe(viewLifecycleOwner) { if (it is S.Ok) ad.submitList(it.data.shuffled()) }
        vm.loadDeals()
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

class ProfileFragment : Fragment() {
    private var _b: FragmentProfileBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentProfileBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(v: View, s: Bundle?) {
        b.tvWishlistCount.text = WishlistManager.getAll(requireContext()).size.toString()
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
