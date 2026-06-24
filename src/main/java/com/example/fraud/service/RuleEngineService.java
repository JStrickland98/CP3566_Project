package com.example.fraud.service;

import com.example.fraud.model.Alert;
import com.example.fraud.model.Case;
import com.example.fraud.model.AuditLog;
import com.example.fraud.model.Rule;
import com.example.fraud.model.Transaction;
import com.example.fraud.model.Watchlist;

import com.example.fraud.repo.AlertRepository;
import com.example.fraud.repo.AuditLogRepository;
import com.example.fraud.repo.CaseRepository;
import com.example.fraud.repo.RuleRepository;
import com.example.fraud.repo.TransactionRepository;
import com.example.fraud.repo.WatchlistRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

/**
 * TODO (student) — THE RULE ENGINE.   PROJECT_BRIEF.html §4.3
 *
 * Scan every transaction and OPEN A NEW CASE for each rule hit:
 *   R1  large single amount
 *   R2  velocity (too many transactions too fast on one account)
 *   R3  counterparty on the watchlist                              [R1–R3 required]
 *   R4  structuring (several transfers just under the limit)       [bonus]
 *
 * The fields and method signatures below are GIVEN — you fill in the bodies.
 *   - Read thresholds from the `rules` table via ruleRepo (do NOT hard-code them).
 *   - Opening a case = save an Alert, then a Case with status "NEW", then an audit_log row.
 *   - On the seeded data this must open EXACTLY 16 cases (5 R1 + 3 R2 + 5 R3 + 3 R4).
 *   - The given repositories return everything (findAll, etc.); you may also ADD query methods
 *     to them (e.g. findByAmountGreaterThanEqual) if you prefer to let the database filter.
 */
@Service
public class RuleEngineService {

    private final TransactionRepository transactionRepo;
    private final RuleRepository ruleRepo;
    private final WatchlistRepository watchlistRepo;
    private final AlertRepository alertRepo;
    private final CaseRepository caseRepo;
    private final AuditLogRepository auditLogRepo;

    public RuleEngineService(TransactionRepository transactionRepo, RuleRepository ruleRepo,
                             WatchlistRepository watchlistRepo, AlertRepository alertRepo,
                             CaseRepository caseRepo, AuditLogRepository auditLogRepo) {
        this.transactionRepo = transactionRepo;
        this.ruleRepo = ruleRepo;
        this.watchlistRepo = watchlistRepo;
        this.alertRepo = alertRepo;
        this.caseRepo = caseRepo;
        this.auditLogRepo = auditLogRepo;
    }

    /**
     * Scan all transactions and open one NEW case per rule hit.
     * @return the number of cases opened (should be 16 on the seeded data)
     */
    public int scanAndOpenCases() {
        int casesOpened = 0;

        // Load all transactions
        List<Transaction> allTransactions = transactionRepo.findAll();

        Rule r1 = ruleRepo.findById("R1")
                .orElseThrow();

        Rule r2 = ruleRepo.findById("R2")
                .orElseThrow();

        List<Transaction> transactions = transactionRepo.findAll();

        List<Watchlist> watchlistEntries = watchlistRepo.findAll();

        Set<String> watchlistNames = new HashSet<>();

        for (Watchlist w : watchlistEntries) {
            watchlistNames.add(w.getName());
        }

        for (Transaction tx : transactions) {

            if (tx.getAmount().compareTo(r1.getThresholdAmount()) >= 0) {

                openCase(
                        tx.getId(),
                        "R1",
                        "Transaction amount exceeds threshold",
                        tx.getOccurredAt()
                );

                casesOpened++;
            }
        }

        for (Transaction tx : transactions) {

            if (watchlistNames.contains(tx.getCounterparty())) {
                openCase(
                        tx.getId(),
                        "R3",
                        "Counterparty appears on watchlist",
                        tx.getOccurredAt()
                );

                casesOpened++;
            }
        }

        return casesOpened;
    }


    private int checkVelocity(List<Transaction> allTransactions, Rule r2, int caseCount) {
        if (r2 == null) {
            return caseCount;
        }

        // Group transactions by accountId
        Map<Long, List<Transaction>> txnsByAccount = new HashMap<>();
        for (Transaction txn : allTransactions) {
            txnsByAccount.computeIfAbsent(txn.getAccountId(), k -> new ArrayList<>())
                    .add(txn);
        }

        // For each account, check if too many transactions occur within the time window
        for (Map.Entry<Long, List<Transaction>> entry : txnsByAccount.entrySet()) {
            List<Transaction> accountTxns = entry.getValue();

            // Sort by timestamp
            accountTxns.sort((t1, t2) -> t1.getOccurredAt().compareTo(t2.getOccurredAt()));

            // Sliding window: check each possible window of transactions
            for (int i = 0; i < accountTxns.size(); i++) {
                Transaction windowStart = accountTxns.get(i);
                LocalDateTime windowEnd = windowStart.getOccurredAt()
                        .plusMinutes(r2.getWindowMinutes());

                // Count transactions within this window
                int countInWindow = 0;
                for (int j = i; j < accountTxns.size(); j++) {
                    Transaction txn = accountTxns.get(j);
                    // Check if transaction falls within the window
                    if (!txn.getOccurredAt().isAfter(windowEnd)) {
                        countInWindow++;
                    } else {
                        break;  // Window is closed, no more transactions can fit
                    }
                }

                // If we hit the threshold, open a case for the window starter
                if (countInWindow >= r2.getMinCount()) {
                    String detail = countInWindow + " transactions in "
                            + r2.getWindowMinutes() + " minutes for account "
                            + entry.getKey();
                    openCase(windowStart.getId(), "R2", detail, windowStart.getOccurredAt());
                    caseCount++;
                    break;  // Only one case per account for R2
                }
            }
        }

        return caseCount;
    }

    /**
     * Open one case for a transaction that tripped a rule.
     * @param transactionId the offending transaction's id
     * @param ruleCode      "R1".."R4"
     * @param detail        a short, human-readable reason
     * @param when          the timestamp to stamp the alert and case with
     */

    private void openCase(long transactionId, String ruleCode, String detail, LocalDateTime when) {

        // Step 1: Create and save Alert
        Alert alert = new Alert(transactionId, ruleCode, detail, when);
        Alert savedAlert = alertRepo.save(alert);
        Long alertId = savedAlert.getId();

        // Step 2: Create and save Case linked to the alert
        Case caseRecord = new Case(alertId, "NEW", null, when);
        Case savedCase = caseRepo.save(caseRecord);
        Long caseId = savedCase.getId();

        // Step 3: Create and save AuditLog entry
        AuditLog auditLog = new AuditLog(
                "system",
                "OPEN_CASE",
                "case",
                caseId,
                ruleCode + ": " + detail,
                when
        );

        auditLogRepo.save(auditLog);
    }
}
