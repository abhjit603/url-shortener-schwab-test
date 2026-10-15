# Workitems Guide

This file explains how to read the project artifacts under `.workitems/`. It is
not the application setup guide; build, run, configuration, and API usage are
documented in [README.md](README.md) and [docs/API.md](docs/API.md).

## Artifact Hierarchy

| Artifact | Purpose |
| --- | --- |
| `.workitems/FEATURE001/outcome-spec.md` | Feature intent, outcomes, scope, and use cases for F60001 |
| `.workitems/FEATURE001/EPIC001/Capabality-spec.md` | Capability requirements and validation criteria for E50001 |
| `.workitems/FEATURE001/EPIC001/solution-arch.md` | Authoritative architecture, API behavior, data, errors, and constraints |
| `.workitems/FEATURE001/EPIC001/Decomposition.md` | Story dependency graph, sequence, and delivery waves |
| `.workitems/FEATURE001/EPIC001/environments.md` | Local environment and required validation command |
| `STORY-001/` | S30001: durable store, health, and shared error contract |
| `STORY-002/` | S30002: create, list, lookup, deactivate, and public resolve |
| `STORY-003/` | S30003: click analytics, reliability regression, and documentation |
| `STORY-004/` | S30004: bounded local redirect metadata cache |

Each story directory contains a user story with acceptance criteria, a design
with implementation decisions, and tasks with delivery/verification evidence.

## Traceability

The identifiers connect the artifacts: F60001 is the feature, E50001 is its
capability/epic, and S30001 through S30004 are the implementation stories.
Outcome use cases map to capability acceptance criteria; story criteria refine
those into behavior that can be tested. The decomposition records dependencies:
`S30001 → S30002 → S30003 → S30004`.

When details disagree, use `solution-arch.md` for the current cross-cutting
contract and the relevant story acceptance criteria for story-specific gates.
Update those artifacts when an intentional API or behavior change is made; do
not treat this guide as a separate specification.

## Status Fields

Frontmatter status describes the artifact's review/approval lifecycle. A task
status describes implementation progress. They are separate signals: a draft
specification can describe implemented behavior without implying formal
approval. Verification sections should name real tests or manual checks that
exist in the repository.
