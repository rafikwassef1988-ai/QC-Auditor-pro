package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AuditRepository(private val auditDao: AuditDao) {

    val allReports: Flow<List<AuditReport>> = auditDao.getAllReports()
    
    val allFindings: Flow<List<FindingOverview>> = auditDao.getAllFindings()

    val allHistoryRows: Flow<List<PreviousAuditRow>> = auditDao.getAllHistory()

    suspend fun getReportWithDetails(reportId: Int): AuditReportWithDetails? = withContext(Dispatchers.IO) {
        val report = auditDao.getReportById(reportId) ?: return@withContext null
        val findings = auditDao.getFindingsForReport(reportId)
        val history = auditDao.getHistoryForReport(reportId)
        AuditReportWithDetails(report, findings, history)
    }

    suspend fun saveReport(details: AuditReportWithDetails): Int = withContext(Dispatchers.IO) {
        auditDao.saveReportWithDetails(details.report, details.findings, details.historyRows)
    }

    suspend fun deleteReport(reportId: Int) = withContext(Dispatchers.IO) {
        auditDao.deleteReportById(reportId)
    }
}
