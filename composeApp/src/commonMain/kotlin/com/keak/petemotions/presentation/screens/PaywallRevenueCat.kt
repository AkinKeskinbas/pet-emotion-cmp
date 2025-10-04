package com.keak.petemotions.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import com.keak.petemotions.data.api.BackendApiService
import com.keak.petemotions.presentation.navigation.HomeRoute
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.models.Store
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun PaywallRevenueCat(navController: NavController) {
    val backendApi: BackendApiService = koinInject()

    val listener = remember {
        object : com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallListener {
            override fun onPurchaseCompleted(customerInfo: com.revenuecat.purchases.kmp.models.CustomerInfo, storeTransaction: com.revenuecat.purchases.kmp.models.StoreTransaction) {
                println("[Paywall] Purchase completed, validating with backend...")

                val productId = storeTransaction.productIds.firstOrNull() ?: ""
                val transactionId = storeTransaction.transactionId ?: ""
                val platform = when (Purchases.sharedInstance.store) {
                    Store.APP_STORE, Store.MAC_APP_STORE -> "ios"
                    Store.PLAY_STORE -> "android"
                    else -> "android"
                }

                println("[Paywall] Purchase Details:")
                println("  - Product ID: $productId")
                println("  - Transaction ID: $transactionId")
                println("  - Platform: $platform")
                println("  - Store Transaction Type: ${storeTransaction::class.simpleName}")
                println("  - Store Transaction: $storeTransaction")
                println("  - Product IDs: ${storeTransaction.productIds}")
                println("  - Purchase Time: ${storeTransaction.purchaseTime}")

                // For Android Google Play, the transactionId should be the purchase token
                // For testing, we'll use the transactionId as-is
                val purchaseToken = transactionId

                println("  - Purchase Token (for backend): $purchaseToken")
                println("  - Purchase Token Length: ${purchaseToken.length}")

                CoroutineScope(Dispatchers.Main).launch {
                    println("[Paywall] Sending validation request to backend...")
                    backendApi.validatePurchase(
                        platform = platform,
                        receipt = purchaseToken,
                        transactionId = transactionId,
                        productId = productId
                    ).fold(
                        onSuccess = { response ->
                            println("[Paywall] ✅ Purchase validated successfully!")
                            println("  - Coins added: ${response.coinsAdded}")
                            println("  - New balance: ${response.newBalance}")
                        },
                        onFailure = { error ->
                            println("[Paywall] ❌ Purchase validation failed: ${error.message}")
                            println("[Paywall] Note: Test purchases may fail validation but RevenueCat already processed the purchase")
                        }
                    )
                }
            }

            override fun onPurchaseError(error: com.revenuecat.purchases.kmp.models.PurchasesError) {
                println("[Paywall] Purchase error: ${error.message}")
            }

            override fun onRestoreCompleted(customerInfo: com.revenuecat.purchases.kmp.models.CustomerInfo) {
                println("[Paywall] Restore completed")
            }

            override fun onRestoreError(error: com.revenuecat.purchases.kmp.models.PurchasesError) {
                println("[Paywall] Restore error: ${error.message}")
            }
        }
    }

    val options = remember(listener) {
        PaywallOptions.Builder(dismissRequest = { navController.navigate(HomeRoute) }).apply {
            this.listener = listener
        }.build()
    }

    Paywall(options)
}