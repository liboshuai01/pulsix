<template>
  <RiskCenterDialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="min(1480px, calc(100vw - 64px))"
    max-height="calc(100vh - 200px)"
    scroll
  >
    <div v-loading="formLoading" class="risk-event-model-form">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基础信息" name="basic">
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="场景编码" prop="sceneCode">
                  <el-select
                    v-model="formData.sceneCode"
                    placeholder="请选择场景"
                    filterable
                    :disabled="formType === 'update'"
                  >
                    <el-option
                      v-for="scene in sceneOptions"
                      :key="scene.sceneCode"
                      :label="`${scene.sceneName} (${scene.sceneCode})`"
                      :value="scene.sceneCode"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="事件编码" prop="eventCode">
                  <el-input
                    v-model="formData.eventCode"
                    placeholder="请输入事件编码"
                    :disabled="formType === 'update'"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="事件名称" prop="eventName">
                  <el-input v-model="formData.eventName" placeholder="请输入事件名称" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="状态">
                  <div class="flex items-center gap-8px flex-wrap">
                    <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="formData.status" />
                    <span
                      v-if="formType === 'create'"
                      class="text-12px text-[var(--el-text-color-secondary)]"
                    >
                      新增默认关闭，创建后可在列表中启用
                    </span>
                  </div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="版本">
                  <el-input :model-value="formData.version ?? '-'" disabled />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="描述" prop="description">
              <el-input
                v-model="formData.description"
                type="textarea"
                :rows="4"
                placeholder="请输入事件模型描述"
              />
            </el-form-item>
            <el-alert
              title="事件与接入源的绑定关系已迁移到《接入映射》页统一维护，这里只维护标准字段定义。"
              type="info"
              :closable="false"
            />
          </el-tab-pane>

          <el-tab-pane label="字段定义" name="fields">
            <div class="mb-12px flex items-center justify-between">
              <div class="text-13px text-[var(--el-text-color-secondary)]">
                维护字段顺序、类型、必填标记和字段级默认值/样例值。
              </div>
              <el-button type="primary" plain @click="addField">
                <Icon icon="ep:plus" class="mr-5px" />新增字段
              </el-button>
            </div>
            <el-alert
              class="mb-12px"
              title="前 5 个公共字段由系统维护，默认存在且固定排在最前，不支持删除或调整。"
              type="info"
              :closable="false"
            />
            <el-table
              :data="formData.fields"
              :fit="false"
              border
              row-key="__key"
              scrollbar-always-on
              class="risk-event-model-form__field-table"
            >
              <el-table-column label="排序" width="96" align="center" header-align="center">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.sortNo"
                    :min="1"
                    :disabled="row.__systemField"
                    controls-position="right"
                    class="risk-event-model-form__sort-input"
                  />
                </template>
              </el-table-column>
              <el-table-column label="字段名" width="170">
                <template #default="{ row }">
                  <el-input v-model="row.fieldName" placeholder="fieldName" :disabled="row.__systemField" />
                </template>
              </el-table-column>
              <el-table-column label="显示名" width="170">
                <template #default="{ row }">
                  <el-input
                    v-model="row.fieldLabel"
                    placeholder="字段显示名"
                    :disabled="row.__systemField"
                  />
                </template>
              </el-table-column>
              <el-table-column label="字段类型" width="150">
                <template #default="{ row }">
                  <el-select v-model="row.fieldType" placeholder="请选择字段类型" :disabled="row.__systemField">
                    <el-option
                      v-for="option in EVENT_FIELD_TYPE_OPTIONS"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="必填" width="90" align="center">
                <template #default="{ row }">
                  <el-switch
                    v-model="row.requiredFlag"
                    :active-value="1"
                    :inactive-value="0"
                    :disabled="row.__systemField"
                  />
                </template>
              </el-table-column>
              <el-table-column label="默认值" width="170">
                <template #default="{ row }">
                  <el-input v-model="row.defaultValue" placeholder="默认值" :disabled="row.__systemField" />
                </template>
              </el-table-column>
              <el-table-column label="样例值" width="190">
                <template #default="{ row }">
                  <el-input v-model="row.sampleValue" placeholder="样例值" :disabled="row.__systemField" />
                </template>
              </el-table-column>
              <el-table-column label="描述" width="210">
                <template #default="{ row }">
                  <el-input v-model="row.description" placeholder="字段描述" :disabled="row.__systemField" />
                </template>
              </el-table-column>
              <el-table-column
                label="操作"
                width="136"
                fixed="right"
                align="center"
                header-align="center"
              >
                <template #default="{ row, $index }">
                  <div v-if="row.__systemField" class="text-12px text-[var(--el-text-color-secondary)]">
                    系统字段
                  </div>
                  <div v-else class="risk-event-model-form__row-actions">
                    <el-button
                      link
                      type="primary"
                      class="risk-event-model-form__row-action-btn"
                      :disabled="!canMoveFieldUp($index)"
                      @click="moveFieldUp($index)"
                    >
                      上移
                    </el-button>
                    <el-button
                      link
                      type="primary"
                      class="risk-event-model-form__row-action-btn"
                      :disabled="row.__systemField || $index >= formData.fields.length - 1"
                      @click="moveFieldDown($index)"
                    >
                      下移
                    </el-button>
                    <el-button
                      link
                      type="danger"
                      class="risk-event-model-form__row-action-btn"
                      @click="removeField($index)"
                    >
                      删除
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-form>
    </div>

    <template #footer>
      <div class="risk-event-model-form__footer">
        <el-button type="primary" :loading="formLoading" @click="submitForm">确 定</el-button>
        <el-button @click="dialogVisible = false">取 消</el-button>
      </div>
    </template>
  </RiskCenterDialog>
