# pulsix-ui AI Baseline Status

Last updated: 2026-03-16
Scope: `pulsix-ui`
Target checks:

- `ts:check`
- `lint:eslint`
- `lint:style`
- `lint:format`
- `build:dev`

## Purpose

这份文档用于给后续 AI 助手保留 `pulsix-ui` 的基线校验现状，避免新功能开发后被历史存量问题干扰。

目标有两个：

1. 让后续 AI 清楚当前哪些检查本身就会失败。
2. 让后续 AI 清楚应该先修什么、不要误修什么、修完后还要更新哪些状态。

## Current Baseline Summary

| Check | Current status | Current result | Main blocker |
| --- | --- | --- | --- |
| `ts:check` | failed | `vue-tsc --noEmit` 被 `SIGABRT` 杀掉 | Node heap OOM，类型检查扫描范围过大 |
| `lint:eslint` | failed | 14 errors | `Tinyflow` vendored JS 被纳入 ESLint |
| `lint:style` | failed | 225 problems, 221 errors, 4 warnings | 历史样式规范债务较多 |
| `lint:format` | failed | 100 files need formatting | Prettier 基线未收敛 |
| `build:dev` | passed with warnings | 能构建成功，但存在多类历史告警 | 环境变量、自动导入、自动组件扫描、历史 CSS 写法 |

## Command Notes

本次排查为了避免直接改文件，使用的是“只检查”的等价命令：

- `ts:check`: `pnpm --dir pulsix-ui exec vue-tsc --noEmit`
- `lint:eslint`: `pnpm --dir pulsix-ui exec eslint --ext .js,.ts,.vue ./src`
- `lint:style`: `pnpm --dir pulsix-ui exec stylelint "./src/**/*.{vue,less,postcss,css,scss}"`
- `lint:format`: `pnpm --dir pulsix-ui exec prettier --check "src/**/*.{js,ts,json,tsx,css,less,scss,vue,html,md}"`
- `build:dev`: `pnpm --dir pulsix-ui run build:dev`

注意：

- `package.json` 里的 `lint:eslint`、`lint:style`、`lint:format` 都是会改文件的版本。
- 后续如果只想验证新功能，不想顺手改一堆旧文件，应优先使用 check-only 命令。

## Findings

### 1. `ts:check`

Current status:

- 失败。
- 运行 `vue-tsc --noEmit` 时出现 Node heap OOM，进程被 `SIGABRT` 终止。

Confirmed root causes:

- `pulsix-ui/tsconfig.json` 开启了 `allowJs: true`。
- `include` 直接包含整个 `src`。
- `src/components/Tinyflow/ui/` 下存在大体积 vendored JS 文件：
  - `src/components/Tinyflow/ui/index.js`
  - `src/components/Tinyflow/ui/index.umd.js`
- `Tinyflow.vue` 实际已经通过 `./ui` 的类型声明文件使用该模块：
  - `src/components/Tinyflow/Tinyflow.vue`
  - `src/components/Tinyflow/ui/index.d.ts`

Impact:

- 当前 `ts:check` 还没稳定跑到“真实类型错误”阶段。
- 在 OOM 修掉前，任何新功能的类型验证都不可靠。

Recommended fix:

1. 新增专用于类型检查的 `tsconfig.typecheck.json`。
2. 从主 `tsconfig.json` 继承，但排除 `src/components/Tinyflow/ui/**` 这类 vendored JS 产物。
3. 优先考虑在 typecheck 配置中关闭 `allowJs`，只检查 `.ts` 和 `.vue`。
4. 修完 OOM 后重新跑 `ts:check`，再记录是否出现真正的类型错误。

Do not do:

- 不要去手改 `Tinyflow` 的 bundle 文件来“修类型检查”。

### 2. `lint:eslint`

Current status:

- 失败。
- 当前共 14 个 error。

Confirmed root causes:

- 错误全部集中在：
  - `src/components/Tinyflow/ui/index.js`
  - `src/components/Tinyflow/ui/index.umd.js`
- 主要规则：
  - `@typescript-eslint/no-this-alias`
- 当前 `.eslintignore` 没有排除这类 vendored 目录。

Impact:

- 后续 AI 新增业务代码后，ESLint 结果会先被旧 bundle 文件污染。
- 无法快速判断新功能是否引入了真实 lint 问题。

