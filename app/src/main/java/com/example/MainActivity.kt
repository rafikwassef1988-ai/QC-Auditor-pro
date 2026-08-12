package com.example

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val repository = AuditRepository(database.auditDao())
        val viewModelFactory = AuditViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                val viewModel: AuditViewModel = viewModel(factory = viewModelFactory)
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

// Global lookup for dynamic bilingual translation
object Bilingual {
    private val en = mapOf(
        "app_name" to "QC Internal Audit",
        "tab_form" to "Form",
        "tab_ncr_obs" to "NCR/OBS Sheet",
        "tab_summary" to "Tracking Summary",
        "tab_history" to "Prev Audits",
        "tab_auditee" to "Auditee Response",
        "tab_export" to "Export",
        "proj_info" to "Project Information",
        "proj_name" to "Project Name",
        "proj_num" to "Project Number",
        "location" to "Location / Site",
        "proj_pm" to "Project Manager",
        "qc_pm" to "QC Manager",
        "subcontractor" to "Contractor / Sub",
        "proj_type" to "Project Type",
        "stage_phase" to "Stage / Phase",
        "audit_details" to "Audit Details",
        "audit_num" to "Audit Number",
        "audit_date" to "Audit Date",
        "duration" to "Duration (days)",
        "auditor_name" to "Auditor Name",
        "rep_date" to "Report Issuance Date",
        "followup_date" to "Follow-up Due Date",
        "form_ref" to "Form Reference",
        "audit_scope" to "Audit Scope / Zone",
        "summary" to "Findings Summary",
        "ncrs" to "NCRs",
        "obs" to "Observations",
        "major" to "Major",
        "minor" to "Minor",
        "open" to "Open",
        "closed" to "Closed",
        "findings" to "Findings - NCR / OBS",
        "add_finding" to "Add Finding",
        "signatures" to "Signatures Approval",
        "issued_by" to "Issued by (Auditor)",
        "reviewed_by" to "Reviewed by (QA/QC Director)",
        "name" to "Name",
        "designation" to "Designation",
        "date" to "Date",
        "save_report" to "Save Audit",
        "reports_history" to "Saved Audits List",
        "auditee_identification" to "Auditee Identification",
        "auditee_name" to "Auditee Name",
        "auditee_desig" to "Designation / Role",
        "auditee_company" to "Company / Subcontractor",
        "response_date" to "Response Date",
        "phone" to "Phone / Contact",
        "email" to "Email Address",
        "general_remarks" to "General Remarks & Commitments",
        "proposed_closure_date" to "Proposed Overall Closure Date",
        "overall_status" to "Overall Response Status",
        "preventive_measures" to "Preventive Measures & Improvements",
        "preventive_actions" to "Preventive Actions (Avoid Recurrence)",
        "training_actions" to "Training / Awareness Actions",
        "procedure_changes" to "Process / Procedure Changes",
        "rectification_photos" to "Rectification & Closure Photos",
        "supporting_docs" to "Supporting Documents",
        "auditee_signatures" to "Auditee Acknowledgement Signatures",
        "auditee_rep" to "Auditee Representative",
        "auditee_sup" to "Auditee Supervisor",
        "qc_reviewer_acceptance" to "QC Auditor - Review & Acceptance",
        "acceptance_status" to "Acceptance Status",
        "review_date" to "Review Date",
        "reviewer_remarks" to "Auditor Remarks on Response",
        "custom_logo" to "Internal Audit Custom Logo",
        "export_report" to "Export Report",
        "export_docx" to "Export Word Doc (.docx)",
        "export_pptx" to "Export PowerPoint (.pptx)",
        "export_pdf" to "Export PDF Document",
        "export_xlsx" to "Export Excel Spreadsheet (.xlsx)",
        "share_pdf" to "Share PDF Document",
        "share_docx" to "Share Word Doc (.docx)",
        "share_pptx" to "Share PowerPoint (.pptx)",
        "share_xlsx" to "Share Excel Spreadsheet (.xlsx)",
        "preview" to "Text Summary Preview",
        "refresh_preview" to "Refresh Preview Text",
        "prev_ncr_t" to "NCRs Total",
        "prev_ncr_c" to "NCRs Closed",
        "prev_obs_t" to "OBS Total",
        "prev_obs_c" to "OBS Closed"
    )

    private val ar = mapOf(
        "app_name" to "المراجعة الداخلية لضبط الجودة",
        "tab_form" to "النموذج",
        "tab_ncr_obs" to "نموذج NCR-OBS",
        "tab_summary" to "ملخص المتابعة",
        "tab_history" to "المراجعات",
        "tab_auditee" to "رد الجهة المنفذة",
        "tab_export" to "التصدير",
        "proj_info" to "معلومات المشروع",
        "proj_name" to "اسم المشروع",
        "proj_num" to "رقم المشروع",
        "location" to "الموقع / موقع العمل",
        "proj_pm" to "مدير المشروع",
        "qc_pm" to "مدير ضبط الجودة",
        "subcontractor" to "المقاول / المقاول الفرعي",
        "proj_type" to "نوع المشروع",
        "stage_phase" to "المرحلة / خطوة العمل",
        "audit_details" to "تفاصيل المراجعة",
        "audit_num" to "رقم المراجعة",
        "audit_date" to "تاريخ المراجعة",
        "duration" to "المدة (أيام)",
        "auditor_name" to "اسم المراجع",
        "rep_date" to "تاريخ إصدار التقرير",
        "followup_date" to "تاريخ متابعة الرد",
        "form_ref" to "مرجع النموذج",
        "audit_scope" to "نطاق المراجعة / المنطقة",
        "summary" to "ملخص نتائج التدقيق",
        "ncrs" to "الـ NCRs",
        "obs" to "الملاحظات",
        "major" to "جسيم",
        "minor" to "بسيط",
        "open" to "مفتوحة",
        "closed" to "مغلقة",
        "findings" to "النتائج والتقارير المرصودة — NCR / OBS",
        "add_finding" to "إضافة ملاحظة جديدة",
        "signatures" to "الاعتمادات والتوقيعات",
        "issued_by" to "صادر من (المراجع بالتكليف)",
        "reviewed_by" to "تمت مراجعته بواسطة (مدير ضمان الجودة)",
        "name" to "الاسم كامل",
        "designation" to "المسمى الوظيفي",
        "date" to "التاريخ",
        "save_report" to "حفظ التقرير",
        "reports_history" to "قائمة التقارير المحفوظة",
        "auditee_identification" to "بيانات الجهة الخاضعة للتدقيق",
        "auditee_name" to "اسم ممثل التدقيق الخارجي",
        "auditee_desig" to "الدور الوظيفي",
        "auditee_company" to "اسم الشركة / المقاول المنفذ",
        "response_date" to "تاريخ الرد المعتمد",
        "phone" to "رقم الهاتف / الاتصال",
        "email" to "البريد الإلكتروني",
        "general_remarks" to "ملاحظات وتعهدات عامة",
        "proposed_closure_date" to "التاريخ المقترح للإغلاق التام",
        "overall_status" to "حالة الاستجابة والمطابقة",
        "preventive_measures" to "الإجراءات الوقائية وتحسين جودة العمل",
        "preventive_actions" to "الإجراءات المانعة لتكرار المخالفة",
        "training_actions" to "المناقشات التدريبية وجلسات التوعية",
        "procedure_changes" to "تعديل العمليات وإجراءات التنفيذ",
        "rectification_photos" to "صور معالجة العيوب وإقفال الملاحظات",
        "supporting_docs" to "المستندات الداعمة والمرفقات",
        "auditee_signatures" to "إقرارات وتوقيعات ممثلي الجهة المنفذة",
        "auditee_rep" to "مهندس الموقع المسؤول",
        "auditee_sup" to "مشرف عام الإنشاءات",
        "qc_reviewer_acceptance" to "مراجعة وقبول المراجع وضوابط التدقيق",
        "acceptance_status" to "حالة الموثوقية والقبول",
        "review_date" to "تاريخ المراجعة والتحقق",
        "reviewer_remarks" to "مرئيات وتوصيات مهندس التدقيق",
        "custom_logo" to "شعار المراجعة الداخلية لضبط الجودة",
        "export_report" to "تصدير التقرير النهائي",
        "export_docx" to "تصدير بصيغة Word (.docx)",
        "export_pptx" to "تصدير بصيغة PowerPoint (.pptx)",
        "export_pdf" to "تصدير بصيغة PDF",
        "export_xlsx" to "تصدير بصيغة Excel (.xlsx)",
        "share_pdf" to "مشاركة بصيغة PDF",

        "share_docx" to "مشاركة بصيغة Word (.docx)",
        "share_pptx" to "مشاركة بصيغة PowerPoint (.pptx)",
        "share_xlsx" to "مشاركة بصيغة Excel (.xlsx)",
        "preview" to "معاينة النص الملخص",
        "refresh_preview" to "تحديث النص المعاين",
        "prev_ncr_t" to "إجمالي الـ NCRs",
        "prev_ncr_c" to "الـ NCRs المغلقة",
        "prev_obs_t" to "إجمالي الملاحظات",
        "prev_obs_c" to "الملاحظات المغلقة"
    )

    fun get(key: String, lang: String): String = if (lang == "ar") ar[key] ?: en[key] ?: key else en[key] ?: key
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    viewModel: AuditViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeDetails by viewModel.activeReport.collectAsStateWithLifecycle()
    val savedReports by viewModel.allReports.collectAsStateWithLifecycle()
    val allFindings by viewModel.allFindings.collectAsStateWithLifecycle()
    val pendingExport by viewModel.pendingExport.collectAsStateWithLifecycle()

    var lang by remember { mutableStateOf("en") }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(pendingExport?.mimeType ?: "*/*")
    ) { uri ->
        val exportObj = pendingExport
        if (uri != null && exportObj != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    exportObj.tempFile.inputStream().use { input ->
                        input.copyTo(os)
                    }
                }
                val successMsg = if (lang == "ar") "تم الحفظ بنجاح بموقعك المختار!" else "${exportObj.fileType} successfully saved to selected location!"
                Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                val failMsg = if (lang == "ar") "فشل الحفظ: ${e.message}" else "Failed to save file: ${e.message}"
                Toast.makeText(context, failMsg, Toast.LENGTH_SHORT).show()
            } finally {
                viewModel.clearPendingExport()
            }
        } else {
            viewModel.clearPendingExport()
        }
    }

    val currentExport = pendingExport
    if (currentExport != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearPendingExport() },
            title = {
                Text(
                    text = if (lang == "ar") "اختر موقع حفظ التصدير" else "Choose Export Destination",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (lang == "ar") {
                        "أين تريد حفظ ملف ${currentExport.fileType}؟ يمكنك حفظه في مجلد التنزيلات الافتراضي، أو تحديد مجلد مخصص على جهازك."
                    } else {
                        "Where would you like to save the generated ${currentExport.fileType}? You can save it to the default Downloads folder or choose a custom folder on your device."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fileName = currentExport.tempFile.name
                        createDocumentLauncher.launch(fileName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (lang == "ar") "اختيار مجلد مخصص..." else "Choose Custom Folder...")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { viewModel.clearPendingExport() }
                    ) {
                        Text(if (lang == "ar") "إلغاء" else "Cancel")
                    }
                    Button(
                        onClick = {
                            viewModel.savePendingToDownloads(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (lang == "ar") "الافتراضي (التنزيلات)" else "Default (Downloads)")
                    }
                }
            }
        )
    }
    var currentTab by remember { mutableStateOf("home") }
    var showReportsListDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    fun getString(key: String): String = Bilingual.get(key, lang)

    val isRtl = lang == "ar"

    val isTabDark = when (currentTab) {
        "home", "settings", "documents", "gemini" -> true
        else -> false
    }
    val appBackground = if (isTabDark) {
        if (currentTab == "settings") Color(0xFF071930) else Color(0xFF010E24)
    } else {
        MaterialTheme.colorScheme.background
    }

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? android.app.Activity)?.window
            if (window != null) {
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isTabDark
                controller.isAppearanceLightNavigationBars = !isTabDark
            }
        }
    }

    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides if (isRtl) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = appBackground
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(appBackground)
            ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "tabAnim"
                ) { targetState ->
                    when (targetState) {
                        "home" -> {
                            StartScreen(
                                lang = lang,
                                onToggleLang = { lang = if (lang == "en") "ar" else "en" },
                                onNavigate = { target ->
                                    currentTab = target
                                    if (target == "form" && activeDetails?.report?.projectName?.isEmpty() == true) {
                                        try {
                                            val prefs = context.getSharedPreferences("QC_Prefs", Context.MODE_PRIVATE)
                                            val defAuditor = prefs.getString("default_auditor_name", "") ?: ""
                                            val defQc = prefs.getString("default_qc_manager", "") ?: ""
                                            val defLocation = prefs.getString("default_location", "") ?: ""
                                            if (defAuditor.isNotEmpty() || defQc.isNotEmpty() || defLocation.isNotEmpty()) {
                                                viewModel.updateReport { report ->
                                                    report.copy(
                                                        auditorName = report.auditorName.ifEmpty { defAuditor },
                                                        sigAuditorName = report.sigAuditorName.ifEmpty { defAuditor },
                                                        qcManager = report.qcManager.ifEmpty { defQc },
                                                        location = report.location.ifEmpty { defLocation }
                                                    )
                                                }
                                            }
                                        } catch (e: java.lang.Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                },
                                onOpenReportsList = { currentTab = "documents" },
                                onOpenSettings = { currentTab = "settings" },
                                viewModel = viewModel,
                                savedReports = savedReports,
                                allFindings = allFindings
                            )
                        }
                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                            ) {
                                // Title bar
                                if (targetState != "settings" && targetState != "documents" && targetState != "gemini" && targetState != "collage") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primary)
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { currentTab = "home" }) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowBack,
                                                contentDescription = "Back",
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Inventory,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = when (targetState) {
                                                "form" -> getString("tab_form")
                                                "ncr_obs" -> getString("tab_ncr_obs")
                                                "summary" -> getString("tab_summary")
                                                "history" -> getString("tab_history")
                                                "auditee" -> getString("tab_auditee")
                                                "export" -> getString("tab_export")
                                                else -> getString("app_name")
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Archive reports list trigger
                                        IconButton(onClick = { showReportsListDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = "Saved Audits List",
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    when (targetState) {
                                        "form" -> FormTab(viewModel, lang)
                                        "ncr_obs" -> NcrObsFormTab(viewModel, lang)
                                        "summary" -> TrackingSummaryTab(viewModel, lang)
                                        "history" -> HistoryTab(viewModel, lang)
                                        "auditee" -> AuditeeResponseTab(viewModel, lang)
                                        "export" -> ExportTab(viewModel, lang)
                                        "settings" -> SettingsTab(viewModel, lang)
                                        "documents" -> SavedAuditsTab(viewModel, lang, onOpenSettings = { currentTab = "settings" }, onNavigateToForm = { currentTab = "form" })
                                        "gemini" -> GeminiAuditorTab(viewModel, lang, onNavigateToHome = { currentTab = "form" })
                                        "collage" -> CollageCreatorTab(lang)
                                    }
                                }

                                if (targetState != "settings" && targetState != "documents" && targetState != "gemini" && targetState != "collage") {
                                    // Bottom floating utility bar: Save report
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Id: ${activeDetails?.report?.auditNumber ?: "None"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { viewModel.createNewBlankReport() },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("New", fontSize = 10.sp)
                                                }
                                                Button(
                                                    onClick = { viewModel.saveReport(context) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(getString("save_report"), fontSize = 10.sp)
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

            // Permanent Bottom Navigation Bar exactly like screenshot!
            CustomBottomBar(
                currentTab = currentTab,
                lang = lang,
                onTabSelected = { tab ->
                    currentTab = tab
                }
            )
        }
    }

        // Saved list history selector dialog overlay
        if (showReportsListDialog) {
            AlertDialog(
                onDismissRequest = { showReportsListDialog = false },
                title = { Text(getString("reports_history"), fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (savedReports.isEmpty()) {
                            Text(
                                text = "No local saved audit reports found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(savedReports) { idx, item ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.loadReport(item.id)
                                                showReportsListDialog = false
                                                Toast
                                                    .makeText(
                                                        context,
                                                        "Loaded Report #${item.auditNumber}",
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                            },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Audit #${item.auditNumber}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = item.projectName.ifEmpty { "Draft Audit" },
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    text = "Date: ${item.auditDate}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(onClick = { viewModel.deleteReport(item.id, context) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showReportsListDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                onDismiss = { showSettingsDialog = false },
                savedReportsCount = savedReports.size,
                onClearAllData = {
                    savedReports.forEach { viewModel.deleteReport(it.id, context) }
                },
                lang = lang
            )
        }
    }
}

// STYLED LETTERHEAD HEADER BANNER COMPONENT AS SHOWN IN USER IMAGE
@Composable
fun Letterhead(report: AuditReport, lang: String, onAuditNumberChange: ((String) -> Unit)? = null) {
    var showEditDialog by remember { mutableStateOf(false) }
    var tempAuditNumber by remember { mutableStateOf(report.auditNumber) }

    if (showEditDialog && onAuditNumberChange != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (lang == "ar") "تعديل رقم التقرير" else "Edit Audit Report #") },
            text = {
                OutlinedTextField(
                    value = tempAuditNumber,
                    onValueChange = { tempAuditNumber = it },
                    label = { Text(if (lang == "ar") "رقم التقرير" else "Report Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAuditNumberChange(tempAuditNumber)
                        showEditDialog = false
                    }
                ) {
                    Text(if (lang == "ar") "حفظ" else "Save")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showEditDialog = false }) {
                    Text(if (lang == "ar") "إلغاء" else "Cancel")
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black)
            .background(Color.White)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Box: Logo
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_qc_audit_logo_1781912328532),
                contentDescription = Bilingual.get("custom_logo", lang),
                modifier = Modifier.height(30.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))

        // Middle Box: Title and Code input in the center of the sheet form
        Column(
            modifier = Modifier
                .weight(1.8f)
                .fillMaxHeight()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (lang == "ar") "تقرير التدقيق الداخلي للجودة # " else "QC Internal Audit Report # ",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                ),
                color = Color.Black
            )
            
            val codeVal = report.auditNumber

            Spacer(modifier = Modifier.height(3.dp))

            // Code input box
            Box(
                modifier = Modifier
                    .width(115.dp)
                    .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (codeVal.isEmpty()) {
                    Text(
                        text = if (lang == "ar") "أدخل الكود" else "Enter Code",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = Color.Gray
                        ),
                        textAlign = TextAlign.Center
                    )
                }
                BasicTextField(
                    value = codeVal,
                    onValueChange = { newCode ->
                        if (onAuditNumberChange != null) {
                            onAuditNumberChange(newCode)
                        }
                    },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color(0xFF2E7D32),
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))

        // Right Box: Department Details
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Quality Control Department",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center
                ),
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "Internal Audit",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center
                ),
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun TableCell(
    text: String,
    isLabel: Boolean,
    modifier: Modifier = Modifier,
    isBoldValue: Boolean = false,
    isCentered: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (isLabel) Color(0xFFF5F5F5) else Color.White)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = if (isCentered) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontSize = if (isLabel) 9.5.sp else 10.5.sp,
            fontWeight = if (isLabel || isBoldValue) FontWeight.Bold else FontWeight.Normal,
            color = if (isLabel) Color.DarkGray else Color.Black,
            textAlign = if (isCentered) TextAlign.Center else TextAlign.Start
        )
    }
}

@Composable
fun TableCellInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(Color.White)
            .clickable { focusRequester.requestFocus() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                color = Color.LightGray,
                fontSize = 11.sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = Color.Black,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
    }
}

@Composable
fun TableCellDatePickerInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(Color.White)
            .clickable {
                showDatePickerDialog(context, value, onValueChange)
            }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                color = Color.LightGray,
                fontSize = 11.sp
            )
        } else {
            Text(
                text = value,
                color = Color.Black,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun TableCellProjectDropdownInput(
    projectName: String,
    projectNumber: String,
    onProjectSelected: (String, String) -> Unit,
    placeholderName: String,
    placeholderNumber: String,
    options: List<Pair<String, Pair<String, String>>>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(Color.White)
            .clickable { expanded = true }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1.4f)) {
                    if (projectName.isEmpty() && placeholderName.isNotEmpty()) {
                        Text(
                            text = placeholderName,
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = projectName,
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
                Text(" / ", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 2.dp))
                Box(modifier = Modifier.weight(0.6f)) {
                    if (projectNumber.isEmpty() && placeholderNumber.isNotEmpty()) {
                        Text(
                            text = placeholderNumber,
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = projectNumber,
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Expand project dropdown",
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.first, fontSize = 11.sp, color = Color.Black) },
                    onClick = {
                        onProjectSelected(option.second.first, option.second.second)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TableCellDropdownInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    options: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(Color.White)
            .clickable { expanded = true }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        text = value,
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Expand dropdown",
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 11.sp, color = Color.Black) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ProjectInfoGridTable(report: AuditReport, viewModel: AuditViewModel, lang: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black)
            .background(Color.White)
    ) {
        // Row 1: Project Name/Number & Location
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            TableCell(text = "Project name/ Number:", isLabel = true, modifier = Modifier.weight(1.1f))
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            val projectOptions = listOf(
                "Borouj 1.17 IBE046" to ("Borouj 1.17" to "IBE046"),
                "Borouj 1.03 IBE045" to ("Borouj 1.03" to "IBE045"),
                "Borouj SA32 - 1.14 - 1.02" to ("Borouj" to "SA32 - 1.14 - 1.02"),
                "French University SA36" to ("French University" to "SA36"),
                "Mostakbal Misr -Zone H2SA3" to ("Mostakbal Misr -Zone H2" to "SA3"),
                "Mostakbal Misr -Zone HSA29" to ("Mostakbal Misr -Zone H" to "SA29"),
                "New Delta Project SA37" to ("New Delta Project" to "SA37"),
                "R07 SA02" to ("R07" to "SA02"),
                "R05 E003" to ("R05" to "E003"),
                "Park St. Edition PK#01 SA43" to ("Park St. Edition PK#01" to "SA43"),
                "New Alamein Downtown (SA06)" to ("New Alamein Downtown" to "SA06"),
                "Allianz HQ (SA30)" to ("Allianz HQ" to "SA30"),
                "Mivida Gardens (SA52)" to ("Mivida Gardens" to "SA52"),
                "Cairo Gate PK 46 Bldg 5, 6, 7 (SA48)" to ("Cairo Gate PK 46 Bldg 5, 6, 7" to "SA48"),
                "American School BV BP#24 (SA28)" to ("American School BV BP#24" to "SA28"),
                "PK31A Locanda (SA40)" to ("PK31A Locanda" to "SA40"),
                "Uptown Cairo – Sports club (SA21 – SA27 – SA35 – SA41)" to ("Uptown Cairo – Sports club" to "SA21 – SA27 – SA35 – SA41"),
                "Soul PK2BR-1 (SA33)" to ("Soul PK2BR-1" to "SA33")
            )
            TableCellProjectDropdownInput(
                projectName = report.projectName,
                projectNumber = report.projectNumber,
                onProjectSelected = { pName, pNum ->
                    viewModel.updateReport { it.copy(projectName = pName, projectNumber = pNum) }
                },
                placeholderName = "Select Name",
                placeholderNumber = "Select Num",
                options = projectOptions,
                modifier = Modifier.weight(1.8f)
            )
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCell(text = "Location:", isLabel = true, modifier = Modifier.weight(1.0f))
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCellInput(
                value = report.location,
                onValueChange = { loc -> viewModel.updateReport { it.copy(location = loc) } },
                placeholder = "El Shrouk City",
                modifier = Modifier.weight(1.5f)
            )
        }
        Divider(color = Color.Black, thickness = 1.dp)

        // Row 2: Project Manager & Acting QC Manager
        val pmOptions = listOf(
            "Ahmad Zahran",
            "Islam Kassem",
            "George Dawood",
            "Maged Mattar",
            "Malak Tawadrous",
            "Mina Khalil",
            "Mina Maged",
            "Mohamed Abdallah",
            "Mohamed Hasaan",
            "Mohamed Nassef",
            "SalahEldin Mounir",
            "Ahmed Mossa",
            "Amer Samuel",
            "Amr Zanaty",
            "Ahmed Abdelmeguid",
            "Ahmed Elshafaai",
            "Elsaeed Elkayal",
            "Michael Kamal",
            "Michel Manassa",
            "Mina Naiem",
            "Ramez Zaky",
            "Mogeeb Shokry"
        )
        val qcOptions = listOf(
            "Mina Medhat",
            "Mina Yakoub",
            "Ahmed Bayoumi",
            "Amir Azmy amir",
            "Eslam Salah",
            "Eslam Fattouh",
            "Kerollous Nageh",
            "Kirolos Azer",
            "Mohamed Ismail",
            "Mohammed Hosni",
            "Moheb Gendy",
            "Romany Helmy",
            "Sayed Ahmed",
            "Mohamed",
            "Ahmed Mazen",
            "Mohamed Galal"
        )
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            TableCell(text = "Project Manager:", isLabel = true, modifier = Modifier.weight(1.1f))
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCellDropdownInput(
                value = report.projectManager,
                onValueChange = { pm -> viewModel.updateReport { it.copy(projectManager = pm) } },
                placeholder = "Select Project Manager",
                options = pmOptions,
                modifier = Modifier.weight(1.8f)
            )
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCell(text = "Acting QC Manager:", isLabel = true, modifier = Modifier.weight(1.0f))
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCellDropdownInput(
                value = report.qcManager,
                onValueChange = { qcm -> viewModel.updateReport { it.copy(qcManager = qcm) } },
                placeholder = "Select QC Manager",
                options = qcOptions,
                modifier = Modifier.weight(1.5f)
            )
        }
        Divider(color = Color.Black, thickness = 1.dp)

        // Row 3: Audit Date/duration & Auditor
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            TableCell(text = "Audit Date/duration:", isLabel = true, modifier = Modifier.weight(1.1f))
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCellInput(
                value = report.auditDate,
                onValueChange = { dt -> viewModel.updateReport { it.copy(auditDate = dt) } },
                placeholder = "11/06/2026 1Day",
                modifier = Modifier.weight(1.8f)
            )
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCell(text = "Auditor:", isLabel = true, modifier = Modifier.weight(1.0f))
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCellDropdownInput(
                value = report.auditorName,
                onValueChange = { name -> viewModel.updateReport { it.copy(auditorName = name) } },
                placeholder = "Select Auditor",
                options = listOf("Rafik Hisham", "Hesham Saeed"),
                modifier = Modifier.weight(1.5f)
            )
        }
        Divider(color = Color.Black, thickness = 1.dp)

        // Row 4: Report issuance date & Audit follow up date
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            TableCell(text = "Report issuance date:", isLabel = true, modifier = Modifier.weight(1.1f))
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCellDatePickerInput(
                value = report.reportIssuanceDate,
                onValueChange = { dt -> viewModel.updateReport { it.copy(reportIssuanceDate = dt) } },
                placeholder = "14/06/2026",
                modifier = Modifier.weight(1.8f)
            )
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCell(text = "Audit follow up date:", isLabel = true, modifier = Modifier.weight(1.0f))
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCellDatePickerInput(
                value = report.followupDueDate,
                onValueChange = { dt -> viewModel.updateReport { it.copy(followupDueDate = dt) } },
                placeholder = "11/07/2026",
                modifier = Modifier.weight(1.5f)
            )
        }
    }
}