</template>

<script setup lang="ts">
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE } from '@/utils/dict'
import * as SceneApi from '@/api/risk/scene'
import * as EventModelApi from '@/api/risk/event-model'
import RiskCenterDialog from '../../components/RiskCenterDialog.vue'
import { EVENT_FIELD_TYPE_OPTIONS } from './constants'
import type { FormRules } from 'element-plus'

defineOptions({ name: 'RiskEventModelForm' })

type EventFieldRow = EventModelApi.EventFieldItemVO & { __key: string; __systemField: boolean }
type EventModelFormData = EventModelApi.EventModelSaveReqVO & {
  fields: EventFieldRow[]
  version?: number
}
type PublicFieldSpec = {
  fieldName: string
  fieldLabel: string
  fieldType: string
  requiredFlag: number
  sortNo: number
  description: string
  buildDefaultValue: (sceneCode: string, eventCode: string) => string
  buildSampleValue: (sceneCode: string, eventCode: string) => string
}

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const activeTab = ref('basic')
const formRef = ref()
const sceneOptions = ref<SceneApi.SceneVO[]>([])

const createFieldKey = () => `${Date.now()}-${Math.random()}`
const buildEventIdSample = (eventCode: string) =>
  eventCode ? `AUTO_${eventCode}_EVENT_ID` : 'AUTO_EVENT_ID'
const buildTraceIdSample = (eventCode: string) =>
  eventCode ? `AUTO_${eventCode}_TRACE_ID` : 'AUTO_TRACE_ID'
const buildEventTimeSample = () => '2026-03-08T10:00:00'
const PUBLIC_FIELD_SPECS: PublicFieldSpec[] = [
  {
    fieldName: 'eventId',
    fieldLabel: '事件ID',
    fieldType: 'STRING',
    requiredFlag: 1,
    sortNo: 1,
    description: '事件唯一标识',
    buildDefaultValue: () => '',
    buildSampleValue: (_sceneCode, eventCode) => buildEventIdSample(eventCode)
  },
  {
    fieldName: 'traceId',
    fieldLabel: '链路ID',
    fieldType: 'STRING',
    requiredFlag: 1,
    sortNo: 2,
    description: '全链路追踪号',
    buildDefaultValue: () => '',
    buildSampleValue: (_sceneCode, eventCode) => buildTraceIdSample(eventCode)
  },
  {
    fieldName: 'sceneCode',
    fieldLabel: '场景编码',
    fieldType: 'STRING',
    requiredFlag: 1,
    sortNo: 3,
    description: '场景编码',
    buildDefaultValue: (sceneCode) => sceneCode || '',
    buildSampleValue: (sceneCode) => sceneCode || ''
  },
  {
    fieldName: 'eventCode',
    fieldLabel: '事件编码',
    fieldType: 'STRING',
    requiredFlag: 1,
    sortNo: 4,
    description: '事件编码',
    buildDefaultValue: (_sceneCode, eventCode) => eventCode || '',
    buildSampleValue: (_sceneCode, eventCode) => eventCode || ''
  },
  {
    fieldName: 'eventTime',
    fieldLabel: '事件时间',
    fieldType: 'DATETIME',
    requiredFlag: 1,
    sortNo: 5,
    description: '事件发生时间',
    buildDefaultValue: () => '',
    buildSampleValue: () => buildEventTimeSample()
  }
]
const PUBLIC_FIELD_NAME_SET = new Set(PUBLIC_FIELD_SPECS.map((field) => field.fieldName))

