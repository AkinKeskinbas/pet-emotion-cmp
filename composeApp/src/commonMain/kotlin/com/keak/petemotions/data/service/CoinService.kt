package com.keak.petemotions.data.service

import com.keak.petemotions.data.model.CoinPackage
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.ktx.SuccessfulPurchase
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitGetProducts
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.CustomerInfo as RevenueCatCustomerInfo
import com.revenuecat.purchases.kmp.models.Offerings as RevenueCatOfferings
import com.revenuecat.purchases.kmp.models.Package as RevenueCatPackage
import com.revenuecat.purchases.kmp.models.PackageType
import com.revenuecat.purchases.kmp.models.PurchasesException
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import com.revenuecat.purchases.kmp.models.Store
import com.revenuecat.purchases.kmp.models.StoreProduct
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class CoinPackageOption(
    val coinPackage: CoinPackage,
    val revenueCatPackage: RevenueCatPackage?
)

interface CoinService {
    suspend fun purchaseCoins(packageToPurchase: RevenueCatPackage): Result<PurchaseResult>
    suspend fun purchaseCoinsWithProductId(productId: String): Result<PurchaseResult>
    suspend fun restorePurchases(): Result<RevenueCatCustomerInfo>
    suspend fun validatePurchase(transactionId: String, productId: String): Result<Int>
    suspend fun getAvailablePackages(): Result<List<CoinPackageOption>>
}

