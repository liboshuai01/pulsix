<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskDecisionLog' })

const pageMeta = {
  groupTitle: '查询分析',
  pageTitle: '决策日志',
  pageDescription: '按 traceId、eventId、sceneCode 查询一次完整决策结果。',
  nextStep: '对接 decision_log 查询接口，优先支持 traceId、eventId、sceneCode 的组合检索。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
