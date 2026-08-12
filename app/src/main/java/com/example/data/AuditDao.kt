package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditDao {

    @Query("""
        SELECT id, auditNumber, auditDate, projectName, projectNumber, location, projectManager, qcManager, contractor, projectType, phase, durationDays, auditorName, reportIssuanceDate, followupDueDate, formReference, auditScope, sigAuditorName, sigAuditorDesignation, sigAuditorDate, sigReviewerName, sigReviewerDesignation, sigReviewerDate, auditeeName, auditeeDesignation, auditeeCompany, auditeeResponseDate, auditeePhone, auditeeEmail, auditeeRemarks, auditeeProposedClosureDate, auditeeOverallStatus, auditeePreventiveActions, auditeeTrainingActions, auditeeProcedureChanges, auditeeDocs, auditeeRefs, auditeeSigName, auditeeSigDesignation, auditeeSigDate, auditeeSupName, auditeeSupDesignation, auditeeSupDate, reviewerName, reviewerStatus, reviewerDate, reviewerRemarks, timestamp, 
        NULL as sigAuditorPh, NULL as sigReviewerPh, NULL as auditeeSigPh, NULL as auditeeSupPh, NULL as auditeePhRef1, NULL as auditeePhRef2, NULL as auditeePhRef3, NULL as auditeePhRef4 
        FROM audit_reports ORDER BY timestamp DESC
    """)
    fun getAllReports(): Flow<List<AuditReport>>

    @Query("SELECT id, reportId, type, severity, status, auditeeStatus FROM findings")
    fun getAllFindings(): Flow<List<FindingOverview>>

    @Query("SELECT * FROM previous_audits")
    fun getAllHistory(): Flow<List<PreviousAuditRow>>

    @Query("SELECT * FROM audit_reports WHERE id = :reportId LIMIT 1")
    suspend fun getReportById(reportId: Int): AuditReport?

    @Query("SELECT * FROM findings WHERE reportId = :reportId")
    fun getFindingsForReportDirect(reportId: Int): Flow<List<Finding>>

    @Query("SELECT * FROM findings WHERE reportId = :reportId")
    suspend fun getFindingsForReport(reportId: Int): List<Finding>

    @Query("SELECT * FROM previous_audits WHERE reportId = :reportId")
    suspend fun getHistoryForReport(reportId: Int): List<PreviousAuditRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: AuditReport): Long

    @Update
    suspend fun updateReport(report: AuditReport)

    @Query("DELETE FROM audit_reports WHERE id = :reportId")
    suspend fun deleteReportById(reportId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinding(finding: Finding): Long

    @Update
    suspend fun updateFinding(finding: Finding)

    @Query("DELETE FROM findings WHERE reportId = :reportId")
    suspend fun deleteFindingsForReport(reportId: Int)

    @Query("DELETE FROM findings WHERE id = :findingId")
    suspend fun deleteFindingById(findingId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryRow(row: PreviousAuditRow): Long

    @Query("DELETE FROM previous_audits WHERE reportId = :reportId")
    suspend fun deleteHistoryForReport(reportId: Int)

    @Transaction
    suspend fun saveReportWithDetails(
        report: AuditReport,
        findings: List<Finding>,
        historyRows: List<PreviousAuditRow>
    ): Int {
        val reportId = if (report.id == 0) {
            insertReport(report).toInt()
        } else {
            updateReport(report)
            report.id
        }

        // Delete old findings and insert updated ones, or merge them.
        // It's simplest to delete all findings and history and insert the new ones,
        // since we insert fully constructed datasets.
        deleteFindingsForReport(reportId)
        findings.forEach { finding ->
            constructWithReportId(finding, reportId)?.let { insertFinding(it) }
        }

        deleteHistoryForReport(reportId)
        historyRows.forEach { row ->
            insertHistoryRow(row.copy(id = 0, reportId = reportId))
        }

        return reportId
    }

    private fun constructWithReportId(finding: Finding, reportId: Int): Finding? {
        return finding.copy(id = 0, reportId = reportId)
    }
}
