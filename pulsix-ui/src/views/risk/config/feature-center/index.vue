<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskFeatureCenter' })

const pageMeta = {
  groupTitle: '配置中心',
  pageTitle: '特征中心',
  pageDescription: '管理 Stream、Lookup、Derived Feature 三类特征，并作为规则与发布的关键输入。',
  nextStep: '对接 feature_def 与 feature_*_conf 的编辑能力，优先打通 user_trade_cnt_5m、device_in_blacklist、high_amt_flag 三类样例特征。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
