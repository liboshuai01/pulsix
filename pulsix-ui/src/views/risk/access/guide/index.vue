<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskAccessGuide' })

const pageMeta = {
  groupTitle: '接入治理',
  pageTitle: '接入指引',
  pageDescription: '展示字段说明、标准事件 JSON 和 SDK 接入步骤，帮助业务方快速接入风控链路。',
  nextStep: '优先整合事件字段定义、标准事件预览与接入源信息，补出字段说明、标准事件 JSON 和接入步骤展示。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
