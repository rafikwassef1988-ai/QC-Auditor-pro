package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import coil.compose.AsyncImage
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.ui.graphics.asImageBitmap
import android.content.Context
import java.io.FileOutputStream
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.example.data.AuditReport
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// ==========================================
// DATA MODELS & ENUMS
// ==========================================

data class CollageImage(
    val id: Int,
    val titleEn: String,
    val titleAr: String,
    val color: Color,
    val startColor: Color,
    val endColor: Color,
    val categoryEn: String,
    val categoryAr: String,
    val emoji: String,
    val isPortrait: Boolean = true,
    val initialHorizonAngle: Float = 0f, // degrees tilted
    var currentHorizonAngle: Float = 0f,
    val keyFaces: List<Offset> = emptyList(), // fractional positions
    val initialExposureOffset: Float = 0f, // for color matching
    var currentExposureOffset: Float = 0f,
    val qualityScore: Float = 0.85f, // for best photo selection
    var isDuplicate: Boolean = false,
    val url: String? = null
)

enum class AspectRatioType(val ratio: Float, val labelEn: String, val labelAr: String) {
    SQUARE(1f, "1:1 Square", "١:١ مربع"),
    STANDARD(4f / 3f, "4:3 Classic", "٤:٣ كلاسيكي"),
    WIDESCREEN(16f / 9f, "16:9 Cinema", "٩:١٦ سينمائي"),
    PORTRAIT(9f / 16f, "9:16 Portrait", "١٦:٩ طولي")
}

enum class BackgroundStyle {
    SOLID, GRADIENT, TRANSPARENT_GRID
}

enum class QualityLevel(val label: String, val resolution: String, val scale: Float) {
    SD("720P SD", "1280 x 720", 0.75f),
    HD("1080P Full HD", "1920 x 1080", 1.0f),
    QHD("2K Quad HD", "2560 x 1440", 1.5f),
    UHD("4K Ultra HD", "3840 x 2160", 2.0f)
}

data class CollageProject(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val imageIds: List<Int>,
    val layoutId: Int,
    val aspectRatio: AspectRatioType,
    val spacingPx: Float,
    val cornerRadiusPx: Float,
    val backgroundStyle: BackgroundStyle,
    val solidColor: Color,
    val borderWidthPx: Float,
    val borderColor: Color,
    val hasShadow: Boolean,
    val blurRadiusPx: Float,
    val smartCropEnabled: Boolean,
    val faceProtectionEnabled: Boolean,
    val horizonAlignmentEnabled: Boolean,
    val colorMatchingEnabled: Boolean,
    val duplicateDetectionEnabled: Boolean,
    val bestPhotoSelectionEnabled: Boolean,
    val photosPerRow: Int
)

// Undo/Redo historical frame
data class HistoryFrame(
    val imageIds: List<Int>,
    val layoutId: Int,
    val spacing: Float,
    val cornerRadius: Float,
    val photosPerRow: Int,
    val smartCrop: Boolean,
    val faceProtection: Boolean,
    val horizonAlignment: Boolean,
    val colorMatching: Boolean,
    val hasShadow: Boolean
)

// ==========================================
// PREINSTALLED BEAUTIFUL SYSTEM GALLERY
// ==========================================

val SAMPLE_POOL = listOf(
    CollageImage(1, "Snowy Peak Lake", "البحيرة الجبلية", Color(0xFF0D47A1), Color(0xFF0D47A1), Color(0xFF1976D2), "Nature", "طبيعة", "🏔️", true, 6f, 6f, listOf(Offset(0.5f, 0.4f)), -0.15f, -0.15f, 0.94f, false, "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=600&auto=format&fit=crop&q=80"),
    CollageImage(2, "Mountain Hiker", "متسلق الجبل", Color(0xFF006064), Color(0xFF006064), Color(0xFF00838F), "Adventure", "مغامرة", "🧗", true, -4f, -4f, emptyList(), 0.1f, 0.1f, 0.81f, false, "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600&auto=format&fit=crop&q=80"),
    CollageImage(3, "Golden Reflection Lake", "البحيرة الذهبية", Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFF4CAF50), "Nature", "طبيعة", "☀️", false, 0f, 0f, emptyList(), -0.05f, -0.05f, 0.95f, false, "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=600&auto=format&fit=crop&q=80"),
    CollageImage(4, "Cozy Street", "قرية دافئة", Color(0xFF37474F), Color(0xFF263238), Color(0xFF546E7A), "City", "مدينة", "🧱", true, 8f, 8f, listOf(Offset(0.4f, 0.3f), Offset(0.6f, 0.3f)), 0.25f, 0.25f, 0.72f, false, "https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?w=600&auto=format&fit=crop&q=80"),
    CollageImage(5, "Autumn Forest Road", "طريق خريفي الغابة", Color(0xFFD84315), Color(0xFFBF360C), Color(0xFFFF5722), "Nature", "طبيعة", "🍁", false, -3f, -3f, listOf(Offset(0.3f, 0.5f)), -0.08f, -0.08f, 0.89f, false, "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=600&auto=format&fit=crop&q=80"),
    CollageImage(6, "Purple Twilight Skyline", "أفق الشفق سماء", Color(0xFF4A148C), Color(0xFF4A148C), Color(0xFF7B1FA2), "City", "مدينة", "🛫", false, 0f, 0f, listOf(Offset(0.5f, 0.5f)), 0.05f, 0.05f, 0.88f, false, "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=600&auto=format&fit=crop&q=80"),
    CollageImage(7, "Cappadocia Balloons", "المنطاد الطائر", Color(0xFF4E342E), Color(0xFF3E2723), Color(0xFF5D4037), "Adventure", "مغامرة", "🌉", true, 12f, 12f, emptyList(), -0.2f, -0.2f, 0.92f, false, "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&auto=format&fit=crop&q=80"),
    CollageImage(8, "Tropical Wooden Path", "المالديف الزرقاء", Color(0xFF1565C0), Color(0xFF0D47A1), Color(0xFF2196F3), "Travel", "سفر", "🚢", false, 0f, 0f, listOf(Offset(0.2f, 0.4f), Offset(0.8f, 0.4f)), 0.12f, 0.12f, 0.78f, false, "https://images.unsplash.com/photo-1433832597026-63a5d08d9663?w=600&auto=format&fit=crop&q=80"),
    CollageImage(9, "Lush Forest Waterfall", "شلال الغابة الخضراء", Color(0xFF2E7D32), Color(0xFF2E7D32), Color(0xFF81C784), "Nature", "طبيعة", "🌳", true, -2f, -2f, emptyList(), -0.12f, -0.12f, 0.91f, false, "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=600&auto=format&fit=crop&q=80")
)

// ==========================================
// CORE LAYOUT SCHEMES PRESETS (15 TEMPLATES)
// ==========================================

// A slot represents fractional bounding box coordinates: (left, top, right, bottom)
data class LayoutScheme(
    val id: Int,
    val nameEn: String,
    val nameAr: String,
    val slots: List<Rect>
)

