package wingzone.zenith.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Repository for payment-related operations
 */
class PaymentRepository {
    
    companion object {
        private const val TAG = "PaymentRepository"
        private const val BASE_URL = "https://us-central1-wingzone-app.cloudfunctions.net"
    }
    
    /**
     * Create a ToyyibPay bill for the given order
     * @return Payment URL for the WebView, or null if failed
     */
    suspend fun createToyyibPayBill(
        orderId: String,
        customerName: String,
        customerEmail: String,
        totalAmount: Double,
        customerPhone: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/createToyyibPayBill")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            // Prepare request body
            val requestBody = JSONObject().apply {
                put("orderId", orderId)
                put("customerName", customerName)
                put("customerEmail", customerEmail)
                put("totalAmount", totalAmount)
                // Include phone if provided (fallback handled by Cloud Function)
                if (!customerPhone.isNullOrBlank()) {
                    put("customerPhone", customerPhone)
                }
            }
            
            // Send request
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }
            
            // Log exact payload for diagnostics
            Log.d(TAG, "Request payload: $requestBody")

            // Read response
            val responseCode = connection.responseCode
            Log.d(TAG, "Cloud Function HTTP status: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.readText()
                }
                Log.d(TAG, "Cloud Function raw response: $response")

                val jsonResponse = JSONObject(response)

                if (jsonResponse.optBoolean("success", false)) {
                    val paymentUrl = jsonResponse.getString("paymentUrl")
                    Log.d(TAG, "Payment URL created successfully: $paymentUrl")
                    Result.success(paymentUrl)
                } else {
                    val error = jsonResponse.optString("error", "Unknown error")
                    val details = jsonResponse.optString("details", "")
                    val rawBody = jsonResponse.optString("rawBody", "")
                    Log.e(TAG, "Payment API error: $error | details: $details | rawBody: $rawBody")
                    Result.failure(Exception("Payment creation failed: $error"))
                }
            } else {
                val errorStream = connection.errorStream
                val rawErrorBody = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                } else {
                    "(no error body)"
                }
                Log.e(TAG, "HTTP $responseCode — raw error body: $rawErrorBody")
                Result.failure(Exception("HTTP $responseCode: $rawErrorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating payment bill", e)
            Result.failure(e)
        }
    }
}
