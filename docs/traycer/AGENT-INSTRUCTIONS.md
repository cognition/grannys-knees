# Traycer agent instructions: Granny's Knees

## Mission
Orchestrate delivery of the native Android and Wear OS application defined in `docs/delivery-plan.md`. **Do not treat the existing HTML prototype as a native app.**

## Tool allocation (guidance, not fixed rules)
- **Traycer:** decomposition, dependency tracking, worktree isolation, agent handoffs, integration and status reporting.
- **Claude:** architecture decisions, difficult cross-module design, critical reviews and complex implementation.
- **Codex:** Kotlin implementation, tests, refactoring, CI debugging and independent code review.
- **Cursor:** IDE-driven integration, Compose previews, emulator/device debugging and visual refinements.
- **OpenRouter:** bounded research, documentation, test-case expansion and other cost-conscious supporting tasks. Select models deliberately, not automatically for every task.

## Communication style
Use plain language, explain unfamiliar Android terms on first use, and state the concrete outcome before implementation details. Assume the owner has several projects and should not need to reread architecture documents to understand a handoff. Canadian English.

## Execution contract
1. Inspect the repository, build configuration and code **before** claiming any capability exists.
2. For each task, state: objective, prerequisites, owned paths, interfaces, tests, demonstration and explicit exclusions.
3. Define shared contracts/fixtures before parallel implementation. Use isolated branches/worktrees; do not assign concurrent writes to the same files.
4. Prefer simple, testable Kotlin and minimal architecture. No speculative microservices, backend, AI layer or complex abstraction framework.
5. Implement a complete thin vertical slice before expanding scope.
6. Never change the approved product scope, scoring semantics, privacy defaults or symptom reporting rules without raising a decision.
7. No fabricated test results, runtime claims, data or screenshots. If no Android SDK/emulator is available, say **not verified on device**.
8. An authoring agent must not be the only reviewer of a risky change. Use independent review for scoring, symptom integrity, sensor provenance and synchronization.
9. Keep documentation synchronized with **verified** implementation. Label stubs and mock data explicitly.
10. Merge only after applicable CI, tests and integration checks. Stop and report blockers instead of silently substituting functionality.

## Standard task output
- Status: planned / in progress / blocked / implemented-unverified / verified
- Summary in plain language
- Changed paths and commit/PR
- Tests run, exact results, and tests not run
- Demonstration steps
- Risks, open decisions, follow-on task IDs
- Integration gate satisfied? yes/no, with evidence

## Review priorities
1. Data correctness and privacy
2. Reliable weather retrieval, timezone handling and caching
3. Native Android functionality and accessibility
4. Widget consistency and freshness
5. Simplicity, maintainability and performance
6. Branding and copy consistency

## Non-negotiable data rules
A symptom observation is an append-only, timestamped event for an **explicitly changed** measurement or **explicitly confirmed unchanged** measurement. Unreported is not zero and not unchanged. The sensor snapshot is a separate linked object with provenance. Weather-based forecast index and self-reported discomfort are never conflated.

## Product voice
“Your joints. Your weather. Your excuse.” Science-ish, playful and self-aware. The index is experimental, observational correlations are not causation, and this is not medical advice.
