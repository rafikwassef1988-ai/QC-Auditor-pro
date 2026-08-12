package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "audit_reports")
data class AuditReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val auditNumber: String = "",
    val auditDate: String = "",
    val projectName: String = "",
    val projectNumber: String = "",
    val location: String = "",
    val projectManager: String = "",
    val qcManager: String = "",
    val contractor: String = "",
    val projectType: String = "",
    val phase: String = "",
    val durationDays: Int = 1,
    val auditorName: String = "",
    val reportIssuanceDate: String = "",
    val followupDueDate: String = "",
    val formReference: String = "innovo/QAQC/FRM-1.14/05 REV 02",
    val auditScope: String = "",
    
    // Signatures
    val sigAuditorName: String = "",
    val sigAuditorDesignation: String = "",
    val sigAuditorDate: String = "",
    val sigAuditorPh: String? = null,
    val sigReviewerName: String = "",
    val sigReviewerDesignation: String = "",
    val sigReviewerDate: String = "",
    val sigReviewerPh: String? = null,
    
    // Auditee response area
    val auditeeName: String = "",
    val auditeeDesignation: String = "",
    val auditeeCompany: String = "",
    val auditeeResponseDate: String = "",
    val auditeePhone: String = "",
    val auditeeEmail: String = "",
    val auditeeRemarks: String = "",
    val auditeeProposedClosureDate: String = "",
    val auditeeOverallStatus: String = "",
    val auditeePreventiveActions: String = "",
    val auditeeTrainingActions: String = "",
    val auditeeProcedureChanges: String = "",
    val auditeeDocs: String = "",
    val auditeeRefs: String = "",
    
    // Auditee Signatures
    val auditeeSigName: String = "",
    val auditeeSigDesignation: String = "",
    val auditeeSigDate: String = "",
    val auditeeSigPh: String? = null,
    val auditeeSupName: String = "",
    val auditeeSupDesignation: String = "",
    val auditeeSupDate: String = "",
    val auditeeSupPh: String? = null,
    
    // QA reviewer response review
    val reviewerName: String = "",
    val reviewerStatus: String = "",
    val reviewerDate: String = "",
    val reviewerRemarks: String = "",
    
    // Auditee overall photos (Base64 strings)
    val auditeePhRef1: String? = null,
    val auditeePhRef2: String? = null,
    val auditeePhRef3: String? = null,
    val auditeePhRef4: String? = null,
    
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "findings",
    foreignKeys = [
        ForeignKey(
            entity = AuditReport::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Finding(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reportId: Int,
    val referenceId: String = "",
    val type: String = "NCR", // NCR or OBS
    val severity: String = "Major", // Major or Minor
    val status: String = "Open", // Open or Closed
    val trade: String = "",
    val activity: String = "",
    val locationZone: String = "",
    val description: String = "",
    val negativeImpact: String = "",
    val materialLosses: String = "",
    val rootCause: String = "",
    val correctiveAction: String = "",
    val issueDate: String = "",
    val dueDate: String = "",
    val replyDate: String = "",
    val repeated: String = "No",
    
    // Auditor / finding photos (Base64 strings)
    val ph1Base64: String? = null,
    val ph2Base64: String? = null,
    val ph3Base64: String? = null,
    val ph4Base64: String? = null,
    
    // Per-finding responses (Auditee section)
    val auditeeResponse: String = "",
    val auditeeRca: String = "",
    val auditeeCorrectiveAction: String = "",
    val auditeeResponsiblePerson: String = "",
    val auditeeTargetDate: String = "",
    val auditeeStatus: String = "Open",
    val auditeeEvidence: String = "",
    val auditeeClosurePhoto: String? = null
)

@Entity(
    tableName = "previous_audits",
    foreignKeys = [
        ForeignKey(
            entity = AuditReport::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PreviousAuditRow(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reportId: Int,
    val auditNumber: String = "",
    val auditDate: String = "",
    val ncrsIssued: Int = 0,
    val ncrsClosed: Int = 0,
    val obsIssued: Int = 0,
    val obsClosed: Int = 0,
    val auditorName: String = ""
)

data class AuditReportWithDetails(
    val report: AuditReport,
    val findings: List<Finding>,
    val historyRows: List<PreviousAuditRow>
)

data class FindingOverview(
    val id: Int,
    val reportId: Int,
    val type: String = "NCR",
    val severity: String = "Major",
    val status: String = "Open",
    val auditeeStatus: String = "Open"
)
