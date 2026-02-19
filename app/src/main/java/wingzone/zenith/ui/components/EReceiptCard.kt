package wingzone.zenith.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import wingzone.zenith.data.models.CartItem
import wingzone.zenith.data.repository.Order

/**
 * E-Receipt Card for Over-The-Counter Cash Payment
 * Displays QR code, detailed item list, and total price for cashier verification
 */
@Composable
fun EReceiptCard(
    order: Order,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Awaiting Counter Payment",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                textAlign = TextAlign.Center,
                color = Color(0xFFEF4444)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Please show this receipt to the cashier",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // QR Code
            val qrCodeBitmap = generateQRCode(order.id)
            if (qrCodeBitmap != null) {
                Image(
                    bitmap = qrCodeBitmap.asImageBitmap(),
                    contentDescription = "Order QR Code",
                    modifier = Modifier
                        .size(180.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Order ID: ${order.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Order Details Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF5F5F5)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Order Details",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF1F2937)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Items List
                    order.items.forEach { item ->
                        ItemDetailRow(item = item)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Total Amount - Prominent display
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEF4444)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Amount",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "RM ${String.format("%.2f", order.total)}",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 40.sp
                        ),
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Instructions
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEF3C7)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "💡 Instructions",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF92400E)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "1. Show this screen to the cashier\n" +
                              "2. Make payment at the counter\n" +
                              "3. Wait for order confirmation\n" +
                              "4. Your order will start preparing automatically",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF92400E),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemDetailRow(item: CartItem) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Item Name and Quantity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "${item.quantity}x ${item.menuItem.name}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF1F2937),
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = "RM ${String.format("%.2f", item.menuItem.price * item.quantity)}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF1F2937)
            )
        }
        
        // Customizations
        item.customization?.let { cust ->
            Spacer(modifier = Modifier.height(6.dp))
            
            val details = buildList<String> {
                if (cust.flavor.displayName != "None") add("🔥 ${cust.flavor.displayName}")
                if (cust.drink.displayName != "None") add("🥤 ${cust.drink.displayName}")
                if (cust.dippingSauce.displayName != "None") add("🍯 ${cust.dippingSauce.displayName}")
                cust.friesExchange?.let { fries ->
                    if (fries.name != "None") add("🍟 ${fries.name} (${fries.selectedSize})")
                }
                cust.boneType?.let { bone ->
                    add("🦴 ${bone.displayName}")
                }
                cust.saladType?.let { salad ->
                    if (salad != "None") add("🥗 $salad")
                }
            }
            
            details.forEach { detail ->
                Text(
                    text = "  • $detail",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        
        // Special Instructions
        if (!item.specialInstructions.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "  📝 ${item.specialInstructions}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/**
 * Generate QR Code for order ID
 */
private fun generateQRCode(orderId: String): android.graphics.Bitmap? {
    return try {
        val size = 512
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(
            orderId,
            BarcodeFormat.QR_CODE,
            size,
            size
        )
        
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
