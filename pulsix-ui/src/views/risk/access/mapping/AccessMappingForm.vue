<template>
  <RiskCenterDialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="min(1560px, calc(100vw - 48px))"
    max-height="calc(100vh - 180px)"
    scroll
  >
    <div v-loading="formLoading" class="risk-access-mapping-form">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基础信息" name="basic">
            <el-row :gutter="18">
              <el-col :xs="24" :md="8">
                <el-form-item label="场景编码" prop="sceneCode">
                  <el-select
                    v-model="formData.sceneCode"
                    placeholder="请选择场景"
                    filterable
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
              <el-col :xs="24" :md="8">
                <el-form-item label="事件模型" prop="eventCode">
                  <el-select
                    v-model="formData.eventCode"
                    placeholder="请选择事件模型"
                    filterable
                    clearable
                    :disabled="!formData.sceneCode"
                    @change="handleEventChange"
                  >
                    <el-option
                      v-for="event in eventOptions"
                      :key="event.eventCode"
                      :label="`${event.eventName} (${event.eventCode})`"
                      :value="event.eventCode"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="接入源" prop="sourceCode">
                  <el-select
                    v-model="formData.sourceCode"
                    placeholder="请选择接入源"
                    filterable
                    clearable
                    :disabled="!formData.sceneCode"
                    @change="handleSourceChange"
                  >
                    <el-option
                      v-for="source in sourceOptions"
                      :key="source.sourceCode"
                      :label="`${source.sourceName} (${source.sourceCode})`"
                      :value="source.sourceCode"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="18">
              <el-col :xs="24" :md="8">
                <el-form-item label="接入类型">
                  <el-input :model-value="sourceTypeLabel" disabled />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="标准 Topic">
                  <el-input :model-value="formData.topicName || '-'" disabled />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="描述" prop="description">
              <el-input
                v-model="formData.description"
                type="textarea"
                :rows="4"
                placeholder="请输入接入映射说明"
              />
            </el-form-item>

            <el-alert
              title="上游请求仍需显式携带标准 eventCode，ingest 将按 sourceCode + eventCode 命中唯一接入映射。"
              type="info"
              :closable="false"
            />
          </el-tab-pane>

          <el-tab-pane label="原始结构" name="raw">
            <el-row :gutter="16">
              <el-col :xs="24" :lg="12">
                <div class="risk-access-mapping-form__section-title">原始样例报文</div>
                <el-input
                  v-model="formData.rawSampleJsonText"
                  type="textarea"
                  :rows="12"
                  placeholder='请输入 JSON，例如 {"order":{"id":"O1001"}}'
                  class="risk-access-mapping-form__json-input"
                />
              </el-col>
              <el-col :xs="24" :lg="12">
                <div class="risk-access-mapping-form__section-title">样例请求头</div>
                <el-input
                  v-model="formData.sampleHeadersJsonText"
                  type="textarea"
                  :rows="12"
                  placeholder='可选，请输入 JSON，例如 {"x-source":"mall"}'
                  class="risk-access-mapping-form__json-input"
                />
              </el-col>
            </el-row>

            <div class="mt-16px mb-12px flex items-center justify-between">
              <div class="text-13px text-[var(--el-text-color-secondary)]">
                原始字段路径仅支持简化点路径，例如 `order.user.id`、`items[0].skuId`。
              </div>
              <el-button type="primary" plain @click="addRawField">
                <Icon icon="ep:plus" class="mr-5px" />新增原始字段
              </el-button>
            </div>

            <el-table
              :data="formData.rawFields"
              :fit="false"
              border
              row-key="__key"
              scrollbar-always-on
              class="risk-access-mapping-form__raw-table"
            >
              <el-table-column label="排序" width="86" align="center">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.sortNo"
                    :min="1"
                    controls-position="right"
                    class="risk-access-mapping-form__sort-input"
                  />
                </template>
              </el-table-column>
              <el-table-column label="字段名" width="140">
                <template #default="{ row }">
                  <el-input v-model="row.fieldName" placeholder="fieldName" />
                </template>
              </el-table-column>
              <el-table-column label="显示名" width="150">
                <template #default="{ row }">
                  <el-input v-model="row.fieldLabel" placeholder="字段显示名" />
                </template>
              </el-table-column>
              <el-table-column label="字段路径" width="200">
                <template #default="{ row }">
                  <el-input v-model="row.fieldPath" placeholder="order.user.id" />
                </template>
              </el-table-column>
              <el-table-column label="字段类型" width="130">
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
              <el-table-column label="必填" width="86" align="center">
                <template #default="{ row }">
                  <el-switch v-model="row.requiredFlag" :active-value="1" :inactive-value="0" />
                </template>
              </el-table-column>
              <el-table-column label="样例值" width="180">
                <template #default="{ row }">
                  <el-input v-model="row.sampleValue" placeholder="样例值" />
                </template>
              </el-table-column>
              <el-table-column label="描述" width="190">
                <template #default="{ row }">
                  <el-input v-model="row.description" placeholder="字段描述" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="136" fixed="right" align="center">
                <template #default="{ $index }">
                  <div class="risk-access-mapping-form__row-actions">
                    <el-button link type="primary" @click="moveRawFieldUp($index)">上移</el-button>
                    <el-button link type="primary" @click="moveRawFieldDown($index)">下移</el-button>
                    <el-button link type="danger" @click="removeRawField($index)">删除</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="标准化规则" name="rules">
            <div class="mb-12px flex items-center justify-between gap-12px">
              <div class="text-13px text-[var(--el-text-color-secondary)]">
                目标字段固定来自当前事件模型的标准字段列表。脚本映射统一使用 AVIATOR，
                可访问 `rawPayload / headers / sourceCode / sceneCode / eventCode`。
              </div>
              <el-button type="primary" plain @click="openPreview">
                <Icon icon="ep:view" class="mr-5px" />预览标准事件
              </el-button>
            </div>

            <el-alert
              v-if="!formData.ruleRows.length"
              title="请先在基础信息中选择事件模型，加载标准字段后再配置映射规则。"
              type="info"
              :closable="false"
              class="mb-12px"
            />

            <el-table
              :data="formData.ruleRows"
              :fit="false"
              border
              row-key="targetFieldName"
              scrollbar-always-on
              class="risk-access-mapping-form__rule-table"
            >
              <el-table-column label="目标字段" width="220">
                <template #default="{ row }">
                  <div class="font-600">{{ row.targetFieldName }}</div>
                  <div class="text-12px text-[var(--el-text-color-secondary)]">
                    {{ row.targetFieldLabel || '-' }}
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="字段类型" width="120">
                <template #default="{ row }">
                  {{ getEventFieldTypeLabel(row.targetFieldType) }}
                </template>
              </el-table-column>
              <el-table-column label="必填" width="86" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.targetRequiredFlag === 1 ? 'danger' : 'info'" effect="plain">
                    {{ row.targetRequiredFlag === 1 ? '是' : '否' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="映射方式" width="150">
                <template #default="{ row }">
                  <el-select
                    v-model="row.mappingType"
                    placeholder="未配置"
                    clearable
                    @change="handleMappingTypeChange(row)"
                  >
                    <el-option
                      v-for="option in ACCESS_MAPPING_TYPE_OPTIONS"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="规则配置" width="360">
                <template #default="{ row }">
                  <div v-if="row.mappingType === 'SOURCE_FIELD'">
                    <el-input v-model="row.sourceFieldPath" placeholder="请输入源字段路径" />
                  </div>
                  <div v-else-if="row.mappingType === 'CONSTANT'">
                    <el-input v-model="row.constantValue" placeholder="请输入常量值" />
                  </div>
                  <div v-else-if="row.mappingType === 'SCRIPT'" class="risk-access-mapping-form__script-box">
                    <el-select v-model="row.scriptEngine" placeholder="请选择脚本引擎">
                      <el-option
                        v-for="option in ACCESS_SCRIPT_ENGINE_OPTIONS"
                        :key="option.value"
                        :label="option.label"
                        :value="option.value"
                        :disabled="option.disabled"
                      />
                    </el-select>
                    <el-input
                      v-model="row.scriptContent"
                      type="textarea"
                      :rows="3"
                      placeholder="请输入表达式，例如 rawPayload['order']['amount']"
                    />
                  </div>
                  <span v-else class="text-[var(--el-text-color-placeholder)]">未配置</span>
                </template>
              </el-table-column>
              <el-table-column label="时间格式" width="180">
                <template #default="{ row }">
                  <el-input v-model="row.timePattern" placeholder="DATETIME 可选" />
                </template>
              </el-table-column>
              <el-table-column label="枚举映射" width="220">
                <template #default="{ row }">
                  <el-input
                    v-model="row.enumMappingText"
                    type="textarea"
                    :rows="3"
                    placeholder='可选 JSON，例如 {"1":"PAID"}'
                  />
                </template>
              </el-table-column>
              <el-table-column label="描述" width="180">
                <template #default="{ row }">
                  <el-input v-model="row.description" placeholder="映射规则描述" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="86" fixed="right" align="center">
                <template #default="{ row }">
                  <el-button link type="danger" @click="clearRuleRow(row)">清空</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-form>
    </div>

    <template #footer>
      <div class="risk-access-mapping-form__footer">
        <el-button type="primary" :loading="formLoading" @click="submitForm">确 定</el-button>
        <el-button @click="dialogVisible = false">取 消</el-button>
      </div>
    </template>
  </RiskCenterDialog>

  <AccessMappingPreviewDialog ref="previewDialogRef" />
</template>

<script setup lang="ts">
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import type { FormRules } from 'element-plus'
import * as AccessMappingApi from '@/api/risk/access-mapping'
import * as AccessSourceApi from '@/api/risk/access-source'
import * as EventModelApi from '@/api/risk/event-model'
import * as SceneApi from '@/api/risk/scene'
import RiskCenterDialog from '../../components/RiskCenterDialog.vue'
import AccessMappingPreviewDialog from './AccessMappingPreviewDialog.vue'
import {
  ACCESS_MAPPING_TYPE_OPTIONS,
  ACCESS_SCRIPT_ENGINE_OPTIONS,
  EVENT_FIELD_TYPE_OPTIONS,
  getEventFieldTypeLabel
} from './constants'

defineOptions({ name: 'RiskAccessMappingForm' })

type RawFieldRow = Partial<AccessMappingApi.AccessRawFieldItemVO> & {
  __key: string
  fieldType: string
  requiredFlag: number
  sortNo: number
}

type RuleRow = {
  targetFieldName: string
  targetFieldLabel?: string
  targetFieldType: string
  targetRequiredFlag: number
  mappingType?: string
  sourceFieldPath?: string
  constantValue?: string
  scriptEngine?: string
  scriptContent?: string
  timePattern?: string
  enumMappingText: string
  description?: string
}

type AccessMappingFormData = {
  id?: number
  sceneCode: string
  eventCode: string
  eventName?: string
  sourceCode: string
  sourceName?: string
  sourceType?: string
  topicName?: string
  description?: string
  rawSampleJsonText: string
  sampleHeadersJsonText: string
  rawFields: RawFieldRow[]
  ruleRows: RuleRow[]
}

type EventOption = EventModelApi.EventModelSimpleVO
type SourceOption = AccessSourceApi.AccessSourceSimpleVO

const DEFAULT_SCRIPT_ENGINE = 'AVIATOR'

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const activeTab = ref<'basic' | 'raw' | 'rules'>('basic')
const formType = ref<'create' | 'update'>('create')
const formRef = ref()
const previewDialogRef = ref()

const sceneOptions = ref<SceneApi.SceneVO[]>([])
const eventOptions = ref<EventOption[]>([])
const sourceOptions = ref<SourceOption[]>([])
const eventFallbackOptions = ref<EventOption[]>([])
const sourceFallbackOptions = ref<SourceOption[]>([])

const createRawFieldRow = (): RawFieldRow => ({
  __key: `${Date.now()}-${Math.random()}`,
  fieldName: '',
  fieldLabel: '',
  fieldPath: '',
  fieldType: 'STRING',
  requiredFlag: 0,
  sampleValue: '',
  description: '',
  sortNo: 1
})

const createDefaultFormData = (): AccessMappingFormData => ({
  id: undefined,
  sceneCode: '',
  eventCode: '',
  eventName: '',
  sourceCode: '',
  sourceName: '',
  sourceType: '',
  topicName: '',
  description: '',
  rawSampleJsonText: '{}',
  sampleHeadersJsonText: '',
  rawFields: [],
  ruleRows: []
})

const formData = ref<AccessMappingFormData>(createDefaultFormData())

const formRules = reactive<FormRules>({
  sceneCode: [{ required: true, message: '场景编码不能为空', trigger: 'change' }],
  eventCode: [{ required: true, message: '请选择事件模型', trigger: 'change' }],
  sourceCode: [{ required: true, message: '请选择接入源', trigger: 'change' }]
})

const sourceTypeLabel = computed(
  () =>
    getStrDictOptions(DICT_TYPE.RISK_ACCESS_SOURCE_TYPE).find(
      (item) => item.value === formData.value.sourceType
    )?.label || formData.value.sourceType || '-'
)

const mergeByKey = <T extends Record<string, any>>(list: T[], fallback: T[], key: keyof T) => {
  const mergedMap = new Map<string, T>()
  list.forEach((item) => mergedMap.set(String(item[key]), item))
  fallback.forEach((item) => {
    const mapKey = String(item[key])
    if (!mergedMap.has(mapKey)) {
      mergedMap.set(mapKey, item)
    }
  })
  return Array.from(mergedMap.values())
}

const loadSceneOptions = async () => {
  sceneOptions.value = await SceneApi.getSimpleSceneList()
}

const loadSceneRelatedOptions = async (sceneCode: string) => {
  if (!sceneCode) {
    eventOptions.value = []
    sourceOptions.value = []
    return
  }
  const [enabledEvents, enabledSources] = await Promise.all([
    EventModelApi.getSimpleEventModelList(sceneCode),
    AccessSourceApi.getSimpleAccessSourceList(sceneCode)
  ])
  eventOptions.value = mergeByKey(enabledEvents, eventFallbackOptions.value, 'eventCode')
  sourceOptions.value = mergeByKey(enabledSources, sourceFallbackOptions.value, 'sourceCode')
}

const buildRuleRows = (
  fields: EventModelApi.EventFieldItemVO[],
  existingRules: AccessMappingApi.AccessMappingRuleItemVO[] = []
) => {
  const ruleMap = new Map(existingRules.map((item) => [item.targetFieldName, item]))
  return fields.map((field) => {
    const rule = ruleMap.get(field.fieldName)
    return {
      targetFieldName: field.fieldName,
      targetFieldLabel: field.fieldLabel,
      targetFieldType: field.fieldType,
      targetRequiredFlag: field.requiredFlag,
      mappingType: rule?.mappingType,
      sourceFieldPath: rule?.sourceFieldPath || '',
      constantValue: rule?.constantValue || '',
      scriptEngine: rule?.scriptEngine || DEFAULT_SCRIPT_ENGINE,
      scriptContent: rule?.scriptContent || '',
      timePattern: rule?.timePattern || '',
      enumMappingText: rule?.enumMappingJson ? JSON.stringify(rule.enumMappingJson, null, 2) : '',
      description: rule?.description || ''
    }
  })
}

const syncSourceMeta = (sourceCode?: string) => {
  const source = sourceOptions.value.find((item) => item.sourceCode === sourceCode)
  formData.value.sourceName = source?.sourceName || ''
  formData.value.sourceType = source?.sourceType || ''
  formData.value.topicName = source?.topicName || ''
}

const loadSelectedEventDetail = async (
  eventCode: string,
  existingRules: AccessMappingApi.AccessMappingRuleItemVO[] = []
) => {
  if (!eventCode) {
    formData.value.eventName = ''
    formData.value.ruleRows = []
    return
  }
  const detail = await EventModelApi.getEventModelByCode(eventCode)
  formData.value.eventName = detail?.eventName || ''
  formData.value.ruleRows = buildRuleRows(detail?.fields || [], existingRules)
}

const handleSceneChange = async (sceneCode: string) => {
  formData.value.sceneCode = sceneCode
  formData.value.eventCode = ''
  formData.value.eventName = ''
  formData.value.sourceCode = ''
  formData.value.sourceName = ''
  formData.value.sourceType = ''
  formData.value.topicName = ''
  formData.value.ruleRows = []
  await loadSceneRelatedOptions(sceneCode)
}

const handleEventChange = async (eventCode: string) => {
  await loadSelectedEventDetail(eventCode)
}

const handleSourceChange = (sourceCode: string) => {
  syncSourceMeta(sourceCode)
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
      const detail = await AccessMappingApi.getAccessMapping(id)
      eventFallbackOptions.value = [
        {
          sceneCode: detail.sceneCode,
          eventCode: detail.eventCode,
          eventName: detail.eventName || detail.eventCode
        }
      ]
      sourceFallbackOptions.value = [
        {
          sourceCode: detail.sourceCode,
          sourceName: detail.sourceName || detail.sourceCode,
          sourceType: detail.sourceType || '',
          topicName: detail.topicName || '',
          status: CommonStatusEnum.DISABLE
        }
      ]
      formData.value = {
        id: detail.id,
        sceneCode: detail.sceneCode,
        eventCode: detail.eventCode,
        eventName: detail.eventName || '',
        sourceCode: detail.sourceCode,
        sourceName: detail.sourceName || '',
        sourceType: detail.sourceType || '',
        topicName: detail.topicName || '',
        description: detail.description || '',
        rawSampleJsonText: JSON.stringify(detail.rawSampleJson || {}, null, 2),
        sampleHeadersJsonText: detail.sampleHeadersJson
          ? JSON.stringify(detail.sampleHeadersJson, null, 2)
          : '',
        rawFields: (detail.rawFields || []).map((item, index) => ({
          ...item,
          __key: `${Date.now()}-${index}-${Math.random()}`,
          fieldType: item.fieldType || 'STRING',
          requiredFlag: item.requiredFlag ?? 0,
          sortNo: item.sortNo ?? index + 1
        })),
        ruleRows: []
      }
      await loadSceneRelatedOptions(detail.sceneCode)
      syncSourceMeta(detail.sourceCode)
      await loadSelectedEventDetail(detail.eventCode, detail.mappingRules || [])
    } finally {
      formLoading.value = false
    }
  } else {
    await loadSceneRelatedOptions(formData.value.sceneCode)
  }
}

