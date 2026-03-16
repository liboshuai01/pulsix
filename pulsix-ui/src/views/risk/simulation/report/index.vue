<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskSimulationReport' })

const pageMeta = {
  groupTitle: '仿真测试',
  pageTitle: '仿真报告',
  pageDescription: '查看每次仿真的执行结果、命中规则和是否通过预期。',
  nextStep: '补 simulation_report 的查询与详情展示，支持按场景、版本和通过状态快速筛选。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
