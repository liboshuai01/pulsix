<template>
  <Dialog v-model="dialogVisible" title="策略规则排序" width="880px">
    <el-alert
      title="FIRST_HIT 按规则顺序收敛；SCORE_CARD 也保留规则顺序，便于还原命中链路与累计得分来源。"
      type="info"
      :closable="false"
      class="mb-16px"
    />

    <el-table v-loading="dialogLoading" :data="ruleRefs">
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
      <el-table-column label="操作" align="center" width="160" fixed="right">
        <template #default="scope">
          <el-button link type="primary" :disabled="scope.$index === 0" @click="moveUp(scope.$index)">上移</el-button>
          <el-button
            link
            type="primary"
            :disabled="scope.$index === ruleRefs.length - 1"
            @click="moveDown(scope.$index)"
          >
            下移
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button :disabled="dialogLoading" @click="dialogVisible = false">取 消</el-button>
      <el-button type="primary" :loading="dialogLoading" @click="submitForm">保 存</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import * as PolicyApi from '@/api/risk/policy'
import { getRiskPolicyDefaultActionLabel, getRiskPolicyDefaultActionTag } from './constants'

defineOptions({ name: 'RiskPolicySortDialog' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogLoading = ref(false)
const policyId = ref<number>()
const ruleRefs = ref<PolicyApi.PolicyRuleRefVO[]>([])

const moveUp = (index: number) => {
  if (index < 1) {
    return
  }
  const list = [...ruleRefs.value]
  ;[list[index - 1], list[index]] = [list[index], list[index - 1]]
  ruleRefs.value = list
}

const moveDown = (index: number) => {
  if (index >= ruleRefs.value.length - 1) {
    return
  }
  const list = [...ruleRefs.value]
  ;[list[index], list[index + 1]] = [list[index + 1], list[index]]
  ruleRefs.value = list
}

const open = async (id: number) => {
  dialogVisible.value = true
  dialogLoading.value = true
  policyId.value = id
  try {
    const data = await PolicyApi.getPolicy(id)
    ruleRefs.value = [...(data.ruleRefs || [])]
  } finally {
    dialogLoading.value = false
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])

const submitForm = async () => {
  if (!policyId.value) {
    return
  }
  dialogLoading.value = true
  try {
    await PolicyApi.sortPolicyRules({
      id: policyId.value,
      ruleCodes: ruleRefs.value.map((item) => item.ruleCode)
    })
    message.success(t('common.updateSuccess'))
    dialogVisible.value = false
    emit('success')
  } finally {
    dialogLoading.value = false
  }
}
</script>