defineExpose({ open })

const emit = defineEmits(['success'])

const addRawField = () => {
  const row = createRawFieldRow()
  row.sortNo = formData.value.rawFields.length + 1
  formData.value.rawFields.push(row)
}

const syncRawFieldSortNo = () => {
  formData.value.rawFields.forEach((item, index) => {
    item.sortNo = index + 1
  })
}

const removeRawField = (index: number) => {
  formData.value.rawFields.splice(index, 1)
  syncRawFieldSortNo()
}

const moveRawFieldUp = (index: number) => {
  if (index === 0) {
    return
  }
  const current = formData.value.rawFields[index]
  formData.value.rawFields.splice(index, 1)
  formData.value.rawFields.splice(index - 1, 0, current)
  syncRawFieldSortNo()
}

const moveRawFieldDown = (index: number) => {
  if (index >= formData.value.rawFields.length - 1) {
    return
  }
  const current = formData.value.rawFields[index]
  formData.value.rawFields.splice(index, 1)
  formData.value.rawFields.splice(index + 1, 0, current)
  syncRawFieldSortNo()
}

const handleMappingTypeChange = (row: RuleRow) => {
  if (row.mappingType === 'SCRIPT' && !row.scriptEngine) {
    row.scriptEngine = DEFAULT_SCRIPT_ENGINE
  }
  if (!row.mappingType) {
    clearRuleRow(row)
  }
}

