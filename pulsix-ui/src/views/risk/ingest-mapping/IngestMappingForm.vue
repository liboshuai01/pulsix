<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="1100px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="接入源" prop="sourceCode">
            <el-select
              v-model="formData.sourceCode"
              class="w-full"
              :disabled="formType === 'update'"
              filterable
              placeholder="请选择接入源"
            >
              <el-option
                v-for="item in sourceOptions"
                :key="item.sourceCode"
                :label="`${item.sourceName} (${item.sourceCode})`"
                :value="item.sourceCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="所属场景" prop="sceneCode">
            <el-select
              v-model="formData.sceneCode"
              class="w-full"
              :disabled="formType === 'update'"
              filterable
              placeholder="请选择所属场景"
              @change="handleSceneChange"
            >
              <el-option
                v-for="item in sceneOptions"
                :key="item.sceneCode"
                :label="`${item.sceneName} (${item.sceneCode})`"
                :value="item.sceneCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="所属事件" prop="eventCode">
            <el-select
              v-model="formData.eventCode"
              class="w-full"
              :disabled="formType === 'update'"
              filterable
              placeholder="请选择所属事件"
              @change="handleEventChange"
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
          <el-form-item label="目标字段" prop="targetFieldCode">
            <el-select
              v-model="formData.targetFieldCode"
              class="w-full"
              :disabled="formType === 'update'"
              filterable
              placeholder="请选择目标字段"
              @change="handleTargetFieldChange"
            >
              <el-option
                v-for="item in fieldOptions"
                :key="item.fieldCode"
                :label="`${item.fieldName} (${item.fieldCode})`"
                :value="item.fieldCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="目标字段名称" prop="targetFieldName">
            <el-input v-model="formData.targetFieldName" disabled placeholder="随目标字段自动带出" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="排序号" prop="sortNo">
            <el-input-number v-model="formData.sortNo" :min="0" class="!w-full" controls-position="right" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="原始字段路径" prop="sourceFieldPath">
            <el-input
              v-model="formData.sourceFieldPath"
              placeholder="例如 $.uid、$.req.traceId；常量赋值时可留空"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="转换类型" prop="transformType">
            <el-select v-model="formData.transformType" class="w-full" placeholder="请选择转换类型">
              <el-option
                v-for="item in riskIngestMappingTransformTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="转换表达式" prop="transformExpr">
            <el-input
              v-model="formData.transformExpr"
              :rows="3"
              type="textarea"
              :placeholder="getTransformExprPlaceholder(formData.transformType)"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="默认值" prop="defaultValue">
            <el-input
              v-model="formData.defaultValue"
              :rows="3"
              type="textarea"
              placeholder="源字段缺失或转换结果为空时使用"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="是否必填" prop="requiredFlag">
            <el-switch v-model="formData.requiredFlag" :active-value="1" :inactive-value="0" />
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
      </el-row>

      <el-form-item label="清洗规则" prop="cleanRuleJson">
        <div class="w-full">
          <JsonEditor v-model="formData.cleanRuleJson" mode="code" height="220px" @error="onJsonError" />
          <div class="mt-8px text-12px text-[var(--el-text-color-secondary)]">
            当前支持：`trim`、`blankToNull`、`upperCase`、`lowerCase`
          </div>
          <div v-if="hasJsonError" class="mt-8px text-12px text-[var(--el-color-danger)]">
            JSON 格式不正确，请修正后再提交
          </div>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :loading="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { CommonStatusEnum } from '@/utils/constants'
import { isEmpty } from '@/utils/is'
import * as IngestMappingApi from '@/api/risk/ingestMapping'
import * as IngestSourceApi from '@/api/risk/ingestSource'
import * as SceneApi from '@/api/risk/scene'
import * as EventSchemaApi from '@/api/risk/eventSchema'
import * as EventFieldApi from '@/api/risk/eventField'
import { riskIngestMappingTransformTypeOptions } from './constants'

