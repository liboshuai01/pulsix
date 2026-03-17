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
                    @change="handleSceneChange"
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
              <el-col :span="12">
                <el-form-item label="事件类型" prop="eventType">
                  <el-input
                    v-model="formData.eventType"
                    placeholder="请输入事件类型"
                    :disabled="formType === 'update'"
                  />
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
          </el-tab-pane>

          <el-tab-pane label="接入绑定" name="binding">
            <div class="mb-12px flex items-center justify-between gap-12px">
              <div class="text-13px text-[var(--el-text-color-secondary)]">
                事件模型负责维护接入绑定。请先选择场景，再从当前场景允许的接入源中至少选择一个。
              </div>
              <el-tag type="primary" effect="plain">
                已选 {{ formData.bindingSourceCodes.length }} 个接入源
              </el-tag>
            </div>
            <el-form-item
              label-width="0"
              prop="bindingSourceCodes"
              class="risk-event-model-form__binding-form-item"
            >
              <el-alert
                v-if="!formData.sceneCode"
                title="请先在基础信息中选择场景编码，再维护接入绑定。"
                type="info"
                :closable="false"
              />
              <div v-else v-loading="bindingLoading" class="risk-event-model-form__binding-panel">
                <el-table
                  v-if="bindingSourceOptions.length"
                  ref="bindingTableRef"
                  :data="bindingSourceOptions"
                  row-key="sourceCode"
                  border
                  class="risk-event-model-form__binding-table"
                  @selection-change="handleBindingSelectionChange"
                >
                  <el-table-column type="selection" width="55" reserve-selection />
                  <el-table-column label="接入源名称" prop="sourceName" min-width="180" />
                  <el-table-column label="接入源编码" prop="sourceCode" min-width="180" />
                  <el-table-column label="接入类型" min-width="120">
                    <template #default="{ row }">
                      <dict-tag :type="DICT_TYPE.RISK_ACCESS_SOURCE_TYPE" :value="row.sourceType" />
                    </template>
                  </el-table-column>
                  <el-table-column label="标准 Topic" min-width="180">
                    <template #default="{ row }">
                      <dict-tag :type="DICT_TYPE.RISK_ACCESS_TOPIC_NAME" :value="row.topicName" />
                    </template>
                  </el-table-column>
                  <el-table-column label="状态" width="100" align="center">
                    <template #default="{ row }">
                      <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="row.status" />
                    </template>
                  </el-table-column>
                </el-table>
                <el-empty
                  v-else-if="!bindingLoading"
                  description="当前场景下暂无可绑定的接入源"
                  :image-size="72"
                />
              </div>
            </el-form-item>
            <div class="text-13px text-[var(--el-text-color-secondary)]">
              只在《事件模型》页维护事件与接入源的关系；已停用但仍被当前事件绑定的接入源，也会保留展示。
            </div>
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
                    controls-position="right"
                    class="risk-event-model-form__sort-input"
                  />
                </template>
              </el-table-column>
              <el-table-column label="字段名" width="170">
                <template #default="{ row }">
                  <el-input v-model="row.fieldName" placeholder="fieldName" />
                </template>
              </el-table-column>
              <el-table-column label="显示名" width="170">
                <template #default="{ row }">
                  <el-input v-model="row.fieldLabel" placeholder="字段显示名" />
                </template>
              </el-table-column>
              <el-table-column label="字段类型" width="150">
                <template #default="{ row }">
                  <el-select v-model="row.fieldType" placeholder="请选择字段类型">
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
                  <el-switch v-model="row.requiredFlag" :active-value="1" :inactive-value="0" />
                </template>
              </el-table-column>
              <el-table-column label="默认值" width="170">
                <template #default="{ row }">
                  <el-input v-model="row.defaultValue" placeholder="默认值" />
                </template>
              </el-table-column>
              <el-table-column label="样例值" width="190">
                <template #default="{ row }">
                  <el-input v-model="row.sampleValue" placeholder="样例值" />
                </template>
              </el-table-column>
              <el-table-column label="描述" width="210">
                <template #default="{ row }">
                  <el-input v-model="row.description" placeholder="字段描述" />
                </template>
              </el-table-column>
              <el-table-column
                label="操作"
                width="136"
                fixed="right"
                align="center"
                header-align="center"
              >
                <template #default="{ $index }">
                  <div class="risk-event-model-form__row-actions">
                    <el-button
                      link
                      type="primary"
                      class="risk-event-model-form__row-action-btn"
                      @click="moveFieldUp($index)"
                    >
                      上移
                    </el-button>
                    <el-button
                      link
                      type="primary"
                      class="risk-event-model-form__row-action-btn"
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
import * as AccessSourceApi from '@/api/risk/access-source'
import RiskCenterDialog from '../../components/RiskCenterDialog.vue'
import { EVENT_FIELD_TYPE_OPTIONS } from './constants'
import type { FormRules } from 'element-plus'

