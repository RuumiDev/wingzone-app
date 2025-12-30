package wingzone.zenith.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import wingzone.zenith.ui.theme.*

data class RewardItem(
    val id: String,
    val title: String,
    val pointsCost: Int,
    val cashValue: Double,
    val category: String
)

data class Mission(
    val id: String,
    val title: String,
    val description: String,
    val pointsReward: Int,
    val daysLeft: Int,
    val progress: Int,
    val total: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Missions", "Redeem Rewards", "My Rewards")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Missions and Rewards",
                        fontWeight = FontWeight.Bold,
                        color = WingZoneRed,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = WingZoneRed
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        selectedContentColor = WingZoneRed,
                        unselectedContentColor = Color.Gray
                    )
                }
            }
            
            // Content based on selected tab
            when (selectedTab) {
                0 -> MissionsTab()
                1 -> RedeemRewardsTab()
                2 -> MyRewardsTab()
            }
        }
    }
}

@Composable
fun MissionsTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Points Display
        item {
            PointsCircle(points = 0)
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Daily Check-in
        item {
            DailyCheckInCard()
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Missions Header
        item {
            Text(
                text = "Complete missions & get exclusive rewards",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Mission Cards
        items(2) { index ->
            MissionCard(
                mission = getMissions()[index]
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun PointsCircle(points: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8EAF6)),
            contentAlignment = Alignment.Center
        ) {
            // Outer ring
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD1D5E8))
            )
            
            // Inner content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cup icon placeholder
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WingZoneRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Cup",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = points.toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = WingZoneRed
                )
                Text(
                    text = "points",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun DailyCheckInCard() {
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
                Text(
                    text = "Daily Check-in",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = { /* Info */ }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Check-in days
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (day in 1..7) {
                    DayCheckBox(
                        day = day,
                        points = if (day == 4) 3 else if (day == 7) 2 else 1,
                        isChecked = false
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Check-in button
            Button(
                onClick = { /* Check in */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = WingZoneRed
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, WingZoneRed)
            ) {
                Text(
                    "Check In & Get 1 pt",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DayCheckBox(day: Int, points: Int, isChecked: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(48.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isChecked) WingZoneOrange.copy(alpha = 0.2f)
                    else Color(0xFFF5F5F5)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${points}pt",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isChecked) WingZoneRed else Color.Gray
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Day $day",
            fontSize = 10.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun MissionCard(mission: Mission) {
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
            // Mission image placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Mission",
                    tint = Color.Gray.copy(alpha = 0.3f),
                    modifier = Modifier.size(60.dp)
                )
                
                // Badge
                Surface(
                    color = Color(0xFF666666),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "TASTE THE WORLD!",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = mission.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = mission.progress.toFloat() / mission.total,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = WingZoneOrange,
                trackColor = Color(0xFFE0E0E0)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Points",
                        tint = WingZoneOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${mission.pointsReward} pts",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Time",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${mission.daysLeft} days left",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun RedeemRewardsTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Easy Goer Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Easy Goer",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = WingZoneRed
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = WingZoneRed
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Points display
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WZ Points",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "0 pts",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    
                    OutlinedButton(
                        onClick = { /* My Rewards */ },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = WingZoneRed
                        )
                    ) {
                        Text("My Rewards")
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // WZ Rewards Section
        item {
            Text(
                text = "WZ Rewards",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Reward cards
        items(3) { index ->
            RewardCard(getRewards()[index])
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // WZ Elite Exclusive Section
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "WZ Elite Exclusive",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        item {
            RewardCard(getEliteReward())
        }
    }
}

@Composable
fun RewardCard(reward: RewardItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Reward image
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (reward.category) {
                            "elite" -> Color(0xFF9E9E9E)
                            else -> WingZoneRed
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "RM",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = reward.cashValue.toInt().toString(),
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                
                if (reward.category == "elite") {
                    Surface(
                        color = WingZoneRed,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Elite",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Reward info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Redeem",
                                tint = WingZoneOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Redeem With",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = "${reward.pointsCost} pts",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Rewards",
                                tint = WingZoneOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Rewards",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = "RM ${String.format("%.0f", reward.cashValue)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyRewardsTab() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "No rewards",
                tint = Color.Gray.copy(alpha = 0.3f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No rewards yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Complete missions to earn rewards",
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
    }
}

// Sample data
fun getMissions(): List<Mission> {
    return listOf(
        Mission(
            id = "1",
            title = "4x orders to Taste The World!",
            description = "Order 4 times to complete",
            pointsReward = 120,
            daysLeft = 24,
            progress = 0,
            total = 4
        ),
        Mission(
            id = "2",
            title = "3x Chocolate Chubbybara orders for a chill",
            description = "Order 3 Chocolate Chubbybara drinks",
            pointsReward = 80,
            daysLeft = 24,
            progress = 0,
            total = 3
        )
    )
}

fun getRewards(): List<RewardItem> {
    return listOf(
        RewardItem(
            id = "1",
            title = "RM 3 Off",
            pointsCost = 300,
            cashValue = 3.0,
            category = "standard"
        ),
        RewardItem(
            id = "2",
            title = "RM 6 Off",
            pointsCost = 600,
            cashValue = 6.0,
            category = "standard"
        ),
        RewardItem(
            id = "3",
            title = "RM 9 Off",
            pointsCost = 900,
            cashValue = 9.0,
            category = "standard"
        )
    )
}

fun getEliteReward(): RewardItem {
    return RewardItem(
        id = "elite_1",
        title = "RM 12 Off - Elite Exclusive",
        pointsCost = 1000,
        cashValue = 12.0,
        category = "elite"
    )
}
