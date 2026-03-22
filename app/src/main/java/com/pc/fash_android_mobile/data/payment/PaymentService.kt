package com.pc.fash_android_mobile.data.payment

import kotlinx.coroutines.delay

/**
 * Abstraction for payment processing. Replace [MockPaymentService] with real
 * implementation (e.g. MoMo/VNPay/ZaloPay SDK or backend API) when integrating.
 */
interface PaymentService {

    /**
     * Processes the payment. In mock: simulates success after short delay.
     * In real: calls payment gateway and returns actual result.
     */
    suspend fun processPayment(request: PaymentRequest): Result<PaymentResult>
}

data class PaymentRequest(
    val listingId: String,
    val productTitle: String,
    val productPriceVnd: Long,
    val platformFeeVnd: Long,
    val totalAmountVnd: Long,
    val address: CheckoutAddress,
    val paymentMethodId: String,
)

data class CheckoutAddress(
    val fullName: String,
    val phone: String,
    val address: String,
    val district: String,
    val city: String,
)

data class PaymentResult(
    val transactionId: String,
    val status: String,
    val message: String? = null,
)

/**
 * Temporary mock implementation. Simulates successful payment.
 * Replace with real PaymentService implementation when API is available.
 */
class MockPaymentService : PaymentService {

    override suspend fun processPayment(request: PaymentRequest): Result<PaymentResult> {
        delay(800) // Simulate network delay
        return Result.success(
            PaymentResult(
                transactionId = "mock_${System.currentTimeMillis()}",
                status = "success",
                message = "Thanh toán thành công (mock)",
            ),
        )
    }
}