defineOptions({ name: 'RiskEventModelForm' })

type EventFieldRow = EventModelApi.EventFieldItemVO & { __key: string }
type EventModelFormData = EventModelApi.EventModelSaveReqVO & {
  fields: EventFieldRow[]
  version?: number
}

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const bindingLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const activeTab = ref('basic')
const formRef = ref()
const bindingTableRef = ref()
const sceneOptions = ref<SceneApi.SceneVO[]>([])
const bindingSourceOptions = ref<AccessSourceApi.AccessSourceSimpleVO[]>([])
const currentBoundSourceFallback = ref<AccessSourceApi.AccessSourceSimpleVO[]>([])

const createFieldRow = (): EventFieldRow => ({
  __key: `${Date.now()}-${Math.random()}`,
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
  eventType: '',
  bindingSourceCodes: [],
  version: undefined,
  status: CommonStatusEnum.DISABLE,
  description: '',
  fields: []
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
  eventName: [{ required: true, message: '事件名称不能为空', trigger: 'blur' }],
  eventType: [{ required: true, message: '事件类型不能为空', trigger: 'blur' }]
})

const loadSceneOptions = async () => {
  sceneOptions.value = await SceneApi.getSimpleSceneList()
}

const toSimpleAccessSource = (
  source: EventModelApi.EventBindingSourceItemVO
): AccessSourceApi.AccessSourceSimpleVO => ({
  sourceCode: source.sourceCode,
  sourceName: source.sourceName,
  sourceType: source.sourceType,
  topicName: source.topicName,
  status: source.status
})

const mergeBindingSourceOptions = (
  preferred: AccessSourceApi.AccessSourceSimpleVO[],
  fallback: AccessSourceApi.AccessSourceSimpleVO[]
) => {
  const mergedMap = new Map<string, AccessSourceApi.AccessSourceSimpleVO>()
  preferred.forEach((item) => mergedMap.set(item.sourceCode, item))
  fallback.forEach((item) => {
    if (!mergedMap.has(item.sourceCode)) {
      mergedMap.set(item.sourceCode, item)
    }
  })
  return Array.from(mergedMap.values())
}

const syncBindingSelection = async () => {
  await nextTick()
  if (!bindingTableRef.value) {
    return
  }
  bindingTableRef.value.clearSelection()
  const selectedSourceCodeSet = new Set(formData.value.bindingSourceCodes)
  bindingSourceOptions.value.forEach((item) => {
    if (selectedSourceCodeSet.has(item.sourceCode)) {
      bindingTableRef.value.toggleRowSelection(item, true)
    }
  })
}