Recommended fix:

1. 将 `src/components/Tinyflow/ui/**` 加入 `.eslintignore`。
2. 或者在 `.eslintrc.js` 中针对该目录加 `overrides` 并关闭相关规则。
3. 推荐方案是直接忽略该目录，而不是修 bundle 产物。

Do not do:

- 不要为了通过 lint 去格式化或重写 `Tinyflow` 产物文件。

### 3. `lint:style`

Current status:

- 失败。
- 当前共 225 个问题，其中 221 个 error、4 个 warning。
- 其中 68 个问题可以通过 `--fix` 自动修复。

Main rule clusters:

- `order/properties-order`
- `rule-empty-line-before`
- `color-function-notation`
- `alpha-value-notation`
- `at-rule-empty-line-before`
- `media-feature-range-notation`
- `custom-property-pattern`
- `no-invalid-double-slash-comments`

Representative files:

- `src/components/DiyEditor/index.vue`
- `src/components/MagicCubeEditor/index.vue`
- `src/components/MarkdownView/index.vue`
- `src/views/mall/promotion/kefu/index.vue`
- `src/views/mall/promotion/kefu/components/KeFuMessageList.vue`
- `src/views/ai/chat/index/components/message/MessageFileUpload.vue`
- `src/views/mall/product/spu/components/SkuList.vue`

Confirmed configuration pressure:

- `stylelint.config.js` 开启了严格的 `order/properties-order`。
- 这条规则是当前历史债务的主要来源之一。

Impact:

- 新功能即便只改一个文件，也很容易被大批旧样式问题淹没。
- 如果直接执行 `lint:style --fix`，会触发较大范围的文件改动。

Recommended fix:

1. 先单独做一笔“样式基线清理”提交，不要混在业务功能提交里。
2. 先执行自动修复能处理的部分。
3. 剩余问题按聚类文件夹手工收敛，优先级建议如下：
   - `components/MarkdownView`
   - `components/DiyEditor`
   - `views/mall/promotion/kefu`
   - `views/ai/chat`
4. 如果团队不想长期维护如此严格的属性顺序，可评估是否下调 `order/properties-order` 的强度。

Special cases already confirmed:

- `src/views/mall/product/spu/components/SkuList.vue` 存在 `//` CSS 注释。
- `src/views/mp/menu/index.vue` 存在 `*zoom: 1;` 这类旧写法，构建时也会带来 CSS 告警。

### 4. `lint:format`

Current status:

- 失败。
- 当前共 100 个文件未通过 Prettier 检查。

Observed distribution:

- `src/api/**`
- `src/components/**`
- `src/views/**`

Impact:

- 新功能开发后，即便只改少量代码，也会被全仓格式债务干扰。
- 如果直接运行 `lint:format`，会导致大批旧文件同时被重写。

Recommended fix:

1. 单独做一笔 Prettier 基线整理提交。
2. 如果某些 vendored 目录不希望被 Prettier 管理，应加入 `.prettierignore`。
3. 建议将 `src/components/Tinyflow/ui/**` 加入 `.prettierignore`，避免格式化 bundle 文件。

### 5. `build:dev`

Current status:

- 构建成功。
- 但存在多类历史告警，会干扰后续 AI 对新功能的构建验收。

Confirmed warning sources:

- `.env.dev` 中声明了 `NODE_ENV=production`，Vite 对 dev build 发出提示。
- 自动组件扫描时，`src/components/DiyEditor/components/mobile/**/property.vue` 大量同名，均映射为 `Property`，出现命名冲突。
- 自动导入同时包含 `vue-i18n`、`@vueuse/core` 和 `src/hooks/web`，导致如下名字冲突：
  - `useI18n`
  - `useNetwork`
  - `useNow`
  - `useTimeAgo`
  - `useTitle`
- 构建时还观察到以下历史代码告警：
  - `src/views/mall/product/spu/components/SkuList.vue` 的 `//` CSS 注释
  - `src/views/mp/menu/index.vue` 的 `*zoom: 1;`
  - `src/components/bpmnProcessDesigner/package/penal/task/task-components/UserTask.vue` 中 `return` 换行导致的自动分号告警
- 构建最终还存在大 chunk 警告。

Impact:

