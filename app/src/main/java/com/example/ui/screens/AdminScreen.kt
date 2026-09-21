package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.KeyItem
import com.example.ui.theme.CyberGradient
import com.example.ui.theme.CyanPrimaryLight
import com.example.ui.theme.GlassCardBg
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.GlassCardInner
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusActiveGreenBg
import com.example.ui.theme.StatusBlockedRed
import com.example.ui.theme.StatusBlockedRedBg
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.theme.StatusWarningAmberBg
import com.example.ui.theme.TechDarkBackground
import com.example.ui.theme.TechDarkBorder
import com.example.ui.theme.TechDarkSurface
import com.example.ui.theme.TechDarkSurfaceVariant
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.KeyAuthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AdminTab {
    GENERATE,
    DATABASE
}

@Composable
fun AdminScreen(viewModel: KeyAuthViewModel) {
    val context = LocalContext.current
    val allKeys by viewModel.allKeys.collectAsState()
    val cloudStatus by viewModel.cloudSyncStatus.collectAsState()
    val isServerOnline by viewModel.isServerOnline.collectAsState()
    val serverMaintenanceNotice by viewModel.maintenanceNotice.collectAsState()
    val isStatusUpdating by viewModel.isStatusUpdating.collectAsState()

    var activeTab by remember { mutableStateOf(AdminTab.GENERATE) }
    var localNoticeInput by remember(serverMaintenanceNotice) { mutableStateOf(serverMaintenanceNotice) }

    // Search & Filter State for Tab 2
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, ACTIVE, BLOCKED, EXPIRED

    // Create Key Form State for Tab 1
    var daysInput by remember { mutableStateOf("3") }
    var selectedMaxDevices by remember { mutableStateOf(1) } // 1 or 2 devices per request
    var customKeyNameInput by remember { mutableStateOf("") }
    var lastGeneratedKeyAlert by remember { mutableStateOf<String?>(null) }

    val activeCount = remember(allKeys) { allKeys.count { it.isActive && !it.isExpired } }
    val blockedCount = remember(allKeys) { allKeys.count { it.isBlocked } }
    val expiredCount = remember(allKeys) { allKeys.count { it.isExpired } }

    val filteredKeys = remember(allKeys, searchQuery, selectedFilter) {
        allKeys.filter { key ->
            val matchesSearch = key.key.contains(searchQuery.trim(), ignoreCase = true) ||
                    key.note.contains(searchQuery.trim(), ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "ACTIVE" -> key.isActive && !key.isExpired
                "BLOCKED" -> key.isBlocked
                "EXPIRED" -> key.isExpired
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TechDarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. TOP STATISTICS BAR (Overview Cards)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Stat 1: Total Keys
                        StatCounterCard(
                            label = "TOTAL",
                            count = allKeys.size.toString(),
                            accentColor = CyanPrimaryLight,
                            modifier = Modifier.weight(1f)
                        )

                        // Stat 2: Active Keys (Neon Green Badge)
                        StatCounterCard(
                            label = "ACTIVE",
                            count = activeCount.toString(),
                            accentColor = StatusActiveGreen,
                            badgeGlow = Color(0x3310B981),
                            modifier = Modifier.weight(1f)
                        )

                        // Stat 3: Blocked Keys (Neon Red Badge)
                        StatCounterCard(
                            label = "BLOCKED",
                            count = blockedCount.toString(),
                            accentColor = StatusBlockedRed,
                            badgeGlow = Color(0x33EF4444),
                            modifier = Modifier.weight(1f)
                        )

                        // Stat 4: Firebase RTDB Cloud Sync Indicator
                        val (cloudIcon, cloudColor, syncLabel) = when (cloudStatus.state) {
                            "SYNCING" -> Triple(Icons.Default.Sync, StatusWarningAmber, "SYNCING")
                            "SYNCED" -> Triple(Icons.Default.CloudDone, StatusActiveGreen, "LIVE")
                            "ERROR" -> Triple(Icons.Default.Cloud, StatusBlockedRed, "ERR")
                            else -> Triple(Icons.Default.Cloud, CyanPrimaryLight, "CLOUD")
                        }

                        Surface(
                            color = GlassCardBg,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GlassCardBorder),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(64.dp)
                                .clickable {
                                    viewModel.syncWithCloud()
                                    Toast.makeText(context, "Syncing Firebase RTDB...", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = cloudIcon,
                                        contentDescription = "Sync",
                                        tint = cloudColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = syncLabel,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp,
                                        color = cloudColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "RTDB SYNC",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = TextMutedDark
                                )
                            }
                        }
                    }
                }
            }

            // GLOBAL APP KILL SWITCH / MAINTENANCE MODE MASTER CONTROL
            item {
                Surface(
                    color = if (isServerOnline) GlassCardBg else Color(0xFF1F1116),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isServerOnline) GlassCardBorder else StatusBlockedRed.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isServerOnline) Color(0x2610B981) else Color(0x33EF4444)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PowerSettingsNew,
                                        contentDescription = null,
                                        tint = if (isServerOnline) StatusActiveGreen else StatusBlockedRed,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Global App Control",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // Neon Glowing Status Badge
                                        Surface(
                                            color = if (isServerOnline) StatusActiveGreenBg else StatusBlockedRedBg,
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isServerOnline) StatusActiveGreen.copy(alpha = 0.5f) else StatusBlockedRed.copy(alpha = 0.6f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isServerOnline) StatusActiveGreen else StatusBlockedRed)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isServerOnline) "SERVER ACTIVE" else "ALL CLIENTS DISABLED",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 0.5.sp,
                                                    color = if (isServerOnline) StatusActiveGreen else StatusBlockedRed
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Server / App Status: " + if (isServerOnline) "ONLINE" else "OFFLINE",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isServerOnline) CyanPrimaryLight else StatusBlockedRed
                                    )
                                }
                            }

                            // Master Toggle Switch
                            Switch(
                                checked = isServerOnline,
                                onCheckedChange = { newState ->
                                    viewModel.setServerStatus(newState) { success ->
                                        if (success) {
                                            Toast.makeText(
                                                context,
                                                if (newState) "✓ Server is now ONLINE" else "⚠️ Server OFFLINE - All clients disabled",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Failed to sync status to Firebase RTDB",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                enabled = !isStatusUpdating,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = StatusActiveGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = StatusBlockedRed,
                                    uncheckedBorderColor = StatusBlockedRed.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.testTag("server_status_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom Maintenance Notice Field
                        Text(
                            text = "CUSTOM MAINTENANCE NOTICE (BROADCAST TO CLIENTS)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = TextMutedDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = localNoticeInput,
                                onValueChange = { localNoticeInput = it },
                                placeholder = {
                                    Text(
                                        "e.g., Server under maintenance, back in 2 hours",
                                        fontSize = 11.5.sp,
                                        color = TextMutedDark
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanPrimaryLight,
                                    unfocusedBorderColor = TechDarkBorder,
                                    focusedContainerColor = TechDarkSurface,
                                    unfocusedContainerColor = TechDarkSurface,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("maintenance_notice_input")
                            )

                            Button(
                                onClick = {
                                    viewModel.updateMaintenanceNotice(localNoticeInput) { success ->
                                        if (success) {
                                            Toast.makeText(
                                                context,
                                                "✓ Notice broadcasted to Firebase RTDB",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Failed to update notice in Firebase",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                enabled = !isStatusUpdating,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = CyanPrimaryLight
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0x4D38BDF8)),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Text(
                                    text = "Send",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!isServerOnline) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = StatusBlockedRed,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Firebase RTDB node settings/app_status = \"offline\"",
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = StatusBlockedRed
                                )
                            }
                        }
                    }
                }
            }

            // 2. CLEAN TAB NAVIGATION
            item {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GlassCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Tab 1 Button: Generate New Key
                        val isTab1 = activeTab == AdminTab.GENERATE
                        Surface(
                            color = if (isTab1) Color(0x3338BDF8) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            border = if (isTab1) BorderStroke(1.dp, CyanPrimaryLight.copy(alpha = 0.5f)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { activeTab = AdminTab.GENERATE }
                                .testTag("tab_generate")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = if (isTab1) CyanPrimaryLight else TextMutedDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Generate Key",
                                    fontWeight = if (isTab1) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isTab1) TextPrimaryDark else TextSecondaryDark
                                )
                            }
                        }

                        // Tab 2 Button: Key Management & Database
                        val isTab2 = activeTab == AdminTab.DATABASE
                        Surface(
                            color = if (isTab2) Color(0x3338BDF8) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            border = if (isTab2) BorderStroke(1.dp, CyanPrimaryLight.copy(alpha = 0.5f)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { activeTab = AdminTab.DATABASE }
                                .testTag("tab_database")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = null,
                                    tint = if (isTab2) CyanPrimaryLight else TextMutedDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Keys Database (${allKeys.size})",
                                    fontWeight = if (isTab2) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isTab2) TextPrimaryDark else TextSecondaryDark
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // TAB 1: GENERATE NEW KEY
            // ==========================================
            if (activeTab == AdminTab.GENERATE) {
                item {
                    Surface(
                        color = GlassCardBg,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, GlassCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            // Header of Tab 1
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x2638BDF8)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = null,
                                            tint = CyanPrimaryLight,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Create License Key",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.3.sp,
                                            color = TextPrimaryDark
                                        )
                                        Text(
                                            text = "Instant push to Firebase RTDB",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = TextSecondaryDark
                                        )
                                    }
                                }

                                Surface(
                                    color = StatusActiveGreenBg,
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, StatusActiveGreen.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = StatusActiveGreen,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "AUTO SYNC",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                            color = StatusActiveGreen
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Validity Presets (1, 3, 7, 30 days)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "VALIDITY (DAYS)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = TextMutedDark
                                )
                                Text(
                                    text = "${daysInput.ifEmpty { "0" }} Days",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CyanPrimaryLight
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // Sleek Pill-shaped toggle chips for presets (1, 3, 7, 30 days)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("1", "3", "7", "30").forEach { d ->
                                    val isSelected = daysInput == d
                                    Surface(
                                        color = if (isSelected) Color(0x3338BDF8) else TechDarkSurfaceVariant,
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) CyanPrimaryLight else TechDarkBorder
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable { daysInput = d }
                                    ) {
                                        Text(
                                            text = "${d} Day${if (d == "1") "" else "s"}",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) CyanPrimaryLight else TextSecondaryDark,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Custom Input
                            OutlinedTextField(
                                value = daysInput,
                                onValueChange = { daysInput = it.filter { ch -> ch.isDigit() } },
                                placeholder = { Text("Custom days (e.g. 15, 60, 365)", fontSize = 12.sp, color = TextMutedDark) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanPrimaryLight,
                                    unfocusedBorderColor = TechDarkBorder,
                                    focusedContainerColor = TechDarkSurface,
                                    unfocusedContainerColor = TechDarkSurface,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("days_input")
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Device Limit Selector (1 or 2 Devices)
                            Text(
                                text = "DEVICE LIMIT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = TextMutedDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf(
                                    1 to "1 Device",
                                    2 to "2 Devices"
                                ).forEach { (count, label) ->
                                    val isSelected = selectedMaxDevices == count
                                    Surface(
                                        color = if (isSelected) Color(0x336366F1) else TechDarkSurfaceVariant,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFF818CF8) else TechDarkBorder
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedMaxDevices = count }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 11.dp, horizontal = 12.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Devices,
                                                contentDescription = null,
                                                tint = if (isSelected) Color(0xFF818CF8) else TextMutedDark,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) TextPrimaryDark else TextSecondaryDark
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Custom Key Code (Optional)
                            Text(
                                text = "CUSTOM KEY NAME (OPTIONAL)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = TextMutedDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customKeyNameInput,
                                onValueChange = { customKeyNameInput = it.uppercase() },
                                placeholder = { Text("Auto-generate if empty (e.g., VIP-KEY-100)", fontSize = 12.sp, color = TextMutedDark) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanPrimaryLight,
                                    unfocusedBorderColor = TechDarkBorder,
                                    focusedContainerColor = TechDarkSurface,
                                    unfocusedContainerColor = TechDarkSurface,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("custom_key_input")
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Glowing "Generate Key" Action Button
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = CyanPrimaryLight, ambientColor = CyanPrimaryLight)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val days = daysInput.toIntOrNull() ?: 3
                                        val customName = customKeyNameInput.trim()
                                        viewModel.generateAndCreateKey(
                                            days = days,
                                            maxDevices = selectedMaxDevices,
                                            customKeyName = if (customName.isNotBlank()) customName else null
                                        ) { generatedKey, _ ->
                                            lastGeneratedKeyAlert = generatedKey
                                            customKeyNameInput = ""
                                            Toast.makeText(
                                                context,
                                                "✓ Key $generatedKey deployed to Firebase!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    .testTag("generate_key_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0x6638BDF8))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(CyberGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(19.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Generate & Deploy Key",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            letterSpacing = 0.5.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // TAB 2: KEY MANAGEMENT & DATABASE
            // ==========================================
            if (activeTab == AdminTab.DATABASE) {
                // Search bar & Filter Chips
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "License Keys",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )

                            TextButton(
                                onClick = {
                                    viewModel.resetToDefaults()
                                    Toast.makeText(context, "Loaded default sample keys", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(13.dp), tint = TextSecondaryDark)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Defaults", fontSize = 11.sp, color = TextSecondaryDark)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Live Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search license key or notes...", fontSize = 12.sp, color = TextMutedDark) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = CyanPrimaryLight, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Text("✕", fontSize = 13.sp, color = TextSecondaryDark)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimaryLight,
                                unfocusedBorderColor = TechDarkBorder,
                                focusedContainerColor = GlassCardBg,
                                unfocusedContainerColor = GlassCardBg,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Filter Chips (All, Active, Blocked)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "ALL" to "All (${allKeys.size})",
                                "ACTIVE" to "Active ($activeCount)",
                                "BLOCKED" to "Blocked ($blockedCount)"
                            ).forEach { (filterKey, label) ->
                                val isSelected = selectedFilter == filterKey
                                Surface(
                                    color = if (isSelected) Color(0x3338BDF8) else GlassCardBg,
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, if (isSelected) CyanPrimaryLight else GlassCardBorder),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { selectedFilter = filterKey }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) CyanPrimaryLight else TextSecondaryDark,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Empty state or Keys list
                if (filteredKeys.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            color = GlassCardBg,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, GlassCardBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = TextMutedDark,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No License Keys Found",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = TextSecondaryDark
                                )
                                Text(
                                    text = "Try adjusting your search query or filters",
                                    fontSize = 12.sp,
                                    color = TextMutedDark
                                )
                            }
                        }
                    }
                } else {
                    items(filteredKeys, key = { it.key }) { keyItem ->
                        ModernKeyCard(
                            keyItem = keyItem,
                            viewModel = viewModel,
                            context = context
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Modal Alert when Key is Generated
    if (lastGeneratedKeyAlert != null) {
        val generated = lastGeneratedKeyAlert!!
        AlertDialog(
            onDismissRequest = { lastGeneratedKeyAlert = null },
            containerColor = TechDarkSurface,
            titleContentColor = TextPrimaryDark,
            textContentColor = TextSecondaryDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0x3310B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = StatusActiveGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Key Deployed Successfully", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "New license key created and pushed to Firebase Realtime Database:",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = TechDarkSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GlassCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = generated,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = CyanPrimaryLight
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("License Key", generated))
                                    Toast.makeText(context, "Copied key to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyanPrimaryLight, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = StatusActiveGreenBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, StatusActiveGreen.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = StatusActiveGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Firebase RTDB Synced (rakib-ai-engine)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusActiveGreen
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("License Key", generated))
                        Toast.makeText(context, "Copied key to clipboard!", Toast.LENGTH_SHORT).show()
                        lastGeneratedKeyAlert = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimaryLight, contentColor = Color(0xFF002844)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Key", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { lastGeneratedKeyAlert = null }) {
                    Text("Close", color = TextSecondaryDark)
                }
            }
        )
    }
}

// 1. Top Stat Overview Card
@Composable
private fun StatCounterCard(
    label: String,
    count: String,
    accentColor: Color,
    badgeGlow: Color? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = GlassCardBg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, GlassCardBorder),
        modifier = modifier.height(64.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (badgeGlow != null) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = count,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = TextMutedDark
            )
        }
    }
}

// Dedicated Sleek Layered Card for Each License Key
@Composable
fun ModernKeyCard(
    keyItem: KeyItem,
    viewModel: KeyAuthViewModel,
    context: Context
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("key_card_${keyItem.key}"),
        color = GlassCardBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (keyItem.isBlocked) StatusBlockedRed.copy(alpha = 0.4f) else GlassCardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Row 1: Key Code with Copy Button & Neon Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Key Code + One Tap Copy
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = keyItem.key,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp,
                        color = if (keyItem.isBlocked) StatusBlockedRed else CyanPrimaryLight
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Key", keyItem.key))
                            Toast.makeText(context, "Copied: ${keyItem.key}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Key",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Distinct Neon Status Pill
                val (badgeBg, badgeColor, statusLabel) = when {
                    keyItem.isBlocked -> Triple(StatusBlockedRedBg, StatusBlockedRed, "BLOCKED")
                    keyItem.isExpired -> Triple(StatusWarningAmberBg, StatusWarningAmber, "EXPIRED")
                    keyItem.isActive -> Triple(StatusActiveGreenBg, StatusActiveGreen, "ACTIVE")
                    else -> Triple(Color(0x2238BDF8), CyanPrimaryLight, "PENDING")
                }

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = statusLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                            color = badgeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Metadata Badges (Device Usage Pill + Expiration Timeline)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device Usage Pill (e.g. "0/1 Used")
                Surface(
                    color = TechDarkSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GlassCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = if (keyItem.devices.size >= keyItem.maxDevices) StatusWarningAmber else CyanPrimaryLight,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${keyItem.devices.size}/${keyItem.maxDevices} Used",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                    }
                }

                // Expiration Details
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val expiryText = when {
                    keyItem.expiresAt == null -> "${keyItem.days} Days (On 1st login)"
                    keyItem.isExpired -> "Expired on ${dateFormat.format(Date(keyItem.expiresAt))}"
                    else -> "Valid until ${dateFormat.format(Date(keyItem.expiresAt))}"
                }

                Text(
                    text = expiryText,
                    fontSize = 11.sp,
                    color = if (keyItem.isExpired) StatusBlockedRed else TextSecondaryDark
                )
            }

            // Bound Device IDs if any
            if (keyItem.devices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Bound:", fontSize = 10.sp, color = TextMutedDark)
                    keyItem.devices.forEach { devId ->
                        Surface(
                            color = TechDarkSurface,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, TechDarkBorder)
                        ) {
                            Text(
                                text = devId,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondaryDark,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Action Controls (Instant Block/Unblock Switch, Reset Device Binding, Delete Button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassCardInner, RoundedCornerShape(10.dp))
                    .border(1.dp, GlassCardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Toggle Switch for Instant Block / Unblock
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = !keyItem.isBlocked,
                        onCheckedChange = { viewModel.toggleKeyStatus(keyItem.key) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = StatusActiveGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = StatusBlockedRed
                        ),
                        modifier = Modifier.size(width = 38.dp, height = 24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (!keyItem.isBlocked) "Active" else "Blocked",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!keyItem.isBlocked) StatusActiveGreen else StatusBlockedRed
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Reset Devices button (if devices are bound)
                    if (keyItem.devices.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.resetKeyDevices(keyItem.key)
                                Toast.makeText(context, "Devices reset for ${keyItem.key}", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Devices",
                                tint = CyanPrimaryLight,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Clean Trash Icon for Delete
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete License",
                            tint = StatusBlockedRed.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = TechDarkSurface,
            titleContentColor = TextPrimaryDark,
            textContentColor = TextSecondaryDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = StatusBlockedRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete License Key?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text("Are you sure you want to permanently delete '${keyItem.key}' from Firebase Realtime Database?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteKey(keyItem.key)
                        showDeleteConfirm = false
                        Toast.makeText(context, "Deleted ${keyItem.key}", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusBlockedRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            }
        )
    }
}
