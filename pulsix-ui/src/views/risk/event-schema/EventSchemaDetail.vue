<template>
  <Dialog v-model="dialogVisible" title="事件 Schema 详情" width="780px">
    <el-descriptions v-loading="detailLoading" :column="2" border>
      <el-descriptions-item label="主键编号">{{ detailData.id }}</el-descriptions-item>
      <el-descriptions-item label="所属场景">{{ detailData.sceneCode }}</el-descriptions-item>
      <el-descriptions-item label="事件编码">{{ detailData.eventCode }}</el-descriptions-item>
      <el-descriptions-item label="事件名称">{{ detailData.eventName }}</el-descriptions-item>
      <el-descriptions-item label="事件类别">
        {{ getRiskEventSchemaTypeLabel(detailData.eventType) }}
      </el-descriptions-item>
      <el-descriptions-item label="接入方式">
        {{ getRiskEventSchemaSourceTypeLabel(detailData.sourceType) }}
      </el-descriptions-item>
      <el-descriptions-item label="模型版本">{{ detailData.version ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="标准 Topic">{{ detailData.standardTopicName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="原始 Topic" :span="2">{{ detailData.rawTopicName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ detailData.creator || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">
        {{ detailData.createTime ? formatDate(detailData.createTime) : '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="更新者">{{ detailData.updater || '-' }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">
        {{ detailData.updateTime ? formatDate(detailData.updateTime) : '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="模型说明" :span="2">
        <div class="whitespace-pre-wrap leading-22px">{{ detailData.description || '-' }}</div>
      </el-descriptions-item>
    </el-descriptions>
  </Dialog>
</template>

<script lang="ts" setup>
import { formatDate } from '@/utils/formatTime'
import * as EventSchemaApi from '@/api/risk/eventSchema'
import { getRiskEventSchemaSourceTypeLabel, getRiskEventSchemaTypeLabel } from './constants'

defineOptions({ name: 'RiskEventSchemaDetail' })

const dialogVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<EventSchemaApi.EventSchemaVO>({
  id: undefined,
  sceneCode: '',
  eventCode: '',
  eventName: '',
  eventType: '',
  sourceType: '',
  version: 1
})

const open = async (id: number) => {
  dialogVisible.value = true
  detailLoading.value = true
  try {
    detailData.value = await EventSchemaApi.getEventSchema(id)
  } finally {
    detailLoading.value = false
  }
}
defineExpose({ open })
</script>