class CoinServiceImpl(
    private val revenueCatService: RevenueCatService
) : CoinService {

    override suspend fun purchaseCoins(packageToPurchase: RevenueCatPackage): Result<PurchaseResult> {
        return revenueCatService.purchasePackage(packageToPurchase).map { result ->
            // Ensure we use the package identifier as productId for backend validation
            val correctedResult = result.copy(productId = packageToPurchase.identifier)
            println("CoinService: Product ID correction:")
            println("  - Original Product ID: ${result.productId}")
            println("  - Corrected Product ID: ${correctedResult.productId}")
            println("  - Package Identifier: ${packageToPurchase.identifier}")
            correctedResult
        }
    }

    override suspend fun purchaseCoinsWithProductId(productId: String): Result<PurchaseResult> {
        return revenueCatService.purchaseProduct(productId).map { result ->
            println("CoinService: Direct product purchase completed:")
            println("  - Product ID: ${result.productId}")
            println("  - Transaction ID: ${result.transactionId}")
            result
        }
    }

    override suspend fun restorePurchases(): Result<RevenueCatCustomerInfo> {
        return revenueCatService.restorePurchases()
    }

    override suspend fun validatePurchase(transactionId: String, productId: String): Result<Int> {
        return try {
            // Coin validation will be handled by backend, return 0 as placeholder
            // Backend should determine coin amount from product ID
            Result.success(0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAvailablePackages(): Result<List<CoinPackageOption>> {
        return try {
            if (!Purchases.isConfigured) {
                println("CoinService: RevenueCat not configured yet, returning empty list")
                return Result.success(emptyList())
            }

            val productIds = listOf("coin_mini", "coin_midi", "coin_mega")
            println("CoinService: Getting products from RevenueCat with IDs: $productIds")

            // Use callback-based API with timeout since awaitGetProducts hangs
            println("CoinService: Calling getProducts with callback...")
            val storeProducts = withTimeout(15000) { // 15 second timeout
                suspendCoroutine { continuation ->
                    Purchases.sharedInstance.getProducts(
                        productIds = productIds,
                        onError = { error ->
                            println("CoinService: ❌ Error getting products")
                            println("CoinService: Error message: ${error.message}")
                            println("CoinService: Error code: ${error.code}")
                            continuation.resumeWithException(Exception("Failed to get products: ${error.message}"))
                        },
                        onSuccess = { products ->
                            println("CoinService: ✅ Successfully retrieved ${products.size} products")
                            continuation.resume(products)
                        }
                    )
                }
            }
            println("CoinService: Retrieved ${storeProducts.size} products from RevenueCat")

            if (storeProducts.isEmpty()) {
                println("CoinService: ERROR - No products found from RevenueCat!")
                println("CoinService: Make sure products are created in Google Play Console and RevenueCat")
                println("CoinService: Required product IDs: $productIds")
                return Result.failure(Exception("No products available. Please configure products in Google Play Console and RevenueCat."))
            }

            val packages = storeProducts.map { storeProduct ->
                val productId = storeProduct.id
                // Map product IDs to coin amounts
                val coinAmount = when (productId) {
                    "coin_mini" -> 10
                    "coin_midi" -> 25
                    "coin_mega" -> 100
                    else -> extractCoinAmountFromProductId(productId)
                }
                val coinPackage = CoinPackage(
                    id = productId,
                    coinAmount = coinAmount,
                    price = storeProduct.price.formatted,
                    revenueCatProductId = productId,
                    isPopular = productId == "coin_midi", // midi is popular
                    bonusPercentage = 0
                )

                println("CoinService: Product $productId - Price: ${storeProduct.price.formatted}, Coin Amount: $coinAmount")

                CoinPackageOption(
                    coinPackage = coinPackage,
                    revenueCatPackage = null // We'll purchase directly with product ID
                )
            }

            println("CoinService: Created ${packages.size} packages with real prices")

            Result.success(packages)
        } catch (e: Exception) {
            println("CoinService: Error creating packages from product IDs: ${e.message}")
            println("CoinService: Exception type: ${e::class.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}

interface RevenueCatService {
    suspend fun getCustomerInfo(): RevenueCatCustomerInfo
    suspend fun purchasePackage(packageToPurchase: RevenueCatPackage): Result<PurchaseResult>
    suspend fun purchaseProduct(productId: String): Result<PurchaseResult>
    suspend fun restorePurchases(): Result<RevenueCatCustomerInfo>
    suspend fun getOfferings(): Result<RevenueCatOfferings>
}

class RevenueCatServiceImpl : RevenueCatService {

    private fun logRevenueCatInfo() {
        if (Purchases.isConfigured) {
            println("RevenueCatServiceImpl: Using existing RevenueCat configuration")
            println("RevenueCat Environment Info:")
            println("  - User ID: ${Purchases.sharedInstance.appUserID}")
            println("  - Is Anonymous: ${Purchases.sharedInstance.isAnonymous}")
            println("  - Store: ${Purchases.sharedInstance.store}")
        } else {
            println("RevenueCatServiceImpl: RevenueCat not yet configured - will be configured during app initialization")
        }
    }

    override suspend fun getCustomerInfo(): RevenueCatCustomerInfo {
        logRevenueCatInfo()
        if (!Purchases.isConfigured) {
            throw IllegalStateException("RevenueCat is not configured yet. Please wait for initialization to complete.")
        }
        return try {
            val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
            println("RevenueCat Customer Info:")
            println("  - Original App Version: ${customerInfo.originalApplicationVersion}")
            println("  - Entitlements count: ${customerInfo.entitlements.active.size}")
            customerInfo
        } catch (e: PurchasesException) {
            println("RevenueCat: Failed to get customer info: ${e.message}")
            throw e
        } catch (e: Exception) {
            println("RevenueCat: Unexpected error getting customer info: ${e.message}")
            throw e
        }
    }

    override suspend fun purchasePackage(
        packageToPurchase: RevenueCatPackage
    ): Result<PurchaseResult> {
        return try {
            val purchase = Purchases.sharedInstance.awaitPurchase(packageToPurchase)
            Result.success(purchase.toPurchaseResult())
        } catch (e: PurchasesTransactionException) {
            println("RevenueCat: Purchase failed with transaction error: ${e.error.message}")
            Result.failure(e)
        } catch (e: CancellationException) {
            println("RevenueCat: Purchase cancelled")
            Result.failure(e)
        } catch (e: Exception) {
            println("RevenueCat: Purchase failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun purchaseProduct(productId: String): Result<PurchaseResult> {
        return try {
            println("RevenueCat: Purchasing product directly: $productId")

            // Get product details first using awaitGetProducts
            val storeProducts = Purchases.sharedInstance.awaitGetProducts(listOf(productId))
            if (storeProducts.isEmpty()) {
                return Result.failure(Exception("Product $productId not found in store"))
            }

            val storeProduct = storeProducts.first()
            println("RevenueCat: Found product ${storeProduct.id} with price ${storeProduct.price.formatted}")

            // Purchase directly with store product
            val purchase = Purchases.sharedInstance.awaitPurchase(storeProduct)
            Result.success(purchase.toPurchaseResult())
        } catch (e: PurchasesTransactionException) {
            println("RevenueCat: Product purchase failed with transaction error: ${e.error.message}")
            Result.failure(e)
        } catch (e: CancellationException) {
            println("RevenueCat: Product purchase cancelled")
            Result.failure(e)
        } catch (e: Exception) {
            println("RevenueCat: Product purchase failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun restorePurchases(): Result<RevenueCatCustomerInfo> {
        return try {
            val customerInfo = Purchases.sharedInstance.awaitRestore()
            Result.success(customerInfo)
        } catch (e: PurchasesException) {
            println("RevenueCat: Restore failed: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            println("RevenueCat: Unexpected error restoring purchases: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getOfferings(): Result<RevenueCatOfferings> {
        return try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            Result.success(offerings)
        } catch (e: PurchasesException) {
            println("RevenueCat: Failed to fetch offerings: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            println("RevenueCat: Unexpected error fetching offerings: ${e.message}")
            Result.failure(e)
        }
    }
}

data class PurchaseResult(
    val transactionId: String,
    val productId: String,
    val purchaseDateMillis: Long,
    val platform: String,
    val receipt: String? = null,
    val customerInfo: RevenueCatCustomerInfo? = null
)

private fun RevenueCatPackage.toCoinPackageOption(): CoinPackageOption {
    val productId = storeProduct.id
    val coinAmount = extractCoinAmountFromProductId(identifier) // Use identifier instead of productId for coin amount
    val coinPackage = CoinPackage(
        id = identifier,
        coinAmount = coinAmount,
        price = storeProduct.price.formatted,
        revenueCatProductId = identifier, // Use package identifier instead of store product ID
        isPopular = packageType.isPopular() || identifier.contains("popular", ignoreCase = true),
        bonusPercentage = 0 // No bonus calculation, just base coins
    )
    return CoinPackageOption(
        coinPackage = coinPackage,
        revenueCatPackage = this
    )
}

private fun PackageType.isPopular(): Boolean = when (this) {
    PackageType.ANNUAL,
    PackageType.THREE_MONTH,
    PackageType.SIX_MONTH -> true
    else -> false
}

private fun extractCoinAmountFromProductId(productId: String): Int {
    // Extract numbers from product ID (e.g., "pet_emotions_coins_25" -> 25)
    val numberRegex = Regex("(\\d+)")
    val matches = numberRegex.findAll(productId)

    // Get the largest number found (usually the coin amount)
    val numbers = matches.map { it.value.toInt() }.toList()
    return numbers.maxOrNull() ?: 0 // Return 0 if no number found, no mock data
}


private fun SuccessfulPurchase.toPurchaseResult(): PurchaseResult {
    val transactionId = storeTransaction.transactionId.orEmpty()
    val productId = storeTransaction.productIds.firstOrNull().orEmpty()
    val platform = mapStoreToPlatform(Purchases.sharedInstance.store)

    // Log purchase details for debugging
    println("=== PURCHASE DETAILS ===")
    println("Transaction ID: $transactionId")
    println("Product ID: $productId")
    println("Product IDs: ${storeTransaction.productIds}")
    println("Platform: $platform")
    println("Purchase Time: ${storeTransaction.purchaseTime}")
    println("Store: ${Purchases.sharedInstance.store}")
    println("Store Transaction Type: ${storeTransaction::class.simpleName}")

    // Store transaction details
    try {
        val storeTransactionString = storeTransaction.toString()
        println("Store Transaction Full: $storeTransactionString")
    } catch (e: Exception) {
        println("Error inspecting StoreTransaction: ${e.message}")
    }

    // Try to get receipt data from customer info
    try {
        println("Customer Info Details:")
        println("  Original App Version: ${customerInfo.originalApplicationVersion}")

        // Check non-subscription purchases for purchase token
        val nonSubscriptionTransactions = customerInfo.nonSubscriptionTransactions
        println("  Non-Subscription Transactions: ${nonSubscriptionTransactions.size}")

        if (nonSubscriptionTransactions.isNotEmpty()) {
            val latestTransaction = nonSubscriptionTransactions.last()
            println("    Latest Transaction: $latestTransaction")
            println("    Latest Transaction Type: ${latestTransaction::class.simpleName}")

            // Get available transaction info
            println("    Transaction Details:")
            println("      Product ID: ${latestTransaction.productIdentifier}")
            println("      Transaction ID: ${latestTransaction.transactionIdentifier}")
            println("      Purchase Date: ${latestTransaction.purchaseDateMillis}")
        }

    } catch (e: Exception) {
        println("Error accessing CustomerInfo: ${e.message}")
    }

    println("======================")

    // For Android, try to get actual purchase token from latest transaction
    val actualReceiptToken = if (platform == "android" && customerInfo.nonSubscriptionTransactions.isNotEmpty()) {
        val latestTransaction = customerInfo.nonSubscriptionTransactions.last()
        // Check if this transaction matches current purchase
        if (latestTransaction.productIdentifier == productId) {
            // Try to extract purchase token - RevenueCat might store it in transactionIdentifier for Android
            latestTransaction.transactionIdentifier
        } else {
            transactionId
        }
    } else {
        transactionId
    }

    println(">>> FINAL RECEIPT TOKEN: $actualReceiptToken")

    return PurchaseResult(
        transactionId = transactionId,
        productId = productId,
        purchaseDateMillis = storeTransaction.purchaseTime,
        platform = platform,
        receipt = actualReceiptToken, // Use actual purchase token for Android
        customerInfo = customerInfo
    )
}

private fun mapStoreToPlatform(store: Store): String = when (store) {
    Store.APP_STORE,
    Store.MAC_APP_STORE -> "ios"
    Store.PLAY_STORE,
    Store.AMAZON,
    Store.RC_BILLING -> "android"
    Store.STRIPE -> "stripe"
    Store.PROMOTIONAL -> "promo"
    Store.EXTERNAL,
    Store.UNKNOWN_STORE -> "unknown"
    else -> store.name.lowercase()
}