const clearRuleRow = (row: RuleRow) => {
  row.mappingType = undefined
  row.sourceFieldPath = ''
  row.constantValue = ''
  row.scriptEngine = DEFAULT_SCRIPT_ENGINE
  row.scriptContent = ''
  row.timePattern = ''
  row.enumMappingText = ''
  row.description = ''
}

const parseJsonObjectText = (text: string, label: string, required: boolean) => {
  const trimmed = text.trim()
  if (!trimmed) {
    if (required) {
      activeTab.value = 'raw'
      message.warning(`${label}不能为空`)
      return null
    }
    return {}
  }
  try {
    const parsed = JSON.parse(trimmed)
    if (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object') {
      throw new Error('必须是 JSON 对象')
    }
    return parsed as Record<string, any>
  } catch (error) {
    activeTab.value = 'raw'
    message.warning(`${label}不是合法的 JSON 对象`)
    return null
  }
}

const parseOptionalJsonObjectText = (text: string, label: string) => {
  const trimmed = text.trim()
  if (!trimmed) {
    return undefined
  }
  try {
    const parsed = JSON.parse(trimmed)
    if (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object') {
      throw new Error('必须是 JSON 对象')
    }
    return parsed as Record<string, any>
  } catch (error) {
    activeTab.value = 'raw'
    message.warning(`${label}不是合法的 JSON 对象`)
    return null
  }
}

