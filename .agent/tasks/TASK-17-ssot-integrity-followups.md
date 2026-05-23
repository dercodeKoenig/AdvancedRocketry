# TASK-17: SSOT integrity follow-ups from 2026-05-23 audit

## Ticket

- Source: 2026-05-23 full repo audit (post-TASK-15 close). Audit
  found pyramid counter in `tasks/README.md` was stale by 236
  tests (claimed 441, real 677) and surfaced 2 satellite test
  pins that violate `testing-principles.md` SOP litmus.
- Status: **Backlog**.
- Created: 2026-05-23.

## Context

Three follow-ups land in this single task because they share a
single shape: each one is a small text-only edit, motivated
directly by the audit, and benefits from being shipped together.

### Phase 1 — Counter-regen step in task-lifecycle SOP

`sops/development/task-lifecycle.md` closure checklist step 2
currently says "Sync the Done table in `tasks/README.md`". The
audit revealed that "free-form claim sweep" (step 3) **did NOT
catch** the pyramid counter drift — the counter line is named
"Pyramid" but is functionally a free-form numeric claim that
agents (including me) didn't think to verify on each TASK close.

Add an explicit step **2.5** between Done-table sync and the
stale-claim sweep:

> **2.5. Regenerate pyramid counter (REQUIRED if the closed TASK
> added or removed any test methods)**
>
> Run:
>
> ```
> for tier in unit integration server client; do
>   echo -n "$tier: "
>   grep -rc '^    @Test$\|^	@Test$' \
>     src/test/java/zmaster587/advancedRocketry/test/$tier/ \
>     2>/dev/null | awk -F: '{s+=$2} END {print s}'
> done
> ```
>
> Update the **Current state** line in `tasks/README.md` with the
> resulting tier counts. Do NOT trust the "+N added" arithmetic in
> commit messages — past drafts have been off by 5+ per session.

Cost: ~5 min edit + bullet in the SOP body explaining why we
distrust draft arithmetic.

### Phase 2 — Satellite pin loosening (×2)

Per audit §6, two assertions are textbook impl-pin per the SOP:

#### 2a. `SatelliteTickBehaviourTest` — exact `powerGen − 1`

The SOP `testing-principles.md` explicitly names this:

> "powerGen − 1 per tick accrual — the `−1` is impl; the contract
> is 'battery accrues at approximately powerGen rate while the
> satellite has work to do'"

The current assertion is exact equality on the per-tick delta.
Replace with a loose-bound:

- Lower: at least 50% of `powerGen` per tick (catches the
  regression class where accrual stops completely)
- Upper: at most `powerGen` per tick (catches accrual-overshoot
  regression)

This still pins the **shape** (drain ≈ generation, monotonic,
non-zero with work) without locking the impl-detail `−1`.

#### 2b. `SatelliteTypeBehaviourTest` — exact 120 RF per processed position

Same shape. SOP names this verbatim ("exactly 120 RF per processed
position — implementation choice"). Replace exact-120 with `> 0`
(regression catch: drain stopped) plus a `≤ powerCap` upper bound.

Cost: ~30 min — locate assertions, swap, re-run TASK-09 tests to
confirm green.

### Phase 3 — README pyramid counter fix

Already fixed inline as part of the audit follow-up (will appear
in the same commit that creates this TASK file). Listed for
completeness so close-out checklist hits it.

## Implementation plan

| Phase | Effort | Result |
|---|---|---|
| 1 | ~15 min | SOP §2.5 added with regen command |
| 2a | ~15 min | `SatelliteTickBehaviourTest` loosened |
| 2b | ~15 min | `SatelliteTypeBehaviourTest` loosened |
| 3 | done | Counter already fixed in backlog-formation commit |

## Acceptance

- [ ] `task-lifecycle.md` has step 2.5 with copy-pasteable command.
- [ ] `SatelliteTickBehaviourTest` exact-equality replaced with
      loose-bound; TASK-09 suite still green.
- [ ] `SatelliteTypeBehaviourTest` exact-120-RF replaced; same.
- [ ] Verify with one synthetic refactor: a behaviour-preserving
      tweak to the satellite tick code (e.g. compute the same
      accrual via a different intermediate variable name) does
      NOT break the loosened tests.

## Technical decisions

- **Inline batched task**, not three separate ones. Each part is
  ~15 min; per-piece overhead of a separate TASK file would
  exceed the work itself.
- **Don't loosen unless the SOP-violation is unambiguous**. The
  audit named exactly two pins; resist scope-creep into other
  "tight pins" that the audit did NOT flag.

## Out of scope

- Loosening of other pins not explicitly named in audit §6.
  Cross-cutting depth audit is its own scope.
- Restructuring the closure-checklist beyond adding step 2.5.

## Dependencies

- Does NOT block any other backlog task.
- Phase 2 touches TASK-09 test classes — verify they remain green
  after edit.

## Estimated effort

~1 h total across phases. Single-session task.
