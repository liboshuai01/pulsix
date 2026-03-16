<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskSimulationCase' })

const pageMeta = {
  groupTitle: '仿真测试',
  pageTitle: '仿真用例',
  pageDescription: '保存测试样例，作为回归验证和发布前校验的输入资产。',
  nextStep: '对接 simulation_case 的列表和编辑能力，优先保存 E_TRADE_0002 到 E_TRADE_0005 四条样例用例。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