- `build:dev` 虽然能过，但噪音很多。
- 后续 AI 看到构建告警时，不容易快速分辨哪些是新问题、哪些是老问题。

Recommended fix:

1. 清理 `.env.dev`、`.env.test`、`.env.stage`、`.env.prod` 中不合适的 `NODE_ENV` 配置。
2. 在 `unplugin-vue-components` 扫描中排除 `property.vue` 这类重复命名文件，或改为显式导入。
3. 在 `unplugin-auto-import` 中规避与本地 hooks 重名的导入源。
4. 修复已确认的历史 CSS 告警和 `UserTask.vue` 中的 `return` 换行写法。
5. 大 chunk 警告可作为第二阶段优化，不必阻塞基线验收。

## Prioritized Remediation Order

建议按以下顺序处理：

1. 修 `ts:check` OOM。
2. 修 `lint:eslint` 对 vendored 目录的误扫。
3. 单独清理 `lint:style` 和 `lint:format` 的历史基线。
4. 清理 `build:dev` 的主要噪音告警。
5. 增加 check-only 脚本，保证未来 AI 验证不触发大范围自动修复。

## Suggested Future Scripts

建议后续补上以下脚本：

- `lint:eslint:check`
  - `eslint --ext .js,.ts,.vue ./src`
- `lint:style:check`
  - `stylelint "./src/**/*.{vue,less,postcss,css,scss}"`
- `lint:format:check`
  - `prettier --check "src/**/*.{js,ts,json,tsx,css,less,scss,vue,html,md}"`
- `ts:check`
  - 改为使用专门的 `tsconfig.typecheck.json`

## Tracking Board

### Completed

- [x] 2026-03-16：完成 `pulsix-ui` 五项基线检查现状排查。
- [x] 2026-03-16：确认 `ts:check` 当前首要问题是 OOM，而不是已知类型错误列表。
- [x] 2026-03-16：确认 `lint:eslint` 的 14 个错误全部来自 `Tinyflow` vendored JS。
- [x] 2026-03-16：确认 `lint:style` 和 `lint:format` 都存在明显历史基线债务。
- [x] 2026-03-16：确认 `build:dev` 可通过，但存在多类历史告警。
- [x] 2026-03-16：将本次排查结果持久化到 `docs/ai/`。

### In Progress

- [ ] 尚无代码修复落地。

### Remaining

- [ ] 新增并切换到专用的类型检查配置，解决 `ts:check` OOM。
- [ ] 排除 `Tinyflow` vendored 目录的 ESLint 扫描。
- [ ] 排除 `Tinyflow` vendored 目录的 Prettier 扫描。
- [ ] 评估是否也需要排除 `Tinyflow` vendored 目录的其他检查。
- [ ] 单独完成一笔 `stylelint` 基线清理提交。
- [ ] 单独完成一笔 `prettier` 基线清理提交。
- [ ] 清理 `.env.*` 中不合适的 `NODE_ENV` 声明。
- [ ] 处理 `property.vue` 自动组件命名冲突。
- [ ] 处理 `useI18n` 等自动导入命名冲突。
- [ ] 清理构建期已知 CSS 告警。
- [ ] 修复 `UserTask.vue` 的自动分号告警。
- [ ] 增加 check-only 脚本。

### Fixed

- [ ] 暂无。

## Update Rules For Future AI

后续任何 AI 助手如果继续处理这条基线任务，必须同步更新本文件：

1. 修复前，先在 `In Progress` 或 `Remaining` 中标记准备处理的项。
2. 修复后，更新 `Completed`、`Fixed`、`Remaining` 三个区域。
3. 如果某个问题不是 bug，而是团队决定接受的历史现状，需要在文档里明确记录“接受原因”。
4. 如果新增了 check-only 脚本，也要把最终脚本名回填到本文档。
5. 如果 `ts:check` 在 OOM 修复后暴露出新的真实类型错误，必须新增一个“Post-OOM Type Errors” 小节记录。

## Recommended Next Step

下一个 AI 最应该做的是：

1. 先解决 `ts:check` OOM。
2. 同时把 `Tinyflow` vendored 目录从 ESLint 和 Prettier 中剔除。
3. 再开始做样式和格式基线清理。

这样可以最快把“校验结果不可信”的问题收敛掉。
