<template>
  <RiskCenterDialog
    v-model="dialogVisible"
    title="标准事件预览"
    width="min(1220px, calc(100vw - 48px))"
    max-height="calc(100vh - 220px)"
    scroll
  >
    <div v-loading="loading" class="risk-access-mapping-preview">
      <el-descriptions :column="2" border class="mb-16px">
        <el-descriptions-item label="场景编码">{{ previewMeta.sceneCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="事件编码">{{ previewMeta.eventCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="事件名称">{{ previewMeta.eventName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="标准 eventType">
          {{ previewMeta.eventType || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="接入源编码">
          {{ previewMeta.sourceCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="接入源名称">
          {{ previewMeta.sourceName || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <el-row :gutter="16">
        <el-col :xs="24" :lg="14">
          <el-card header="标准事件 JSON" shadow="never">
            <pre class="risk-access-mapping-preview__json">{{ standardJsonText }}</pre>
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="10">
          <el-card header="字段来源" shadow="never">
            <el-table :data="fieldSourceRows" border size="small" empty-text="暂无字段来源">
              <el-table-column label="字段名" prop="fieldName" min-width="140" />
              <el-table-column label="来源" prop="sourceLabel" min-width="120" />
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <el-card header="预览消息" shadow="never" class="mt-16px">
        <el-empty
          v-if="!previewResult?.messages?.length"
          description="未发现校验或转换提示"
          :image-size="60"
        />
        <ul v-else class="risk-access-mapping-preview__message-list">
          <li v-for="(message, index) in previewResult.messages" :key="`${index}-${message}`">
            {{ message }}
          </li>
        </ul>
      </el-card>
    </div>
  </RiskCenterDialog>
</template>

<script setup lang="ts">
import * as AccessMappingApi from '@/api/risk/access-mapping'
import RiskCenterDialog from '../../components/RiskCenterDialog.vue'
import { getFieldSourceLabel } from './constants'

defineOptions({ name: 'RiskAccessMappingPreviewDialog' })

type PreviewMeta = Partial<
  Pick<
    AccessMappingApi.AccessMappingVO,
    'sceneCode' | 'eventCode' | 'eventName' | 'eventType' | 'sourceCode' | 'sourceName'
  >
>

const dialogVisible = ref(false)
const loading = ref(false)
const previewMeta = ref<PreviewMeta>({})
const previewResult = ref<AccessMappingApi.AccessMappingPreviewVO>()

const standardJsonText = computed(() =>
  JSON.stringify(previewResult.value?.standardEventJson ?? {}, null, 2)
)

const fieldSourceRows = computed(() =>
  Object.entries(previewResult.value?.fieldSourceMap ?? {}).map(([fieldName, source]) => ({
    fieldName,
    source,
    sourceLabel: getFieldSourceLabel(source)
  }))
)

const buildPreviewPayload = (
  detail: AccessMappingApi.AccessMappingVO
): AccessMappingApi.AccessMappingSaveReqVO => ({
  id: detail.id,
  eventCode: detail.eventCode,
  sourceCode: detail.sourceCode,
  description: detail.description,
  rawSampleJson: detail.rawSampleJson || {},
  sampleHeadersJson: detail.sampleHeadersJson,
  rawFields: detail.rawFields || [],
  mappingRules: detail.mappingRules || []
})

const openById = async (id: number) => {
  dialogVisible.value = true
  loading.value = true
  previewResult.value = undefined
  previewMeta.value = {}
  try {
    const detail = await AccessMappingApi.getAccessMapping(id)
    previewMeta.value = {
      sceneCode: detail.sceneCode,
      eventCode: detail.eventCode,
      eventName: detail.eventName,
      eventType: detail.eventType,
      sourceCode: detail.sourceCode,
      sourceName: detail.sourceName
    }
    previewResult.value = await AccessMappingApi.previewStandardEvent(buildPreviewPayload(detail))
  } finally {
    loading.value = false
  }
}

const openWithPayload = async (
  payload: AccessMappingApi.AccessMappingSaveReqVO,
  meta: PreviewMeta = {}
) => {
  dialogVisible.value = true
  loading.value = true
  previewResult.value = undefined
  previewMeta.value = meta
  try {
    previewResult.value = await AccessMappingApi.previewStandardEvent(payload)
  } finally {
    loading.value = false
  }
}

defineExpose({ openById, openWithPayload })
</script>

<style scoped lang="scss">
.risk-access-mapping-preview {
  min-height: 280px;
}

.risk-access-mapping-preview__json {
  margin: 0;
  min-height: 320px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.6;
}

.risk-access-mapping-preview__message-list {
  margin: 0;
  padding-left: 20px;
  color: var(--el-text-color-regular);
  line-height: 1.8;
}
</style>
