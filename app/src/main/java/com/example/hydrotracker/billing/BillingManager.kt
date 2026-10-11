package com.example.hydrotracker.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*

class BillingManager(
    context: Context,
    private val onOwnedChanged: (Boolean) -> Unit,
    private val onStateChanged: (BillingState) -> Unit = {}
) {
    enum class BillingState { CONNECTING, AVAILABLE, UNAVAILABLE }

    companion object { const val PRODUCT_ID = "pro_unlock" }

    private val prefs = context.getSharedPreferences("hydration_prefs", Context.MODE_PRIVATE)

    private val client = BillingClient.newBuilder(context)
        .setListener { result, purchases ->
            if (result.responseCode == BillingResponseCode.OK) handle(purchases.orEmpty())
        }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun connect() {
        if (client.isReady) { checkOwned(); return }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) {
                if (r.responseCode == BillingResponseCode.OK) {
                    onStateChanged(BillingState.AVAILABLE); checkOwned()
                } else onStateChanged(BillingState.UNAVAILABLE)
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    fun purchase(activity: Activity) {
        if (!client.isReady) return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build()))
            .build()
        client.queryProductDetailsAsync(params) { _, details ->
            val d = details.firstOrNull() ?: return@queryProductDetailsAsync
            client.launchBillingFlow(activity, BillingFlowParams.newBuilder()
                .setProductDetailsParams(BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(d).build()).build())
        }
    }

    private fun checkOwned() {
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { _, purchases -> handle(purchases.orEmpty()) }
    }

    private fun handle(purchases: List<Purchase>) {
        val mine = purchases.filter { PRODUCT_ID in it.products }
        val owned = mine.any { it.purchaseState == PurchaseState.PURCHASED }
        mine.filter { it.purchaseState == PurchaseState.PURCHASED && !it.isAcknowledged }.forEach {
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build()) {}
        }
        prefs.edit().putBoolean("pro_owned", owned).apply()
        onOwnedChanged(owned)
    }

    fun cachedOwned(): Boolean = prefs.getBoolean("pro_owned", false)
    fun destroy() = client.endConnection()
}