const buildRawFields = (): AccessMappingApi.AccessRawFieldItemVO[] | null => {
  if (!formData.value.rawFields.length) {
    activeTab.value = 'raw'
    message.warning('请至少维护一个原始字段')
    return null
  }

  const fieldPathSet = new Set<string>()
  const result: AccessMappingApi.AccessRawFieldItemVO[] = []
  for (const [index, row] of formData.value.rawFields.entries()) {
    const fieldName = row.fieldName?.trim()
    const fieldPath = row.fieldPath?.trim()
    if (!fieldName || !fieldPath) {
      activeTab.value = 'raw'
      message.warning(`第 ${index + 1} 个原始字段的字段名和字段路径不能为空`)
      return null
    }
    if (fieldPathSet.has(fieldPath)) {
      activeTab.value = 'raw'
      message.warning(`原始字段路径重复：${fieldPath}`)
      return null
    }
    fieldPathSet.add(fieldPath)
    result.push({
      fieldName,
      fieldLabel: row.fieldLabel?.trim() || undefined,
      fieldPath,
      fieldType: row.fieldType,
      requiredFlag: row.requiredFlag ?? 0,
      sampleValue: row.sampleValue?.trim() || undefined,
      description: row.description?.trim() || undefined,
      sortNo: row.sortNo ?? index + 1
    })
  }
  return result
}

