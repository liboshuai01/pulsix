<template>
  <Dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="90%"
    :fullscreen="false"
    :scroll="true"
    max-height="78vh"
  >
    <el-form ref="formRef" v-loading="formLoading" :model="formData" :rules="formRules" label-width="110px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="所属场景" prop="sceneCode">
            <el-input v-model="formData.sceneCode" placeholder="请输入场景编码，例如 TRADE_RISK" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="用例编码" prop="caseCode">
            <el-input v-model="formData.caseCode" placeholder="请输入用例编码，例如 SIM_REJECT_BLACKLIST" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="用例名称" prop="caseName">
            <el-input v-model="formData.caseName" placeholder="请输入用例名称" />
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

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="版本模式" prop="versionSelectMode">
            <el-select v-model="formData.versionSelectMode" class="w-full" placeholder="请选择版本模式">
              <el-option
                v-for="item in simulationVersionSelectModeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="固定版本号" prop="versionNo">
            <el-input-number
              v-model="formData.versionNo"
              :min="1"
              :disabled="formData.versionSelectMode !== 'FIXED'"
              class="!w-full"
              controls-position="right"
              placeholder="请输入固定版本号"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="用例说明" prop="description">
        <el-input
          v-model="formData.description"
          :rows="3"
          type="textarea"
          placeholder="请输入用例说明，例如本次主要验证黑名单或高频交易链路"
        />
      </el-form-item>

      <el-form-item label="断言说明">
        <el-alert
          title="期望动作与命中规则可留空；留空时仅执行仿真，不对对应结果做断言。"
          type="info"
          :closable="false"
          show-icon
        />
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="期望动作" prop="expectedAction">
            <el-select v-model="formData.expectedAction" class="w-full" clearable placeholder="留空表示不校验动作">
              <el-option
                v-for="item in riskActionOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="期望规则" prop="expectedHitRules">
            <el-select
              v-model="formData.expectedHitRules"
              class="w-full"
              multiple
              filterable
              allow-create
              default-first-option
              clearable
              placeholder="可录入多个规则编码，例如 R001、R002"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="输入事件" prop="inputEventJson" required>
        <div class="w-full">
          <div class="mb-8px text-12px leading-20px text-[var(--el-text-color-secondary)]">
            请输入单条标准事件 JSON；系统会自动补齐缺失的 sceneCode、traceId、eventId、eventTime、eventType。
          </div>
          <JsonEditor v-model="formData.inputEventJson" mode="code" height="320px" @error="(errors) => onJsonError('inputEventJson', errors)" />
          <div v-if="jsonErrors.inputEventJson" class="mt-8px text-12px text-[var(--el-color-danger)]">
            输入事件 JSON 格式不正确，请修正后再提交
          </div>
        </div>
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="模拟特征" prop="mockFeatureJson">
            <div class="w-full">
              <div class="mb-8px text-12px leading-20px text-[var(--el-text-color-secondary)]">
                将映射到内核 `streamFeatures` 覆盖，例如 `{ "user_trade_cnt_5m": 3 }`。
              </div>
              <JsonEditor v-model="formData.mockFeatureJson" mode="code" height="220px" @error="(errors) => onJsonError('mockFeatureJson', errors)" />
              <div v-if="jsonErrors.mockFeatureJson" class="mt-8px text-12px text-[var(--el-color-danger)]">
                模拟特征 JSON 格式不正确，请修正后再提交
              </div>
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="模拟 Lookup" prop="mockLookupJson">
            <div class="w-full">
              <div class="mb-8px text-12px leading-20px text-[var(--el-text-color-secondary)]">
                将映射到内核 `lookupFeatures` 覆盖，例如 `{ "device_in_blacklist": true }`。
              </div>
              <JsonEditor v-model="formData.mockLookupJson" mode="code" height="220px" @error="(errors) => onJsonError('mockLookupJson', errors)" />
              <div v-if="jsonErrors.mockLookupJson" class="mt-8px text-12px text-[var(--el-color-danger)]">
                模拟 Lookup JSON 格式不正确，请修正后再提交
              </div>
            </div>
          </el-form-item>
        </el-col>
      </el-row>
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
import * as SimulationApi from '@/api/risk/simulation'
import { riskActionOptions, simulationVersionSelectModeOptions } from './constants'

defineOptions({ name: 'RiskSimulationCaseForm' })

const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref()

const createDefaultFormData = (): SimulationApi.SimulationCaseVO => ({
  id: undefined,
  sceneCode: 'TRADE_RISK',
  caseCode: '',
  caseName: '',
  versionSelectMode: 'LATEST',
  versionNo: undefined,
  inputEventJson: {},
  mockFeatureJson: {},
  mockLookupJson: {},
  expectedAction: undefined,
  expectedHitRules: [],
  status: CommonStatusEnum.ENABLE,
  description: ''
})

