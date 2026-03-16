<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskReleaseDiffRollback' })

const pageMeta = {
  groupTitle: '发布中心',
  pageTitle: '版本对比 / 回滚',
  pageDescription: '查看不同版本之间的差异，并支持快速回滚到上一个稳定版本。',
  nextStep: '对接 scene_release_change 与回滚接口，优先支撑 v1 和 v2 的差异对比及一键回滚。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
