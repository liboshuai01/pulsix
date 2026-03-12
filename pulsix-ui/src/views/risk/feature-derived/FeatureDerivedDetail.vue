<template>
  <Dialog v-model="dialogVisible" title="派生特征详情" width="920px">
    <el-descriptions v-loading="detailLoading" :column="2" border>
      <el-descriptions-item label="主键编号">{{ detailData.id }}</el-descriptions-item>
      <el-descriptions-item label="所属场景">{{ detailData.sceneCode }}</el-descriptions-item>
      <el-descriptions-item label="特征编码">{{ detailData.featureCode }}</el-descriptions-item>
      <el-descriptions-item label="特征名称">{{ detailData.featureName }}</el-descriptions-item>
      <el-descriptions-item label="表达式引擎">
        {{ getRiskFeatureDerivedEngineTypeLabel(detailData.engineType) }}
      </el-descriptions-item>
      <el-descriptions-item label="结果类型">
        {{ getRiskFeatureDerivedValueTypeLabel(detailData.valueType) }}
      </el-descriptions-item>
      <el-descriptions-item label="依赖项" :span="2">
        {{ formatRiskFeatureDerivedDependsOn(detailData.dependsOnJson) }}
      </el-descriptions-item>
      <el-descriptions-item label="表达式内容" :span="2">
        <div class="whitespace-pre-wrap leading-22px">{{ detailData.exprContent || '-' }}</div>
      </el-descriptions-item>
      <el-descriptions-item label="脚本沙箱">{{ formatRiskFeatureDerivedSandbox(detailData.sandboxFlag) }}</el-descriptions-item>
      <el-descriptions-item label="超时配置">{{ detailData.timeoutMs != null ? `${detailData.timeoutMs} ms` : '-' }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="detailData.status" />
      </el-descriptions-item>
      <el-descriptions-item label="设计态版本">{{ detailData.version ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="扩展配置 JSON" :span="2">
        <pre class="m-0 whitespace-pre-wrap break-all leading-22px">{{ extraJsonContent }}</pre>
      </el-descriptions-item>
      <el-descriptions-item label="创建者">{{ detailData.creator || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">
        {{ detailData.createTime ? formatDate(detailData.createTime) : '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="更新者">{{ detailData.updater || '-' }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">
        {{ detailData.updateTime ? formatDate(detailData.updateTime) : '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="口径说明" :span="2">
        <div class="whitespace-pre-wrap leading-22px">{{ detailData.description || '-' }}</div>
      </el-descriptions-item>
    </el-descriptions>
  </Dialog>
</template>

<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as FeatureDerivedApi from '@/api/risk/featureDerived'
import {
  formatRiskFeatureDerivedDependsOn,
  formatRiskFeatureDerivedSandbox,
  getRiskFeatureDerivedEngineTypeLabel,
  getRiskFeatureDerivedValueTypeLabel
} from './constants'

defineOptions({ name: 'RiskFeatureDerivedDetail' })

const dialogVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<FeatureDerivedApi.FeatureDerivedVO>({
  id: undefined,
  sceneCode: '',
  featureCode: '',
  featureName: '',
  engineType: 'AVIATOR',
  exprContent: '',
  dependsOnJson: [],
  valueType: 'BOOLEAN',
  sandboxFlag: 1,
  status: 0
})

const extraJsonContent = computed(() => {
  return detailData.value.extraJson ? JSON.stringify(detailData.value.extraJson, null, 2) : '-'
})

const open = async (id: number) => {
  dialogVisible.value = true
  detailLoading.value = true
  try {
    detailData.value = await FeatureDerivedApi.getFeatureDerived(id)
  } finally {
    detailLoading.value = false
  }
}
defineExpose({ open })
</script>
