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
          <el-form-item label="IP 白名单" prop="ipWhitelist">
            <div class="risk-access-source-form__whitelist-panel">
              <el-radio-group
                v-model="formData.ipWhitelistMode"
                class="risk-access-source-form__whitelist-mode"
              >
                <el-radio value="ALL">允许所有 IP</el-radio>
                <el-radio value="WHITELIST">启用白名单</el-radio>
              </el-radio-group>

              <el-alert
                v-if="formData.ipWhitelistMode === 'ALL'"
                title="当前未启用白名单，将允许所有来源 IP 接入。"
                type="info"
                :closable="false"
                class="risk-access-source-form__whitelist-alert"
              />

              <template v-else>
                <div class="risk-access-source-form__whitelist-tip">
                  逐条添加允许接入的 IPv4 或 CIDR 网段，例如 `10.0.0.1`、`172.20.8.0/24`
                </div>
                <div class="risk-access-source-form__whitelist-input-row">
                  <el-input
                    ref="ipWhitelistInputRef"
                    v-model="formData.ipWhitelistDraft"
                    placeholder="请输入 IP 或 CIDR"
                    @keyup.enter="handleAddIpWhitelist"
                  />
                  <el-button type="primary" plain @click="handleAddIpWhitelist">添加</el-button>
                </div>
                <div
                  v-if="formData.ipWhitelist.length"
                  class="risk-access-source-form__whitelist-tag-list"
                >
                  <el-tag
                    v-for="item in formData.ipWhitelist"
                    :key="item"
                    closable
                    class="mr-8px mb-8px"
                    @close="handleRemoveIpWhitelist(item)"
                  >
                    {{ item }}
                  </el-tag>
                </div>
                <el-empty
                  v-else
                  description="请至少添加一个允许接入的 IP 或 CIDR"
                  :image-size="56"
                />
              </template>
            </div>
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

type AccessSourceFormData = Omit<AccessSourceApi.AccessSourceVO, 'ipWhitelist'> & {
  ipWhitelist: string[]
  ipWhitelistMode: 'ALL' | 'WHITELIST'
  ipWhitelistDraft: string
}

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref()
const ipWhitelistInputRef = ref()
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
  ipWhitelistMode: 'ALL',
  ipWhitelistDraft: '',
  status: CommonStatusEnum.ENABLE,
  description: ''
})

const formData = ref<AccessSourceFormData>(createDefaultFormData())

const IPV4_SEGMENT = '(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)'
const IPV4_REGEXP = new RegExp(`^${IPV4_SEGMENT}(\\.${IPV4_SEGMENT}){3}$`)
const IPV4_CIDR_REGEXP = new RegExp(`^${IPV4_SEGMENT}(\\.${IPV4_SEGMENT}){3}\\/(3[0-2]|[12]?\\d)$`)

const isValidIpWhitelistEntry = (value: string) =>
  IPV4_REGEXP.test(value) || IPV4_CIDR_REGEXP.test(value)

const validateIpWhitelist = (_rule: any, value: string[], callback: (error?: Error) => void) => {
  if (formData.value.ipWhitelistMode === 'ALL') {
    callback()
    return
  }
  if (!value?.length) {
    callback(new Error('启用白名单后，至少添加一个 IP 或 CIDR'))
    return
  }
  const invalidEntry = value.find((item) => !isValidIpWhitelistEntry(item))
  if (invalidEntry) {
    callback(new Error(`IP 白名单条目格式不正确：${invalidEntry}`))
    return
  }
  callback()
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
  ipWhitelist: [{ validator: validateIpWhitelist, trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const loadSceneOptions = async () => {
  sceneOptions.value = await SceneApi.getSimpleSceneList()
}

const focusIpWhitelistInput = () => {
  nextTick(() => {
    ipWhitelistInputRef.value?.focus?.()
  })
}

const handleAddIpWhitelist = () => {
  const candidate = formData.value.ipWhitelistDraft.trim()
  if (!candidate) {
    return
  }
  if (!isValidIpWhitelistEntry(candidate)) {
    message.error('只支持 IPv4 或 IPv4/CIDR，例如 10.0.0.1 或 172.20.8.0/24')
    return
  }
  if (formData.value.ipWhitelist.includes(candidate)) {
    message.warning('该 IP 白名单条目已存在')
    formData.value.ipWhitelistDraft = ''
    return
  }
  formData.value.ipWhitelist = [...formData.value.ipWhitelist, candidate]
  formData.value.ipWhitelistDraft = ''
  formRef.value?.clearValidate?.('ipWhitelist')
  focusIpWhitelistInput()
}

const handleRemoveIpWhitelist = (item: string) => {
  formData.value.ipWhitelist = formData.value.ipWhitelist.filter((entry) => entry !== item)
}

watch(
  () => formData.value.ipWhitelistMode,
  (mode) => {
    formRef.value?.clearValidate?.('ipWhitelist')
    if (mode === 'WHITELIST') {
      focusIpWhitelistInput()
      return
    }
    formData.value.ipWhitelistDraft = ''
  }
)

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
        ipWhitelistMode: data.ipWhitelist?.length ? 'WHITELIST' : 'ALL',
        ipWhitelistDraft: '',
        description: data.description || ''
      }
    } finally {
      formLoading.value = false
    }
  }
}

defineExpose({ open })

const emit = defineEmits(['success'])

const buildPayload = (): AccessSourceApi.AccessSourceVO => ({
  id: formData.value.id,
  sourceCode: formData.value.sourceCode,
  sourceName: formData.value.sourceName,
  sourceType: formData.value.sourceType,
  topicName: formData.value.topicName,
  rateLimitQps: formData.value.rateLimitQps ?? undefined,
  allowedSceneCodes: Array.from(new Set(formData.value.allowedSceneCodes)),
  ipWhitelist:
    formData.value.ipWhitelistMode === 'WHITELIST' && formData.value.ipWhitelist.length
      ? Array.from(new Set(formData.value.ipWhitelist))
      : undefined,
  status: formData.value.status,
  description: formData.value.description || undefined
})

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

.risk-access-source-form__whitelist-panel {
  width: 100%;
  border: 1px solid var(--el-border-color);
  border-radius: 10px;
  padding: 14px 14px 6px;
  background: var(--el-fill-color-blank);
}

.risk-access-source-form__whitelist-mode {
  margin-bottom: 12px;
}

.risk-access-source-form__whitelist-alert {
  margin-bottom: 8px;
}

.risk-access-source-form__whitelist-tip {
  margin-bottom: 10px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}

.risk-access-source-form__whitelist-input-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  margin-bottom: 12px;
}

.risk-access-source-form__whitelist-tag-list {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
}

@media (max-width: 768px) {
  .risk-access-source-form__whitelist-input-row {
    grid-template-columns: 1fr;
  }
}
</style>
