package com.dealhunt.app.ui

import androidx.lifecycle.*
import com.dealhunt.app.data.GameRepository
import com.dealhunt.app.data.TLRate
import com.dealhunt.app.model.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

sealed class S<out T> {
    object Idle : S<Nothing>()
    object Loading : S<Nothing>()
    data class Ok<T>(val data: T) : S<T>()
    data class Err(val msg: String) : S<Nothing>()
}

class MainViewModel : ViewModel() {
    private val repo = GameRepository()
    val search = MutableLiveData<S<List<GameSearchResult>>>(S.Idle)
    val deals  = MutableLiveData<S<List<DealDetail>>>(S.Loading)
    private var searchJob: Job? = null

    init { loadDeals() }

    fun loadDeals() {
        viewModelScope.launch {
            deals.value = S.Loading
            try {
                val list = repo.getFeaturedDeals()
                deals.value = if (list.isNotEmpty()) S.Ok(list) else S.Err("Firsat bulunamadi")
            } catch (e: Throwable) {
                deals.value = S.Err("Yuklenemedi")
            }
        }
    }

    fun search(q: String) {
        if (q.isBlank()) { search.value = S.Idle; return }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            search.value = S.Loading
            try {
                val r = repo.search(q)
                search.value = if (r.isNotEmpty()) S.Ok(r) else S.Err("'$q' bulunamadi")
            } catch (e: Throwable) {
                search.value = S.Err("Baglanti hatasi")
            }
        }
    }

    fun clearSearch() { searchJob?.cancel(); search.value = S.Idle }
    fun searchGenre(g: String) = search(g)
}

class DetailViewModel : ViewModel() {
    private val repo = GameRepository()
    val detail = MutableLiveData<S<GameDetailUiState>>(S.Loading)

    fun loadByGameId(gameId: String) {
        if (gameId.isBlank()) { detail.value = S.Err("Gecersiz oyun"); return }
        viewModelScope.launch {
            detail.value = S.Loading
            try {
                val (info, prices) = repo.getGamePrices(gameId)
                detail.value = S.Ok(buildState(info, prices))
            } catch (e: Throwable) {
                detail.value = S.Err("Detay yuklenemedi")
            }
        }
    }

    fun loadByDealId(dealId: String, title: String, thumbnail: String) {
        if (dealId.isBlank()) { detail.value = S.Err("Gecersiz firsat"); return }
        viewModelScope.launch {
            detail.value = S.Loading
            try {
                val (info, prices) = repo.getGamePricesByDeal(dealId, title, thumbnail)
                detail.value = S.Ok(buildState(info, prices))
            } catch (e: Throwable) {
                detail.value = S.Err("Detay yuklenemedi")
            }
        }
    }

    private fun buildState(info: GameInfo, prices: List<PlatformPrice>): GameDetailUiState {
        val fmt = SimpleDateFormat("dd MMM yyyy", Locale("tr"))
        val date = try {
            if (info.cheapestPriceEver.date > 0)
                fmt.format(Date(info.cheapestPriceEver.date * 1000)) else "Bilinmiyor"
        } catch (e: Throwable) { "Bilinmiyor" }

        return GameDetailUiState(
            title = info.info.title.ifBlank { "Oyun" },
            thumbnail = info.info.thumbnail,
            steamAppId = info.info.steamAppId,
            platformPrices = prices,
            cheapestEver = TLRate.fmt(info.cheapestPriceEver.price.toDoubleOrNull() ?: 0.0),
            cheapestEverDate = date
        )
    }
}
