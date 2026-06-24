package com.example.fraud.web;

import com.example.fraud.model.Case;
import com.example.fraud.repo.CaseRepository;
import com.example.fraud.service.CaseService;
import com.example.fraud.service.RuleEngineService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseRepository caseRepo;
    private final CaseService caseService;
    private final RuleEngineService ruleEngineService;

    public CaseController(CaseRepository caseRepo,
                          CaseService caseService,
                          RuleEngineService ruleEngineService) {
        this.caseRepo = caseRepo;
        this.caseService = caseService;
        this.ruleEngineService = ruleEngineService;
    }

    @GetMapping
    public ResponseEntity<List<Case>> getCases(@RequestParam(required = false) String status) {
        List<Case> result;
        if (status == null || status.isBlank()) {
            result = caseRepo.findAll();
        } else {
            result = caseRepo.findAll().stream()
                    .filter(c -> status.equals(c.getStatus()))
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Case> getCase(@PathVariable Long id) {
        return caseRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
    }

    @PostMapping("/{id}/pickup")
    public ResponseEntity<Case> pickup(
            @PathVariable Long id,
            @RequestHeader(name = "X-User", required = false) String actorUsername,
            @RequestHeader(name = "X-Role", required = false) String actorRole) {
        if (actorUsername == null || actorRole == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication headers");
        }
        Case updated = caseService.apply(id, "pickup", actorUsername, actorRole);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/escalate")
    public ResponseEntity<Case> escalate(
            @PathVariable Long id,
            @RequestHeader(name = "X-User", required = false) String actorUsername,
            @RequestHeader(name = "X-Role", required = false) String actorRole) {
        if (actorUsername == null || actorRole == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication headers");
        }
        Case updated = caseService.apply(id, "escalate", actorUsername, actorRole);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/send-back")
    public ResponseEntity<Case> sendBack(
            @PathVariable Long id,
            @RequestHeader(name = "X-User", required = false) String actorUsername,
            @RequestHeader(name = "X-Role", required = false) String actorRole) {
        if (actorUsername == null || actorRole == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication headers");
        }
        Case updated = caseService.apply(id, "send-back", actorUsername, actorRole);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/close-false")
    public ResponseEntity<Case> closeFalse(
            @PathVariable Long id,
            @RequestHeader(name = "X-User", required = false) String actorUsername,
            @RequestHeader(name = "X-Role", required = false) String actorRole) {
        if (actorUsername == null || actorRole == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication headers");
        }
        Case updated = caseService.apply(id, "close-false", actorUsername, actorRole);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/close-fraud")
    public ResponseEntity<Case> closeFraud(
            @PathVariable Long id,
            @RequestHeader(name = "X-User", required = false) String actorUsername,
            @RequestHeader(name = "X-Role", required = false) String actorRole) {
        if (actorUsername == null || actorRole == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication headers");
        }
        Case updated = caseService.apply(id, "close-fraud", actorUsername, actorRole);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<Void> addNote(
            @PathVariable Long id,
            @RequestBody NoteRequest note,
            @RequestHeader(name = "X-User", required = false) String actorUsername) {
        if (actorUsername == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User header");
        }
        if (!caseRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found");
        }
        // TODO: persist note to DB via a Note entity/repo
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public static class NoteRequest {
        private String text;
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}