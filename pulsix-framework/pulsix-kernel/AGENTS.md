# pulsix-kernel phase-1 instructions

When a task touches `pulsix-framework/pulsix-kernel`, read these files first:

- `docs/wiki/pulsix-engine-kernel-一期开发指南.md`
- `docs/wiki/pulsix-engine-kernel-一期进度.md`
- `docs/wiki/pulsix-engine-kernel-一期-ai自动化开发方案.md`
- `docs/wiki/pulsix-engine-kernel-一期任务卡-K02.md` if the task is about kernel migration

Rules:

- Treat `pulsix-kernel` as the owner of pure execution semantics.
- Keep Flink adapters in `pulsix-engine` unless the task explicitly says otherwise.
- Use exactly one task ID per coding session.
- After substantive changes, update the progress doc and the AI workflow/task-card status.
- Prefer minimal migration slices that keep `pulsix-engine` compiling after each step.
