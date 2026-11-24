package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class HealthResponseDto(
    val ok: Boolean = false,
    val version: String? = null,
    val db: String? = null,
    val leaderboard: Int? = null
)

@JsonClass(generateAdapter = true)
data class SessionResponseDto(
    val sessionId: String,
    val seed: String,
    val token: String
)

@JsonClass(generateAdapter = true)
data class ActionWireDto(
    val t: String,
    val dx: Int? = null,
    val dy: Int? = null,
    val x: Int? = null,
    val y: Int? = null,
    val tick: Int
)

@JsonClass(generateAdapter = true)
data class RunSubmissionDto(
    val sessionId: String,
    val token: String,
    val seed: String,
    val version: String,
    val difficulty: String,
    val modifier: String,
    val biome: String,
    val actions: List<ActionWireDto>,
    val ticks: Int,
    val clientScore: Int
)

@JsonClass(generateAdapter = true)
data class RunVerifyResponseDto(
    val ok: Boolean = false,
    val verified: Boolean = false,
    val serverScore: Int? = null,
    val rank: Int? = null,
    val fingerprint: String? = null,
    val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class PurchaseVerifyRequestDto(
    val productId: String,
    val purchaseToken: String,
    val packageName: String,
    val orderId: String? = null
)

@JsonClass(generateAdapter = true)
data class PurchaseVerifyResponseDto(
    val ok: Boolean = false,
    val verified: Boolean = false,
    val entitlement: String? = null,
    val reason: String? = null
)

interface VerifierApiService {
    @GET("api/health")
    suspend fun health(): HealthResponseDto

    @POST("api/session")
    suspend fun createSession(): SessionResponseDto

    @POST("api/run")
    suspend fun submitRun(@Body submission: RunSubmissionDto): RunVerifyResponseDto

    /**
     * Server-side Google Play Developer API (`purchases.products.get`) token validation endpoint
     * executed prior to granting permanent entitlements or consumable gem packs.
     */
    @POST("api/billing/verify")
    suspend fun verifyPurchase(@Body request: PurchaseVerifyRequestDto): PurchaseVerifyResponseDto
}

object VerifierApiFactory {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build()

    fun create(baseUrl: String): VerifierApiService? {
        val trimmed = baseUrl.trim()
        if (trimmed.isEmpty()) return null
        val normalized = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        return runCatching {
            Retrofit.Builder()
                .baseUrl(normalized)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(VerifierApiService::class.java)
        }.getOrNull()
    }
}
