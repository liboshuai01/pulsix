<template>
  <Dialog v-model="dialogVisible" title="流式特征详情" width="920px">
    <el-descriptions v-loading="detailLoading" :column="2" border>
      <el-descriptions-item label="主键编号">{{ detailData.id }}</el-descriptions-item>
      <el-descriptions-item label="所属场景">{{ detailData.sceneCode }}</el-descriptions-item>
      <el-descriptions-item label="特征编码">{{ detailData.featureCode }}</el-descriptions-item>
      <el-descriptions-item label="特征名称">{{ detailData.featureName }}</el-descriptions-item>
      <el-descriptions-item label="实体类型">{{ detailData.entityName || detailData.entityType }}</el-descriptions-item>
      <el-descriptions-item label="实体键字段">{{ detailData.entityKeyFieldName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="来源事件" :span="2">
        {{ formatRiskFeatureSourceEvents(detailData.sourceEventCodes) }}
      </el-descriptions-item>
      <el-descriptions-item label="实体键表达式">{{ detailData.entityKeyExpr || '-' }}</el-descriptions-item>
      <el-descriptions-item label="聚合类型">{{ getRiskFeatureAggTypeLabel(detailData.aggType) }}</el-descriptions-item>
      <el-descriptions-item label="值类型">{{ getRiskFeatureValueTypeLabel(detailData.valueType) }}</el-descriptions-item>
      <el-descriptions-item label="窗口类型">{{ getRiskFeatureWindowTypeLabel(detailData.windowType) }}</el-descriptions-item>
      <el-descriptions-item label="窗口配置">{{ formatRiskFeatureWindow(detailData) }}</el-descriptions-item>
      <el-descriptions-item label="计入当前事件">{{ detailData.includeCurrentEvent === 1 ? '是' : '否' }}</el-descriptions-item>
      <el-descriptions-item label="TTL(秒)">{{ detailData.ttlSeconds ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="detailData.status" />
      </el-descriptions-item>
      <el-descriptions-item label="设计态版本">{{ detailData.version ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="取值表达式" :span="2">{{ detailData.valueExpr || '-' }}</el-descriptions-item>
      <el-descriptions-item label="过滤表达式" :span="2">{{ detailData.filterExpr || '-' }}</el-descriptions-item>
      <el-descriptions-item label="状态提示 JSON" :span="2">
        <pre class="m-0 whitespace-pre-wrap break-all leading-22px">{{ stateHintJsonContent }}</pre>
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
import * as FeatureStreamApi from '@/api/risk/featureStream'
import {
  formatRiskFeatureSourceEvents,
  formatRiskFeatureWindow,
  getRiskFeatureAggTypeLabel,
  getRiskFeatureValueTypeLabel,
  getRiskFeatureWindowTypeLabel
} from './constants'

defineOptions({ name: 'RiskFeatureStreamDetail' })

const dialogVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<FeatureStreamApi.FeatureStreamVO>({
  id: undefined,
  sceneCode: '',
  featureCode: '',
  featureName: '',
  entityType: '',
  sourceEventCodes: [],
  entityKeyExpr: '',
  aggType: '',
  valueType: '',
  windowType: '',
  windowSize: '',
  includeCurrentEvent: 1,
  status: 0
})

const stateHintJsonContent = computed(() => {
  return detailData.value.stateHintJson ? JSON.stringify(detailData.value.stateHintJson, null, 2) : '-'
})

const open = async (id: number) => {
  dialogVisible.value = true
  detailLoading.value = true
  try {
    detailData.value = await FeatureStreamApi.getFeatureStream(id)
  } finally {
    detailLoading.value = false
  }
}
defineExpose({ open })
</script>
