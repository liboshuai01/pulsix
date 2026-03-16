<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskRuleCenter' })

const pageMeta = {
  groupTitle: '配置中心',
  pageTitle: '规则中心',
  pageDescription: '管理规则表达式、命中动作、优先级和命中原因模板。',
  nextStep: '补 rule_def 的 CRUD、表达式校验与动作配置，优先录入 TRADE_R001 到 TRADE_R005 样例规则。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
