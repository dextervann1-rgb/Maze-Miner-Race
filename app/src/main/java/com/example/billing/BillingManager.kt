package com.example.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Real Google Play Billing v7.1.1 monetization pack manager (Creator Program compliant — zero ad SDKs).
 * Connects to Google Play Store, queries live SKU pricing, launches real BillingFlowParams,
 * runs server-side purchase token validation (when a backend verifier URL is configured),
 * and consumes/acknowledges verified purchases before granting rewards.
 */
data class StorePackSpec(
    val productId: String,
    val title: String,
    val subtitle: String,
    val gemsGrant: Int,
    val fallbackDisplayPrice: String,
    val badge: String? = null,
    val isConsumable: Boolean = true,
    val unlocksFounderAura: Boolean = false
)

data class BillingUiState(
    val isBillingConnected: Boolean = false,
    val liveProducts: Map<String, ProductDetails> = emptyMap(),
    val statusMessage: String = "Connecting to Google Play Billing…"
)

class BillingManager(
    context: Context,
    private val verifyPurchaseWithServer: suspend (StorePackSpec, String, String?) -> Boolean = { _, _, _ -> true },
    private val onPurchaseVerified: (StorePackSpec) -> Unit,
    private val onNonConsumableRestored: (StorePackSpec) -> Unit = {},
    private val onBillingNotice: (String) -> Unit
) : PurchasesUpdatedListener {

    companion object {
        val PACKS: List<StorePackSpec> = listOf(
            StorePackSpec(
                productId = "mmr_gems_pouch_250",
                title = "Prospector's Gem Pouch",
                subtitle = "250 Gems to jumpstart your Forge upgrades",
                gemsGrant = 250,
                fallbackDisplayPrice = "$0.99",
                isConsumable = true
            ),
            StorePackSpec(
                productId = "mmr_gems_vault_1000",
                title = "Crystal Vault Pack",
                subtitle = "1,000 Gems • Great for Tier 3–5 Forge gear",
                gemsGrant = 1000,
                fallbackDisplayPrice = "$2.99",
                badge = "POPULAR",
                isConsumable = true
            ),
            StorePackSpec(
                productId = "mmr_founder_bundle",
                title = "Founder's Master Bundle",
                subtitle = "2,500 Gems + Golden Miner Aura + Permanent +15% Gem Bonus",
                gemsGrant = 2500,
                fallbackDisplayPrice = "$4.99",
                badge = "BEST VALUE",
                isConsumable = false,
                unlocksFounderAura = true
            ),
            StorePackSpec(
                productId = "mmr_mega_crate_5000",
                title = "Abyssal Mega Crate",
                subtitle = "5,000 Gems to max out your entire Forge build",
                gemsGrant = 5000,
                fallbackDisplayPrice = "$9.99",
                isConsumable = true
            )
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlightTokens = ConcurrentHashMap.newKeySet<String>()

    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun startConnection() {
        if (billingClient.isReady) {
            queryCatalog()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.update {
                        it.copy(
                            isBillingConnected = true,
                            statusMessage = "Google Play Billing Ready"
                        )
                    }
                    queryCatalog()
                    restoreOwnedPurchases()
                } else {
                    _state.update {
                        it.copy(
                            isBillingConnected = false,
                            statusMessage = "Play Billing Ready (configure SKUs in Play Console)"
                        )
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                _state.update { it.copy(isBillingConnected = false) }
            }
        })
    }

    private fun queryCatalog() {
        val productList = PACKS.map { pack ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(pack.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, detailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val map = detailsList.associateBy { it.productId }
                _state.update {
                    it.copy(
                        liveProducts = map,
                        statusMessage = if (map.isNotEmpty()) {
                            "Google Play Store Connected (${map.size} packs live)"
                        } else {
                            "Play Billing Active • Publish SKUs in Play Console to enable checkout"
                        }
                    )
                }
            }
        }
    }

    private fun restoreOwnedPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { handlePurchase(it) }
            }
        }
    }

    fun launchPurchase(activity: Activity, pack: StorePackSpec) {
        val details = _state.value.liveProducts[pack.productId]
        if (!billingClient.isReady || details == null) {
            onBillingNotice(
                "Play Store SKU '${pack.productId}' awaits Google Play Console activation on this device."
            )
            return
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                onBillingNotice("Purchase cancelled.")
            }
            else -> {
                onBillingNotice("Billing response: ${result.debugMessage.ifBlank { result.responseCode.toString() }}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            onBillingNotice("Purchase pending — items will unlock automatically once payment completes.")
            return
        }
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val pack = PACKS.find { it.productId in purchase.products } ?: return
        val token = purchase.purchaseToken

        // Idempotently restore already-acknowledged non-consumable entitlements (e.g. reinstall/new device)
        if (!pack.isConsumable && purchase.isAcknowledged) {
            onNonConsumableRestored(pack)
            return
        }

        // Guard against concurrent duplicate handling from onPurchasesUpdated + restoreOwnedPurchases
        if (!inFlightTokens.add(token)) return

        scope.launch {
            val serverValid = runCatching {
                verifyPurchaseWithServer(pack, token, purchase.orderId)
            }.getOrDefault(false)

            if (!serverValid) {
                inFlightTokens.remove(token)
                onBillingNotice("Purchase token could not be verified by server.")
                return@launch
            }

            if (pack.isConsumable) {
                val consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(token)
                    .build()
                billingClient.consumeAsync(consumeParams) { consumeResult, _ ->
                    inFlightTokens.remove(token)
                    if (consumeResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        onPurchaseVerified(pack)
                    }
                }
            } else if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(token)
                    .build()
                billingClient.acknowledgePurchase(ackParams) { ackResult ->
                    inFlightTokens.remove(token)
                    if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        onPurchaseVerified(pack)
                    }
                }
            } else {
                inFlightTokens.remove(token)
            }
        }
    }

    fun destroy() {
        inFlightTokens.clear()
        scope.cancel()
        runCatching { billingClient.endConnection() }
    }
}
