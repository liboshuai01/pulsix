<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskAlertConfig' })

const pageMeta = {
  groupTitle: '告警中心',
  pageTitle: '告警配置',
  pageDescription: '管理模板、渠道、级别、静默、去重和限流等告警规则。',
  nextStep: '对接 alert_channel_def、alert_template_def 与 alert_rule_def，优先打通渠道和模板的配置闭环。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
