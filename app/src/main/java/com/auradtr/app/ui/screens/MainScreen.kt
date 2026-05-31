package com.auradtr.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradtr.app.ui.DtrViewModel
import com.auradtr.app.ui.export.PdfExporter
import com.auradtr.app.ui.export.PdfPreviewDialog
import com.auradtr.app.data.Profile
import com.auradtr.app.data.TimeLog
import com.auradtr.app.ui.theme.glassCard
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File

enum class DtrTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Done),
    CLOCK("Time Clock", Icons.Default.PlayArrow),
    LOGBOOK("History", Icons.Default.List),
    PROFILE("Settings", Icons.Default.Settings),
    SUPERVISOR("Supervisor", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: DtrViewModel) {
    var selectedTab by rememberSaveable { mutableStateOf(DtrTab.DASHBOARD) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showPdfPreview by remember { mutableStateOf(false) }
    var previewProfile by remember { mutableStateOf<Profile?>(null) }
    var previewLogs by remember { mutableStateOf<List<TimeLog>>(emptyList()) }

    val infiniteTransition = rememberInfiniteTransition(label = "ambient_blobs")
    
    // Blob 1 translation values (drifting atmospheric orbs)
    val blob1X by infiniteTransition.animateFloat(
        initialValue = -150f,
        targetValue = 250f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1_x"
    )
    val blob1Y by infiniteTransition.animateFloat(
        initialValue = -150f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1_y"
    )

    // Blob 2 translation values (drifting atmospheric orbs)
    val blob2X by infiniteTransition.animateFloat(
        initialValue = 150f,
        targetValue = -250f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2_x"
    )
    val blob2Y by infiniteTransition.animateFloat(
        initialValue = 150f,
        targetValue = -350f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2_y"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.auradtr.app.ui.theme.GlassSystem.liquidBackgroundBrush)
    ) {
        // Floating 3D Ambient Glowing Blobs
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            // Blob 1 (Top Left): Indigo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x2A4F46E5), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(blob1X, blob1Y),
                    radius = size.width * 0.8f
                ),
                center = androidx.compose.ui.geometry.Offset(blob1X, blob1Y),
                radius = size.width * 0.8f
            )
            // Blob 2 (Bottom Right): Violet/Magenta
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x22EC4899), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width + blob2X, size.height + blob2Y),
                    radius = size.width * 0.9f
                ),
                center = androidx.compose.ui.geometry.Offset(size.width + blob2X, size.height + blob2Y),
                radius = size.width * 0.9f
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "AURA DTR",
                            fontWeight = FontWeight.W900,
                            fontSize = 19.sp,
                            letterSpacing = 2.sp,
                            color = Color.White
                        )
                    },
                    actions = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                val profile = viewModel.profile.firstOrNull()
                                val logs = viewModel.allLogs.firstOrNull() ?: emptyList()
                                if (profile != null) {
                                    previewProfile = profile
                                    previewLogs = logs
                                    showPdfPreview = true
                                } else {
                                    Toast.makeText(context, "Please configure profile settings first.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export PDF",
                                tint = Color(0xFF14B8A6) // Neon Teal
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            },
            bottomBar = {
                // Frosted Glass Floating Dock
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .glassCard(cornerRadius = 24, isDark = true)
                        .shadow(24.dp, RoundedCornerShape(24.dp), clip = false)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        DtrTab.values().forEach { tab ->
                            val isSelected = selectedTab == tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent
                                    )
                                    .clickable { selectedTab = tab }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        tint = if (isSelected) Color(0xFF14B8A6) else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tab.label,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    DtrTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                    DtrTab.CLOCK -> ClockScreen(viewModel = viewModel)
                    DtrTab.LOGBOOK -> LogbookScreen(viewModel = viewModel)
                    DtrTab.PROFILE -> ProfileScreen(viewModel = viewModel)
                    DtrTab.SUPERVISOR -> SupervisorScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Print Preview and Layout Customization Dialog
    if (showPdfPreview && previewProfile != null) {
        PdfPreviewDialog(
            profile = previewProfile!!,
            logs = previewLogs,
            onDismiss = { showPdfPreview = false },
            onDownload = { template, filteredLogs, coverageText ->
                try {
                    val downloadsDir = context.getExternalFilesDir(null)
                    val pdfFile = File(downloadsDir, "OJT_DTR_${previewProfile!!.studentName.replace(" ", "_")}.pdf")
                    
                    PdfExporter().exportDtrToPdf(context, previewProfile!!, filteredLogs, pdfFile, templateType = template, coverageText = coverageText)
                    showPdfPreview = false
                    
                    // Directly launch native sharing Intent chooser immediately (QoL 6)
                    val pdfUri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "com.auradtr.app.fileprovider",
                        pdfFile
                    )
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(android.content.Intent.EXTRA_STREAM, pdfUri)
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "OJT DTR Timesheet - ${previewProfile!!.studentName}")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share your OJT DTR PDF Timesheet"))

                    Toast.makeText(
                        context,
                        "PDF compiled successfully using $template template!",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
