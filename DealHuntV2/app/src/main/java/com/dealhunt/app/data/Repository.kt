package com.dealhunt.app.data

import com.dealhunt.app.model.*
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.net.URL
import java.util.concurrent.TimeUnit

interface CheapSharkApi {
    @GET("games")
    suspend fun searchGames(@Query("title") title: String, @Query("limit") limit: Int = 60): List<GameSearchResult>

    @GET("games")
    suspend fun getGameInfo(@Query("id") gameId: String): GameInfo

    @GET("deals")
    suspend fun getDeals(
        @Query("storeID") storeIds: String = "1,25,7,11,15,28,8,21",
        @Query("pageSize") pageSize: Int = 30,
        @Query("sortBy") sortBy: String = "Savings",
        @Query("onSale") onSale: Int = 1
    ): List<DealDetail>

    // Tek deal detayi - NESNE doner, dizi degil!
    @GET("deals")
    suspend fun getSingleDeal(@Query("id") dealId: String): SingleDealResponse

    @GET("stores")
    suspend fun getStores(): List<Store>
}

object NetworkClient {
    private const val BASE = "https://www.cheapshark.com/api/1.0/"
    private const val CDN = "https://www.cheapshark.com"

    val api: CheapSharkApi = Retrofit.Builder()
        .baseUrl(BASE)
        .client(OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CheapSharkApi::class.java)

    fun logoUrl(path: String) = "$CDN$path"
    fun dealUrl(id: String) = "https://www.cheapshark.com/redirect?dealID=$id"
}

object TLRate {
    private var rate: Double = 35.0
    private var lastFetch: Long = 0
    private const val TTL = 30 * 60 * 1000L

    fun getCached() = rate

    suspend fun fetch(): Double {
        val now = System.currentTimeMillis()
        if (rate > 0 && now - lastFetch < TTL) return rate
        val fresh = try {
            val j = URL("https://api.exchangerate-api.com/v4/latest/USD").readText()
            JSONObject(j).getJSONObject("rates").getDouble("TRY")
        } catch (e: Exception) {
            try {
                val j = URL("https://open.er-api.com/v6/latest/USD").readText()
                JSONObject(j).getJSONObject("rates").getDouble("TRY")
            } catch (e2: Exception) { 35.0 }
        }
        if (fresh > 0) { rate = fresh; lastFetch = now }
        return rate
    }

    fun fmt(usd: Double): String {
        if (usd <= 0.0) return "Ucretsiz"
        val tl = usd * rate
        return if (tl >= 1000) "\u20BA${String.format("%,.0f", tl)}" else "\u20BA${String.format("%.0f", tl)}"
    }
}

object Stores {
    private val map = mapOf(
        "1" to "Steam", "25" to "Epic Games", "7" to "GOG",
        "11" to "Humble Bundle", "15" to "Fanatical", "28" to "IndieGala",
        "8" to "GamersGate", "21" to "WinGameStore", "23" to "GameBillet",
        "13" to "Gamesplanet", "24" to "Voidu"
    )
    private var cache: Map<String, Store> = emptyMap()

    fun name(id: String) = cache[id]?.storeName ?: map[id] ?: "Platform $id"
    fun setCache(list: List<Store>) { cache = list.associateBy { it.storeId } }
    fun logoUrl(id: String, cdn: String) = cache[id]?.let { "$cdn${it.images.logo}" } ?: ""
}

class GameRepository {
    private val api = NetworkClient.api
    private var storesLoaded = false

    private suspend fun ensureStores() {
        if (!storesLoaded) {
            try { Stores.setCache(api.getStores()); storesLoaded = true } catch (_: Exception) {}
        }
    }

    suspend fun search(query: String): List<GameSearchResult> {
        ensureStores()
        return api.searchGames(query)
    }

    suspend fun getFeaturedDeals(): List<DealDetail> {
        ensureStores()
        TLRate.fetch()
        return api.getDeals()
    }

    // gameId ile detay (arama sonuçlarından)
    suspend fun getGamePrices(gameId: String): Pair<GameInfo, List<PlatformPrice>> {
        ensureStores()
        TLRate.fetch()
        val info = api.getGameInfo(gameId)
        val minPrice = info.deals.mapNotNull { it.price.toDoubleOrNull() }.minOrNull() ?: 0.0
        val prices = info.deals.map { deal ->
            val usd = deal.price.toDoubleOrNull() ?: 0.0
            val ret = deal.retailPrice.toDoubleOrNull() ?: usd
            PlatformPrice(
                storeId = deal.storeId,
                storeName = Stores.name(deal.storeId),
                logoUrl = Stores.logoUrl(deal.storeId, "https://www.cheapshark.com"),
                currentPrice = usd,
                originalPrice = ret,
                savingsPercent = deal.savings.toDoubleOrNull() ?: 0.0,
                dealId = deal.dealId,
                isBestDeal = usd == minPrice
            )
        }.sortedBy { it.currentPrice }
        return Pair(info, prices)
    }

    // dealId ile detay (ana sayfa fırsat kartlarından) — doğru endpoint şekliyle
    suspend fun getGamePricesByDeal(dealId: String, fallbackTitle: String, fallbackThumb: String): Pair<GameInfo, List<PlatformPrice>> {
        ensureStores()
        TLRate.fetch()

        val single = try { api.getSingleDeal(dealId) } catch (e: Exception) { null }
        val gameId = single?.gameInfo?.gameId ?: ""

        // gameId bulunduysa tam veri çek
        if (gameId.isNotBlank()) {
            try {
                return getGamePrices(gameId)
            } catch (_: Exception) {}
        }

        // gameId yoksa, tekil deal verisinden minimal bir görünüm oluştur
        val gi = single?.gameInfo
        val usd = gi?.salePrice?.toDoubleOrNull() ?: 0.0
        val retail = gi?.retailPrice?.toDoubleOrNull() ?: usd
        val storeId = gi?.storeId ?: "1"

        val prices = listOf(
            PlatformPrice(
                storeId = storeId,
                storeName = Stores.name(storeId),
                logoUrl = Stores.logoUrl(storeId, "https://www.cheapshark.com"),
                currentPrice = usd,
                originalPrice = retail,
                savingsPercent = if (retail > 0) ((retail - usd) / retail * 100) else 0.0,
                dealId = dealId,
                isBestDeal = true
            )
        )

        val info = GameInfo(
            info = GameInfoDetail(
                title = gi?.name?.ifBlank { fallbackTitle } ?: fallbackTitle,
                steamAppId = gi?.steamAppId,
                thumbnail = gi?.thumbnail?.ifBlank { fallbackThumb } ?: fallbackThumb
            ),
            cheapestPriceEver = CheapestPriceEver(price = usd.toString(), date = 0),
            deals = emptyList()
        )
        return Pair(info, prices)
    }
}
