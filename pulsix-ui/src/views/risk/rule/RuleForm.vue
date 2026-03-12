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
        title="S11 只做单条规则表达式、优先级、动作和命中原因模板；策略收敛与排序留到 S12。"
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
          <el-form-item label="规则编码" prop="ruleCode">
            <el-input
              v-model="formData.ruleCode"
              placeholder="请输入规则编码"
              :disabled="formType === 'update'"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="规则名称" prop="ruleName">
            <el-input v-model="formData.ruleName" placeholder="请输入规则名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="规则类型" prop="ruleType">
            <el-select v-model="formData.ruleType" class="w-full" placeholder="请选择规则类型">
              <el-option v-for="item in riskRuleTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="表达式引擎" prop="engineType">
            <el-select v-model="formData.engineType" class="w-full" placeholder="请选择表达式引擎">
              <el-option
                v-for="item in riskRuleEngineTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="命中动作" prop="hitAction">
            <el-select v-model="formData.hitAction" class="w-full" placeholder="请选择命中动作">
              <el-option
                v-for="item in riskRuleHitActionOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="规则优先级" prop="priority">
            <el-input-number v-model="formData.priority" class="!w-full" :min="0" :precision="0" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="风险分值" prop="riskScore">
            <el-input-number v-model="formData.riskScore" class="!w-full" :min="0" :precision="0" />
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
          <el-form-item label="规则表达式" prop="exprContent">
            <el-input
              v-model="formData.exprContent"
              type="textarea"
              :rows="6"
              :placeholder="exprPlaceholder"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="命中原因模板" prop="hitReasonTemplate">
            <el-input
              v-model="formData.hitReasonTemplate"
              type="textarea"
              :rows="3"
              placeholder="例如 用户5分钟交易次数={user_trade_cnt_5m}, 当前金额={amount}"
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
              v-hasPermi="['risk:rule:validate']"
            >
              <Icon icon="ep:circle-check" class="mr-5px" /> 校验规则
            </el-button>
            <span class="ml-12px text-12px text-#909399">
              会校验表达式编译结果，并检查命中原因模板中的占位符是否存在。
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
          <el-form-item label="规则说明" prop="description">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="4"
              placeholder="建议写清楚规则意图、误杀边界和适用场景"
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
import * as RuleApi from '@/api/risk/rule'
import { riskRuleEngineTypeOptions, riskRuleHitActionOptions, riskRuleTypeOptions } from './constants'

defineOptions({ name: 'RiskRuleForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const validateLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const validateResult = ref<RuleApi.RuleValidateRespVO>()

const createDefaultFormData = (): RuleApi.RuleVO => ({
  id: undefined,
  sceneCode: 'TRADE_RISK',
  ruleCode: '',
  ruleName: '',
  ruleType: 'NORMAL',
  engineType: 'AVIATOR',
  exprContent: 'device_in_blacklist == true',
  priority: 100,
  hitAction: 'REJECT',
  riskScore: 100,
  hitReasonTemplate: '设备命中黑名单',
  status: CommonStatusEnum.ENABLE,
  description: ''
})

const formData = ref<RuleApi.RuleVO>(createDefaultFormData())
const formRef = ref()

const exprPlaceholder = computed(() => {
  if (formData.value.engineType === 'GROOVY') {
    return "例如 return device_bind_user_cnt_1h >= 4 && ['M','H'].contains(user_risk_level)"
  }
  if (formData.value.engineType === 'DSL') {
    return '例如 amount >= 5000 && user_trade_cnt_5m >= 3'
  }
  return '例如 device_in_blacklist == true'
})

const formRules = reactive({
  sceneCode: [{ required: true, message: '所属场景不能为空', trigger: 'blur' }],
  ruleCode: [
    { required: true, message: '规则编码不能为空', trigger: 'blur' },
    {
      pattern: /^[A-Za-z][A-Za-z0-9_]*$/,
      message: '规则编码只允许字母、数字、下划线，且必须以字母开头',
      trigger: 'blur'
    }
  ],
  ruleName: [{ required: true, message: '规则名称不能为空', trigger: 'blur' }],
  ruleType: [{ required: true, message: '规则类型不能为空', trigger: 'change' }],
  engineType: [{ required: true, message: '表达式引擎不能为空', trigger: 'change' }],
  exprContent: [{ required: true, message: '规则表达式不能为空', trigger: 'blur' }],
  priority: [{ required: true, message: '规则优先级不能为空', trigger: 'change' }],
  hitAction: [{ required: true, message: '命中动作不能为空', trigger: 'change' }],
  riskScore: [{ required: true, message: '风险分值不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const resetForm = () => {
  formData.value = createDefaultFormData()
  validateResult.value = undefined
  formRef.value?.resetFields()
}

watch(
  () => formData.value.ruleType,
  (value) => {
    if (!dialogVisible.value || formType.value !== 'create') {
      return
    }
    if (value === 'TAG_ONLY') {
      formData.value.hitAction = 'TAG_ONLY'
      if (!formData.value.hitReasonTemplate?.trim()) {
        formData.value.hitReasonTemplate = '命中标签规则'
      }
      return
    }
    if (value === 'MANUAL_REVIEW_HINT') {
      formData.value.hitAction = 'REVIEW'
      return
    }
  }
)

const open = async (type: 'create' | 'update', id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await RuleApi.getRule(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])

const buildValidateReq = (): RuleApi.RuleValidateReqVO => ({
  sceneCode: formData.value.sceneCode.trim(),
  engineType: formData.value.engineType,
  exprContent: formData.value.exprContent.trim(),
  hitReasonTemplate: formData.value.hitReasonTemplate?.trim() || undefined
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
  if (!formData.value.exprContent?.trim()) {
    message.error('规则表达式不能为空')
    return
  }
  validateLoading.value = true
  try {
    validateResult.value = await RuleApi.validateRule(buildValidateReq())
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

  formLoading.value = true
  try {
    const data: RuleApi.RuleVO = {
      ...formData.value,
      sceneCode: formData.value.sceneCode.trim(),
      ruleCode: formData.value.ruleCode.trim(),
      ruleName: formData.value.ruleName.trim(),
      exprContent: formData.value.exprContent.trim(),
      hitReasonTemplate: formData.value.hitReasonTemplate?.trim() || undefined,
      description: formData.value.description?.trim() || undefined
    }
    if (formType.value === 'create') {
      await RuleApi.createRule(data)
      message.success(t('common.createSuccess'))
    } else {
      await RuleApi.updateRule(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
