<template>
  <el-drawer v-model="drawerVisible" :title="drawerTitle" size="1100px" destroy-on-close>
    <div v-loading="formLoading" class="risk-event-model-form">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
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
              <el-col :span="12">
                <el-form-item label="事件类型" prop="eventType">
                  <el-input v-model="formData.eventType" placeholder="请输入事件类型" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="接入类型" prop="sourceType">
                  <el-input v-model="formData.sourceType" placeholder="例如 HTTP / SDK" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="标准 Topic" prop="topicName">
                  <el-input v-model="formData.topicName" placeholder="请输入标准事件 Topic" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="状态" prop="status">
                  <el-radio-group v-model="formData.status">
                    <el-radio
                      v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
                      :key="dict.value"
                      :value="dict.value"
                    >
                      {{ dict.label }}
                    </el-radio>
                  </el-radio-group>
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

          <el-tab-pane label="字段定义" name="fields">
            <div class="mb-12px flex items-center justify-between">
              <div class="text-13px text-[var(--el-text-color-secondary)]">
                维护字段顺序、类型、必填标记和字段级默认值/样例值。
              </div>
              <el-button type="primary" plain @click="addField">
                <Icon icon="ep:plus" class="mr-5px" />新增字段
              </el-button>
            </div>
            <el-table :data="formData.fields" border row-key="__key">
              <el-table-column label="排序" width="80" align="center">
                <template #default="{ row }">
                  <el-input-number v-model="row.sortNo" :min="1" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="字段名" min-width="150">
                <template #default="{ row }">
                  <el-input v-model="row.fieldName" placeholder="fieldName" />
                </template>
              </el-table-column>
              <el-table-column label="显示名" min-width="150">
                <template #default="{ row }">
                  <el-input v-model="row.fieldLabel" placeholder="字段显示名" />
                </template>
              </el-table-column>
              <el-table-column label="字段类型" min-width="140">
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
              <el-table-column label="默认值" min-width="150">
                <template #default="{ row }">
                  <el-input v-model="row.defaultValue" placeholder="默认值" />
                </template>
              </el-table-column>
              <el-table-column label="样例值" min-width="170">
                <template #default="{ row }">
                  <el-input v-model="row.sampleValue" placeholder="样例值" />
                </template>
              </el-table-column>
              <el-table-column label="描述" min-width="170">
                <template #default="{ row }">
                  <el-input v-model="row.description" placeholder="字段描述" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="210" fixed="right" align="center">
                <template #default="{ $index }">
                  <el-button link type="primary" @click="moveFieldUp($index)">上移</el-button>
                  <el-button link type="primary" @click="moveFieldDown($index)">下移</el-button>
                  <el-button link type="warning" @click="openExtJsonDialog($index)">高级</el-button>
                  <el-button link type="danger" @click="removeField($index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="样例报文" name="sample">
            <div class="mb-12px flex items-center gap-12px">
              <el-button @click="formatSampleJson">格式化</el-button>
              <el-button @click="validateSampleJsonText">校验 JSON</el-button>
              <span class="text-13px text-[var(--el-text-color-secondary)]">
                样例报文需为 JSON Object，顶层字段必须在字段定义中声明。
              </span>
            </div>
            <el-input
              v-model="sampleEventJsonText"
              type="textarea"
              :rows="20"
              placeholder="请输入样例报文 JSON"
            />
          </el-tab-pane>

          <el-tab-pane label="标准事件预览" name="preview">
            <div class="mb-12px flex items-center gap-12px">
              <el-button type="primary" plain :loading="previewLoading" @click="refreshPreview">
                <Icon icon="ep:refresh" class="mr-5px" />刷新预览
              </el-button>
              <span class="text-13px text-[var(--el-text-color-secondary)]">
                预览会按字段类型、默认值和固定字段推导规则组装标准事件。
              </span>
            </div>
            <el-row :gutter="16">
              <el-col :span="14">
                <el-card header="标准事件 JSON" shadow="never">
                  <pre class="risk-event-model-form__json">{{ previewJsonText }}</pre>
                </el-card>
              </el-col>
              <el-col :span="10">
                <el-card header="字段摘要" shadow="never">
                  <div>
                    <div class="mb-10px font-600">必填字段</div>
                    <el-tag
                      v-for="field in previewResult.requiredFields"
                      :key="field"
                      type="danger"
                      effect="plain"
                      class="mr-6px mb-6px"
                    >
                      {{ field }}
                    </el-tag>
                  </div>
                  <div class="mt-12px">
                    <div class="mb-10px font-600">可选字段</div>
                    <el-tag
                      v-for="field in previewResult.optionalFields"
                      :key="field"
                      effect="plain"
                      class="mr-6px mb-6px"
                    >
                      {{ field }}
                    </el-tag>
                  </div>
                </el-card>
                <el-card header="校验消息" shadow="never" class="mt-16px">
                  <el-empty
                    v-if="!previewResult.validationMessages.length"
                    description="当前草稿校验通过"
                    :image-size="60"
                  />
                  <el-alert
                    v-for="message in previewResult.validationMessages"
                    :key="message"
                    :title="message"
                    type="warning"
                    :closable="false"
                    class="mb-8px"
                  />
                </el-card>
              </el-col>
            </el-row>
          </el-tab-pane>
        </el-tabs>
      </el-form>
    </div>

    <template #footer>
      <div class="risk-event-model-form__footer">
        <el-button type="primary" :loading="formLoading" @click="submitForm">确 定</el-button>
        <el-button @click="drawerVisible = false">取 消</el-button>
      </div>
    </template>
  </el-drawer>

  <Dialog v-model="extJsonDialogVisible" title="字段高级配置" width="720px" max-height="420px" scroll>
    <el-input
      v-model="extJsonText"
      type="textarea"
      :rows="14"
      placeholder="请输入 extJson，留空表示清空高级配置"
    />
    <template #footer>
      <el-button type="primary" @click="saveExtJson">保 存</el-button>
      <el-button @click="extJsonDialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import * as SceneApi from '@/api/risk/scene'
