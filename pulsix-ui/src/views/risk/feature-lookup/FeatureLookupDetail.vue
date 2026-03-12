<template>
  <Dialog v-model="dialogVisible" title="查询特征详情" width="880px">
    <el-descriptions v-loading="detailLoading" :column="2" border>
      <el-descriptions-item label="主键编号">{{ detailData.id }}</el-descriptions-item>
      <el-descriptions-item label="所属场景">{{ detailData.sceneCode }}</el-descriptions-item>
      <el-descriptions-item label="特征编码">{{ detailData.featureCode }}</el-descriptions-item>
      <el-descriptions-item label="特征名称">{{ detailData.featureName }}</el-descriptions-item>
      <el-descriptions-item label="查询类型">{{ getRiskFeatureLookupTypeLabel(detailData.lookupType) }}</el-descriptions-item>
      <el-descriptions-item label="值类型">{{ getRiskFeatureLookupValueTypeLabel(detailData.valueType) }}</el-descriptions-item>
      <el-descriptions-item label="Key 表达式">{{ detailData.keyExpr || '-' }}</el-descriptions-item>
      <el-descriptions-item label="来源引用">{{ detailData.sourceRef || '-' }}</el-descriptions-item>
      <el-descriptions-item label="默认值">{{ detailData.defaultValue || '-' }}</el-descriptions-item>
      <el-descriptions-item label="超时/缓存">{{ formatRiskFeatureLookupTimeout(detailData) }}</el-descriptions-item>
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
import * as FeatureLookupApi from '@/api/risk/featureLookup'
import {
  formatRiskFeatureLookupTimeout,
  getRiskFeatureLookupTypeLabel,
  getRiskFeatureLookupValueTypeLabel
} from './constants'

defineOptions({ name: 'RiskFeatureLookupDetail' })

const dialogVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<FeatureLookupApi.FeatureLookupVO>({
  id: undefined,
  sceneCode: '',
  featureCode: '',
  featureName: '',
  lookupType: '',
  keyExpr: '',
  sourceRef: '',
  valueType: 'STRING',
  status: 0
})

const extraJsonContent = computed(() => {
  return detailData.value.extraJson ? JSON.stringify(detailData.value.extraJson, null, 2) : '-'
})

const open = async (id: number) => {
  dialogVisible.value = true
  detailLoading.value = true
  try {
    detailData.value = await FeatureLookupApi.getFeatureLookup(id)
  } finally {
    detailLoading.value = false
  }
}
defineExpose({ open })
</script>
