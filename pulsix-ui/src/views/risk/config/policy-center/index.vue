<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskPolicyCenter' })

const pageMeta = {
  groupTitle: '配置中心',
  pageTitle: '策略中心',
  pageDescription: '把多条规则编排为 FIRST_HIT 策略，并配置默认动作和执行顺序。',
  nextStep: '对接 policy_def 与 policy_rule_ref 的维护能力，优先完成 TRADE_RISK_POLICY_FIRST_HIT 的顺序配置。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