const loadBindingSourceOptions = async (
  sceneCode: string,
  fallbackSources: AccessSourceApi.AccessSourceSimpleVO[] = currentBoundSourceFallback.value
) => {
  if (!sceneCode) {
    bindingSourceOptions.value = []
    formData.value.bindingSourceCodes = []
    await syncBindingSelection()
    return
  }

  bindingLoading.value = true
  try {
    const enabledSources = await AccessSourceApi.getSimpleAccessSourceList(sceneCode)
    bindingSourceOptions.value = mergeBindingSourceOptions(enabledSources, fallbackSources)
    const availableSourceCodes = new Set(
      bindingSourceOptions.value.map((item) => item.sourceCode)
    )
    formData.value.bindingSourceCodes = formData.value.bindingSourceCodes.filter((sourceCode) =>
      availableSourceCodes.has(sourceCode)
    )
    await syncBindingSelection()
  } finally {
    bindingLoading.value = false
  }
}

const handleSceneChange = async (sceneCode: string) => {
  currentBoundSourceFallback.value = []
  formData.value.bindingSourceCodes = []
  await loadBindingSourceOptions(sceneCode, [])
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
      currentBoundSourceFallback.value = (data.bindingSources || []).map(toSimpleAccessSource)
      formData.value = {
        id: data.id,
        sceneCode: data.sceneCode,
        eventCode: data.eventCode,
        eventName: data.eventName,
        eventType: data.eventType,
        bindingSourceCodes: currentBoundSourceFallback.value.map((item) => item.sourceCode),
        version: data.version,
        status: data.status,
        description: data.description || '',
        fields: (data.fields || []).map((field, index) => ({
          ...field,
          sortNo: field.sortNo ?? index + 1,
          __key: `${Date.now()}-${index}-${Math.random()}`
        }))
      }
      await loadBindingSourceOptions(formData.value.sceneCode)
    } finally {
      formLoading.value = false
    }
  }
}

defineExpose({ open })

const emit = defineEmits(['success'])

const validateSubmitSections = async () => {
  if (!formData.value.bindingSourceCodes.length) {
    activeTab.value = 'binding'
    await nextTick()
    message.warning('请先在【接入绑定】中至少选择一个接入源')
    return false
  }
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
  formData.value.fields.splice(index, 1)
  syncFieldSortNo()
}

const moveFieldUp = (index: number) => {
  if (index === 0) {
    return
  }
  const current = formData.value.fields[index]
  formData.value.fields.splice(index, 1)
  formData.value.fields.splice(index - 1, 0, current)
  syncFieldSortNo()
}

const moveFieldDown = (index: number) => {
  if (index >= formData.value.fields.length - 1) {
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

const handleBindingSelectionChange = (selection: AccessSourceApi.AccessSourceSimpleVO[]) => {
  formData.value.bindingSourceCodes = selection.map((item) => item.sourceCode)
  formRef.value?.clearValidate?.('bindingSourceCodes')
}

const buildPayload = (): EventModelApi.EventModelSaveReqVO | null => {
  if (!formData.value.bindingSourceCodes.length) {
    message.warning('请先在【接入绑定】中至少选择一个接入源')
    activeTab.value = 'binding'
    return null
  }
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

  return {
    id: formData.value.id,
    sceneCode: formData.value.sceneCode,
    eventCode: formData.value.eventCode,
    eventName: formData.value.eventName,
    eventType: formData.value.eventType,
    bindingSourceCodes: Array.from(new Set(formData.value.bindingSourceCodes)),
    status: formData.value.status,
    description: formData.value.description || undefined,
    fields: formData.value.fields.map((field: EventFieldRow) => {
      const { __key: _key, ...fieldData } = field
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
  bindingSourceOptions.value = []
  currentBoundSourceFallback.value = []
  formRef.value?.resetFields()
  nextTick(() => {
    bindingTableRef.value?.clearSelection?.()
  })
}
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

.risk-event-model-form__binding-form-item {
  :deep(.el-form-item__content) {
    width: 100%;
  }
}

.risk-event-model-form__binding-table {
  width: 100%;
}

.risk-event-model-form__binding-panel {
  min-height: 120px;
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
