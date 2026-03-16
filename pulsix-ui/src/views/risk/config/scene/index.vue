<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskScene' })

const pageMeta = {
  groupTitle: '配置中心',
  pageTitle: '场景管理',
  pageDescription: '管理营销反作弊、提现审核、订单后置风控等场景，是其他配置对象的组织根。',
  nextStep: '对接 scene_def 的分页、创建、修改、启停与详情接口，优先打通 TRADE_RISK 的新增和查询闭环。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