const parseEnumMappingText = (text: string, fieldName: string) => {
  const trimmed = text.trim()
  if (!trimmed) {
    return undefined
  }
  try {
    const parsed = JSON.parse(trimmed)
    if (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object') {
      throw new Error('必须是对象')
    }
    return Object.fromEntries(
      Object.entries(parsed as Record<string, unknown>).map(([key, value]) => [key, String(value)])
    )
  } catch (error) {
    activeTab.value = 'rules'
    message.warning(`字段【${fieldName}】的枚举映射必须是 JSON 对象`)
    return null
  }
}

const buildMappingRules = (): AccessMappingApi.AccessMappingRuleItemVO[] | null => {
  const configuredRows = formData.value.ruleRows.filter((row) => row.mappingType)
  if (!configuredRows.length) {
    activeTab.value = 'rules'
    message.warning('请至少配置一条标准化规则')
    return null
  }

  const result: AccessMappingApi.AccessMappingRuleItemVO[] = []
  for (const row of configuredRows) {
    if (row.mappingType === 'SOURCE_FIELD' && !row.sourceFieldPath?.trim()) {
      activeTab.value = 'rules'
      message.warning(`字段【${row.targetFieldName}】缺少源字段路径`)
      return null
    }
    if (row.mappingType === 'CONSTANT' && !row.constantValue?.trim()) {
      activeTab.value = 'rules'
      message.warning(`字段【${row.targetFieldName}】缺少常量值`)
      return null
    }
    if (row.mappingType === 'SCRIPT') {
      if (row.scriptEngine !== DEFAULT_SCRIPT_ENGINE) {
        activeTab.value = 'rules'
        message.warning('一期仅支持 AVIATOR 表达式映射')
        return null
      }
      if (!row.scriptContent?.trim()) {
        activeTab.value = 'rules'
        message.warning(`字段【${row.targetFieldName}】缺少表达式内容`)
        return null
      }
    }

    const enumMappingJson = parseEnumMappingText(row.enumMappingText, row.targetFieldName)
    if (enumMappingJson === null) {
      return null
    }

    result.push({
      targetFieldName: row.targetFieldName,
      mappingType: row.mappingType!,
      sourceFieldPath: row.sourceFieldPath?.trim() || undefined,
      constantValue: row.constantValue?.trim() || undefined,
      scriptEngine: row.mappingType === 'SCRIPT' ? row.scriptEngine || DEFAULT_SCRIPT_ENGINE : undefined,
      scriptContent: row.scriptContent?.trim() || undefined,
      timePattern: row.timePattern?.trim() || undefined,
      enumMappingJson,
      description: row.description?.trim() || undefined
    })
  }

  return result
}

