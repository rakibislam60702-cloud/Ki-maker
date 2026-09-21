package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.KeyItem
import com.example.ui.theme.CyanPrimaryLight
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusActiveGreenBg
import com.example.ui.theme.StatusBlockedRed
import com.example.ui.theme.StatusBlockedRedBg
import com.example.ui.theme.TechDarkSurfaceVariant
import com.example.ui.viewmodel.KeyAuthViewModel
import com.example.util.DeviceUtils
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun WorkspaceScreen(
    keyItem: KeyItem,
    viewModel: KeyAuthViewModel
) {
    val context = LocalContext.current
    val currentDeviceId by viewModel.currentDeviceId.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()
    val isServerOnline by viewModel.isServerOnline.collectAsState()
    val maintenanceNotice by viewModel.maintenanceNotice.collectAsState()
    // Subscribed to live 1-second ticker so countdown updates smoothly
    val ticker by viewModel.ticker.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedToolTab by remember { mutableIntStateOf(0) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Global Maintenance Notice if offline
        if (!isServerOnline) {
            Surface(
                color = StatusBlockedRedBg,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, StatusBlockedRed.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(StatusBlockedRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = StatusBlockedRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBengali) "সার্ভার স্থগিত রয়েছে (রক্ষণাবেক্ষণ)" else "GLOBAL MAINTENANCE ACTIVE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = StatusBlockedRed
                        )
                        Text(
                            text = if (maintenanceNotice.isNotBlank()) maintenanceNotice else "Cloud operations temporarily disabled by administrator.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Active VIP Header Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Top row with Key and Active Badge
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyanPrimaryLight.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = CyanPrimaryLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBengali) "সক্রিয় লাইসেন্স" else "Active License",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = keyItem.key,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Copy Key Icon
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Key", keyItem.key))
                            Toast.makeText(context, "Key copied!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Key",
                            tint = CyanPrimaryLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Status Badge
                    Surface(
                        color = StatusActiveGreenBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StatusActiveGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACTIVE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = StatusActiveGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Live Countdown Ticker
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = CyanPrimaryLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBengali) "অবশিষ্ট মেয়াদ" else "Remaining Validity",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Re-evaluated with ticker
                            val remaining = keyItem.remainingTimeMillis()
                            Text(
                                text = keyItem.formatRemainingTime(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (remaining < 3600000) StatusBlockedRed else CyanPrimaryLight
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { keyItem.progressFraction() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = CyanPrimaryLight,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        val expiresStr = keyItem.expiresAt?.let { dateFormat.format(Date(it)) } ?: "Pending first login"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${if (isBengali) "মোট মেয়াদ" else "Total plan"}: ${keyItem.days} ${if (isBengali) "দিন" else "days"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${if (isBengali) "মেয়াদোত্তীর্ণ" else "Expires"}: $expiresStr",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Device Binding Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = IndigoAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBengali) "যুক্ত ডিভাইস:" else "Bound Devices:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "${keyItem.devices.size} / ${keyItem.maxDevices} max",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = CyanPrimaryLight
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Devices List Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    keyItem.devices.forEach { devId ->
                        val isCurrent = devId == currentDeviceId
                        Surface(
                            color = if (isCurrent)
                                CyanPrimaryLight.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            border = if (isCurrent)
                                BorderStroke(1.dp, CyanPrimaryLight)
                            else null
                        ) {
                            Text(
                                text = if (isCurrent) "$devId (This)" else devId,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isCurrent) CyanPrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons Row: Refresh & Logout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.refreshActiveKey()
                    Toast.makeText(context, "License refreshed", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isBengali) "লাইসেন্স রিফ্রেশ" else "Refresh Status")
            }

            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("logout_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusBlockedRedBg,
                    contentColor = StatusBlockedRed
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isBengali) "লগআউট" else "Logout")
            }
        }

        // VIP Protected Feature Tools (Unlocked by key)
        Text(
            text = if (isBengali) "ভিআইপি ফিচার ও সিকিউরিটি টুলস" else "VIP Security & Tool Suite",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        TabRow(
            selectedTabIndex = selectedToolTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = CyanPrimaryLight,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedToolTab == 0,
                onClick = { selectedToolTab = 0 },
                text = { Text(if (isBengali) "ডিভাইস" else "Telemetry", fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedToolTab == 1,
                onClick = { selectedToolTab = 1 },
                text = { Text(if (isBengali) "হ্যাশ টুল" else "Hasher", fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedToolTab == 2,
                onClick = { selectedToolTab = 2 },
                text = { Text(if (isBengali) "টোকেন" else "Tokens", fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedToolTab) {
            0 -> DeviceTelemetryCard(isBengali)
            1 -> HashGeneratorCard(context, isBengali)
            2 -> TokenGeneratorCard(context, isBengali)
        }
    }

    if (showLogoutDialog) {
        var unbindDeviceCheck by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = StatusBlockedRed
                )
            },
            title = {
                Text(if (isBengali) "লগআউট নিশ্চিত করুন" else "Confirm Logout")
            },
            text = {
                Column {
                    Text(
                        text = if (isBengali)
                            "আপনি কি এই লাইসেন্স সেশন থেকে বের হতে চান?"
                        else
                            "Are you sure you want to end your active session?"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { unbindDeviceCheck = !unbindDeviceCheck },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (unbindDeviceCheck) StatusBlockedRed else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(
                                    text = if (unbindDeviceCheck) "✓ Unbind Device" else "Keep Device Bound",
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (unbindDeviceCheck)
                                    "Frees 1 device slot"
                                else
                                    "Retains device binding",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout(unbindThisDevice = unbindDeviceCheck)
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusBlockedRed)
                ) {
                    Text(if (isBengali) "লগআউট" else "Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun DeviceTelemetryCard(isBengali: Boolean) {
    val info = remember { DeviceUtils.getDeviceInfo() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = if (isBengali) "ডিভাইস টেলিমেট্রি ও সিস্টেম তথ্য" else "Hardware & Security Diagnostics",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            info.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun HashGeneratorCard(context: Context, isBengali: Boolean) {
    var inputText by remember { mutableStateOf("KeyAuth-2026") }

    fun md5(s: String): String {
        return MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun sha256(s: String): String {
        return MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    val md5Hash = remember(inputText) { md5(inputText) }
    val sha256Hash = remember(inputText) { sha256(inputText) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = if (isBengali) "ক্রিপ্টোগ্রাফিক হ্যাশ ক্যালকুলেটর" else "Cryptographic Hash Generator",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text(if (isBengali) "ইনপুট স্ট্রিং" else "Input String") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // MD5 Result
            HashResultRow(title = "MD5", hash = md5Hash, context = context)

            Spacer(modifier = Modifier.height(6.dp))

            // SHA-256 Result
            HashResultRow(title = "SHA-256", hash = sha256Hash, context = context)
        }
    }
}

@Composable
fun HashResultRow(title: String, hash: String, context: Context) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanPrimaryLight)
                Text(
                    text = hash,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(title, hash))
                    Toast.makeText(context, "$title copied!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun TokenGeneratorCard(context: Context, isBengali: Boolean) {
    var generatedToken by remember { mutableStateOf(UUID.randomUUID().toString().replace("-", "").uppercase()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = if (isBengali) "নিরাপদ অথ সেশন টোকেন" else "Secure Session Token Generator",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = generatedToken,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanPrimaryLight,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Token", generatedToken))
                            Toast.makeText(context, "Token copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    generatedToken = UUID.randomUUID().toString().replace("-", "").uppercase()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimaryLight, contentColor = Color(0xFF002844))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isBengali) "নতুন টোকেন জেনারেট করুন" else "Generate New Token")
            }
        }
    }
}
