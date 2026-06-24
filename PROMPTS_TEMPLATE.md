# PROMPTS.md — AI prompt log (mandatory)

**CP3566 Term Project · Fraud Case-Management**

> Rename this file to `PROMPTS.md` and commit it to your GitHub repository.

## Why this file exists

**GitHub Copilot (and Copilot Chat) is the only AI tool you may use on this project.**
This file is your honest record of the significant prompts you gave it.

- It is **mandatory**: a missing or fabricated log makes the **20 work marks zero** (see the brief, §6 and §7).
- Using **any other AI tool** — ChatGPT, Claude, Gemini, Cursor, etc. — is an **automatic zero on the whole project**.
- Together with your **5–10 commits**, this log is how we see the work is yours and grew over time.

## What to log

Log every **significant** prompt — the ones that produced code you kept, or that shaped a design decision.
You do **not** need to log tiny autocompletes (a variable name, an `import`). Aim for honesty, not volume.

For each one, note what you asked, what Copilot gave you, and **what you did with it** (kept / changed / rejected) —
because you must be able to **explain every line in your demo**.

## Format — copy one block per prompt

```
### Entry N — <short title>
- When: <date>
- Working on: <which part, e.g. CaseService state machine>
- Prompt: <what you typed to Copilot / Copilot Chat>
- Copilot suggested: <one-line summary of what it produced>
- What I did: kept / changed / rejected — <why, in one line>
```
---

## My log

### Entry 1 —
- When: 2026-06-23
- Working on: RuleEngineService
- Prompt: Can you show me how to implement an openCase() method that saves an Alert, then a Case linked to that alert, and then an AuditLog entry?
- Copilot suggested: Copilot generated a method that creates an Alert, then a Case, and finally an AuditLog entry, linking them together.
- What I did: kept — The generated code was correct and followed the required sequence of operations. I made minor adjustments to variable names for clarity.

### Entry 2 —
- When: 2026-06-24
- Working on: Implementing R1 fraud rule
- Prompt: How do I implement the R1 fraud rule that opens a case for transactions with amounts greater than or equal to a threshold?
- Copilot suggested: You can implement the R1 rule by iterating through all transactions, checking if their amount is greater than or equal to the R1 threshold, and calling openCase() for each matching transaction.
- What I did: kept — The suggestion was correct and I implemented it accordingly.

### Entry 3 —
- **When:**
- **Working on:**
- **Prompt:**
- **Copilot suggested:**
- **What I did:**

*(add more entries as you go)*
