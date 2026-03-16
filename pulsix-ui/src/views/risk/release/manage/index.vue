<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskReleaseManage' })

const pageMeta = {
  groupTitle: '发布中心',
  pageTitle: '发布管理',
  pageDescription: '执行发布前校验、依赖分析、快照编译和正式发布，生成可下发的场景版本。',
  nextStep: '补发布前校验与快照编译流程，优先完成 scene_release v1 的生成和发布入口联调。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
