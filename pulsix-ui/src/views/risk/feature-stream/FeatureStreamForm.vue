<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="980px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-alert
        title="流式特征第一版只做基础聚合能力；推荐优先配置 COUNT、SUM、DISTINCT_COUNT 三类样例。"
        type="info"
        :closable="false"
        class="mb-16px"
      />

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="所属场景" prop="sceneCode">
            <el-input
              v-model="formData.sceneCode"
              placeholder="请输入所属场景编码"
              :disabled="formType === 'update'"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="特征编码" prop="featureCode">
            <el-input
              v-model="formData.featureCode"
              placeholder="请输入特征编码"
              :disabled="formType === 'update'"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="特征名称" prop="featureName">
            <el-input v-model="formData.featureName" placeholder="请输入特征名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="实体类型" prop="entityType">
            <el-select v-model="formData.entityType" class="w-full" filterable placeholder="请选择实体类型">
              <el-option
                v-for="item in entityTypeOptions"
                :key="item.entityType"
                :label="`${item.entityName} (${item.keyFieldName})`"
                :value="item.entityType"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="来源事件" prop="sourceEventCodes">
            <el-select
              v-model="formData.sourceEventCodes"
              class="w-full"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择来源事件编码"
            >
              <el-option
                v-for="item in eventOptions"
                :key="item.eventCode"
                :label="`${item.eventName} (${item.eventCode})`"
                :value="item.eventCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="实体键表达式" prop="entityKeyExpr">
            <el-input v-model="formData.entityKeyExpr" placeholder="例如 userId、deviceId" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="聚合类型" prop="aggType">
            <el-select v-model="formData.aggType" class="w-full" placeholder="请选择聚合类型">
              <el-option
                v-for="item in riskFeatureAggTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="值类型" prop="valueType">
            <el-select v-model="formData.valueType" class="w-full" placeholder="请选择值类型">
              <el-option
                v-for="item in riskFeatureValueTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-select v-model="formData.status" class="w-full" placeholder="请选择状态">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="窗口类型" prop="windowType">
            <el-select v-model="formData.windowType" class="w-full" placeholder="请选择窗口类型">
              <el-option
                v-for="item in riskFeatureWindowTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="窗口大小" prop="windowSize">
            <el-input v-model="formData.windowSize" placeholder="例如 5m、30m、1h" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="滑动步长" prop="windowSlide">
            <el-input
              v-model="formData.windowSlide"
              placeholder="滑动窗口必填，例如 1m"
              :disabled="formData.windowType !== 'SLIDING'"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态 TTL(秒)" prop="ttlSeconds">
            <el-input-number v-model="formData.ttlSeconds" class="!w-full" :min="1" :precision="0" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="计入当前事件" prop="includeCurrentEvent">
            <el-switch
              v-model="formData.includeCurrentEvent"
              :active-value="1"
              :inactive-value="0"
              inline-prompt
              active-text="计入"
              inactive-text="不计入"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="取值表达式" prop="valueExpr">
            <el-input v-model="formData.valueExpr" :placeholder="valueExprPlaceholder" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="过滤表达式" prop="filterExpr">
            <el-input v-model="formData.filterExpr" placeholder="例如 result == 'SUCCESS'" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="状态提示 JSON">
            <el-input
              v-model="stateHintJsonText"
              type="textarea"
              :rows="5"
              placeholder="可选，例如 {&#10;  &quot;bucketHint&quot;: 1024&#10;}"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="口径说明" prop="description">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="4"
              placeholder="建议写清楚统计口径、过滤条件和边界说明"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <el-button :disabled="formLoading" @click="dialogVisible = false">取 消</el-button>
      <el-button type="primary" :loading="formLoading" @click="submitForm">确 定</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import * as EventSchemaApi from '@/api/risk/eventSchema'
import * as FeatureStreamApi from '@/api/risk/featureStream'
import {
  riskDurationPattern,
  riskFeatureAggTypeOptions,
  riskFeatureValueTypeOptions,
  riskFeatureWindowTypeOptions
} from './constants'

