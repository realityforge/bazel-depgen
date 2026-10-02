# Task Map

- Spec: [SPEC.md](../SPEC.md)
- Status: planned
- Current frontier: T01
- Planning reviewer: /root/plan_review (2/3 rounds), Findings: none
- Plan checkpoint: automatic; user explicit evidence-based entry exception and passing planning review
- Implementation reviewer: pending (0/5 rounds)

## Full-scope validation

- Gate: tools/check.sh; final diff and source coverage/worker evidence
- Evidence: pending

## Tasks

| ID | Task | Status | Blocked by |
| --- | --- | --- | --- |
| T01 | [Migrate worker formatting](T01-worker-format.md) | in_progress | None |

## Sequencing notes

One coherent migration owns all R1–R4 / AC1–AC4: implementation and validation commit precedes
implementation review, then reviewed evidence is recorded and tree removed in closeout;
publication/assignment/auto-merge follows closeout and is verified externally.

## Promoted knowledge

not-required: no domain documentation directories or configuration exist.
