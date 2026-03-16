<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskAlertRecord' })

const pageMeta = {
  groupTitle: '告警中心',
  pageTitle: '告警记录',
  pageDescription: '查看飞书、钉钉、企业微信、Webhook 等告警发送记录。',
  nextStep: '对接 alert_record 查询接口，优先展示发送状态、静默命中和去重命中情况。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
