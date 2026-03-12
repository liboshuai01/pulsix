<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="1180px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-alert
        :title="isScoreCard ? 'S17 已补齐 SCORE_CARD：可维护评分段，并按总分预览最终动作。' : 'S17 在保留 FIRST_HIT 的同时补齐 SCORE_CARD；当前可切换策略决策模式。 '"
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
          <el-form-item label="决策模式" prop="decisionMode">
            <el-select v-model="formData.decisionMode" class="w-full" placeholder="请选择决策模式">
              <el-option
                v-for="item in riskPolicyDecisionModeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
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
                    <span class="text-12px text-#909399">
                      {{ getRiskPolicyDefaultActionLabel(item.hitAction) }} / 优先级 {{ item.priority }} / 分值 {{ item.riskScore ?? 0 }}
                    </span>
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
                <el-table-column label="规则分值" align="center" prop="riskScore" width="100" />
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
        <el-col v-if="isScoreCard" :span="24">
          <el-form-item label="评分段配置" prop="scoreBands">
            <div class="w-full">
              <div class="mb-12px flex items-center justify-between gap-12px">
                <span class="text-13px text-#606266">按行顺序维护分数区间；若区间有重叠，保存和预览都会拦截。</span>
                <el-button type="primary" plain @click="addScoreBand">新增分段</el-button>
              </div>
              <el-table :data="formData.scoreBands || []" border>
                <el-table-column label="顺序" align="center" width="80">
                  <template #default="scope">{{ scope.$index + 1 }}</template>
                </el-table-column>
                <el-table-column label="最小分值(含)" align="center" min-width="150">
                  <template #default="scope">
                    <el-input-number v-model="scope.row.minScore" class="!w-full" :min="0" :precision="0" />
                  </template>
                </el-table-column>
                <el-table-column label="最大分值(含)" align="center" min-width="150">
                  <template #default="scope">
                    <el-input-number v-model="scope.row.maxScore" class="!w-full" :min="0" :precision="0" />
                  </template>
                </el-table-column>
                <el-table-column label="命中动作" align="center" min-width="180">
                  <template #default="scope">
                    <el-select v-model="scope.row.hitAction" class="w-full" placeholder="请选择动作">
                      <el-option
                        v-for="item in riskPolicyDefaultActionOptions"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                      />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="命中原因模板" align="center" min-width="320">
                  <template #default="scope">
                    <el-input
                      v-model="scope.row.hitReasonTemplate"
                      placeholder="可引用 {totalScore}"
                      maxlength="1024"
                      show-word-limit
                    />
                  </template>
                </el-table-column>
                <el-table-column label="操作" align="center" width="180" fixed="right">
                  <template #default="scope">
                    <el-button link type="primary" :disabled="scope.$index === 0" @click="moveScoreBandUp(scope.$index)">上移</el-button>
                    <el-button
                      link
                      type="primary"
                      :disabled="scope.$index === (formData.scoreBands?.length || 0) - 1"
                      @click="moveScoreBandDown(scope.$index)"
                    >
                      下移
                    </el-button>
                    <el-button link type="danger" @click="removeScoreBand(scope.$index)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="!(formData.scoreBands || []).length" class="mt-8px text-12px text-#909399">请至少配置一个评分段，例如 0~59=PASS、60~119=REVIEW、120+=REJECT。</div>
            </div>
          </el-form-item>
        </el-col>
        <el-col v-if="isScoreCard" :span="24">
          <el-form-item label="总分预览">
            <div class="w-full rounded-4px border border-solid border-#ebeef5 p-16px">
              <div class="flex items-center gap-12px">
                <el-input-number v-model="previewTotalScore" :min="0" :precision="0" placeholder="请输入总分" />
                <el-button type="primary" :loading="previewLoading" @click="handlePreviewScore">预览动作</el-button>
              </div>
              <el-descriptions v-if="previewResult" :column="2" border class="mt-12px">
                <el-descriptions-item label="最终动作">
                  <el-tag :type="getRiskPolicyDefaultActionTag(previewResult.finalAction)" effect="plain">
                    {{ getRiskPolicyDefaultActionLabel(previewResult.finalAction) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="是否命中分段">
                  {{ previewResult.matched ? '是' : '否' }}
                </el-descriptions-item>
                <el-descriptions-item label="命中区间" :span="2">
                  {{
                    previewResult.matched
                      ? `BAND_${previewResult.matchedBandNo} / ${previewResult.matchedMinScore} ~ ${previewResult.matchedMaxScore}`
                      : '未命中评分段，返回默认动作'
                  }}
                </el-descriptions-item>
                <el-descriptions-item label="预览原因" :span="2">
                  <div class="whitespace-pre-wrap leading-22px">{{ previewResult.reason || '-' }}</div>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="策略说明" prop="description">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="4"
              :placeholder="isScoreCard ? '建议写清楚评分段含义、总分映射动作及默认动作兜底逻辑' : '建议写清楚默认动作、规则顺序意图和 FIRST_HIT 收敛逻辑'"
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
  riskPolicyDecisionModeOptions,
  riskPolicyDefaultActionOptions
} from './constants'

defineOptions({ name: 'RiskPolicyForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const previewLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const ruleOptions = ref<PolicyApi.PolicyRuleOptionVO[]>([])
const selectedRuleCode = ref<string>()
const previewTotalScore = ref<number>()
const previewResult = ref<PolicyApi.PolicyScorePreviewRespVO>()

const createDefaultFormData = (): PolicyApi.PolicyVO => ({
  id: undefined,
  sceneCode: 'TRADE_RISK',
  policyCode: 'TRADE_RISK_POLICY',
  policyName: '交易风控主策略',
  decisionMode: 'FIRST_HIT',
  defaultAction: 'PASS',
  scoreCalcMode: 'NONE',
  ruleCodes: ['R001', 'R003', 'R002'],
  ruleRefs: [],
  scoreBands: [],
  status: CommonStatusEnum.ENABLE,
  description: ''
})

const formData = ref<PolicyApi.PolicyVO>(createDefaultFormData())
const formRef = ref()

const isScoreCard = computed(() => formData.value.decisionMode === 'SCORE_CARD')

const ruleOptionsMap = computed(() => {
  return new Map(ruleOptions.value.map((item) => [item.ruleCode, item]))
})

const selectedRuleRows = computed(() => {
  return formData.value.ruleCodes.map((ruleCode) => {
    const option = ruleOptionsMap.value.get(ruleCode)
    return (
      option || {
        ruleCode,
        ruleName: ruleCode,
        hitAction: 'TAG_ONLY',
        priority: 0,
        riskScore: 0,
        status: CommonStatusEnum.ENABLE
      }
    )
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
  decisionMode: [{ required: true, message: '决策模式不能为空', trigger: 'change' }],
  defaultAction: [{ required: true, message: '默认动作不能为空', trigger: 'change' }],
  ruleCodes: [{ type: 'array', required: true, message: '规则顺序不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const cloneScoreBands = (scoreBands?: PolicyApi.PolicyScoreBandVO[]) => {
  return (scoreBands || []).map((item) => ({
    bandNo: item.bandNo,
    minScore: item.minScore,
    maxScore: item.maxScore,
    hitAction: item.hitAction || 'PASS',
    hitReasonTemplate: item.hitReasonTemplate || ''
  }))
}

const resetPreview = () => {
  previewTotalScore.value = undefined
  previewResult.value = undefined
}

const resetForm = () => {
  formData.value = createDefaultFormData()
  selectedRuleCode.value = undefined
  ruleOptions.value = []
  resetPreview()
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

watch(
  () => formData.value.decisionMode,
  (value, oldValue) => {
    if (!dialogVisible.value || value === oldValue) {
      return
    }
    resetPreview()
    if (value === 'SCORE_CARD' && !(formData.value.scoreBands || []).length) {
      addScoreBand()
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

const addScoreBand = () => {
  formData.value.scoreBands = [
    ...(formData.value.scoreBands || []),
    { minScore: undefined, maxScore: undefined, hitAction: 'PASS', hitReasonTemplate: '' }
  ]
}

const moveScoreBandUp = (index: number) => {
  if (index < 1) {
    return
  }
  const list = [...(formData.value.scoreBands || [])]
  ;[list[index - 1], list[index]] = [list[index], list[index - 1]]
  formData.value.scoreBands = list
}

const moveScoreBandDown = (index: number) => {
  const scoreBands = formData.value.scoreBands || []
  if (index >= scoreBands.length - 1) {
    return
  }
  const list = [...scoreBands]
  ;[list[index], list[index + 1]] = [list[index + 1], list[index]]
  formData.value.scoreBands = list
}

const removeScoreBand = (index: number) => {
  formData.value.scoreBands = (formData.value.scoreBands || []).filter((_, currentIndex) => currentIndex !== index)
}

const validateScoreBands = (showMessage = true) => {
  if (!isScoreCard.value) {
    return true
  }
  const scoreBands = formData.value.scoreBands || []
  if (!scoreBands.length) {
    showMessage && message.error('SCORE_CARD 至少需要配置一个评分段')
    return false
  }
  for (let index = 0; index < scoreBands.length; index++) {
    const item = scoreBands[index]
    if (item.minScore === undefined || item.maxScore === undefined) {
      showMessage && message.error(`第 ${index + 1} 个评分段缺少分值范围`)
      return false
    }
    if (item.minScore > item.maxScore) {
      showMessage && message.error(`第 ${index + 1} 个评分段最小分值不能大于最大分值`)
      return false
    }
    if (!item.hitAction) {
      showMessage && message.error(`第 ${index + 1} 个评分段缺少命中动作`)
      return false
    }
  }
  const sortedBands = [...scoreBands].sort((left, right) => {
    if ((left.minScore ?? 0) !== (right.minScore ?? 0)) {
      return (left.minScore ?? 0) - (right.minScore ?? 0)
    }
    return (left.maxScore ?? 0) - (right.maxScore ?? 0)
  })
  for (let index = 1; index < sortedBands.length; index++) {
    const previous = sortedBands[index - 1]
    const current = sortedBands[index]
    if ((current.minScore ?? 0) <= (previous.maxScore ?? 0)) {
      showMessage && message.error('评分段区间存在重叠，请调整后再保存')
      return false
    }
  }
  return true
}

const handlePreviewScore = async () => {
  if (!isScoreCard.value) {
    return
  }
  if (previewTotalScore.value === undefined) {
    message.error('请输入要预览的总分')
    return
  }
  if (!validateScoreBands()) {
    return
  }
  previewLoading.value = true
  try {
    previewResult.value = await PolicyApi.previewScoreCard({
      decisionMode: formData.value.decisionMode || 'SCORE_CARD',
      defaultAction: formData.value.defaultAction,
      totalScore: previewTotalScore.value,
      scoreBands: (formData.value.scoreBands || []).map((item) => ({
        minScore: item.minScore,
        maxScore: item.maxScore,
        hitAction: item.hitAction,
        hitReasonTemplate: item.hitReasonTemplate?.trim() || undefined
      }))
    })
  } finally {
    previewLoading.value = false
  }
}

const open = async (type: 'create' | 'update', id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      const data = await PolicyApi.getPolicy(id)
      formData.value = {
        ...createDefaultFormData(),
        ...data,
        ruleCodes: [...(data.ruleCodes || [])],
        ruleRefs: [...(data.ruleRefs || [])],
        scoreBands: cloneScoreBands(data.scoreBands)
      }
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
  if (!validateScoreBands()) {
    return
  }

  formLoading.value = true
  try {
    const data: PolicyApi.PolicyVO = {
      ...formData.value,
      sceneCode: formData.value.sceneCode.trim(),
      policyCode: formData.value.policyCode.trim(),
      policyName: formData.value.policyName.trim(),
      decisionMode: formData.value.decisionMode || 'FIRST_HIT',
      ruleCodes: [...formData.value.ruleCodes],
      scoreBands: isScoreCard.value
        ? cloneScoreBands(formData.value.scoreBands).map((item) => ({
            minScore: item.minScore,
            maxScore: item.maxScore,
            hitAction: item.hitAction,
            hitReasonTemplate: item.hitReasonTemplate?.trim() || undefined
          }))
        : [],
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