defineOptions({ name: 'RiskFeatureStreamForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const entityTypeOptions = ref<FeatureStreamApi.EntityTypeVO[]>([])
const eventOptions = ref<EventSchemaApi.EventSchemaVO[]>([])
const stateHintJsonText = ref('')

const createDefaultFormData = (): FeatureStreamApi.FeatureStreamVO => ({
  id: undefined,
  sceneCode: 'TRADE_RISK',
  featureCode: '',
  featureName: '',
  entityType: 'USER',
  sourceEventCodes: ['TRADE_EVENT'],
  entityKeyExpr: 'userId',
  aggType: 'COUNT',
  valueType: 'LONG',
  valueExpr: '',
  filterExpr: '',
  windowType: 'SLIDING',
  windowSize: '5m',
  windowSlide: '1m',
  includeCurrentEvent: 1,
  ttlSeconds: 600,
  stateHintJson: undefined,
  status: CommonStatusEnum.ENABLE,
  description: ''
})

const formData = ref<FeatureStreamApi.FeatureStreamVO>(createDefaultFormData())
const formRef = ref()

const needsValueExpr = computed(() => ['SUM', 'MAX', 'LATEST', 'DISTINCT_COUNT'].includes(formData.value.aggType))
const valueExprPlaceholder = computed(() => {
  if (!needsValueExpr.value) {
    return 'COUNT 可留空，默认只做计数'
  }
  if (formData.value.aggType === 'DISTINCT_COUNT') {
    return '请输入去重字段，例如 userId'
  }
  return '请输入聚合取值表达式，例如 amount'
})

const validateValueExpr = (_rule, value, callback) => {
  if (needsValueExpr.value && !(value && String(value).trim())) {
    callback(new Error('当前聚合类型需要填写取值表达式'))
    return
  }
  callback()
}

const validateWindowSlide = (_rule, value, callback) => {
  if (formData.value.windowType !== 'SLIDING') {
    callback()
    return
  }
  if (!value || !riskDurationPattern.test(String(value))) {
    callback(new Error('滑动窗口必须填写步长，例如 1m'))
    return
  }
  callback()
}

const validateTtlSeconds = (_rule, value, callback) => {
  if (value == null || Number(value) < 1) {
    callback(new Error('状态 TTL 必须大于 0'))
    return
  }
  callback()
}

const formRules = reactive({
  sceneCode: [{ required: true, message: '所属场景不能为空', trigger: 'blur' }],
  featureCode: [
    { required: true, message: '特征编码不能为空', trigger: 'blur' },
    {
      pattern: /^[A-Za-z][A-Za-z0-9_]*$/,
      message: '特征编码只允许字母、数字、下划线，且必须以字母开头',
      trigger: 'blur'
    }
  ],
  featureName: [{ required: true, message: '特征名称不能为空', trigger: 'blur' }],
  entityType: [{ required: true, message: '实体类型不能为空', trigger: 'change' }],
  sourceEventCodes: [{ type: 'array', required: true, message: '来源事件不能为空', trigger: 'change' }],
  entityKeyExpr: [{ required: true, message: '实体键表达式不能为空', trigger: 'blur' }],
  aggType: [{ required: true, message: '聚合类型不能为空', trigger: 'change' }],
  valueType: [{ required: true, message: '值类型不能为空', trigger: 'change' }],
  valueExpr: [{ validator: validateValueExpr, trigger: 'blur' }],
  windowType: [{ required: true, message: '窗口类型不能为空', trigger: 'change' }],
  windowSize: [
    { required: true, message: '窗口大小不能为空', trigger: 'blur' },
    { pattern: riskDurationPattern, message: '窗口大小格式必须是数字加 s/m/h/d，例如 5m', trigger: 'blur' }
  ],
  windowSlide: [{ validator: validateWindowSlide, trigger: 'blur' }],
  ttlSeconds: [{ validator: validateTtlSeconds, trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const resetForm = () => {
  formData.value = createDefaultFormData()
  stateHintJsonText.value = ''
  formRef.value?.resetFields()
}

const loadEntityTypeList = async () => {
  entityTypeOptions.value = await FeatureStreamApi.getEntityTypeList()
}

const loadEventOptions = async (sceneCode?: string) => {
  if (!sceneCode) {
    eventOptions.value = []
    return
  }
  const data = await EventSchemaApi.getEventSchemaPage({
    pageNo: 1,
    pageSize: 100,
    sceneCode,
    eventCode: undefined,
    eventName: undefined,
    eventType: undefined
  })
  eventOptions.value = data.list
}

const applyEntityKeyExprDefault = () => {
  const option = entityTypeOptions.value.find((item) => item.entityType === formData.value.entityType)
  if (!option) {
    return
  }
  if (formType.value === 'create' || !formData.value.entityKeyExpr) {
    formData.value.entityKeyExpr = option.keyFieldName
  }
}

const applyAggDefaults = () => {
  if (formData.value.aggType === 'COUNT') {
    formData.value.valueType = 'LONG'
  }
  if (formData.value.aggType === 'DISTINCT_COUNT') {
    formData.value.valueType = 'LONG'
  }
  if (formData.value.aggType === 'SUM' && !['INT', 'LONG', 'DECIMAL'].includes(formData.value.valueType)) {
    formData.value.valueType = 'DECIMAL'
  }
  if (formData.value.aggType === 'LATEST') {
    formData.value.windowType = 'NONE'
    formData.value.windowSlide = ''
    if (!formData.value.windowSize) {
      formData.value.windowSize = '1m'
    }
  }
}

watch(
  () => formData.value.entityType,
  () => {
    if (!dialogVisible.value) {
      return
    }
    applyEntityKeyExprDefault()
  }
)

watch(
  () => formData.value.sceneCode,
  async (value) => {
    if (!dialogVisible.value) {
      return
    }
    await loadEventOptions(value)
  }
)

watch(
  () => formData.value.aggType,
  () => {
    if (!dialogVisible.value) {
      return
    }
    applyAggDefaults()
  }
)

watch(
  () => formData.value.windowType,
  (value) => {
    if (!dialogVisible.value) {
      return
    }
    if (value !== 'SLIDING') {
      formData.value.windowSlide = ''
    }
  }
)

const open = async (type: 'create' | 'update', id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  formLoading.value = true
  try {
    await loadEntityTypeList()
    if (id) {
      formData.value = await FeatureStreamApi.getFeatureStream(id)
      stateHintJsonText.value = formData.value.stateHintJson
        ? JSON.stringify(formData.value.stateHintJson, null, 2)
        : ''
    }
    await loadEventOptions(formData.value.sceneCode)
    applyEntityKeyExprDefault()
    applyAggDefaults()
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return

  let stateHintJson: Record<string, any> | undefined
  const stateHintJsonRaw = stateHintJsonText.value.trim()
  if (stateHintJsonRaw) {
    try {
      stateHintJson = JSON.parse(stateHintJsonRaw)
    } catch {
      message.error('状态提示 JSON 格式不正确')
      return
    }
  }

  formLoading.value = true
  try {
    const data: FeatureStreamApi.FeatureStreamVO = {
      ...formData.value,
      sourceEventCodes: [...formData.value.sourceEventCodes],
      valueExpr: formData.value.valueExpr?.trim() || undefined,
      filterExpr: formData.value.filterExpr?.trim() || undefined,
      windowSlide: formData.value.windowType === 'SLIDING' ? formData.value.windowSlide?.trim() || undefined : undefined,
      stateHintJson,
      description: formData.value.description?.trim() || undefined
    }
    if (formType.value === 'create') {
      await FeatureStreamApi.createFeatureStream(data)
      message.success(t('common.createSuccess'))
    } else {
      await FeatureStreamApi.updateFeatureStream(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
