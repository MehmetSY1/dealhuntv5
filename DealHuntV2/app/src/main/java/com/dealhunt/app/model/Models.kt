package com.dealhunt.app.model
import com.google.gson.annotations.SerializedName

data class GameSearchResult(
    @SerializedName("gameID") val gameId: String = "",
    @SerializedName("steamAppID") val steamAppId: String? = null,
    @SerializedName("cheapest") val cheapest: String = "0",
    @SerializedName("cheapestDealID") val cheapestDealId: String = "",
    @SerializedName("external") val title: String = "",
    @SerializedName("thumb") val thumbnail: String = "",
    @SerializedName("internalName") val internalName: String = ""
)

data class DealDetail(
    @SerializedName("dealID") val dealId: String = "",
    @SerializedName("storeID") val storeId: String = "",
    @SerializedName("gameID") val gameId: String = "",
    @SerializedName("salePrice") val salePrice: String = "0",
    @SerializedName("normalPrice") val normalPrice: String = "0",
    @SerializedName("savings") val savings: String = "0",
    @SerializedName("title") val title: String = "",
    @SerializedName("thumb") val thumbnail: String = ""
)

// /deals?id={dealID} tek bir NESNE döndürür, dizi değil — farklı şekli var
data class SingleDealResponse(
    @SerializedName("gameInfo") val gameInfo: SingleDealGameInfo = SingleDealGameInfo()
)

data class SingleDealGameInfo(
    @SerializedName("storeID") val storeId: String = "",
    @SerializedName("gameID") val gameId: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("salePrice") val salePrice: String = "0",
    @SerializedName("retailPrice") val retailPrice: String = "0",
    @SerializedName("steamAppID") val steamAppId: String? = null,
    @SerializedName("thumb") val thumbnail: String = ""
)

data class Store(
    @SerializedName("storeID") val storeId: String = "",
    @SerializedName("storeName") val storeName: String = "",
    @SerializedName("isActive") val isActive: Int = 0,
    @SerializedName("images") val images: StoreImages = StoreImages()
)

data class StoreImages(
    @SerializedName("logo") val logo: String = "",
    @SerializedName("icon") val icon: String = ""
)

data class GameInfo(
    @SerializedName("info") val info: GameInfoDetail = GameInfoDetail(),
    @SerializedName("cheapestPriceEver") val cheapestPriceEver: CheapestPriceEver = CheapestPriceEver(),
    @SerializedName("deals") val deals: List<GameDealItem> = emptyList()
)

data class GameInfoDetail(
    @SerializedName("title") val title: String = "",
    @SerializedName("steamAppID") val steamAppId: String? = null,
    @SerializedName("thumb") val thumbnail: String = ""
)

data class CheapestPriceEver(
    @SerializedName("price") val price: String = "0",
    @SerializedName("date") val date: Long = 0
)

data class GameDealItem(
    @SerializedName("storeID") val storeId: String = "",
    @SerializedName("dealID") val dealId: String = "",
    @SerializedName("price") val price: String = "0",
    @SerializedName("retailPrice") val retailPrice: String = "0",
    @SerializedName("savings") val savings: String = "0"
)

data class PlatformPrice(
    val storeId: String = "",
    val storeName: String = "",
    val logoUrl: String = "",
    val currentPrice: Double = 0.0,
    val originalPrice: Double = 0.0,
    val savingsPercent: Double = 0.0,
    val dealId: String = "",
    val isBestDeal: Boolean = false
)

data class GameDetailUiState(
    val title: String = "",
    val thumbnail: String = "",
    val steamAppId: String? = null,
    val platformPrices: List<PlatformPrice> = emptyList(),
    val cheapestEver: String = "",
    val cheapestEverDate: String = ""
)