val COLLAGE_SCHEMES = listOf(
    LayoutScheme(1, "Single Space", "مساحة فردية", listOf(Rect(0f, 0f, 1f, 1f))),
    LayoutScheme(2, "Vertical Split", "انقسام عمودي", listOf(Rect(0f, 0f, 0.5f, 1f), Rect(0.5f, 0f, 1f, 1f))),
    LayoutScheme(3, "Horizontal Split", "انقسام أفقي", listOf(Rect(0f, 0f, 1f, 0.5f), Rect(0f, 0.5f, 1f, 1f))),
    LayoutScheme(4, "Quad Grid", "شبكة رباعية", listOf(
        Rect(0f, 0f, 0.5f, 0.5f), Rect(0.5f, 0f, 1f, 0.5f),
        Rect(0f, 0.5f, 0.5f, 1f), Rect(0.5f, 0.5f, 1f, 1f)
    )),
    LayoutScheme(5, "Triptych Left", "ثلاثي يساري", listOf(
        Rect(0f, 0f, 0.5f, 1f), Rect(0.5f, 0f, 1f, 0.5f), Rect(0.5f, 0.5f, 1f, 1f)
    )),
    LayoutScheme(6, "Triptych Right", "ثلاثي يميني", listOf(
        Rect(0f, 0f, 0.5f, 0.5f), Rect(0f, 0.5f, 0.5f, 1f), Rect(0.5f, 0f, 1f, 1f)
    )),
    LayoutScheme(7, "Triptych Top", "ثلاثي علوي", listOf(
        Rect(0f, 0f, 1f, 0.5f), Rect(0f, 0.5f, 0.5f, 1f), Rect(0.5f, 0.5f, 1f, 1f)
    )),
    LayoutScheme(8, "Triptych Bottom", "ثلاثي سفلي", listOf(
        Rect(0f, 0f, 0.5f, 0.5f), Rect(0.5f, 0f, 1f, 0.5f), Rect(0f, 0.5f, 1f, 1f)
    )),
    LayoutScheme(9, "3 Columns", "٣ أعمدة طولي", listOf(
        Rect(0f, 0f, 0.333f, 1f), Rect(0.333f, 0f, 0.666f, 1f), Rect(0.666f, 0f, 1f, 1f)
    )),
    LayoutScheme(10, "3 Rows", "٣ صفوف عرضي", listOf(
        Rect(0f, 0f, 1f, 0.333f), Rect(0f, 0.333f, 1f, 0.666f), Rect(0f, 0.666f, 1f, 1f)
    )),
    LayoutScheme(11, "Cinematic Banner", "بانوراما سينمائية", listOf(
        Rect(0f, 0f, 1f, 0.6f), Rect(0f, 0.6f, 0.5f, 1f), Rect(0.5f, 0.6f, 1f, 1f)
    )),
    LayoutScheme(12, "Mosaic Grid", "شبكة فسيفساء هجينة", listOf(
        Rect(0f, 0f, 0.4f, 0.6f), Rect(0f, 0.6f, 0.4f, 1f),
        Rect(0.4f, 0f, 1f, 0.4f), Rect(0.4f, 0.4f, 0.7f, 1f), Rect(0.7f, 0.4f, 1f, 1f)
    )),
    LayoutScheme(13, "Symmetrical Pinwheel", "دولاب الهواء", listOf(
        Rect(0f, 0f, 0.35f, 0.35f), Rect(0.35f, 0f, 1f, 0.35f),
        Rect(0f, 0.35f, 0.65f, 1f), Rect(0.65f, 0.35f, 1f, 1f)
    )),
    LayoutScheme(14, "Hero Centerpiece", "لقطة البطل المركزية", listOf(
        Rect(0f, 0f, 0.3f, 0.3f), Rect(0.3f, 0f, 0.7f, 0.3f), Rect(0.7f, 0f, 1f, 0.3f),
        Rect(0f, 0.3f, 0.3f, 1f), Rect(0.3f, 0.3f, 0.7f, 1f), Rect(0.7f, 0.3f, 1f, 1f)
    )),
    LayoutScheme(15, "Multi Cell Grid", "شبكة خلايا مكثفة", listOf(
        Rect(0f, 0f, 0.333f, 0.333f), Rect(0.333f, 0f, 0.666f, 0.333f), Rect(0.666f, 0f, 1f, 0.333f),
        Rect(0f, 0.333f, 0.333f, 0.666f), Rect(0.333f, 0.333f, 0.666f, 0.666f), Rect(0.666f, 0.333f, 1f, 0.666f),
        Rect(0f, 0.666f, 0.333f, 1f), Rect(0.333f, 0.666f, 0.666f, 1f), Rect(0.666f, 0.666f, 1f, 1f)
    ))
)