const createSystemFieldRow = (
  spec: PublicFieldSpec,
  sceneCode: string,
  eventCode: string,
  source?: Partial<EventFieldRow>
): EventFieldRow => ({
  __key: source?.__key || createFieldKey(),
  __systemField: true,
  fieldName: spec.fieldName,
  fieldLabel: spec.fieldLabel,
  fieldType: spec.fieldType,
  requiredFlag: spec.requiredFlag,
  defaultValue: spec.buildDefaultValue(sceneCode, eventCode),
  sampleValue: spec.buildSampleValue(sceneCode, eventCode),
  description: spec.description,
  sortNo: source?.sortNo ?? spec.sortNo
})

const createFieldRow = (): EventFieldRow => ({
  __key: createFieldKey(),
  __systemField: false,
  fieldName: '',
  fieldLabel: '',
  fieldType: 'STRING',
  requiredFlag: 0,
  defaultValue: '',
  sampleValue: '',
  description: '',
  sortNo: 1
})

const createDefaultFormData = (): EventModelFormData => ({
  id: undefined,
  sceneCode: '',
  eventCode: '',
  eventName: '',
  version: undefined,
  status: CommonStatusEnum.DISABLE,
  description: '',
  fields: PUBLIC_FIELD_SPECS.map((spec) => createSystemFieldRow(spec, '', ''))
})

const formData = ref<EventModelFormData>(createDefaultFormData())

const formRules = reactive<FormRules>({
  sceneCode: [{ required: true, message: '场景编码不能为空', trigger: 'change' }],
  eventCode: [
    { required: true, message: '事件编码不能为空', trigger: 'blur' },
    {
      pattern: /^[A-Z0-9_]+$/,
      message: '事件编码只能包含大写字母、数字和下划线',
      trigger: 'blur'
    }
  ],
  eventName: [{ required: true, message: '事件名称不能为空', trigger: 'blur' }]
})

const loadSceneOptions = async () => {
  sceneOptions.value = await SceneApi.getSimpleSceneList()
}

const mapToFieldRow = (field: EventModelApi.EventFieldItemVO, index: number): EventFieldRow => ({
  ...field,
  __key: createFieldKey(),
  __systemField: false,
  fieldLabel: field.fieldLabel || '',
  defaultValue: field.defaultValue || '',
  sampleValue: field.sampleValue || '',
  description: field.description || '',
  sortNo: field.sortNo ?? index + 1
})

const ensurePublicFields = (
  fields: EventFieldRow[],
  sceneCode: string,
  eventCode: string
): EventFieldRow[] => {
  const sortedFields = [...fields].sort((a, b) => (a.sortNo || 0) - (b.sortNo || 0))
  const publicFieldMap = new Map<string, EventFieldRow[]>()
  const businessFields: EventFieldRow[] = []
  PUBLIC_FIELD_SPECS.forEach((field) => publicFieldMap.set(field.fieldName, []))

  sortedFields.forEach((field, index) => {
    const normalizedField = {
      ...field,
      __key: field.__key || createFieldKey(),
      __systemField: false,
      fieldLabel: field.fieldLabel || '',
      defaultValue: field.defaultValue || '',
      sampleValue: field.sampleValue || '',
      description: field.description || '',
      sortNo: field.sortNo ?? index + 1
    }
    if (PUBLIC_FIELD_NAME_SET.has(normalizedField.fieldName)) {
      publicFieldMap.get(normalizedField.fieldName)?.push(normalizedField)
      return
    }
    businessFields.push(normalizedField)
  })

  const normalizedFields: EventFieldRow[] = []
  PUBLIC_FIELD_SPECS.forEach((spec) => {
    const matches = publicFieldMap.get(spec.fieldName) || []
    if (!matches.length) {
      normalizedFields.push(createSystemFieldRow(spec, sceneCode, eventCode))
      return
    }
    matches.forEach((field) => {
      normalizedFields.push(createSystemFieldRow(spec, sceneCode, eventCode, field))
    })
  })

  businessFields.forEach((field) => {
    normalizedFields.push({
      ...field,
      __systemField: false
    })
  })

  return normalizedFields.map((field, index) => ({
    ...field,
    sortNo: index + 1
  }))
}

const syncSystemFields = () => {
  formData.value.fields = ensurePublicFields(
    formData.value.fields,
    formData.value.sceneCode,
    formData.value.eventCode
  )
}

const open = async (type: 'create' | 'update', id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? t('action.create') : t('action.update')
  formType.value = type
  activeTab.value = 'basic'
  resetForm()
  await loadSceneOptions()
  if (id) {
    formLoading.value = true
    try {
      const data = await EventModelApi.getEventModel(id)
      formData.value = {
        id: data.id,
        sceneCode: data.sceneCode,
        eventCode: data.eventCode,
        eventName: data.eventName,
        version: data.version,
        status: data.status,
        description: data.description || '',
        fields: ensurePublicFields(
          (data.fields || []).map((field, index) => mapToFieldRow(field, index)),
          data.sceneCode,
          data.eventCode
        )
      }
    } finally {
      formLoading.value = false
    }
  }
}

