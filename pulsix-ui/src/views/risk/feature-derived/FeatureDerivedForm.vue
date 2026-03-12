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
        title="S10 先聚焦依赖选择、表达式校验和结果类型；Groovy 在当前阶段强制开启脚本沙箱。"
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
          <el-form-item label="表达式引擎" prop="engineType">
            <el-select v-model="formData.engineType" class="w-full" placeholder="请选择表达式引擎">
              <el-option
                v-for="item in riskFeatureDerivedEngineTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="结果类型" prop="valueType">
            <el-select v-model="formData.valueType" class="w-full" placeholder="请选择结果类型">
              <el-option
                v-for="item in riskFeatureDerivedValueTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="脚本沙箱" prop="sandboxFlag">
            <el-switch
              v-model="formData.sandboxFlag"
              :active-value="1"
              :inactive-value="0"
              :disabled="formData.engineType === 'GROOVY'"
            />
            <span class="ml-12px text-12px text-#909399">
              {{ formData.engineType === 'GROOVY' ? 'Groovy 当前阶段固定开启' : 'Aviator 可按需关闭' }}
            </span>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="超时(ms)" prop="timeoutMs">
            <el-input-number v-model="formData.timeoutMs" class="!w-full" :min="1" :precision="0" />
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
        <el-col :span="24">
          <el-form-item label="依赖项" prop="dependsOnJson">
            <el-select
              v-model="formData.dependsOnJson"
              class="w-full"
              multiple
              filterable
              clearable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择字段/特征依赖项"
            >
              <el-option
                v-for="item in dependencyOptions"
                :key="item.code"
                :label="buildRiskFeatureDerivedDependencyLabel(item)"
                :value="item.code"
              >
                <div class="flex items-center justify-between gap-12px">
                  <span>{{ item.code }} - {{ item.name || item.code }}</span>
                  <span class="text-12px text-#909399">
                    {{ getRiskFeatureDerivedDependencyTypeLabel(item.dependencyType) }}
                    <template v-if="item.valueType"> / {{ item.valueType }}</template>
                  </span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="表达式内容" prop="exprContent">
            <el-input
              v-model="formData.exprContent"
              type="textarea"
              :rows="6"
              :placeholder="exprPlaceholder"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item>
            <el-button
              type="primary"
              plain
              :loading="validateLoading"
              @click="handleValidate"
              v-hasPermi="['risk:feature-derived:validate']"
            >
              <Icon icon="ep:circle-check" class="mr-5px" /> 校验表达式
            </el-button>
            <span class="ml-12px text-12px text-#909399">
              依赖存在性、循环依赖和脚本编译都会在这里校验。
            </span>
          </el-form-item>
        </el-col>
        <el-col :span="24" v-if="validateResult">
          <el-alert
            :title="validateResult.message"
            :type="validateResult.valid ? 'success' : 'error'"
            :closable="false"
            class="mb-8px"
          />
        </el-col>
        <el-col :span="24">
          <el-form-item label="扩展配置 JSON">
            <el-input
              v-model="extraJsonText"
              type="textarea"
              :rows="5"
              placeholder="可选，例如 {&#10;  &quot;resultAlias&quot;: &quot;tradeBurstFlag&quot;&#10;}"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="口径说明" prop="description">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="4"
              placeholder="建议写清楚依赖来源、表达式含义和结果类型约定"
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
import * as FeatureDerivedApi from '@/api/risk/featureDerived'
import {
  buildRiskFeatureDerivedDependencyLabel,
  getRiskFeatureDerivedDependencyTypeLabel,
  riskFeatureDerivedEngineTypeOptions,
  riskFeatureDerivedValueTypeOptions
} from './constants'

