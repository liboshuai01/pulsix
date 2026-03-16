<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskAccessAuth' })

const pageMeta = {
  groupTitle: '接入治理',
  pageTitle: '鉴权配置',
  pageDescription: '管理 SDK 和 HTTP 接入的鉴权参数、签名方式和生效时间。',
  nextStep: '对接 access_auth_conf 的 CRUD 与校验逻辑，优先覆盖合法与非法请求的鉴权配置验收。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
