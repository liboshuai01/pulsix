<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskEventModel' })

const pageMeta = {
  groupTitle: '配置中心',
  pageTitle: '事件模型',
  pageDescription: '管理事件类型、字段定义、样例报文、默认值、必填校验和标准事件预览。',
  nextStep: '对接 event_schema 与 event_field_def 的 CRUD，并补出标准事件预览能力，优先覆盖 trade 事件模型。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
