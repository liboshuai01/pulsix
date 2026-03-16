<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskAccessSource' })

const pageMeta = {
  groupTitle: '接入治理',
  pageTitle: '接入源管理',
  pageDescription: '管理 HTTP、SDK 等接入来源和启停状态，统一维护风控接入边界。',
  nextStep: '对接 access_source_def 的 CRUD，优先支持接入源启停、来源类型和允许场景维护。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
