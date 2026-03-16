<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskRiskEvent' })

const pageMeta = {
  groupTitle: '查询分析',
  pageTitle: '风险事件查询',
  pageDescription: '查询高风险事件、TAG_ONLY 结果和可疑事件沉淀记录。',
  nextStep: '对接 risk_event 的分页与状态查询，优先支撑 REVIEW、REJECT、TAG_ONLY 三类结果过滤。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