defineOptions({ name: 'RiskIngestMappingForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const hasJsonError = ref(false)
const sourceOptions = ref<IngestSourceApi.IngestSourceVO[]>([])
const sceneOptions = ref<SceneApi.SceneVO[]>([])
const eventOptions = ref<EventSchemaApi.EventSchemaVO[]>([])
const fieldOptions = ref<EventFieldApi.EventFieldVO[]>([])

const createDefaultFormData = (): IngestMappingApi.IngestMappingVO => ({
  id: undefined,
  sourceCode: 'trade_http_demo',
  sceneCode: 'TRADE_RISK',
  eventCode: 'TRADE_EVENT',
  sourceFieldPath: '',
  targetFieldCode: '',
  targetFieldName: '',
  transformType: 'DIRECT',
  transformExpr: '',
  defaultValue: '',
  requiredFlag: 1,
  cleanRuleJson: {},
  sortNo: 0,
  status: CommonStatusEnum.ENABLE
})

const formData = ref<IngestMappingApi.IngestMappingVO>(createDefaultFormData())
const formRules = reactive({
  sourceCode: [{ required: true, message: '接入源不能为空', trigger: 'change' }],
  sceneCode: [{ required: true, message: '所属场景不能为空', trigger: 'change' }],
  eventCode: [{ required: true, message: '所属事件不能为空', trigger: 'change' }],
  targetFieldCode: [{ required: true, message: '目标字段不能为空', trigger: 'change' }],
  sourceFieldPath: [
    {
      validator: (_rule, value, callback) => {
        if (formData.value.transformType !== 'CONST' && !value) {
          callback(new Error('除常量赋值外，原始字段路径不能为空'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  transformType: [{ required: true, message: '转换类型不能为空', trigger: 'change' }],
  sortNo: [{ required: true, message: '排序号不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})
const formRef = ref()

const getTransformExprPlaceholder = (transformType?: string) => {
  if (transformType === 'CONST') {
    return '常量赋值时填写常量内容，例如 trade'
  }
  if (transformType === 'ENUM_MAP') {
    return '枚举映射时填写 JSON 对象，例如 {"ok":"SUCCESS","fail":"FAIL"}'
  }
  return '没有额外表达式时可留空'
}

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

const loadFieldOptions = async (sceneCode: string, eventCode: string) => {
  if (!sceneCode || !eventCode) {
    fieldOptions.value = []
    return
  }
  const data = await EventFieldApi.getEventFieldPage({ pageNo: 1, pageSize: 200, sceneCode, eventCode })
  fieldOptions.value = data.list
  syncTargetFieldName()
}

const syncTargetFieldName = () => {
  const field = fieldOptions.value.find((item) => item.fieldCode === formData.value.targetFieldCode)
  formData.value.targetFieldName = field?.fieldName || ''
}

const resetForm = () => {
  formData.value = createDefaultFormData()
  hasJsonError.value = false
  formRef.value?.resetFields()
}

const handleSceneChange = async () => {
  formData.value.eventCode = ''
  formData.value.targetFieldCode = ''
  formData.value.targetFieldName = ''
  await loadEventOptions(formData.value.sceneCode)
  fieldOptions.value = []
}

const handleEventChange = async () => {
  formData.value.targetFieldCode = ''
  formData.value.targetFieldName = ''
  await loadFieldOptions(formData.value.sceneCode, formData.value.eventCode)
}

const handleTargetFieldChange = () => {
  syncTargetFieldName()
}

const open = async (type: 'create' | 'update', id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  await loadBaseOptions()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await IngestMappingApi.getIngestMapping(id)
      formData.value.cleanRuleJson = formData.value.cleanRuleJson || {}
    } finally {
      formLoading.value = false
    }
  }
  await loadEventOptions(formData.value.sceneCode)
  await loadFieldOptions(formData.value.sceneCode, formData.value.eventCode)
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return
  if (hasJsonError.value) {
    message.warning('JSON 格式不正确，请修正后再提交')
    return
  }
  formLoading.value = true
  try {
    const data = {
      ...formData.value,
      cleanRuleJson: formData.value.cleanRuleJson || {}
    }
    if (formType.value === 'create') {
      await IngestMappingApi.createIngestMapping(data)
      message.success(t('common.createSuccess'))
    } else {
      await IngestMappingApi.updateIngestMapping(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const onJsonError = (errors: any) => {
  hasJsonError.value = !isEmpty(errors)
}
</script>
