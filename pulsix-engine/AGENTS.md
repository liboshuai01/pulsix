# pulsix-engine phase-1 instructions

When a task touches `pulsix-engine`, read these files first:

- `docs/wiki/pulsix-engine-kernel-一期开发指南.md`
- `docs/wiki/pulsix-engine-kernel-一期进度.md`
- `docs/wiki/pulsix-engine-kernel-一期-ai自动化开发方案.md`

Rules:

- Treat the development guide as the scope boundary.
- Treat the progress doc as the current ground truth.
- Use exactly one task ID from the AI workflow doc per coding session.
- Do not expand work into `pulsix-module-risk`, `pulsix-access`, or UI unless the current task explicitly requires it.
- After substantive changes, update the progress doc and the AI workflow task status.
- Prefer targeted validation first, then `pulsix-engine` module validation.
