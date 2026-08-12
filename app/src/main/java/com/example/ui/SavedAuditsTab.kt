package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*

@Composable
fun SavedAuditsTab(
    viewModel: AuditViewModel,
    lang: String,
    onOpenSettings: () -> Unit,
    onNavigateToForm: () -> Unit
) {
    val context = LocalContext.current
    val isAr = lang == "ar"

    // Collect data from database flows
    val savedReports by viewModel.allReports.collectAsStateWithLifecycle()
    val allFindings by viewModel.allFindings.collectAsStateWithLifecycle()
    val allHistoryRows by viewModel.allHistoryRows.collectAsStateWithLifecycle()

    // State managers
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedStatusFilter by rememberSaveable { mutableStateOf("All") } // "All", "Draft", "Ready to Submit", "Submitted"
    var isGridView by rememberSaveable { mutableStateOf(false) }
    var starredAudits by remember { mutableStateOf(setOf<Int>()) }

    // Dynamic metrics calculation
    val totalSavedCount = savedReports.size
    val totalNcrsCount = allFindings.count { (it.type ?: "").uppercase() == "NCR" }
    val totalObsCount = allFindings.count { (it.type ?: "").uppercase() == "OBS" || (it.type ?: "").uppercase() == "OBSERVATION" }
    val totalPreviousCount = allHistoryRows.size + savedReports.count { (it.auditeeOverallStatus ?: "") == "Submitted" }

    // Filter audits search
    val filteredReports = savedReports.filter { report ->
        val matchesSearch = (report.projectName ?: "").contains(searchQuery, ignoreCase = true) ||
                (report.auditNumber ?: "").contains(searchQuery, ignoreCase = true) ||
                (report.location ?: "").contains(searchQuery, ignoreCase = true) ||
                (report.auditorName ?: "").contains(searchQuery, ignoreCase = true)

        val matchesStatus = if (selectedStatusFilter == "All") {
            true
        } else {
            (report.auditeeOverallStatus ?: "").equals(selectedStatusFilter, ignoreCase = true)
        }

        matchesSearch && matchesStatus
    }

    Scaffold(
        containerColor = Color(0xFF010E24) // Deep high-contrast dark theme background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // =================== HEADER SECTION ===================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                IconButton(
                    onClick = onNavigateToForm,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF031633), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Brand Center Logo (Exactly matching screenshot)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "QC",
                            style = TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "QC Logo",
                            tint = Color(0xFF00BFA5),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "INTERNAL AUDIT",
                        style = TextStyle(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 2.sp
                        )
                    )
                }

                // Cloud actions / Settings menu
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, if (isAr) "مزامنة السحابة..." else "Syncing with Cloud...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF031633), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Cloud Upload",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF031633), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            }

            // Title & Description
            Text(
                text = if (isAr) "التدقيقات المحفوظة" else "Saved Audits",
                style = TextStyle(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isAr) "عرض وإدارة جميع تقييمات التدقيق المحفوظة لديك." else "View and manage all your saved audit assessments.",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // =================== METRICS CARDS ROW ===================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Saved Card
                MetricCard(
                    title = if (isAr) "المحفوظة" else "Total Saved",
                    count = totalSavedCount,
                    icon = Icons.Default.Folder,
                    glowColor = Color(0xFF00BFA5), // Teal
                    modifier = Modifier.width(115.dp)
                )

                // Drafts / NCR Card
                MetricCard(
                    title = if (isAr) "تقارير NCR" else "Total NCRs",
                    count = totalNcrsCount,
                    icon = Icons.Default.Warning,
                    glowColor = Color(0xFFFF9100), // Orange
                    modifier = Modifier.width(115.dp)
                )

                // Ready to Submit / OBS Card
                MetricCard(
                    title = if (isAr) "ملاحظات OBS" else "Total OBS",
                    count = totalObsCount,
                    icon = Icons.Default.CheckCircleOutline,
                    glowColor = Color(0xFF2979FF), // Blue
                    modifier = Modifier.width(115.dp)
                )

                // Submitted / Previous Audits Card
                MetricCard(
                    title = if (isAr) "السابقة" else "Prev Audits",
                    count = totalPreviousCount,
                    icon = Icons.Default.History,
                    glowColor = Color(0xFFB388FF), // Purple
                    modifier = Modifier.width(115.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // =================== SEARCH & FILTER BAR ===================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search field
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (isAr) "ابحث عن التدقيق بالاسم، الرقم..." else "Search audits by name, ID, department...",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF031633),
                        unfocusedContainerColor = Color(0xFF031633),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    singleLine = true
                )

                // Filter Option Dropdown Trigger Block
                var showFilterMenu by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { showFilterMenu = !showFilterMenu },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF031633)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (selectedStatusFilter == "All") (if (isAr) "تصفية" else "Filter") else selectedStatusFilter,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                        modifier = Modifier.background(Color(0xFF031633))
                    ) {
                        listOf("All", "Draft", "Ready to Submit", "Submitted").forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status, color = Color.White, fontSize = 13.sp) },
                                onClick = {
                                    selectedStatusFilter = status
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                }

                // Layout Toggles (Grid vs List)
                IconButton(
                    onClick = { isGridView = false },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (!isGridView) Color(0xFF00BFA5).copy(alpha = 0.2f) else Color(0xFF031633),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListBulleted,
                        contentDescription = "List View",
                        tint = if (!isGridView) Color(0xFF00BFA5) else Color.White
                    )
                }

                IconButton(
                    onClick = { isGridView = true },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isGridView) Color(0xFF00BFA5).copy(alpha = 0.2f) else Color(0xFF031633),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Grid View",
                        tint = if (isGridView) Color(0xFF00BFA5) else Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            // =================== AUDITS CONTENT LIST ===================
            if (filteredReports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Empty",
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isAr) "لا توجد تدقيقات مطابقة لخيارات البحث" else "No audit assessments matching your criteria were found.",
                            style = TextStyle(
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredReports, key = { it.id }) { report ->
                            AuditGridCard(
                                report = report,
                                isStarred = starredAudits.contains(report.id),
                                onStarToggle = {
                                    starredAudits = if (starredAudits.contains(report.id)) {
                                        starredAudits - report.id
                                    } else {
                                        starredAudits + report.id
                                    }
                                },
                                viewModel = viewModel,
                                onOpenReport = {
                                    viewModel.loadReport(report.id)
                                    if (report.auditeeOverallStatus.equals("Submitted", ignoreCase = true)) {
                                        // Navigate to Tracking Summary
                                        onNavigateToForm() // or similar (handled in MainActivity)
                                    } else {
                                        onNavigateToForm()
                                    }
                                    Toast.makeText(context, "${report.projectName} Loaded", Toast.LENGTH_SHORT).show()
                                },
                                isAr = isAr
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredReports, key = { it.id }) { report ->
                            AuditListCard(
                                report = report,
                                isStarred = starredAudits.contains(report.id),
                                onStarToggle = {
                                    starredAudits = if (starredAudits.contains(report.id)) {
                                        starredAudits - report.id
                                    } else {
                                        starredAudits + report.id
                                    }
                                },
                                viewModel = viewModel,
                                onOpenReport = {
                                    viewModel.loadReport(report.id)
                                    onNavigateToForm()
                                    Toast.makeText(context, "${report.projectName} Loaded", Toast.LENGTH_SHORT).show()
                                },
                                isAr = isAr
                            )
                        }
                    }
                }
            }
        }
    }
}