import * as EventModelApi from '@/api/risk/event-model'
import { EVENT_FIELD_TYPE_OPTIONS, EVENT_MODEL_DEFAULT_TOPIC } from './constants'
import type { FormRules } from 'element-plus'

defineOptions({ name: 'RiskEventModelForm' })

type EventFieldRow = EventModelApi.EventFieldItemVO & { __key: string }
type EventModelFormData = Omit<EventModelApi.EventModelVO, 'fields'> & { fields: EventFieldRow[] }

const { t } = useI18n()
const message = useMessage()

const drawerVisible = ref(false)
const drawerTitle = ref('')
const formLoading = ref(false)
const previewLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const activeTab = ref('basic')
const formRef = ref()
const sceneOptions = ref<SceneApi.SceneVO[]>([])

const extJsonDialogVisible = ref(false)
const extJsonText = ref('')
const activeExtJsonRowIndex = ref<number | undefined>()

const createFieldRow = (): EventFieldRow => ({
  __key: `${Date.now()}-${Math.random()}`,
  fieldName: '',
  fieldLabel: '',
  fieldType: 'STRING',
  requiredFlag: 0,
  defaultValue: '',
  sampleValue: '',
  description: '',
  sortNo: 1,
  extJson: undefined
})

const createDefaultFormData = (): EventModelFormData => ({
  id: undefined,
  sceneCode: '',
  eventCode: '',
  eventName: '',
  eventType: '',
  sourceType: '',
  topicName: EVENT_MODEL_DEFAULT_TOPIC,
  sampleEventJson: {},
  version: undefined,
  status: CommonStatusEnum.ENABLE,
  description: '',
  fields: []
})

const formData = ref<EventModelFormData>(createDefaultFormData())
const sampleEventJsonText = ref('{}')
const previewResult = ref<EventModelApi.EventModelPreviewVO>({
  standardEventJson: {},
  requiredFields: [],
  optionalFields: [],
  fieldTypes: {},
  validationMessages: []
})

