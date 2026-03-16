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
  pageDescription: '展示字段说明、样例报文和 SDK 接入步骤，帮助业务方快速接入风控链路。',
  nextStep: '优先整合 event_schema、event_field_def 与 access_source_def，补出字段说明、样例事件和接入步骤展示。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
