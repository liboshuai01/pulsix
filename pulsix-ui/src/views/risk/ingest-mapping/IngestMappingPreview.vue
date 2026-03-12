<template>
  <Dialog v-model="dialogVisible" title="字段映射预览" width="1220px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="92px" class="mb-12px">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="接入源" prop="sourceCode">
            <el-select v-model="formData.sourceCode" class="w-full" filterable placeholder="请选择接入源">
              <el-option
                v-for="item in sourceOptions"
                :key="item.sourceCode"
                :label="`${item.sourceName} (${item.sourceCode})`"
                :value="item.sourceCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="所属场景" prop="sceneCode">
            <el-select v-model="formData.sceneCode" class="w-full" filterable placeholder="请选择所属场景" @change="handleSceneChange">
              <el-option
                v-for="item in sceneOptions"
                :key="item.sceneCode"
                :label="`${item.sceneName} (${item.sceneCode})`"
                :value="item.sceneCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="所属事件" prop="eventCode">
            <el-select v-model="formData.eventCode" class="w-full" filterable placeholder="请选择所属事件">
              <el-option
                v-for="item in eventOptions"
                :key="item.eventCode"
                :label="`${item.eventName} (${item.eventCode})`"
                :value="item.eventCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <div class="mb-12px flex gap-8px">
        <el-button type="primary" :loading="previewLoading" @click="handlePreview">开始预览</el-button>
        <el-button @click="handleResetDemo">重置 Demo 报文</el-button>
      </div>
    </el-form>

    <el-alert
      v-if="previewData?.mappedFields?.length"
      :title="`已命中映射字段：${previewData.mappedFields.join('、')}`"
      type="info"
      show-icon
      class="mb-12px"
    />
    <el-alert
      v-if="previewData?.missingRequiredFields?.length"
      :title="`缺失必填字段：${previewData.missingRequiredFields.join('、')}`"
      type="warning"
      show-icon
      class="mb-12px"
    />
    <el-alert
      v-if="previewData?.defaultedFields?.length"
      :title="`已用默认值补齐字段：${previewData.defaultedFields.join('、')}`"
      type="success"
      show-icon
      class="mb-12px"
    />

    <el-row :gutter="16">
      <el-col :span="12">
        <div class="mb-8px font-600">原始报文</div>
        <JsonEditor v-model="formData.rawEventJson" mode="code" height="460px" @error="onJsonError" />
        <div v-if="hasJsonError" class="mt-8px text-12px text-[var(--el-color-danger)]">
          JSON 格式不正确，请修正后再预览
        </div>
      </el-col>
      <el-col :span="12">
        <div class="mb-8px font-600">标准事件预览</div>
        <JsonEditor v-model="standardEventJson" mode="view" height="460px" />
      </el-col>
    </el-row>
  </Dialog>
</template>

<script lang="ts" setup>
import { isEmpty } from '@/utils/is'
import * as IngestMappingApi from '@/api/risk/ingestMapping'
import * as IngestSourceApi from '@/api/risk/ingestSource'
import * as SceneApi from '@/api/risk/scene'
import * as EventSchemaApi from '@/api/risk/eventSchema'

defineOptions({ name: 'RiskIngestMappingPreview' })

const message = useMessage()

const dialogVisible = ref(false)
const previewLoading = ref(false)
const hasJsonError = ref(false)
const sourceOptions = ref<IngestSourceApi.IngestSourceVO[]>([])
const sceneOptions = ref<SceneApi.SceneVO[]>([])
const eventOptions = ref<EventSchemaApi.EventSchemaVO[]>([])
const previewData = ref<IngestMappingApi.IngestMappingPreviewVO>()
const standardEventJson = ref<Record<string, any>>({})
const formRef = ref()

const createDemoRawEventJson = () => ({
  event_id: 'E_RAW_9103',
  occur_time_ms: 1773287100000,
  req: {
    traceId: 'T_RAW_9103'
  },
  uid: ' U9003 ',
  dev_id: 'D9003',
  client_ip: '88.66.55.44',
  pay_amt: 256800,
  trade_result: 'ok'
})

const createDefaultFormData = (): IngestMappingApi.IngestMappingPreviewReqVO => ({
  sourceCode: 'trade_http_demo',
  sceneCode: 'TRADE_RISK',
  eventCode: 'TRADE_EVENT',
  rawEventJson: createDemoRawEventJson()
})

const formData = ref<IngestMappingApi.IngestMappingPreviewReqVO>(createDefaultFormData())
const formRules = reactive({
  sourceCode: [{ required: true, message: '接入源不能为空', trigger: 'change' }],
  sceneCode: [{ required: true, message: '所属场景不能为空', trigger: 'change' }],
  eventCode: [{ required: true, message: '所属事件不能为空', trigger: 'change' }]
})

const loadBaseOptions = async () => {
  const [sourceData, sceneData] = await Promise.all([
    IngestSourceApi.getIngestSourcePage({ pageNo: 1, pageSize: 200 }),
    SceneApi.getScenePage({ pageNo: 1, pageSize: 200 })
  ])
  sourceOptions.value = sourceData.list
  sceneOptions.value = sceneData.list
}

const loadEventOptions = async (sceneCode: string) => {
  if (!sceneCode) {
    eventOptions.value = []
    return
  }
  const data = await EventSchemaApi.getEventSchemaPage({ pageNo: 1, pageSize: 200, sceneCode })
  eventOptions.value = data.list
}

const resetState = () => {
  formData.value = createDefaultFormData()
  previewData.value = undefined
  standardEventJson.value = {}
  hasJsonError.value = false
}

const handleSceneChange = async () => {
  await loadEventOptions(formData.value.sceneCode)
  const exists = eventOptions.value.some((item) => item.eventCode === formData.value.eventCode)
  if (!exists) {
    formData.value.eventCode = eventOptions.value[0]?.eventCode || ''
  }
}

const handleResetDemo = () => {
  formData.value.rawEventJson = createDemoRawEventJson()
  previewData.value = undefined
  standardEventJson.value = {}
  hasJsonError.value = false
}

const open = async (context?: Partial<IngestMappingApi.IngestMappingPreviewReqVO>) => {
  dialogVisible.value = true
  resetState()
  await loadBaseOptions()
  if (context) {
    formData.value = {
      ...formData.value,
      ...context,
      rawEventJson: context.rawEventJson || formData.value.rawEventJson
    }
  }
  await loadEventOptions(formData.value.sceneCode)
  await handlePreview()
}
defineExpose({ open })

const handlePreview = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return
  if (hasJsonError.value) {
    message.warning('JSON 格式不正确，请修正后再预览')
    return
  }
  previewLoading.value = true
  try {
    const data = await IngestMappingApi.previewIngestMapping(formData.value)
    previewData.value = data
    standardEventJson.value = data.standardEventJson || {}
  } finally {
    previewLoading.value = false
  }
}

const onJsonError = (errors: any) => {
  hasJsonError.value = !isEmpty(errors)
}
</script>