const buildPayload = (): AccessMappingApi.AccessMappingSaveReqVO | null => {
  const rawSampleJson = parseJsonObjectText(formData.value.rawSampleJsonText, '原始样例报文', true)
  if (!rawSampleJson) {
    return null
  }
  const sampleHeadersJson = parseOptionalJsonObjectText(formData.value.sampleHeadersJsonText, '样例请求头')
  if (sampleHeadersJson === null) {
    return null
  }
  const rawFields = buildRawFields()
  if (!rawFields) {
    return null
  }
  const mappingRules = buildMappingRules()
  if (!mappingRules) {
    return null
  }
  return {
    id: formData.value.id,
    eventCode: formData.value.eventCode,
    sourceCode: formData.value.sourceCode,
    description: formData.value.description?.trim() || undefined,
    rawSampleJson,
    sampleHeadersJson,
    rawFields,
    mappingRules
  }
}

const validateBasicForm = async () => {
  if (!formRef.value) {
    return false
  }
  try {
    await formRef.value.validate()
    return true
  } catch {
    activeTab.value = 'basic'
    await nextTick()
    message.warning('请先完善【基础信息】中的必填项')
    return false
  }
}

const submitForm = async () => {
  const basicValid = await validateBasicForm()
  if (!basicValid) {
    return
  }
  const payload = buildPayload()
  if (!payload) {
    return
  }
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await AccessMappingApi.createAccessMapping(payload)
      message.success(t('common.createSuccess'))
    } else {
      await AccessMappingApi.updateAccessMapping(payload)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const openPreview = async () => {
  const basicValid = await validateBasicForm()
  if (!basicValid) {
    return
  }
  const payload = buildPayload()
  if (!payload) {
    return
  }
  previewDialogRef.value?.openWithPayload(payload, {
    sceneCode: formData.value.sceneCode,
    eventCode: formData.value.eventCode,
    eventName: formData.value.eventName,
    sourceCode: formData.value.sourceCode,
    sourceName: formData.value.sourceName
  })
}

const resetForm = () => {
  formData.value = createDefaultFormData()
  eventFallbackOptions.value = []
  sourceFallbackOptions.value = []
  eventOptions.value = []
  sourceOptions.value = []
  formRef.value?.resetFields()
}
</script>

<style scoped lang="scss">
.risk-access-mapping-form {
  padding-right: 12px;
}

.risk-access-mapping-form__footer {
  display: flex;
  justify-content: flex-end;
  width: 100%;
}

.risk-access-mapping-form__section-title {
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.risk-access-mapping-form__json-input {
  :deep(textarea) {
    font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
    font-size: 13px;
    line-height: 1.6;
  }
}

.risk-access-mapping-form__raw-table,
.risk-access-mapping-form__rule-table {
  :deep(.el-input),
  :deep(.el-select),
  :deep(.el-input-number),
  :deep(textarea) {
    width: 100%;
  }

  :deep(.el-table__cell .cell) {
    padding-left: 8px;
    padding-right: 8px;
  }
}

.risk-access-mapping-form__raw-table {
  min-width: 1360px;
}

.risk-access-mapping-form__rule-table {
  min-width: 1620px;
}

.risk-access-mapping-form__script-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.risk-access-mapping-form__row-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.risk-access-mapping-form__sort-input {
  :deep(.el-input__inner) {
    text-align: center;
  }
}
</style>
