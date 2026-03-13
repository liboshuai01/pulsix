<template>
  <Dialog v-model="dialogVisible" title="接入异常详情" width="90%" :fullscreen="false" :scroll="true" max-height="78vh">
    <div v-loading="detailLoading">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="链路号">{{ detailData.traceId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="接入源">{{ detailData.sourceCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="原始事件编号">{{ detailData.rawEventId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属场景">{{ detailData.sceneCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="事件编码">{{ detailData.eventCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="异常时间">{{ detailData.occurTime ? formatDate(detailData.occurTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="异常阶段">
          <el-tag :type="getIngestStageTag(detailData.ingestStage)" effect="plain">
            {{ getIngestStageLabel(detailData.ingestStage) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="错误码">{{ detailData.errorCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="DLQ Topic">{{ detailData.errorTopicName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="重处理状态">
          <el-tag :type="getReprocessStatusTag(detailData.reprocessStatus)" effect="plain">
            {{ getReprocessStatusLabel(detailData.reprocessStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="记录状态">
          <el-tag :type="getIngestRecordStatusTag(detailData.status)" effect="plain">
            {{ getIngestRecordStatusLabel(detailData.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="错误说明" :span="3">
          <div class="whitespace-pre-wrap leading-22px">{{ detailData.errorMessage || '-' }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <ContentWrap class="mt-16px">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="原始坏报文" name="raw">
            <JsonEditor :model-value="ensureObject(detailData.rawPayloadJson)" mode="view" height="420px" />
          </el-tab-pane>
          <el-tab-pane label="标准化中间报文" name="standard">
            <JsonEditor :model-value="ensureObject(detailData.standardPayloadJson)" mode="view" height="420px" />
          </el-tab-pane>
        </el-tabs>
      </ContentWrap>
    </div>
  </Dialog>
</template>

<script lang="ts" setup>
import { formatDate } from '@/utils/formatTime'
import * as IngestErrorApi from '@/api/risk/ingest-error'
import {
  ensureObject,
  getIngestRecordStatusLabel,
  getIngestRecordStatusTag,
  getIngestStageLabel,
  getIngestStageTag,
  getReprocessStatusLabel,
  getReprocessStatusTag
} from './constants'

defineOptions({ name: 'RiskIngestErrorDetailDialog' })

const dialogVisible = ref(false)
const detailLoading = ref(false)
const activeTab = ref('raw')

const createDefaultDetailData = (): IngestErrorApi.IngestErrorDetailVO => ({
  id: undefined,
  traceId: '',
  sourceCode: '',
  sceneCode: '',
  eventCode: '',
  rawEventId: '',
  ingestStage: '',
  errorCode: '',
  errorMessage: '',
  errorTopicName: '',
  reprocessStatus: '',
  status: 1,
  occurTime: undefined,
  rawPayloadJson: {},
  standardPayloadJson: {}
})

const detailData = ref<IngestErrorApi.IngestErrorDetailVO>(createDefaultDetailData())

const open = async (id: number) => {
  dialogVisible.value = true
  detailLoading.value = true
  activeTab.value = 'raw'
  try {
    detailData.value = {
      ...createDefaultDetailData(),
      ...(await IngestErrorApi.getIngestError(id))
    }
  } finally {
    detailLoading.value = false
  }
}

defineExpose({ open })
</script>
