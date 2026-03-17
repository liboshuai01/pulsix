<template>
  <RiskCenterDialog
    v-model="dialogVisible"
    title="标准事件预览"
    width="1100px"
    max-height="calc(100vh - 240px)"
    scroll
  >
    <div v-loading="loading">
      <el-descriptions :column="2" border class="mb-16px">
        <el-descriptions-item label="场景编码">{{ detail?.sceneCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="事件编码">{{ detail?.eventCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="事件名称">{{ detail?.eventName || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-row :gutter="16">
        <el-col :span="14">
          <el-card header="标准事件 JSON" shadow="never">
            <pre class="risk-event-preview__json">{{ standardJsonText }}</pre>
          </el-card>
        </el-col>
        <el-col :span="10">
          <el-card header="字段摘要" shadow="never">
            <div class="risk-event-preview__meta">
              <div>
                <span class="risk-event-preview__label">必填字段</span>
                <el-tag
                  v-for="field in previewResult?.requiredFields || []"
                  :key="field"
                  type="danger"
                  effect="plain"
                  class="mr-6px mb-6px"
                >
                  {{ field }}
                </el-tag>
              </div>
              <div class="mt-12px">
                <span class="risk-event-preview__label">可选字段</span>
                <el-tag
                  v-for="field in previewResult?.optionalFields || []"
                  :key="field"
                  effect="plain"
                  class="mr-6px mb-6px"
                >
                  {{ field }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </RiskCenterDialog>
</template>

<script setup lang="ts">
import * as EventModelApi from '@/api/risk/event-model'
import RiskCenterDialog from '../../components/RiskCenterDialog.vue'

defineOptions({ name: 'RiskEventModelPreviewDialog' })

const dialogVisible = ref(false)
const loading = ref(false)
const detail = ref<EventModelApi.EventModelVO>()
const previewResult = ref<EventModelApi.EventModelPreviewVO>()

const standardJsonText = computed(() =>
  JSON.stringify(previewResult.value?.standardEventJson ?? {}, null, 2)
)

const buildPreviewPayload = (eventModel: EventModelApi.EventModelVO): EventModelApi.EventModelSaveReqVO => ({
  id: eventModel.id,
  sceneCode: eventModel.sceneCode,
  eventCode: eventModel.eventCode,
  eventName: eventModel.eventName,
  status: eventModel.status,
  description: eventModel.description,
  fields: eventModel.fields || []
})

const open = async (id: number) => {
  dialogVisible.value = true
  loading.value = true
  detail.value = undefined
  previewResult.value = undefined
  try {
    detail.value = await EventModelApi.getEventModel(id)
    if (!detail.value) {
      return
    }
    previewResult.value = await EventModelApi.previewStandardEvent(buildPreviewPayload(detail.value))
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.risk-event-preview__json {
  margin: 0;
  min-height: 280px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.6;
}

.risk-event-preview__label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
}
</style>
