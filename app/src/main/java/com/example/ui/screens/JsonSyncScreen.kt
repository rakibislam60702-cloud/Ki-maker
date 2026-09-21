package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimaryLight
import com.example.ui.viewmodel.KeyAuthViewModel

@Composable
fun JsonSyncScreen(viewModel: KeyAuthViewModel) {
    val context = LocalContext.current
    val isBengali by viewModel.isBengali.collectAsState()
    val allKeys by viewModel.allKeys.collectAsState()

    var jsonText by remember { mutableStateOf("") }
    var importInputText by remember { mutableStateOf("") }
    var isEditingImport by remember { mutableStateOf(false) }

    LaunchedEffect(allKeys) {
        jsonText = viewModel.getExportJson()
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(verticalScroll)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = CyanPrimaryLight,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isBengali) "জেসন স্কিমা ও ডেটা সিঙ্ক" else "JSON Schema & Sync",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isBengali) "রিয়েলটাইম ডাটাবেজ ফরম্যাট" else "Schema format for Firebase / REST",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Keys JSON", jsonText))
                Toast.makeText(context, "JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
            }) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy JSON",
                    tint = CyanPrimaryLight
                )
            }
        }

        // Action Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Keys JSON", jsonText))
                    Toast.makeText(context, "JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimaryLight, contentColor = Color(0xFF002844)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isBengali) "কপি জেসন" else "Copy JSON", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { isEditingImport = !isEditingImport },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (isEditingImport) Icons.Default.FileDownload else Icons.Default.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEditingImport) (if (isBengali) "প্রিভিউ দেখুন" else "View Output") else (if (isBengali) "ইমপোর্ট করুন" else "Import JSON"),
                    fontSize = 12.sp
                )
            }
        }

        if (isEditingImport) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = if (isBengali) "কাস্টম জেসন পেস্ট করুন" else "Paste Custom JSON Payload",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Format: { \"keys\": { \"KEY-XYZ\": { \"days\": 3, \"maxDevices\": 1, \"status\": \"active\"... } } }",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = { importInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        placeholder = { Text("Paste JSON here...") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.importJson(importInputText) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    if (success) isEditingImport = false
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isBengali) "ইমপোর্ট সম্পন্ন করুন" else "Apply & Save")
                        }

                        OutlinedButton(
                            onClick = { isEditingImport = false },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isBengali) "বাতিল" else "Cancel")
                        }
                    }
                }
            }
        } else {
            // Raw JSON Viewer Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = if (isBengali) "লাইভ ডাটাবেজ জেসন আউটপুট:" else "Live Database Schema Representation:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScroll)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = jsonText,
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
