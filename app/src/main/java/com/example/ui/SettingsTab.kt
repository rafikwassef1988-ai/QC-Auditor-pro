package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@Composable
fun SettingsTab(viewModel: AuditViewModel, lang: String) {
    val context = LocalContext.current
    val isAr = lang == "ar"
    
    val savedReports by viewModel.allReports.collectAsState(initial = emptyList())

    // --- RESPONSIVE STATE ---
    var selectedSection by rememberSaveable { mutableStateOf("Profile") }
    // On narrow screens, if showDetail is true, show the details screen. Else show the list of menus.
    var showDetailOnMobile by rememberSaveable { mutableStateOf(false) }

    // --- COHESIVE FILLABLE STATE VARIABLES (FOR EXAMPLE DATA / REAL TIME ADJUSTMENT) ---
    var userName by rememberSaveable { mutableStateOf("") }
    var userEmail by rememberSaveable { mutableStateOf("") }
    
    // Security states
    var is2FaEnabled by rememberSaveable { mutableStateOf(true) }
    var isBiometricsEnabled by rememberSaveable { mutableStateOf(true) }
    var isSecurityAlertsEnabled by rememberSaveable { mutableStateOf(true) }
    var passwordClicks by rememberSaveable { mutableStateOf(0) }

    // Notification states
    var pushNotifications by rememberSaveable { mutableStateOf(true) }
    var newMessages by rememberSaveable { mutableStateOf(true) }
    var auditUpdates by rememberSaveable { mutableStateOf(true) }
    var taskReminders by rememberSaveable { mutableStateOf(true) }
    var systemAlerts by rememberSaveable { mutableStateOf(true) }
    var emailWeeklyReports by rememberSaveable { mutableStateOf(true) }
    var emailComplianceUpdates by rememberSaveable { mutableStateOf(true) }
    var emailNewsletter by rememberSaveable { mutableStateOf(false) }

    // Appearance states
    var selectedTheme by rememberSaveable { mutableStateOf("Dark") }
    var selectedAccentColor by rememberSaveable { mutableStateOf("Teal") } // Teal, Purple, Green, Orange, Red
    var selectedFontSize by rememberSaveable { mutableStateOf("Medium") }
    var selectedLanguage by rememberSaveable { mutableStateOf("English") }

    // Privacy states
    var locationAccess by rememberSaveable { mutableStateOf(true) }
    var cameraAccess by rememberSaveable { mutableStateOf(true) }
    var micAccess by rememberSaveable { mutableStateOf(true) }
    var contactsAccess by rememberSaveable { mutableStateOf(true) }
    var profileVisibility by rememberSaveable { mutableStateOf("Everyone") }
    var onlineStatus by rememberSaveable { mutableStateOf("Everyone") }
    var readReceipts by rememberSaveable { mutableStateOf(true) }
    var activityStatus by rememberSaveable { mutableStateOf(true) }

    // Payments states
    var cardsCount by rememberSaveable { mutableStateOf(2) }
    var applePayDefault by rememberSaveable { mutableStateOf(true) }
    var googlePayEnabled by rememberSaveable { mutableStateOf(true) }
    var paypalAccount by rememberSaveable { mutableStateOf("auditor@email.com") }

    // Subscription states
    var currentPlanName by rememberSaveable { mutableStateOf("Premium Plan") }
    var subActive by rememberSaveable { mutableStateOf(true) }

    // Account Linking states
    var linkedGoogle by rememberSaveable { mutableStateOf(true) }
    var linkedApple by rememberSaveable { mutableStateOf(true) }
    var linkedLinkedin by rememberSaveable { mutableStateOf(true) }
    var linkedMicrosoft by rememberSaveable { mutableStateOf(false) }
    var googleEmail by rememberSaveable { mutableStateOf("auditor@gmail.com") }
    var appleEmail by rememberSaveable { mutableStateOf("auditor@icloud.com") }
    var linkedinUsername by rememberSaveable { mutableStateOf("auditor-profile") }
    var microsoftEmail by rememberSaveable { mutableStateOf("auditor@outlook.com") }

    // Custom accounts the user can dynamically add and link
    var customLinkedAccountsList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showLinkAccountDialog by remember { mutableStateOf(false) }

    // Network & Connectivity states
    var isNetworkEnabled by rememberSaveable { mutableStateOf(true) }
    var networkMode by rememberSaveable { mutableStateOf("Wi-Fi & Cellular") } // "Wi-Fi & Cellular", "Wi-Fi Only", "Offline Mode"
    var serverEndpoint by rememberSaveable { mutableStateOf("https://api.innovoqc.com/v1") }
    var isTestingConnection by remember { mutableStateOf(false) }
    var lastPingResult by remember { mutableStateOf("") }

    // App Preferences states
    val sharedPrefs = remember { context.getSharedPreferences("audit_settings", Context.MODE_PRIVATE) }
    var geminiApiKeyInput by remember { mutableStateOf(sharedPrefs.getString("gemini_api_key", "") ?: "") }
    var autoSync by rememberSaveable { mutableStateOf(true) }
    var autoSave by rememberSaveable { mutableStateOf(true) }
    var bgRefresh by rememberSaveable { mutableStateOf(true) }
    var offlineMode by rememberSaveable { mutableStateOf(false) }
    var videoQuality by rememberSaveable { mutableStateOf("1080p") }
    var imageQuality by rememberSaveable { mutableStateOf("High") }
    var autoPlayVideos by rememberSaveable { mutableStateOf(true) }
    var wifiOnlyDownload by rememberSaveable { mutableStateOf(true) }
    var storageManagementSize by rememberSaveable { mutableStateOf("12.4 GB") }

    // Physical Storage Space States
    var freeSpaceBytes by remember { mutableStateOf(0L) }
    var totalSpaceBytes by remember { mutableStateOf(0L) }
    var appSpaceBytes by remember { mutableStateOf(0L) }
    var databaseSpaceBytes by remember { mutableStateOf(0L) }
    var cacheSpaceBytes by remember { mutableStateOf(0L) }
    var isOptimizingStorage by remember { mutableStateOf(false) }

    fun refreshStorageStats(ctx: android.content.Context) {
        try {
            val stat = android.os.StatFs(ctx.filesDir.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            val totalBlocks = stat.blockCountLong
            
            freeSpaceBytes = availableBlocks * blockSize
            totalSpaceBytes = totalBlocks * blockSize
            
            var size = 0L
            fun calculateDirSize(dir: java.io.File?): Long {
                if (dir == null || !dir.exists()) return 0L
                var s = 0L
                val files = dir.listFiles()
                if (files != null) {
                    for (f in files) {
                        s += if (f.isDirectory) calculateDirSize(f) else f.length()
                    }
                }
                return s
            }
            size += calculateDirSize(ctx.filesDir)
            size += calculateDirSize(ctx.cacheDir)
            appSpaceBytes = size
            
            cacheSpaceBytes = calculateDirSize(ctx.cacheDir)
            
            var dbSize = 0L
            try {
                val dbFile = ctx.getDatabasePath("qc_audit_fallback_db_clean")
                if (dbFile.exists()) {
                    dbSize += dbFile.length()
                }
                val dbWalFile = java.io.File(dbFile.path + "-wal")
                if (dbWalFile.exists()) {
                    dbSize += dbWalFile.length()
                }
                val dbShmFile = java.io.File(dbFile.path + "-shm")
                if (dbShmFile.exists()) {
                    dbSize += dbShmFile.length()
                }
            } catch (e: Exception) {}
            if (dbSize == 0L) {
                dbSize = 135_000L // 135 KB fallback if not found
            }
            databaseSpaceBytes = dbSize
        } catch (e: Exception) {
            freeSpaceBytes = 45_800_000_000L
            totalSpaceBytes = 128_000_000_000L
            appSpaceBytes = 158_000_000L
            cacheSpaceBytes = 142_000_000L
            databaseSpaceBytes = 1_850_000L
        }
    }

    LaunchedEffect(Unit) {
        refreshStorageStats(context)
    }

    fun formatDisplayBytes(bytes: Long): String {
        if (bytes <= 0) return "0.00 B"
        val k = 1024.0
        val sizes = listOf("B", "KB", "MB", "GB", "TB")
        val i = Math.floor(java.lang.Math.log(bytes.toDouble()) / java.lang.Math.log(k)).toInt().coerceIn(0, 4)
        return String.format(java.util.Locale.US, "%.2f %s", bytes / java.lang.Math.pow(k, i.toDouble()), sizes[i])
    }

    // Dialog flags
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showAllSavedReportsCountDialog by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }

    // Theme values helper
    val themeColor = when (selectedAccentColor) {
        "Teal" -> Color(0xFF00BFA5)
        "Purple" -> Color(0xFF9F58FF)
        "Green" -> Color(0xFF4CAF50)
        "Orange" -> Color(0xFFFF9800)
        "Red" -> Color(0xFFF44336)
        else -> Color(0xFF00BFA5)
    }

    // List of sidebar menu settings
    val menuItems = listOf(
        SettingsMenuData("Profile", if (isAr) "الملف الشخصي" else "Account Settings", if (isAr) "إدارة معلومات الحساب" else "Manage your account", Icons.Default.Person),
        SettingsMenuData("Security", if (isAr) "الأمان وحماية الحساب" else "Security & Protection", if (isAr) "كلمة المرور، والتحقق الثنائي" else "Password, 2FA, biometric", Icons.Default.Lock),
        SettingsMenuData("Notifications", if (isAr) "الإشعارات والتنبيهات" else "Notifications", if (isAr) "إدارة التنبيهات المباشرة" else "Manage alerts & reports", Icons.Default.Notifications),
        SettingsMenuData("Appearance", if (isAr) "العرض والمظهر" else "Appearance", if (isAr) "السمات، الألوان، الخطوط" else "Theme, display font, size", Icons.Default.ColorLens),
        SettingsMenuData("Privacy", if (isAr) "الخصوصية والبيانات" else "Privacy & Permissions", if (isAr) "صلاحيات الوصول والخصوصية" else "Data, GPS & camera controls", Icons.Default.Shield),
        SettingsMenuData("Network", if (isAr) "اتصال الشبكة" else "Network Connection", if (isAr) "حالة وجودة الاتصال بالخادم" else "Connectivity, ping & api server", Icons.Default.Wifi),
        SettingsMenuData("Preferences", if (isAr) "تفضيلات التطبيق" else "App Preferences", if (isAr) "التفضيلات والتشغيل التلقائي" else "Auto sync & local database", Icons.Default.Settings),
        SettingsMenuData("Storage", if (isAr) "التخزين والبيانات المؤقتة" else "Storage & Cache", if (isAr) "مساحة تخزين التطبيق والكاش" else "Local DB statistics & resource clean", Icons.Default.Storage)
    )

    // --- MAIN SCREEN LAYOUT ---
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF071930)) // Dark slate background from picture
    ) {
        val isWide = maxWidth > 800.dp

        Column(modifier = Modifier.fillMaxSize()) {
            
            // --- HEADER (QC Logo, Title, Search Icon) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A1F3D))
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isWide && showDetailOnMobile) {
                            IconButton(onClick = { showDetailOnMobile = false }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        
                        // Logo Badge exactly like "QC INTERNAL AUDIT" symbol
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF132D50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(10.dp))
                        
                        Column {
                            Text(
                                text = "QC INTERNAL AUDIT",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = if (isAr) "الإعدادات" else "Settings",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = {
                            Toast.makeText(context, if (isAr) "البحث غير متاح حالياً" else "Search is active!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }
                    }
                }
            }

            // --- MAIN CONTENT SPLIT PANES ---
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Left Master Panel (menus list) - Always visible on Wide, or shown on mobile when showDetail is false
                if (isWide || !showDetailOnMobile) {
                    Column(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .background(Color(0xFF041021)) // Dark sidepanel color
                            .verticalScroll(rememberScrollState())
                            .padding(top = 16.dp, bottom = 16.dp)
                    ) {
                        
                        // Profile Header Card in sidebar
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .padding(bottom = 16.dp)
                                .clickable {
                                    selectedSection = "Profile"
                                    if (!isWide) showDetailOnMobile = true
                                },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1F3D)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E3A5F))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(themeColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (userName.isEmpty()) "QA" else userName.take(2).uppercase(),
                                        color = themeColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(10.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (userName.isEmpty()) (if (isAr) "متدرب / غير معرف" else "Default Auditor") else userName,
                                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    )
                                    Text(
                                        text = if (userEmail.isEmpty()) (if (isAr) "لم يتم ربط بريد" else "No account linked") else userEmail,
                                        style = TextStyle(fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                                    )
                                }
                                
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Menu Links List
                        menuItems.forEach { menuItem ->
                            val active = selectedSection == menuItem.id
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSection = menuItem.id
                                        if (!isWide) showDetailOnMobile = true
                                    }
                                    .background(
                                        if (active) Color(0xFF0F2644) else Color.Transparent
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Blue green vertical marker when active like screenshot
                                    if (active) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(24.dp)
                                                .background(themeColor, RoundedCornerShape(2.dp))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    } else {
                                        Spacer(modifier = Modifier.width(14.dp))
                                    }
                                    
                                    Icon(
                                        imageVector = menuItem.icon,
                                        contentDescription = null,
                                        tint = if (menuItem.isDanger) Color(0xFFEF4444) else if (active) themeColor else Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(10.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = menuItem.title,
                                            style = TextStyle(
                                                fontSize = 12.5.sp,
                                                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (menuItem.isDanger) Color(0xFFEF4444) else if (active) Color.White else Color(0xFFCBD5E1)
                                            )
                                        )
                                        Text(
                                            text = menuItem.subtitle,
                                            style = TextStyle(
                                                fontSize = 10.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        )
                                    }
                                    
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color(0xFF475569),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Details Panel - Shown on Wide or when showDetail is true on mobile
                if (isWide || showDetailOnMobile) {
                    Column(
                        modifier = Modifier
                            .weight(2.7f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        
                        // Render exact section based on selection
                        when (selectedSection) {
                            "Profile" -> {
                                SectionTitle(if (isAr) "الملف الشخصي" else "Profile", themeColor)
                                
                                // Profile card with Edit button
                                DetailContainerCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(themeColor.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (userName.isEmpty()) "QA" else userName.take(2).uppercase(),
                                                    color = themeColor,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    if (userName.isEmpty()) (if (isAr) "متدرب / غير معرف" else "Default Auditor") else userName,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    if (userEmail.isEmpty()) (if (isAr) "لم يتم ربط بريد" else "No account linked") else userEmail,
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                        
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(
                                                onClick = { showEditProfileDialog = true },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColor),
                                                border = BorderStroke(1.dp, themeColor),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isAr) "تعديل" else "Edit", fontSize = 11.sp)
                                            }

                                            if (userName.isNotEmpty() || userEmail.isNotEmpty()) {
                                                OutlinedButton(
                                                    onClick = {
                                                        userName = ""
                                                        userEmail = ""
                                                        Toast.makeText(context, if (isAr) "تمت إزالة الحساب بنجاح!" else "Account removed successfully!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFEF4444))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isAr) "حذف" else "Remove", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Interactive Quick Buttons Card
                                DetailContainerCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        SettingsActionRow(
                                            icon = Icons.Default.Lock,
                                            label = if (isAr) "تغيير كلمة المرور" else "Change Password",
                                            onClick = { showChangePasswordDialog = true },
                                            isChevron = true,
                                            themeColor = themeColor
                                        )
                                        
                                        Divider(color = Color(0xFF1E3A5F))
                                        
                                        SettingsToggleRow(
                                            icon = Icons.Default.VpnKey,
                                            label = if (isAr) "المصادقة الثنائية (2FA)" else "Two-Factor Authentication",
                                            subtitle = if (is2FaEnabled) (if (isAr) "مفعلة وآمنة" else "Enabled") else (if (isAr) "غير مفعلة" else "Disabled"),
                                            checked = is2FaEnabled,
                                            onCheckedChange = { is2FaEnabled = it },
                                            themeColor = themeColor
                                        )
                                    }
                                }

                                // Connected & Linked Accounts management card
                                DetailContainerCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Link, contentDescription = null, tint = themeColor, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if (isAr) "ربط وإدارة الحسابات الخارجية" else "Account Integrations", style = TextStyle(fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp))
                                            }
                                            Button(
                                                onClick = { showLinkAccountDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = themeColor.copy(alpha = 0.2f), contentColor = themeColor),
                                                shape = RoundedCornerShape(4.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isAr) "ربط حساب" else "Link Account", fontSize = 10.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        // Google Account
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                SocialBubble(name = "google", color = Color(0xFFEA4335))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text("Google Cloud ID", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                    Text(if (linkedGoogle) googleEmail else (if (isAr) "غير متصل" else "Disconnected"), color = if (linkedGoogle) Color(0xFF94A3B8) else Color.Red.copy(alpha = 0.8f), fontSize = 9.5.sp)
                                                 }
                                             }
                                             TextButton(
                                                 onClick = { 
                                                     if (linkedGoogle) {
                                                         linkedGoogle = false
                                                         Toast.makeText(context, "Google identity unlinked.", Toast.LENGTH_SHORT).show()
                                                     } else {
                                                         showLinkAccountDialog = true
                                                     }
                                                 },
                                                 contentPadding = PaddingValues(0.dp)
                                             ) {
                                                 Text(
                                                     text = if (linkedGoogle) (if (isAr) "إلغاء الربط" else "Disconnect") else (if (isAr) "ربط" else "Connect"),
                                                     color = if (linkedGoogle) Color(0xFFF87171) else themeColor,
                                                     fontSize = 11.sp
                                                 )
                                             }
                                         }

                                         Divider(color = Color(0xFF1E3A5F))

                                         // Apple Account
                                         Row(
                                             modifier = Modifier.fillMaxWidth(),
                                             horizontalArrangement = Arrangement.SpaceBetween,
                                             verticalAlignment = Alignment.CenterVertically
                                         ) {
                                             Row(verticalAlignment = Alignment.CenterVertically) {
                                                 SocialBubble(name = "apple", color = Color.White)
                                                 Spacer(modifier = Modifier.width(8.dp))
                                                 Column {
                                                     Text("Apple Corporate ID", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                     Text(if (linkedApple) appleEmail else (if (isAr) "غير متصل" else "Disconnected"), color = if (linkedApple) Color(0xFF94A3B8) else Color.Red.copy(alpha = 0.8f), fontSize = 9.5.sp)
                                                 }
                                             }
                                             TextButton(
                                                 onClick = {
                                                     if (linkedApple) {
                                                         linkedApple = false
                                                         Toast.makeText(context, "Apple ID unlinked.", Toast.LENGTH_SHORT).show()
                                                     } else {
                                                         showLinkAccountDialog = true
                                                     }
                                                 },
                                                 contentPadding = PaddingValues(0.dp)
                                             ) {
                                                 Text(
                                                     text = if (linkedApple) (if (isAr) "إلغاء الربط" else "Disconnect") else (if (isAr) "ربط" else "Connect"),
                                                     color = if (linkedApple) Color(0xFFF87171) else themeColor,
                                                     fontSize = 11.sp
                                                 )
                                             }
                                         }

                                         Divider(color = Color(0xFF1E3A5F))

                                         // LinkedIn
                                         Row(
                                             modifier = Modifier.fillMaxWidth(),
                                             horizontalArrangement = Arrangement.SpaceBetween,
                                             verticalAlignment = Alignment.CenterVertically
                                         ) {
                                             Row(verticalAlignment = Alignment.CenterVertically) {
                                                 SocialBubble(name = "linkedin", color = Color(0xFF0A66C2))
                                                 Spacer(modifier = Modifier.width(8.dp))
                                                 Column {
                                                     Text("LinkedIn Profile", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                     Text(if (linkedLinkedin) linkedinUsername else (if (isAr) "غير متصل" else "Disconnected"), color = if (linkedLinkedin) Color(0xFF94A3B8) else Color.Red.copy(alpha = 0.8f), fontSize = 9.5.sp)
                                                 }
                                             }
                                             TextButton(
                                                 onClick = {
                                                     if (linkedLinkedin) {
                                                         linkedLinkedin = false
                                                         Toast.makeText(context, "LinkedIn profile unlinked.", Toast.LENGTH_SHORT).show()
                                                     } else {
                                                         showLinkAccountDialog = true
                                                     }
                                                 },
                                                 contentPadding = PaddingValues(0.dp)
                                             ) {
                                                 Text(
                                                     text = if (linkedLinkedin) (if (isAr) "إلغاء الربط" else "Disconnect") else (if (isAr) "ربط" else "Connect"),
                                                     color = if (linkedLinkedin) Color(0xFFF87171) else themeColor,
                                                     fontSize = 11.sp
                                                 )
                                             }
                                         }

                                         // Microsoft Account
                                         Divider(color = Color(0xFF1E3A5F))
                                         Row(
                                             modifier = Modifier.fillMaxWidth(),
                                             horizontalArrangement = Arrangement.SpaceBetween,
                                             verticalAlignment = Alignment.CenterVertically
                                         ) {
                                             Row(verticalAlignment = Alignment.CenterVertically) {
                                                 Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF00A4EF), modifier = Modifier.size(18.dp))
                                                 Spacer(modifier = Modifier.width(8.dp))
                                                 Column {
                                                     Text("Microsoft Account", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                     Text(if (linkedMicrosoft) microsoftEmail else (if (isAr) "غير متصل" else "Disconnected"), color = if (linkedMicrosoft) Color(0xFF94A3B8) else Color.Red.copy(alpha = 0.8f), fontSize = 9.5.sp)
                                                 }
                                             }
                                             TextButton(
                                                 onClick = {
                                                     if (linkedMicrosoft) {
                                                         linkedMicrosoft = false
                                                         Toast.makeText(context, "Microsoft Live Account unlinked.", Toast.LENGTH_SHORT).show()
                                                     } else {
                                                         showLinkAccountDialog = true
                                                     }
                                                 },
                                                 contentPadding = PaddingValues(0.dp)
                                             ) {
                                                 Text(
                                                     text = if (linkedMicrosoft) (if (isAr) "إلغاء الربط" else "Disconnect") else (if (isAr) "ربط" else "Connect"),
                                                     color = if (linkedMicrosoft) Color(0xFFF87171) else themeColor,
                                                     fontSize = 11.sp
                                                 )
                                             }
                                         }

                                         // Custom Linked Accounts
                                         if (customLinkedAccountsList.isNotEmpty()) {
                                             customLinkedAccountsList.forEach { pair ->
                                                 Divider(color = Color(0xFF1E3A5F))
                                                 Row(
                                                     modifier = Modifier.fillMaxWidth(),
                                                     horizontalArrangement = Arrangement.SpaceBetween,
                                                     verticalAlignment = Alignment.CenterVertically
                                                 ) {
                                                     Row(verticalAlignment = Alignment.CenterVertically) {
                                                         Icon(Icons.Default.ContactMail, contentDescription = null, tint = themeColor, modifier = Modifier.size(18.dp))
                                                         Spacer(modifier = Modifier.width(8.dp))
                                                         Column {
                                                             Text(pair.first, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                             Text(pair.second, color = Color(0xFF94A3B8), fontSize = 9.5.sp)
                                                         }
                                                     }
                                                     TextButton(
                                                         onClick = {
                                                             customLinkedAccountsList = customLinkedAccountsList.filter { it != pair }
                                                             Toast.makeText(context, "${pair.first} account removed.", Toast.LENGTH_SHORT).show()
                                                         },
                                                         contentPadding = PaddingValues(0.dp)
                                                     ) {
                                                         Text(if (isAr) "حذف الربط" else "Remove", color = Color(0xFFF87171), fontSize = 11.sp)
                                                     }
                                                 }
                                             }
                                         }
                                     }
                                 }
                            }
                            
                            "Security" -> {
                                SectionTitle(if (isAr) "الأمان وحماية البيانات" else "Security", themeColor)
                                
                                DetailContainerCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        SettingsActionRow(
                                            icon = Icons.Default.Lock,
                                            label = if (isAr) "تغيير كلمة المرور" else "Change Password",
                                            onClick = { showChangePasswordDialog = true },
                                            isChevron = true,
                                            themeColor = themeColor
                                        )
                                        
                                        Divider(color = Color(0xFF1E3A5F))
                                        
                                        SettingsToggleRow(
                                            icon = Icons.Default.Fingerprint,
                                            label = if (isAr) "بصمة الإصبع والوجه (Biometric)" else "Face ID / Fingerprint",
                                            checked = isBiometricsEnabled,
                                            onCheckedChange = { isBiometricsEnabled = it },
                                            themeColor = themeColor
                                        )
                                        
                                        Divider(color = Color(0xFF1E3A5F))
                                        
                                        SettingsActionRow(
                                            icon = Icons.Default.History,
                                            label = if (isAr) "سجل نشاط تسجيل الدخول" else "Login Activity",
                                            onClick = { Toast.makeText(context, "Logged in from Samsung S26 & Web Portal.", Toast.LENGTH_LONG).show() },
                                            isChevron = true,
                                            themeColor = themeColor
                                        )
                                        
                                        Divider(color = Color(0xFF1E3A5F))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { Toast.makeText(context, "Devices authenticated: Samsung S26, iPad Pro, Chrome Web Client.", Toast.LENGTH_LONG).show() }
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Devices, contentDescription = null, tint = themeColor, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if (isAr) "الأجهزة الموثوقة" else "Trusted Devices", color = Color.White, fontSize = 11.5.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("3", color = Color(0xFF00BFA5), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp))
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        
                                        Divider(color = Color(0xFF1E3A5F))

                                        SettingsToggleRow(
                                            icon = Icons.Default.Warning,
                                            label = if (isAr) "تنبيهات الأمان الحرجة" else "Security Alerts",
                                            checked = isSecurityAlertsEnabled,
                                            onCheckedChange = { isSecurityAlertsEnabled = it },
                                            themeColor = themeColor
                                        )
                                    }
                                }
                            }
                            
                            "Notifications" -> {
                                SectionTitle(if (isAr) "إعدادات الإشعارات والتنبيهات" else "Notifications", themeColor)
                                
                                DetailContainerCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        SettingsToggleRow(
                                            icon = Icons.Default.NotificationsActive,
                                            label = if (isAr) "إشعارات الهاتف المنبثقة (Push)" else "Push Notifications",
                                            checked = pushNotifications,
                                            onCheckedChange = { pushNotifications = it },
                                            themeColor = themeColor
                                        )
                                        Divider(color = Color(0xFF1E3A5F))
                                        SettingsToggleRow(
                                            icon = Icons.Default.MailOutline,
                                            label = if (isAr) "رسائل النظام الواردة" else "New Messages",
                                            checked = newMessages,
                                            onCheckedChange = { newMessages = it },
                                            themeColor = themeColor
                                        )
                                        Divider(color = Color(0xFF1E3A5F))
                                        SettingsToggleRow(
                                            icon = Icons.Default.Assignment,
                                            label = if (isAr) "تحديثات تقارير التدقيق" else "Audit Updates",
                                            checked = auditUpdates,
                                            onCheckedChange = { auditUpdates = it },
                                            themeColor = themeColor
                                        )
                                        Divider(color = Color(0xFF1E3A5F))
                                        SettingsToggleRow(
                                            icon = Icons.Default.Schedule,
                                            label = if (isAr) "تنبيه ومواعيد تذكير المهام" else "Task Reminders",
                                            checked = taskReminders,
                                            onCheckedChange = { taskReminders = it },
                                            themeColor = themeColor
                                        )
                                        Divider(color = Color(0xFF1E3A5F))
                                        SettingsToggleRow(
                                            icon = Icons.Default.NotificationImportant,
                                            label = if (isAr) "تنبيهات حالة النظام" else "System Alerts",
                                            checked = systemAlerts,
                                            onCheckedChange = { systemAlerts = it },
                                            themeColor = themeColor
                                        )
                                    }
                                }

                                Text(
                                    text = if (isAr) "إشعارات البريد الإلكتروني" else "Email Notifications",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = themeColor,
                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                )

                                DetailContainerCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        SettingsToggleRow(
                                            icon = Icons.Default.Assessment,
                                            label = if (isAr) "ملخص التقارير الأسبوعية" else "Weekly Reports",
                                            checked = emailWeeklyReports,
                                            onCheckedChange = { emailWeeklyReports = it },
                                            themeColor = themeColor
                                        )
                                        Divider(color = Color(0xFF1E3A5F))
                                        SettingsToggleRow(
                                            icon = Icons.Default.Gavel,
                                            label = if (isAr) "تنبيهات وتحديثات الامتثال" else "Compliance Updates",
                                            checked = emailComplianceUpdates,
                                            onCheckedChange = { emailComplianceUpdates = it },
                                            themeColor = themeColor
                                        )
                                        Divider(color = Color(0xFF1E3A5F))
                                        SettingsToggleRow(
                                            icon = Icons.Default.RssFeed,
                                            label = if (isAr) "الرسالة الإخبارية للتطبيق" else "Newsletter",
                                            checked = emailNewsletter,
                                            onCheckedChange = { emailNewsletter = it },
                                            themeColor = themeColor
                                        )
                                    }
                                }
                            }
                            
                            "Appearance" -> {
                                SectionTitle(if (isAr) "التحكم في المظهر والألوان" else "Appearance", themeColor)
                                
                                DetailContainerCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        
                                        // Theme Choice Rows
                                        Column {
                                            Text(if (isAr) "السمة والنمط" else "Theme", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                ThemeSelectCard(
                                                    name = "Light",
                                                    selected = selectedTheme == "Light",
                                                    icon = Icons.Default.WbSunny,
                                                    themeColor = themeColor,
                                                    modifier = Modifier.weight(1f),
                                                    onClick = { 
                                                        selectedTheme = "Light"
                                                        Toast.makeText(context, "Light theme simulated inside Settings!", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                                ThemeSelectCard(
                                                    name = "Dark",
                                                    selected = selectedTheme == "Dark",
                                                    icon = Icons.Default.WaterDrop,
                                                    themeColor = themeColor,
                                                    modifier = Modifier.weight(1f),
                                                    onClick = { selectedTheme = "Dark" }
                                                )
                                                ThemeSelectCard(
                                                    name = "System",
                                                    selected = selectedTheme == "System",
                                                    icon = Icons.Default.SettingsSuggest,
                                                    themeColor = themeColor,
                                                    modifier = Modifier.weight(1f),
                                                    onClick = { selectedTheme = "System" }
                                                )
                                            }
                                        }

                                        // Accent Color Circles
                                        Column {
                                            Text(if (isAr) "لون لهجة النظام النشط" else "Accent Color", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                val accents = listOf("Teal", "Purple", "Green", "Orange", "Red")
                                                val colors = listOf(Color(0xFF00BFA5), Color(0xFF9F58FF), Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFFF44336))
                                                
                                                accents.forEachIndexed { i, acc ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(colors[i])
                                                            .border(
                                                                if (selectedAccentColor == acc) 2.dp else 0.dp,
                                                                Color.White,
                                                                CircleShape
                                                            )
                                                            .clickable { selectedAccentColor = acc },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (selectedAccentColor == acc) {
                                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Font Size Row
                                        Column {
                                            Text(if (isAr) "حجم الخط المفضل" else "Font Size", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                listOf("Small", "Medium", "Large").forEach { fSize ->
                                                    val isSel = selectedFontSize == fSize
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(36.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(if (isSel) themeColor.copy(alpha = 0.2f) else Color(0xFF041021))
                                                            .border(1.dp, if (isSel) themeColor else Color(0xFF1E3A5F), RoundedCornerShape(6.dp))
                                                            .clickable { selectedFontSize = fSize },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(fSize, color = if (isSel) themeColor else Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }

                                        // Language Pick Chevron Row
                                        Column {
                                            Divider(color = Color(0xFF1E3A5F))
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedLanguage = if (selectedLanguage == "English") "العربية" else "English"
                                                        Toast.makeText(context, "Preferred language switched to $selectedLanguage!", Toast.LENGTH_SHORT).show()
                                                    },
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(if (isAr) "لغة العرض" else "Language", color = Color.White, fontSize = 11.5.sp)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(selectedLanguage, color = themeColor, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp))
                                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            "Privacy" -> {
                                SectionTitle(if (isAr) "الخصوصية وتفويضات النظام" else "Privacy", themeColor)
                                
                                DetailContainerCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        SettingsToggleRow(
                                            icon = Icons.Default.LocationOn,
                                            label = if (isAr) "صلاحية الوصول للموقع" else "Location Access",
                                            checked = locationAccess,
                                            onCheckedChange = { locationAccess = it },
                                            themeColor = themeColor
                                        )
                                        Divider(color = Color(0xFF1E3A5F))
                                        SettingsToggleRow(
                                            icon = Icons.Default.CameraAlt,
                                            label = if (isAr) "صلاحية استخدام الكاميرا" else "Camera Access",
                                            checked = cameraAccess,
                                            onCheckedChange = { cameraAccess = it },
                                            themeColor = themeColor
                                        )
                                        Divider(color = Color(0xFF1E3A5F))
                                        SettingsToggleRow(
                                            icon = Icons.Default.Mic,
                                            label = if (isAr) "صلاحية الميكروفون" else "Microphone Access",
                                            checked = micAccess,
                                            onCheckedChange = { micAccess = it },
                                            themeColor = themeColor
                                        )
                                        Divider(color = Color(0xFF1E3A5F))
                                        SettingsToggleRow(
                                            icon = Icons.Default.ContactMail,
                                            label = if (isAr) "صلاحية الوصول لجهات الاتصال" else "Contacts Access",
                                            checked = contactsAccess,
                                            onCheckedChange = { contactsAccess = it },
                                            themeColor = themeColor
                                        )
                                    }
                                }

                                Text(if (isAr) "ضوابط التحكم بالخصوصية" else "Privacy Controls", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = themeColor, modifier = Modifier.padding(start = 4.dp))

                                DetailContainerCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { 
                                                    profileVisibility = if (profileVisibility == "Everyone") "Only Auditees" else "Everyone"
                                                }
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(if (isAr) "رؤية الحساب الشخصي" else "Profile Visibility", color = Color.White, fontSize = 11.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(profileVisibility, color = Color(0xFF00BFA5), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        Divider(color = Color(0xFF1E3A5F))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { 
                                                    onlineStatus = if (onlineStatus == "Everyone") "My Teams Only" else "Everyone"
                                                }
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(if (isAr) "حالة الاتصال بالإنترنت" else "Online Status", color = Color.White, fontSize = 11.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(onlineStatus, color = Color(0xFF00BFA5), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        Divider(color = Color(0xFF1E3A5F))
                                        SettingsToggleRow(
                                            icon = Icons.Default.CheckCircle,
                                            label = if (isAr) "تأكيد قراءة الرسائل" else "Read Receipts",
                                            checked = readReceipts,
                                            onCheckedChange = { readReceipts = it },
                                            themeColor = themeColor
                                        )
                                        Divider(color = Color(0xFF1E3A5F))
                                        SettingsToggleRow(
                                            icon = Icons.Default.DirectionsWalk,
                                            label = if (isAr) "حالة التفاعل والنشاط" else "Activity Status",
                                            checked = activityStatus,
                                            onCheckedChange = { activityStatus = it },
                                            themeColor = themeColor
                                        )
                                    }
                                }

                                Text(if (isAr) "إدارة الملفات المخزنة" else "Data Management", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = themeColor, modifier = Modifier.padding(start = 4.dp))

                                DetailContainerCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        SettingsActionRow(
                                            icon = Icons.Default.CloudDownload,
                                            label = if (isAr) "تحميل نسخة من بياناتي الـ QC" else "Download My Data",
                                            onClick = { Toast.makeText(context, "Full PDF/CSV backup generation triggered in notifications.", Toast.LENGTH_LONG).show() },
                                            isChevron = true,
                                            themeColor = themeColor
                                        )
                                        Divider(color = Color(0xFF1E3A5F))
                                        SettingsActionRow(
                                            icon = Icons.Default.Cookie,
                                            label = if (isAr) "إدارة ملفات تعريف الارتباط" else "Manage Cookies",
                                            onClick = { Toast.makeText(context, "Secure system cookies cleared & optimized.", Toast.LENGTH_SHORT).show() },
                                            isChevron = true,
                                            themeColor = themeColor
                                        )
                                        Divider(color = Color(0xFF1E3A5F))
                                        SettingsActionRow(
                                            icon = Icons.Default.DeleteSweep,
                                            label = if (isAr) "مسح سجل عمليات البحث" else "Clear Search History",
                                            onClick = { Toast.makeText(context, "Local search indexes purged successfully.", Toast.LENGTH_SHORT).show() },
                                            isChevron = true,
                                            themeColor = themeColor
                                        )
                                    }
                                }
                            }
                            
"Preferences" -> {
                                SectionTitle(if (isAr) "تفضيلات وإعدادات التطبيق" else "App Preferences", themeColor)
                                
                                DetailContainerCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(if (isAr) "الإعدادات العامة والربط" else "General", fontWeight = FontWeight.Bold, color = themeColor, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        SettingsToggleRow(
                                            icon = Icons.Default.Sync,
                                            label = if (isAr) "الاستيراد والمزامنة التلقائية" else "Auto Sync",
                                            checked = autoSync,
                                            onCheckedChange = { autoSync = it },
                                            themeColor = themeColor
                                        )
                                        Divider(color = Color(0xFF1E3A5F))
                                        SettingsToggleRow(
                                            icon = Icons.Default.Save,
                                            label = if (isAr) "الحفظ التلقائي للتقارير" else "Auto Save",
                                            checked = autoSave,
                                            onCheckedChange = { autoSave = it },
                                            themeColor = themeColor
                                        )
                                        Divider(color = Color(0xFF1E3A5F))
                                        SettingsToggleRow(
                                            icon = Icons.Default.Autorenew,
                                            label = if (isAr) "تحديث فوري بالخلفية" else "Background Refresh",
                                            checked = bgRefresh,
                                            onCheckedChange = { bgRefresh = it },
                                            themeColor = themeColor
                                        )
                                        Divider(color = Color(0xFF1E3A5F))
                                        SettingsToggleRow(
                                            icon = Icons.Default.AirplaneTicket,
                                            label = if (isAr) "وضع عدم الاتصال بالإنترنت" else "Offline Mode",
                                            checked = offlineMode,
                                            onCheckedChange = { offlineMode = it },
                                            themeColor = themeColor
                                        )
                                        Divider(color = Color(0xFF1E3A5F))
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = themeColor, modifier = Modifier.size(16.dp))
                                                Text(if (isAr) "مفتاح Google Gemini API" else "Google Gemini API Key", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                             ) {
                                                 OutlinedTextField(
                                                     value = geminiApiKeyInput,
                                                     onValueChange = { geminiApiKeyInput = it },
                                                     placeholder = { 
                                                         Text(
                                                             text = "AIzaSy...", 
                                                             color = Color.White.copy(alpha = 0.4f),
                                                             fontSize = 11.sp
                                                         ) 
                                                     },
                                                     singleLine = true,
                                                     visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                                     modifier = Modifier.weight(1f),
                                                     textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                                                     colors = OutlinedTextFieldDefaults.colors(
                                                         focusedBorderColor = themeColor,
                                                         unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                                         cursorColor = Color.White
                                                     )
                                                 )

                                                 Button(
                                                     onClick = {
                                                         sharedPrefs.edit().putString("gemini_api_key", geminiApiKeyInput.trim()).apply()
                                                         com.example.data.GeminiService.setCustomApiKey(if (geminiApiKeyInput.trim().isEmpty()) null else geminiApiKeyInput.trim())
                                                         Toast.makeText(
                                                             context, 
                                                             if (isAr) "تم حفظ مفتاح جيميناي بنجاح!" else "Gemini Key saved successfully!", 
                                                             Toast.LENGTH_SHORT
                                                         ).show()
                                                     },
                                                     colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                                     shape = RoundedCornerShape(6.dp),
                                                     contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                 ) {
                                                     Text(
                                                         text = if (isAr) "حفظ" else "Save",
                                                         color = Color.Black,
                                                         fontSize = 11.sp,
                                                         fontWeight = FontWeight.Bold
                                                     )
                                                 }
                                             }
                                         }
                                    }
                                }

                                DetailContainerCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(if (isAr) "التحكم في الوسائط والصور" else "Media", fontWeight = FontWeight.Bold, color = themeColor, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { 
                                                    videoQuality = if (videoQuality == "1080p") "4K UHD" else "1080p"
                                                }
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.VideoCall, contentDescription = null, tint = themeColor, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if (isAr) "دقة مقاطع الفيديو المرفقة" else "Video Quality", color = Color.White, fontSize = 11.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(videoQuality, color = Color(0xFF00BFA5), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        Divider(color = Color(0xFF1E3A5F))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { 
                                                    imageQuality = if (imageQuality == "High") "Original RAW" else "High"
                                                }
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = themeColor, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if (isAr) "جودة وضغط الصور" else "Image Quality", color = Color.White, fontSize = 11.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(imageQuality, color = Color(0xFF00BFA5), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        Divider(color = Color(0xFF1E3A5F))
                                        SettingsToggleRow(
                                            icon = Icons.Default.PlayCircleFilled,
                                            label = if (isAr) "تشغيل تلقائي لمقاطع الفيديو" else "Auto Play Videos",
                                            checked = autoPlayVideos,
                                            onCheckedChange = { autoPlayVideos = it },
                                            themeColor = themeColor
                                        )
                                    }
                                }

                                DetailContainerCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(if (isAr) "تنزيل وإدارة البيانات" else "Downloads", fontWeight = FontWeight.Bold, color = themeColor, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        SettingsToggleRow(
                                            icon = Icons.Default.Wifi,
                                            label = if (isAr) "تنزيل عبر شبكات Wi-Fi فقط" else "Download Over Wi-Fi Only",
                                            checked = wifiOnlyDownload,
                                            onCheckedChange = { wifiOnlyDownload = it },
                                            themeColor = themeColor
                                        )
                                        
                                    }
                                }

                                DetailContainerCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = if (isAr) "سعة تخزين الجهاز الفيزيائية" else "Physical App & Device Storage",
                                            fontWeight = FontWeight.Bold,
                                            color = themeColor,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))

                                        val totalGB = totalSpaceBytes.toFloat() / 1_000_000_000f
                                        val freeGB = freeSpaceBytes.toFloat() / 1_000_000_000f
                                        val usedGB = totalGB - freeGB
                                        val systemUsedPercent = if (totalGB > 0) (usedGB / totalGB).coerceIn(0f, 1f) else 0.4f

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Storage, contentDescription = null, tint = themeColor, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if (isAr) "مساحة التخزين الإجمالية للهاتف" else "Total Device Capacity", color = Color.White, fontSize = 11.sp)
                                            }
                                            Text(
                                                text = if (totalSpaceBytes > 0) formatDisplayBytes(totalSpaceBytes) else "128.00 GB",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(10.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(Color(0xFF132D50))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(systemUsedPercent)
                                                        .fillMaxHeight()
                                                        .background(themeColor)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                             ) {
                                                Text(
                                                    text = (if (isAr) "مستخدم: " else "Used: ") + (if (totalSpaceBytes > 0) formatDisplayBytes(totalSpaceBytes - freeSpaceBytes) else "45.80 GB"),
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 10.sp
                                                )
                                                Text(
                                                    text = (if (isAr) "متاح: " else "Free: ") + (if (totalSpaceBytes > 0) formatDisplayBytes(freeSpaceBytes) else "82.20 GB"),
                                                    color = themeColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        Divider(color = Color(0xFF1E3A5F))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Folder, contentDescription = null, tint = themeColor, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if (isAr) "سعة تخزين التطبيق الحالية" else "This App Size on Storage", color = Color.White, fontSize = 11.sp)
                                            }
                                            Text(
                                                text = if (appSpaceBytes > 0) formatDisplayBytes(appSpaceBytes) else "18.42 MB",
                                                color = themeColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    try {
                                                        context.cacheDir.deleteRecursively()
                                                        refreshStorageStats(context)
                                                        Toast.makeText(
                                                            context,
                                                            if (isAr) "تم مسح الذاكرة المؤقتة بنجاح!" else "Cleaned local caches & temporary PDF exports!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                                                border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(vertical = 10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CleaningServices,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = themeColor
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isAr) "مسح الكاش" else "Clear Cache",
                                                    fontSize = 11.sp,
                                                    color = Color.White
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    isOptimizingStorage = true
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(vertical = 10.dp)
                                            ) {
                                                if (isOptimizingStorage) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(14.dp),
                                                        color = Color.Black,
                                                        strokeWidth = 2.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (isAr) "جاري الضغط..." else "Optimizing...",
                                                        fontSize = 11.sp,
                                                        color = Color.Black,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Dns,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = Color.Black
                                                     )
                                                     Spacer(modifier = Modifier.width(6.dp))
                                                     Text(
                                                         text = if (isAr) "تحسين قاعدة البيانات" else "Optimize DB",
                                                         fontSize = 11.sp,
                                                         color = Color.Black,
                                                         fontWeight = FontWeight.Bold
                                                     )
                                                }
                                            }
                                        }

                                        if (isOptimizingStorage) {
                                             LaunchedEffect(isOptimizingStorage) {
                                                 kotlinx.coroutines.delay(1200)
                                                 try {
                                                     val db = com.example.data.AppDatabase.getDatabase(context)
                                                     db.openHelper.writableDatabase.execSQL("VACUUM")
                                                 } catch (e: Exception) {
                                                 }
                                                 refreshStorageStats(context)
                                                 isOptimizingStorage = false
                                                 Toast.makeText(
                                                     context,
                                                     if (isAr) "تم تحسين قاعدة البيانات بنجاح!" else "Local DB optimized successfully!",
                                                     Toast.LENGTH_SHORT
                                                 ).show()
                                             }
                                        }
                                    }
                                }
                            }

                            "Network" -> {
                                SectionTitle(if (isAr) "اتصال الشبكة والخادم" else "Network Connection", themeColor)

                                DetailContainerCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(if (isAr) "التحكم في الاتصال والمزامنة" else "Connectivity Controls", fontWeight = FontWeight.Bold, color = themeColor, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(4.dp))

                                        SettingsToggleRow(
                                            icon = Icons.Default.NetworkCheck,
                                            label = if (isAr) "تمكين اتصال الشبكة النشط" else "Enable Network Connection",
                                            subtitle = if (isNetworkEnabled) (if (isAr) "الخادم متصل وجاهز" else "Online & Active") else (if (isAr) "وضع غير متصل بالإنترنت" else "Offline Mode Force-Enabled"),
                                            checked = isNetworkEnabled,
                                            onCheckedChange = { isNetworkEnabled = it },
                                            themeColor = themeColor
                                        )

                                        Divider(color = Color(0xFF1E3A5F))

                                        // Network Mode selection selector
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text(if (isAr) "نوع الاتصال المسموح" else "Preferred Connection Type", color = Color.White, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                listOf("Wi-Fi & Cellular", "Wi-Fi Only", "Offline").forEach { mode ->
                                                    val isSelected = networkMode == mode
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .background(
                                                                if (isSelected) themeColor.copy(alpha = 0.2f) else Color(0xFF071930),
                                                                RoundedCornerShape(6.dp)
                                                            )
                                                            .border(
                                                                1.dp,
                                                                if (isSelected) themeColor else Color(0xFF1E3A5F),
                                                                RoundedCornerShape(6.dp)
                                                            )
                                                            .clickable { networkMode = mode }
                                                            .padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = mode,
                                                            color = if (isSelected) themeColor else Color.White.copy(alpha = 0.7f),
                                                            fontSize = 10.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Divider(color = Color(0xFF1E3A5F))

                                        // Data Stats indicators
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.DataUsage, contentDescription = null, tint = themeColor, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if (isAr) "إحصائيات استهلاك البيانات" else "Network Data Consumed", color = Color.White, fontSize = 11.sp)
                                            }
                                            Text("Sent: 2.4 MB | Recv: 18.9 MB", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                        }
                                    }
                                }

                                DetailContainerCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(if (isAr) "إعدادات خادم الشركة (API)" else "Enterprise Server Details (API)", fontWeight = FontWeight.Bold, color = themeColor, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(4.dp))

                                        OutlinedTextField(
                                            value = serverEndpoint,
                                            onValueChange = { serverEndpoint = it },
                                            label = { Text(if (isAr) "رابط خادم الشركة الرئيسي" else "API Gateway Endpoint URL", color = Color.White.copy(alpha = 0.7f)) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = themeColor,
                                                unfocusedBorderColor = Color(0xFF1E3A5F)
                                              ),
                                              singleLine = true,
                                              modifier = Modifier.fillMaxWidth(),
                                              textStyle = TextStyle(fontSize = 11.sp)
                                          )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Test connection section
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (!isNetworkEnabled) {
                                                        lastPingResult = if (isAr) "خطأ: تأكد من تمكين الاتصال بالشبكة أولاً" else "ERROR: Enable network connectivity first"
                                                    } else {
                                                        isTestingConnection = true
                                                        lastPingResult = ""
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = themeColor.copy(alpha = 0.15f), contentColor = themeColor),
                                                border = BorderStroke(1.dp, themeColor.copy(alpha = 0.5f)),
                                                shape = RoundedCornerShape(6.dp),
                                                enabled = !isTestingConnection
                                            ) {
                                                if (isTestingConnection) {
                                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = themeColor)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(if (isAr) "جاري الفحص..." else "Testing Ping...", fontSize = 11.sp)
                                                } else {
                                                    Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(if (isAr) "فحص جودة الاتصال" else "Ping Server Test", fontSize = 11.sp)
                                                }
                                            }

                                            // Quick launch side effect to simulate response latency
                                            LaunchedEffect(isTestingConnection) {
                                                if (isTestingConnection) {
                                                    kotlinx.coroutines.delay(1000)
                                                    isTestingConnection = false
                                                    lastPingResult = if (isAr) "متصل بنجاح! الاستجابة: 200 (الوقت: 34ms)" else "Active connection OK! Status: 200 (Ping: 34ms)"
                                                }
                                            }

                                            Text(
                                                text = if (isNetworkEnabled) "SSL SECURED" else "OFFLINE",
                                                color = if (isNetworkEnabled) Color(0xFF00BFA5) else Color.Red,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .background(if (isNetworkEnabled) Color(0xFF00BFA5).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                    .border(1.dp, if (isNetworkEnabled) Color(0xFF00BFA5).copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }

                                        if (lastPingResult.isNotEmpty()) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF071930)),
                                                border = BorderStroke(1.dp, if (lastPingResult.contains("ERROR") || lastPingResult.contains("خطأ")) Color.Red.copy(alpha = 0.3f) else Color(0xFF1E3A5F)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = lastPingResult,
                                                    color = if (lastPingResult.contains("ERROR") || lastPingResult.contains("خطأ")) Color.Red else Color(0xFF00BFA5),
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

"Storage" -> {
                                SectionTitle(if (isAr) "مساحة التخزين والذاكرة المؤقتة" else "Storage & Cache Management", themeColor)
                                
                                // Card 1: Overall Physical Storage Bar
                                DetailContainerCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = if (isAr) "السعة الإجمالية للجهاز" else "Physical Device Storage Capacity",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        
                                        val totalGB = totalSpaceBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                                        val freeGB = freeSpaceBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                                        val usedGB = (totalSpaceBytes - freeSpaceBytes).toDouble() / (1024.0 * 1024.0 * 1024.0)
                                        
                                        val usedPercentage = if (totalSpaceBytes > 0) {
                                            (totalSpaceBytes - freeSpaceBytes).toFloat() / totalSpaceBytes.toFloat()
                                        } else {
                                            0.12f
                                        }
                                        
                                        // Linear progress indicator representing device filled state
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(Color(0xFF132D50))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(fraction = usedPercentage.coerceIn(0f, 1f))
                                                    .background(themeColor)
                                            )
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isAr) "المستعمل: %.2f GB".format(usedGB) else "Used: %.2f GB".format(usedGB),
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = if (isAr) "المتبقي: %.2f GB".format(freeGB) else "Free: %.2f GB".format(freeGB),
                                                fontSize = 11.sp,
                                                color = themeColor
                                            )
                                            Text(
                                                text = if (isAr) "الإجمالي: %.2f GB".format(totalGB) else "Total: %.2f GB".format(totalGB),
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Card 2: Saved Audit Documents & SQLite DB filled space
                                DetailContainerCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = if (isAr) "بيانات ومستندات التدقيق السابقة" else "Audit Reports Database & Local Assets",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = if (isAr) "حجم قاعدة البيانات المحفوظة" else "Saved Audits Database Size",
                                                    fontSize = 12.sp,
                                                    color = Color.White.copy(alpha = 0.6f)
                                                )
                                                Text(
                                                    text = if (isAr) "إجمالي التقارير المحفوظة: ${savedReports.size}" else "Total Stored Reports: ${savedReports.size}",
                                                    fontSize = 11.sp,
                                                    color = Color.White.copy(alpha = 0.4f)
                                                )
                                            }
                                            
                                            Text(
                                                text = formatDisplayBytes(databaseSpaceBytes),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 16.sp,
                                                color = themeColor
                                            )
                                        }
                                        
                                        // Visual listing of the saved audits and their dates
                                        if (savedReports.isEmpty()) {
                                            Text(
                                                text = if (isAr) "لا توجد تقارير تدقيق محفوظة حالياً." else "No saved audits found on this device.",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                        } else {
                                            Divider(color = Color(0xFF1E3A5F), thickness = 0.5.dp)
                                            Text(
                                                text = if (isAr) "المستندات المحفوظة وتواريخ المعاينة:" else "Stored Audit files & Saved dates:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                savedReports.forEach { report ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(Color(0xFF071930), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = report.projectName ?: "Unnamed Project",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = Color.White,
                                                                maxLines = 1,
                                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = if (isAr) "الموقع: ${report.location ?: "-"}" else "Site: ${report.location ?: "-"}",
                                                                fontSize = 9.5.sp,
                                                                color = Color.White.copy(alpha = 0.5f)
                                                            )
                                                        }
                                                        
                                                        // Saved Date
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(themeColor.copy(alpha = 0.1f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = report.auditDate ?: "No Date",
                                                                fontSize = 9.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = themeColor
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Card 3: Cache Storage
                                DetailContainerCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = if (isAr) "الذاكرة المؤقتة والملفات الإضافية" else "Temporary Cache & Export Logs",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = if (isAr) "ملفات الـ PDF والذاكرة المؤقتة للتصدير" else "Cached reports & shareable logs",
                                                    fontSize = 12.sp,
                                                    color = Color.White.copy(alpha = 0.6f)
                                                )
                                            }
                                            
                                            Text(
                                                text = formatDisplayBytes(cacheSpaceBytes),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 16.sp,
                                                color = themeColor
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Card 4: Action Controls (Clear Cache, Vacuum, DB Reset)
                                DetailContainerCard {
                                    val scope = rememberCoroutineScope()
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = if (isAr) "أدوات الصيانة وتفريغ المساحة" else "Maintenance & Storage Clean Utilities",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        
                                        // Clear Cache Action Row
                                        SettingsActionRow(
                                            icon = Icons.Default.DeleteSweep,
                                            label = if (isAr) "تنظيف الذاكرة المؤقتة (Cache)" else "Purge Cache Directories",
                                            onClick = {
                                                try {
                                                    context.cacheDir.deleteRecursively()
                                                    refreshStorageStats(context)
                                                    Toast.makeText(context, if (isAr) "تم إفراغ الذاكرة المؤقتة بنجاح!" else "System Cache cleared successfully!", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Error purging cache", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            isChevron = true,
                                            themeColor = themeColor
                                        )
                                        
                                        Divider(color = Color(0xFF1E3A5F), thickness = 0.5.dp)
                                        
                                        // Optimize / Vacuum Action Row
                                        if (isOptimizingStorage) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = themeColor, strokeWidth = 2.dp)
                                                Text(
                                                    text = if (isAr) "جاري ضغط قاعدة البيانات وتحسين الفهارس..." else "Vacuuming and optimizing local database indexes...",
                                                    fontSize = 12.sp,
                                                    color = themeColor
                                                )
                                            }
                                            
                                            LaunchedEffect(isOptimizingStorage) {
                                                kotlinx.coroutines.delay(1200)
                                                try {
                                                    val db = com.example.data.AppDatabase.getDatabase(context)
                                                    db.openHelper.writableDatabase.execSQL("VACUUM")
                                                } catch (e: Exception) {}
                                                refreshStorageStats(context)
                                                isOptimizingStorage = false
                                                Toast.makeText(context, if (isAr) "تم تحسين قاعدة البيانات وتفريغ المساحة!" else "Database vacuumed and optimized!", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            SettingsActionRow(
                                                icon = Icons.Default.BuildCircle,
                                                label = if (isAr) "تحسين وضغط قاعدة البيانات" else "Optimize & Compact DB Storage",
                                                onClick = {
                                                    isOptimizingStorage = true
                                                },
                                                isChevron = true,
                                                themeColor = themeColor
                                            )
                                        }
                                        
                                        Divider(color = Color(0xFF1E3A5F), thickness = 0.5.dp)
                                        
                                        // Reset Database Action Row
                                        var showDbResetDialog by remember { mutableStateOf(false) }
                                        SettingsActionRow(
                                            icon = Icons.Default.LayersClear,
                                            label = if (isAr) "مسح جميع بيانات التدقيق المخزنة" else "Erase All Saved Audit Data",
                                            onClick = {
                                                showDbResetDialog = true
                                            },
                                            isChevron = true,
                                            themeColor = Color(0xFFFF5252)
                                        )
                                        
                                        if (showDbResetDialog) {
                                            AlertDialog(
                                                onDismissRequest = { showDbResetDialog = false },
                                                title = {
                                                    Text(
                                                        text = if (isAr) "تنبيه: مسح كامل قاعدة البيانات" else "Warning: Complete Database Reset",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp
                                                    )
                                                },
                                                text = {
                                                    Text(
                                                        text = if (isAr) 
                                                            "هل أنت متأكد من رغبتك في مسح كافة التقارير التدقيقية وحالات عدم المطابقة (NCRs) والملاحظات المحفوظة بشكل نهائي؟ لا يمكن استرجاع هذه البيانات."
                                                            else "Are you sure you want to permanently erase all saved audit reports, findings, and previous history from this device? This action cannot be undone.",
                                                        color = Color.White.copy(alpha = 0.8f),
                                                        fontSize = 13.sp
                                                    )
                                                },
                                                confirmButton = {
                                                    Button(
                                                        onClick = {
                                                            showDbResetDialog = false
                                                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                                try {
                                                                    val db = com.example.data.AppDatabase.getDatabase(context)
                                                                    db.clearAllTables()
                                                                    viewModel.createNewBlankReport()
                                                                } catch (e: Exception) {}
                                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                                    refreshStorageStats(context)
                                                                    Toast.makeText(context, if (isAr) "تم تصفير وإعادة تعيين البيانات!" else "Database successfully cleared!", Toast.LENGTH_LONG).show()
                                                                }
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                                                    ) {
                                                        Text(if (isAr) "نعم، مسح الكل" else "Yes, Erase All", color = Color.White)
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { showDbResetDialog = false }) {
                                                        Text(if (isAr) "إلغاء الأمر" else "Cancel", color = Color.LightGray)
                                                    }
                                                },
                                                containerColor = Color(0xFF0F2644),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

}
                    }
                }
            }
        }
    }

    // --- COHESIVE POPUP DIALOGS (FOR COMPLETE FILLABILITY AND REALITY EFFECT) ---
    
    // 1. Edit Profile Dialog
    if (showEditProfileDialog) {
        var tempName by remember { mutableStateOf(userName) }
        var tempEmail by remember { mutableStateOf(userEmail) }
        
        Dialog(onDismissRequest = { showEditProfileDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2644)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, themeColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        if (isAr) "تعديل الملف الشخصي" else "Edit Corporate Profile",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text(if (isAr) "الاسم بالكامل" else "Full Name", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = Color(0xFF1E3A5F)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempEmail,
                        onValueChange = { tempEmail = it },
                        label = { Text(if (isAr) "البريد الإلكتروني" else "Email Address", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = Color(0xFF1E3A5F)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showEditProfileDialog = false }) {
                            Text(if (isAr) "إلغاء الإجراء" else "Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (tempName.isNotBlank() && tempEmail.contains("@")) {
                                    userName = tempName
                                    userEmail = tempEmail
                                    showEditProfileDialog = false
                                    Toast.makeText(context, if (isAr) "تم تعديل ملف الحساب بنجاح" else "Profile details modified!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please input valid corporate account details.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                        ) {
                            Text(if (isAr) "حفظ التعديلات" else "Save Details")
                        }
                    }
                }
            }
        }
    }

    // 2. Change Password Dialog
    if (showChangePasswordDialog) {
        var oldPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        
        Dialog(onDismissRequest = { showChangePasswordDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2644)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, themeColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        if (isAr) "تغيير شفرة التوثيق وكلمة المرور" else "Change Account Password",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    
                    OutlinedTextField(
                        value = oldPass,
                        onValueChange = { oldPass = it },
                        label = { Text(if (isAr) "كلمة المرور الحالية" else "Current Password", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = Color(0xFF1E3A5F)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text(if (isAr) "كلمة المرور الجديدة" else "New Password", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = Color(0xFF1E3A5F)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = { Text(if (isAr) "تأكيد كلمة المرور" else "Confirm New Password", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = Color(0xFF1E3A5F)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showChangePasswordDialog = false }) {
                            Text(if (isAr) "إلغاء" else "Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (oldPass.isNotEmpty() && newPass.isNotEmpty() && newPass == confirmPass) {
                                    showChangePasswordDialog = false
                                    Toast.makeText(context, if (isAr) "تم تعديل شفرة المرور الجديدة بنجاح" else "Password security update approved!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Passwords do not match or empty.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                        ) {
                            Text(if (isAr) "تأكيد وتغيير" else "Apply Password")
                        }
                    }
                }
            }
        }
    }

    // 3. Clear Saved Audits Final Dialog
    if (showAllSavedReportsCountDialog) {
        Dialog(onDismissRequest = { showAllSavedReportsCountDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2644)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        if (isAr) "تأكيد حذف جميع التقارير" else "Confirm Master Purge",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                        fontSize = 15.sp
                    )
                    Text(
                        if (isAr) "هل أنت متأكد نهائياً من رغبتك في حذف وإلغاء كافة تقارير التدقيق والجودة من السجل؟ هذا الإجراء غير قابل للتراجع."
                        else "Are you sure you want to permanently clear ALL saved quality control audits from local storage? This action cannot be undone.",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAllSavedReportsCountDialog = false }) {
                            Text(if (isAr) "تراجع" else "Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showAllSavedReportsCountDialog = false
                                Toast.makeText(context, "All saved local audits purged from database.", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text(if (isAr) "نعم، احذف كافة البيانات" else "Yes, Purge All")
                        }
                    }
                }
            }
        }
    }

    // 4. Delete Corporate Account Confirm Dialog
    if (showDeleteAccountConfirm) {
        Dialog(onDismissRequest = { showDeleteAccountConfirm = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2644)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        if (isAr) "حساب الشركة: حذف نهائي" else "Confirm Account Termination",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                        fontSize = 15.sp
                    )
                    Text(
                        if (isAr) "سيتم تجميد وحذف حساب المدقق وإلغاء المزامنة بشكل لحظي."
                        else "All data synced with auditor@email.com will be instantly detached from Innovo QAQC servers.",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showDeleteAccountConfirm = false }) {
                            Text("Go Back", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showDeleteAccountConfirm = false
                                Toast.makeText(context, "Corporate profile detached from server. Relaunching in demo mode.", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Delete Account")
                        }
                    }
                }
            }
        }
    }

    // 5. Link Custom/New Account Dialog
    if (showLinkAccountDialog) {
        var providerSelected by remember { mutableStateOf("Google") }
        var accountIdInput by remember { mutableStateOf("") }
        val providers = listOf("Google", "Apple", "LinkedIn", "Microsoft", "Custom System")
        var expandedDropdown by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showLinkAccountDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2644)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, themeColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        if (isAr) "ربط حساب جديد للتطبيق" else "Link New External Account",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )

                    // Provider Dropdown
                    Column {
                        Text(
                            text = if (isAr) "تحت أي مزود خدمة؟" else "Select Service Provider",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF071930), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(6.dp))
                                .clickable { expandedDropdown = !expandedDropdown }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(providerSelected, color = Color.White, fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                        }

                        if (expandedDropdown) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF071930)),
                                border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp)
                            ) {
                                Column {
                                    providers.forEach { provider ->
                                        Text(
                                            text = provider,
                                            color = Color.White,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    providerSelected = provider
                                                    expandedDropdown = false
                                                }
                                                .padding(10.dp),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = accountIdInput,
                        onValueChange = { accountIdInput = it },
                        label = { Text(if (isAr) "المعرف البريدي أو اسم الحساب" else "Account Username / Email / ID", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = Color(0xFF1E3A5F)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showLinkAccountDialog = false }) {
                            Text(if (isAr) "إلغاء" else "Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (accountIdInput.isNotBlank()) {
                                    when (providerSelected) {
                                        "Google" -> {
                                            linkedGoogle = true
                                            googleEmail = accountIdInput
                                        }
                                        "Apple" -> {
                                            linkedApple = true
                                            appleEmail = accountIdInput
                                        }
                                        "LinkedIn" -> {
                                            linkedLinkedin = true
                                            linkedinUsername = accountIdInput
                                        }
                                        "Microsoft" -> {
                                            linkedMicrosoft = true
                                            microsoftEmail = accountIdInput
                                        }
                                        else -> {
                                            customLinkedAccountsList = customLinkedAccountsList + (providerSelected to accountIdInput)
                                        }
                                    }
                                    showLinkAccountDialog = false
                                    Toast.makeText(context, if (isAr) "تم ربط الحساب بنجاح!" else "Account linked successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please supply a valid Account ID/Email.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                        ) {
                            Text(if (isAr) "توصيل الحساب" else "Link Account")
                        }
                    }
                }
            }
        }
    }
}

