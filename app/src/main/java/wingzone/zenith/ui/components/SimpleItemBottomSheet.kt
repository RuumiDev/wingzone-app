package wingzone.zenith.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import wingzone.zenith.data.models.MenuItem
import wingzone.zenith.ui.theme.*

@Composable
fun SimpleItemBottomSheet(
    menuItem: MenuItem,
    onDismiss: () -> Unit = {},
    onAddToCart: (quantity: Int) -> Unit = {}
) {
    var quantity by remember { mutableStateOf(1) }
    var isImageLoading by remember { mutableStateOf(true) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp, start = 16.dp, end = 16.dp)
                    .clickable(enabled = false) { }, // Prevent card clicks from dismissing
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 32.dp)
                ) {
                    // Close indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFE0E0E0))
                        )
                    }
                    
                    // Item Image with skeleton loader
                    if (!menuItem.imageUrl.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF5F5F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = menuItem.imageUrl,
                                contentDescription = menuItem.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                onSuccess = { isImageLoading = false },
                                onError = { isImageLoading = false }
                            )
                            
                            // Loading indicator
                            if (isImageLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFE0E0E0).copy(alpha = 0.6f))
                                )
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = WingZoneOrange
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    
                    // Item name
                    Text(
                        text = menuItem.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkGray
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Item description
                    if (menuItem.description.isNotEmpty()) {
                        Text(
                            text = menuItem.description,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Price
                    Text(
                        text = "RM ${String.format("%.2f", menuItem.price)}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WingZoneRed
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Quantity selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quantity",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkGray
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Minus button
                            IconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                enabled = quantity > 1,
                                modifier = Modifier
                                    .size(40.dp)
                                    .border(
                                        width = 2.dp,
                                        color = if (quantity > 1) WingZoneOrange else Color(0xFFE0E0E0),
                                        shape = CircleShape
                                    )
                            ) {
                                Text(
                                    text = "−",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (quantity > 1) WingZoneOrange else Color(0xFFBDBDBD)
                                )
                            }
                            
                            // Quantity display
                            Text(
                                text = quantity.toString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkGray,
                                modifier = Modifier.widthIn(min = 30.dp)
                            )
                            
                            // Plus button
                            IconButton(
                                onClick = { if (quantity < 10) quantity++ },
                                enabled = quantity < 10,
                                modifier = Modifier
                                    .size(40.dp)
                                    .border(
                                        width = 2.dp,
                                        color = if (quantity < 10) WingZoneOrange else Color(0xFFE0E0E0),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase quantity",
                                    tint = if (quantity < 10) WingZoneOrange else Color(0xFFBDBDBD)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Add to cart button
                    Button(
                        onClick = {
                            onAddToCart(quantity)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WingZoneRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add to Cart • RM ${String.format("%.2f", menuItem.price * quantity)}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}
