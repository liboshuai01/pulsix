<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskAccessError' })

const pageMeta = {
  groupTitle: '接入治理',
  pageTitle: '错误治理',
  pageDescription: '统一查询解析失败、标准化失败和 DLQ 事件，并按错误类型进行治理。',
  nextStep: '对接 risk_error_log 查询接口，优先支持错误阶段、错误类型和处理状态筛选。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