@Composable
fun AuditFindingsSummaryTable(
    ncrCount: Int,
    obsCount: Int,
    dueDate: String,
    lang: String,
    onDueDateChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black)
            .background(Color.White)
    ) {
        // Headers Row
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 44.dp)) {
            TableCell(
                text = if (lang == "ar") "إجمالي عدد الـ NCRs الصادرة" else "Total number of issued NCRs",
                isLabel = true,
                modifier = Modifier.weight(1f),
                isCentered = true
            )
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCell(
                text = if (lang == "ar") "إجمالي عدد الملاحظات الصادرة" else "Total number of issued Observations",
                isLabel = true,
                modifier = Modifier.weight(1.1f),
                isCentered = true
            )
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCell(
                text = if (lang == "ar") "تاريخ الاستحقاق" else "Due date",
                isLabel = true,
                modifier = Modifier.weight(1.1f),
                isCentered = true
            )
        }
        Divider(color = Color.Black, thickness = 1.dp)
        // Values Row
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 44.dp)) {
            TableCell(
                text = String.format("%02d", ncrCount),
                isLabel = false,
                isBoldValue = true,
                modifier = Modifier.weight(1f),
                isCentered = true
            )
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCell(
                text = String.format("%02d", obsCount),
                isLabel = false,
                isBoldValue = true,
                modifier = Modifier.weight(1.1f),
                isCentered = true
            )
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            
            // Interactive Due Date Cell
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .background(Color.White)
                    .clickable {
                        showDatePickerDialog(context, dueDate.ifEmpty { "11/07/2026" }, onDueDateChange)
                    }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Due Date",
                        tint = Color(0xFF0D253F),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dueDate.ifEmpty { "11/07/2026" },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun SignaturePhotoPickerCell(
    sigName: String,
    base64Str: String?,
    onPhotoSelected: (String) -> Unit,
    onPhotoCleared: () -> Unit,
    modifier: Modifier = Modifier,
    lang: String = "en"
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var tempCameraUriStr by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val tempCameraUri = tempCameraUriStr?.let { android.net.Uri.parse(it) }
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                coroutineScope.launch(Dispatchers.IO) {
                    val base = saveUriToInternalStorage(context, uri)
                    if (base.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            onPhotoSelected(base)
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
            tempCameraUriStr?.let { uriStr ->
                try {
                    cameraLauncher.launch(android.net.Uri.parse(uriStr))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            Toast.makeText(context, if (lang == "ar") "إذن الكاميرا مطلوب لالتقاط الصور" else "Camera permission is required to capture photos.", Toast.LENGTH_SHORT).show()
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val base = saveUriToInternalStorage(context, uri)
                withContext(Dispatchers.Main) {
                    onPhotoSelected(base)
                }
            }
        }
    }
    
    var showChooser by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(Color.White)
            .clickable { showChooser = true }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (base64Str != null) {
             var bitmap by remember(base64Str) { mutableStateOf<android.graphics.Bitmap?>(null) }
             LaunchedEffect(base64Str) {
                 try {
                     val decoded = withContext(Dispatchers.IO) {
                         loadImageStringToBitmap(context, base64Str, 400)
                     }
                     bitmap = decoded
                 } catch (e: Exception) {
                     e.printStackTrace()
                 }
             }
            bitmap?.let { bmp ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Signature image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    )
                    // Clear/Remove button on top-right
                    IconButton(
                        onClick = {
                            onPhotoCleared()
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .background(Color(0x99FFFFFF), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear signature",
                            tint = Color.Red,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            } ?: Text(
                text = sigName,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20),
                fontSize = 11.5.sp
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = sigName,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    fontSize = 11.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Add photo signature",
                        tint = Color.Gray,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Pic / صورة",
                        fontSize = 8.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }

    if (showChooser) {
        AlertDialog(
            onDismissRequest = { showChooser = false },
            title = {
                Text(
                    text = if (lang == "ar") "مصدر الصورة ($sigName)" else "Select Image Source ($sigName)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
            },
            text = {
                Text(
                    text = if (lang == "ar") "يرجى اختيار فتح الكاميرا لالتقاط صورة التوقيع أو المعرض لاختيار صورة التوقيع." else "Please choose whether to open the Camera to capture signature, or select from your Gallery.",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = {
                        try {
                            galleryLauncher.launch("image/*")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        showChooser = false
                    }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (lang == "ar") "المعرض" else "Gallery")
                    }
                    Button(onClick = {
                        try {
                            val tempFile = java.io.File.createTempFile("sig_capture_", ".jpg", context.cacheDir).apply {
                                deleteOnExit()
                            }
                            val authority = "${context.packageName}.fileprovider"
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, tempFile)
                            tempCameraUriStr = uri.toString()
                            
                            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.CAMERA
                            )
                            if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        showChooser = false
                    }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (lang == "ar") "الكاميرا" else "Camera")
                    }
                }
            }
        )
    }
}

@Composable
fun SignaturesTable(report: AuditReport, viewModel: AuditViewModel, lang: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black)
            .background(Color.White)
    ) {
        // Headers Row
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            TableCell(text = if (lang == "ar") "صدر بواسطة (مهندس جودة)" else "Audit report issued by", isLabel = true, modifier = Modifier.weight(1.0f), isCentered = true)
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCell(text = if (lang == "ar") "تمت مراجعته وتدقيقه بواسطة" else "Audit report reviewed by", isLabel = true, modifier = Modifier.weight(1.0f), isCentered = true)
        }
        Divider(color = Color.Black, thickness = 1.dp)

        // Signature Row
        Row(modifier = Modifier.fillMaxWidth().height(50.dp)) {
            TableCell(text = "Signature:", isLabel = true, modifier = Modifier.weight(0.42f))
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            SignaturePhotoPickerCell(
                sigName = report.sigAuditorName.ifEmpty { "Auditor" },
                base64Str = report.sigAuditorPh,
                onPhotoSelected = { base -> viewModel.updateReport { it.copy(sigAuditorPh = base) } },
                onPhotoCleared = { viewModel.updateReport { it.copy(sigAuditorPh = null) } },
                modifier = Modifier.weight(0.58f).fillMaxHeight(),
                lang = lang
            )
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCell(text = "Signature:", isLabel = true, modifier = Modifier.weight(0.42f))
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            SignaturePhotoPickerCell(
                sigName = report.sigReviewerName.ifEmpty { "QC Manager" },
                base64Str = report.sigReviewerPh,
                onPhotoSelected = { base -> viewModel.updateReport { it.copy(sigReviewerPh = base) } },
                onPhotoCleared = { viewModel.updateReport { it.copy(sigReviewerPh = null) } },
                modifier = Modifier.weight(0.58f).fillMaxHeight(),
                lang = lang
            )
        }
        Divider(color = Color.Black, thickness = 1.dp)

        // Name Row
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            TableCell(text = "Name:", isLabel = true, modifier = Modifier.weight(0.42f))
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCellDropdownInput(
                value = report.sigAuditorName,
                onValueChange = { name -> viewModel.updateReport { it.copy(sigAuditorName = name) } },
                placeholder = "Select Auditor",
                options = listOf("Rafik Hisham", "Hesham Saeed"),
                modifier = Modifier.weight(0.58f)
            )
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCell(text = "Name:", isLabel = true, modifier = Modifier.weight(0.42f))
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCellInput(
                value = report.sigReviewerName,
                onValueChange = { name -> viewModel.updateReport { it.copy(sigReviewerName = name) } },
                placeholder = "QC Manager Name",
                modifier = Modifier.weight(0.58f)
            )
        }
        Divider(color = Color.Black, thickness = 1.dp)

        // Designation Row
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            TableCell(text = "Designation:", isLabel = true, modifier = Modifier.weight(0.42f))
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCellInput(
                value = report.sigAuditorDesignation,
                onValueChange = { des -> viewModel.updateReport { it.copy(sigAuditorDesignation = des) } },
                placeholder = "Quality Auditor",
                modifier = Modifier.weight(0.58f)
            )
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCell(text = "Designation:", isLabel = true, modifier = Modifier.weight(0.42f))
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCellInput(
                value = report.sigReviewerDesignation,
                onValueChange = { des -> viewModel.updateReport { it.copy(sigReviewerDesignation = des) } },
                placeholder = "Quality Director",
                modifier = Modifier.weight(0.58f)
            )
        }
        Divider(color = Color.Black, thickness = 1.dp)

        // Date Row
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            TableCell(text = "Date:", isLabel = true, modifier = Modifier.weight(0.42f))
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCellDatePickerInput(
                value = report.sigAuditorDate,
                onValueChange = { dt -> viewModel.updateReport { it.copy(sigAuditorDate = dt) } },
                placeholder = "14/06/2026",
                modifier = Modifier.weight(0.58f)
            )
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCell(text = "Date:", isLabel = true, modifier = Modifier.weight(0.42f))
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCellDatePickerInput(
                value = report.sigReviewerDate,
                onValueChange = { dt -> viewModel.updateReport { it.copy(sigReviewerDate = dt) } },
                placeholder = "15/06/2026",
                modifier = Modifier.weight(0.58f)
            )
        }
    }
}

@Composable
fun DocumentFooterTable(report: AuditReport, viewModel: AuditViewModel, totalPages: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black)
            .background(Color.White)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            TableCell(text = "Internal Quality Audit Report", isLabel = true, modifier = Modifier.weight(1.3f))
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCellInput(
                value = report.formReference,
                onValueChange = { ref -> viewModel.updateReport { it.copy(formReference = ref) } },
                placeholder = "innovo/QAQC/FRM - 1.14/05",
                modifier = Modifier.weight(2.0f)
            )
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCell(text = "REV 02", isLabel = true, modifier = Modifier.weight(0.6f), isCentered = true)
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCell(text = "1-Mar-2023", isLabel = true, modifier = Modifier.weight(0.8f), isCentered = true)
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCell(text = "Page 1 of $totalPages", isLabel = true, modifier = Modifier.weight(0.8f), isCentered = true)
        }
    }
}

// 1. FORM TAB DETAIL SECTION
@Composable
fun FormTab(viewModel: AuditViewModel, lang: String) {
    val activeDetailsState by viewModel.activeReport.collectAsStateWithLifecycle()
    val activeDetails = activeDetailsState
    if (activeDetails == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val report = activeDetails.report
    val findings = activeDetails.findings

    fun getString(key: String): String = Bilingual.get(key, lang)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Letterhead(report = report, lang = lang, onAuditNumberChange = { num -> viewModel.updateReport { it.copy(auditNumber = num) } })
        }

        // Project Info Section represented in exactly the 4-row paper-sheet grid
        item {
            ProjectInfoGridTable(report, viewModel, lang)
        }

        // 01- Audit findings summary
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = if (lang == "ar") "01- Audit findings summary:" else "01- Audit findings summary:",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        textDecoration = TextDecoration.Underline
                    ),
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                AuditFindingsSummaryTable(
                    ncrCount = findings.count { it.type == "NCR" },
                    obsCount = findings.count { it.type == "OBS" },
                    dueDate = report.followupDueDate,
                    lang = lang,
                    onDueDateChange = { dt -> viewModel.updateReport { it.copy(followupDueDate = dt) } }
                )
            }
        }

        // 02- Audit Major findings
        item {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = if (lang == "ar") "02- Audit Major findings:" else "02- Audit Major findings:",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        textDecoration = TextDecoration.Underline
                    ),
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }

        item {
            MajorFindingsUnifiedTable(findings = findings, viewModel = viewModel, lang = lang)
        }

        item {
            OutlinedButton(
                onClick = { viewModel.addFinding() },
                border = BorderStroke(1.dp, Color.Black),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(getString("add_finding"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Signatures approval block styled like paper
        item {
            SignaturesTable(report, viewModel, lang)
        }

        // Document bottom official footer box
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                DocumentFooterTable(report, viewModel, totalPages = 1 + findings.size)
            }
        }
    }
}

@Composable
fun DatePickerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    labelString: String,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(labelString) },
            trailingIcon = {
                IconButton(onClick = {
                    showDatePickerDialog(context, value, onValueChange)
                }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Date"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    showDatePickerDialog(context, value, onValueChange)
                }
        )
    }
}

