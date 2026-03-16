<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskListCenter' })

const pageMeta = {
  groupTitle: '配置中心',
  pageTitle: '名单中心',
  pageDescription: '管理黑白名单、标签名单，支持导入导出、启停和同步 Redis。',
  nextStep: '对接 list_set 与 list_item 的 CRUD、导入和同步 Redis，优先完成 D_BLACK_001 的导入与同步验收。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