defineOptions({ name: 'RiskFeatureDerivedForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const validateLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const dependencyOptions = ref<FeatureDerivedApi.FeatureDerivedDependencyOptionVO[]>([])
const extraJsonText = ref('')
const validateResult = ref<FeatureDerivedApi.FeatureDerivedValidateRespVO>()

const createDefaultFormData = (): FeatureDerivedApi.FeatureDerivedVO => ({
  id: undefined,
  sceneCode: 'TRADE_RISK',
  featureCode: '',
  featureName: '',
  engineType: 'AVIATOR',
  exprContent: 'amount >= 5000',
  dependsOnJson: ['amount'],
  valueType: 'BOOLEAN',
  sandboxFlag: 1,
  timeoutMs: 50,
  extraJson: undefined,
  status: CommonStatusEnum.ENABLE,
  description: ''
})

const formData = ref<FeatureDerivedApi.FeatureDerivedVO>(createDefaultFormData())
const formRef = ref()

const exprPlaceholder = computed(() => {
  if (formData.value.engineType === 'GROOVY') {
    return '例如 return user_trade_cnt_5m >= 3 && amount >= 5000'
  }
  return '例如 user_trade_cnt_5m >= 3 && amount >= 5000'
})

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
  engineType: [{ required: true, message: '表达式引擎不能为空', trigger: 'change' }],
  valueType: [{ required: true, message: '结果类型不能为空', trigger: 'change' }],
  dependsOnJson: [{ type: 'array', required: true, message: '依赖项不能为空', trigger: 'change' }],
  exprContent: [{ required: true, message: '表达式内容不能为空', trigger: 'blur' }],
  sandboxFlag: [{ required: true, message: '脚本沙箱不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const resetForm = () => {
  formData.value = createDefaultFormData()
  dependencyOptions.value = []
  extraJsonText.value = ''
  validateResult.value = undefined
  formRef.value?.resetFields()
}

const applyEngineDefaults = () => {
  if (formData.value.engineType === 'GROOVY') {
    formData.value.sandboxFlag = 1
  }
}

watch(
  () => formData.value.engineType,
  () => {
    if (!dialogVisible.value) {
      return
    }
    applyEngineDefaults()
    validateResult.value = undefined
  }
)

watch(
  () => formData.value.sceneCode,
  async (value, oldValue) => {
    if (!dialogVisible.value || !value || value === oldValue) {
      return
    }
    await loadDependencyOptions()
    validateResult.value = undefined
  }
)

const loadDependencyOptions = async () => {
  if (!formData.value.sceneCode?.trim()) {
    dependencyOptions.value = []
    return
  }
  dependencyOptions.value = await FeatureDerivedApi.getDependencyOptions(
    formData.value.sceneCode.trim(),
    formType.value === 'update' ? formData.value.featureCode : undefined
  )
}

const open = async (type: 'create' | 'update', id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await FeatureDerivedApi.getFeatureDerived(id)
      extraJsonText.value = formData.value.extraJson ? JSON.stringify(formData.value.extraJson, null, 2) : ''
      applyEngineDefaults()
      await loadDependencyOptions()
    } finally {
      formLoading.value = false
    }
  } else {
    applyEngineDefaults()
    await loadDependencyOptions()
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])

const buildValidateReq = (): FeatureDerivedApi.FeatureDerivedValidateReqVO => ({
  sceneCode: formData.value.sceneCode.trim(),
  featureCode: formType.value === 'update' ? formData.value.featureCode.trim() : formData.value.featureCode?.trim() || undefined,
  engineType: formData.value.engineType,
  exprContent: formData.value.exprContent.trim(),
  dependsOnJson: formData.value.dependsOnJson,
  sandboxFlag: formData.value.engineType === 'GROOVY' ? 1 : formData.value.sandboxFlag
})

const handleValidate = async () => {
  if (!formData.value.sceneCode?.trim()) {
    message.error('所属场景不能为空')
    return
  }
  if (!formData.value.engineType) {
    message.error('表达式引擎不能为空')
    return
  }
  if (!formData.value.dependsOnJson?.length) {
    message.error('依赖项不能为空')
    return
  }
  if (!formData.value.exprContent?.trim()) {
    message.error('表达式内容不能为空')
    return
  }

  validateLoading.value = true
  try {
    validateResult.value = await FeatureDerivedApi.validateFeatureDerivedExpression(buildValidateReq())
    if (validateResult.value.valid) {
      message.success(validateResult.value.message)
    } else {
      message.error(validateResult.value.message)
    }
  } finally {
    validateLoading.value = false
  }
}

const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return

  let extraJson: Record<string, any> | undefined
  const extraJsonRaw = extraJsonText.value.trim()
  if (extraJsonRaw) {
    try {
      extraJson = JSON.parse(extraJsonRaw)
    } catch {
      message.error('扩展配置 JSON 格式不正确')
      return
    }
  }

  formLoading.value = true
  try {
    const data: FeatureDerivedApi.FeatureDerivedVO = {
      ...formData.value,
      sceneCode: formData.value.sceneCode.trim(),
      featureCode: formData.value.featureCode.trim(),
      featureName: formData.value.featureName.trim(),
      exprContent: formData.value.exprContent.trim(),
      dependsOnJson: formData.value.dependsOnJson,
      sandboxFlag: formData.value.engineType === 'GROOVY' ? 1 : formData.value.sandboxFlag,
      extraJson,
      description: formData.value.description?.trim() || undefined
    }
    if (formType.value === 'create') {
      await FeatureDerivedApi.createFeatureDerived(data)
      message.success(t('common.createSuccess'))
    } else {
      await FeatureDerivedApi.updateFeatureDerived(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
