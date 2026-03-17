<template>
  <RiskCenterDialog
    v-model="dialogVisible"
    :title="dialogTitle"
    :fullscreen="false"
    width="min(960px, calc(100vw - 48px))"
    max-height="calc(100vh - 220px)"
    scroll
  >
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      class="risk-access-source-form"
    >
      <el-row :gutter="18">
        <el-col :xs="24" :md="12">
          <el-form-item label="接入源名称" prop="sourceName">
            <el-input v-model="formData.sourceName" placeholder="请输入接入源名称" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="接入源编码" prop="sourceCode">
            <el-input
              v-model="formData.sourceCode"
              placeholder="请输入接入源编码"
              :disabled="formType === 'update'"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="18">
        <el-col :xs="24" :md="12">
          <el-form-item label="接入类型" prop="sourceType">
            <el-select v-model="formData.sourceType" placeholder="请选择接入类型">
              <el-option
                v-for="dict in sourceTypeOptions"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="标准 Topic" prop="topicName">
            <el-select v-model="formData.topicName" placeholder="请选择标准 Topic">
              <el-option
                v-for="dict in topicNameOptions"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="允许场景" prop="allowedSceneCodes">
        <el-select
          v-model="formData.allowedSceneCodes"
          placeholder="请选择允许场景"
          filterable
          multiple
          collapse-tags
          collapse-tags-tooltip
        >
          <el-option
            v-for="scene in sceneOptions"
            :key="scene.sceneCode"
            :label="`${scene.sceneName} (${scene.sceneCode})`"
            :value="scene.sceneCode"
          />
        </el-select>
      </el-form-item>

      <el-row :gutter="18">
        <el-col :xs="24" :md="12">
          <el-form-item label="限流 QPS" prop="rateLimitQps">
            <el-input-number
              v-model="formData.rateLimitQps"
              :min="1"
              :precision="0"
              controls-position="right"
              class="risk-access-source-form__number"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item v-if="formType === 'create'" label="状态" prop="status">
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
          <el-form-item v-else label="状态">
            <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="formData.status" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="18">
        <el-col :xs="24" :md="12">
          <el-form-item label="IP 白名单" prop="ipWhitelistText">
            <el-input
              v-model="formData.ipWhitelistText"
              type="textarea"
              :rows="6"
              placeholder='请输入 JSON 数组，例如 ["10.0.0.1","172.20.8.0/24"]'
              class="risk-access-source-form__code-input"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="描述" prop="description">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="6"
              placeholder="请输入接入源描述"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <el-button type="primary" :disabled="formLoading" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </RiskCenterDialog>
</template>

<script setup lang="ts">
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getIntDictOptions, getStrDictOptions } from '@/utils/dict'
import * as SceneApi from '@/api/risk/scene'
import * as AccessSourceApi from '@/api/risk/access-source'
import RiskCenterDialog from '../../components/RiskCenterDialog.vue'
import type { FormRules } from 'element-plus'

defineOptions({ name: 'RiskAccessSourceForm' })

type AccessSourceFormData = AccessSourceApi.AccessSourceVO & {
  ipWhitelistText: string
}

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref()
const sceneOptions = ref<SceneApi.SceneVO[]>([])

const sourceTypeOptions = computed(() => getStrDictOptions(DICT_TYPE.RISK_ACCESS_SOURCE_TYPE))
const topicNameOptions = computed(() => getStrDictOptions(DICT_TYPE.RISK_ACCESS_TOPIC_NAME))

const createDefaultFormData = (): AccessSourceFormData => ({
  id: undefined,
  sourceCode: '',
  sourceName: '',
  sourceType: '',
  topicName: '',
  rateLimitQps: undefined,
  allowedSceneCodes: [],
  ipWhitelist: [],
  ipWhitelistText: '[]',
  status: CommonStatusEnum.ENABLE,
  description: ''
})

const formData = ref<AccessSourceFormData>(createDefaultFormData())