defineExpose({ open })

const emit = defineEmits(['success'])

const validateSubmitSections = async () => {
  if (!formData.value.fields.length) {
    activeTab.value = 'fields'
    await nextTick()
    message.warning('请先在【字段定义】中至少新增一个字段')
    return false
  }
  return true
}

const submitForm = async () => {
  if (!formRef.value) {
    return
  }
  try {
    await formRef.value.validate()
  } catch {
    activeTab.value = 'basic'
    await nextTick()
    message.warning('请先完善【基础信息】中的必填项')
    return
  }
  const sectionsValid = await validateSubmitSections()
  if (!sectionsValid) {
    return
  }
  const payload = buildPayload()
  if (!payload) {
    return
  }
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await EventModelApi.createEventModel(payload)
      message.success(t('common.createSuccess'))
    } else {
      await EventModelApi.updateEventModel(payload)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const addField = () => {
  const row = createFieldRow()
  row.sortNo = formData.value.fields.length + 1
  formData.value.fields.push(row)
}

const removeField = (index: number) => {
  if (formData.value.fields[index]?.__systemField) {
    return
  }
  formData.value.fields.splice(index, 1)
  syncFieldSortNo()
}

const canMoveFieldUp = (index: number) => {
  return !formData.value.fields[index]?.__systemField && index > PUBLIC_FIELD_SPECS.length
}

const moveFieldUp = (index: number) => {
  if (!canMoveFieldUp(index)) {
    return
  }
  const current = formData.value.fields[index]
  formData.value.fields.splice(index, 1)
  formData.value.fields.splice(index - 1, 0, current)
  syncFieldSortNo()
}

const moveFieldDown = (index: number) => {
  if (formData.value.fields[index]?.__systemField || index >= formData.value.fields.length - 1) {
    return
  }
  const current = formData.value.fields[index]
  formData.value.fields.splice(index, 1)
  formData.value.fields.splice(index + 1, 0, current)
  syncFieldSortNo()
}

const syncFieldSortNo = () => {
  formData.value.fields.forEach((field, index) => {
    field.sortNo = index + 1
  })
}

const buildPayload = (): EventModelApi.EventModelSaveReqVO | null => {
  if (!formData.value.fields.length) {
    message.warning('请先在【字段定义】中至少新增一个字段')
    activeTab.value = 'fields'
    return null
  }

  const hasDisabledExtField = formData.value.fields.some(
    (field) => field.fieldName?.trim().toLowerCase() === 'ext'
  )
  if (hasDisabledExtField) {
    message.warning('字段名 ext 已停用，请改为具体业务字段名')
    activeTab.value = 'fields'
    return null
  }

  const normalizedFields = ensurePublicFields(
    formData.value.fields,
    formData.value.sceneCode,
    formData.value.eventCode
  )
  formData.value.fields = normalizedFields

  return {
    id: formData.value.id,
    sceneCode: formData.value.sceneCode,
    eventCode: formData.value.eventCode,
    eventName: formData.value.eventName,
    status: formData.value.status,
    description: formData.value.description || undefined,
    fields: normalizedFields.map((field: EventFieldRow) => {
      const { __key: _key, __systemField: _systemField, ...fieldData } = field
      return {
        ...fieldData,
        defaultValue: fieldData.defaultValue || undefined,
        sampleValue: fieldData.sampleValue || undefined,
        description: fieldData.description || undefined,
        fieldLabel: fieldData.fieldLabel || undefined
      }
    })
  }
}

const resetForm = () => {
  formData.value = createDefaultFormData()
  formRef.value?.resetFields()
}

watch(
  () => [formData.value.sceneCode, formData.value.eventCode],
  () => {
    syncSystemFields()
  }
)
</script>

<style scoped lang="scss">
.risk-event-model-form {
  padding-right: 12px;
}

.risk-event-model-form__footer {
  display: flex;
  justify-content: flex-end;
  width: 100%;
}

.risk-event-model-form__field-table {
  min-width: 1410px;

  :deep(.el-input),
  :deep(.el-select),
  :deep(.el-input-number) {
    width: 100%;
  }

  :deep(.el-table__cell .cell) {
    padding-left: 8px;
    padding-right: 8px;
  }
}

.risk-event-model-form__row-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.risk-event-model-form__row-action-btn {
  margin-left: 0 !important;
  padding: 0;
}

.risk-event-model-form__sort-input {
  :deep(.el-input__inner) {
    text-align: center;
  }
}
</style>