fun showDatePickerDialog(context: android.content.Context, currentValue: String, onDateSelected: (String) -> Unit) {
    val calendar = java.util.Calendar.getInstance()
    if (currentValue.isNotEmpty()) {
        try {
            val delimiters = Regex("[-/.]")
            val parts = currentValue.split(delimiters)
            if (parts.size == 3) {
                if (parts[0].length == 4) {
                    val y = parts[0].toInt()
                    val m = parts[1].toInt() - 1
                    val d = parts[2].toInt()
                    calendar.set(y, m, d)
                } else if (parts[2].length == 4) {
                    val d = parts[0].toInt()
                    val m = parts[1].toInt() - 1
                    val y = parts[2].toInt()
                    calendar.set(y, m, d)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }
    val year = calendar.get(java.util.Calendar.YEAR)
    val month = calendar.get(java.util.Calendar.MONTH)
    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

    android.app.DatePickerDialog(
        context,
        { _, y, m, d ->
            val formattedDate = String.format("%04d-%02d-%02d", y, m + 1, d)
            onDateSelected(formattedDate)
        },
        year,
        month,
        day
    ).show()
}

@Composable
fun TradeDropdownTextField(
    value: String,
    onValueChange: (String) -> Unit,
    labelString: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Civil", "Structure", "Architecture", "MEP", "Infrastructure")
    
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(labelString) },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Select Trade"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = !expanded }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun getActivityCategories(): LinkedHashMap<String, List<String>> {
    val categories = LinkedHashMap<String, List<String>>()
    categories["EARTHWORKS"] = listOf(
        "Site Clearing", "Vegetation Removal", "Tree Removal", "Shrub Removal",
        "Existing Structure Demolition", "Existing Pavement Removal", "Existing Utility Removal",
        "Debris Removal", "Topsoil Stripping", "Disposal of Unsuitable Material", "Site Cleaning",
        "Excavation", "Bulk Excavation", "Foundation Excavation", "Basement Excavation",
        "Trench Excavation", "Pit Excavation", "Lift Pit Excavation", "Equipment Pit Excavation",
        "Rock Excavation", "Mechanical Excavation", "Manual Excavation",
        "Excavation Around Existing Utilities", "Excavation in Restricted Areas",
        "Dewatering", "Well Point System Installation", "Deep Well Installation",
        "Dewatering Pump Installation", "Monitoring Wells", "Water Discharge System",
        "Dewatering Operation", "Groundwater Monitoring",
        "Soil Improvement", "Vibro Compaction", "Dynamic Compaction", "Stone Columns",
        "Grouting", "Soil Stabilization", "Ground Improvement",
        "Backfilling", "Structural Backfill", "Sand Backfill", "Selected Fill",
        "Imported Fill", "Utility Backfill", "Layered Backfill", "Compaction", "Density Testing"
    )
    categories["FOUNDATIONS"] = listOf(
        "Shallow Foundations", "Blinding Concrete", "Isolated Footings", "Combined Footings",
        "Strip Footings", "Raft Foundations", "Tie Beams", "Ground Beams",
        "Deep Foundations", "Bored Piles", "Driven Piles", "CFA Piles", "Micro piles",
        "Barrettes", "Pile Caps", "Pile Integrity Testing", "Static Load Testing", "Dynamic Load Testing"
    )
    categories["REINFORCED CONCRETE WORKS"] = listOf(
        "Substructure", "Raft Foundations", "Basement Walls", "Retaining Walls",
        "Foundation Beams", "Lift Pits", "Pump Rooms", "Water Tanks", "Underground Chambers",
        "Superstructure", "Columns", "Shear Walls", "Core Walls", "Transfer Beams",
        "Beams", "Slabs", "Flat Slabs", "Post-Tension Slabs", "Stairs", "Ramps",
        "Parapets", "Roof Structures",
        "Concrete Activities", "Reinforcement Fabrication", "Reinforcement Installation",
        "Formwork Installation", "Embedded Items Installation", "Concrete Pouring",
        "Concrete Finishing", "Concrete Curing", "Formwork Removal", "Concrete Repair"
    )
    categories["STRUCTURAL STEEL WORKS"] = listOf(
        "Fabrication", "Material Inspection", "Cutting", "Drilling", "Welding",
        "NDT Testing", "Surface Preparation", "Shop Painting", "Galvanizing",
        "Erection", "Anchor Bolts", "Base Plates", "Main Steel Frames", "Secondary Steel",
        "Bracing Systems", "Roof Trusses", "Space Frames", "Steel Decking", "Catwalks",
        "Platforms", "Handrails", "Ladders"
    )
    categories["MASONRY WORKS"] = listOf(
        "Concrete Block Walls", "AAC Block Walls", "Clay Brick Walls", "Stone Masonry",
        "Fire Rated Walls", "Shaft Walls", "Partition Walls", "External Walls",
        "Internal Walls", "Wall Reinforcement", "Lintels", "Wall Ties"
    )
    categories["WATERPROOFING WORKS"] = listOf(
        "Below Ground Waterproofing", "Basement Waterproofing", "Foundation Waterproofing",
        "Retaining Wall Waterproofing", "Water Stops", "Injection Systems",
        "Above Ground Waterproofing", "Roof Waterproofing", "Wet Area Waterproofing",
        "Water Tank Waterproofing", "Swimming Pool Waterproofing", "Expansion Joint Waterproofing"
    )
    categories["THERMAL INSULATION"] = listOf(
        "Roof Insulation", "Wall Insulation", "Pipe Insulation", "Duct Insulation",
        "Equipment Insulation", "Cold Room Insulation"
    )
    categories["ACOUSTIC INSULATION"] = listOf(
        "Acoustic Walls", "Acoustic Ceilings", "Acoustic Floors", "Acoustic Doors", "Acoustic Panels"
    )
    categories["FACADE WORKS"] = listOf(
        "Glass Facades", "Curtain Walls", "Structural Glazing", "Spider Systems", "Skylights",
        "Cladding Systems", "Stone Cladding", "Marble Cladding", "Granite Cladding",
        "Aluminum Cladding", "Composite Panels", "Terracotta Cladding", "GRC Cladding",
        "GRP Cladding", "Metal Cladding",
        "Architectural Features", "Louvers", "Sun Breakers", "Decorative Screens", "Architectural Canopies"
    )
    categories["DOORS AND WINDOWS"] = listOf(
        "Doors", "Wooden Doors", "Metal Doors", "Stainless Steel Doors", "Fire Rated Doors",
        "Acoustic Doors", "Automatic Doors", "Revolving Doors",
        "Windows", "Aluminum Windows", "UPVC Windows", "Glass Partitions", "Curtain Wall Doors"
    )
    categories["PLASTERING WORKS"] = listOf(
        "Internal Plaster", "External Plaster", "Cement Render", "Decorative Render",
        "Waterproof Render", "Repair Mortar Applications"
    )
    categories["FLOOR FINISHES"] = listOf(
        "Ceramic Tiles", "Porcelain Tiles", "Marble Flooring", "Granite Flooring",
        "Terrazzo Flooring", "Vinyl Flooring", "SPC Flooring", "Epoxy Flooring",
        "Polyurethane Flooring", "Raised Floors", "Carpet Flooring", "Wooden Flooring"
    )
    categories["WALL FINISHES"] = listOf(
        "Paint Systems", "Decorative Paint", "Epoxy Paint", "Wall Coverings", "Wallpaper",
        "Wood Cladding", "Stone Cladding", "Marble Cladding", "HPL Panels"
    )
    categories["CEILING FINISHES"] = listOf(
        "Gypsum Board Ceilings", "Metal Ceilings", "Acoustic Ceilings", "Wooden Ceilings",
        "Decorative Ceilings", "Stretch Ceilings"
    )
    categories["JOINERY WORKS"] = listOf(
        "Cabinets", "Wardrobes", "Kitchen Units", "Reception Counters", "Display Units",
        "Wooden Cladding", "Furniture Installation", "Decorative Wood Works"
    )
    categories["PLUMBING WORKS"] = listOf(
        "Water Supply Systems", "Potable Water Networks", "Hot Water Networks",
        "Water Storage Tanks", "Booster Pumps", "Water Heaters",
        "Drainage Systems", "Soil Drainage", "Waste Drainage", "Vent Systems",
        "Storm Water Systems", "Grease Interceptors",
        "Irrigation Systems", "Irrigation Networks", "Irrigation Pumps", "Irrigation Controllers"
    )
    categories["FIRE FIGHTING WORKS"] = listOf(
        "Fire Water Tanks", "Fire Pumps", "Fire Hydrants", "Hose Reel Systems",
        "Sprinkler Systems", "Deluge Systems", "Foam Systems", "Clean Agent Systems",
        "Fire Extinguishers", "Fire Department Connections"
    )
    categories["HVAC WORKS"] = listOf(
        "Chilled Water Systems", "Chillers", "Cooling Towers", "Pumps", "Expansion Tanks",
        "Air Separators",
        "Air Distribution Systems", "Ductwork", "Air Handling Units", "Fan Coil Units",
        "VAV Boxes", "Dampers", "Diffusers", "Grilles",
        "Ventilation Systems", "Exhaust Fans", "Fresh Air Fans", "Smoke Extraction Systems",
        "Stair Pressurization Systems",
        "Refrigerant Systems", "VRF Systems", "Split Units", "Package Units"
    )
    categories["ELECTRICAL WORKS"] = listOf(
        "Medium Voltage", "Substations", "Transformers", "RMU Units", "Medium Voltage Cables",
        "Cable Terminations",
        "Low Voltage", "Main Distribution Boards", "Sub Main Distribution Boards",
        "Distribution Boards", "Cable Trays", "Cable Ladders", "Power Cables",
        "Lighting Systems", "Socket Outlets", "Isolation Switches",
        "Grounding Systems", "Earthing Networks", "Ground Rods", "Equipotential Bonding",
        "Lightning Protection", "Air Terminals", "Down Conductors", "Grounding Network",
        "Backup Power", "Diesel Generators", "Fuel Systems", "Synchronization Panels", "UPS Systems",
        "ELV SYSTEMS", "Structured Cabling", "Data Networks", "Fiber Optic Networks",
        "Telephone Systems", "Wi-Fi Systems", "IPTV Systems", "MATV Systems", "CCTV Systems",
        "Access Control Systems", "Intrusion Detection Systems", "Public Address Systems",
        "Intercom Systems", "BMS Systems", "Parking Management Systems", "Audio Visual Systems",
        "Nurse Call Systems", "Queue Management Systems", "Master Clock Systems"
    )
    categories["VERTICAL TRANSPORTATION"] = listOf(
        "Passenger Elevators", "Service Elevators", "Freight Elevators", "Hospital Elevators",
        "Escalators", "Moving Walkways", "Dumbwaiters", "BMU Systems"
    )
    categories["EXTERNAL WORKS"] = listOf(
        "Boundary Walls", "Security Fences", "Entrance Gates", "Guard Houses",
        "External Lighting", "Site Furniture", "Bollards", "Signage Systems", "Flag Poles"
    )
    categories["INFRASTRUCTURE WORKS"] = listOf(
        "Water Networks", "Potable Water Networks", "Fire Water Networks", "Irrigation Networks",
        "Sewer Networks", "Gravity Sewers", "Force Mains", "Manholes", "Lift Stations",
        "Storm Water Networks", "Storm Pipelines", "Catch Basins", "Culverts", "Retention Ponds",
        "Utility Networks", "Electrical Networks", "Telecom Networks", "Fiber Optic Networks",
        "Utility Tunnels", "Duct Banks"
    )
    categories["ROAD WORKS"] = listOf(
        "Subgrade Preparation", "Subbase Construction", "Road Base Construction",
        "Prime Coat", "Tack Coat", "Asphalt Binder Course", "Asphalt Wearing Course",
        "Concrete Roads", "Curbstones", "Road Markings", "Traffic Signs", "Crash Barriers"
    )
    categories["LANDSCAPE WORKS"] = listOf(
        "Hardscape", "Interlock Paving", "Natural Stone Paving", "Decorative Concrete",
        "Pergolas", "Gazebos", "Benches", "Planters", "Water Features", "Fountains", "Retaining Walls",
        "Soft scape", "Trees", "Palm Trees", "Shrubs", "Ground Cover", "Turf Grass", "Seasonal Plants",
        "Irrigation", "Irrigation Pipework", "Irrigation Valves", "Irrigation Pumps", "Irrigation Controllers"
    )
    categories["INDUSTRIAL WORKS"] = listOf(
        "Process Equipment Foundations", "Storage Tanks", "Silos", "Pressure Vessels",
        "Pipe Racks", "Industrial Piping",
        "Utility Systems", "Compressed Air Systems", "Steam Systems", "Fuel Systems"
    )
    categories["MARINE WORKS"] = listOf(
        "Breakwaters", "Quay Walls", "Jetties", "Dolphins", "Marine Piles", "Fender Systems",
        "Bollard Systems", "Dredging", "Reclamation Works", "Navigation Aids"
    )
    categories["HIGH-RISE BUILDING WORKS"] = listOf(
        "Core Construction", "Jump Form Systems", "Climbing Formwork", "High Rise Facades",
        "BMU Systems", "Sky Bridges", "Observation Decks", "Tuned Mass Dampers"
    )
    categories["TESTING & COMMISSIONING"] = listOf(
        "Material Testing", "Field Testing", "Factory Acceptance Testing", "Site Acceptance Testing",
        "Pressure Testing", "Hydrostatic Testing", "Electrical Testing", "Functional Testing",
        "Integrated Systems Testing", "Performance Testing", "Reliability Testing"
    )
    categories["HANDING OVER"] = listOf(
        "Snagging", "Punch List Closure", "Final Cleaning", "As-Built Drawings",
        "Operation Manuals", "Maintenance Manuals", "Spare Parts Delivery", "Training Programs",
        "Final Inspection", "Final Acceptance", "Taking Over Certificate",
        "Defects Liability Period Management"
    )
    return categories
}

@Composable
fun ActivityDropdownTextField(
    value: String,
    onValueChange: (String) -> Unit,
    labelString: String = "Activity",
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val allCategoriesAndActivities = remember { getActivityCategories() }
    
    val filteredList = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            allCategoriesAndActivities
        } else {
            val result = LinkedHashMap<String, List<String>>()
            allCategoriesAndActivities.forEach { (category, list) ->
                val matching = list.filter { it.contains(searchQuery, ignoreCase = true) }
                if (category.contains(searchQuery, ignoreCase = true) || matching.isNotEmpty()) {
                    result[category] = matching.ifEmpty { list }
                }
            }
            result
        }
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(labelString) },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Select Activity"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = !expanded }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search activity...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                singleLine = true,
                textStyle = TextStyle(fontSize = 12.sp)
            )
            Divider()
            
            filteredList.forEach { (category, list) ->
                DropdownMenuItem(
                    text = { Text(category, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp) },
                    onClick = {},
                    enabled = false
                )
                list.forEach { item ->
                    DropdownMenuItem(
                        text = { Text("  • $item", fontSize = 11.sp, color = Color.Black) },
                        onClick = {
                            onValueChange(item)
                            searchQuery = ""
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GridInCellTradeDropdownField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Civil", "Structure", "Architecture", "MEP", "Infrastructure")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clickable { expanded = !expanded }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 9.sp),
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
        Box(modifier = Modifier.weight(2.5f)) {
            Text(
                text = value.ifEmpty { "Select..." },
                style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 9.5.sp, color = if (value.isEmpty()) Color.Gray else Color.Black)
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(180.dp)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, fontSize = 11.sp) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GridInCellActivityDropdownField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val allCategoriesAndActivities = remember { getActivityCategories() }
    
    val filteredList = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            allCategoriesAndActivities
        } else {
            val result = LinkedHashMap<String, List<String>>()
            allCategoriesAndActivities.forEach { (category, list) ->
                val matching = list.filter { it.contains(searchQuery, ignoreCase = true) }
                if (category.contains(searchQuery, ignoreCase = true) || matching.isNotEmpty()) {
                    result[category] = matching.ifEmpty { list }
                }
            }
            result
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clickable { expanded = !expanded }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 9.sp),
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
        Box(modifier = Modifier.weight(2.5f)) {
            Text(
                text = value.ifEmpty { "Select..." },
                style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 9.5.sp, color = if (value.isEmpty()) Color.Gray else Color.Black)
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(280.dp).heightIn(max = 350.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search...", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 11.sp)
                )
                Divider()
                filteredList.forEach { (category, list) ->
                    DropdownMenuItem(
                        text = { Text(category, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp) },
                        onClick = {},
                        enabled = false
                    )
                    list.forEach { item ->
                        DropdownMenuItem(
                            text = { Text("  • $item", fontSize = 10.sp, color = Color.Black) },
                            onClick = {
                                onValueChange(item)
                                searchQuery = ""
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectTypeDropdownTextField(
    value: String,
    onValueChange: (String) -> Unit,
    labelString: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "Residential",
        "Commercial",
        "Hospital",
        "Hotel",
        "Educational",
        "Infrastructure",
        "Shopping Mall",
        "Industrial",
        "High rise buildings - Towers",
        "Landscape",
        "Administrative Building"
    )
    
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(labelString) },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Select Project Type"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = !expanded }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SummaryBox(label: String, valStr: String, tint: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .width(46.dp)
    ) {
        Text(text = valStr, fontWeight = FontWeight.Bold, color = tint, fontSize = 11.sp)
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 6.sp, textAlign = TextAlign.Center)
    }
}

// 2. DETAILED FINDING EDITING COMPONENT TABLE AND DIALOG
@Composable
fun MajorFindingsUnifiedTable(
    findings: List<Finding>,
    viewModel: AuditViewModel,
    lang: String
) {
    var editingFindingIndex by remember { mutableStateOf<Int?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black)
            .background(Color.White)
    ) {
        // Table Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
                .height(IntrinsicSize.Min)
        ) {
            TableCell(text = "Sr.", isLabel = true, modifier = Modifier.weight(0.25f), isCentered = true)
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCell(text = "Finding Description", isLabel = true, modifier = Modifier.weight(1.8f), isCentered = true)
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCell(text = if (lang == "ar") "الأثر السلبي على جودة المشروع" else "Negative Impact on the quality of the project for the same item and/or other elements.", isLabel = true, modifier = Modifier.weight(1.5f), isCentered = true)
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            TableCell(text = if (lang == "ar") "الخسائر المادية والعمالية" else "Losses in material and manpower.", isLabel = true, modifier = Modifier.weight(1.5f), isCentered = true)
        }
        
        Divider(color = Color.Black, thickness = 1.dp)
        
        // Findings Rows
        findings.forEachIndexed { index, finding ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clickable { editingFindingIndex = index }
            ) {
                // Column 1: Sr.
                TableCell(text = (index + 1).toString(), isLabel = false, modifier = Modifier.weight(0.25f), isCentered = true, isBoldValue = true)
                Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                
                // Column 2: Finding Description
                Box(
                    modifier = Modifier
                        .weight(1.8f)
                        .fillMaxHeight()
                        .padding(6.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Column {
                        Text(
                            text = finding.description,
                            fontSize = 9.5.sp,
                            color = Color.Black,
                            lineHeight = 12.sp
                        )
                        if (finding.referenceId.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = finding.referenceId,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline,
                                color = Color.Black
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                
                // Column 3: Negative Impact
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                        .padding(6.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Text(
                        text = finding.negativeImpact,
                        fontSize = 9.5.sp,
                        color = Color.Black,
                        lineHeight = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
                
                // Column 4: Losses
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                        .padding(6.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Text(
                        text = finding.materialLosses,
                        fontSize = 9.5.sp,
                        color = Color.Black,
                        lineHeight = 12.sp
                    )
                }
            }
            
            if (index < findings.size - 1) {
                Divider(color = Color.Black, thickness = 1.dp)
            }
        }
    }
    
    // Edit Dialog Overlay
    editingFindingIndex?.let { index ->
        if (index in findings.indices) {
            FindingEditDialog(
                index = index,
                finding = findings[index],
                viewModel = viewModel,
                lang = lang,
                onDismiss = { editingFindingIndex = null }
            )
        } else {
            editingFindingIndex = null
        }
    }
}

@Composable
fun FindingEditDialog(
    index: Int,
    finding: Finding,
    viewModel: AuditViewModel,
    lang: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    fun getString(key: String): String = Bilingual.get(key, lang)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (lang == "ar") "تعديل بيانات المخالفة #${index + 1}" else "Edit Finding Details #${index + 1}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = finding.referenceId,
                    onValueChange = { value -> viewModel.updateFinding(index) { f -> f.copy(referenceId = value) } },
                    label = { Text("Reference ID / Code (e.g. FU-NCR-0001)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // NCR / OBS select row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Type", style = MaterialTheme.typography.labelSmall)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { viewModel.updateFinding(index) { it.copy(type = "NCR") } },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (finding.type == "NCR") Color(0xFFFCE8E6) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (finding.type == "NCR") Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("NCR", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = { viewModel.updateFinding(index) { it.copy(type = "OBS") } },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (finding.type == "OBS") Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (finding.type == "OBS") Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("OBS", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Severity", style = MaterialTheme.typography.labelSmall)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { viewModel.updateFinding(index) { it.copy(severity = "Major") } },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (finding.severity == "Major") Color(0xFFFCE8E6) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (finding.severity == "Major") Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Major", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = { viewModel.updateFinding(index) { it.copy(severity = "Minor") } },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (finding.severity == "Minor") Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (finding.severity == "Minor") Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Minor", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Status", style = MaterialTheme.typography.labelSmall)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { viewModel.updateFinding(index) { it.copy(status = "Open") } },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (finding.status == "Open") Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (finding.status == "Open") Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Open", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = { viewModel.updateFinding(index) { it.copy(status = "Closed") } },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (finding.status == "Closed") Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (finding.status == "Closed") Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Closed", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                TradeDropdownTextField(
                    value = finding.trade,
                    onValueChange = { valStr -> viewModel.updateFinding(index) { it.copy(trade = valStr) } },
                    labelString = "Trade",
                    modifier = Modifier.fillMaxWidth()
                )
                ActivityDropdownTextField(
                    value = finding.activity,
                    onValueChange = { valStr -> viewModel.updateFinding(index) { it.copy(activity = valStr) } },
                    labelString = "Activity",
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = finding.locationZone,
                    onValueChange = { valStr -> viewModel.updateFinding(index) { it.copy(locationZone = valStr) } },
                    label = { Text("Location / Zone") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = finding.description,
                    onValueChange = { valStr -> viewModel.updateFinding(index) { it.copy(description = valStr) } },
                    label = { Text(if (lang == "ar") "وصف المخالفة المرصودة (Finding Description)" else "Finding Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                OutlinedTextField(
                    value = finding.negativeImpact,
                    onValueChange = { valStr -> viewModel.updateFinding(index) { it.copy(negativeImpact = valStr) } },
                    label = { Text(if (lang == "ar") "الأثر السلبي على جودة المشروع" else "Negative Impact on the quality of the project") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                OutlinedTextField(
                    value = finding.materialLosses,
                    onValueChange = { valStr -> viewModel.updateFinding(index) { it.copy(materialLosses = valStr) } },
                    label = { Text(if (lang == "ar") "الخسائر المادية والعمالية" else "Losses in material and manpower") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                OutlinedTextField(
                    value = finding.rootCause,
                    onValueChange = { valStr -> viewModel.updateFinding(index) { it.copy(rootCause = valStr) } },
                    label = { Text("Root Cause") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = finding.correctiveAction,
                    onValueChange = { valStr -> viewModel.updateFinding(index) { it.copy(correctiveAction = valStr) } },
                    label = { Text("Corrective Action required") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatePickerTextField(
                        value = finding.issueDate,
                        onValueChange = { valStr -> viewModel.updateFinding(index) { it.copy(issueDate = valStr) } },
                        labelString = "Issue date",
                        modifier = Modifier.weight(1f)
                    )
                    DatePickerTextField(
                        value = finding.dueDate,
                        onValueChange = { valStr -> viewModel.updateFinding(index) { it.copy(dueDate = valStr) } },
                        labelString = "Due date",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Photos attached (tap slot to attach image)", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PhotoSlotBox(
                            title = "Photo 1",
                            base64Str = finding.ph1Base64,
                            onPhotoSelected = { base -> viewModel.updateFinding(index) { it.copy(ph1Base64 = base) } },
                            modifier = Modifier.weight(1f).height(120.dp),
                            lang = lang,
                            onClear = { viewModel.updateFinding(index) { f -> f.copy(ph1Base64 = null) } }
                        )
                        PhotoSlotBox(
                            title = "Photo 2",
                            base64Str = finding.ph2Base64,
                            onPhotoSelected = { base -> viewModel.updateFinding(index) { it.copy(ph2Base64 = base) } },
                            modifier = Modifier.weight(1f).height(120.dp),
                            lang = lang,
                            onClear = { viewModel.updateFinding(index) { f -> f.copy(ph2Base64 = null) } }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PhotoSlotBox(
                            title = "Photo 3",
                            base64Str = finding.ph3Base64,
                            onPhotoSelected = { base -> viewModel.updateFinding(index) { f -> f.copy(ph3Base64 = base) } },
                            modifier = Modifier.weight(1f).height(120.dp),
                            lang = lang,
                            onClear = { viewModel.updateFinding(index) { f -> f.copy(ph3Base64 = null) } }
                        )
                        PhotoSlotBox(
                            title = "Photo 4",
                            base64Str = finding.ph4Base64,
                            onPhotoSelected = { base -> viewModel.updateFinding(index) { f -> f.copy(ph4Base64 = base) } },
                            modifier = Modifier.weight(1f).height(120.dp),
                            lang = lang,
                            onClear = { viewModel.updateFinding(index) { f -> f.copy(ph4Base64 = null) } }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(if (lang == "ar") "موافق" else "Done")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.removeFinding(index)
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (lang == "ar") "حذف المخالفة" else "Delete Finding")
            }
        }
    )
}

@Composable
fun PhotoSlotBox(
    title: String,
    base64Str: String?,
    onPhotoSelected: (String) -> Unit,
    modifier: Modifier = Modifier.size(width = 80.dp, height = 75.dp),
    lang: String = "en",
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var tempCameraUriStr by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val tempCameraUri = tempCameraUriStr?.let { android.net.Uri.parse(it) }
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                coroutineScope.launch(Dispatchers.IO) {
                    val base = saveUriToInternalStorage(context, uri)
                    if (base.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            onPhotoSelected(base)
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
            tempCameraUriStr?.let { uriStr ->
                try {
                    cameraLauncher.launch(android.net.Uri.parse(uriStr))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            Toast.makeText(context, if (lang == "ar") "إذن الكاميرا مطلوب لالتقاط الصور" else "Camera permission is required to capture photos.", Toast.LENGTH_SHORT).show()
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val base = saveUriToInternalStorage(context, uri)
                withContext(Dispatchers.Main) {
                    onPhotoSelected(base)
                }
            }
        }
    }
    
    var showChooser by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(8.dp))
            .clickable { showChooser = true },
        contentAlignment = Alignment.Center
    ) {
        if (base64Str != null) {
            var bmp by remember(base64Str) { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(base64Str) {
                try {
                    val decoded = withContext(Dispatchers.IO) {
                        loadImageStringToBitmap(context, base64Str, 200)
                    }
                    bmp = decoded
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (bmp != null) {
                val currentBmp = bmp
                if (currentBmp != null) {
                    Image(
                        bitmap = currentBmp.asImageBitmap(),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                // Clear button
                IconButton(
                    onClick = { onClear() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .padding(2.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f), shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "clear", tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(12.dp))
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "camera",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    if (showChooser) {
        AlertDialog(
            onDismissRequest = { showChooser = false },
            title = {
                Text(
                    text = if (lang == "ar") "مصدر الصورة ($title)" else "Select Image Source ($title)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
            },
            text = {
                Text(
                    text = if (lang == "ar") "يرجى اختيار فتح الكاميرا لالتقاط صورة أو المعرض لاختيار صورة وجودة." else "Please choose whether to open the Camera to take a photo, or select from your Gallery.",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = {
                        try {
                            galleryLauncher.launch("image/*")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        showChooser = false
                    }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (lang == "ar") "المعرض" else "Gallery")
                    }
                    Button(onClick = {
                        try {
                            val tempFile = java.io.File.createTempFile("photo_capture_", ".jpg", context.cacheDir).apply {
                                deleteOnExit()
                            }
                            val authority = "${context.packageName}.fileprovider"
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, tempFile)
                            tempCameraUriStr = uri.toString()
                            
                            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.CAMERA
                            )
                            if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        showChooser = false
                    }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (lang == "ar") "الكاميرا" else "Camera")
                    }
                }
            }
        )
    }
}

// 2. PREVIOUS AUDITS HISTORIC TAB
@Composable
fun HistoryTab(viewModel: AuditViewModel, lang: String) {
    val activeDetails by viewModel.activeReport.collectAsStateWithLifecycle()
    val historyRows = activeDetails?.historyRows ?: emptyList()

    fun getString(key: String): String = Bilingual.get(key, lang)

    var numInput by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf("") }
    var ncrTotalInput by remember { mutableStateOf("0") }
    var ncrClosedInput by remember { mutableStateOf("0") }
    var obsTotalInput by remember { mutableStateOf("0") }
    var obsClosedInput by remember { mutableStateOf("0") }
    var auditorInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Add Historic Audit Record",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = numInput,
                        onValueChange = { numInput = it },
                        label = { Text("Audit Number #") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ncrTotalInput,
                            onValueChange = { ncrTotalInput = it },
                            label = { Text(getString("prev_ncr_t")) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ncrClosedInput,
                            onValueChange = { ncrClosedInput = it },
                            label = { Text(getString("prev_ncr_c")) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = obsTotalInput,
                            onValueChange = { obsTotalInput = it },
                            label = { Text(getString("prev_obs_t")) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = obsClosedInput,
                            onValueChange = { obsClosedInput = it },
                            label = { Text(getString("prev_obs_c")) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = auditorInput,
                        onValueChange = { auditorInput = it },
                        label = { Text("Auditor Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.addHistoryRow(
                                auditNum = numInput.ifEmpty { "100" },
                                date = dateInput.ifEmpty { "2026-01-01" },
                                ncrT = ncrTotalInput.toIntOrNull() ?: 0,
                                ncrC = ncrClosedInput.toIntOrNull() ?: 0,
                                obsT = obsTotalInput.toIntOrNull() ?: 0,
                                obsC = obsClosedInput.toIntOrNull() ?: 0,
                                auditor = auditorInput.ifEmpty { "Auditor" }
                            )
                            // Clear inputs
                            numInput = ""
                            dateInput = ""
                            ncrTotalInput = "0"
                            ncrClosedInput = "0"
                            obsTotalInput = "0"
                            obsClosedInput = "0"
                            auditorInput = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add Record to History list")
                    }
                }
            }
        }

        item {
            Text(
                text = "Previous Audits Status List",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        itemsIndexed(historyRows) { hIndex, record ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Audit #${record.auditNumber}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(text = "Date: ${record.auditDate}", style = MaterialTheme.typography.bodySmall)

                        Spacer(modifier = Modifier.height(6.dp))

                        // Progress closure indicators
                        val ncrClosedPct = if (record.ncrsIssued > 0) {
                            (record.ncrsClosed.toFloat() / record.ncrsIssued.toFloat() * 100f).toInt()
                        } else {
                            100
                        }
                        val obsClosedPct = if (record.obsIssued > 0) {
                            (record.obsClosed.toFloat() / record.obsIssued.toFloat() * 100f).toInt()
                        } else {
                            100
                        }

                        Text(
                            text = "NCRs Closed: ${record.ncrsClosed}/${record.ncrsIssued} ($ncrClosedPct%)",
                            style = MaterialTheme.typography.labelSmall
                        )
                        LinearProgressIndicator(
                            progress = { ncrClosedPct / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = Color(0xFF2E7D32)
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "OBS Closed: ${record.obsClosed}/${record.obsIssued} ($obsClosedPct%)",
                            style = MaterialTheme.typography.labelSmall
                        )
                        LinearProgressIndicator(
                            progress = { obsClosedPct / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = Color(0xFFEF6C00)
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Auditor: ${record.auditorName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }

                    IconButton(onClick = { viewModel.removeHistoryRow(hIndex) }) {
                        Icon(Icons.Default.Delete, contentDescription = "delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// 3. AUDITEE RESPONSE RESPOND TAB
@Composable
fun AuditeeResponseTab(viewModel: AuditViewModel, lang: String) {
    val context = LocalContext.current
    val activeDetailsState by viewModel.activeReport.collectAsStateWithLifecycle()
    val activeDetails = activeDetailsState
    if (activeDetails == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val report = activeDetails.report
    val findings = activeDetails.findings

    fun getString(key: String): String = Bilingual.get(key, lang)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Notice Warning header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Auditee Response Section",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "This area should be filled response to each finding. Save after filling fields.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Auditee Identification Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Icon(Icons.Default.AccountBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            getString("auditee_identification"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = report.auditeeName,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeeName = valStr) } },
                        label = { Text(getString("auditee_name")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = report.auditeeDesignation,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeeDesignation = valStr) } },
                        label = { Text(getString("auditee_desig")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = report.auditeeCompany,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeeCompany = valStr) } },
                        label = { Text(getString("auditee_company")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    DatePickerTextField(
                        value = report.auditeeResponseDate,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeeResponseDate = valStr) } },
                        labelString = getString("response_date"),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = report.auditeePhone,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeePhone = valStr) } },
                        label = { Text(getString("phone")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = report.auditeeEmail,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeeEmail = valStr) } },
                        label = { Text(getString("email")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Syced responses cards loop
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Responses per Finding Checklist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { viewModel.syncAuditeeFindingsFromAuditor() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync Findings", fontSize = 9.sp)
                }
            }
        }

        if (findings.isEmpty()) {
            item {
                Text(
                    "No findings added yet. Please use the Form tab first.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }

        itemsIndexed(findings) { idx, item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text((idx + 1).toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.referenceId.ifEmpty { "Finding" },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Description: ${item.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = item.auditeeResponse,
                        onValueChange = { valStr -> viewModel.updateFinding(idx) { it.copy(auditeeResponse = valStr) } },
                        label = { Text("Response / Acknowledgement") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = item.auditeeRca,
                        onValueChange = { valStr -> viewModel.updateFinding(idx) { it.copy(auditeeRca = valStr) } },
                        label = { Text("Root cause (Auditee view)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = item.auditeeCorrectiveAction,
                        onValueChange = { valStr -> viewModel.updateFinding(idx) { it.copy(auditeeCorrectiveAction = valStr) } },
                        label = { Text("Corrective action planned") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = item.auditeeResponsiblePerson,
                        onValueChange = { valStr -> viewModel.updateFinding(idx) { it.copy(auditeeResponsiblePerson = valStr) } },
                        label = { Text("Responsible person") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    DatePickerTextField(
                        value = item.auditeeTargetDate,
                        onValueChange = { valStr -> viewModel.updateFinding(idx) { it.copy(auditeeTargetDate = valStr) } },
                        labelString = "Target closure date (YYYY-MM-DD)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = item.auditeeEvidence,
                        onValueChange = { valStr -> viewModel.updateFinding(idx) { it.copy(auditeeEvidence = valStr) } },
                        label = { Text("Evidence / proof of closure") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Evidence Closure photo (tap slot)", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    PhotoSlotBox(
                        title = "Closure proof",
                        base64Str = item.auditeeClosurePhoto,
                        onPhotoSelected = { base -> viewModel.updateFinding(idx) { it.copy(auditeeClosurePhoto = base) } },
                        lang = lang,
                        onClear = { viewModel.updateFinding(idx) { it.copy(auditeeClosurePhoto = null) } }
                    )
                }
            }
        }

        // Commitments remarks
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = getString("general_remarks"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = report.auditeeRemarks,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeeRemarks = valStr) } },
                        label = { Text("Overall general remarks") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    DatePickerTextField(
                        value = report.auditeeProposedClosureDate,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeeProposedClosureDate = valStr) } },
                        labelString = getString("proposed_closure_date"),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = report.auditeeOverallStatus,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeeOverallStatus = valStr) } },
                        label = { Text(getString("overall_status")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Preventive measures
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            getString("preventive_measures"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = report.auditeePreventiveActions,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeePreventiveActions = valStr) } },
                        label = { Text(getString("preventive_actions")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = report.auditeeTrainingActions,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeeTrainingActions = valStr) } },
                        label = { Text(getString("training_actions")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = report.auditeeProcedureChanges,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeeProcedureChanges = valStr) } },
                        label = { Text(getString("procedure_changes")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Rectification overall photos Grid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        getString("rectification_photos"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PhotoSlotBox(
                                title = "Photo 1",
                                base64Str = report.auditeePhRef1,
                                onPhotoSelected = { base -> viewModel.updateReport { it.copy(auditeePhRef1 = base) } },
                                modifier = Modifier.weight(1f).height(140.dp),
                                lang = lang,
                                onClear = { viewModel.updateReport { it.copy(auditeePhRef1 = null) } }
                            )
                            PhotoSlotBox(
                                title = "Photo 2",
                                base64Str = report.auditeePhRef2,
                                onPhotoSelected = { base -> viewModel.updateReport { it.copy(auditeePhRef2 = base) } },
                                modifier = Modifier.weight(1f).height(140.dp),
                                lang = lang,
                                onClear = { viewModel.updateReport { it.copy(auditeePhRef2 = null) } }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PhotoSlotBox(
                                title = "Photo 3",
                                base64Str = report.auditeePhRef3,
                                onPhotoSelected = { base -> viewModel.updateReport { it.copy(auditeePhRef3 = base) } },
                                modifier = Modifier.weight(1f).height(140.dp),
                                lang = lang,
                                onClear = { viewModel.updateReport { it.copy(auditeePhRef3 = null) } }
                            )
                            PhotoSlotBox(
                                title = "Photo 4",
                                base64Str = report.auditeePhRef4,
                                onPhotoSelected = { base -> viewModel.updateReport { it.copy(auditeePhRef4 = base) } },
                                modifier = Modifier.weight(1f).height(140.dp),
                                lang = lang,
                                onClear = { viewModel.updateReport { it.copy(auditeePhRef4 = null) } }
                            )
                        }
                    }
                }
            }
        }

        // Supporting reference documents
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            getString("supporting_docs"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = report.auditeeDocs,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeeDocs = valStr) } },
                        label = { Text("Referenced MS / ITP materials submittals") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = report.auditeeRefs,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeeRefs = valStr) } },
                        label = { Text("NCR / Work permit references") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Auditee signature block
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = getString("auditee_signatures"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(getString("auditee_rep"), fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = report.auditeeSigName,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeeSigName = valStr) } },
                        label = { Text(getString("name")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = report.auditeeSigDesignation,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeeSigDesignation = valStr) } },
                        label = { Text(getString("designation")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    DatePickerTextField(
                        value = report.auditeeSigDate,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeeSigDate = valStr) } },
                        labelString = getString("date"),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(if (lang == "ar") "صورة التوقيع لمهندس الموقع:" else "Representative Signature Photo:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    PhotoSlotBox(
                        title = getString("auditee_rep"),
                        base64Str = report.auditeeSigPh,
                        onPhotoSelected = { base -> viewModel.updateReport { it.copy(auditeeSigPh = base) } },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        lang = lang,
                        onClear = { viewModel.updateReport { it.copy(auditeeSigPh = null) } }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(getString("auditee_sup"), fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = report.auditeeSupName,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeeSupName = valStr) } },
                        label = { Text(getString("name")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = report.auditeeSupDesignation,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeeSupDesignation = valStr) } },
                        label = { Text(getString("designation")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    DatePickerTextField(
                        value = report.auditeeSupDate,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(auditeeSupDate = valStr) } },
                        labelString = getString("date"),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(if (lang == "ar") "صورة التوقيع للمشرف العام:" else "Supervisor Signature Photo:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    PhotoSlotBox(
                        title = getString("auditee_sup"),
                        base64Str = report.auditeeSupPh,
                        onPhotoSelected = { base -> viewModel.updateReport { it.copy(auditeeSupPh = base) } },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        lang = lang,
                        onClear = { viewModel.updateReport { it.copy(auditeeSupPh = null) } }
                    )
                }
            }
        }

        // QC Auditor Acceptance Review
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        getString("qc_reviewer_acceptance"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = report.reviewerName,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(reviewerName = valStr) } },
                        label = { Text("Reviewed by (Auditor)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = report.reviewerStatus,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(reviewerStatus = valStr) } },
                        label = { Text(getString("overall_status")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = report.reviewerRemarks,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(reviewerRemarks = valStr) } },
                        label = { Text("Auditor remarks / requirements") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    DatePickerTextField(
                        value = report.reviewerDate,
                        onValueChange = { valStr -> viewModel.updateReport { it.copy(reviewerDate = valStr) } },
                        labelString = getString("date"),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// 4. EXPORT AND PREVIEW TAB SCREEN
@Composable
fun ExportTab(viewModel: AuditViewModel, lang: String) {
    val context = LocalContext.current
    val activeDetails by viewModel.activeReport.collectAsStateWithLifecycle()
    val report = activeDetails?.report ?: return
    val findings = activeDetails?.findings ?: emptyList()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()

    fun getString(key: String): String = Bilingual.get(key, lang)

    var previewText by remember { mutableStateOf("") }

    // Helper functions to generate raw text summary
    fun refreshTextSummary() {
        val ncr = findings.count { it.type == "NCR" }
        val obs = findings.count { it.type == "OBS" }
        val open = findings.count { it.status == "Open" }

        val sb = java.lang.StringBuilder()
        sb.append("QC INTERNAL AUDIT REPORT REPORT SUMMARY\n")
        sb.append("=========================================\n")
        sb.append("Report Ref ID: #${report.auditNumber}\n")
        sb.append("Project Name: ${report.projectName}\n")
        sb.append("Location: ${report.location}\n")
        sb.append("Audit Date: ${report.auditDate}\n")
        sb.append("Auditor Name: ${report.auditorName}\n")
        sb.append("Reference: ${report.formReference}\n")
        sb.append("=========================================\n")
        sb.append("Aggregates: NCRs: $ncr | Observations: $obs | Status Open: $open\n\n")

        findings.forEachIndexed { idx, finding ->
            sb.append("${idx + 1}. [${finding.type} / ${finding.severity} / ${finding.status}] ${finding.referenceId}\n")
            sb.append("   Trade: ${finding.trade} | Activity: ${finding.activity}\n")
            sb.append("   Zone: ${finding.locationZone}\n")
            sb.append("   Issue: ${finding.description}\n")
            sb.append("   Root: ${finding.rootCause}\n")
            sb.append("   Action: ${finding.correctiveAction}\n\n")
        }
        previewText = sb.toString()
    }

    LaunchedEffect(activeDetails) {
        refreshTextSummary()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = getString("export_report"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (lang == "ar") "يمكنك تنزيل التقرير محلياً أو مشاركته مباشرةً مع الآخرين (عبر الواتساب، البلوتوث، الإيميل وغيرها)." else "You can download the report locally or share it directly with others (via WhatsApp, Bluetooth, Email, etc.).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (lang == "ar") "1. الحفظ على الجهاز (تنزيلات)" else "1. Save Locally (Downloads)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.exportPdf(context) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(getString("export_pdf"), fontSize = 9.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { viewModel.exportDoc(context) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D253F)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(getString("export_docx"), fontSize = 9.sp, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.exportPptx(context) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD35400)), // PowerPoint Orange
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Slideshow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(getString("export_pptx"), fontSize = 9.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { viewModel.exportXlsx(context) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)), // Excel Green
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(getString("export_xlsx"), fontSize = 9.sp, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.exportZip(context) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)), // Teal Accent
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (lang == "ar") "تصدير ملف ZIP بالصور" else "Export ZIP Archive (with Photos)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (lang == "ar") "2. المشاركة المباشرة (واتساب، بلوتوث...)" else "2. Direct Share (WhatsApp, Bluetooth...)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.sharePdf(context) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(getString("share_pdf"), fontSize = 9.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { viewModel.shareDoc(context) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(getString("share_docx"), fontSize = 9.sp, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.sharePptx(context) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE67E22)), // PowerPoint Orange
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(getString("share_pptx"), fontSize = 9.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { viewModel.shareXlsx(context) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), // Excel Green Share
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(getString("share_xlsx"), fontSize = 9.sp, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.shareZip(context) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)), // Teal Accent
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (lang == "ar") "مشاركة ملف ZIP بالصور" else "Share ZIP Archive (with Photos)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            getString("preview"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { refreshTextSummary() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(4.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(4.dp))
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = previewText.ifEmpty { "Refresh to display textual review summary." },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }

    if (isExporting) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {}
        ) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = if (lang == "ar") "جاري إنشاء المستند بالدقة الكاملة... يرجى الانتظار." else "Generating high-resolution document... Please wait.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ------------------- SUMMARY AND TRACKING SHEET TAB -------------------

data class SummaryRow(
    val trade: String,
    val enTrade: String,
    val ncrOpen: Int,
    val ncrClosed: Int,
    val ncrCumulative: Int,
    val obsOpen: Int,
    val obsClosed: Int,
    val obsCumulative: Int
)

@Composable
fun RowScope.SummaryTableCell(
    text: String,
    weight: Float,
    bg: Color,
    textColor: Color,
    isBold: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    alignLeft: Boolean = false
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .background(bg)
            .border(0.5.dp, Color(0xFFCCCCCC))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = if (alignLeft) Alignment.CenterStart else Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                fontSize = fontSize,
                color = textColor,
                textAlign = if (alignLeft) TextAlign.Start else TextAlign.Center
            )
        )
    }
}

@Composable
fun GroupedBarChart(
    title: String,
    seriesLabels: List<String>,
    seriesColors: List<Color>,
    categories: List<String>,
    values: List<List<Float>>,
    maxVal: Float = 8f
) {
    val dynamicMax = maxOf(maxVal, values.flatten().maxOrNull() ?: 1f)
    val scaleMax = if (dynamicMax <= 8f) 8f else (Math.ceil(dynamicMax / 4.0) * 4).toFloat()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .border(1.dp, Color(0xFFD0D0D0), RoundedCornerShape(4.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Y-Axis labels and grid lines
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(28.dp)
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    val steps = 4
                    for (i in steps downTo 0) {
                        val labelVal = (scaleMax * i / steps).toInt()
                        Text(
                            text = labelVal.toString(),
                            style = TextStyle(fontSize = 10.sp, color = Color.Gray)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(6.dp))
                
                // Chart area (Bars + grid lines)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Draw horizontal grid lines
                    Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 20.dp)) {
                        val steps = 4
                        val rowH = size.height / steps
                        for (i in 0..steps) {
                            val y = rowH * i
                            drawLine(
                                color = Color(0xFFE5E5E5),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                        }
                    }
                    
                    // Grouped bars
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        categories.forEachIndexed { catIdx, category ->
                            val catValues = values[catIdx]
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                catValues.forEachIndexed { seriesIdx, valF ->
                                    val barColor = seriesColors[seriesIdx]
                                    val pct = if (scaleMax > 0f) (valF / scaleMax).coerceIn(0f, 1f) else 0f
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .fillMaxHeight(pct)
                                            .width(11.dp)
                                            .background(barColor)
                                    )
                                }
                            }
                        }
                    }
                    
                    // X-Axis labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .height(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        categories.forEach { category ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = category,
                                    style = TextStyle(fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(10.dp))
                
                // Legend
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(bottom = 20.dp)
                        .width(115.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    seriesLabels.forEachIndexed { idx, label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .background(seriesColors[idx])
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = label,
                                style = TextStyle(fontSize = 9.sp, color = Color.Black)
                            )
                        }
                    }
                }
            }
            
            Text(
                text = "Period",
                style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Black),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun TrackingSummaryTab(viewModel: AuditViewModel, lang: String) {
    val activeDetailsState by viewModel.activeReport.collectAsStateWithLifecycle()
    val activeDetails = activeDetailsState
    if (activeDetails == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val findings = activeDetails.findings
    val report = activeDetails.report

    val isAr = lang == "ar"
    val context = LocalContext.current

    // Filter Trade State
    var selectedTrade by remember { mutableStateOf("All Trades") }
    var showTradeDropdown by remember { mutableStateOf(false) }

    fun getUnifiedTrade(trade: String): String {
        val trim = trade.trim()
        return when {
            trim.equals("Civil", ignoreCase = true) || trim.equals("Structure", ignoreCase = true) || trim.equals("Structural", ignoreCase = true) -> "Structure"
            trim.equals("Architecture", ignoreCase = true) || trim.equals("Architectural", ignoreCase = true) -> "Architectural"
            trim.equals("MEP", ignoreCase = true) || trim.equals("Electrical", ignoreCase = true) -> "Electrical"
            trim.equals("Mechanical", ignoreCase = true) -> "Mechanical"
            trim.equals("Infrastructure", ignoreCase = true) -> "Infrastructure"
            else -> "Structure"
        }
    }

    fun getLocalizedTrade(enTrade: String): String {
        return if (isAr) {
            when (enTrade) {
                "Structure" -> "إنشائي"
                "Architectural" -> "معماري"
                "Electrical" -> "كهرباء"
                "Mechanical" -> "ميكانيك"
                "Infrastructure" -> "بنية تحتية"
                "All Trades" -> "جميع التخصصات"
                else -> enTrade
            }
        } else {
            enTrade
        }
    }

    // Days late helper
    fun getDaysLate(dueDateStr: String): Int {
        val trimmed = dueDateStr.trim()
        if (trimmed.isEmpty()) return 0
        try {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
            val dueDate = sdf.parse(trimmed) ?: return 0
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse("2026-06-16") ?: return 0
            if (dueDate.before(today)) {
                val diffMs = today.time - dueDate.time
                return (diffMs / (1000 * 60 * 60 * 24)).toInt()
            }
        } catch (e: Exception) {
            // Try ISO format
            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val dueDate = sdf.parse(trimmed) ?: return 0
                val today = sdf.parse("2026-06-16") ?: return 0
                if (dueDate.before(today)) {
                    val diffMs = today.time - dueDate.time
                    return (diffMs / (1000 * 60 * 60 * 24)).toInt()
                }
            } catch (ex: Exception) { }
        }
        return 0
    }

    fun getMonthYearSymbol(dateStr: String): String {
        val cleaned = dateStr.trim()
        if (cleaned.isEmpty()) return ""
        val parts = cleaned.split('/', '-')
        if (parts.size >= 3) {
            val p0 = parts[0]
            val p1 = parts[1]
            val p2 = parts[2]
            val year = if (p2.length == 4) p2 else if (p0.length == 4) p0 else "2026"
            val monthPart = if (p2.length == 4) p1 else p1
            val mInt = monthPart.toIntOrNull() ?: 1
            val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val mStr = if (mInt in 1..12) months[mInt] else "Jun"
            return "$mStr $year"
        }
        return ""
    }

    // Filtered findings list
    val filteredFindings = if (selectedTrade == "All Trades") {
        findings
    } else {
        findings.filter { getUnifiedTrade(it.trade) == selectedTrade }
    }

    // Aggregates for dynamic cards
    val totalNcrs = filteredFindings.count { it.type == "NCR" }
    val openNcrs = filteredFindings.count { it.type == "NCR" && it.status == "Open" }
    val closedNcrs = filteredFindings.count { it.type == "NCR" && it.status == "Closed" }
    val ncrClosureRate = if (totalNcrs > 0) (closedNcrs.toFloat() / totalNcrs * 100f) else 0f

    val totalObs = filteredFindings.count { it.type == "OBS" }
    val openObs = filteredFindings.count { it.type == "OBS" && it.status == "Open" }
    val closedObs = filteredFindings.count { it.type == "OBS" && it.status == "Closed" }
    val obsClosureRate = if (totalObs > 0) (closedObs.toFloat() / totalObs * 100f) else 0f

    val overdueNcrs = filteredFindings.count { it.type == "NCR" && it.status == "Open" && getDaysLate(it.dueDate) > 0 }

    // Lists for Trade structures
    val standardTrades = listOf("Structure", "Architectural", "Electrical", "Mechanical", "Infrastructure")

    // Real-time table structures
    val ncrTradeRows = standardTrades.map { tr ->
        val matching = findings.filter { getUnifiedTrade(it.trade) == tr && it.type == "NCR" }
        val o = matching.count { it.status == "Open" }
        val c = matching.count { it.status == "Closed" }
        val tot = o + c
        val rate = if (tot > 0) (c.toFloat() / tot * 100f) else 0f
        TradeStats(getLocalizedTrade(tr), o, c, tot, rate)
    }

    val obsTradeRows = standardTrades.map { tr ->
        val matching = findings.filter { getUnifiedTrade(it.trade) == tr && it.type == "OBS" }
        val o = matching.count { it.status == "Open" }
        val c = matching.count { it.status == "Closed" }
        val tot = o + c
        val rate = if (tot > 0) (c.toFloat() / tot * 100f) else 0f
        TradeStats(getLocalizedTrade(tr), o, c, tot, rate)
    }

    // Monthly cumulative analytics calculation (last 6 months)
    val monthsList = listOf("Jan 2026", "Feb 2026", "Mar 2026", "Apr 2026", "May 2026", "Jun 2026")
    var previousNcrCount = findings.count { it.type == "NCR" && (getMonthYearSymbol(it.issueDate).endsWith("2025") || getMonthYearSymbol(it.issueDate).isEmpty()) }
    var previousObsCount = findings.count { it.type == "OBS" && (getMonthYearSymbol(it.issueDate).endsWith("2025") || getMonthYearSymbol(it.issueDate).isEmpty()) }

    val cumulativeRows = monthsList.map { m ->
        val nM = findings.count { it.type == "NCR" && getMonthYearSymbol(it.issueDate) == m }
        val oM = findings.count { it.type == "OBS" && getMonthYearSymbol(it.issueDate) == m }
        previousNcrCount += nM
        previousObsCount += oM
        CumulativeTrackerRow(m, nM, previousNcrCount, oM, previousObsCount)
    }

    // List of active overdue items
    val activeOverdueItems = findings.filter { it.type == "NCR" && it.status == "Open" && getDaysLate(it.dueDate) > 0 }
        .map { OverdueItem(it.referenceId, getLocalizedTrade(getUnifiedTrade(it.trade)), it.dueDate, getDaysLate(it.dueDate)) }
        .sortedByDescending { it.daysLate }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF071930)) // Premium deep midnight background
            .verticalScroll(rememberScrollState())
    ) {
        // --- 1. PREMIUM HEADER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A1F3D))
                .padding(vertical = 18.dp, horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAr) "متابعة وإحصاءات الأخطاء والملحوظات" else "NCR / OBS Analytics",
                        style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, color = Color.White)
                    )
                    Text(
                        text = if (isAr) "لوحة التحليل والتحكم والمتابعة" else "Dashboard & Analysis",
                        style = TextStyle(fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Normal)
                    )
                }
                
                // Last updated and Refresh state
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Last Updated:",
                        style = TextStyle(fontSize = 9.sp, color = Color(0xFF94A3B8))
                    )
                    val currentDateTimeStr = remember {
                        try {
                            java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.US).format(java.util.Date())
                        } catch (e: Exception) {
                            "11/06/2026 09:30 AM"
                        }
                    }
                    Text(
                        text = currentDateTimeStr,
                        style = TextStyle(fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // --- 1B. EXECUTIVE EXPORT SEGMENTED TABS ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2644)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAr) "تصدير لوحة الحسابات والتحليلات" else "EXPORT ANALYTICS DATA REPORT",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF38BDF8), letterSpacing = 0.5.sp)
                    )
                    Text(
                        text = if (isAr) "خيارات تصدير التقرير" else "Report Export Tabs",
                        style = TextStyle(fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // PDF Option Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(55.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2E1520))
                            .clickable { viewModel.exportPdf(context, selectedTrade = selectedTrade) }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "PDF",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isAr) "تصدير PDF" else "Export PDF",
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFFFCA5A5))
                            )
                        }
                    }

                    // Word Option Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(55.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF132D50))
                            .clickable { viewModel.exportDoc(context) }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = "DOCX",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isAr) "تصدير WORD" else "Export WORD",
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFFBFDBFE))
                            )
                        }
                    }

                    // PowerPoint Option Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(55.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF3B2516))
                            .clickable { viewModel.exportPptx(context) }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Slideshow,
                                contentDescription = "PPTX",
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isAr) "تصدير PPTX" else "Export PPTX",
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFFFED7AA))
                            )
                        }
                    }

                    // ZIP Option Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(55.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF022E26)) // Dark Teal
                            .clickable { viewModel.exportZip(context) }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Archive,
                                contentDescription = "ZIP",
                                tint = Color(0xFF00BFA5),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isAr) "تصدير ZIP" else "Export ZIP",
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFFA7F3D0))
                            )
                        }
                    }
                }
            }
        }

        // --- 2. INTERACTIVE FILTERS ROW ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2644)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter Trade Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { showTradeDropdown = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Trade: ${getLocalizedTrade(selectedTrade)}",
                                fontSize = 11.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showTradeDropdown,
                            onDismissRequest = { showTradeDropdown = false },
                            modifier = Modifier.width(200.dp).background(Color(0xFF0F2644))
                        ) {
                            listOf("All Trades", "Structure", "Architectural", "Electrical", "Mechanical", "Infrastructure").forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(getLocalizedTrade(t), color = Color.White, fontSize = 12.sp) },
                                    onClick = {
                                        selectedTrade = t
                                        showTradeDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Reset Filters Button
                    Button(
                        onClick = { selectedTrade = "All Trades" },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF94A3B8)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(if (isAr) "إعادة تعيين" else "Reset Filters", fontSize = 11.sp, color = Color(0xFF071930))
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Active configuration text details
                val activeProjName = report.projectName.ifEmpty { if (isAr) "غير محدد" else "Not Specified" }
                val activeDate = report.auditDate.ifEmpty { if (isAr) "غير محدد" else "Not Specified" }
                Text(
                    text = "Project: $activeProjName  |  Date: $activeDate",
                    style = TextStyle(fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                )
            }
        }

        // --- 3. DYNAMIC KPI KEY METRIC CARD GRID ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // First Row: NCR Stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(
                    title = "Total NCRs",
                    value = totalNcrs.toString(),
                    icon = Icons.Default.Assignment,
                    iconBg = Color(0xFF2563EB).copy(alpha = 0.2f),
                    iconColor = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Open NCRs",
                    value = openNcrs.toString(),
                    icon = Icons.Default.FolderOpen,
                    iconBg = Color(0xFFDC2626).copy(alpha = 0.2f),
                    iconColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(
                    title = "Closed NCRs",
                    value = closedNcrs.toString(),
                    icon = Icons.Default.Check,
                    iconBg = Color(0xFF16A34A).copy(alpha = 0.2f),
                    iconColor = Color(0xFF22C55E),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Closure Rate (NCR)",
                    value = "${String.format(java.util.Locale.US, "%.1f", ncrClosureRate)}%",
                    icon = Icons.Default.PieChart,
                    iconBg = Color(0xFF0891B2).copy(alpha = 0.2f),
                    iconColor = Color(0xFF06B6D4),
                    modifier = Modifier.weight(1f),
                    radialProgressValue = ncrClosureRate
                )
            }

            // Second Row: OBS Stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(
                    title = "Total OBS",
                    value = totalObs.toString(),
                    icon = Icons.Default.Visibility,
                    iconBg = Color(0xFFFFB020).copy(alpha = 0.15f),
                    iconColor = Color(0xFFFFB020),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Open OBS",
                    value = openObs.toString(),
                    icon = Icons.Default.VisibilityOff,
                    iconBg = Color(0xFFF97316).copy(alpha = 0.2f),
                    iconColor = Color(0xFFFB923C),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(
                    title = "Closed OBS",
                    value = closedObs.toString(),
                    icon = Icons.Default.Verified,
                    iconBg = Color(0xFF10B981).copy(alpha = 0.2f),
                    iconColor = Color(0xFF34D399),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Overdue NCRs",
                    value = overdueNcrs.toString(),
                    icon = Icons.Default.Warning,
                    iconBg = Color(0xFF7F1D1D).copy(alpha = 0.3f),
                    iconColor = Color(0xFFFCA5A5),
                    modifier = Modifier.weight(1f),
                    isOverdueBadge = overdueNcrs > 0
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 4. STATUS DISTRIBUTION SHIELDS ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2644)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "NCR Status Distribution",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TrackingDonutChart(
                        totalCount = totalNcrs,
                        openCount = openNcrs,
                        closedCount = closedNcrs,
                        openColor = Color(0xFFEF4444),
                        closedColor = Color(0xFF22C55E),
                        openLabel = "Open NCRs",
                        closedLabel = "Closed NCRs"
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2644)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "OBS Status Distribution",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TrackingDonutChart(
                        totalCount = totalObs,
                        openCount = openObs,
                        closedCount = closedObs,
                        openColor = Color(0xFFF97316),
                        closedColor = Color(0xFF10B981),
                        openLabel = "Open OBS",
                        closedLabel = "Closed OBS"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 5. PROGRESS TRENDS AND GRAPHS ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dynamic Line Cumulative Trend
            TrendLineChart(
                months = monthsList,
                ncrCumulativeValues = cumulativeRows.map { it.ncrCumulative.toFloat() },
                obsCumulativeValues = cumulativeRows.map { it.obsCumulative.toFloat() }
            )

            // Bar grouped distribution by trade
            GroupedBarChart(
                title = "By Trade (Total Findings)",
                seriesLabels = listOf("NCR", "OBS"),
                seriesColors = listOf(Color(0xFF3B82F6), Color(0xFFFFB020)),
                categories = standardTrades.map { getLocalizedTrade(it) },
                values = standardTrades.map { tr ->
                    val ncrCount = findings.count { getUnifiedTrade(it.trade) == tr && it.type == "NCR" }
                    val obsCount = findings.count { getUnifiedTrade(it.trade) == tr && it.type == "OBS" }
                    listOf(ncrCount.toFloat(), obsCount.toFloat())
                }
            )

            // Pareto Analysis Chart
            ParetoRootCauseChart(findings = findings)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 6. REAL TIME DATA TABLES ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Table 1: NCR Analysis by Trade
            TableSectionCard(title = "NCR Analysis by Trade") {
                TableHeaderRow(cols = listOf("Trade", "Open", "Closed", "Total", "Closure %"))
                ncrTradeRows.forEach { row ->
                    TableDataRow(
                        cells = listOf(
                            row.tradeName,
                            row.open.toString(),
                            row.closed.toString(),
                            row.total.toString(),
                            "${String.format(java.util.Locale.US, "%.1f", row.closureRate)}%"
                        )
                    )
                }
                // Summary Total Row
                val sumOpen = ncrTradeRows.sumOf { it.open }
                val sumClosed = ncrTradeRows.sumOf { it.closed }
                val sumTotal = sumOpen + sumClosed
                val overallRate = if (sumTotal > 0) (sumClosed.toFloat() / sumTotal * 100f) else 0f
                TableHeaderRow(
                    cols = listOf(
                        "Total",
                        sumOpen.toString(),
                        sumClosed.toString(),
                        sumTotal.toString(),
                        "${String.format(java.util.Locale.US, "%.1f", overallRate)}%"
                    ),
                    isTotalRow = true
                )
            }

            // Table 2: OBS Analysis by Trade
            TableSectionCard(title = "OBS Analysis by Trade") {
                TableHeaderRow(cols = listOf("Trade", "Open", "Closed", "Total", "Closure %"))
                obsTradeRows.forEach { row ->
                    TableDataRow(
                        cells = listOf(
                            row.tradeName,
                            row.open.toString(),
                            row.closed.toString(),
                            row.total.toString(),
                            "${String.format(java.util.Locale.US, "%.1f", row.closureRate)}%"
                        )
                    )
                }
                // Summary Total Row
                val sumOpen = obsTradeRows.sumOf { it.open }
                val sumClosed = obsTradeRows.sumOf { it.closed }
                val sumTotal = sumOpen + sumClosed
                val overallRate = if (sumTotal > 0) (sumClosed.toFloat() / sumTotal * 100f) else 0f
                TableHeaderRow(
                    cols = listOf(
                        "Total",
                        sumOpen.toString(),
                        sumClosed.toString(),
                        sumTotal.toString(),
                        "${String.format(java.util.Locale.US, "%.1f", overallRate)}%"
                    ),
                    isTotalRow = true
                )
            }

            // Table 3: Cumulative Analysis Table
            TableSectionCard(title = "Cumulative Analysis (Monthly)") {
                TableHeaderRow(cols = listOf("Month", "NCR (Mon)", "NCR (Cum)", "OBS (Mon)", "OBS (Cum)"))
                cumulativeRows.forEach { r ->
                    TableDataRow(
                        cells = listOf(
                            r.month,
                            r.ncrMonthly.toString(),
                            r.ncrCumulative.toString(),
                            r.obsMonthly.toString(),
                            r.obsCumulative.toString()
                        )
                    )
                }
            }

            // Table 4: Overdue NCR Details logs
            TableSectionCard(title = "Overdue NCRs Details") {
                if (activeOverdueItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No Overdue NCR items found.",
                            style = TextStyle(fontSize = 11.5.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
                        )
                    }
                } else {
                    TableHeaderRow(cols = listOf("NCR No.", "Trade", "Due Date", "Days Late"))
                    activeOverdueItems.take(5).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.25.dp, Color(0xFF334155))
                                .background(Color(0xFF0F172A))
                                .padding(vertical = 10.dp, horizontal = 6.dp)
                        ) {
                            Text(
                                text = item.ncrNum,
                                modifier = Modifier.weight(1.3f),
                                style = TextStyle(fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontSize = 10.5.sp),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = item.trade,
                                modifier = Modifier.weight(1.2f),
                                style = TextStyle(color = Color.White, fontSize = 10.5.sp),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = item.dueDate,
                                modifier = Modifier.weight(1.2f),
                                style = TextStyle(color = Color.White, fontSize = 10.5.sp),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = item.daysLate.toString(),
                                modifier = Modifier.weight(1f),
                                style = TextStyle(fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontSize = 11.sp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- HELPER COMPOSABLES AND COMPONENT PIECES ---

data class TradeStats(
    val tradeName: String,
    val open: Int,
    val closed: Int,
    val total: Int,
    val closureRate: Float
)

data class CumulativeTrackerRow(
    val month: String,
    val ncrMonthly: Int,
    val ncrCumulative: Int,
    val obsMonthly: Int,
    val obsCumulative: Int
)

data class OverdueItem(
    val ncrNum: String,
    val trade: String,
    val dueDate: String,
    val daysLate: Int
)

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    radialProgressValue: Float? = null,
    isOverdueBadge: Boolean = false
) {
    Card(
        modifier = modifier
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2644)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TextStyle(fontSize = 10.5.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black, color = if (isOverdueBadge && value != "0") Color(0xFFF87171) else Color.White)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (radialProgressValue != null) {
                    Canvas(modifier = Modifier.size(28.dp)) {
                        drawArc(
                            color = iconColor.copy(alpha = 0.2f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = stroke(width = 3.dp.toPx())
                        )
                        drawArc(
                            color = iconColor,
                            startAngle = -90f,
                            sweepAngle = (radialProgressValue / 100f) * 360f,
                            useCenter = false,
                            style = stroke(width = 3.dp.toPx())
                        )
                    }
                } else {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun TrackingDonutChart(
    totalCount: Int,
    openCount: Int,
    closedCount: Int,
    openColor: Color,
    closedColor: Color,
    openLabel: String,
    closedLabel: String
) {
    val openPct = if (totalCount > 0) (openCount.toFloat() / totalCount * 100f) else 0f
    val closedPct = if (totalCount > 0) (closedCount.toFloat() / totalCount * 100f) else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 12.dp.toPx()
                val radius = (size.minDimension - strokeW) / 2f
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                var startAngle = -90f

                // Closed segment
                val closedSweep = (closedPct / 100f) * 360f
                if (closedSweep > 0f) {
                    drawArc(
                        color = closedColor,
                        startAngle = startAngle,
                        sweepAngle = closedSweep,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    startAngle += closedSweep
                }

                // Open segment
                val openSweep = (openPct / 100f) * 360f
                if (openSweep > 0f) {
                    drawArc(
                        color = openColor,
                        startAngle = startAngle,
                        sweepAngle = openSweep,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = totalCount.toString(),
                    style = TextStyle(fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                )
                Text(
                    text = "Total",
                    style = TextStyle(fontSize = 9.sp, color = Color(0xFF64748B))
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Open legend
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(openColor, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(text = openLabel, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White))
                    Text(text = "$openCount (${String.format(java.util.Locale.US, "%.1f", openPct)}%)", style = TextStyle(fontSize = 10.sp, color = Color(0xFF94A3B8)))
                }
            }
            // Closed legend
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(closedColor, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(text = closedLabel, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White))
                    Text(text = "$closedCount (${String.format(java.util.Locale.US, "%.1f", closedPct)}%)", style = TextStyle(fontSize = 10.sp, color = Color(0xFF94A3B8)))
                }
            }
        }
    }
}

@Composable
fun TrendLineChart(
    months: List<String>,
    ncrCumulativeValues: List<Float>,
    obsCumulativeValues: List<Float>
) {
    val dynamicMax = maxOf(
        (ncrCumulativeValues.maxOrNull() ?: 1f),
        (obsCumulativeValues.maxOrNull() ?: 1f),
        8f
    )
    val scaleMax = (Math.ceil(dynamicMax / 50.0) * 50).toFloat().coerceAtLeast(100f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2644))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = "Monthly Trend (Cumulative)",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                // Y-Axis labels and grid lines
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(bottom = 20.dp)
                        .width(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    val steps = 5
                    for (i in steps downTo 0) {
                        val labelVal = (scaleMax * i / steps).toInt()
                        Text(
                            text = labelVal.toString(),
                            style = TextStyle(fontSize = 9.sp, color = Color(0xFF94A3B8))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 20.dp)) {
                        val w = size.width
                        val h = size.height

                        val steps = 5
                        for (i in 0..steps) {
                            val y = h * i / steps
                            drawLine(
                                color = Color(0xFF1E293B),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                        }

                        val pointsCount = months.size
                        if (pointsCount > 1) {
                            val stepX = w / (pointsCount - 1)

                            // 1. NCR Line (Teal Accent)
                            val ncrPath = androidx.compose.ui.graphics.Path()
                            val ncrPoints = ncrCumulativeValues.mapIndexed { idx, valF ->
                                val pct = valF / scaleMax
                                Offset(idx * stepX, h - (pct * h))
                            }
                            ncrPath.moveTo(ncrPoints[0].x, ncrPoints[0].y)
                            for (i in 1 until ncrPoints.size) {
                                ncrPath.lineTo(ncrPoints[i].x, ncrPoints[i].y)
                            }
                            drawPath(
                                path = ncrPath,
                                color = Color(0xFF00BFA5),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                            )
                            ncrPoints.forEach { pt ->
                                drawCircle(color = Color(0xFF00BFA5), radius = 4.dp.toPx(), center = pt)
                                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
                            }

                            // 2. OBS Line (Orange Accent)
                            val obsPath = androidx.compose.ui.graphics.Path()
                            val obsPoints = obsCumulativeValues.mapIndexed { idx, valF ->
                                val pct = valF / scaleMax
                                Offset(idx * stepX, h - (pct * h))
                            }
                            obsPath.moveTo(obsPoints[0].x, obsPoints[0].y)
                            for (i in 1 until obsPoints.size) {
                                obsPath.lineTo(obsPoints[i].x, obsPoints[i].y)
                            }
                            drawPath(
                                path = obsPath,
                                color = Color(0xFFED7D31),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                            )
                            obsPoints.forEach { pt ->
                                drawCircle(color = Color(0xFFED7D31), radius = 4.dp.toPx(), center = pt)
                                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
                            }
                        }
                    }

                    // X-Axis labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .height(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        months.forEach { month ->
                            Text(
                                text = month.split(" ")[0], // Show abbreviation
                                style = TextStyle(fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF00BFA5), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("NCR (Cumulative)", fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
                Spacer(modifier = Modifier.width(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFED7D31), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("OBS (Cumulative)", fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
fun ParetoRootCauseChart(findings: List<Finding>) {
    val causes = listOf("Poor Supervision", "Improper Workmanship", "Missing Inspection", "Material Defect", "Others")
    val countsMap = causes.associateWith { cause ->
        findings.count { it.rootCause.trim().equals(cause, ignoreCase = true) }
    }.toMutableMap()

    // Add remaining unmatched to Others
    val totalMatched = countsMap.values.sum()
    val totalFindings = findings.size
    if (totalFindings > totalMatched) {
        countsMap["Others"] = (countsMap["Others"] ?: 0) + (totalFindings - totalMatched)
    }

    // Sort descending
    val sortedCauses = countsMap.entries.sortedByDescending { it.value }
    val totalSum = sortedCauses.map { it.value }.sum()

    // Calculate Pareto cumulative percentages
    var runningCount = 0
    val paretos = sortedCauses.map { entry ->
        runningCount += entry.value
        val pct = if (totalSum > 0) (runningCount.toFloat() / totalSum * 100) else 0f
        Pair(entry.key, pct)
    }

    val maxCount = (sortedCauses.firstOrNull()?.value ?: 5).coerceAtLeast(5)
    val scaleMaxCount = (Math.ceil(maxCount / 5.0) * 5).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2644))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = "Root Cause Analysis (Pareto)",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                // Left Y-Axis (Defect Count)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(bottom = 25.dp)
                        .width(22.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    val steps = 5
                    for (i in steps downTo 0) {
                        val label = (scaleMaxCount * i / steps)
                        Text(label.toString(), style = TextStyle(fontSize = 9.sp, color = Color(0xFF94A3B8)))
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Chart Canvas Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 25.dp)) {
                        val w = size.width
                        val h = size.height

                        val steps = 5
                        for (i in 0..steps) {
                            val y = h * i / steps
                            drawLine(
                                color = Color(0xFF1E293B),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                        }

                        val barCount = sortedCauses.size
                        if (barCount > 0) {
                            val colW = w / barCount
                            val barW = colW * 0.45f

                            // Bars
                            sortedCauses.forEachIndexed { idx, entry ->
                                val pct = entry.value.toFloat() / scaleMaxCount
                                val barH = pct * h
                                val x = idx * colW + (colW - barW) / 2f
                                drawRoundRect(
                                    color = Color(0xFF1E3A5F),
                                    topLeft = Offset(x, h - barH),
                                    size = Size(barW, barH),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }

                            // Pareto line overlay
                            val linePath = androidx.compose.ui.graphics.Path()
                            val pts = paretos.mapIndexed { idx, pair ->
                                val pct = pair.second / 100f
                                Offset(idx * colW + colW / 2f, h - (pct * h))
                            }
                            linePath.moveTo(pts[0].x, pts[0].y)
                            for (i in 1 until pts.size) {
                                linePath.lineTo(pts[i].x, pts[i].y)
                            }
                            drawPath(
                                path = linePath,
                                color = Color(0xFFED7D31),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                            )
                            pts.forEach { pt ->
                                drawCircle(color = Color(0xFFED7D31), radius = 3.dp.toPx(), center = pt)
                                drawCircle(color = Color.White, radius = 1.3.dp.toPx(), center = pt)
                            }
                        }
                    }

                    // X-Axis labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .height(25.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        sortedCauses.forEach { entry ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                val shortName = when (entry.key) {
                                    "Poor Supervision" -> "Superv."
                                    "Improper Workmanship" -> "Workman."
                                    "Missing Inspection" -> "Inspect."
                                    "Material Defect" -> "Mater."
                                    else -> entry.key
                                }
                                Text(
                                    text = shortName,
                                    style = TextStyle(fontSize = 7.5.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Right Y-Axis (% Cumulative)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(bottom = 25.dp)
                        .width(22.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start
                ) {
                    val steps = 5
                    for (i in steps downTo 0) {
                        val pct = 100 * i / steps
                        Text("$pct%", style = TextStyle(fontSize = 9.sp, color = Color(0xFF94A3B8)))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pareto Legend indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF1E3A5F), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Defects Count", fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
                Spacer(modifier = Modifier.width(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFED7D31), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("% Cumulative", fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
fun TableSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2644)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color.White),
                modifier = Modifier.padding(bottom = 10.dp)
            )
            content()
        }
    }
}

@Composable
fun TableHeaderRow(cols: List<String>, isTotalRow: Boolean = false) {
    val bg = if (isTotalRow) Color(0xFF1E293B) else Color(0xFF1E3A5F)
    val textStyle = TextStyle(
        fontWeight = FontWeight.Bold,
        color = if (isTotalRow) Color(0xFF38BDF8) else Color.White,
        fontSize = 10.sp
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .border(0.5.dp, Color(0xFF334155))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        cols.forEachIndexed { index, title ->
            // Let first columns draw wider
            val weight = if (index == 0) 1.5f else 1f
            Text(
                text = title,
                modifier = Modifier.weight(weight),
                style = textStyle,
                textAlign = if (index == 0) TextAlign.Start else TextAlign.Center,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TableDataRow(cells: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .border(0.25.dp, Color(0xFF334155))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        cells.forEachIndexed { index, cell ->
            val weight = if (index == 0) 1.5f else 1f
            Text(
                text = cell,
                modifier = Modifier.weight(weight),
                style = TextStyle(
                    color = if (index == 0) Color.White else Color(0xFFCBD5E1),
                    fontSize = 10.5.sp,
                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
                ),
                textAlign = if (index == 0) TextAlign.Start else TextAlign.Center,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

private fun stroke(width: Float) = androidx.compose.ui.graphics.drawscope.Stroke(
    width = width,
    cap = androidx.compose.ui.graphics.StrokeCap.Round
)

// ------------------- NEW TAB: DETAILED NCR-OBS FORM SHEET -------------------

@Composable
fun NcrObsFormTab(viewModel: AuditViewModel, lang: String) {
    val activeDetailsState by viewModel.activeReport.collectAsStateWithLifecycle()
    val activeDetails = activeDetailsState
    if (activeDetails == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val findings = activeDetails.findings
    val report = activeDetails.report
    val context = LocalContext.current

    // Keep track of which finding is currently selected for editing in this sheet
    var selectedIndex by remember { mutableStateOf(0) }

    // If findings list is empty, show a button to add first finding, or reset index to 0
    if (findings.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(
                    text = if (lang == "ar") "لا توجد أي تقارير NCR/OBS مضافة حالياً. انقر بالأسفل لإضافة أول تقرير." else "No NCR/OBS sheets exist in this report yet.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.addFinding() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (lang == "ar") "إضافة تقرير NCR/OBS جديد" else "Create New NCR/OBS")
                }
            }
        }
        return
    }

    val safeSelectedIndex = if (findings.isNotEmpty()) {
        selectedIndex.coerceIn(0, findings.size - 1)
    } else {
        0
    }

    if (selectedIndex != safeSelectedIndex) {
        selectedIndex = safeSelectedIndex
    }

    val finding = findings[safeSelectedIndex]

    val coroutineScope = rememberCoroutineScope()
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    var sequentialCameraBase64s by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSequentialCameraProgressDialog by remember { mutableStateOf(false) }

    var tempCameraUriStr by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val tempCameraUri = tempCameraUriStr?.let { android.net.Uri.parse(it) }
    var targetPhotoSlot by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                coroutineScope.launch(Dispatchers.IO) {
                    val base64 = saveUriToInternalStorage(context, uri)
                    if (base64.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            if (targetPhotoSlot == "sequential") {
                                sequentialCameraBase64s = sequentialCameraBase64s + base64
                                showSequentialCameraProgressDialog = true
                            } else {
                                viewModel.updateFinding(selectedIndex) { f ->
                                    when (targetPhotoSlot) {
                                        "ph1" -> f.copy(ph1Base64 = base64, ph2Base64 = null, ph3Base64 = null, ph4Base64 = null)
                                        "ph2" -> f.copy(ph2Base64 = base64)
                                        "ph3" -> f.copy(ph3Base64 = base64)
                                        "ph4" -> f.copy(ph4Base64 = base64)
                                        "rect" -> f.copy(auditeeClosurePhoto = base64)
                                        else -> f
                                    }
                                }
                            }
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
            tempCameraUriStr?.let { uriStr ->
                try {
                    cameraLauncher.launch(android.net.Uri.parse(uriStr))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            Toast.makeText(context, if (lang == "ar") "إذن الكاميرا مطلوب لالتقاط الصور" else "Camera permission is required to capture photos.", Toast.LENGTH_SHORT).show()
        }
    }

    val onLaunchCamera: (String) -> Unit = { slot ->
        try {
            targetPhotoSlot = slot
            val tempFile = java.io.File.createTempFile("photo_capture_", ".jpg", context.cacheDir).apply {
                deleteOnExit()
            }
            val authority = "${context.packageName}.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, tempFile)
            tempCameraUriStr = uri.toString()
            
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            )
            if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                cameraLauncher.launch(uri)
            } else {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val pick1 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                val base64 = saveUriToInternalStorage(context, it)
                withContext(Dispatchers.Main) {
                    viewModel.updateFinding(selectedIndex) { f ->
                        f.copy(
                            ph1Base64 = base64,
                            ph2Base64 = null,
                            ph3Base64 = null,
                            ph4Base64 = null
                        )
                    }
                }
            }
        }
    }
    val pickMultiple = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    if (uris.size > 9) {
                        withContext(Dispatchers.Main) {
                            val msg = if (lang == "ar") "تنبيه: يمكنك اختيار ٩ صور كحد أقصى. سيتم استخدام أول ٩ صور فقط." else "Note: You can select up to 9 photos. Only the first 9 will be used."
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                    val limitedUris = uris.take(9)
                    val bitmaps = limitedUris.mapNotNull { uri ->
                        decodeSampledBitmapFromUri(context, uri, 400)
                    }
                    if (bitmaps.isNotEmpty()) {
                        val collageBmp = createAutoCollage(bitmaps, 1200, 1200)
                        
                        val outputStream = java.io.ByteArrayOutputStream()
                        collageBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, outputStream)
                        val bytes = outputStream.toByteArray()
                        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        
                        collageBmp.recycle()
                        bitmaps.forEach { it.recycle() }
                        outputStream.close()
                        
                        withContext(Dispatchers.Main) {
                            viewModel.updateFinding(selectedIndex) { f ->
                                f.copy(
                                    ph1Base64 = base64,
                                    ph2Base64 = null,
                                    ph3Base64 = null,
                                    ph4Base64 = null
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error creating collage: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    val pick2 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                val base64 = saveUriToInternalStorage(context, it)
                withContext(Dispatchers.Main) {
                    viewModel.updateFinding(selectedIndex) { f -> f.copy(ph2Base64 = base64) }
                }
            }
        }
    }
    val pick3 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                val base64 = saveUriToInternalStorage(context, it)
                withContext(Dispatchers.Main) {
                    viewModel.updateFinding(selectedIndex) { f -> f.copy(ph3Base64 = base64) }
                }
            }
        }
    }
    val pick4 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                val base64 = saveUriToInternalStorage(context, it)
                withContext(Dispatchers.Main) {
                    viewModel.updateFinding(selectedIndex) { f -> f.copy(ph4Base64 = base64) }
                }
            }
        }
    }
    val pickRect = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                val base64 = saveUriToInternalStorage(context, it)
                withContext(Dispatchers.Main) {
                    viewModel.updateFinding(selectedIndex) { f -> f.copy(auditeeClosurePhoto = base64) }
                }
            }
        }
    }

    if (showDeleteConfirmationDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = {
                Text(
                    text = if (lang == "ar") "حذف ورقة NCR/OBS" else "Delete NCR/OBS Sheet",
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            },
            text = {
                Text(
                    text = if (lang == "ar") 
                        "هل أنت متأكد من رغبتك في حذف ورقة النتائج هذه (${finding.referenceId}) نهائياً؟ لا يمكن التراجع عن هذا الإجراء."
                    else 
                        "Are you sure you want to permanently delete this finding sheet (${finding.referenceId})? This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        val oldSize = findings.size
                        viewModel.removeFinding(selectedIndex)
                        if (selectedIndex >= oldSize - 1) {
                            selectedIndex = maxOf(0, oldSize - 2)
                        }
                    }
                ) {
                    Text(
                        text = if (lang == "ar") "حذف" else "Delete",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text(text = if (lang == "ar") "إلغاء" else "Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEEEEEE))
            .padding(10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Selector row with dynamic dropdown/row of tabs or picker + New tab action
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dropdown or horizontal buttons of findings
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (lang == "ar") "اختر ورقة NCR-OBS لتعديلها:" else "Select NCR-OBS Sheet to edit:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Simple Row with clickable finding pills to toggle active finding sheet
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        findings.forEachIndexed { index, item ->
                            val isSelected = index == selectedIndex
                            Button(
                                onClick = { selectedIndex = index },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    contentColor = if (isSelected) Color.White else Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(15.dp)
                            ) {
                                Text(
                                    text = item.referenceId.ifEmpty { "Finding #${index + 1}" },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (finding.status == "Open") {
                        Button(
                            onClick = { showDeleteConfirmationDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (lang == "ar") "حذف" else "Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Create new NCR button on right
                    Button(
                        onClick = {
                            viewModel.addFinding()
                            selectedIndex = findings.size // Select the newly created finding
                        },
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (lang == "ar") "إضافة" else "New", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Print representation paper container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color.Black, shape = RoundedCornerShape(2.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header layout: Logo - RefId - Dept Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left element: logo
                    Image(
                        painter = painterResource(id = R.drawable.img_qc_audit_logo_1781912328532),
                        contentDescription = "Logo",
                        modifier = Modifier.height(38.dp),
                        contentScale = ContentScale.Fit
                    )

                    // Center element: editable Reference ID in big font size
                    Box(modifier = Modifier.width(180.dp), contentAlignment = Alignment.Center) {
                        BasicTextField(
                            value = finding.referenceId,
                            onValueChange = { newVal ->
                                viewModel.updateFinding(selectedIndex) { f -> f.copy(referenceId = newVal) }
                            },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                        // Simple indicator to helper
                        if (finding.referenceId.isEmpty()) {
                            Text("FU-NCR-XXXX", color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Right element: Dept info
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Quality Control Department", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 9.sp), color = Color.Black)
                        Text("Internal Audit", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 9.sp), color = Color.DarkGray)
                        Text("NCR / OBS Form", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 9.sp), color = Color.Black)
                        Text("Audit #${report.auditNumber.ifEmpty { "255" }}", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 9.sp), color = Color.Black)
                    }
                }

                Divider(color = Color.Black, thickness = 1.dp)

                // Photos row: Auditor Photos, Rectification Photos, Comments & NCR Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    // Col 1: NCR Photos (weight = 0.45)
                    Column(
                        modifier = Modifier
                            .weight(0.45f)
                            .padding(6.dp)
                    ) {
                        val isObsMode = finding.type == "OBS"
                        Text(
                            text = if (lang == "ar") {
                                if (isObsMode) "صور المعاينة (بواسطة المراجع)\nOBS Photo (By auditor)" else "صور المعاينة (بواسطة المراجع)\nNCR Photo (By auditor)"
                            } else {
                                if (isObsMode) "OBS Photo\n(By auditor)" else "NCR Photo\n(By auditor)"
                            },
                            style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Single photo/collage slot with options
                        var showPhotoOptionsDialog by remember { mutableStateOf(false) }
                        
                        if (showPhotoOptionsDialog) {
                            androidx.compose.ui.window.Dialog(onDismissRequest = { showPhotoOptionsDialog = false }) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = if (lang == "ar") "إضافة صورة أو كولاج" else "Add Photo or Collage",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.Black
                                        )
                                        
                                        // Camera Option: Capture a single photo
                                        Button(
                                            onClick = {
                                                showPhotoOptionsDialog = false
                                                onLaunchCamera("ph1")
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.White)
                                                Text(if (lang == "ar") "كاميرا: التقاط صورة واحدة" else "Camera: Capture a single photo", color = Color.White)
                                            }
                                        }

                                        // Gallery Option: Choose a single photo
                                        Button(
                                            onClick = {
                                                showPhotoOptionsDialog = false
                                                pick1.launch("image/*")
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Image, contentDescription = "Gallery Single", tint = Color.White)
                                                Text(if (lang == "ar") "المعرض (صورة واحدة): اختيار صورة من المعرض" else "Gallery (1 Photo): Choose a single photo from your gallery", color = Color.White)
                                            }
                                        }

                                        // Create Collage Option (1 to 9 photos)
                                        Button(
                                            onClick = {
                                                showPhotoOptionsDialog = false
                                                pickMultiple.launch("image/*")
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) // Clean green
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Collections, contentDescription = "Gallery Multiple", tint = Color.White)
                                                Text(if (lang == "ar") "إنشاء كولاج (تحديد ١-٩ صور): المعرض لتحديد ١-٩ صور" else "Create Collage (Select 1–9 Photos): Launch the multi-image picker to select up to 9 photos", color = Color.White)
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        TextButton(onClick = { showPhotoOptionsDialog = false }) {
                                            Text(if (lang == "ar") "إلغاء" else "Cancel")
                                        }
                                    }
                                }
                            }
                        }

                        if (showSequentialCameraProgressDialog) {
                            androidx.compose.ui.window.Dialog(onDismissRequest = { /* Modal */ }) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = if (lang == "ar") "صور الكولاج الملتقطة" else "Captured Collage Photos",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.Black
                                        )
                                        
                                        Text(
                                            text = if (lang == "ar") "تم التقاط ${sequentialCameraBase64s.size} من ٩ صور" else "Captured ${sequentialCameraBase64s.size} of 9 photos",
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                        
                                        // 3x3 Grid
                                        val rows = (sequentialCameraBase64s.size + 2) / 3
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                                        ) {
                                            for (r in 0 until rows) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    for (c in 0 until 3) {
                                                        val index = r * 3 + c
                                                        if (index < sequentialCameraBase64s.size) {
                                                            val b64 = sequentialCameraBase64s[index]
                                                            var bmp by remember(b64) { mutableStateOf<android.graphics.Bitmap?>(null) }
                                                            LaunchedEffect(b64) {
                                                                try {
                                                                    val decoded = withContext(Dispatchers.IO) {
                                                                        loadImageStringToBitmap(context, b64, 300)
                                                                    }
                                                                    bmp = decoded
                                                                } catch (e: Exception) {
                                                                    e.printStackTrace()
                                                                }
                                                            }
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .aspectRatio(1f)
                                                                    .background(Color(0xFFEEEEEE), shape = RoundedCornerShape(4.dp))
                                                            ) {
                                                                val currentBmp = bmp
                                                                if (currentBmp != null) {
                                                                    Image(
                                                                        bitmap = currentBmp.asImageBitmap(),
                                                                        contentDescription = null,
                                                                        modifier = Modifier.fillMaxSize(),
                                                                        contentScale = ContentScale.Crop
                                                                    )
                                                                }
                                                                IconButton(
                                                                    onClick = {
                                                                        sequentialCameraBase64s = sequentialCameraBase64s.toMutableList().apply {
                                                                            removeAt(index)
                                                                        }
                                                                    },
                                                                    modifier = Modifier
                                                                        .align(Alignment.TopEnd)
                                                                        .size(20.dp)
                                                                        .background(Color.White.copy(alpha = 0.8f), shape = CircleShape)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Close,
                                                                        contentDescription = "Delete",
                                                                        tint = Color.Red,
                                                                        modifier = Modifier.size(12.dp)
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
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        if (sequentialCameraBase64s.size < 9) {
                                            Button(
                                                onClick = {
                                                    onLaunchCamera("sequential")
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.White)
                                                    Text(if (lang == "ar") "التقاط الصورة التالية (+)" else "Capture Next Photo (+)", color = Color.White)
                                                }
                                            }
                                        }
                                        
                                        Button(
                                            onClick = {
                                                showSequentialCameraProgressDialog = false
                                                if (sequentialCameraBase64s.isNotEmpty()) {
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        try {
                                                            val limitedBase64s = sequentialCameraBase64s.take(9)
                                                            val bitmaps = limitedBase64s.mapNotNull { b64 ->
                                                                loadImageStringToBitmap(context, b64, 400)
                                                            }
                                                            if (bitmaps.isNotEmpty()) {
                                                                val collageBmp = createAutoCollage(bitmaps, 1200, 1200)
                                                                val outputStream = java.io.ByteArrayOutputStream()
                                                                collageBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, outputStream)
                                                                val bytes = outputStream.toByteArray()
                                                                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                                                
                                                                collageBmp.recycle()
                                                                bitmaps.forEach { it.recycle() }
                                                                outputStream.close()
                                                                
                                                                withContext(Dispatchers.Main) {
                                                                    viewModel.updateFinding(selectedIndex) { f ->
                                                                        f.copy(
                                                                            ph1Base64 = base64,
                                                                            ph2Base64 = null,
                                                                            ph3Base64 = null,
                                                                            ph4Base64 = null
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            enabled = sequentialCameraBase64s.isNotEmpty()
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White)
                                                Text(if (lang == "ar") "إنشاء الكولاج وحفظ (✓)" else "Create Collage & Save (✓)", color = Color.White)
                                            }
                                        }
                                        
                                        TextButton(
                                            onClick = {
                                                sequentialCameraBase64s = emptyList()
                                                showSequentialCameraProgressDialog = false
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(if (lang == "ar") "إلغاء وتراجع" else "Cancel & Discard", color = Color.Red)
                                        }
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(204.dp)
                                .background(Color(0xFFFAFAFA))
                                .border(1.dp, Color.Black)
                                .clickable {
                                    if (finding.ph1Base64 == null) {
                                        showPhotoOptionsDialog = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            var bmp by remember(finding.ph1Base64) { mutableStateOf<android.graphics.Bitmap?>(null) }
                            LaunchedEffect(finding.ph1Base64) {
                                try {
                                    val decoded = withContext(Dispatchers.IO) {
                                        loadImageStringToBitmap(context, finding.ph1Base64, 400)
                                    }
                                    bmp = decoded
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            val currentBmp = bmp
                            if (currentBmp != null) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = currentBmp.asImageBitmap(),
                                        contentDescription = "Auditor Photo or Collage",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Clear Button
                                    IconButton(
                                        onClick = {
                                            viewModel.updateFinding(selectedIndex) { f ->
                                                f.copy(
                                                    ph1Base64 = null,
                                                    ph2Base64 = null,
                                                    ph3Base64 = null,
                                                    ph4Base64 = null
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(24.dp)
                                            .background(Color.White.copy(alpha = 0.7f), shape = CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear image",
                                            tint = Color.Red,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Collections,
                                        contentDescription = "Photos icon",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = if (lang == "ar") "اضغط لالتقاط أو اختيار صور\n(١-٩ صور لصنع كولاج تلقائي)" else "Tap to capture or choose photo\n(Select 1-9 photos for Auto-Collage)",
                                        style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 9.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))

                    // Col 2: Rectification Photo (weight = 0.35)
                    Column(
                        modifier = Modifier
                            .weight(0.35f)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = if (lang == "ar") "صور المعالجة (بواسطة المنفذ)\nRectification Photo (By auditee)" else "Rectification Photo\n(By auditee)",
                            style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LargePhotoSlot(
                            base64Str = finding.auditeeClosurePhoto,
                            lang = lang,
                            onChooseGallery = { pickRect.launch("image/*") },
                            onLaunchCamera = { onLaunchCamera("rect") },
                            modifier = Modifier.fillMaxWidth().height(204.dp)
                        ) {
                            viewModel.updateFinding(selectedIndex) { f -> f.copy(auditeeClosurePhoto = null) }
                        }
                    }

                    Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))

                    // Col 3: NCR Status & Comments (weight = 0.20)
                    Column(
                        modifier = Modifier
                            .weight(0.20f)
                            .background(Color.White)
                    ) {
                        // Header
                        val isObsStatus = finding.type == "OBS"
                        Text(
                            text = if (lang == "ar") {
                                if (isObsStatus) "حالة OBS\nOBS Status (By Auditor)" else "حالة NCR\nNCR Status (By Auditor)"
                            } else {
                                if (isObsStatus) "OBS Status\n(By Auditor)" else "NCR Status\n(By Auditor)"
                            },
                            style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 9.sp, textAlign = TextAlign.Center),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            color = Color.Black
                        )
                        
                        Divider(color = Color.Black)
                        
                        // Comments content area
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                        ) {
                            Text("Comments:", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 8.5.sp), color = Color.Black)
                            Spacer(modifier = Modifier.height(4.dp))
                            BasicTextField(
                                value = finding.auditeeResponse,
                                onValueChange = { newVal ->
                                    viewModel.updateFinding(selectedIndex) { f -> f.copy(auditeeResponse = newVal) }
                                },
                                textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 9.5.sp, color = Color.Black),
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 6
                            )
                        }

                        // Bottom highlighted status box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF59D)) // Beautiful construction yellow highlight
                                .padding(4.dp)
                                .border(1.dp, Color.Black)
                        ) {
                            val isObsStatusLabel = finding.type == "OBS"
                            Text(
                                text = if (isObsStatusLabel) "OBS Status:" else "NCR Status:",
                                style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 9.sp, textDecoration = TextDecoration.Underline),
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                // Open selector
                                Text(
                                    text = "Open",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (finding.status == "Open") Color(0xFFC62828) else Color.Gray,
                                        textDecoration = if (finding.status == "Open") TextDecoration.Underline else TextDecoration.None
                                    ),
                                    modifier = Modifier.clickable {
                                        viewModel.updateFinding(selectedIndex) { f -> f.copy(status = "Open") }
                                    }
                                )
                                // Closed selector
                                Text(
                                    text = "Closed",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (finding.status == "Closed") Color(0xFF2E7D32) else Color.Gray,
                                        textDecoration = if (finding.status == "Closed") TextDecoration.Underline else TextDecoration.None
                                    ),
                                    modifier = Modifier.clickable {
                                        viewModel.updateFinding(selectedIndex) { f -> f.copy(status = "Closed") }
                                    }
                                )
                            }
                        }
                    }
                }

                Divider(color = Color.Black, thickness = 1.dp)

                // Key-value grid row: Trade, Activity, Location vs Corrective Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    // Left subcolumn: Trade, Activity, Location
                    Column(modifier = Modifier.weight(0.45f)) {
                        GridInCellTradeDropdownField(
                            label = "Trade:",
                            value = finding.trade,
                            onValueChange = { newVal ->
                                viewModel.updateFinding(selectedIndex) { f -> f.copy(trade = newVal) }
                            }
                        )
                        Divider(color = Color.Black)
                        GridInCellActivityDropdownField(
                            label = "Activity:",
                            value = finding.activity,
                            onValueChange = { newVal ->
                                viewModel.updateFinding(selectedIndex) { f -> f.copy(activity = newVal) }
                            }
                        )
                        Divider(color = Color.Black)
                        GridInCellTextField(
                            label = "Location:",
                            value = finding.locationZone,
                            onValueChange = { newVal ->
                                viewModel.updateFinding(selectedIndex) { f -> f.copy(locationZone = newVal) }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))

                    // Right subcolumn: Corrective Action spanning height
                    Column(
                        modifier = Modifier
                            .weight(0.55f)
                            .padding(6.dp)
                    ) {
                        Text("Corrective Action required:", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 9.sp), color = Color.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        BasicTextField(
                            value = finding.correctiveAction,
                            onValueChange = { newVal ->
                                viewModel.updateFinding(selectedIndex) { f -> f.copy(correctiveAction = newVal) }
                            },
                            textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 9.5.sp, color = Color.Black),
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4
                        )
                    }
                }

                Divider(color = Color.Black, thickness = 1.dp)

                // Description: Improper quality of reinforced concrete slab casting (spans full width)
                Column(modifier = Modifier.padding(6.dp)) {
                    Text(
                        text = if (lang == "ar") "الوصف التفصيلي (Description):" else "Description:",
                        style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BasicTextField(
                        value = finding.description,
                        onValueChange = { newVal ->
                            viewModel.updateFinding(selectedIndex) { f -> f.copy(description = newVal) }
                        },
                        textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 9.5.sp, color = Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }

                Divider(color = Color.Black, thickness = 1.dp)

                // Subgrid: Type, Dates, Signatures vs Root Cause, Reply states
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    // Left segment (Type, Dates, Auditor)
                    Column(modifier = Modifier.weight(0.45f)) {
                        // Type Selection Row (NCR checked or OBS checked)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Type:", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 9.sp), color = Color.Black, modifier = Modifier.weight(1f))
                            
                            // NCR Option
                            Row(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .clickable {
                                        viewModel.updateFinding(selectedIndex) { f -> f.copy(type = "NCR") }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .border(1.dp, Color.Black)
                                        .background(if (finding.type == "NCR") Color.Black else Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (finding.type == "NCR") {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("NCR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                            
                            // OBS Option
                            Row(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .clickable {
                                        viewModel.updateFinding(selectedIndex) { f -> f.copy(type = "OBS") }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .border(1.dp, Color.Black)
                                        .background(if (finding.type == "OBS") Color.Black else Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (finding.type == "OBS") {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("OBS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        
                        Divider(color = Color.Black)

                        // Issuing Date input
                        GridDatePickerCell(
                            label = "Issuing Date:",
                            value = finding.issueDate,
                            onValueChange = { newVal ->
                                viewModel.updateFinding(selectedIndex) { f -> f.copy(issueDate = newVal) }
                            }
                        )

                        Divider(color = Color.Black)

                        // Due date input
                        GridDatePickerCell(
                            label = "Due Date:",
                            value = finding.dueDate,
                            onValueChange = { newVal ->
                                viewModel.updateFinding(selectedIndex) { f -> f.copy(dueDate = newVal) }
                            }
                        )

                        Divider(color = Color.Black)

                        // Auditor Name
                        GridInCellDropdown(
                            label = "Auditor Name:",
                            value = report.auditorName,
                            onValueChange = { newVal ->
                                viewModel.updateReport { r -> r.copy(auditorName = newVal) }
                            },
                            options = listOf("Rafik Hisham", "Hesham Saeed"),
                            placeholder = "Select Auditor"
                        )

                        Divider(color = Color.Black)

                        // Auditor Signature
                        Column(modifier = Modifier.padding(4.dp)) {
                            Text("Auditor Signature:", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 8.5.sp), color = Color.Black)
                            Spacer(modifier = Modifier.height(2.dp))
                            BasicTextField(
                                value = report.sigAuditorName,
                                onValueChange = { newVal ->
                                    viewModel.updateReport { r -> r.copy(sigAuditorName = newVal) }
                                },
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Cursive,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.Blue
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            SignaturePhotoPickerCell(
                                sigName = report.sigAuditorName.ifEmpty { "Auditor" },
                                base64Str = report.sigAuditorPh,
                                onPhotoSelected = { base -> viewModel.updateReport { it.copy(sigAuditorPh = base) } },
                                onPhotoCleared = { viewModel.updateReport { it.copy(sigAuditorPh = null) } },
                                modifier = Modifier.fillMaxWidth().height(42.dp).border(0.5.dp, Color.LightGray)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black))

                    // Middle segment (Root Cause, QC Manager)
                    Column(modifier = Modifier.weight(0.55f)) {
                        // Root Cause Section
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("Root Cause:", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 9.sp), color = Color.Black)
                            Spacer(modifier = Modifier.height(4.dp))
                            BasicTextField(
                                value = finding.rootCause,
                                onValueChange = { newVal ->
                                    viewModel.updateFinding(selectedIndex) { f -> f.copy(rootCause = newVal) }
                                },
                                textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 9.5.sp, color = Color.Black),
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        }

                        Divider(color = Color.Black)

                        // Reply Date input
                        GridDatePickerCell(
                            label = "Reply Date:",
                            value = finding.replyDate,
                            onValueChange = { newVal ->
                                viewModel.updateFinding(selectedIndex) { f -> f.copy(replyDate = newVal) }
                            }
                        )

                        Divider(color = Color.Black)

                        // QC Manager Name
                        val qcSelectOptions = listOf(
                            "Mina Medhat",
                            "Mina Yakoub",
                            "Ahmed Bayoumi",
                            "Amir Azmy amir",
                            "Eslam Salah",
                            "Eslam Fattouh",
                            "Kerollous Nageh",
                            "Kirolos Azer",
                            "Mohamed Ismail",
                            "Mohammed Hosni",
                            "Moheb Gendy",
                            "Romany Helmy",
                            "Sayed Ahmed",
                            "Mohamed",
                            "Ahmed Mazen",
                            "Mohamed Galal"
                        )
                        GridInCellDropdown(
                            label = "QC Manager name:",
                            value = report.qcManager,
                            onValueChange = { newVal ->
                                viewModel.updateReport { r -> r.copy(qcManager = newVal) }
                            },
                            options = qcSelectOptions
                        )

                        Divider(color = Color.Black)

                        // QC Manager Signature
                        Column(modifier = Modifier.padding(4.dp)) {
                            Text("QC Manager Signature:", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 8.5.sp), color = Color.Black)
                            Spacer(modifier = Modifier.height(2.dp))
                            BasicTextField(
                                value = report.sigReviewerName,
                                onValueChange = { newVal ->
                                    viewModel.updateReport { r -> r.copy(sigReviewerName = newVal) }
                                },
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Cursive,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.Blue
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            SignaturePhotoPickerCell(
                                sigName = report.sigReviewerName.ifEmpty { "QC Manager" },
                                base64Str = report.sigReviewerPh,
                                onPhotoSelected = { base -> viewModel.updateReport { it.copy(sigReviewerPh = base) } },
                                onPhotoCleared = { viewModel.updateReport { it.copy(sigReviewerPh = null) } },
                                modifier = Modifier.fillMaxWidth().height(42.dp).border(0.5.dp, Color.LightGray)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------- HELPER FOR DETAILS SHEET FIELDS -------------------

@Composable
fun LargePhotoSlot(
    base64Str: String?,
    lang: String,
    onChooseGallery: () -> Unit,
    onLaunchCamera: () -> Unit,
    modifier: Modifier = Modifier,
    onClear: () -> Unit
) {
    var showSourceDialog by remember { mutableStateOf(false) }

    if (showSourceDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showSourceDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (lang == "ar") "مصدر الصورة" else "Select Image Source",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Camera button
                        Button(
                            onClick = {
                                showSourceDialog = false
                                onLaunchCamera()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Camera")
                                Text(if (lang == "ar") "كاميرا" else "Camera", fontSize = 12.sp)
                            }
                        }

                        // Gallery button
                        Button(
                            onClick = {
                                showSourceDialog = false
                                onChooseGallery()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Image, contentDescription = "Gallery")
                                Text(if (lang == "ar") "المعرض" else "Gallery", fontSize = 12.sp)
                            }
                        }
                    }
                    
                    TextButton(onClick = { showSourceDialog = false }) {
                        Text(if (lang == "ar") "إلغاء" else "Cancel")
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFFFAFAFA))
            .border(1.dp, Color.Black)
            .clickable { 
                if (base64Str == null) {
                    showSourceDialog = true 
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        if (base64Str != null) {
            var bmp by remember(base64Str) { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(base64Str) {
                try {
                    val decoded = withContext(Dispatchers.IO) {
                        loadImageStringToBitmap(context, base64Str, 400)
                    }
                    bmp = decoded
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (bmp != null) {
                val currentBmp = bmp
                if (currentBmp != null) {
                    Image(
                        bitmap = currentBmp.asImageBitmap(),
                        contentDescription = "Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // Clear button
                IconButton(
                    onClick = { onClear() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .padding(2.dp)
                        .background(Color.White, shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "clear", tint = Color.Red, modifier = Modifier.size(14.dp))
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "camera",
                    tint = Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(if (lang == "ar") "انقر لالتقاط" else "Tap to capture", fontSize = 8.5.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun GridInCellTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 9.sp),
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 9.5.sp, color = Color.Black),
            modifier = Modifier.weight(2.5f)
        )
    }
}

@Composable
fun GridInCellDropdown(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    placeholder: String = "Select QC Manager"
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clickable { expanded = true }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 9.sp),
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier.weight(2.5f),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (value.isEmpty()) placeholder else value,
                    style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 9.5.sp, color = if (value.isEmpty()) Color.LightGray else Color.Black)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand dropdown",
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, fontSize = 9.5.sp, color = Color.Black) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GridDatePickerCell(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clickable {
                showDatePickerDialog(context, value, onValueChange)
            }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 9.sp),
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.ifEmpty { "YYYY-MM-DD" },
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 9.5.sp,
                color = if (value.isEmpty()) Color.LightGray else Color.Black
            ),
            modifier = Modifier.weight(2.5f)
        )
    }
}

// =================== CUSTOM BOTTOM NAVIGATION BAR ===================
@Composable
fun CustomBottomBar(
    currentTab: String,
    lang: String,
    onTabSelected: (String) -> Unit
) {
    val isAr = lang == "ar"
    val items = listOf(
        NavigationItem("home", if (isAr) "الرئيسية" else "Home", Icons.Default.Home),
        NavigationItem("form", if (isAr) "النموذج" else "Audit Form", Icons.Default.Assignment),
        NavigationItem("ncr_obs", if (isAr) "النتائج" else "NCR/OBS", Icons.Default.Warning),
        NavigationItem("summary", if (isAr) "الملخص" else "Tracking", Icons.Default.ShowChart),
        NavigationItem("history", if (isAr) "السابقة" else "History", Icons.Default.History),
        NavigationItem("auditee", if (isAr) "الرد" else "Response", Icons.Default.Forum),
        NavigationItem("export", if (isAr) "تصدير" else "Export File", Icons.Default.Description),
        NavigationItem("documents", if (isAr) "التدقيقات" else "Audits", Icons.Default.Folder),
        NavigationItem("gemini", if (isAr) "ذكاء جيميناي" else "Gemini QC", Icons.Default.AutoAwesome),
        NavigationItem("collage", if (isAr) "تجميع الصور" else "Collage", Icons.Default.Collections),
        NavigationItem("settings", if (isAr) "الإعدادات" else "Settings", Icons.Default.Settings)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF030D1E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, Color(0xFF00BFA5).copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val active = currentTab == item.id
                val activeColor = Color(0xFF00BFA5)
                val inactiveColor = Color.White.copy(alpha = 0.6f)
                
                Box(
                    modifier = Modifier
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) activeColor.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onTabSelected(item.id) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (active) activeColor else inactiveColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.label,
                            style = TextStyle(
                                fontSize = 9.5.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) activeColor else inactiveColor
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

data class NavigationItem(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

// =================== NEW START SCREEN COMPONENTS (BILINGUAL) ===================

@Composable
fun AuditDashboardCard(
    title: String,
    count: Int,
    ncrCount: Int,
    obsCount: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(115.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF04142B).copy(alpha = 0.85f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = count.toString(),
                    style = TextStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                )
            }
            Column {
                Text(
                    text = title,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "NCR: $ncrCount  •  OBS: $obsCount",
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StartScreen(
    lang: String,
    onToggleLang: () -> Unit,
    onNavigate: (String) -> Unit,
    onOpenReportsList: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: AuditViewModel,
    savedReports: List<AuditReport>,
    allFindings: List<FindingOverview>
) {
    val isAr = lang == "ar"
    
    // Dynamic metrics calculation
    val completedReports = savedReports.filter { report ->
        val revStatus = report.reviewerStatus.trim().lowercase()
        val audStatus = report.auditeeOverallStatus.trim().lowercase()
        
        val isCompletedMarker = revStatus == "approved" || revStatus == "closed" || revStatus == "completed" || revStatus == "مكتمل" || revStatus == "موافق" ||
                audStatus == "closed" || audStatus == "completed" || audStatus == "مكتمل" || audStatus == "مغلق"
                
        val reportFindings = allFindings.filter { it.reportId == report.id }
        val hasFindings = reportFindings.isNotEmpty()
        val allClosed = hasFindings && reportFindings.all { it.status.trim().lowercase() == "closed" || it.auditeeStatus.trim().lowercase() == "closed" }
        
        isCompletedMarker || (hasFindings && allClosed)
    }

    val flaggedReports = savedReports.filter { report ->
        val reportFindings = allFindings.filter { it.reportId == report.id }
        val hasOpenMajor = reportFindings.any { 
            (it.status.trim().lowercase() == "open" || it.auditeeStatus.trim().lowercase() == "open") && 
            (it.severity.trim().lowercase() == "major" || it.severity.trim().lowercase() == "critical") 
        }
        
        val revStatus = report.reviewerStatus.trim().lowercase()
        val isFlaggedMarker = revStatus.contains("flagged") || revStatus.contains("ncr") || revStatus.contains("هام") || revStatus.contains("مرفوض")
        
        hasOpenMajor || isFlaggedMarker
    }

    val pendingReports = savedReports.filter { report ->
        val revStatus = report.reviewerStatus.trim().lowercase()
        val audStatus = report.auditeeOverallStatus.trim().lowercase()
        val isCompleted = revStatus == "approved" || revStatus == "closed" || revStatus == "completed" || revStatus == "مكتمل" || revStatus == "موافق" ||
                audStatus == "closed" || audStatus == "completed" || audStatus == "مكتمل" || audStatus == "مغلق"
        
        val reportFindings = allFindings.filter { it.reportId == report.id }
        val hasFindings = reportFindings.isNotEmpty()
        val allClosed = hasFindings && reportFindings.all { it.status.trim().lowercase() == "closed" || it.auditeeStatus.trim().lowercase() == "closed" }
        val isCompletedAlt = isCompleted || (hasFindings && allClosed)
        
        val hasOpenMajor = reportFindings.any { 
            (it.status.trim().lowercase() == "open" || it.auditeeStatus.trim().lowercase() == "open") && 
            (it.severity.trim().lowercase() == "major" || it.severity.trim().lowercase() == "critical") 
        }
        val isFlagged = hasOpenMajor || revStatus.contains("flagged") || revStatus.contains("ncr") || revStatus.contains("هام") || revStatus.contains("مرفوض")
        
        !isCompletedAlt && !isFlagged
    }

    val pendingCount = pendingReports.size
    val completedCount = completedReports.size
    val flaggedCount = flaggedReports.size

    val pendingReportIds = pendingReports.map { it.id }.toSet()
    val pendingFindings = allFindings.filter { it.reportId in pendingReportIds }
    val pendingNcr = pendingFindings.count { it.type.trim().uppercase() == "NCR" }
    val pendingObs = pendingFindings.count { it.type.trim().uppercase() == "OBS" }

    val completedReportIds = completedReports.map { it.id }.toSet()
    val completedFindings = allFindings.filter { it.reportId in completedReportIds }
    val completedNcr = completedFindings.count { it.type.trim().uppercase() == "NCR" }
    val completedObs = completedFindings.count { it.type.trim().uppercase() == "OBS" }

    val flaggedReportIds = flaggedReports.map { it.id }.toSet()
    val flaggedFindings = allFindings.filter { it.reportId in flaggedReportIds }
    val flaggedNcr = flaggedFindings.count { it.type.trim().uppercase() == "NCR" }
    val flaggedObs = flaggedFindings.count { it.type.trim().uppercase() == "OBS" }
    
    var activeDashboardFilter by remember { mutableStateOf<String?>(null) }
    
    // Bottom sheet list / Dialog logic when clicking a dynamic stat card
    activeDashboardFilter?.let { filter ->
        val filteredList = when (filter) {
            "Pending" -> pendingReports
            "Completed" -> completedReports
            "Flagged" -> flaggedReports
            else -> emptyList()
        }
        
        AlertDialog(
            onDismissRequest = { activeDashboardFilter = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val titleIcon = when (filter) {
                        "Pending" -> Icons.Default.Schedule
                        "Completed" -> Icons.Default.CheckCircle
                        else -> Icons.Default.Warning
                    }
                    val titleColor = when (filter) {
                        "Pending" -> Color(0xFFED7D31)
                        "Completed" -> Color(0xFF00BFA5)
                        else -> Color(0xFFFF5252)
                    }
                    val titleText = when (filter) {
                        "Pending" -> if (isAr) "التدقيقات المعلقة" else "Pending Audits"
                        "Completed" -> if (isAr) "التدقيقات المكتملة" else "Completed Audits"
                        else -> if (isAr) "التدقيقات المستهدفة ببلاغات" else "Flagged Audits"
                    }
                    Icon(imageVector = titleIcon, contentDescription = null, tint = titleColor)
                    Text(
                        text = "$titleText (${filteredList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAr) "لا توجد تدقيقات في هذا التصنيف حالياً" else "No audits found in this category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList) { report ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.loadReport(report.id)
                                        activeDashboardFilter = null
                                        onNavigate("form")
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF06152D)
                                ),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = report.projectName.ifEmpty { if (isAr) "بدون اسم مشروع" else "Unnamed Project" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = report.auditDate.ifEmpty { report.reportIssuanceDate },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                            if (report.auditNumber.isNotEmpty()) {
                                                Text(
                                                    text = "#${report.auditNumber}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF00BFA5),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Open",
                                        tint = Color(0xFF00BFA5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeDashboardFilter = null }) {
                    Text(
                        text = if (isAr) "إغلاق" else "Close",
                        color = Color(0xFF00BFA5),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = Color(0xFF010E24),
            textContentColor = Color.White
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_home_bg_1781831330332),
            contentDescription = "QC Internal Audit Premium Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with Close/Shutdown on the left and Language Selector on the right
            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    TextButton(
                        onClick = {
                            (context as? android.app.Activity)?.finishAndRemoveTask()
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                            .border(BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f)), RoundedCornerShape(20.dp))
                            .testTag("shutdown_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = if (isAr) "إغلاق" else "Shut Down",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isAr) "إغلاق" else "Close",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFF5252)
                            )
                        }
                    }

                    TextButton(
                        onClick = onToggleLang,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                        modifier = Modifier.background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = if (isAr) "English" else "العربية",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF00BFA5)
                        )
                    }
                }
            }

            // Beautiful generous spacer to let the glorious high-res background logo, midline, waves & tagline shine untouched!
            Spacer(modifier = Modifier.height(300.dp))
            
            // === AUDIT DASHBOARD COMPONENT ===
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isAr) "لوحة تحكم عمليات التدقيق" else "Audit Dashboard",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = if (isAr) 0.5.sp else 1.sp
                    ),
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuditDashboardCard(
                        title = if (isAr) "قيد الانتظار" else "Pending",
                        count = pendingCount,
                        ncrCount = pendingNcr,
                        obsCount = pendingObs,
                        icon = Icons.Default.Schedule,
                        color = Color(0xFFED7D31),
                        onClick = { activeDashboardFilter = "Pending" },
                        modifier = Modifier.weight(1f).testTag("dashboard_pending_card")
                    )
                    AuditDashboardCard(
                        title = if (isAr) "مكتملة" else "Completed",
                        count = completedCount,
                        ncrCount = completedNcr,
                        obsCount = completedObs,
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF00BFA5),
                        onClick = { activeDashboardFilter = "Completed" },
                        modifier = Modifier.weight(1f).testTag("dashboard_completed_card")
                    )
                    AuditDashboardCard(
                        title = if (isAr) "مستهدفة ببلاغ" else "Flagged",
                        count = flaggedCount,
                        ncrCount = flaggedNcr,
                        obsCount = flaggedObs,
                        icon = Icons.Default.Warning,
                        color = Color(0xFFFF5252),
                        onClick = { activeDashboardFilter = "Flagged" },
                        modifier = Modifier.weight(1f).testTag("dashboard_flagged_card")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))


            // Ordered Tiles Grid: 4 Rows of 2 columns
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: 1 & 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        TileItem(
                            numberAndTitle = if (isAr) "1. نموذج التدقيق" else "1. Audit Form",
                            description = if (isAr) "إنشاء وإدارة نماذج التدقيق" else "Create and manage audit forms",
                            icon = Icons.Default.Assignment,
                            onClick = { onNavigate("form") }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        TileItem(
                            numberAndTitle = if (isAr) "2. نتائج التدقيق" else "2. Audit Findings",
                            description = if (isAr) "رصد ومتابعة حالات عدم المطابقة والملحوظات" else "Record and manage NCR/OBS findings",
                            icon = Icons.Default.Warning,
                            onClick = { onNavigate("ncr_obs") }
                        )
                    }
                }

                // Row 2: 3 & 4
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        TileItem(
                            numberAndTitle = if (isAr) "3. ملخص المتابعة" else "3. Tracking Summary",
                            description = if (isAr) "تتبع ومراقبة تقدم أعمال التدقيق" else "Track and monitor audit progress",
                            icon = Icons.Default.ShowChart,
                            onClick = { onNavigate("summary") }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        TileItem(
                            numberAndTitle = if (isAr) "4. المراجعات السابقة" else "4. Previous Audits",
                            description = if (isAr) "استعراض ومراجعة التقارير المحفوظة" else "View and review previous audits",
                            icon = Icons.Default.History,
                            onClick = { onNavigate("history") }
                        )
                    }
                }

                // Row 3: 5 & 6
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        TileItem(
                            numberAndTitle = if (isAr) "5. رد الجهة المنفذة" else "5. Auditee Response",
                            description = if (isAr) "متابعة وإدارة التعهدات والإجراءات الوقائية" else "Review and manage auditee responses",
                            icon = Icons.Default.Forum,
                            onClick = { onNavigate("auditee") }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        TileItem(
                            numberAndTitle = if (isAr) "6. تصدير التقارير" else "6. Export PDF / WORD",
                            description = if (isAr) "تصدير المستند بصيغة PDF أو Word" else "Export audit reports in PDF or Word",
                            icon = Icons.Default.Description,
                            onClick = { onNavigate("export") }
                        )
                    }
                }

                // Row 4: 7 & 8
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        TileItem(
                            numberAndTitle = if (isAr) "7. المستندات والتقارير" else "7. Saved Documents",
                            description = if (isAr) "الوصول السريع لجميع المستندات وقائمة الحفظ" else "Access all saved local reports",
                            icon = Icons.Default.FolderOpen,
                            onClick = onOpenReportsList
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        TileItem(
                            numberAndTitle = if (isAr) "8. إعدادات النظام" else "8. Settings",
                            description = if (isAr) "تفضيلات وقيم مدقق الجودة الافتراضية" else "Configure application default settings",
                            icon = Icons.Default.Settings,
                            onClick = onOpenSettings
                        )
                    }
                }

                // Row 5: Gemini AI Auditor (Full Width)
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .clickable(onClick = { onNavigate("gemini") }),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF031633).copy(alpha = 0.85f)
                        ),
                        border = BorderStroke(1.5.dp, Color(0xFF00BFA5))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF00BFA5).copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Gemini Auditor",
                                    tint = Color(0xFF00BFA5),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isAr) "9. ذكاء جيميناي الرقمي" else "9. Gemini AI Auditor",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF00BFA5).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "SMART",
                                            color = Color(0xFF00BFA5),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isAr) {
                                        "محرك جيميناي للإجابة على تساؤلات بنود ISO 9001:2015 وتحليل الأسباب الجذرية"
                                    } else {
                                        "Ask Gemini about QMS ISO 9001:2015, Auditing clauses, and Root Cause Analysis"
                                    },
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // Row 6: Photo Collage Builder (Full Width)
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .clickable(onClick = { onNavigate("collage") }),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF031633).copy(alpha = 0.85f)
                        ),
                        border = BorderStroke(1.5.dp, Color(0xFF00BFA5))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF00BFA5).copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Collections,
                                    contentDescription = "Photo Collage Builder",
                                    tint = Color(0xFF00BFA5),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isAr) "10. تجميع الصور الذكي" else "10. Photo Collage Builder",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF00BFA5).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "COLLAGE",
                                            color = Color(0xFF00BFA5),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isAr) {
                                        "تصميم وتنظيم ألبوم صور تدقيق الجودة آلياً وتطبيق قوالب تجميع متقدمة مع تعديل خط الأفق"
                                    } else {
                                        "Arrange multi-inspection photos into smart visual collages using templates and horizon correction"
                                    },
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                
                // Space & divider for advanced AI innovation panel
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFF00BFA5).copy(alpha = 0.3f), thickness = 1.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                AiInnovationHub(lang = lang)
            }
        }
    }
}

@Composable
fun TileItem(
    numberAndTitle: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF06152D).copy(alpha = 0.85f)
        ),
        border = BorderStroke(1.dp, Color(0xFF00BFA5).copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF030D1E), Color(0xFF0A1F3D))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF00BFA5),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = numberAndTitle,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = Color.White
                    ),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = TextStyle(
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    ),
                    maxLines = 2,
                    lineHeight = 11.sp
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    savedReportsCount: Int,
    onClearAllData: () -> Unit,
    lang: String
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("QC_Prefs", Context.MODE_PRIVATE) }
    
    var defaultAuditor by remember { mutableStateOf(prefs.getString("default_auditor_name", "") ?: "") }
    var defaultQcManager by remember { mutableStateOf(prefs.getString("default_qc_manager", "") ?: "") }
    var defaultLocation by remember { mutableStateOf(prefs.getString("default_location", "") ?: "") }
    
    var showConfirmReset by remember { mutableStateOf(false) }

    if (showConfirmReset) {
        AlertDialog(
            onDismissRequest = { showConfirmReset = false },
            title = {
                Text(
                    text = if (lang == "ar") "تأكيد مسح البيانات" else "Confirm Master Reset",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = if (lang == "ar")
                        "هل أنت متأكد من مسح جميع التقارير المحفوظة نهائياً من قاعدة البيانات؟ لا يمكن التراجع عن هذا الإجراء."
                    else
                        "Are you sure you want to permanently delete ALL saved audit reports from the database? This action is irreversible."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showConfirmReset = false
                        Toast.makeText(context, if (lang == "ar") "تم مسح البيانات" else "All data reset successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (lang == "ar") "نعم، امسح الكل" else "Yes, Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmReset = false }) {
                    Text(if (lang == "ar") "إلغاء" else "Cancel")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (lang == "ar") "إعدادات المدقق" else "QC Auditor Settings",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (lang == "ar") "قم بإعداد القيم الافتراضية للتقارير الجديدة:" else "Configure default values for new draft audits:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Default Auditor Name
                OutlinedTextField(
                    value = defaultAuditor,
                    onValueChange = {
                        defaultAuditor = it
                        prefs.edit().putString("default_auditor_name", it).apply()
                    },
                    label = { Text(if (lang == "ar") "اسم المراجع الافتراضي" else "Default Auditor Name") },
                    placeholder = { Text("e.g. Auditor Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Default QC Manager
                OutlinedTextField(
                    value = defaultQcManager,
                    onValueChange = {
                        defaultQcManager = it
                        prefs.edit().putString("default_qc_manager", it).apply()
                    },
                    label = { Text(if (lang == "ar") "مدير ضبط الجودة الافتراضي" else "Default QC Manager") },
                    placeholder = { Text("e.g. Mina Melad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Default Location
                OutlinedTextField(
                    value = defaultLocation,
                    onValueChange = {
                        defaultLocation = it
                        prefs.edit().putString("default_location", it).apply()
                    },
                    label = { Text(if (lang == "ar") "موقع العمل الافتراضي" else "Default Audit Location") },
                    placeholder = { Text("e.g. El Shrouk City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // Database Statistics and Reset
                Text(
                    text = if (lang == "ar") "إحصائيات النظام" else "System Statistics",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (lang == "ar") "التقارير المحفوظة:" else "Saved Reports:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "$savedReportsCount",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Button(
                    onClick = { showConfirmReset = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (lang == "ar") "مسح جميع البيانات" else "Master Clear All Reports")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (lang == "ar") "موافق" else "OK")
            }
        }
    )
}

private fun rotateImageIfRequired(context: android.content.Context, img: android.graphics.Bitmap, selectedImage: android.net.Uri): android.graphics.Bitmap {
    return try {
        val input = context.contentResolver.openInputStream(selectedImage) ?: return img
        val exifInterface = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
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
        
        val rotated = android.graphics.Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        if (rotated != img) {
            img.recycle()
        }
        rotated
    } catch (e: Exception) {
        e.printStackTrace()
        img
    }
}

fun createAutoCollage(bitmaps: List<android.graphics.Bitmap>, width: Int = 1200, height: Int = 1200): android.graphics.Bitmap {
    val result = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(result)
    canvas.drawColor(android.graphics.Color.WHITE) // Clean white background/spacing
    
    val count = bitmaps.size
    if (count == 0) return result
    if (count == 1) {
        // Just draw the single bitmap to fill the canvas
        drawBitmapCenterCrop(canvas, bitmaps[0], android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat()))
        return result
    }
    
    // Determine grid rows and columns automatically based on count (1 to 9)
    val cols = when (count) {
        2 -> 2
        3 -> 3
        4 -> 2
        5, 6 -> 3
        else -> 3 // 7, 8, 9 -> 3 columns
    }
    val rows = when (count) {
        2 -> 1
        3 -> 1
        4 -> 2
        5, 6 -> 2
        else -> 3 // 7, 8, 9 -> 3 rows
    }
    
    val spacing = 8f // Elegant spacing between photos
    val cellW = (width - (cols + 1) * spacing) / cols
    val cellH = (height - (rows + 1) * spacing) / rows
    
    for (i in 0 until count) {
        val r = i / cols
        val c = i % cols
        val left = spacing + c * (cellW + spacing)
        val top = spacing + r * (cellH + spacing)
        val right = left + cellW
        val bottom = top + cellH
        
        drawBitmapCenterCrop(canvas, bitmaps[i], android.graphics.RectF(left, top, right, bottom))
    }
    
    return result
}

fun drawBitmapCenterCrop(canvas: android.graphics.Canvas, bitmap: android.graphics.Bitmap, targetRect: android.graphics.RectF) {
    val bitmapWidth = bitmap.width.toFloat()
    val bitmapHeight = bitmap.height.toFloat()
    
    val targetWidth = targetRect.width()
    val targetHeight = targetRect.height()
    
    val scale = Math.max(targetWidth / bitmapWidth, targetHeight / bitmapHeight)
    val drawWidth = bitmapWidth * scale
    val drawHeight = bitmapHeight * scale
    
    val left = targetRect.left + (targetWidth - drawWidth) / 2f
    val top = targetRect.top + (targetHeight - drawHeight) / 2f
    
    val destRect = android.graphics.RectF(left, top, left + drawWidth, top + drawHeight)
    
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
        isDither = true
    }
    canvas.drawBitmap(bitmap, null, destRect, paint)
}

fun decodeSampledBitmapFromUri(context: android.content.Context, uri: android.net.Uri, maxSide: Int): android.graphics.Bitmap? {
    var inputStream: java.io.InputStream? = null
    return try {
        val options = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        inputStream = context.contentResolver.openInputStream(uri)
        android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()
        inputStream = null

        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        var inSampleSize = 1
        val originalMaxSide = Math.max(options.outWidth, options.outHeight)
        if (originalMaxSide > maxSide) {
            var tempSize = 1
            while (originalMaxSide / (tempSize * 2) >= maxSide) {
                tempSize *= 2
            }
            inSampleSize = tempSize
        }

        var bitmap: android.graphics.Bitmap? = null
        try {
            val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inJustDecodeBounds = false
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            }
            inputStream = context.contentResolver.openInputStream(uri)
            bitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()
            inputStream = null
        } catch (oom: java.lang.OutOfMemoryError) {
            System.gc()
            try {
                val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inJustDecodeBounds = false
                    inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                }
                inputStream = context.contentResolver.openInputStream(uri)
                bitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                inputStream?.close()
                inputStream = null
            } catch (oom2: java.lang.OutOfMemoryError) {
                System.gc()
                val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize * 2
                    inJustDecodeBounds = false
                    inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                }
                inputStream = context.contentResolver.openInputStream(uri)
                bitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                inputStream?.close()
                inputStream = null
            }
        }

        if (bitmap == null) return null

        if (Math.max(bitmap.width, bitmap.height) > maxSide) {
            val scale = maxSide.toFloat() / Math.max(bitmap.width, bitmap.height)
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            if (scaledBitmap != bitmap) {
                bitmap.recycle()
                bitmap = scaledBitmap
            }
        }

        rotateImageIfRequired(context, bitmap, uri)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } catch (oom: java.lang.OutOfMemoryError) {
        oom.printStackTrace()
        System.gc()
        null
    } finally {
        try {
            inputStream?.close()
        } catch (ignored: Exception) {}
    }
}

fun compressUriToBase64(context: android.content.Context, uri: android.net.Uri): String {
    return try {
        val bitmap = decodeSampledBitmapFromUri(context, uri, 2048) ?: return ""
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, outputStream)
        val bytes = outputStream.toByteArray()
        bitmap.recycle()
        outputStream.close()
        android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

fun saveUriToInternalStorage(context: android.content.Context, uri: android.net.Uri): String {
    return try {
        val bitmap = decodeSampledBitmapFromUri(context, uri, 2048) ?: return ""
        
        val dir = java.io.File(context.filesDir, "audit_photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        val filename = "img_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}.jpg"
        val file = java.io.File(dir, filename)
        
        val fos = java.io.FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos)
        fos.flush()
        fos.close()
        bitmap.recycle()
        
        "file:$filename"
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

fun loadImageStringToBitmap(context: android.content.Context, imageStr: String?, maxDim: Int): android.graphics.Bitmap? {
    if (imageStr.isNullOrEmpty()) return null
    return try {
        if (imageStr.startsWith("file:")) {
            val filename = imageStr.substring(5)
            val dir = java.io.File(context.filesDir, "audit_photos")
            val file = java.io.File(dir, filename)
            if (file.exists()) {
                val path = file.absolutePath
                val options = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                android.graphics.BitmapFactory.decodeFile(path, options)
                
                var inSampleSize = 1
                if (maxDim > 0 && (options.outWidth > maxDim || options.outHeight > maxDim)) {
                    var tempSize = 1
                    val originalMaxSide = Math.max(options.outWidth, options.outHeight)
                    while (originalMaxSide / (tempSize * 2) >= maxDim) {
                        tempSize *= 2
                    }
                    inSampleSize = tempSize
                }
                
                var bmp: android.graphics.Bitmap? = null
                try {
                    val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                        this.inSampleSize = inSampleSize
                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    }
                    bmp = android.graphics.BitmapFactory.decodeFile(path, decodeOptions)
                } catch (oom: java.lang.OutOfMemoryError) {
                    System.gc()
                    try {
                        val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                            this.inSampleSize = inSampleSize
                            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                        }
                        bmp = android.graphics.BitmapFactory.decodeFile(path, decodeOptions)
                    } catch (oom2: java.lang.OutOfMemoryError) {
                        System.gc()
                        val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                            this.inSampleSize = inSampleSize * 2
                            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                        }
                        bmp = android.graphics.BitmapFactory.decodeFile(path, decodeOptions)
                    }
                }
                bmp
            } else {
                null
            }
        } else {
            val cleanStr = if (imageStr.contains(",")) imageStr.split(",")[1] else imageStr
            val bytes = android.util.Base64.decode(cleanStr, android.util.Base64.DEFAULT)
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            
            var inSampleSize = 1
            if (maxDim > 0 && (options.outWidth > maxDim || options.outHeight > maxDim)) {
                var tempSize = 1
                val originalMaxSide = Math.max(options.outWidth, options.outHeight)
                while (originalMaxSide / (tempSize * 2) >= maxDim) {
                    tempSize *= 2
                }
                inSampleSize = tempSize
            }
            
            var bmp: android.graphics.Bitmap? = null
            try {
                val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                }
                bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            } catch (oom: java.lang.OutOfMemoryError) {
                System.gc()
                try {
                    val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                        this.inSampleSize = inSampleSize
                        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                    }
                    bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                } catch (oom2: java.lang.OutOfMemoryError) {
                    System.gc()
                    val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                        this.inSampleSize = inSampleSize * 2
                        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                    }
                    bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                }
            }
            bmp
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun AiInnovationHub(lang: String) {
    val isAr = lang == "ar"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var activeTab by remember { mutableStateOf("intelli") } // "intelli", "vision", "generator", "editing"
    
    // TAB 1 States: Cognitive Intelligence & Grounding / Thinking
    var selectedModel by remember { mutableStateOf("gemini-3.5-flash") }
    var groundingTool by remember { mutableStateOf("none") }
    var isHighThinking by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var intelliResponse by remember { mutableStateOf("") }
    var isLoadIntelli by remember { mutableStateOf(false) }
    var thinkingLogText by remember { mutableStateOf("") }
    
    // TAB 2 States: Vision Inspector
    var selectedPresetIndex by remember { mutableStateOf(1) }
    var imageUriForVision by remember { mutableStateOf<Uri?>(null) }
    var visionPrompt by remember { mutableStateOf("Focus on the structural or quality compliance defects in this inspection resource, and recommend ISO 9001:2015 corrective acts.") }
    var visionResponse by remember { mutableStateOf("") }
    var isLoadVision by remember { mutableStateOf(false) }
    
    // Image selection contract for Vision Photo Upload
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUriForVision = uri
            selectedPresetIndex = -1 // custom uploaded
        }
    }
    
    // TAB 3 States: Image Art Generator
    var genPrompt by remember { mutableStateOf("A modern high-precision ISO 9001 compliance quality testing lab banner, digital art") }
    var selectedSize by remember { mutableStateOf("1K") }
    var generatedImageBase64 by remember { mutableStateOf<String?>(null) }
    var isLoadGen by remember { mutableStateOf(false) }
    
    // TAB 4 States: Pixel Editor
    var editPresetIndex by remember { mutableStateOf(1) }
    var editPrompt by remember { mutableStateOf("Indicate the non-compliance anomaly using bright red warning indicators") }
    var editedImageBase64 by remember { mutableStateOf<String?>(null) }
    var isLoadEdit by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF04122C).copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, Color(0xFF00BFA5).copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF00BFA5).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF00BFA5),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isAr) "مختبر جيميناي للذكاء والصور" else "Gemini Intelligent Agent & Image Lab",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (isAr) "الجيل الجديد من تقنيات التفكير، البحث، والوسائط المتعددة" else "Advanced Cognition, Grounded Search, and Image Tools",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tab Selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    Triple("intelli", Icons.Default.Psychology, if (isAr) "تفكير وبحث" else "Cognitive Chat"),
                    Triple("vision", Icons.Default.PhotoCamera, if (isAr) "تحليل صور" else "Vision Audit"),
                    Triple("generator", Icons.Default.Brush, if (isAr) "توليد صور" else "Image Art"),
                    Triple("editing", Icons.Default.Palette, if (isAr) "تعديل صور" else "Pixel Editor")
                )
                tabs.forEach { (tabId, icon, label) ->
                    val active = activeTab == tabId
                    Button(
                        onClick = { activeTab = tabId },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (active) Color(0xFF00BFA5) else Color.White.copy(alpha = 0.06f),
                            contentColor = if (active) Color.Black else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Render specific Tab Content
            when (activeTab) {
                "intelli" -> {
                    // TAB 1: COGNITIVE INTELLIGENCE
                    Text(
                        text = if (isAr) "مستوى الذكاء ونوع النموذج:" else "Select Intelligence Tier / Model:",
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Models Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair("gemini-3.1-flash-lite-preview", if (isAr) "سريع (Lite)" else "Fast (Lite)"),
                            Pair("gemini-3.5-flash", if (isAr) "عام (Flash)" else "General (Flash)"),
                            Pair("gemini-3.1-pro-preview", if (isAr) "ذكي (Pro)" else "Powerful (Pro)")
                        ).forEach { (mCode, mLabel) ->
                            val isSel = selectedModel == mCode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSel) Color(0xFF00BFA5).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSel) Color(0xFF00BFA5) else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { 
                                        selectedModel = mCode
                                        if (mCode != "gemini-3.1-pro-preview") {
                                            isHighThinking = false
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mLabel,
                                    color = if (isSel) Color(0xFF00BFA5) else Color.White,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Grounding Selector Row
                    Text(
                        text = if (isAr) "أدوات البحث والتقصي الجغرافي (Grounding):" else "Search & Maps Live Grounding:",
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair("none", if (isAr) "بدون" else "None"),
                            Pair("googleSearch", if (isAr) "بحث جوجل 🌐" else "Google Search 🌐"),
                            Pair("googleMaps", if (isAr) "خرائط جوجل 📍" else "Google Maps 📍")
                        ).forEach { (gCode, gLabel) ->
                            val isSel = groundingTool == gCode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSel) Color(0xFF00BFA5).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSel) Color(0xFF00BFA5) else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { groundingTool = gCode }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = gLabel,
                                    color = if (isSel) Color(0xFF00BFA5) else Color.White,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // High Thinking mode toggle row (Enabled only for Pro)
                    if (selectedModel == "gemini-3.1-pro-preview") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF00BFA5).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .clickable { isHighThinking = !isHighThinking }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = if (isHighThinking) Color(0xFF00BFA5) else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAr) "تفعيل التفكير الفائق (Thinking Mode)" else "Enable High Thinking Mode",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                                Text(
                                    text = if (isAr) "يحلل المشاكل المعقدة بعمق عالي وإلغاء قيود الحد الأقصى للمخرجات" else "Thorough logical reasoning & unlimited outputs for hard queries",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 9.sp
                                )
                            }
                            Switch(
                                checked = isHighThinking,
                                onCheckedChange = { isHighThinking = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF00BFA5),
                                    checkedTrackColor = Color(0xFF00BFA5).copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Input text box
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                text = if (isAr) "اسأل جيميناي أي شيء..." else "Ask Gemini anything...",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00BFA5),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.03f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                        ),
                        textStyle = TextStyle(fontSize = 12.5.sp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Submit button
                    Button(
                        onClick = {
                            if (textInput.isBlank()) return@Button
                            isLoadIntelli = true
                            intelliResponse = ""
                            thinkingLogText = ""
                            
                            scope.launch {
                                // Simulate thinking log stream if high thinking is active
                                if (isHighThinking) {
                                    thinkingLogText = if (isAr) {
                                        "⚙️ جاري بدء محرك التفكير العميق...\n- تحليل الأسباب والمخاوف الملازمة لبنود ISO...\n- استرجاع الأدلة وتطابق البنود..."
                                    } else {
                                        "⚙️ Booting High-level logical engine...\n- Synthesizing structural ISO clause metrics...\n- Planning response coordinates..."
                                    }
                                }
                                
                                val res = GeminiService.generateAdvancedResponse(
                                    prompt = textInput,
                                    modelName = selectedModel,
                                    groundingTool = groundingTool,
                                    thinkingLevel = if (isHighThinking) "HIGH" else "none",
                                    context = context
                                )
                                
                                isLoadIntelli = false
                                if (isHighThinking) {
                                    thinkingLogText += if (isAr) {
                                        "\n✅ اكتمل تخطيط التفكير العميق بنجاح!"
                                    } else {
                                        "\n✅ Deep thinking layout constructed!"
                                    }
                                }
                                intelliResponse = res
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoadIntelli && textInput.isNotBlank()
                    ) {
                        if (isLoadIntelli) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isAr) "جاري الاسترجاع والتفكير..." else "Consulting Gemini Core...", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isAr) "إرسال الاستعلام الذكي" else "Submit Smart Query", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    
                    // Response text representation with visual box
                    if (thinkingLogText.isNotEmpty() || intelliResponse.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            if (thinkingLogText.isNotEmpty()) {
                                Text(
                                    text = thinkingLogText,
                                    color = Color(0xFF00BFA5).copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            Text(
                                text = intelliResponse,
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
                
                "vision" -> {
                    // TAB 2: VISION ANALYSIS (Analyze Photo)
                    Text(
                        text = if (isAr) "اختر صورة للمعاينة والتحليل:" else "Select/Upload Inspection Photo or Document:",
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Preset Thumbnails & Upload trigger row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Preset 1: Document Checklist
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(65.dp)
                                .background(Color.DarkGray, RoundedCornerShape(8.dp))
                                .border(
                                    2.dp,
                                    if (selectedPresetIndex == 0) Color(0xFF00BFA5) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { 
                                    selectedPresetIndex = 0
                                    imageUriForVision = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(Color(0xFF2C3E50))
                                drawLine(Color.White, Offset(20f, 20f), Offset(100f, 20f), 3f)
                                drawLine(Color.White, Offset(20f, 40f), Offset(110f, 40f), 3f)
                                drawCircle(Color(0xFF00BFA5), 8f, Offset(115f, 90f))
                            }
                            Text("QMS Doc", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // Preset 2: Structural Crack
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(65.dp)
                                .background(Color.DarkGray, RoundedCornerShape(8.dp))
                                .border(
                                    2.dp,
                                    if (selectedPresetIndex == 1) Color(0xFF00BFA5) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { 
                                    selectedPresetIndex = 1
                                    imageUriForVision = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(Color(0xFF425661))
                                drawLine(Color.Black, Offset(20f, 20f), Offset(60f, 80f), 4f)
                                drawLine(Color.Black, Offset(60f, 80f), Offset(50f, 130f), 4f)
                                drawLine(Color.Black, Offset(50f, 130f), Offset(110f, 160f), 4f)
                            }
                            Text("Crack", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // Preset 3: Electrical Box Sparks
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(65.dp)
                                .background(Color.DarkGray, RoundedCornerShape(8.dp))
                                .border(
                                    2.dp,
                                    if (selectedPresetIndex == 2) Color(0xFF00BFA5) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { 
                                    selectedPresetIndex = 2
                                    imageUriForVision = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(Color(0xFFC0392B))
                                drawLine(Color.Yellow, Offset(80f, 20f), Offset(40f, 100f), 6f)
                                drawLine(Color.Yellow, Offset(40f, 100f), Offset(120f, 100f), 6f)
                            }
                            Text("Electric", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // Custom upload box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(65.dp)
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    if (selectedPresetIndex == -1) Color(0xFF00BFA5) else Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (imageUriForVision != null) "Uploaded" else "Upload",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Display Visual Representation Canvas depending on active preset
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedPresetIndex == 0) {
                            // Interactive Doc draw representation
                            Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                drawRect(Color.White)
                                drawLine(Color(0xFF07142A), Offset(40f, 40f), Offset(350f, 40f), 6f)
                                drawLine(Color.LightGray, Offset(40f, 85f), Offset(300f, 85f), 4f)
                                drawLine(Color.LightGray, Offset(40f, 125f), Offset(280f, 125f), 4f)
                            }
                        } else if (selectedPresetIndex == 1) {
                            // Crack representation drawing
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(Color(0xFF6B7A82))
                                drawLine(Color.Black, Offset(60f, 10f), Offset(180f, 120f), 6f)
                                drawLine(Color.Black, Offset(180f, 120f), Offset(140f, 210f), 6f)
                                drawLine(Color.Black, Offset(140f, 210f), Offset(290f, 260f), 6f)
                            }
                        } else if (selectedPresetIndex == 2) {
                            // Electrical spark risk hazards
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(Color(0xFF962D22))
                                drawLine(Color.Yellow, Offset(180f, 20f), Offset(110f, 150f), 10f)
                                drawLine(Color.Yellow, Offset(110f, 150f), Offset(250f, 150f), 10f)
                                drawLine(Color.Yellow, Offset(250f, 150f), Offset(180f, 20f), 10f)
                            }
                        } else {
                            // Custom selection view
                            val uriForVision = imageUriForVision
                            if (uriForVision != null) {
                                val bmap = remember(uriForVision) {
                                    decodeSampledBitmapFromUri(context, uriForVision, 800)
                                }
                                if (bmap != null) {
                                    Image(
                                        bitmap = bmap.asImageBitmap(),
                                        contentDescription = "Uploaded check resource",
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Text("Image selected successfully 📁", color = Color(0xFF00BFA5), fontSize = 11.sp)
                                }
                            } else {
                                Text(
                                    if (isAr) "يرجى رفع ملف أو تحديد نموذج مسبق للبدء" else "Please select preset or upload a custom image",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Prompt box
                    OutlinedTextField(
                        value = visionPrompt,
                        onValueChange = { visionPrompt = it },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00BFA5),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        ),
                        textStyle = TextStyle(fontSize = 12.sp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Perform Analysis click
                    Button(
                        onClick = {
                            isLoadVision = true
                            visionResponse = ""
                            
                            scope.launch {
                                val currentUri = imageUriForVision
                                val base64Str = if (selectedPresetIndex != -1) {
                                    getPresetBase64(selectedPresetIndex)
                                } else if (currentUri != null) {
                                    withContext(Dispatchers.IO) {
                                        compressUriToBase64(context, currentUri)
                                    }
                                } else {
                                    ""
                                }
                                
                                val res = GeminiService.generateAdvancedResponse(
                                    prompt = visionPrompt,
                                    modelName = "gemini-3.1-pro-preview", // Specified model for image understanding
                                    imageBytesBase64 = base64Str.ifEmpty { null },
                                    context = context
                                )
                                
                                isLoadVision = false
                                visionResponse = res
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoadVision
                    ) {
                        if (isLoadVision) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isAr) "جاري المسح البصري..." else "Analyzing Visual Anomaly...", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isAr) "تحليل الصورة بواسطة جيميناي برو" else "Analyze Image with Gemini 3.1 Pro", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    
                    if (visionResponse.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = visionResponse,
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
                
                "generator" -> {
                    // TAB 3: IMAGE GENERATOR
                    Text(
                        text = if (isAr) "ادخل الوصف التفصيلي ومعاير الأبعاد للتوليد:" else "Specify Text Prompt & Artwork Parameters:",
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        value = genPrompt,
                        onValueChange = { genPrompt = it },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00BFA5),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.02f)
                        ),
                        textStyle = TextStyle(fontSize = 12.sp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Resolutions Row selector (1K, 2K, 4K)
                    Text(
                        text = if (isAr) "دقة ومساحة الصورة المطلوبة:" else "Specify Image Dimensions / Resolution:",
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("1K", "2K", "4K").forEach { sizeOpt ->
                            val isSel = selectedSize == sizeOpt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSel) Color(0xFF00BFA5).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f),
                                        RoundingNormal
                                    )
                                    .border(
                                        1.dp,
                                        if (isSel) Color(0xFF00BFA5) else Color.White.copy(alpha = 0.1f),
                                        RoundingNormal
                                    )
                                    .clickable { selectedSize = sizeOpt }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = sizeOpt, color = if (isSel) Color(0xFF00BFA5) else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        text = when(sizeOpt) {
                                            "1K" -> "1024 x 1024"
                                            "2K" -> "2048 x 2048"
                                            else -> "4096 x 4096"
                                        },
                                        color = Color.LightGray.copy(alpha = 0.5f),
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Submit button
                    Button(
                        onClick = {
                            isLoadGen = true
                            generatedImageBase64 = null
                            
                            scope.launch {
                                val res = GeminiService.generateAdvancedResponse(
                                    prompt = genPrompt,
                                    modelName = "gemini-3-pro-image-preview", // Image Generator Model
                                    imageSize = selectedSize,
                                    isImageGeneration = true,
                                    context = context
                                )
                                
                                isLoadGen = false
                                if (res == "MOCK_IMAGE_FALLBACK") {
                                    generatedImageBase64 = generatePlaceholderPattern(genPrompt, selectedSize)
                                } else if (res.startsWith("SUCCESS_IMAGE:")) {
                                    generatedImageBase64 = res.substringAfter("SUCCESS_IMAGE:")
                                } else {
                                    Toast.makeText(context, res, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoadGen && genPrompt.isNotBlank()
                    ) {
                        if (isLoadGen) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isAr) "توليد الرسوم بنظام جيميناي..." else "Generating high-fidelity 4K canvas...", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Brush, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isAr) "توليد صورة احترافية" else "Generate High-Quality Artwork", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    
                    val imgBase64 = generatedImageBase64
                    if (imgBase64 != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .border(1.5.dp, Color(0xFF00BFA5), RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val bitmap = remember(imgBase64) { base64ToBitmap(imgBase64) }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Gemini 3 Pro Generated Artwork",
                                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text("Error loading generated raster layout.", color = Color.Red, fontSize = 11.sp)
                            }
                        }
                    }
                }
                
                "editing" -> {
                    // TAB 4: IMAGE EDITING
                    Text(
                        text = if (isAr) "حدد صورة المرجعية والتعليمات للتعديل:" else "Select Reference Image & Edit Directives:",
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Thumbnails Choice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(0, if (isAr) "قائمة ISO" else "Checklist", "QMS Checklist"),
                            Triple(1, if (isAr) "رسم التشقق" else "Cracked Pier", "Cracked Pier"),
                            Triple(2, if (isAr) "لوحة مفاتيح" else "Volt Box", "Volt Controller")
                        ).forEach { (idx, l, d) ->
                            val isSel = editPresetIndex == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(45.dp)
                                    .background(
                                        if (isSel) Color(0xFF00BFA5).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.5.dp,
                                        if (isSel) Color(0xFF00BFA5) else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { editPresetIndex = idx }
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(l, color = if (isSel) Color(0xFF00BFA5) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        value = editPrompt,
                        onValueChange = { editPrompt = it },
                        modifier = Modifier.fillMaxWidth().height(70.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00BFA5),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.02f)
                        ),
                        textStyle = TextStyle(fontSize = 12.sp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Trigger Button
                    Button(
                        onClick = {
                            isLoadEdit = true
                            editedImageBase64 = null
                            
                            scope.launch {
                                val baseImg = getPresetBase64(editPresetIndex)
                                val res = GeminiService.generateAdvancedResponse(
                                    prompt = editPrompt,
                                    modelName = "gemini-3.1-flash-image-preview", // Image Editor Model
                                    imageBytesBase64 = baseImg,
                                    isImageEditing = true,
                                    context = context
                                )
                                
                                isLoadEdit = false
                                if (res == "MOCK_IMAGE_FALLBACK") {
                                    editedImageBase64 = generateEditedPlaceholder(editPresetIndex, editPrompt)
                                } else if (res.startsWith("SUCCESS_IMAGE:")) {
                                    editedImageBase64 = res.substringAfter("SUCCESS_IMAGE:")
                                } else {
                                    Toast.makeText(context, res, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoadEdit && editPrompt.isNotBlank()
                    ) {
                        if (isLoadEdit) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isAr) "جاري تعديل الرسم..." else "Executing Pixel Modifications...", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isAr) "تطبيق التعليمات على الصورة" else "Process Quality Image Modification", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    
                    val editBase64 = editedImageBase64
                    if (editBase64 != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .border(1.5.dp, Color(0xFF00BFA5), RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            var bitmap by remember(editBase64) { mutableStateOf<android.graphics.Bitmap?>(null) }
                            LaunchedEffect(editBase64) {
                                try {
                                    val decoded = withContext(Dispatchers.IO) {
                                        base64ToBitmap(editBase64)
                                    }
                                    bitmap = decoded
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            val curBmp = bitmap
                            if (curBmp != null) {
                                Image(
                                    bitmap = curBmp.asImageBitmap(),
                                    contentDescription = "Gemini 3.1 Flash Edited Output",
                                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val RoundingNormal = RoundedCornerShape(8.dp)

fun base64ToBitmap(base64Str: String): android.graphics.Bitmap? {
    return try {
        val decodedBytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
        val options = android.graphics.BitmapFactory.Options().apply {
            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
        }
        android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, options)
    } catch (e: Exception) {
        null
    }
}

fun getPresetBase64(id: Int): String {
    val bitmap = Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()
    
    paint.color = android.graphics.Color.DKGRAY
    canvas.drawRect(0f, 0f, 150f, 150f, paint)
    
    if (id == 0) {
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(20f, 20f, 130f, 130f, paint)
        paint.color = android.graphics.Color.BLUE
        paint.strokeWidth = 4f
        canvas.drawLine(30f, 40f, 120f, 40f, paint)
        canvas.drawLine(30f, 70f, 120f, 70f, paint)
        canvas.drawLine(30f, 100f, 100f, 100f, paint)
    } else if (id == 1) {
        paint.color = android.graphics.Color.GRAY
        canvas.drawRect(10f, 10f, 140f, 140f, paint)
        paint.color = android.graphics.Color.BLACK
        paint.strokeWidth = 3f
        canvas.drawLine(20f, 20f, 50f, 70f, paint)
        canvas.drawLine(50f, 70f, 40f, 110f, paint)
        canvas.drawLine(40f, 110f, 120f, 130f, paint)
    } else {
        paint.color = android.graphics.Color.RED
        paint.strokeWidth = 5f
        canvas.drawLine(75f, 20f, 35f, 90f, paint)
        canvas.drawLine(35f, 90f, 115f, 90f, paint)
        canvas.drawLine(115f, 90f, 75f, 130f, paint)
    }
    
    val outputStream = java.io.ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

fun generatePlaceholderPattern(prompt: String, sizeSpec: String): String {
    val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()
    
    paint.shader = android.graphics.LinearGradient(
        0f, 0f, 300f, 300f,
        android.graphics.Color.parseColor("#091C35"),
        android.graphics.Color.parseColor("#00BFA5"),
        android.graphics.Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, 300f, 300f, paint)
    paint.shader = null
    
    paint.color = android.graphics.Color.parseColor("#1BFFFFFF")
    paint.strokeWidth = 1f
    for (i in 0..300 step 30) {
        canvas.drawLine(i.toFloat(), 0f, i.toFloat(), 300f, paint)
        canvas.drawLine(0f, i.toFloat(), 300f, i.toFloat(), paint)
    }
    
    paint.color = android.graphics.Color.parseColor("#00BFA5")
    paint.strokeWidth = 8f
    paint.style = android.graphics.Paint.Style.STROKE
    canvas.drawRect(8f, 8f, 292f, 292f, paint)
    paint.style = android.graphics.Paint.Style.FILL
    
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 14f
    paint.isAntiAlias = true
    canvas.drawText("⚡ GEMINI AI LAB", 30f, 50f, paint)
    canvas.drawText("MODE: GENERATION", 30f, 80f, paint)
    canvas.drawText("RESOLUTION: ${sizeSpec}", 30f, 110f, paint)
    
    paint.color = android.graphics.Color.parseColor("#D9FFFFFF")
    paint.textSize = 11f
    val words = prompt.split(" ")
    var line = ""
    var y = 160f
    for (word in words) {
        if (paint.measureText(line + word) > 240) {
            canvas.drawText(line, 30f, y, paint)
            line = "$word "
            y += 20f
        } else {
            line += "$word "
        }
    }
    canvas.drawText(line, 30f, y, paint)
    
    canvas.drawText("[PREVIEW ONLY - CONFIGURE API KEY]", 30f, 270f, paint)
    
    val outputStream = java.io.ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

fun generateEditedPlaceholder(id: Int, prompt: String): String {
    val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()
    
    paint.color = android.graphics.Color.DKGRAY
    canvas.drawRect(0f, 0f, 300f, 300f, paint)
    
    if (id == 0) {
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(40f, 40f, 260f, 260f, paint)
        paint.color = android.graphics.Color.BLUE
        paint.strokeWidth = 8f
        canvas.drawLine(60f, 80f, 240f, 80f, paint)
        canvas.drawLine(60f, 140f, 240f, 140f, paint)
        canvas.drawLine(60f, 200f, 200f, 200f, paint)
    } else if (id == 1) {
        paint.color = android.graphics.Color.GRAY
        canvas.drawRect(20f, 20f, 280f, 280f, paint)
        paint.color = android.graphics.Color.BLACK
        paint.strokeWidth = 6f
        canvas.drawLine(40f, 40f, 100f, 140f, paint)
        canvas.drawLine(100f, 140f, 80f, 220f, paint)
        canvas.drawLine(80f, 220f, 240f, 260f, paint)
    } else {
        paint.color = android.graphics.Color.RED
        paint.strokeWidth = 10f
        canvas.drawLine(150f, 40f, 70f, 180f, paint)
        canvas.drawLine(70f, 180f, 230f, 180f, paint)
        canvas.drawLine(230f, 180f, 150f, 260f, paint)
    }
    
    paint.color = android.graphics.Color.RED
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 10f
    canvas.drawCircle(150f, 150f, 80f, paint)
    
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = android.graphics.Color.RED
    paint.textSize = 13f
    paint.isAntiAlias = true
    canvas.drawText("✏️ [EDIT APPLIED]", 30f, 30f, paint)
    
    paint.color = android.graphics.Color.YELLOW
    paint.textSize = 10f
    canvas.drawText("Action: $prompt", 30f, 290f, paint)
    
    val outputStream = java.io.ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