const parseIpWhitelistText = (value: string) => {
  if (!value.trim()) {
    return []
  }
  let parsed: unknown
  try {
    parsed = JSON.parse(value)
  } catch (error: any) {
    throw new Error(`IP 白名单格式不正确：${error.message}`)
  }
  if (!Array.isArray(parsed)) {
    throw new Error('IP 白名单必须是 JSON 数组')
  }
  const ipWhitelist = parsed.map((item) => {
    if (typeof item !== 'string' || !item.trim()) {
      throw new Error('IP 白名单中的每一项都必须是非空字符串')
    }
    return item.trim()
  })
  return Array.from(new Set(ipWhitelist))
}

const validateIpWhitelistText = (_rule: any, value: string, callback: (error?: Error) => void) => {
  try {
    parseIpWhitelistText(value || '[]')
    callback()
  } catch (error: any) {
    callback(error)
  }
}

const formRules = reactive<FormRules>({
  sourceName: [{ required: true, message: '接入源名称不能为空', trigger: 'blur' }],
  sourceCode: [
    { required: true, message: '接入源编码不能为空', trigger: 'blur' },
    {
      pattern: /^[A-Z0-9_]+$/,
      message: '接入源编码只能包含大写字母、数字和下划线',
      trigger: 'blur'
    }
  ],
  sourceType: [{ required: true, message: '接入类型不能为空', trigger: 'change' }],
  topicName: [{ required: true, message: '标准 Topic 不能为空', trigger: 'change' }],
  allowedSceneCodes: [{ required: true, message: '至少选择一个允许场景', trigger: 'change' }],
  ipWhitelistText: [{ validator: validateIpWhitelistText, trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const loadSceneOptions = async () => {
  sceneOptions.value = await SceneApi.getSimpleSceneList()
}

const open = async (type: 'create' | 'update', id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? t('action.create') : t('action.update')
  formType.value = type
  resetForm()
  await loadSceneOptions()
  if (id) {
    formLoading.value = true
    try {
      const data = await AccessSourceApi.getAccessSource(id)
      formData.value = {
        ...data,
        sourceCode: data.sourceCode || '',
        sourceName: data.sourceName || '',
        sourceType: data.sourceType || '',
        topicName: data.topicName || '',
        allowedSceneCodes: data.allowedSceneCodes || [],
        ipWhitelist: data.ipWhitelist || [],
        ipWhitelistText: JSON.stringify(data.ipWhitelist || [], null, 2),
        description: data.description || ''
      }
    } finally {
      formLoading.value = false
    }
  }
}

defineExpose({ open })

const emit = defineEmits(['success'])

const buildPayload = (): AccessSourceApi.AccessSourceVO | null => {
  try {
    const ipWhitelist = parseIpWhitelistText(formData.value.ipWhitelistText || '[]')
    return {
      id: formData.value.id,
      sourceCode: formData.value.sourceCode,
      sourceName: formData.value.sourceName,
      sourceType: formData.value.sourceType,
      topicName: formData.value.topicName,
      rateLimitQps: formData.value.rateLimitQps ?? undefined,
      allowedSceneCodes: Array.from(new Set(formData.value.allowedSceneCodes)),
      ipWhitelist: ipWhitelist.length ? ipWhitelist : undefined,
      status: formData.value.status,
      description: formData.value.description || undefined
    }
  } catch (error: any) {
    message.error(error.message)
    return null
  }
}

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
      await AccessSourceApi.createAccessSource(payload)
      message.success(t('common.createSuccess'))
    } else {
      await AccessSourceApi.updateAccessSource(payload)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = createDefaultFormData()
  formRef.value?.resetFields()
}
</script>

<style scoped lang="scss">
.risk-access-source-form {
  :deep(.el-form-item) {
    margin-bottom: 16px;
  }
}

.risk-access-source-form__number {
  width: 100%;
}

.risk-access-source-form__code-input {
  :deep(.el-textarea__inner) {
    font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
    line-height: 1.6;
  }
}
</style>
