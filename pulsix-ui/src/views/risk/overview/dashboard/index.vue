<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskDashboard' })

const pageMeta = {
  groupTitle: '风控总览',
  pageTitle: 'Dashboard',
  pageDescription: '查看事件量、决策量、命中规则、活跃版本、延迟和异常。',
  nextStep: '补充首页指标接口和图表卡片，优先覆盖事件量、决策量、动作分布、命中 Top 规则与错误数。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
