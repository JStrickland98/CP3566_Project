package com.example.fraud.service;

import com.example.fraud.model.Case;
import com.example.fraud.repo.AuditLogRepository;
import com.example.fraud.repo.CaseRepository;
import org.springframework.stereotype.Service;
import com.example.fraud.model.AuditLog;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

/**
 * TODO (student) — THE CASE WORKFLOW (state machine).   PROJECT_BRIEF.html §4.4
 *
 * Legal moves (anything else must be rejected):
 *   pickup:      NEW       -> REVIEWING      (ANALYST)
 *   escalate:    REVIEWING -> ESCALATED      (ANALYST)
 *   send-back:   ESCALATED -> REVIEWING      (INVESTIGATOR)
 *   close-false: REVIEWING -> CLOSED_FALSE   (ANALYST)
 *   close-false: ESCALATED -> CLOSED_FALSE   (INVESTIGATOR)
 *   close-fraud: ESCALATED -> CLOSED_FRAUD   (INVESTIGATOR)
 *
 * The fields and the method signature below are GIVEN — you fill in the body.
 */
@Service
public class CaseService {

    private final CaseRepository caseRepo;
    private final AuditLogRepository auditLogRepo;

    public CaseService(CaseRepository caseRepo, AuditLogRepository auditLogRepo) {
        this.caseRepo = caseRepo;
        this.auditLogRepo = auditLogRepo;
    }

    /**
     * Apply an action to a case, enforcing the state machine and the role rules. Throw so your
     * controller can map it to the right HTTP status:
     *   case not found -> 404,  illegal move from the current state -> 409,  wrong role -> 403.
     *
     * @param caseId        the case to act on
     * @param action        one of: pickup, escalate, send-back, close-false, close-fraud
     * @param actorUsername who is doing it (use for assignedTo on pickup, and for the audit log)
     * @param actorRole     ANALYST | INVESTIGATOR | ADMIN
     * @return the updated case
     */
    public Case apply(Long caseId, String action, String actorUsername, String actorRole) {

        //load the case (404 if not found)
        Case c = caseRepo.findById(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case id " + caseId + " not found"));

        String currentStatus = c.getStatus();
        String newStatus;

        //determine next status (illegal move -> 409)
        try {
            newStatus = getNextStatus(currentStatus, action);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

        //validate actor role (403 if not allowed)
        if (!isRoleAllowed(action, actorRole, currentStatus)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Role " + actorRole + " not permitted to perform action '" + action + "' from status " + currentStatus);
        }

        //apply the change (set assignedTo on pickup), save the case
        String oldStatus = c.getStatus();
        c.setStatus(newStatus);
        if ("pickup".equals(action)) {
            c.setAssignedTo(actorUsername);
        }
        Case updated = caseRepo.save(c);

        //write audit log and return updated case
        AuditLog log = new AuditLog(
                actorUsername,
                action,
                "case",
                caseId,
                "status: " + oldStatus + " -> " + newStatus,
                LocalDateTime.now()
        );
        auditLogRepo.save(log);

        return updated;
    }

    private String getNextStatus(String currentStatus, String action) {
        return switch (currentStatus) {
            case "NEW" -> {
                if ("pickup".equals(action)) yield "REVIEWING";
                else throw new IllegalStateException("Action '" + action + "' not allowed from NEW");
            }
            case "REVIEWING" -> {
                if ("escalate".equals(action)) yield "ESCALATED";
                else if ("close-false".equals(action)) yield "CLOSED_FALSE";
                else throw new IllegalStateException("Action '" + action + "' not allowed from REVIEWING");
            }
            case "ESCALATED" -> {
                if ("send-back".equals(action)) yield "REVIEWING";
                else if ("close-false".equals(action)) yield "CLOSED_FALSE";
                else if ("close-fraud".equals(action)) yield "CLOSED_FRAUD";
                else throw new IllegalStateException("Action '" + action + "' not allowed from ESCALATED");
            }
            case "CLOSED_FALSE", "CLOSED_FRAUD" -> throw new IllegalStateException("Cannot transition from terminal state '" + currentStatus + "'");
            default -> throw new IllegalStateException("Unknown current status '" + currentStatus + "'");
        };
    }

    private boolean isRoleAllowed(String action, String actorRole, String currentStatus) {
        if ("ADMIN".equals(actorRole)) return true;

        switch (action) {
            case "pickup":
                return "ANALYST".equals(actorRole);
            case "escalate":
                return "ANALYST".equals(actorRole);
            case "send-back":
                return "INVESTIGATOR".equals(actorRole);
            case "close-false":
                if ("REVIEWING".equals(currentStatus)) return "ANALYST".equals(actorRole);
                if ("ESCALATED".equals(currentStatus)) return "INVESTIGATOR".equals(actorRole);
                return false;
            case "close-fraud":
                return "ESCALATED".equals(currentStatus) && "INVESTIGATOR".equals(actorRole);
            default:
                return false;
        }
    }
}
