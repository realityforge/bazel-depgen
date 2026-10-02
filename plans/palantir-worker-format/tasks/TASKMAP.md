# Task Map

- Spec: [SPEC.md](../SPEC.md)
- Status: implementation-review
- Current frontier: implementation review
- Planning reviewer: /root/plan_review (2/3 rounds), Findings: none
- Plan checkpoint: automatic; user explicit evidence-based entry exception and passing planning review
- Implementation reviewer: pending (0/5 rounds)

## Full-scope validation

- Gate: tools/check.sh; final diff and source coverage/worker evidence
- Evidence: tools/check.sh passed (4 release/config + 15 coverage targets); line 96.34%, branch 87.04%; see [EVIDENCE.md](../EVIDENCE.md)

## Tasks

| ID | Task | Status | Blocked by |
| --- | --- | --- | --- |
| T01 | [Migrate worker formatting](T01-worker-format.md) | complete | None |

## Sequencing notes

One coherent migration owns all R1–R4 / AC1–AC4: implementation and validation commit precedes
implementation review, then reviewed evidence is recorded and tree removed in closeout;
publication/assignment/auto-merge follows closeout and is verified externally.

## Promoted knowledge

not-required: no domain documentation directories or configuration exist.
