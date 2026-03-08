# AGENTS.md

## Repo Focus
- This repository is a real-time risk control platform, not a generic CRUD backend.
- During early initialization, prioritize architecture boundaries, contracts, and module skeletons.
- Do not add business code before contracts and ADRs are stabilized.

## Working Rules
- Keep changes minimal and incremental.
- Reuse the existing module layout under `pulsix-framework`, `pulsix-server`, `pulsix-module-risk`, `pulsix-engine`, and `pulsix-ui`.
- Prefer documenting decisions in `docs/adr/` and contracts in `docs/contracts/` before implementation.

## Current Phase
- The repository is in project skeleton and preparation stage.
- Placeholder files under `docs/`, `deploy/`, and other top-level modules are allowed.
- Avoid filling in detailed implementation, page code, or dependency specifics too early.
