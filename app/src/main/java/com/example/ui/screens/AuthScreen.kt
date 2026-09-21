package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimaryLight
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusActiveGreenBg
import com.example.ui.theme.StatusBlockedRed
import com.example.ui.theme.StatusBlockedRedBg
import com.example.ui.theme.TechDarkSurfaceVariant
import com.example.ui.viewmodel.KeyAuthViewModel

@Composable
fun AuthScreen(viewModel: KeyAuthViewModel) {
    val context = LocalContext.current
    val inputKey by viewModel.inputKey.collectAsState()
    val currentDeviceId by viewModel.currentDeviceId.collectAsState()
    val simulatedDeviceId by viewModel.simulatedDeviceId.collectAsState()
    val authUiState by viewModel.authUiState.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()
    val allKeys by viewModel.allKeys.collectAsState()
    val isServerOnline by viewModel.isServerOnline.collectAsState()
    val maintenanceNotice by viewModel.maintenanceNotice.collectAsState()

    var showSimulateDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Maintenance Alert Banner if Server is Offline
        if (!isServerOnline) {
            Surface(
                color = StatusBlockedRedBg,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, StatusBlockedRed.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StatusBlockedRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = StatusBlockedRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBengali) "সার্ভার সাময়িকভাবে বন্ধ রয়েছে" else "MAINTENANCE MODE ACTIVE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp,
                            color = StatusBlockedRed
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (maintenanceNotice.isNotBlank())
                                maintenanceNotice
                            else if (isBengali)
                                "সার্ভার রক্ষণাবেক্ষণের জন্য সাময়িকভাবে বন্ধ আছে। কিছুক্ষণের মধ্যে ফিরে আসবে।"
                            else
                                "App server is offline for maintenance. Please check back shortly.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Hero Icon with Gradient Glow
        Box(
            modifier = Modifier
                .padding(top = 12.dp, bottom = 16.dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            CyanPrimaryLight.copy(alpha = 0.35f),
                            IndigoAccent.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .border(2.dp, CyanPrimaryLight.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = "Key Icon",
                tint = CyanPrimaryLight,
                modifier = Modifier.size(40.dp)
            )
        }

        Text(
            text = if (isBengali) "লাইসেন্স কী প্রমাণীকরণ" else "License Key Authentication",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = if (isBengali)
                "ডিভাইস বাইন্ডিং ও মেয়াদ গণনা (প্রথম লগইনে সময় শুরু)"
            else
                "Device binding & expiry tracking (Validity starts on 1st login)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // Device ID Card with Copy & Switch button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "Device ID",
                        tint = CyanPrimaryLight,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isBengali) "আপনার ডিভাইস আইডি" else "Your Device ID",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (simulatedDeviceId != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Simulated",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = currentDeviceId,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row {
                    // Copy Device ID
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Device ID", currentDeviceId))
                            Toast.makeText(context, "Device ID copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("copy_device_id_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Device ID",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Switch / Simulate Device ID (for testing multi-device rules)
                    IconButton(
                        onClick = { showSimulateDialog = true },
                        modifier = Modifier.testTag("simulate_device_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Switch or Simulate Device",
                            tint = CyanPrimaryLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Key Input Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = if (isBengali) "লাইসেন্স কী প্রবেশ করান" else "Enter License Key",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { viewModel.onInputKeyChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("key_input_field"),
                    label = { Text(if (isBengali) "লাইসেন্স কী" else "License Key") },
                    placeholder = { Text("e.g. KEY-ABC123XYZ") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = CyanPrimaryLight
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    viewModel.onInputKeyChange(text)
                                }
                            },
                            modifier = Modifier.testTag("paste_key_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimaryLight,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = { viewModel.authenticate() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("verify_key_button"),
                    enabled = !authUiState.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimaryLight,
                        contentColor = Color(0xFF002844)
                    )
                ) {
                    if (authUiState.isLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFF002844),
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBengali) "যাচাই করা হচ্ছে..." else "Verifying...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBengali) "চাবি সক্রিয় ও লগইন করুন" else "Verify & Activate License",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Error message banner
        AnimatedVisibility(
            visible = authUiState.errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            authUiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusBlockedRedBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = StatusBlockedRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFFD1D1),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Success message banner
        AnimatedVisibility(
            visible = authUiState.successMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            authUiState.successMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusActiveGreenBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = StatusActiveGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD1FAE5),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Quick Test Keys Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
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
                        text = if (isBengali) "টেস্ট করার জন্য দ্রুত চাবি নির্বাচন" else "Quick Test Keys",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${allKeys.size} ${if (isBengali) "টি চাবি" else "keys"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                allKeys.take(4).forEach { keyItem ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                viewModel.selectKeyForLogin(keyItem.key)
                            },
                        color = if (inputKey == keyItem.key)
                            CyanPrimaryLight.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surface,
                        border = if (inputKey == keyItem.key)
                            BorderStroke(1.dp, CyanPrimaryLight)
                        else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = keyItem.key,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    // Status tag
                                    val (badgeBg, badgeText, label) = when {
                                        keyItem.isBlocked -> Triple(StatusBlockedRedBg, StatusBlockedRed, "BLOCKED")
                                        keyItem.isExpired -> Triple(StatusBlockedRedBg, StatusBlockedRed, "EXPIRED")
                                        keyItem.isUsed -> Triple(StatusActiveGreenBg, StatusActiveGreen, "IN USE")
                                        else -> Triple(TechDarkSurfaceVariant, CyanPrimaryLight, "NEW")
                                    }
                                    Surface(
                                        color = badgeBg,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeText,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${keyItem.days} days • max ${keyItem.maxDevices} device(s) • ${if (keyItem.isUsed) "Activated" else "Not started"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = if (inputKey == keyItem.key) "Selected" else "Use",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CyanPrimaryLight
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog to Switch / Simulate Device ID (Crucial for testing maxDevices rule)
    if (showSimulateDialog) {
        var customDeviceInput by remember { mutableStateOf(currentDeviceId) }

        AlertDialog(
            onDismissRequest = { showSimulateDialog = false },
            title = {
                Text(if (isBengali) "ডিভাইস আইডি পরিবর্তন বা সিমুলেশন" else "Switch / Simulate Device ID")
            },
            text = {
                Column {
                    Text(
                        text = if (isBengali)
                            "আপনি যেকোনো ডিভাইস আইডি দিয়ে মাল্টি-ডিভাইস সীমা (maxDevices) পরীক্ষা করতে পারেন। যেমন: DeviceID_1, DeviceID_2"
                        else
                            "Test multi-device limits and device binding by switching to another device ID (e.g. DeviceID_1, DeviceID_2).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customDeviceInput,
                        onValueChange = { customDeviceInput = it },
                        label = { Text("Device ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { customDeviceInput = "DeviceID_1" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("DeviceID_1", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { customDeviceInput = "DeviceID_2" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("DeviceID_2", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.changeDeviceId(customDeviceInput)
                        showSimulateDialog = false
                    }
                ) {
                    Text(if (isBengali) "প্রয়োগ করুন" else "Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreHardwareDeviceId()
                        showSimulateDialog = false
                    }
                ) {
                    Text(if (isBengali) "আসল আইডিতে ফিরুন" else "Restore Real ID")
                }
            }
        )
    }
}
