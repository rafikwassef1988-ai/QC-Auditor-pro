package com.example.ui

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.utils.DocumentExporter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class PendingExport(
    val fileType: String, // PDF, Word, PowerPoint, Excel
    val tempFile: File,
    val mimeType: String,
    val selectedTrade: String = "All Trades"
)

class AuditViewModel(private val repository: AuditRepository) : ViewModel() {

    // Pending export flow for dynamic location prompt
    private val _pendingExport = MutableStateFlow<PendingExport?>(null)
    val pendingExport: StateFlow<PendingExport?> = _pendingExport.asStateFlow()

    private val _isExporting = MutableStateFlow<Boolean>(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun clearPendingExport() {
        _pendingExport.value = null
    }

    // List of all local reports
    val allReports: StateFlow<List<AuditReport>> = repository.allReports
        .catch { e ->
            e.printStackTrace()
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // List of all local findings
    val allFindings: StateFlow<List<FindingOverview>> = repository.allFindings
        .catch { e ->
            e.printStackTrace()
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // List of all previous audits across all reports
    val allHistoryRows: StateFlow<List<PreviousAuditRow>> = repository.allHistoryRows
        .catch { e ->
            e.printStackTrace()
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current report detail under editing
    private val _activeReport = MutableStateFlow<AuditReportWithDetails?>(null)
    val activeReport: StateFlow<AuditReportWithDetails?> = _activeReport.asStateFlow()

    init {
        // Initialize with a default template report
        createNewBlankReport()
    }

    fun createNewBlankReport() {
        val report = AuditReport(
            projectName = "",
            auditNumber = "",
            auditDate = "",
            reportIssuanceDate = "",
            followupDueDate = "",
            formReference = "",
            sigAuditorDate = "",
            sigReviewerDate = "",
            auditeeResponseDate = "",
            auditeeProposedClosureDate = "",
            auditeeSigDate = "",
            auditeeSupDate = "",
            reviewerDate = ""
        )

        _activeReport.value = AuditReportWithDetails(
            report = report,
            findings = emptyList(),
            historyRows = emptyList()
        )
    }

    private suspend fun loadReportSuspend(reportId: Int) {
        try {
            val details = repository.getReportWithDetails(reportId)
            if (details != null) {
                _activeReport.value = details
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadReport(reportId: Int) {
        viewModelScope.launch {
            try {
                loadReportSuspend(reportId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveReport(context: Context, onComplete: () -> Unit = {}) {
        val details = _activeReport.value ?: return
        viewModelScope.launch {
            try {
                val savedId = repository.saveReport(details)
                // Reload the report with generated Room ID sequentially
                loadReportSuspend(savedId)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context.applicationContext, "Audit Report Saved Successfully", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context.applicationContext, "Error saving report: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun deleteReport(reportId: Int, context: Context) {
        viewModelScope.launch {
            repository.deleteReport(reportId)
            Toast.makeText(context.applicationContext, "Audit Report Deleted", Toast.LENGTH_SHORT).show()
            // If we deleted the active report, create a new blank one
            if (_activeReport.value?.report?.id == reportId) {
                createNewBlankReport()
            }
        }
    }

    // Live state field updaters for smooth Composable forms binding
    fun updateReport(block: (AuditReport) -> AuditReport) {
        val current = _activeReport.value ?: return
        val updatedReport = block(current.report)
        
        // Auto update reference IDs prefix if project name changes and finding uses auto-pattern
        val findings = if (updatedReport.projectName != current.report.projectName) {
            val clean = updatedReport.projectName.trim().replace(Regex("[^a-zA-Z0-9]"), "")
            val prefix = if (clean.length >= 2) {
                clean.take(2).uppercase()
            } else if (clean.isNotEmpty()) {
                clean.take(1).uppercase() + "X"
            } else {
                "QC"
            }
            val pattern = Regex("^[A-Z0-9]{2,3}-(NCR|OBS)-\\d+$")
            current.findings.mapIndexed { idx, f ->
                if (f.referenceId.isEmpty() || f.referenceId == "TP-NCR-${1000 + idx + 1}" || f.referenceId == "TP-OBS-${1000 + idx + 1}" || pattern.matches(f.referenceId)) {
                    f.copy(referenceId = "$prefix-${f.type}-${1000 + idx + 1}")
                } else {
                    f
                }
            }
        } else {
            current.findings
        }
        
        _activeReport.value = current.copy(report = updatedReport, findings = findings)
    }

    fun addFinding() {
        val current = _activeReport.value ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // Initialize prefix dynamically based on first 2 characters of project name
        val projectName = current.report.projectName
        val clean = projectName.trim().replace(Regex("[^a-zA-Z0-9]"), "")
        val prefix = if (clean.length >= 2) {
            clean.take(2).uppercase()
        } else if (clean.isNotEmpty()) {
            clean.take(1).uppercase() + "X"
        } else {
            "QC"
        }
        val type = "NCR"
        val nextNum = 1000 + current.findings.size + 1
        val refId = "$prefix-$type-$nextNum"
        
        val newFinding = Finding(
            reportId = current.report.id,
            referenceId = refId,
            type = type,
            severity = "Major",
            status = "Open",
            issueDate = today,
            dueDate = today
        )
        _activeReport.value = current.copy(findings = current.findings + newFinding)
    }

    fun updateFinding(index: Int, block: (Finding) -> Finding) {
        val current = _activeReport.value ?: return
        val list = current.findings.toMutableList()
        if (index in list.indices) {
            val oldFinding = list[index]
            val updated = block(oldFinding)
            
            // Auto update reference ID if it follows any standard template prefix
            val finalFinding = if (updated.type != oldFinding.type || updated.referenceId.isEmpty()) {
                val projectName = current.report.projectName
                val clean = projectName.trim().replace(Regex("[^a-zA-Z0-9]"), "")
                val prefix = if (clean.length >= 2) {
                    clean.take(2).uppercase()
                } else if (clean.isNotEmpty()) {
                    clean.take(1).uppercase() + "X"
                } else {
                    "QC"
                }
                val pattern = Regex("^[A-Z0-9]{2,3}-(NCR|OBS)-\\d+$")
                if (updated.referenceId.isEmpty() || updated.referenceId == "TP-NCR-${1000 + index + 1}" || updated.referenceId == "TP-OBS-${1000 + index + 1}" || pattern.matches(updated.referenceId)) {
                    updated.copy(referenceId = "$prefix-${updated.type}-${1000 + index + 1}")
                } else {
                    updated
                }
            } else {
                updated
            }
            
            list[index] = finalFinding
            _activeReport.value = current.copy(findings = list)
        }
    }

    fun removeFinding(index: Int) {
        val current = _activeReport.value ?: return
        val list = current.findings.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _activeReport.value = current.copy(findings = list)
        }
    }

    fun addHistoryRow(auditNum: String, date: String, ncrT: Int, ncrC: Int, obsT: Int, obsC: Int, auditor: String) {
        val current = _activeReport.value ?: return
        val row = PreviousAuditRow(
            reportId = current.report.id,
            auditNumber = auditNum,
            auditDate = date,
            ncrsIssued = ncrT,
            ncrsClosed = ncrC,
            obsIssued = obsT,
            obsClosed = obsC,
            auditorName = auditor
        )
        _activeReport.value = current.copy(historyRows = current.historyRows + row)
    }

    fun removeHistoryRow(index: Int) {
        val current = _activeReport.value ?: return
        val list = current.historyRows.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _activeReport.value = current.copy(historyRows = list)
        }
    }

    // Sync auditee findings responds from auditor findings
    fun syncAuditeeFindingsFromAuditor() {
        val current = _activeReport.value ?: return
        val updatedFindings = current.findings.map { finding ->
            if (finding.auditeeResponse.isEmpty()) {
                finding.copy(
                    auditeeStatus = finding.status,
                    auditeeTargetDate = finding.dueDate
                )
            } else {
                finding
            }
        }
        _activeReport.value = current.copy(findings = updatedFindings)
    }

    fun loadSampleReportFromImage() {
        val report = AuditReport(
            id = _activeReport.value?.report?.id ?: 0,
            auditNumber = "255",
            auditDate = "11/06/2026 1Day",
            projectName = "The French University",
            projectNumber = "SA36",
            location = "El Shrouk City",
            projectManager = "Mikhael Telmeez",
            qcManager = "Mina Melad",
            contractor = "Innovo Construction",
            projectType = "Educational Building",
            phase = "Concrete Structure",
            durationDays = 1,
            auditorName = "Rafik Hisham",
            reportIssuanceDate = "14/06/2026",
            followupDueDate = "11/07/2026",
            formReference = "innovo/QAQC/FRM - 1.14/05 REV 02",
            auditScope = "Building A",
            sigAuditorName = "Rafik Hisham",
            sigAuditorDesignation = "Quality Auditor",
            sigAuditorDate = "14/06/2026",
            sigReviewerName = "Moamen Othman",
            sigReviewerDesignation = "Quality Director",
            sigReviewerDate = "15/06/2026"
        )
        
        val findings = listOf(
            Finding(
                reportId = report.id,
                referenceId = "FU-NCR-0001",
                type = "NCR",
                severity = "Major",
                status = "Closed",
                trade = "Structure",
                activity = "Columns Dimension",
                locationZone = "Building A",
                description = "variations in concrete column cross-sectional dimensions (differing widths, depths, or reinforcement layouts) across its structural framework at building A.",
                negativeImpact = "1-Uneven Load Distribution\n2-Seismic Vulnerability\n3-Code Violations\n4-Contractual Penalties\n5-Future Liability",
                materialLosses = "-Cost of wasted concrete from demolition/ chiseling of non-compliant columns, Excess reinforcement steel, Additional formwork or and test of required inspections and manpower required.",
                correctiveAction = "Rework structural sizing and perform structural testing on columns.",
                rootCause = "Inadequate quality oversight",
                issueDate = "11/06/2026",
                dueDate = "11/07/2026"
            ),
            Finding(
                reportId = report.id,
                referenceId = "FU-NCR-0003",
                type = "NCR",
                severity = "Major",
                status = "Open",
                trade = "Structure",
                activity = "Concrete Casting",
                locationZone = "Building A",
                description = "Non-Compliance with Concrete Casting Procedures Leading to Uneven Surfaces in Building A.",
                negativeImpact = "1-Visible defects lead to rejection of work or contractual penalties.\n2-Exposes rebar",
                materialLosses = "-Cost of wasted concrete from removal of uneven sections, Excess grinding pads, epoxy leveling compounds, or repair mortars and Manpower required.",
                correctiveAction = "Grind surface to level, apply high-strength repair mortar.",
                rootCause = "Poor supervision during casting",
                issueDate = "11/06/2026",
                dueDate = "11/07/2026"
            ),
            Finding(
                reportId = report.id,
                referenceId = "FU-NCR-0004",
                type = "NCR",
                severity = "Major",
                status = "Open",
                trade = "Structure",
                activity = "Column Beams Detailing",
                locationZone = "Building A",
                description = "Discrepancy in Column-Beam Joint Detailing at Building A",
                negativeImpact = "1-Creates potential weak points in moment-resisting frames\n2-Increases risk of cracking at connections",
                materialLosses = "-Cost of Additional materials for reinforcement corrections, partial demolition and Manpower required.",
                correctiveAction = "Reinstall rebar corrections before next casting.",
                rootCause = "Faulty design routing details execution review",
                issueDate = "11/06/2026",
                dueDate = "11/07/2026"
            ),
            Finding(
                reportId = report.id,
                referenceId = "FU-NCR-0005",
                type = "NCR",
                severity = "Major",
                status = "Open",
                trade = "Structure",
                activity = "Stair Flight Alignment",
                locationZone = "Building A",
                description = "Stair Flight Misalignment at Building A",
                negativeImpact = "1- Visible defects lead to rejection of work or contractual penalties\n2- Handrail usability",
                materialLosses = "-Cost of additional repair materials, wasted concrete from removal of unaligned parts and labor costs",
                correctiveAction = "Re-cast the unaligned stairs sections.",
                rootCause = "Inadequate leveling and layout surveying",
                issueDate = "11/06/2026",
                dueDate = "11/07/2026"
            ),
            Finding(
                reportId = report.id,
                referenceId = "FU-NCR-0006",
                type = "NCR",
                severity = "Major",
                status = "Open",
                trade = "Structure",
                activity = "Slab Formwork",
                locationZone = "Building A",
                description = "Improper Slab formwork installation at building A",
                negativeImpact = "1-Structural Misalignment\n2-Support Deficiencies\n3-Safety Violations",
                materialLosses = "-Cost of formwork removal and reinstallation.",
                correctiveAction = "Remove formwork and reinstall under safety check.",
                rootCause = "Work force negligence",
                issueDate = "11/06/2026",
                dueDate = "11/07/2026"
            ),
            Finding(
                reportId = report.id,
                referenceId = "FU-NCR-0007",
                type = "NCR",
                severity = "Major",
                status = "Open",
                trade = "Structure",
                activity = "Slab Casting",
                locationZone = "Building A",
                description = "Improper quality of reinforced concrete slab casting:\n1-Exposed rebar/tie wire projecting\n2-Honeycombing & open form-tie voids\n3-Debris of wood and paper on working platforms across the side elevation of building A.",
                negativeImpact = "1-Visible defects lead to rejection of work or contractual penalties.\n2-Exposes rebar & weakens structural strength.",
                materialLosses = "Cost of patch-up mortar, grinding, and inspection.",
                correctiveAction = "All exposed structural rebars and tie wires shall be cut & patched with approved high-strength non-shrink grout. Clean all debris from platform.",
                rootCause = "Poor cleanliness and inspection before casting slab",
                issueDate = "14/06/2026",
                dueDate = "30/06/2026"
            ),
            Finding(
                reportId = report.id,
                referenceId = "FU-OBS-0001",
                type = "OBS",
                severity = "Minor",
                status = "Open",
                trade = "Structure",
                activity = "Housekeeping",
                locationZone = "Building A Layout",
                description = "Observation of minor dust and material stockpiles adjacent to access paths.",
                negativeImpact = "Minor safety hazard and site cleanliness obstruction.",
                materialLosses = "None.",
                correctiveAction = "Clean materials and clear access paths daily.",
                rootCause = "Inadequate daily housekeeping routines",
                issueDate = "11/06/2026",
                dueDate = "11/07/2026"
            )
        )
        
        _activeReport.value = AuditReportWithDetails(
            report = report,
            findings = findings,
            historyRows = emptyList()
        )
    }

    // Exporters & Share Helpers
    private fun saveFileToDownloads(context: Context, tempFile: File, mimeType: String): Boolean {
        if (!tempFile.exists() || tempFile.length() == 0L) {
            return false
        }
        try {
            val resolver = context.contentResolver
            val fileName = tempFile.name
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        tempFile.inputStream().use { inputStream ->
                            inputStream.copyTo(os)
                        }
                    }
                    return true
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val targetFile = File(downloadsDir, fileName)
                tempFile.inputStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun getSafeFile(context: Context, projectName: String, auditNumber: String, extension: String): File {
        val cleanProject = projectName.ifEmpty { "Audit" }
            .replace("[\\\\/:*?\"<>|]".toRegex(), "-")
            .replace("\\s+".toRegex(), "-")
        val cleanAudit = auditNumber
            .replace("[\\\\/:*?\"<>|]".toRegex(), "-")
            .replace("\\s+".toRegex(), "-")
        val fileName = "QC-Audit-${cleanProject}-${cleanAudit.ifEmpty { "unassigned" }}.$extension"
        return File(context.cacheDir, fileName)
    }

    fun savePendingToDownloads(context: Context): Boolean {
        val pending = _pendingExport.value ?: return false
        val success = saveFileToDownloads(context, pending.tempFile, pending.mimeType)
        _pendingExport.value = null
        return success
    }

    fun exportPdf(context: Context, selectedTrade: String = "All Trades") {
        val details = _activeReport.value ?: return
        val cacheFile = getSafeFile(context, details.report.projectName, details.report.auditNumber, "pdf")
        
        viewModelScope.launch {
            _isExporting.value = true
            try {
                withContext(Dispatchers.IO) {
                    if (cacheFile.exists()) { cacheFile.delete() }
                    DocumentExporter.exportToPdf(context, details, cacheFile, selectedTrade)
                }
                if (cacheFile.exists() && cacheFile.length() > 0L) {
                    _pendingExport.value = PendingExport("PDF Document", cacheFile, "application/pdf", selectedTrade)
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context.applicationContext, "PDF generation failed - empty file created", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context.applicationContext, "PDF Generation Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportDoc(context: Context) {
        val details = _activeReport.value ?: return
        val cacheFile = getSafeFile(context, details.report.projectName, details.report.auditNumber, "docx")
        
        viewModelScope.launch {
            _isExporting.value = true
            try {
                withContext(Dispatchers.IO) {
                    if (cacheFile.exists()) { cacheFile.delete() }
                    DocumentExporter.exportToDoc(context, details, cacheFile)
                }
                if (cacheFile.exists() && cacheFile.length() > 0L) {
                    _pendingExport.value = PendingExport("Word Document (.docx)", cacheFile, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context.applicationContext, "DOCX generation failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context.applicationContext, "DOCX Export Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportPptx(context: Context) {
        val details = _activeReport.value ?: return
        val cacheFile = getSafeFile(context, details.report.projectName, details.report.auditNumber, "pptx")
        
        viewModelScope.launch {
            _isExporting.value = true
            try {
                withContext(Dispatchers.IO) {
                    if (cacheFile.exists()) { cacheFile.delete() }
                    DocumentExporter.exportToPptx(context, details, cacheFile)
                }
                if (cacheFile.exists() && cacheFile.length() > 0L) {
                    _pendingExport.value = PendingExport("PowerPoint (.pptx)", cacheFile, "application/vnd.openxmlformats-officedocument.presentationml.presentation")
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context.applicationContext, "PPTX generation failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context.applicationContext, "PPTX Export Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportXlsx(context: Context) {
        val details = _activeReport.value ?: return
        val cacheFile = getSafeFile(context, details.report.projectName, details.report.auditNumber, "xlsx")
        
        viewModelScope.launch {
            _isExporting.value = true
            try {
                withContext(Dispatchers.IO) {
                    if (cacheFile.exists()) { cacheFile.delete() }
                    DocumentExporter.exportToXlsx(context, details, cacheFile)
                }
                if (cacheFile.exists() && cacheFile.length() > 0L) {
                    _pendingExport.value = PendingExport("Excel Spreadsheet (.xlsx)", cacheFile, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context.applicationContext, "Excel generation failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context.applicationContext, "Excel Export Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun sharePdf(context: Context, selectedTrade: String = "All Trades") {
        val details = _activeReport.value ?: return
        val file = getSafeFile(context, details.report.projectName, details.report.auditNumber, "pdf")
        
        viewModelScope.launch {
            _isExporting.value = true
            try {
                withContext(Dispatchers.IO) {
                    if (file.exists()) { file.delete() }
                    DocumentExporter.exportToPdf(context, details, file, selectedTrade)
                }
                if (file.exists() && file.length() > 0L) {
                    withContext(Dispatchers.Main) {
                        shareFile(context, file, "application/pdf")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context.applicationContext, "PDF Sharing Failed - file generation failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context.applicationContext, "PDF Sharing Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun shareDoc(context: Context) {
        val details = _activeReport.value ?: return
        val file = getSafeFile(context, details.report.projectName, details.report.auditNumber, "docx")
        
        viewModelScope.launch {
            _isExporting.value = true
            try {
                withContext(Dispatchers.IO) {
                    if (file.exists()) { file.delete() }
                    DocumentExporter.exportToDoc(context, details, file)
                }
                if (file.exists() && file.length() > 0L) {
                    withContext(Dispatchers.Main) {
                        shareFile(context, file, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context.applicationContext, "DOCX Sharing Failed - file generation failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context.applicationContext, "DOCX Sharing Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun sharePptx(context: Context) {
        val details = _activeReport.value ?: return
        val file = getSafeFile(context, details.report.projectName, details.report.auditNumber, "pptx")
        
        viewModelScope.launch {
            _isExporting.value = true
            try {
                withContext(Dispatchers.IO) {
                    if (file.exists()) { file.delete() }
                    DocumentExporter.exportToPptx(context, details, file)
                }
                if (file.exists() && file.length() > 0L) {
                    withContext(Dispatchers.Main) {
                        shareFile(context, file, "application/vnd.openxmlformats-officedocument.presentationml.presentation")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context.applicationContext, "PPTX Sharing Failed - file generation failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context.applicationContext, "PPTX Sharing Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun shareXlsx(context: Context) {
        val details = _activeReport.value ?: return
        val file = getSafeFile(context, details.report.projectName, details.report.auditNumber, "xlsx")
        
        viewModelScope.launch {
            _isExporting.value = true
            try {
                withContext(Dispatchers.IO) {
                    if (file.exists()) { file.delete() }
                    DocumentExporter.exportToXlsx(context, details, file)
                }
                if (file.exists() && file.length() > 0L) {
                    withContext(Dispatchers.Main) {
                        shareFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context.applicationContext, "Excel Sharing Failed - file generation failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context.applicationContext, "Excel Sharing Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    private fun addPhotoToZip(context: Context, zipOut: java.util.zip.ZipOutputStream, photoStr: String?, entryName: String) {
        if (photoStr.isNullOrBlank()) return
        try {
            if (photoStr.startsWith("file:")) {
                val filename = photoStr.substring(5)
                val dir = java.io.File(context.filesDir, "audit_photos")
                val file = java.io.File(dir, filename)
                if (file.exists()) {
                    zipOut.putNextEntry(java.util.zip.ZipEntry(entryName))
                    file.inputStream().use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
            } else {
                val cleanBase64 = if (photoStr.startsWith("data:image")) {
                    val commaIdx = photoStr.indexOf(",")
                    if (commaIdx != -1) photoStr.substring(commaIdx + 1) else photoStr
                } else {
                    photoStr
                }
                val decodedBytes = android.util.Base64.decode(cleanBase64.trim(), android.util.Base64.DEFAULT)
                if (decodedBytes.isNotEmpty()) {
                    zipOut.putNextEntry(java.util.zip.ZipEntry(entryName))
                    zipOut.write(decodedBytes)
                    zipOut.closeEntry()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writeAuditToZip(context: Context, details: AuditReportWithDetails, pdfFile: File, zipFile: File) {
        java.util.zip.ZipOutputStream(java.io.FileOutputStream(zipFile)).use { zipOut ->
            if (pdfFile.exists() && pdfFile.length() > 0L) {
                zipOut.putNextEntry(java.util.zip.ZipEntry(pdfFile.name))
                pdfFile.inputStream().use { input ->
                    input.copyTo(zipOut)
                }
                zipOut.closeEntry()
            }
            
            addPhotoToZip(context, zipOut, details.report.sigAuditorPh, "photos/auditor_signature.jpg")
            addPhotoToZip(context, zipOut, details.report.sigReviewerPh, "photos/reviewer_signature.jpg")
            addPhotoToZip(context, zipOut, details.report.auditeeSigPh, "photos/auditee_signature.jpg")
            addPhotoToZip(context, zipOut, details.report.auditeeSupPh, "photos/auditee_supervisor_signature.jpg")
            
            addPhotoToZip(context, zipOut, details.report.auditeePhRef1, "photos/auditee_reference_photo_1.jpg")
            addPhotoToZip(context, zipOut, details.report.auditeePhRef2, "photos/auditee_reference_photo_2.jpg")
            addPhotoToZip(context, zipOut, details.report.auditeePhRef3, "photos/auditee_reference_photo_3.jpg")
            addPhotoToZip(context, zipOut, details.report.auditeePhRef4, "photos/auditee_reference_photo_4.jpg")
            
            details.findings.forEachIndexed { idx, fi ->
                val fNum = idx + 1
                val refId = fi.referenceId.ifEmpty { "Finding-$fNum" }
                    .replace("[\\\\/:*?\"<>|]".toRegex(), "-")
                    .replace("\\s+".toRegex(), "-")
                
                addPhotoToZip(context, zipOut, fi.ph1Base64, "photos/${refId}_photo_1.jpg")
                addPhotoToZip(context, zipOut, fi.ph2Base64, "photos/${refId}_photo_2.jpg")
                addPhotoToZip(context, zipOut, fi.ph3Base64, "photos/${refId}_photo_3.jpg")
                addPhotoToZip(context, zipOut, fi.ph4Base64, "photos/${refId}_photo_4.jpg")
                addPhotoToZip(context, zipOut, fi.auditeeClosurePhoto, "photos/${refId}_closure_photo.jpg")
            }
        }
    }

    fun exportZip(context: Context) {
        val details = _activeReport.value ?: return
        val cacheFile = getSafeFile(context, details.report.projectName, details.report.auditNumber, "zip")
        
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val tempPdfFile = getSafeFile(context, details.report.projectName, details.report.auditNumber, "pdf")
                withContext(Dispatchers.IO) {
                    if (cacheFile.exists()) { cacheFile.delete() }
                    if (tempPdfFile.exists()) { tempPdfFile.delete() }
                    DocumentExporter.exportToPdf(context, details, tempPdfFile, "All Trades")
                    
                    if (tempPdfFile.exists() && tempPdfFile.length() > 0L) {
                        writeAuditToZip(context, details, tempPdfFile, cacheFile)
                        try { tempPdfFile.delete() } catch (ignored: Exception) {}
                    }
                }
                
                if (cacheFile.exists() && cacheFile.length() > 0L) {
                    _pendingExport.value = PendingExport("Zip Package (.zip)", cacheFile, "application/zip")
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context.applicationContext, "ZIP generation failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context.applicationContext, "ZIP Export Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun shareZip(context: Context) {
        val details = _activeReport.value ?: return
        val file = getSafeFile(context, details.report.projectName, details.report.auditNumber, "zip")
        
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val tempPdfFile = getSafeFile(context, details.report.projectName, details.report.auditNumber, "pdf")
                withContext(Dispatchers.IO) {
                    if (file.exists()) { file.delete() }
                    if (tempPdfFile.exists()) { tempPdfFile.delete() }
                    DocumentExporter.exportToPdf(context, details, tempPdfFile, "All Trades")
                    
                    if (tempPdfFile.exists() && tempPdfFile.length() > 0L) {
                        writeAuditToZip(context, details, tempPdfFile, file)
                        try { tempPdfFile.delete() } catch (ignored: Exception) {}
                    }
                }
                
                if (file.exists() && file.length() > 0L) {
                    withContext(Dispatchers.Main) {
                        shareFile(context, file, "application/zip")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context.applicationContext, "ZIP Sharing Failed - file generation failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context.applicationContext, "ZIP Sharing Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportZipForReport(context: Context, reportId: Int) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val details = repository.getReportWithDetails(reportId)
                if (details != null) {
                    val cacheFile = getSafeFile(context, details.report.projectName, details.report.auditNumber, "zip")
                    if (cacheFile.exists()) { cacheFile.delete() }
                    
                    val tempPdfFile = getSafeFile(context, details.report.projectName, details.report.auditNumber, "pdf")
                    if (tempPdfFile.exists()) { tempPdfFile.delete() }
                    
                    withContext(Dispatchers.IO) {
                        DocumentExporter.exportToPdf(context, details, tempPdfFile, "All Trades")
                    }
                    
                    if (tempPdfFile.exists() && tempPdfFile.length() > 0L) {
                        withContext(Dispatchers.IO) {
                            writeAuditToZip(context, details, tempPdfFile, cacheFile)
                        }
                        try { tempPdfFile.delete() } catch (ignored: Exception) {}
                        
                        if (cacheFile.exists() && cacheFile.length() > 0L) {
                            _pendingExport.value = PendingExport("Zip Package (.zip)", cacheFile, "application/zip")
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context.applicationContext, "ZIP generation failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context.applicationContext, "Failed to generate PDF for ZIP", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context.applicationContext, "ZIP Export Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun shareZipForReport(context: Context, reportId: Int) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val details = repository.getReportWithDetails(reportId)
                if (details != null) {
                    val file = getSafeFile(context, details.report.projectName, details.report.auditNumber, "zip")
                    if (file.exists()) { file.delete() }
                    
                    val tempPdfFile = getSafeFile(context, details.report.projectName, details.report.auditNumber, "pdf")
                    if (tempPdfFile.exists()) { tempPdfFile.delete() }
                    
                    withContext(Dispatchers.IO) {
                        DocumentExporter.exportToPdf(context, details, tempPdfFile, "All Trades")
                    }
                    
                    if (tempPdfFile.exists() && tempPdfFile.length() > 0L) {
                        withContext(Dispatchers.IO) {
                            writeAuditToZip(context, details, tempPdfFile, file)
                        }
                        try { tempPdfFile.delete() } catch (ignored: Exception) {}
                        
                        if (file.exists() && file.length() > 0L) {
                            withContext(Dispatchers.Main) {
                                shareFile(context, file, "application/zip")
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context.applicationContext, "ZIP Sharing Failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context.applicationContext, "Failed to generate PDF for ZIP", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context.applicationContext, "ZIP Sharing Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportPdfForReport(context: Context, reportId: Int, selectedTrade: String = "All Trades") {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val details = repository.getReportWithDetails(reportId)
                if (details != null) {
                    val cacheFile = getSafeFile(context, details.report.projectName, details.report.auditNumber, "pdf")
                    withContext(Dispatchers.IO) {
                        if (cacheFile.exists()) { cacheFile.delete() }
                        DocumentExporter.exportToPdf(context, details, cacheFile, selectedTrade)
                    }
                    if (cacheFile.exists() && cacheFile.length() > 0L) {
                        _pendingExport.value = PendingExport("PDF Document", cacheFile, "application/pdf", selectedTrade)
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context.applicationContext, "PDF generation failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context.applicationContext, "PDF Export Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun sharePdfForReport(context: Context, reportId: Int, selectedTrade: String = "All Trades") {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val details = repository.getReportWithDetails(reportId)
                if (details != null) {
                    val file = getSafeFile(context, details.report.projectName, details.report.auditNumber, "pdf")
                    withContext(Dispatchers.IO) {
                        if (file.exists()) { file.delete() }
                        DocumentExporter.exportToPdf(context, details, file, selectedTrade)
                    }
                    if (file.exists() && file.length() > 0L) {
                        withContext(Dispatchers.Main) {
                            shareFile(context, file, "application/pdf")
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context.applicationContext, "PDF Sharing Failed - file generation failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context.applicationContext, "PDF Sharing Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = Intent.createChooser(intent, "Share Report / مشاركة التقرير")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context.applicationContext, "Error launching share sheet: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun seedExampleAudits() {
        viewModelScope.launch {
            val sampleAudits = listOf(
                AuditReportWithDetails(
                    report = AuditReport(
                        auditNumber = "2024-Q2-01",
                        projectName = "Q2 Internal Audit 2024",
                        projectType = "Internal Audit",
                        location = "Finance Department",
                        auditorName = "Internal Auditor",
                        auditeeOverallStatus = "Draft",
                        auditDate = "20/05/2024",
                        reportIssuanceDate = "20/05/2024",
                        followupDueDate = "20/06/2024"
                    ),
                    findings = listOf(
                        Finding(reportId = 0, referenceId = "FI-NCR-001", type = "NCR", status = "Open", trade = "Finance", activity = "Asset Review", description = "Discrepancy in quarterly asset depreciation calculations"),
                        Finding(reportId = 0, referenceId = "FI-NCR-002", type = "NCR", status = "Open", trade = "Finance", activity = "Invoicing", description = "Unreconciled invoice records for vendors"),
                        Finding(reportId = 0, referenceId = "FI-OBS-001", type = "OBS", status = "Open", trade = "Administration", activity = "Record Keeping", description = "Paper-based archives slow retrieval times")
                    ),
                    historyRows = listOf(
                        PreviousAuditRow(reportId = 0, auditNumber = "2023-Q4", auditDate = "15/12/2023", ncrsIssued = 1, ncrsClosed = 1, obsIssued = 2, obsClosed = 2, auditorName = "Internal Auditor")
                    )
                ),
                AuditReportWithDetails(
                    report = AuditReport(
                        auditNumber = "2024-P-02",
                        projectName = "Procurement Process Audit",
                        projectType = "Process Audit",
                        location = "Procurement Department",
                        auditorName = "Sarah Johnson",
                        auditeeOverallStatus = "Ready to Submit",
                        auditDate = "18/05/2024",
                        reportIssuanceDate = "18/05/2024",
                        followupDueDate = "18/06/2024"
                    ),
                    findings = listOf(
                        Finding(reportId = 0, referenceId = "PR-NCR-001", type = "NCR", status = "Open", trade = "Logistics", activity = "Purchase Orders", description = "Missing lead-time signatures for urgent orders"),
                        Finding(reportId = 0, referenceId = "PR-OBS-001", type = "OBS", status = "Closed", trade = "Procurement", activity = "Sourcing", description = "Supplier evaluations have minor record gaps")
                    ),
                    historyRows = listOf(
                        PreviousAuditRow(reportId = 0, auditNumber = "2023-P1", auditDate = "10/11/2023", ncrsIssued = 2, ncrsClosed = 2, obsIssued = 1, obsClosed = 1, auditorName = "Sarah Johnson")
                    )
                ),
                AuditReportWithDetails(
                    report = AuditReport(
                        auditNumber = "2024-IT-03",
                        projectName = "IT Security Audit 2024",
                        projectType = "Compliance",
                        location = "IT Department",
                        auditorName = "Michael Brown",
                        auditeeOverallStatus = "Submitted",
                        auditDate = "15/05/2024",
                        reportIssuanceDate = "16/05/2024",
                        followupDueDate = "15/06/2024"
                    ),
                    findings = listOf(
                        Finding(reportId = 0, referenceId = "IT-NCR-001", type = "NCR", status = "Closed", trade = "Cybersecurity", activity = "Access Control", description = "Inactive user accounts were not revoked within 30 days"),
                        Finding(reportId = 0, referenceId = "IT-OBS-001", type = "OBS", status = "Closed", trade = "IT Support", activity = "Ticketing", description = "Response times occasionally exceed SLA limits")
                    ),
                    historyRows = listOf(
                        PreviousAuditRow(reportId = 0, auditNumber = "2023-IT", auditDate = "22/10/2023", ncrsIssued = 3, ncrsClosed = 3, obsIssued = 2, obsClosed = 2, auditorName = "Michael Brown")
                    )
                ),
                AuditReportWithDetails(
                    report = AuditReport(
                        auditNumber = "2024-MF-04",
                        projectName = "Manufacturing Quality Audit",
                        projectType = "Quality Audit",
                        location = "Production Department",
                        auditorName = "Emily Davis",
                        auditeeOverallStatus = "Draft",
                        auditDate = "10/05/2024",
                        reportIssuanceDate = "10/05/2024",
                        followupDueDate = "10/06/2024"
                    ),
                    findings = listOf(
                        Finding(reportId = 0, referenceId = "MF-NCR-001", type = "NCR", status = "Open", trade = "Manufacturing", activity = "Calibration", description = "Assembly line scales are out of calibration tolerance"),
                        Finding(reportId = 0, referenceId = "MF-OBS-001", type = "OBS", status = "Open", trade = "Safety", activity = "Material Handling", description = "Improper storage spacing of raw aluminum sheets")
                    ),
                    historyRows = emptyList()
                ),
                AuditReportWithDetails(
                    report = AuditReport(
                        auditNumber = "2024-HR-05",
                        projectName = "HR Compliance Audit",
                        projectType = "Compliance",
                        location = "Human Resources",
                        auditorName = "David Wilson",
                        auditeeOverallStatus = "Draft",
                        auditDate = "08/05/2024",
                        reportIssuanceDate = "08/05/2024",
                        followupDueDate = "08/06/2024"
                    ),
                    findings = listOf(
                        Finding(reportId = 0, referenceId = "HR-NCR-001", type = "NCR", status = "Open", trade = "Legal Compliance", activity = "Orientation Logs", description = "Missing formal signature confirmation on new-hire forms"),
                        Finding(reportId = 0, referenceId = "HR-OBS-001", type = "OBS", status = "Open", trade = "Compliance", activity = "Workshops", description = "Refresher training schedules were delayed")
                    ),
                    historyRows = listOf(
                        PreviousAuditRow(reportId = 0, auditNumber = "2023-HR", auditDate = "01/09/2023", ncrsIssued = 0, ncrsClosed = 0, obsIssued = 1, obsClosed = 1, auditorName = "David Wilson")
                    )
                ),
                AuditReportWithDetails(
                    report = AuditReport(
                        auditNumber = "2024-Q1-06",
                        projectName = "Q1 Internal Audit 2024",
                        projectType = "Internal Audit",
                        location = "Finance Department",
                        auditorName = "Internal Auditor",
                        auditeeOverallStatus = "Submitted",
                        auditDate = "30/04/2024",
                        reportIssuanceDate = "01/05/2024",
                        followupDueDate = "31/05/2024"
                    ),
                    findings = listOf(
                        Finding(reportId = 0, referenceId = "Q1-NCR-001", type = "NCR", status = "Closed", trade = "Finance", activity = "Asset Disposal", description = "Missing write-off slips for legacy laptops"),
                        Finding(reportId = 0, referenceId = "Q1-OBS-001", type = "OBS", status = "Closed", trade = "Administration", activity = "Expenses", description = "Delayed travel expense submittals")
                    ),
                    historyRows = listOf(
                        PreviousAuditRow(reportId = 0, auditNumber = "2023-Q3", auditDate = "22/07/2023", ncrsIssued = 1, ncrsClosed = 1, obsIssued = 3, obsClosed = 3, auditorName = "Internal Auditor")
                    )
                )
            )

            sampleAudits.forEach { details ->
                repository.saveReport(details)
            }
        }
    }
}

class AuditViewModelFactory(private val repository: AuditRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuditViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuditViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
