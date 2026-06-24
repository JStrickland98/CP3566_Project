package com.example.fraud.web;

import com.example.fraud.model.Rule;
import com.example.fraud.repo.RuleRepository;
import com.example.fraud.service.RuleEngineService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

    private final RuleRepository ruleRepo;
    private final RuleEngineService ruleEngineService;

    public RuleController(RuleRepository ruleRepo, RuleEngineService ruleEngineService) {
        this.ruleRepo = ruleRepo;
        this.ruleEngineService = ruleEngineService;
    }

    @GetMapping
    public ResponseEntity<List<Rule>> getRules() {
        return ResponseEntity.ok(ruleRepo.findAll());
    }

    @PutMapping("/{code}")
    public ResponseEntity<Rule> updateRule(
            @PathVariable String code,
            @RequestBody Rule updatedRule,
            @RequestHeader(name = "X-Role", required = false) String actorRole) {

        if (!"ADMIN".equals(actorRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN role required");
        }

        Rule rule = ruleRepo.findById(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found"));

        rule.setThresholdAmount(updatedRule.getThresholdAmount());
        rule.setWindowMinutes(updatedRule.getWindowMinutes());
        rule.setMinCount(updatedRule.getMinCount());
        rule.setEnabled(updatedRule.isEnabled());

        return ResponseEntity.ok(ruleRepo.save(rule));
    }

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> triggerScan(
            @RequestHeader(name = "X-Role", required = false) String actorRole) {

        if (!"ADMIN".equals(actorRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN role required");
        }

        int casesOpened = ruleEngineService.scanAndOpenCases();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "casesOpened", casesOpened,
                "message", "Rule engine scan completed successfully"
        ));
    }
}