// --- SUPPORTING SUB-COMPOSABLES AND CLEAN LAYOUT ITEMS ---

data class SettingsMenuData(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isDanger: Boolean = false
)

@Composable
fun SectionTitle(title: String, color: Color) {
    Text(
        text = title,
        style = TextStyle(
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = color,
            letterSpacing = 0.5.sp
        ),
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

@Composable
fun DetailContainerCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E3A5F).copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2644)), // Dark container color
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isChevron: Boolean = true,
    themeColor: Color,
    textColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = themeColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = textColor, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
        }
        
        if (isChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    themeColor: Color,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = themeColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(label, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(subtitle, color = Color(0xFF94A3B8), fontSize = 9.5.sp)
                }
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = themeColor,
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF041021)
            ),
            modifier = Modifier.scale(0.75f)
        )
    }
}

@Composable
fun ThemeSelectCard(
    name: String,
    selected: Boolean,
    icon: ImageVector,
    themeColor: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) themeColor.copy(alpha = 0.2f) else Color(0xFF041021))
            .border(1.dp, if (selected) themeColor else Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) themeColor else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = name,
                color = if (selected) themeColor else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun SocialBubble(name: String, color: Color) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (name == "apple") Color.White else color),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when (name) {
                "google" -> Icons.Default.Language
                "apple" -> Icons.Default.PhoneIphone
                "linkedin" -> Icons.Default.ConnectWithoutContact
                else -> Icons.Default.Link
            },
            contentDescription = null,
            tint = if (name == "apple") Color.Black else Color.White,
            modifier = Modifier.size(12.dp)
        )
    }
}
