<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskEntityType' })

const pageMeta = {
  groupTitle: '配置中心',
  pageTitle: '实体类型',
  pageDescription: '定义 user、device、ip、mobile 等实体及主键字段，统一特征聚合口径。',
  nextStep: '对接 entity_type_def 的维护能力，并为后续特征页提供可选实体类型与主键字段引用。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