const formData = ref<SimulationApi.SimulationCaseVO>(createDefaultFormData())
const jsonErrors = reactive({
  inputEventJson: false,
  mockFeatureJson: false,
  mockLookupJson: false
})

const ensureObject = (value: unknown): Record<string, any> => {
  return value && typeof value === 'object' && !Array.isArray(value) ? (value as Record<string, any>) : {}
}

const trimToUndefined = (value?: string) => {
  const normalized = value?.trim()
  return normalized ? normalized : undefined
}

const normalizeHitRules = (value?: string[]) => {
  return Array.from(new Set((value ?? []).map((item) => item?.trim()).filter(Boolean)))
}

const normalizeFormData = (data?: Partial<SimulationApi.SimulationCaseVO>): SimulationApi.SimulationCaseVO => ({
  ...createDefaultFormData(),
  ...data,
  sceneCode: trimToUndefined(data?.sceneCode) ?? 'TRADE_RISK',
  caseCode: trimToUndefined(data?.caseCode) ?? '',
  caseName: trimToUndefined(data?.caseName) ?? '',
  versionSelectMode: trimToUndefined(data?.versionSelectMode) ?? 'LATEST',
  versionNo: data?.versionSelectMode === 'FIXED' || data?.versionSelectMode === undefined ? data?.versionNo : undefined,
  inputEventJson: ensureObject(data?.inputEventJson),
  mockFeatureJson: ensureObject(data?.mockFeatureJson),
  mockLookupJson: ensureObject(data?.mockLookupJson),
  expectedAction: trimToUndefined(data?.expectedAction),
  expectedHitRules: normalizeHitRules(data?.expectedHitRules),
  status: data?.status ?? CommonStatusEnum.ENABLE,
  description: trimToUndefined(data?.description) ?? ''
})

const buildSubmitData = (): SimulationApi.SimulationCaseVO => {
  const data = normalizeFormData(formData.value)
  return {
    id: data.id,
    sceneCode: data.sceneCode,
    caseCode: data.caseCode,
    caseName: data.caseName,
    versionSelectMode: data.versionSelectMode,
    versionNo: data.versionSelectMode === 'FIXED' ? data.versionNo : undefined,
    inputEventJson: ensureObject(data.inputEventJson),
    mockFeatureJson: ensureObject(data.mockFeatureJson),
    mockLookupJson: ensureObject(data.mockLookupJson),
    expectedAction: data.expectedAction,
    expectedHitRules: normalizeHitRules(data.expectedHitRules),
    status: data.status,
    description: trimToUndefined(data.description)
  }
}

const formRules = reactive({
  sceneCode: [{ required: true, message: '所属场景不能为空', trigger: 'blur' }],
  caseCode: [
    { required: true, message: '用例编码不能为空', trigger: 'blur' },
    {
      pattern: /^[A-Za-z][A-Za-z0-9_]*$/,
      message: '用例编码只允许字母、数字、下划线，且必须以字母开头',
      trigger: 'blur'
    }
  ],
  caseName: [{ required: true, message: '用例名称不能为空', trigger: 'blur' }],
  versionSelectMode: [{ required: true, message: '版本模式不能为空', trigger: 'change' }],
  versionNo: [
    {
      validator: (_rule, value, callback) => {
        if (formData.value.versionSelectMode === 'FIXED' && (!value || value <= 0)) {
          callback(new Error('固定版本模式下必须填写大于 0 的版本号'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }],
  inputEventJson: [
    {
      validator: (_rule, value, callback) => {
        const payload = ensureObject(value)
        if (Object.keys(payload).length === 0) {
          callback(new Error('输入事件 JSON 不能为空'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
})

const resetForm = () => {
  formData.value = createDefaultFormData()
  jsonErrors.inputEventJson = false
  jsonErrors.mockFeatureJson = false
  jsonErrors.mockLookupJson = false
  formRef.value?.resetFields()
}

const open = async (type: 'create' | 'update', id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? '新增仿真用例' : '编辑仿真用例'
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      const data = await SimulationApi.getSimulationCase(id)
      formData.value = normalizeFormData(data)
    } finally {
      formLoading.value = false
    }
  }
}

defineExpose({ open })

const emit = defineEmits(['success'])

const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return
  if (Object.values(jsonErrors).some(Boolean)) {
    message.warning('存在 JSON 格式错误，请修正后再提交')
    return
  }
  formLoading.value = true
  try {
    const payload = buildSubmitData()
    if (formType.value === 'create') {
      await SimulationApi.createSimulationCase(payload)
      message.success('创建成功')
    } else {
      await SimulationApi.updateSimulationCase(payload)
      message.success('更新成功')
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const onJsonError = (field: 'inputEventJson' | 'mockFeatureJson' | 'mockLookupJson', errors: any) => {
  jsonErrors[field] = !isEmpty(errors)
}

watch(
  () => formData.value.versionSelectMode,
  (value) => {
    if (value !== 'FIXED') {
      formData.value.versionNo = undefined
    }
  }
)
</script>
