<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskSimulationEvent' })

const pageMeta = {
  groupTitle: '仿真测试',
  pageTitle: '单条事件仿真',
  pageDescription: '输入事件 JSON，指定场景和版本，查看特征值、命中规则和最终动作。',
  nextStep: '补单条仿真请求与结果展示，优先输出 finalAction、命中规则和特征快照三个核心结果。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
