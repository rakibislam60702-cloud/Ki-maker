package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AdminScreen
import com.example.ui.theme.CyanPrimaryLight
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TechDarkBackground
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.viewmodel.KeyAuthViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: KeyAuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: KeyAuthViewModel) {
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TechDarkBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyanPrimaryLight.copy(alpha = 0.15f))
                                .border(1.dp, CyanPrimaryLight.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = CyanPrimaryLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Key Manager",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = 0.5.sp,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0x330EA5E9),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0x6638BDF8))
                        ) {
                            Text(
                                text = "ADMIN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = CyanPrimaryLight,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Firebase Sync / Refresh Button
                    IconButton(
                        onClick = {
                            viewModel.syncWithCloud()
                            Toast.makeText(
                                context,
                                "Syncing with Firebase RTDB...",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Surface(
                            color = Color(0x1A38BDF8),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0x3338BDF8)),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = CyanPrimaryLight,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = TechDarkBackground
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TechDarkBackground)
                .padding(innerPadding)
        ) {
            // Key Manager (Admin Panel) is the only main screen of the app
            AdminScreen(viewModel = viewModel)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