// =================== REUSABLE METRIC SUMMARY CARD ===================
@Composable
fun MetricCard(
    title: String,
    count: Int,
    icon: ImageVector,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(82.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF031633)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circle icon
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(glowColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = glowColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Count
                Text(
                    text = "$count",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Label
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.6f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))
            // Underline highlight block matching screenshot
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(glowColor)
            )
        }
    }
}

// =================== LIST CARD LAYOUT ===================
@Composable
fun AuditListCard(
    report: AuditReport,
    isStarred: Boolean,
    onStarToggle: () -> Unit,
    viewModel: AuditViewModel,
    onOpenReport: () -> Unit,
    isAr: Boolean
) {
    val context = LocalContext.current
    val status = (report.auditeeOverallStatus ?: "").ifEmpty { "Draft" }

    // Map departments to high-quality visual icons & colors
    val (deptIcon, deptColor) = remember(report.location) {
        val locLower = (report.location ?: "").lowercase()
        when {
            locLower.contains("finance") -> Pair(Icons.Default.Business, Color(0xFF00BFA5))       // Teal
            locLower.contains("procurement") -> Pair(Icons.Default.ShoppingCart, Color(0xFFFF9100)) // Orange
            locLower.contains("it") -> Pair(Icons.Default.Security, Color(0xFF2979FF))            // Blue
            locLower.contains("production") || locLower.contains("manufac") -> Pair(Icons.Default.Build, Color(0xFF00E676)) // Light green
            locLower.contains("human") || locLower.contains("hr") -> Pair(Icons.Default.Group, Color(0xFFD500F9))           // Purple
            else -> Pair(Icons.Default.Assignment, Color(0xFFE0E0E0)) // Default Grey
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenReport() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF031633)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF00BFA5).copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Main Top Title & Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle Badge Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(deptColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = deptIcon,
                        contentDescription = "Audit Icon",
                        tint = deptColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))

                // Content Area
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = report.projectName,
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Star/Favorites Icon Toggle
                        Icon(
                            imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Starred",
                            tint = if (isStarred) Color(0xFFFFD600) else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onStarToggle() }
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = report.location.ifEmpty { "General Worksite" },
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }

                // Status Pill badge (exactly matching screenshot colorings)
                val (statusBg, statusText) = when (status.lowercase()) {
                    "draft" -> Pair(Color(0xFF00E676).copy(alpha = 0.15f), Color(0xFF00E676))
                    "ready to submit" -> Pair(Color(0xFF2979FF).copy(alpha = 0.15f), Color(0xFF2979FF))
                    "submitted" -> Pair(Color(0xFFB388FF).copy(alpha = 0.15f), Color(0xFFB388FF))
                    else -> Pair(Color.White.copy(alpha = 0.1f), Color.White)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusText
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Metadata row: Date, Auditor, Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date metadata
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Date",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = report.auditDate,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // Lead auditor
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "AuditorName",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = report.auditorName.ifEmpty { "Auditor" },
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Type
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sell,
                        contentDescription = "Type",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = report.projectType.ifEmpty { "Audit" },
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Divider Line
            Divider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom action row: Actions (Download, Upload, Share) + Load Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Secondary Left-facing utility buttons (Share, Upload, Download)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Share
                    IconButton(
                        onClick = {
                            viewModel.sharePdfForReport(context, report.id)
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Cloud Upload simulation
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Cloud upload successful for report #${report.auditNumber}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Cloud Upload",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Export PDF/Excel
                    IconButton(
                        onClick = {
                            viewModel.exportPdfForReport(context, report.id)
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Report",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Export ZIP package with photos attached
                    IconButton(
                        onClick = {
                            viewModel.exportZipForReport(context, report.id)
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .border(1.dp, Color(0xFF00BFA5).copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = if (isAr) "تنزيل ملف ZIP بالصور" else "Download ZIP with Photos",
                            tint = Color(0xFF00BFA5),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Delete report with confirmation
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .size(34.dp)
                            .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Report",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { 
                                Text(
                                    text = if (isAr) "حذف تقرير التدقيق" else "Delete Audit Report", 
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ) 
                            },
                            text = { 
                                Text(
                                    text = if (isAr) 
                                        "هل أنت متأكد أنك تريد حذف هذا التقرير نهائياً؟ لا يمكن التراجع عن هذا الإجراء." 
                                        else "Are you sure you want to permanently delete this audit report? This action cannot be undone.", 
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp
                                ) 
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.deleteReport(report.id, context)
                                        showDeleteDialog = false
                                        Toast.makeText(
                                            context, 
                                            if (isAr) "تم حذف التقرير بنجاح" else "Audit report deleted successfully", 
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                                ) {
                                    Text(if (isAr) "حذف" else "Delete", color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text(if (isAr) "إلغاء" else "Cancel", color = Color.LightGray)
                                }
                            },
                            containerColor = Color(0xFF031633),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Primary right Button based on status
                val buttonText = when (status.lowercase()) {
                    "draft" -> if (isAr) "استمرار" else "Continue"
                    "ready to submit" -> if (isAr) "مراجعة" else "Review"
                    "submitted" -> if (isAr) "عرض التقرير" else "View Report"
                    else -> if (isAr) "فتح" else "Open"
                }

                Button(
                    onClick = onOpenReport,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

// =================== GRID CARD LAYOUT ===================
@Composable
fun AuditGridCard(
    report: AuditReport,
    isStarred: Boolean,
    onStarToggle: () -> Unit,
    viewModel: AuditViewModel,
    onOpenReport: () -> Unit,
    isAr: Boolean
) {
    val status = (report.auditeeOverallStatus ?: "").ifEmpty { "Draft" }

    val (deptIcon, deptColor) = remember(report.location) {
        val locLower = (report.location ?: "").lowercase()
        when {
            locLower.contains("finance") -> Pair(Icons.Default.Business, Color(0xFF00BFA5))
            locLower.contains("procurement") -> Pair(Icons.Default.ShoppingCart, Color(0xFFFF9100))
            locLower.contains("it") -> Pair(Icons.Default.Security, Color(0xFF2979FF))
            locLower.contains("production") || locLower.contains("manufac") -> Pair(Icons.Default.Build, Color(0xFF00E676))
            locLower.contains("human") || locLower.contains("hr") -> Pair(Icons.Default.Group, Color(0xFFD500F9))
            else -> Pair(Icons.Default.Assignment, Color(0xFFE0E0E0))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenReport() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF031633)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Icon + Star row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(deptColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = deptIcon,
                        contentDescription = "Badge",
                        tint = deptColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Starred",
                        tint = if (isStarred) Color(0xFFFFD600) else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onStarToggle() }
                    )

                    var showDeleteDialog by remember { mutableStateOf(false) }
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252).copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { showDeleteDialog = true }
                    )

                    if (showDeleteDialog) {
                        val context = LocalContext.current
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { 
                                Text(
                                    text = if (isAr) "حذف تقرير التدقيق" else "Delete Audit Report", 
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ) 
                            },
                            text = { 
                                Text(
                                    text = if (isAr) 
                                        "هل أنت متأكد أنك تريد حذف هذا التقرير نهائياً؟" 
                                        else "Are you sure you want to permanently delete this audit report?", 
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                ) 
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.deleteReport(report.id, context)
                                        showDeleteDialog = false
                                        Toast.makeText(
                                            context, 
                                            if (isAr) "تم حذف التقرير بنجاح" else "Audit report deleted successfully", 
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(if (isAr) "حذف" else "Delete", color = Color.White, fontSize = 11.sp)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showDeleteDialog = false },
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(if (isAr) "إلغاء" else "Cancel", color = Color.LightGray, fontSize = 11.sp)
                                }
                            },
                            containerColor = Color(0xFF031633),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Project Title & Status
            Text(
                text = report.projectName,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = report.location.ifEmpty { "General Worksite" },
                style = TextStyle(
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Status Badge
            val (statusBg, statusText) = when (status.lowercase()) {
                "draft" -> Pair(Color(0xFF00E676).copy(alpha = 0.15f), Color(0xFF00E676))
                "ready to submit" -> Pair(Color(0xFF2979FF).copy(alpha = 0.15f), Color(0xFF2979FF))
                "submitted" -> Pair(Color(0xFFB388FF).copy(alpha = 0.15f), Color(0xFFB388FF))
                else -> Pair(Color.White.copy(alpha = 0.1f), Color.White)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = status,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusText
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Load Action button
            Button(
                onClick = onOpenReport,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
            ) {
                Text(
                    text = if (status.lowercase() == "draft") (if (isAr) "استمرار" else "Continue") else (if (isAr) "مراجعة" else "Review"),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}
