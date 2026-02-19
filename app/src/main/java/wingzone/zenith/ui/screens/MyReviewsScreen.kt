package wingzone.zenith.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import wingzone.zenith.data.models.Review
import wingzone.zenith.data.repository.Order
import wingzone.zenith.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReviewsScreen(
    onBack: () -> Unit = {},
    onRateOrder: (Order) -> Unit = {}
) {
    // Handle back button
    BackHandler {
        onBack()
    }
    
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var pendingOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var publishedReviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showRatingSheet by remember { mutableStateOf(false) }
    var orderToRate by remember { mutableStateOf<Order?>(null) }

    // Fetch pending orders (delivered but not rated)
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            firestore.collection("orders")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("status", "delivered")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        loading = false
                        return@addSnapshotListener
                    }

                    val orders = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val ratedAt = doc.get("ratedAt")
                            // Only include orders that haven't been rated
                            if (ratedAt == null) {
                                Order(
                                    id = doc.id,
                                    userId = doc.getString("userId") ?: "",
                                    userName = doc.getString("userName") ?: "",
                                    items = emptyList(),
                                    subtotal = (doc.get("subtotal") as? Number)?.toDouble() ?: 0.0,
                                    tax = (doc.get("tax") as? Number)?.toDouble() ?: 0.0,
                                    total = (doc.get("total") as? Number)?.toDouble() ?: 0.0,
                                    status = doc.getString("status") ?: "pending",
                                    paymentStatus = doc.getString("paymentStatus") ?: "unpaid",
                                    paymentMethod = doc.getString("paymentMethod") ?: "cash",
                                    createdAt = (doc.get("createdAt") as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                                    updatedAt = (doc.get("updatedAt") as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                                    deliveryAddress = doc.getString("deliveryAddress"),
                                    deliveryNotes = doc.getString("deliveryNotes"),
                                    phoneNumber = doc.getString("phoneNumber"),
                                    ratedAt = null,
                                    rating = null
                                )
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    } ?: emptyList()

                    pendingOrders = orders
                    loading = false
                }
        } else {
            loading = false
        }
    }

    // Fetch user's published reviews
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            firestore.collection("reviews")
                .whereEqualTo("userId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener

                    val reviews = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            Review(
                                id = doc.id,
                                orderId = doc.getString("orderId") ?: "",
                                userId = doc.getString("userId") ?: "",
                                userName = doc.getString("userName") ?: "",
                                rating = (doc.get("rating") as? Number)?.toInt() ?: 0,
                                comment = doc.getString("comment") ?: "",
                                createdAt = (doc.get("createdAt") as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                                menuItemIds = (doc.get("menuItemIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                isEnabled = doc.getBoolean("isEnabled") ?: true,
                                moderationStatus = doc.getString("moderationStatus") ?: "pending"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } ?: emptyList()

                    publishedReviews = reviews
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Reviews",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    navigationIconContentColor = TextPrimary,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WingZoneRed)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Section 1: Pending Reviews
                    if (pendingOrders.isNotEmpty()) {
                        item {
                            SectionTitle("Pending Reviews")
                        }
                        items(pendingOrders) { order ->
                            PendingReviewCard(
                                order = order,
                                onRateClick = { 
                                    orderToRate = order
                                    showRatingSheet = true
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Section 2: Published Reviews
                    item {
                        SectionTitle("Published")
                    }
                    
                    if (publishedReviews.isEmpty()) {
                        item {
                            EmptyState(
                                message = "You haven't submitted any reviews yet"
                            )
                        }
                    } else {
                        items(publishedReviews) { review ->
                            PublishedReviewCard(review = review)
                        }
                    }
                }
            }
        }
    }
    
    // Rating Bottom Sheet
    if (showRatingSheet && orderToRate != null) {
        RatingBottomSheet(
            order = orderToRate!!,
            onDismiss = { 
                showRatingSheet = false
                orderToRate = null
            },
            onSubmit = { _, _ ->
                showRatingSheet = false
                orderToRate = null
            }
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    )
}

@Composable
fun PendingReviewCard(
    order: Order,
    onRateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order #${order.id.take(8).uppercase()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    Text(
                        text = dateFormat.format(order.createdAt),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                
                Text(
                    text = "RM ${String.format("%.2f", order.total)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WingZoneRed
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onRateClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WingZoneRed
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = "Rate",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Rate Now",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PublishedReviewCard(review: Review) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order #${review.orderId.take(8).uppercase()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    Text(
                        text = dateFormat.format(review.createdAt),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                
                // Status Badge
                ReviewStatusBadge(
                    moderationStatus = review.moderationStatus,
                    isEnabled = review.isEnabled
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Star rating
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = if (index < review.rating) Color(0xFFFFD700) else Color(0xFFE0E0E0),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            if (review.comment.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = review.comment,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun ReviewStatusBadge(
    moderationStatus: String,
    isEnabled: Boolean
) {
    val (text, bgColor, textColor) = when {
        !isEnabled -> Triple("Disabled", Color(0xFFFFEBEE), Color(0xFFF44336))
        moderationStatus == "approved" -> Triple("Live", Color(0xFFE8F5E9), Color(0xFF4CAF50))
        moderationStatus == "rejected" -> Triple("Rejected", Color(0xFFFFEBEE), Color(0xFFF44336))
        else -> Triple("Under Review", Color(0xFFFFF3E0), Color(0xFFFF8F00))
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
    }
}
