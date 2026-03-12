<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="1020px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-alert
        title="S12 当前只做 FIRST_HIT；可维护默认动作并对规则顺序做上移/下移排序。"
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
          <el-form-item label="策略编码" prop="policyCode">
            <el-input
              v-model="formData.policyCode"
              placeholder="请输入策略编码"
              :disabled="formType === 'update'"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="策略名称" prop="policyName">
            <el-input v-model="formData.policyName" placeholder="请输入策略名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="决策模式">
            <el-input model-value="FIRST_HIT / 首条命中即返回" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="默认动作" prop="defaultAction">
            <el-select v-model="formData.defaultAction" class="w-full" placeholder="请选择默认动作">
              <el-option
                v-for="item in riskPolicyDefaultActionOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
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
          <el-form-item label="添加规则">
            <div class="flex w-full gap-12px">
              <el-select v-model="selectedRuleCode" class="flex-1" filterable clearable placeholder="请选择待加入策略的规则">
                <el-option
                  v-for="item in availableRuleOptions"
                  :key="item.ruleCode"
                  :label="`${item.ruleCode} - ${item.ruleName}`"
                  :value="item.ruleCode"
                >
                  <div class="flex items-center justify-between gap-12px">
                    <span>{{ item.ruleCode }} - {{ item.ruleName }}</span>
                    <span class="text-12px text-#909399">{{ getRiskPolicyDefaultActionLabel(item.hitAction) }} / 优先级 {{ item.priority }}</span>
                  </div>
                </el-option>
              </el-select>
              <el-button type="primary" plain @click="addRule">添加规则</el-button>
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="规则顺序" prop="ruleCodes">
            <div class="w-full">
              <el-table :data="selectedRuleRows" border>
                <el-table-column label="顺序" align="center" width="80">
                  <template #default="scope">{{ scope.$index + 1 }}</template>
                </el-table-column>
                <el-table-column label="规则编码" align="center" prop="ruleCode" min-width="120" />
                <el-table-column label="规则名称" align="center" prop="ruleName" min-width="180" />
                <el-table-column label="命中动作" align="center" min-width="150">
                  <template #default="scope">
                    <el-tag :type="getRiskPolicyDefaultActionTag(scope.row.hitAction)" effect="plain">
                      {{ getRiskPolicyDefaultActionLabel(scope.row.hitAction) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="优先级" align="center" prop="priority" width="100" />
                <el-table-column label="状态" align="center" prop="status" width="100">
                  <template #default="scope">
                    <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" align="center" width="180" fixed="right">
                  <template #default="scope">
                    <el-button link type="primary" :disabled="scope.$index === 0" @click="moveUp(scope.$index)">上移</el-button>
                    <el-button
                      link
                      type="primary"
                      :disabled="scope.$index === selectedRuleRows.length - 1"
                      @click="moveDown(scope.$index)"
                    >
                      下移
                    </el-button>
                    <el-button link type="danger" @click="removeRule(scope.row.ruleCode)">移除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="!selectedRuleRows.length" class="mt-8px text-12px text-#909399">请至少添加一条规则，并调整到期望顺序。</div>
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="策略说明" prop="description">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="4"
              placeholder="建议写清楚默认动作、规则顺序意图和 FIRST_HIT 收敛逻辑"
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
import * as PolicyApi from '@/api/risk/policy'
import {
  getRiskPolicyDefaultActionLabel,
  getRiskPolicyDefaultActionTag,
  riskPolicyDefaultActionOptions
} from './constants'

defineOptions({ name: 'RiskPolicyForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const ruleOptions = ref<PolicyApi.PolicyRuleOptionVO[]>([])
const selectedRuleCode = ref<string>()

const createDefaultFormData = (): PolicyApi.PolicyVO => ({
  id: undefined,
  sceneCode: 'TRADE_RISK',
  policyCode: 'TRADE_RISK_POLICY',
  policyName: '交易风控主策略',
  decisionMode: 'FIRST_HIT',
  defaultAction: 'PASS',
  ruleCodes: ['R001', 'R003', 'R002'],
  ruleRefs: [],
  status: CommonStatusEnum.ENABLE,
  description: ''
})

const formData = ref<PolicyApi.PolicyVO>(createDefaultFormData())
const formRef = ref()

const ruleOptionsMap = computed(() => {
  return new Map(ruleOptions.value.map((item) => [item.ruleCode, item]))
})

const selectedRuleRows = computed(() => {
  return formData.value.ruleCodes.map((ruleCode) => {
    const option = ruleOptionsMap.value.get(ruleCode)
    return option || {
      ruleCode,
      ruleName: ruleCode,
      hitAction: 'TAG_ONLY',
      priority: 0,
      status: CommonStatusEnum.ENABLE
    }
  })
})

const availableRuleOptions = computed(() => {
  const selectedSet = new Set(formData.value.ruleCodes)
  return ruleOptions.value.filter((item) => !selectedSet.has(item.ruleCode))
})

const formRules = reactive({
  sceneCode: [{ required: true, message: '所属场景不能为空', trigger: 'blur' }],
  policyCode: [
    { required: true, message: '策略编码不能为空', trigger: 'blur' },
    {
      pattern: /^[A-Za-z][A-Za-z0-9_]*$/,
      message: '策略编码只允许字母、数字、下划线，且必须以字母开头',
      trigger: 'blur'
    }
  ],
  policyName: [{ required: true, message: '策略名称不能为空', trigger: 'blur' }],
  defaultAction: [{ required: true, message: '默认动作不能为空', trigger: 'change' }],
  ruleCodes: [{ type: 'array', required: true, message: '规则顺序不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const resetForm = () => {
  formData.value = createDefaultFormData()
  selectedRuleCode.value = undefined
  ruleOptions.value = []
  formRef.value?.resetFields()
}

const loadRuleOptions = async () => {
  if (!formData.value.sceneCode?.trim()) {
    ruleOptions.value = []
    return
  }
  ruleOptions.value = await PolicyApi.getRuleOptions(formData.value.sceneCode.trim())
}

watch(
  () => formData.value.sceneCode,
  async (value, oldValue) => {
    if (!dialogVisible.value || !value || value === oldValue) {
      return
    }
    await loadRuleOptions()
    if (formType.value === 'create') {
      formData.value.ruleCodes = []
    }
  }
)

const addRule = () => {
  if (!selectedRuleCode.value) {
    message.error('请先选择一条规则')
    return
  }
  if (formData.value.ruleCodes.includes(selectedRuleCode.value)) {
    message.error('该规则已在当前策略中')
    return
  }
  formData.value.ruleCodes = [...formData.value.ruleCodes, selectedRuleCode.value]
  selectedRuleCode.value = undefined
}

const moveUp = (index: number) => {
  if (index < 1) {
    return
  }
  const list = [...formData.value.ruleCodes]
  ;[list[index - 1], list[index]] = [list[index], list[index - 1]]
  formData.value.ruleCodes = list
}

const moveDown = (index: number) => {
  if (index >= formData.value.ruleCodes.length - 1) {
    return
  }
  const list = [...formData.value.ruleCodes]
  ;[list[index], list[index + 1]] = [list[index + 1], list[index]]
  formData.value.ruleCodes = list
}

const removeRule = (ruleCode: string) => {
  formData.value.ruleCodes = formData.value.ruleCodes.filter((item) => item !== ruleCode)
}

const open = async (type: 'create' | 'update', id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await PolicyApi.getPolicy(id)
      await loadRuleOptions()
    } finally {
      formLoading.value = false
    }
  } else {
    await loadRuleOptions()
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])

const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return

  formLoading.value = true
  try {
    const data: PolicyApi.PolicyVO = {
      ...formData.value,
      sceneCode: formData.value.sceneCode.trim(),
      policyCode: formData.value.policyCode.trim(),
      policyName: formData.value.policyName.trim(),
      ruleCodes: [...formData.value.ruleCodes],
      description: formData.value.description?.trim() || undefined
    }
    if (formType.value === 'create') {
      await PolicyApi.createPolicy(data)
      message.success(t('common.createSuccess'))
    } else {
      await PolicyApi.updatePolicy(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
