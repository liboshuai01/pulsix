<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskReleaseRecord' })

const pageMeta = {
  groupTitle: '发布中心',
  pageTitle: '发布记录',
  pageDescription: '查看 scene_release 的历史版本、状态、发布人和发布时间。',
  nextStep: '对接 scene_release 的分页与详情查询，支持查看 checksum、状态流转和编译报告摘要。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
