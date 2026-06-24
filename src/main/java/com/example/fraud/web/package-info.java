/**
 * Web layer for the fraud application.
 *
 * Responsibilities:
 *  - Provide REST endpoints under /api for authentication and case management.
 *  - Return JSON and appropriate HTTP status codes:
 *      200 OK, 201 Created, 401 Unauthorized, 403 Forbidden, 404 Not Found, 409 Conflict.
 *  - Keep controllers thin: validate and map requests, call @Service classes, return results.
 *
 * Required endpoints (implement as @RestController classes in this package):
 *  - POST /api/login
 *  -   request: username/password
 *  -   responses: 200 (successful, return token/session), 401 (bad credentials)
 *  - GET  /api/cases[?status=]
 *  - GET  /api/cases/{id}
 *  - POST /api/cases/{id}/{pickup|escalate|send-back|close-false|close-fraud}
 *  -   responses: 200 (success), 403 (wrong role), 404 (case not found), 409 (illegal transition)
 *  - POST /api/cases/{id}/notes
 *  - GET  /api/rules
 *  - PUT  /api/rules/{code}   (ADMIN only)
 *
 * Security:
 *  - Require authentication for all protected endpoints: unauthenticated → 401.
 *  - Enforce role-based authorization (ANALYST / INVESTIGATOR / ADMIN) → 403 when unauthorized.
 *  - Verify passwords using a Spring Security PasswordEncoder (do not compare plaintext).
 *  - You may implement security checks via a filter/interceptor or use Spring Security.
 *
 * Error mapping:
 *  - Services should throw domain exceptions or ResponseStatusException and controllers/global
 *    @ControllerAdvice should map them to correct HTTP statuses and JSON error bodies.
 *
 * Notes:
 *  - Do business logic in @Service classes (CaseService, RuleEngineService, etc.). Controllers
 *    should be thin adapters (DTO ↔ model conversions, request validation, status codes).
 *  - Keep consistent JSON shapes (use DTOs or map models carefully).
 */
package com.example.fraud.web;