const previewJsonText = computed(() =>
  JSON.stringify(previewResult.value.standardEventJson ?? {}, null, 2)
)

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
  eventType: [{ required: true, message: '事件类型不能为空', trigger: 'blur' }],
  topicName: [{ required: true, message: '标准 Topic 不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const loadSceneOptions = async () => {
  sceneOptions.value = await SceneApi.getSimpleSceneList()
}

const open = async (type: 'create' | 'update', id?: number) => {
  drawerVisible.value = true
  drawerTitle.value = type === 'create' ? t('action.create') : t('action.update')
  formType.value = type
  activeTab.value = 'basic'
  previewResult.value = {
    standardEventJson: {},
    requiredFields: [],
    optionalFields: [],
    fieldTypes: {},
    validationMessages: []
  }
  resetForm()
  await loadSceneOptions()
  if (id) {
    formLoading.value = true
    try {
      const data = await EventModelApi.getEventModel(id)
      formData.value = {
        ...data,
        topicName: data.topicName || EVENT_MODEL_DEFAULT_TOPIC,
        fields: (data.fields || []).map((field, index) => ({
          ...field,
          sortNo: field.sortNo ?? index + 1,
          __key: `${Date.now()}-${index}-${Math.random()}`
        }))
      }
      sampleEventJsonText.value = JSON.stringify(data.sampleEventJson ?? {}, null, 2)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])

const submitForm = async () => {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate()
  if (!valid) {
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
    drawerVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const handleTabChange = async (name: string | number) => {
  if (name === 'preview') {
    await refreshPreview()
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

const openExtJsonDialog = (index: number) => {
  activeExtJsonRowIndex.value = index
  const row = formData.value.fields[index]
  extJsonText.value = row.extJson ? JSON.stringify(row.extJson, null, 2) : ''
  extJsonDialogVisible.value = true
}

const saveExtJson = () => {
  if (activeExtJsonRowIndex.value === undefined) {
    return
  }
  if (!extJsonText.value.trim()) {
    formData.value.fields[activeExtJsonRowIndex.value].extJson = undefined
    extJsonDialogVisible.value = false
    return
  }
  try {
    const parsed = JSON.parse(extJsonText.value)
    if (Array.isArray(parsed) || parsed === null || typeof parsed !== 'object') {
      message.error('extJson 必须是 JSON Object')
      return
    }
    formData.value.fields[activeExtJsonRowIndex.value].extJson = parsed
    extJsonDialogVisible.value = false
  } catch (error: any) {
    message.error(`extJson 格式不正确：${error.message}`)
  }
}

const formatSampleJson = () => {
  try {
    sampleEventJsonText.value = JSON.stringify(parseSampleJsonText(), null, 2)
  } catch (error: any) {
    message.error(error.message)
  }
}

const validateSampleJsonText = () => {
  try {
    parseSampleJsonText()
    message.success('样例报文格式正确')
  } catch (error: any) {
    message.error(error.message)
  }
}

const parseSampleJsonText = () => {
  try {
    const parsed = JSON.parse(sampleEventJsonText.value || '{}')
    if (Array.isArray(parsed) || parsed === null || typeof parsed !== 'object') {
      throw new Error('样例报文必须是 JSON Object')
    }
    return parsed as Record<string, any>
  } catch (error: any) {
    throw new Error(`样例报文格式不正确：${error.message}`)
  }
}

const buildPayload = () => {
  let sampleEventJson: Record<string, any>
  try {
    sampleEventJson = parseSampleJsonText()
  } catch (error: any) {
    message.error(error.message)
    activeTab.value = 'sample'
    return null
  }

  return {
    ...formData.value,
    topicName: formData.value.topicName || EVENT_MODEL_DEFAULT_TOPIC,
    sampleEventJson,
    fields: formData.value.fields.map((field: EventFieldRow) => {
      const { __key: _key, ...fieldData } = field
      return {
        ...fieldData,
        defaultValue: fieldData.defaultValue || undefined,
        sampleValue: fieldData.sampleValue || undefined,
        description: fieldData.description || undefined,
        fieldLabel: fieldData.fieldLabel || undefined,
        extJson:
          fieldData.extJson && Object.keys(fieldData.extJson).length ? fieldData.extJson : undefined
      }
    })
  } as EventModelApi.EventModelVO
}

const refreshPreview = async () => {
  const payload = buildPayload()
  if (!payload) {
    return
  }
  previewLoading.value = true
  try {
    previewResult.value = await EventModelApi.previewStandardEvent(payload)
  } finally {
    previewLoading.value = false
  }
}

const resetForm = () => {
  formData.value = createDefaultFormData()
  sampleEventJsonText.value = '{}'
  extJsonText.value = ''
  activeExtJsonRowIndex.value = undefined
  formRef.value?.resetFields()
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

.risk-event-model-form__json {
  margin: 0;
  min-height: 280px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.6;
}
</style>