// ==========================================
// PHOTO COLLAGE WORKSPACE TABLE COMPOSABLE
// ==========================================

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CollageCreatorTab(
    lang: String
) {
    val isAr = lang == "ar"
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- Dynamic User Session States ---
    val context = LocalContext.current
    var selectedImages by remember { mutableStateOf(emptyList<CollageImage>()) }
    val loadedImagesMap = remember { mutableStateMapOf<Int, Bitmap>() }

    var currentLayout by remember { mutableStateOf(COLLAGE_SCHEMES[14]) } // default #15 Multi Cell Grid
    var currentAspectRatio by remember { mutableStateOf(AspectRatioType.SQUARE) }
    var spacing by remember { mutableStateOf(12f) }
    var cornerRadius by remember { mutableStateOf(0f) }
    var photosPerRow by remember { mutableStateOf(3) }
    var useDynamicSpacingGrid by remember { mutableStateOf(false) } // true: dynamic rows, false: scheme template

    // Custom stylings
    var backgroundStyle by remember { mutableStateOf(BackgroundStyle.SOLID) }
    var solidBackgroundColor by remember { mutableStateOf(Color.White) }
    var borderWidth by remember { mutableStateOf(0f) }
    var borderColor by remember { mutableStateOf(Color(0xFF00BFA5)) }
    var hasShadow by remember { mutableStateOf(false) }
    var backgroundBlur by remember { mutableStateOf(0f) }

    // AI smart engines state flags
    var smartCropEnabled by remember { mutableStateOf(true) }
    var faceProtectionEnabled by remember { mutableStateOf(true) }
    var horizonAlignmentEnabled by remember { mutableStateOf(true) }
    var colorMatchingEnabled by remember { mutableStateOf(false) }
    var duplicateDetectionEnabled by remember { mutableStateOf(true) }
    var bestPhotoSelectionEnabled by remember { mutableStateOf(false) }

    // Undo / Redo registers
    val undoStack = remember { mutableStateListOf<HistoryFrame>() }
    val redoStack = remember { mutableStateListOf<HistoryFrame>() }

    // Interactive zooms and pans
    var zoomFactor by remember { mutableStateOf(1.0f) }
    var globalPanOffset by remember { mutableStateOf(Offset.Zero) }

    // --- System Image Pickers (Gallery/Memory) ---
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                uris.forEach { uri ->
                    val bitmap = loadBitmapFromUri(context, uri)
                    if (bitmap != null) {
                        val newId = (System.currentTimeMillis() + (0..1000).random()).toInt()
                        val newImg = CollageImage(
                            id = newId,
                            titleEn = "Gallery Image",
                            titleAr = "صورة مضافة",
                            color = Color.DarkGray,
                            startColor = Color.DarkGray,
                            endColor = Color.Black,
                            categoryEn = "Gallery",
                            categoryAr = "معرض الصور",
                            emoji = "🖼️",
                            isPortrait = bitmap.height > bitmap.width,
                            url = uri.toString()
                        )
                        withContext(Dispatchers.Main) {
                            loadedImagesMap[newId] = bitmap
                            selectedImages = selectedImages + newImg
                        }
                    }
                }
            }
        }
    }

    // --- System Camera (Capture) ---
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                scope.launch(Dispatchers.IO) {
                    val bitmap = loadBitmapFromUri(context, uri)
                    if (bitmap != null) {
                        val newId = (System.currentTimeMillis() + (0..1000).random()).toInt()
                        val newImg = CollageImage(
                            id = newId,
                            titleEn = "Camera Photo",
                            titleAr = "صورة الكاميرا",
                            color = Color.DarkGray,
                            startColor = Color.DarkGray,
                            endColor = Color.Black,
                            categoryEn = "Camera",
                            categoryAr = "كاميرا",
                            emoji = "📷",
                            isPortrait = bitmap.height > bitmap.width,
                            url = uri.toString()
                        )
                        withContext(Dispatchers.Main) {
                            loadedImagesMap[newId] = bitmap
                            selectedImages = selectedImages + newImg
                        }
                    }
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            tempCameraUri?.let { uri ->
                try {
                    cameraLauncher.launch(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            android.widget.Toast.makeText(context, if (isAr) "إذن الكاميرا مطلوب لالتقاط الصور" else "Camera permission is required to capture photos.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // --- Document Saver (Create Collage Jpeg) ---
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val bitmapWidth = 1080
                    val bitmapHeight = if (currentAspectRatio.ratio > 0) (bitmapWidth / currentAspectRatio.ratio).toInt() else 1080
                    val renderBitmap = renderCollageToBitmap(
                        context = context,
                        width = bitmapWidth,
                        height = bitmapHeight,
                        selectedImages = selectedImages,
                        currentLayout = currentLayout,
                        useDynamicSpacingGrid = useDynamicSpacingGrid,
                        photosPerRow = photosPerRow,
                        spacing = spacing,
                        cornerRadius = cornerRadius,
                        backgroundStyle = backgroundStyle,
                        solidBackgroundColor = solidBackgroundColor,
                        loadedImagesMap = loadedImagesMap,
                        smartCropEnabled = smartCropEnabled,
                        zoomFactor = zoomFactor
                    )
                    
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        renderBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                    }
                    
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar(
                            if (isAr) "تم حفظ الصورة بنجاح!" else "Collage successfully saved to internal memory!"
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar(
                            if (isAr) "عذرًا، حدث خطأ أثناء الحفظ" else "Error writing collage to storage."
                        )
                    }
                }
            }
        }
    }

    var activeCompareMode by remember { mutableStateOf(false) }
    var fullScreenPreview by remember { mutableStateOf(false) }

    // Saved projects register
    var savedProjectsList = remember {
        mutableStateListOf<CollageProject>(
            CollageProject(
                "p_1", "Executive Site Assembly", "تجمع موقع تنفيذي",
                listOf(1, 2, 3), 4, AspectRatioType.STANDARD, 8f, 12f, BackgroundStyle.SOLID, Color(0xFF04142B), 2f, Color(0xFF00BFA5), true, 0f,
                true, true, true, false, true, false, 3
            )
        )
    }

    // Modal pickers
    var showAddPhotosDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showProjectManagerDialog by remember { mutableStateOf(false) }

    // Register active undo point
    fun recordHistory(action: String) {
        val currentFrame = HistoryFrame(
            imageIds = selectedImages.map { it.id },
            layoutId = currentLayout.id,
            spacing = spacing,
            cornerRadius = cornerRadius,
            photosPerRow = photosPerRow,
            smartCrop = smartCropEnabled,
            faceProtection = faceProtectionEnabled,
            horizonAlignment = horizonAlignmentEnabled,
            colorMatching = colorMatchingEnabled,
            hasShadow = hasShadow
        )
        undoStack.add(currentFrame)
        redoStack.clear()
    }

    // Auto layout matching criteria (calculates layout based on image count)
    fun autoSelectBestLayout() {
        val qty = selectedImages.size
        val matchingScheme = when (qty) {
            1 -> COLLAGE_SCHEMES[0] // Single
            2 -> COLLAGE_SCHEMES[1] // 1x2 Vertical split
            3 -> COLLAGE_SCHEMES[4] // Triptych left
            4 -> COLLAGE_SCHEMES[3] // Quad grid
            5 -> COLLAGE_SCHEMES[11] // Mosaic
            else -> COLLAGE_SCHEMES[14] // Dense multicell grid
        }
        currentLayout = matchingScheme
    }

    // Smart AI crop & algorithms triggers
    fun triggerHorizonAlignment(align: Boolean) {
        selectedImages = selectedImages.map {
            it.copy(currentHorizonAngle = if (align) 0f else it.initialHorizonAngle)
        }
    }

    fun triggerColorMatch(match: Boolean) {
        selectedImages = selectedImages.map { img ->
            img.copy(currentExposureOffset = if (match) 0f else img.initialExposureOffset)
        }
    }

    // Dynamic duplicate warning logic
    val duplicatesList = remember(selectedImages, duplicateDetectionEnabled) {
        if (!duplicateDetectionEnabled) emptySet<Int>()
        else {
            val seen = mutableSetOf<Int>()
            val dups = mutableSetOf<Int>()
            selectedImages.forEach {
                if (!seen.add(it.id)) {
                    dups.add(it.id)
                }
            }
            dups
        }
    }

    // Trigger state autosave notification
    LaunchedEffect(
        selectedImages, currentLayout, currentAspectRatio, spacing, cornerRadius,
        smartCropEnabled, faceProtectionEnabled, backgroundStyle, solidBackgroundColor,
        borderWidth, borderColor, hasShadow
    ) {
        // Debounce autosave simulation
        delay(1500)
        // Auto Save to current log trace
    }

    // Beautiful styling parameters
    val surfaceColor = Color(0xFF010A18)
    val accentTurquoise = Color(0xFF00BFA5)
    val darkCardColor = Color(0xFF04142B)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF010A18), // Deep slate navy background
        topBar = {
            Surface(
                color = Color(0xFF020E22), // Styled darker top bar color
                border = BorderStroke(1.dp, accentTurquoise.copy(alpha = 0.08f)),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        IconButton(
                            onClick = { /* menu action */ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column {
                            Text(
                                text = if (isAr) "تجميع الصور" else "Photo Collage",
                                style = TextStyle(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = if (isAr) "ادمج صورك الذكية" else "Auto-Combine Photos",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    // Top control buttons (Undo, Redo, Projects List)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (undoStack.isNotEmpty()) {
                                    val last = undoStack.removeLast()
                                    redoStack.add(
                                        HistoryFrame(
                                            selectedImages.map { it.id }, currentLayout.id, spacing, cornerRadius,
                                            photosPerRow, smartCropEnabled, faceProtectionEnabled, horizonAlignmentEnabled,
                                            colorMatchingEnabled, hasShadow
                                        )
                                    )
                                    selectedImages = SAMPLE_POOL.filter { it.id in last.imageIds }
                                    currentLayout = COLLAGE_SCHEMES.find { it.id == last.layoutId } ?: currentLayout
                                    spacing = last.spacing
                                    cornerRadius = last.cornerRadius
                                    photosPerRow = last.photosPerRow
                                    smartCropEnabled = last.smartCrop
                                    faceProtectionEnabled = last.faceProtection
                                    horizonAlignmentEnabled = last.horizonAlignment
                                    colorMatchingEnabled = last.colorMatching
                                    hasShadow = last.hasShadow
                                }
                            },
                            enabled = undoStack.isNotEmpty(),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Undo",
                                tint = if (undoStack.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (redoStack.isNotEmpty()) {
                                    val top = redoStack.removeLast()
                                    undoStack.add(
                                        HistoryFrame(
                                            selectedImages.map { it.id }, currentLayout.id, spacing, cornerRadius,
                                            photosPerRow, smartCropEnabled, faceProtectionEnabled, horizonAlignmentEnabled,
                                            colorMatchingEnabled, hasShadow
                                        )
                                    )
                                    selectedImages = SAMPLE_POOL.filter { it.id in top.imageIds }
                                    currentLayout = COLLAGE_SCHEMES.find { it.id == top.layoutId } ?: currentLayout
                                    spacing = top.spacing
                                    cornerRadius = top.cornerRadius
                                    photosPerRow = top.photosPerRow
                                    smartCropEnabled = top.smartCrop
                                    faceProtectionEnabled = top.faceProtection
                                    horizonAlignmentEnabled = top.horizonAlignment
                                    colorMatchingEnabled = top.colorMatching
                                    hasShadow = top.hasShadow
                                }
                            },
                            enabled = redoStack.isNotEmpty(),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Redo,
                                contentDescription = "Redo",
                                tint = if (redoStack.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { showProjectManagerDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Projects",
                                tint = accentTurquoise,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAr) "اختر الصور" else "SELECT PHOTOS",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = accentTurquoise
                        )
                    )

                    TextButton(
                        onClick = {
                            recordHistory("Clear All")
                            selectedImages = emptyList()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(if (isAr) "مسح الكل" else "Clear All", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White.copy(alpha = 0.6f))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Beautiful custom dotted border empty state card matching the mockup exactly
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF031633).copy(alpha = 0.4f))
                        .drawBehind {
                            drawRoundRect(
                                color = accentTurquoise,
                                style = Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                ),
                                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                            )
                        }
                        .clickable { showAddPhotosDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Drawing professional overlapping card visual elements in turquoise
                        Box(
                            modifier = Modifier.size(54.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Bottom/Behind shifted card block
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .offset(x = (-8).dp, y = (-4).dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF020E22))
                                    .border(BorderStroke(1.5.dp, accentTurquoise.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                            )
                            // Top overlapping card block
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .offset(x = 4.dp, y = 4.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF020E22))
                                    .border(BorderStroke(1.5.dp, accentTurquoise), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Mini gallery sun circle inside top card
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopStart)
                                        .offset(5.dp, 5.dp)
                                        .background(accentTurquoise, CircleShape)
                                )
                            }
                            // Overlay Circle Plus badge
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(accentTurquoise, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (isAr) "أضف صورك للبدء" else "Add photos to get started",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isAr) "اضغط على الزر أدناه لإضافة لقطات\nمن معرض الصور الخاص بك." else "Tap the button below to add photos\nfrom your gallery.",
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // "+ Add Photos" Button in turquoise
                        Button(
                            onClick = { showAddPhotosDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = accentTurquoise),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = if (isAr) "+ إضافة صور" else "+ Add Photos",
                                style = TextStyle(
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                // Smooth Horizontal Slider of selected pool ONLY displayed if we have selected images
                if (selectedImages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(darkCardColor, RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(selectedImages) { img ->
                            val isDup = duplicatesList.contains(img.id)
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        BorderStroke(
                                            2.dp,
                                            if (isDup) Color(0xFFFF5252) else accentTurquoise.copy(alpha = 0.3f)
                                        ),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .background(img.color)
                            ) {
                                if (img.url != null) {
                                    AsyncImage(
                                        model = img.url,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.verticalGradient(listOf(img.startColor, img.endColor)))
                                    )
                                }

                                // Mini badge/delete action
                                IconButton(
                                    onClick = {
                                        recordHistory("Remove image")
                                        selectedImages = selectedImages.filter { it != img }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(18.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 2: COLLAGE PREVIEW (LAYOUTS GRID)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAr) "مخطط التجميع" else "COLLAGE PREVIEW",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = accentTurquoise
                        )
                    )

                    // Segmented Options pick controller: [Square] and [Rectangle]
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(BorderStroke(1.dp, accentTurquoise.copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
                            .background(Color(0xFF031633).copy(alpha = 0.3f))
                    ) {
                        listOf("Square", "Rectangle").forEach { option ->
                            val isSelected = (option == "Square" && currentAspectRatio == AspectRatioType.SQUARE) ||
                                    (option == "Rectangle" && currentAspectRatio != AspectRatioType.SQUARE)
                            Box(
                                modifier = Modifier
                                    .background(if (isSelected) accentTurquoise else Color.Transparent)
                                    .clickable {
                                        currentAspectRatio = if (option == "Square") AspectRatioType.SQUARE else AspectRatioType.WIDESCREEN
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Square else Icons.Default.CropSquare,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.Black else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isAr) (if (option == "Square") "مربع" else "مستطيل") else option,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Grid mapping 15 template items elegantly (3 rows of 5 columns) matching the mockup!
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val itemsPerRow = 5
                    val totalItems = COLLAGE_SCHEMES.size
                    val totalRows = (totalItems + itemsPerRow - 1) / itemsPerRow

                    for (rowIdx in 0 until totalRows) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (colIdx in 0 until itemsPerRow) {
                                val itemIdx = (rowIdx * itemsPerRow) + colIdx
                                if (itemIdx < totalItems) {
                                    val scheme = COLLAGE_SCHEMES[itemIdx]
                                    val isSelected = currentLayout.id == scheme.id
                                    val borderCol = if (isSelected) accentTurquoise else Color.White.copy(alpha = 0.12f)
                                    val cardBg = if (isSelected) accentTurquoise.copy(alpha = 0.15f) else Color(0xFF031633).copy(alpha = 0.15f)

                                    // Render miniature aspect layout template card
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.85f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(cardBg)
                                            .border(
                                                BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderCol),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable {
                                                recordHistory("Change Layout Template")
                                                currentLayout = scheme
                                                useDynamicSpacingGrid = false
                                            }
                                            .padding(3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Micro structural Canvas cell
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                                    .padding(2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    val sizeW = size.width
                                                    val sizeH = size.height
                                                    scheme.slots.forEach { rawRect ->
                                                        val scaledLeft = rawRect.left * sizeW
                                                        val scaledTop = rawRect.top * sizeH
                                                        val scaledRight = rawRect.right * sizeW
                                                        val scaledBottom = rawRect.bottom * sizeH

                                                        drawRect(
                                                            color = if (isSelected) accentTurquoise.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.4f),
                                                            topLeft = Offset(scaledLeft + 0.5f, scaledTop + 0.5f),
                                                            size = Size((scaledRight - scaledLeft - 1f).coerceAtLeast(0.5f), (scaledBottom - scaledTop - 1f).coerceAtLeast(0.5f)),
                                                            style = Stroke(width = 1f)
                                                        )
                                                    }
                                                }

                                                // Top-left tiny numbering badge (1 to 15)
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .size(11.dp)
                                                        .background(accentTurquoise, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${scheme.id}",
                                                        fontSize = 7.sp,
                                                        color = Color.Black,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Text(
                                                text = if (isAr) scheme.nameAr else scheme.nameEn,
                                                fontSize = 7.sp,
                                                color = if (isSelected) accentTurquoise else Color.White.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 3: AUTO COLLAGE PREVIEW (CANVAS CONTAINER)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAr) "معاينة التجميع الآلية" else "Auto Collage Preview",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    )

                    // Interactive Toolbar
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { activeCompareMode = !activeCompareMode },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = "Compare",
                                tint = if (activeCompareMode) accentTurquoise else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { fullScreenPreview = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                zoomFactor = (zoomFactor + 0.15f).coerceAtMost(2.5f)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Zoom In",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                zoomFactor = (zoomFactor - 0.15f).coerceAtLeast(0.7f)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "Zoom Out",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                zoomFactor = 1.0f
                                globalPanOffset = Offset.Zero
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterCenterFocus,
                                contentDescription = "Reset Camera",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // The Golden Canvas Card representing real-time rendering compilation
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = solidBackgroundColor),
                    border = BorderStroke(1.5.dp, borderColor.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (hasShadow) 12.dp else 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(currentAspectRatio.ratio)
                            .background(
                                when (backgroundStyle) {
                                    BackgroundStyle.SOLID -> solidBackgroundColor
                                    BackgroundStyle.GRADIENT -> Color.Transparent
                                    BackgroundStyle.TRANSPARENT_GRID -> Color.Transparent
                                }
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background Transparent Checkerboard grid pattern rendering or Gradient backplane drawing
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (backgroundStyle == BackgroundStyle.TRANSPARENT_GRID) {
                                val sizeGrid = 16f
                                val cols = (size.width / sizeGrid).toInt() + 1
                                val rows = (size.height / sizeGrid).toInt() + 1
                                for (c in 0..cols) {
                                    for (r in 0..rows) {
                                        val fillCol = if ((c + r) % 2 == 0) Color.DarkGray.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.5f)
                                        drawRect(
                                            color = fillCol,
                                            topLeft = Offset(c * sizeGrid, r * sizeGrid),
                                            size = Size(sizeGrid, sizeGrid)
                                        )
                                    }
                                }
                            } else if (backgroundStyle == BackgroundStyle.GRADIENT) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        listOf(solidBackgroundColor, accentTurquoise.copy(alpha = 0.4f))
                                    )
                                )
                            }
                        }

                        // Compare layouts split visual bar
                        var splitScreenX by remember { mutableStateOf(0.5f) }

                        // Draw Collage elements inside high speed acceleration Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    // Custom user pan guestures
                                }
                        ) {
                            val viewW = size.width
                            val viewH = size.height

                            val currentTargetSlots = if (useDynamicSpacingGrid) {
                                // Draw mathematically calculated rows dynamically on demand!
                                val count = selectedImages.size.coerceAtLeast(1)
                                val rowsNeeded = (count + photosPerRow - 1) / photosPerRow
                                val lst = mutableListOf<Rect>()
                                for (i in 0 until count) {
                                    val rowIdx = i / photosPerRow
                                    val colIdx = i % photosPerRow
                                    val blockW = 1f / photosPerRow
                                    val blockH = 1f / rowsNeeded
                                    lst.add(
                                        Rect(
                                            colIdx * blockW,
                                            rowIdx * blockH,
                                            (colIdx + 1) * blockW,
                                            (rowIdx + 1) * blockH
                                        )
                                    )
                                }
                                lst
                            } else {
                                currentLayout.slots
                            }

                            // Render each occupied slot on Canvas
                            currentTargetSlots.forEachIndexed { index, rootRect ->
                                val scaledLeft = rootRect.left * viewW
                                val scaledTop = rootRect.top * viewH
                                val scaledRight = rootRect.right * viewW
                                val scaledBottom = rootRect.bottom * viewH

                                val cellW = scaledRight - scaledLeft
                                val cellH = scaledBottom - scaledTop

                                // Apply image padding margin
                                val insetLeft = scaledLeft + spacing
                                val insetTop = scaledTop + spacing
                                val insetWidth = cellW - (spacing * 2)
                                val insetHeight = cellH - (spacing * 2)

                                if (insetWidth > 0 && insetHeight > 0) {
                                    // Identify image from selected queue
                                    val img = if (selectedImages.isNotEmpty()) {
                                        selectedImages[index % selectedImages.size]
                                    } else null

                                    val cellPath = Path().apply {
                                        addRoundRect(
                                            RoundRect(
                                                rect = Rect(Offset(insetLeft, insetTop), Size(insetWidth, insetHeight)),
                                                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                                            )
                                        )
                                    }

                                    // Store canvas translation state
                                    clipPath(cellPath) {
                                        if (img != null) {
                                            val localBitmap = loadedImagesMap[img.id]
                                            if (localBitmap != null) {
                                                val imageBitmap = localBitmap.asImageBitmap()
                                                val srcWidth = localBitmap.width.toFloat()
                                                val srcHeight = localBitmap.height.toFloat()
                                                val dstWidth = insetWidth
                                                val dstHeight = insetHeight
                                                
                                                val srcRatio = srcWidth / srcHeight
                                                val dstRatio = dstWidth / dstHeight
                                                
                                                val srcSize = if (srcRatio > dstRatio) {
                                                    val newWidth = srcHeight * dstRatio
                                                    IntSize(newWidth.toInt(), srcHeight.toInt())
                                                } else {
                                                    val newHeight = srcWidth / dstRatio
                                                    IntSize(srcWidth.toInt(), newHeight.toInt())
                                                }
                                                
                                                val srcOffset = if (srcRatio > dstRatio) {
                                                    IntOffset(((srcWidth - srcSize.width) / 2).toInt(), 0)
                                                } else {
                                                    IntOffset(0, ((srcHeight - srcSize.height) / 2).toInt())
                                                }
                                                
                                                drawImage(
                                                    image = imageBitmap,
                                                    srcOffset = srcOffset,
                                                    srcSize = srcSize,
                                                    dstOffset = IntOffset(insetLeft.toInt(), insetTop.toInt()),
                                                    dstSize = IntSize(insetWidth.toInt(), insetHeight.toInt())
                                                )
                                            } else {
                                                // Draw High-Res Simulated Gradient scenery structure
                                                val grad = Brush.verticalGradient(
                                                    listOf(
                                                        img.startColor.copy(alpha = 1f - img.currentExposureOffset),
                                                        img.endColor.copy(alpha = 1f + img.currentExposureOffset)
                                                    )
                                                )
                                                drawRect(
                                                    brush = grad,
                                                    topLeft = Offset(insetLeft, insetTop),
                                                    size = Size(insetWidth, insetHeight)
                                                )
                                            }

                                            if (localBitmap == null) {
                                                // Smart AI Crop Simulation: Zoom centers the main content automatically!
                                                val subjectScale = if (smartCropEnabled) 1.25f else 1.0f
                                                val scaleFactor = zoomFactor * subjectScale

                                                // Rotate canvas according to Horizon tilt/alignment settings!
                                                val angleToUse = img.currentHorizonAngle
                                                val centerX = insetLeft + (insetWidth / 2f)
                                                val centerY = insetTop + (insetHeight / 2f)

                                                rotate(angleToUse, Offset(centerX, centerY)) {
                                                    // Draw beautiful vector details representing natural structure components!
                                                    val horizonY = centerY + (insetHeight * 0.1f)
                                                    drawLine(
                                                        color = Color.White.copy(alpha = 0.25f),
                                                        start = Offset(insetLeft, horizonY),
                                                        end = Offset(insetLeft + insetWidth, horizonY),
                                                        strokeWidth = 2f
                                                    )

                                                    // Draw sky stars or clouds representation
                                                    drawCircle(
                                                        color = Color.Yellow.copy(alpha = 0.35f),
                                                        center = Offset(centerX - (insetWidth * 0.2f), centerY - (insetHeight * 0.25f)),
                                                        radius = (insetWidth * 0.15f).coerceAtMost(30f) * scaleFactor
                                                    )

                                                    // AI Spot indicator or structural emoji rendering
                                                    // Draws beautiful large professional icons dynamically
                                                }
                                            }

                                            // Draw Category/Emoji & Label Info inside the preview slot
                                            val labelText = "${img.emoji} ${if (isAr) img.titleAr else img.titleEn}"
                                            // Draw custom title metadata
                                            // Render overlay representing Face Protection system if active
                                            if (faceProtectionEnabled && img.keyFaces.isNotEmpty()) {
                                                img.keyFaces.forEach { rawFace ->
                                                    val faceX = insetLeft + (rawFace.x * insetWidth)
                                                    val faceY = insetTop + (rawFace.y * insetHeight)
                                                    drawCircle(
                                                        color = accentTurquoise.copy(alpha = 0.8f),
                                                        center = Offset(faceX, faceY),
                                                        radius = 12f,
                                                        style = Stroke(width = 2f)
                                                    )
                                                    drawCircle(
                                                        color = accentTurquoise.copy(alpha = 0.3f),
                                                        center = Offset(faceX, faceY),
                                                        radius = 6f
                                                    )
                                                }
                                            }

                                            // Best Photo AI selection glow border
                                            if (bestPhotoSelectionEnabled && img.qualityScore > 0.9f) {
                                                drawPath(
                                                    path = cellPath,
                                                    color = accentTurquoise,
                                                    style = Stroke(width = 4f + (sin(index.toFloat()) * 2f))
                                                )
                                            }
                                        } else {
                                            // Fallback empty preview slot color
                                            drawRect(
                                                color = Color.White.copy(alpha = 0.05f),
                                                topLeft = Offset(insetLeft, insetTop),
                                                size = Size(insetWidth, insetHeight)
                                            )
                                        }
                                    }

                                    // Outer Border Rendering
                                    if (borderWidth > 0f) {
                                        drawPath(
                                            path = cellPath,
                                            color = borderColor,
                                            style = Stroke(width = borderWidth)
                                        )
                                    }
                                }
                            }

                            // Dynamic compare slide visualization overlay Line
                            if (activeCompareMode) {
                                val lineX = splitScreenX * viewW
                                drawLine(
                                    color = accentTurquoise,
                                    start = Offset(lineX, 0f),
                                    end = Offset(lineX, viewH),
                                    strokeWidth = 3f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                                )
                                drawCircle(
                                    color = accentTurquoise,
                                    center = Offset(lineX, viewH / 2f),
                                    radius = 16f
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isAr) "توزيع آلي يعتمد على دقة الرندر والسرعة" else "Calculated layout dynamically rendered via Compose",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    if (zoomFactor != 1.0f) {
                        Text(
                            text = "Zoom: ${"%.1f".format(zoomFactor)}x",
                            fontSize = 10.sp,
                            color = accentTurquoise,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ==========================================
            // SECTION 4: DETAILED LAYOUT SETTINGS
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .background(darkCardColor, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isAr) "تخصيص الخصائص والمسافات" else "Layout Settings",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                )

                Divider(color = Color.White.copy(alpha = 0.08f))

                // Control A: Spacing slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isAr) "هامش فواصل الصور" else "Image Spacing", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        Text("${spacing.toInt()} px", fontSize = 12.sp, color = accentTurquoise, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = spacing,
                        onValueChange = {
                            recordHistory("Change Spacing")
                            spacing = it
                        },
                        valueRange = 0f..24f,
                        colors = SliderDefaults.colors(
                            thumbColor = accentTurquoise,
                            activeTrackColor = accentTurquoise
                        )
                    )
                }

                // Control B: Corner Radius slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isAr) "دوران الزوايا" else "Corner Radius", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        Text("${cornerRadius.toInt()} px", fontSize = 12.sp, color = accentTurquoise, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = cornerRadius,
                        onValueChange = {
                            recordHistory("Change Radius")
                            cornerRadius = it
                        },
                        valueRange = 0f..24f,
                        colors = SliderDefaults.colors(
                            thumbColor = accentTurquoise,
                            activeTrackColor = accentTurquoise
                        )
                    )
                }

                // Control C: Photos Per Row slider (visible if free-grid enabled)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(if (isAr) "شبكة صفوف ديناميكية" else "Dynamic Row Grid", fontSize = 12.sp, color = Color.White)
                        Text(if (isAr) "تجاوز مخططات القوالب وحساب الصفوف" else "Synthesizes grid by row metrics", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                    Switch(
                        checked = useDynamicSpacingGrid,
                        onCheckedChange = { useDynamicSpacingGrid = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = accentTurquoise)
                    )
                }

                if (useDynamicSpacingGrid) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (isAr) "عدد الصور في الصف" else "Photos Per Row", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                            Text("$photosPerRow", fontSize = 12.sp, color = accentTurquoise, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = photosPerRow.toFloat(),
                            onValueChange = {
                                recordHistory("Change photos per row")
                                photosPerRow = it.toInt()
                            },
                            valueRange = 2f..5f,
                            steps = 2,
                            colors = SliderDefaults.colors(
                                thumbColor = accentTurquoise,
                                activeTrackColor = accentTurquoise
                            )
                        )
                    }
                }

                // Control D: Aspect Ratio Selector
                Column {
                    Text(if (isAr) "نسب الأبعاد الرياضية (المقاس)" else "Aspect Ratio", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AspectRatioType.values().forEach { ratio ->
                            val isSelected = currentAspectRatio == ratio
                            val btnBg = if (isSelected) accentTurquoise else Color.White.copy(alpha = 0.05f)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(btnBg)
                                    .clickable {
                                        recordHistory("Change Aspect Ratio")
                                        currentAspectRatio = ratio
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isAr) ratio.labelAr else ratio.labelEn,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }

                // Control E: Background styles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isAr) "نمط الخلفية" else "Background Style", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            BackgroundStyle.values().forEach { style ->
                                val isSelected = backgroundStyle == style
                                val fillCol = if (isSelected) accentTurquoise else Color.White.copy(alpha = 0.05f)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(fillCol)
                                        .clickable { backgroundStyle = style },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = style.name,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isAr) "لون الخلفية" else "Solid Color", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(Color(0xFF04142B), Color(0xFF004D40), Color(0xFF1A1A1A), Color(0xFFE0F2F1), Color.Transparent).forEach { colColor ->
                                val isSelected = solidBackgroundColor == colColor
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (colColor == Color.Transparent) Color.DarkGray else colColor)
                                        .border(
                                            BorderStroke(
                                                1.5.dp,
                                                if (isSelected) Color.White else Color.Transparent
                                            ), CircleShape
                                        )
                                        .clickable { solidBackgroundColor = colColor }
                                ) {
                                    if (colColor == Color.Transparent) {
                                        Text("T", modifier = Modifier.align(Alignment.Center), fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // Control F: Border settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (isAr) "عرض حدود الإطار" else "Border Width", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            Text("${borderWidth.toInt()} px", fontSize = 11.sp, color = accentTurquoise)
                        }
                        Slider(
                            value = borderWidth,
                            onValueChange = { borderWidth = it },
                            valueRange = 0f..8f
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isAr) "لون حدود الإطار" else "Border Color", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(accentTurquoise, Color.White, Color(0xFFED7D31), Color.Transparent).forEach { bCol ->
                                val isSelected = borderColor == bCol
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (bCol == Color.Transparent) Color.DarkGray else bCol)
                                        .border(
                                            BorderStroke(
                                                1.5.dp,
                                                if (isSelected) Color.White else Color.Transparent
                                            ), CircleShape
                                        )
                                        .clickable { borderColor = bCol }
                                ) {
                                    if (bCol == Color.Transparent) {
                                        Text("X", modifier = Modifier.align(Alignment.Center), fontSize = 9.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // Shadow & Blur Control toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                            .clickable { hasShadow = !hasShadow }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isAr) "ظلال خلايا التجميع" else "Shadow Effects", fontSize = 11.sp, color = Color.White)
                        Checkbox(
                            checked = hasShadow,
                            onCheckedChange = { hasShadow = it },
                            colors = CheckboxDefaults.colors(checkedColor = accentTurquoise)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                            .clickable {
                                backgroundBlur = if (backgroundBlur == 0f) 10f else 0f
                            }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isAr) "تغبيش وضبابية الخلفية" else "Background Blur", fontSize = 11.sp, color = Color.White)
                        Switch(
                            checked = backgroundBlur > 0f,
                            onCheckedChange = {
                                backgroundBlur = if (it) 10f else 0f
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentTurquoise)
                        )
                    }
                }
            }

            // ==========================================
            // SECTION 5: SMART AI ASSIST TOOLS
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .background(darkCardColor, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accentTurquoise, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAr) "مساعد التوجيه والتحسين الذكي" else "Real-Time AI Toolset",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(accentTurquoise.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Active GPU", color = accentTurquoise, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // AI Tool Grid (6 specific functions in spec)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    // Row A
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // AI Tool 1: Smart Crop
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clickable { smartCropEnabled = !smartCropEnabled },
                            colors = CardDefaults.cardColors(
                                containerColor = if (smartCropEnabled) Color(0xFF032629) else Color.White.copy(alpha = 0.02f)
                            ),
                            border = BorderStroke(1.dp, if (smartCropEnabled) accentTurquoise else Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Crop,
                                        contentDescription = null,
                                        tint = if (smartCropEnabled) accentTurquoise else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isAr) "قص ذكي ومحاذاة" else "Smart Crop",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    if (isAr) "تركيز تلقائي على الهدف" else "Autocenters critical regions",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }

                        // AI Tool 2: Face Protection
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clickable { faceProtectionEnabled = !faceProtectionEnabled },
                            colors = CardDefaults.cardColors(
                                containerColor = if (faceProtectionEnabled) Color(0xFF032629) else Color.White.copy(alpha = 0.02f)
                            ),
                            border = BorderStroke(1.dp, if (faceProtectionEnabled) accentTurquoise else Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = null,
                                        tint = if (faceProtectionEnabled) accentTurquoise else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isAr) "تأمين الوجوه ومراقبتها" else "Face Protection",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    if (isAr) "يمنع اقتصاص الوجوه" else "Prevents facial occlusions",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    // Row B
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // AI Tool 3: Horizon Alignment
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clickable {
                                    horizonAlignmentEnabled = !horizonAlignmentEnabled
                                    triggerHorizonAlignment(horizonAlignmentEnabled)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (horizonAlignmentEnabled) Color(0xFF032629) else Color.White.copy(alpha = 0.02f)
                            ),
                            border = BorderStroke(1.dp, if (horizonAlignmentEnabled) accentTurquoise else Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Landscape,
                                        contentDescription = null,
                                        tint = if (horizonAlignmentEnabled) accentTurquoise else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isAr) "تعديل خط الأفق" else "Horizon Align",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    if (isAr) "يعدل ميلان اللقطة آلياً" else "Straightens tilted lines",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }

                        // AI Tool 4: Color Matching
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clickable {
                                    colorMatchingEnabled = !colorMatchingEnabled
                                    triggerColorMatch(colorMatchingEnabled)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (colorMatchingEnabled) Color(0xFF032629) else Color.White.copy(alpha = 0.02f)
                            ),
                            border = BorderStroke(1.dp, if (colorMatchingEnabled) accentTurquoise else Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = if (colorMatchingEnabled) accentTurquoise else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isAr) "تناسق ومطابقة الألوان" else "Color Matching",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    if (isAr) "يوحد درجات التعريض ودرجة الحرارة" else "Aligns white point exposure",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    // Row C
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // AI Tool 5: Duplicate Detection
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clickable { duplicateDetectionEnabled = !duplicateDetectionEnabled },
                            colors = CardDefaults.cardColors(
                                containerColor = if (duplicateDetectionEnabled) Color(0xFF032629) else Color.White.copy(alpha = 0.02f)
                            ),
                            border = BorderStroke(1.dp, if (duplicateDetectionEnabled) accentTurquoise else Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.BurstMode,
                                        contentDescription = null,
                                        tint = if (duplicateDetectionEnabled) accentTurquoise else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isAr) "كشف الصور المتكررة" else "Duplicate Detect",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    if (isAr) "ينبه عند إضافة مكرر لخط الإنتاج" else "Warns if same file added twice",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }

                        // AI Tool 6: Best Photo Selection
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clickable { bestPhotoSelectionEnabled = !bestPhotoSelectionEnabled },
                            colors = CardDefaults.cardColors(
                                containerColor = if (bestPhotoSelectionEnabled) Color(0xFF032629) else Color.White.copy(alpha = 0.02f)
                            ),
                            border = BorderStroke(1.dp, if (bestPhotoSelectionEnabled) accentTurquoise else Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Stars,
                                        contentDescription = null,
                                        tint = if (bestPhotoSelectionEnabled) accentTurquoise else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isAr) "الفرز الفائق للجودة" else "Best Selection",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    if (isAr) "يبرز الصور عالية الدقة والتباين" else "Pinpoints the highest-res pool",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 6: EXPORT & EXPORT CONFIGURATOR
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                Button(
                    onClick = { showExportDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = accentTurquoise),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("collage_export_button")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAr) "حفظ وتصدير التجميع النهائي" else "Save & Export Collage",
                        style = TextStyle(
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }

    // ==========================================
    // DIALOG 1: ADD MULTI SELECTION GALLERY PICKER (UPDATED: 2 OPTIONS SELECTOR)
    // ==========================================
    if (showAddPhotosDialog) {
        Dialog(
            onDismissRequest = { showAddPhotosDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.5.dp, accentTurquoise.copy(alpha = 0.4f)), RoundedCornerShape(16.dp)),
                color = surfaceColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isAr) "إضافة صور للبدء" else "Add Photos to Get Started",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isAr) "اختر طريقة لإضافة صورك إلى لوحة التجميع" else "Choose how you want to add photos to your collage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Option 1: Pick from Device Gallery / File Memory
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(12.dp))
                            .clickable {
                                showAddPhotosDialog = false
                                galleryLauncher.launch("image/*")
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(accentTurquoise.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = accentTurquoise,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAr) "فتح ذاكرة الهاتف الداخلية" else "Open Device Memory",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAr) "اختر صورة أو أكثر من معرض الصور بجهازك" else "Select one or more photos from storage",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Option 2: Capture with Mobile Camera
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(12.dp))
                            .clickable {
                                showAddPhotosDialog = false
                                val u = getTempCameraUri(context)
                                if (u != null) {
                                    tempCameraUri = u
                                    val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.CAMERA
                                    )
                                    if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        cameraLauncher.launch(u)
                                    } else {
                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (isAr) "تعذر تشغيل الكاميرا" else "Failed to initialize camera"
                                        )
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFFE040FB).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Camera,
                                    contentDescription = null,
                                    tint = Color(0xFFE040FB),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAr) "التقاط صورة بالكاميرا" else "Capture with Camera",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAr) "التقط صورة جديدة من كاميرا هاتفك الآن" else "Take a new photo using your camera now",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    OutlinedButton(
                        onClick = { showAddPhotosDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(if (isAr) "إلغاء" else "Cancel")
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG 2: SAVE & EXPORT CONFIGURATOR
    // ==========================================
    if (showExportDialog) {
        Dialog(onDismissRequest = { showExportDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.5.dp, accentTurquoise.copy(alpha = 0.3f)), RoundedCornerShape(16.dp)),
                color = surfaceColor
            ) {
                var selectedQuality by remember { mutableStateOf(QualityLevel.HD) }
                var fileFormat by remember { mutableStateOf("PNG") }
                var compressionSetting by remember { mutableStateOf(85f) }
                var preserveMetadata by remember { mutableStateOf(true) }
                var transparentExport by remember { mutableStateOf(false) }

                var exportProgress by remember { mutableStateOf(0f) }
                var activeExporting by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        if (isAr) "خيارات تصدير التجميع الفائق" else "Save & Export Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Divider(color = Color.White.copy(alpha = 0.08f))

                    if (activeExporting) {
                        // Export Progress Slider
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                if (isAr) "جاري تصنيع وتصدير الصورة..." else "Synthesizing High Resolution Frame...",
                                style = TextStyle(fontSize = 12.sp, color = Color.White)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = exportProgress,
                                color = accentTurquoise,
                                trackColor = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${(exportProgress * 100).toInt()}%",
                                style = TextStyle(fontWeight = FontWeight.Bold, color = accentTurquoise)
                            )
                        }
                    } else {
                        // Format Picker
                        Column {
                            Text(if (isAr) "صيغة الملف المستهدفة" else "File Extension Format", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("JPG", "PNG", "WEBP").forEach { fmt ->
                                    val isSelected = fileFormat == fmt
                                    val fillC = if (isSelected) accentTurquoise else Color.White.copy(alpha = 0.05f)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(fillC)
                                            .clickable { fileFormat = fmt },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            fmt,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Quality picker
                        Column {
                            Text(if (isAr) "دقة وحجم الرندر النهائي" else "Export Quality Resolution", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(6.dp))
                            QualityLevel.values().forEach { ql ->
                                val isSelected = selectedQuality == ql
                                val cardBg = if (isSelected) accentTurquoise.copy(alpha = 0.12f) else Color.Transparent
                                val borderC = if (isSelected) accentTurquoise else Color.White.copy(alpha = 0.1f)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(cardBg)
                                        .border(BorderStroke(1.dp, borderC), RoundedCornerShape(6.dp))
                                        .clickable { selectedQuality = ql }
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        ql.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) accentTurquoise else Color.White
                                    )
                                    Text(
                                        ql.resolution,
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }

                        // Quality compression slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(if (isAr) "تعديل جودة ضغط الملف" else "Compression Settings (Quality)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                                Text("${compressionSetting.toInt()}%", fontSize = 11.sp, color = accentTurquoise)
                            }
                            Slider(
                                value = compressionSetting,
                                onValueChange = { compressionSetting = it },
                                valueRange = 50f..100f
                            )
                        }

                        // Switches: Transparent PNG, Preserve metadata
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .clickable { preserveMetadata = !preserveMetadata }
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(if (isAr) "بيانات الميتاداتا" else "Preserve EXIF", fontSize = 10.sp, color = Color.White)
                                Checkbox(
                                    checked = preserveMetadata,
                                    onCheckedChange = { preserveMetadata = it },
                                    colors = CheckboxDefaults.colors(checkedColor = accentTurquoise)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .clickable { transparentExport = !transparentExport }
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(if (isAr) "تفريغ شفاف PNG" else "Transparent PNG", fontSize = 10.sp, color = Color.White)
                                Checkbox(
                                    checked = transparentExport,
                                    onCheckedChange = { transparentExport = it },
                                    colors = CheckboxDefaults.colors(checkedColor = accentTurquoise)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showExportDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text(if (isAr) "إغلاق" else "Close")
                        }

                        Button(
                            onClick = {
                                showExportDialog = false
                                saveFileLauncher.launch("collage_${System.currentTimeMillis()}.jpg")
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = accentTurquoise)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isAr) "حفظ بالمعرض" else "Save File",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG 3: PROJECT MANAGEMENT & AUTO-SAVES
    // ==========================================
    if (showProjectManagerDialog) {
        Dialog(onDismissRequest = { showProjectManagerDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.5.dp, accentTurquoise.copy(alpha = 0.3f)), RoundedCornerShape(16.dp)),
                color = surfaceColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        if (isAr) "قائمة المشاريع ومسودات الحفظ" else "Project Management Registry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Divider(color = Color.White.copy(alpha = 0.08f))

                    // Auto saving banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accentTurquoise, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isAr) "الحفظ التلقائي قيد التشغيل والعمل حالياً" else "Autosave actively tracking changes locally",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // Save Current Project layout button
                    var localProjectTitle by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = localProjectTitle,
                            onValueChange = { localProjectTitle = it },
                            placeholder = { Text(if (isAr) "عنوان مشروع تجميع جديد" else "New project name", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentTurquoise,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(fontSize = 12.sp)
                        )

                        Button(
                            onClick = {
                                if (localProjectTitle.isNotEmpty()) {
                                    val newProj = CollageProject(
                                        id = "p_${System.currentTimeMillis()}",
                                        nameEn = localProjectTitle,
                                        nameAr = localProjectTitle,
                                        imageIds = selectedImages.map { it.id },
                                        layoutId = currentLayout.id,
                                        aspectRatio = currentAspectRatio,
                                        spacingPx = spacing,
                                        cornerRadiusPx = cornerRadius,
                                        backgroundStyle = backgroundStyle,
                                        solidColor = solidBackgroundColor,
                                        borderWidthPx = borderWidth,
                                        borderColor = borderColor,
                                        hasShadow = hasShadow,
                                        blurRadiusPx = backgroundBlur,
                                        smartCropEnabled = smartCropEnabled,
                                        faceProtectionEnabled = faceProtectionEnabled,
                                        horizonAlignmentEnabled = horizonAlignmentEnabled,
                                        colorMatchingEnabled = colorMatchingEnabled,
                                        duplicateDetectionEnabled = duplicateDetectionEnabled,
                                        bestPhotoSelectionEnabled = bestPhotoSelectionEnabled,
                                        photosPerRow = photosPerRow
                                    )
                                    savedProjectsList.add(newProj)
                                    localProjectTitle = ""
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (isAr) "تم حفظ المشروع وحالته الحالية بنجاح!"
                                            else "Project saved successfully!"
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentTurquoise)
                        ) {
                            Text(if (isAr) "حفظ جديد" else "Save Current", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Iterative Saved list
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        savedProjectsList.forEach { proj ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Load fully stored past projects parameters!
                                        selectedImages = SAMPLE_POOL.filter { it.id in proj.imageIds }
                                        currentLayout = COLLAGE_SCHEMES.find { it.id == proj.layoutId } ?: currentLayout
                                        currentAspectRatio = proj.aspectRatio
                                        spacing = proj.spacingPx
                                        cornerRadius = proj.cornerRadiusPx
                                        backgroundStyle = proj.backgroundStyle
                                        solidBackgroundColor = proj.solidColor
                                        borderWidth = proj.borderWidthPx
                                        borderColor = proj.borderColor
                                        hasShadow = proj.hasShadow
                                        backgroundBlur = proj.blurRadiusPx
                                        smartCropEnabled = proj.smartCropEnabled
                                        faceProtectionEnabled = proj.faceProtectionEnabled
                                        horizonAlignmentEnabled = proj.horizonAlignmentEnabled
                                        colorMatchingEnabled = proj.colorMatchingEnabled
                                        duplicateDetectionEnabled = proj.duplicateDetectionEnabled
                                        bestPhotoSelectionEnabled = proj.bestPhotoSelectionEnabled
                                        photosPerRow = proj.photosPerRow

                                        showProjectManagerDialog = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (isAr) "تم تحميل قالب مشروع: ${proj.nameAr}"
                                                else "Loaded project: ${proj.nameEn}"
                                            )
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            if (isAr) proj.nameAr else proj.nameEn,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            "${proj.imageIds.size} images • Layout #${proj.layoutId} • Slot Ratio ${proj.aspectRatio.labelEn}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.4f)
                                        )
                                    }

                                    IconButton(
                                        onClick = { savedProjectsList.remove(proj) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    OutlinedButton(
                        onClick = { showProjectManagerDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(if (isAr) "إغلاق" else "Close Windows")
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG 4: FULLSCREEN VIEW FOR ZOOM PAN COMPARE
    // ==========================================
    if (fullScreenPreview) {
        Dialog(
            onDismissRequest = { fullScreenPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Backplane canvas
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing centered preview container under global parameters
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .aspectRatio(currentAspectRatio.ratio),
                            colors = CardDefaults.cardColors(containerColor = solidBackgroundColor),
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val viewW = size.width
                                    val viewH = size.height

                                    val currentTargetSlots = if (useDynamicSpacingGrid) {
                                        val count = selectedImages.size.coerceAtLeast(1)
                                        val rowsNeeded = (count + photosPerRow - 1) / photosPerRow
                                        val lst = mutableListOf<Rect>()
                                        for (i in 0 until count) {
                                            val rowIdx = i / photosPerRow
                                            val colIdx = i % photosPerRow
                                            val blockW = 1f / photosPerRow
                                            val blockH = 1f / rowsNeeded
                                            lst.add(
                                                Rect(
                                                    colIdx * blockW,
                                                    rowIdx * blockH,
                                                    (colIdx + 1) * blockW,
                                                    (rowIdx + 1) * blockH
                                                )
                                            )
                                        }
                                        lst
                                    } else {
                                        currentLayout.slots
                                    }

                                    currentTargetSlots.forEachIndexed { index, rect ->
                                        val insetLeft = (rect.left * viewW) + spacing
                                        val insetTop = (rect.top * viewH) + spacing
                                        val insetWidth = ((rect.right - rect.left) * viewW) - (spacing * 2)
                                        val insetHeight = ((rect.bottom - rect.top) * viewH) - (spacing * 2)

                                        if (insetWidth > 0 && insetHeight > 0) {
                                            val img = if (selectedImages.isNotEmpty()) {
                                                selectedImages[index % selectedImages.size]
                                            } else null

                                            val cellPath = Path().apply {
                                                addRoundRect(
                                                    RoundRect(
                                                        rect = Rect(Offset(insetLeft, insetTop), Size(insetWidth, insetHeight)),
                                                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                                                    )
                                                )
                                            }

                                            clipPath(cellPath) {
                                                if (img != null) {
                                                    val localBitmap = loadedImagesMap[img.id]
                                                    if (localBitmap != null) {
                                                        val imageBitmap = localBitmap.asImageBitmap()
                                                        val srcWidth = localBitmap.width.toFloat()
                                                        val srcHeight = localBitmap.height.toFloat()
                                                        val dstWidth = insetWidth
                                                        val dstHeight = insetHeight
                                                        
                                                        val srcRatio = srcWidth / srcHeight
                                                        val dstRatio = dstWidth / dstHeight
                                                        
                                                        val srcSize = if (srcRatio > dstRatio) {
                                                            val newWidth = srcHeight * dstRatio
                                                            IntSize(newWidth.toInt(), srcHeight.toInt())
                                                        } else {
                                                            val newHeight = srcWidth / dstRatio
                                                            IntSize(srcWidth.toInt(), newHeight.toInt())
                                                        }
                                                        
                                                        val srcOffset = if (srcRatio > dstRatio) {
                                                            IntOffset(((srcWidth - srcSize.width) / 2).toInt(), 0)
                                                         } else {
                                                            IntOffset(0, ((srcHeight - srcSize.height) / 2).toInt())
                                                        }
                                                        
                                                        drawImage(
                                                            image = imageBitmap,
                                                            srcOffset = srcOffset,
                                                            srcSize = srcSize,
                                                            dstOffset = IntOffset(insetLeft.toInt(), insetTop.toInt()),
                                                            dstSize = IntSize(insetWidth.toInt(), insetHeight.toInt())
                                                        )
                                                    } else {
                                                        drawRect(
                                                            brush = Brush.verticalGradient(
                                                                listOf(
                                                                    img.startColor.copy(alpha = 1f - img.currentExposureOffset),
                                                                    img.endColor.copy(alpha = 1f + img.currentExposureOffset)
                                                                )
                                                            ),
                                                            topLeft = Offset(insetLeft, insetTop),
                                                            size = Size(insetWidth, insetHeight)
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

                    // Top back action overlay
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { fullScreenPreview = false },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        Text(
                            if (isAr) "معاينة كامل الشاشة الملقمة" else "GPU Fullscreen Lightbox",
                            style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        )

                        Box(modifier = Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}

private fun rotateImageIfRequired(context: Context, img: Bitmap, selectedImage: Uri): Bitmap {
    return try {
        val input = context.contentResolver.openInputStream(selectedImage) ?: return img
        val exifInterface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.media.ExifInterface(input)
        } else {
            return img
        }
        val orientation = exifInterface.getAttributeInt(
            android.media.ExifInterface.TAG_ORIENTATION,
            android.media.ExifInterface.ORIENTATION_NORMAL
        )
        input.close()
        
        val matrix = android.graphics.Matrix()
        when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return img
        }
        
        val rotated = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        if (rotated != img) {
            img.recycle()
        }
        rotated
    } catch (e: Exception) {
        e.printStackTrace()
        img
    }
}

fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    var inputStream: java.io.InputStream? = null
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()
        inputStream = null

        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        val maxSide = 1200
        var inSampleSize = 1
        val originalMaxSide = Math.max(options.outWidth, options.outHeight)
        if (originalMaxSide > maxSide) {
            inSampleSize = originalMaxSide / maxSide
            if (inSampleSize < 1) inSampleSize = 1
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inJustDecodeBounds = false
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        
        inputStream = context.contentResolver.openInputStream(uri)
        var bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        inputStream?.close()
        inputStream = null

        if (bitmap == null) return null

        var mutableBmp = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (mutableBmp != bitmap) {
            bitmap.recycle()
        }

        rotateImageIfRequired(context, mutableBmp, uri)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } catch (oom: OutOfMemoryError) {
        oom.printStackTrace()
        System.gc()
        null
    } finally {
        try {
            inputStream?.close()
        } catch (ignored: Exception) {}
    }
}

fun getTempCameraUri(context: Context): Uri? {
    return try {
        val cacheDir = context.cacheDir
        val tempFile = File.createTempFile("camera_temp_", ".jpg", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun renderCollageToBitmap(
    context: Context,
    width: Int,
    height: Int,
    selectedImages: List<CollageImage>,
    currentLayout: LayoutScheme,
    useDynamicSpacingGrid: Boolean,
    photosPerRow: Int,
    spacing: Float,
    cornerRadius: Float,
    backgroundStyle: BackgroundStyle,
    solidBackgroundColor: Color,
    loadedImagesMap: Map<Int, Bitmap>,
    smartCropEnabled: Boolean,
    zoomFactor: Float
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(
            (solidBackgroundColor.alpha * 255).toInt(),
            (solidBackgroundColor.red * 255).toInt(),
            (solidBackgroundColor.green * 255).toInt(),
            (solidBackgroundColor.blue * 255).toInt()
        )
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
    
    val currentTargetSlots = if (useDynamicSpacingGrid) {
        val count = selectedImages.size.coerceAtLeast(1)
        val rowsNeeded = (count + photosPerRow - 1) / photosPerRow
        val lst = mutableListOf<Rect>()
        for (i in 0 until count) {
            val rowIdx = i / photosPerRow
            val colIdx = i % photosPerRow
            val blockW = 1f / photosPerRow
            val blockH = 1f / rowsNeeded
            lst.add(
                Rect(
                    colIdx * blockW,
                    rowIdx * blockH,
                    (colIdx + 1) * blockW,
                    (rowIdx + 1) * blockH
                )
            )
        }
        lst
    } else {
        currentLayout.slots
    }
    
    currentTargetSlots.forEachIndexed { index, rect ->
        val insetLeft = (rect.left * width) + spacing
        val insetTop = (rect.top * height) + spacing
        val insetRight = (rect.right * width) - spacing
        val insetBottom = (rect.bottom * height) - spacing
        
        val w = insetRight - insetLeft
        val h = insetBottom - insetTop
        
        if (w > 0 && h > 0) {
            val img = if (selectedImages.isNotEmpty()) {
                selectedImages[index % selectedImages.size]
            } else null
            
            if (img != null) {
                val path = android.graphics.Path()
                val rectF = android.graphics.RectF(insetLeft, insetTop, insetRight, insetBottom)
                path.addRoundRect(rectF, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
                
                canvas.save()
                canvas.clipPath(path)
                
                val localBitmap = loadedImagesMap[img.id]
                if (localBitmap != null) {
                    drawBitmapCenterCrop(canvas, localBitmap, rectF)
                } else {
                    val startC = img.startColor
                    val endC = img.endColor
                    val gradShader = android.graphics.LinearGradient(
                        insetLeft, insetTop, insetLeft, insetBottom,
                        android.graphics.Color.argb((startC.alpha*255).toInt(), (startC.red*255).toInt(), (startC.green*255).toInt(), (startC.blue*255).toInt()),
                        android.graphics.Color.argb((endC.alpha*255).toInt(), (endC.red*255).toInt(), (endC.green*255).toInt(), (endC.blue*255).toInt()),
                        android.graphics.Shader.TileMode.CLAMP
                    )
                    val gradPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        shader = gradShader
                    }
                    canvas.drawRect(rectF, gradPaint)
                }
                canvas.restore()
            }
        }
    }
    
    return bitmap
}

fun drawBitmapCenterCrop(canvas: android.graphics.Canvas, bitmap: Bitmap, targetRect: android.graphics.RectF) {
    val srcWidth = bitmap.width.toFloat()
    val srcHeight = bitmap.height.toFloat()
    val dstWidth = targetRect.width()
    val dstHeight = targetRect.height()
    
    val srcRatio = srcWidth / srcHeight
    val dstRatio = dstWidth / dstHeight
    
    val srcRect = android.graphics.Rect()
    if (srcRatio > dstRatio) {
        val newWidth = srcHeight * dstRatio
        val left = ((srcWidth - newWidth) / 2f).toInt()
        srcRect.set(left, 0, (left + newWidth).toInt(), srcHeight.toInt())
    } else {
        val newHeight = srcWidth / dstRatio
        val top = ((srcHeight - newHeight) / 2f).toInt()
        srcRect.set(0, top, srcWidth.toInt(), (top + newHeight).toInt())
    }
    canvas.drawBitmap(bitmap, srcRect, targetRect, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG))
}
