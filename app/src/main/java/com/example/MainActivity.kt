package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AdminScreen
import com.example.ui.theme.CyberNeonHeaderGradient
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

@Composable
fun RotatingNeonTitleCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "NeonBorderSweep")
    // Slow-motion calm rotation (6000ms duration)
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BorderAngle"
    )

    // Dual-color combination: Electric Neon Blue (#00D2FF) and Warm Light Orange/Gold (#FFA726)
    // with smooth fading boundaries around the sweep perimeter
    val neonSweepBrush = Brush.sweepGradient(
        colors = listOf(
            Color(0xFF00D2FF), // Electric Neon Blue beam start
            Color(0x3300D2FF), // Trail fade
            Color(0x1A1E293B), // Subtle baseline border
            Color(0x1A1E293B), // Subtle baseline border
            Color(0xFFFFA726), // Warm Light Gold/Orange beam
            Color(0x33FFA726), // Trail fade
            Color(0x1A1E293B), // Subtle baseline border
            Color(0xFF00D2FF)  // Seamless loop to Electric Neon Blue
        )
    )

    val cardShape = RoundedCornerShape(16.dp)

    // Outer container: hosts the slow-motion rotating neon light sweep strictly along the outer boundary
    Box(
        modifier = modifier
            .clip(cardShape)
            .background(Color(0xFF1E293B).copy(alpha = 0.4f)) // Subtle outer edge halo
            .drawWithContent {
                // 1. Draw rotating sweep light border strictly on the perimeter stroke
                rotate(degrees = angle) {
                    drawRoundRect(
                        brush = neonSweepBrush,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
                // 2. Draw content on top so inner card covers the internal area completely
                drawContent()
            }
            .padding(2.dp) // Gap for outer border stroke so animation never encroaches into inner surface
    ) {
        // Inner card: strictly #0B1120 dark dashboard background with 14dp inner radius, completely shielding the content
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0B1120))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        RotatingNeonTitleCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x2600D2FF))
                                        .border(1.dp, Color(0x6600D2FF), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = Color(0xFF00D2FF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Rakib Official",
                                    style = TextStyle(
                                        color = Color(0xFFF8FAFC),
                                        shadow = Shadow(
                                            color = Color(0x8000D2FF),
                                            offset = Offset(0f, 1f),
                                            blurRadius = 6f
                                        ),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0x2600D2FF),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0x8000D2FF))
                        ) {
                            Text(
                                text = "ADMIN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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
