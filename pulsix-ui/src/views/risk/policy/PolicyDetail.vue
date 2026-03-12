<template>
  <Dialog v-model="dialogVisible" title="策略详情" width="980px">
    <el-descriptions v-loading="detailLoading" :column="2" border>
      <el-descriptions-item label="主键编号">{{ detailData.id }}</el-descriptions-item>
      <el-descriptions-item label="所属场景">{{ detailData.sceneCode }}</el-descriptions-item>
      <el-descriptions-item label="策略编码">{{ detailData.policyCode }}</el-descriptions-item>
      <el-descriptions-item label="策略名称">{{ detailData.policyName }}</el-descriptions-item>
      <el-descriptions-item label="决策模式">
        {{ getRiskPolicyDecisionModeLabel(detailData.decisionMode) }}
      </el-descriptions-item>
      <el-descriptions-item label="默认动作">
        <el-tag :type="getRiskPolicyDefaultActionTag(detailData.defaultAction)" effect="plain">
          {{ getRiskPolicyDefaultActionLabel(detailData.defaultAction) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="规则数量">{{ formatRiskPolicyRuleCount(detailData) }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="detailData.status" />
      </el-descriptions-item>
      <el-descriptions-item label="设计态版本">{{ detailData.version ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="规则顺序" :span="2">
        {{ formatRiskPolicyRuleOrder(detailData.ruleRefs) }}
      </el-descriptions-item>
      <el-descriptions-item label="策略说明" :span="2">
        <div class="whitespace-pre-wrap leading-22px">{{ detailData.description || '-' }}</div>
      </el-descriptions-item>
      <el-descriptions-item label="创建者">{{ detailData.creator || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">
        {{ detailData.createTime ? formatDate(detailData.createTime) : '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="更新者">{{ detailData.updater || '-' }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">
        {{ detailData.updateTime ? formatDate(detailData.updateTime) : '-' }}
      </el-descriptions-item>
    </el-descriptions>

    <ContentWrap class="mt-16px" v-if="detailData.ruleRefs?.length">
      <el-table :data="detailData.ruleRefs">
        <el-table-column label="顺序" align="center" prop="orderNo" width="90" />
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
      </el-table>
    </ContentWrap>
  </Dialog>
</template>

<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as PolicyApi from '@/api/risk/policy'
import {
  formatRiskPolicyRuleCount,
  formatRiskPolicyRuleOrder,
  getRiskPolicyDecisionModeLabel,
  getRiskPolicyDefaultActionLabel,
  getRiskPolicyDefaultActionTag
} from './constants'

defineOptions({ name: 'RiskPolicyDetail' })

const dialogVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<PolicyApi.PolicyVO>({
  id: undefined,
  sceneCode: '',
  policyCode: '',
  policyName: '',
  decisionMode: 'FIRST_HIT',
  defaultAction: 'PASS',
  ruleCodes: [],
  ruleRefs: [],
  status: 0
})

const open = async (id: number) => {
  dialogVisible.value = true
  detailLoading.value = true
  try {
    detailData.value = await PolicyApi.getPolicy(id)
  } finally {
    detailLoading.value = false
  }
}
defineExpose({ open })
</script>
