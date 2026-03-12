<template>
  <Dialog v-model="dialogVisible" title="策略详情" width="1080px">
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
      <el-descriptions-item label="分值模式">
        {{ getRiskPolicyScoreCalcModeLabel(detailData.scoreCalcMode) }}
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
        <el-table-column label="规则分值" align="center" prop="riskScore" width="100" />
        <el-table-column label="分值权重" align="center" min-width="110">
          <template #default="scope">{{ scope.row.scoreWeight ?? 1 }}</template>
        </el-table-column>
        <el-table-column label="命中即停" align="center" min-width="100">
          <template #default="scope">{{ scope.row.stopOnHit === 0 ? '否' : '是' }}</template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template #default="scope">
            <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
          </template>
        </el-table-column>
      </el-table>
    </ContentWrap>

    <ContentWrap class="mt-16px" v-if="detailData.scoreBands?.length">
      <template #header>
        <span>评分段</span>
      </template>
      <el-table :data="detailData.scoreBands">
        <el-table-column label="顺序" align="center" prop="bandNo" width="100" />
        <el-table-column label="最小分值(含)" align="center" prop="minScore" width="130" />
        <el-table-column label="最大分值(含)" align="center" prop="maxScore" width="130" />
        <el-table-column label="命中动作" align="center" min-width="150">
          <template #default="scope">
            <el-tag :type="getRiskPolicyDefaultActionTag(scope.row.hitAction)" effect="plain">
              {{ getRiskPolicyDefaultActionLabel(scope.row.hitAction) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="命中原因模板" align="center" prop="hitReasonTemplate" min-width="320" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.hitReasonTemplate || '-' }}</template>
        </el-table-column>
      </el-table>
    </ContentWrap>

    <ContentWrap class="mt-16px" v-if="detailData.decisionMode === 'SCORE_CARD' && detailData.scoreBands?.length">
      <template #header>
        <span>总分预览</span>
      </template>
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
        <el-descriptions-item label="是否命中分段">{{ previewResult.matched ? '是' : '否' }}</el-descriptions-item>
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
  getRiskPolicyDefaultActionTag,
  getRiskPolicyScoreCalcModeLabel
} from './constants'

defineOptions({ name: 'RiskPolicyDetail' })

const message = useMessage()
const dialogVisible = ref(false)
const detailLoading = ref(false)
const previewLoading = ref(false)
const previewTotalScore = ref<number>()
const previewResult = ref<PolicyApi.PolicyScorePreviewRespVO>()
const detailData = ref<PolicyApi.PolicyVO>({
  id: undefined,
  sceneCode: '',
  policyCode: '',
  policyName: '',
  decisionMode: 'FIRST_HIT',
  defaultAction: 'PASS',
  scoreCalcMode: 'NONE',
  ruleCodes: [],
  ruleRefs: [],
  scoreBands: [],
  status: 0
})

const open = async (id: number) => {
  dialogVisible.value = true
  detailLoading.value = true
  previewTotalScore.value = undefined
  previewResult.value = undefined
  try {
    detailData.value = await PolicyApi.getPolicy(id)
  } finally {
    detailLoading.value = false
  }
}
defineExpose({ open })

const handlePreviewScore = async () => {
  if (previewTotalScore.value === undefined) {
    message.error('请输入要预览的总分')
    return
  }
  previewLoading.value = true
  try {
    previewResult.value = await PolicyApi.previewScoreCard({
      decisionMode: detailData.value.decisionMode || 'SCORE_CARD',
      defaultAction: detailData.value.defaultAction,
      totalScore: previewTotalScore.value,
      scoreBands: (detailData.value.scoreBands || []).map((item) => ({
        minScore: item.minScore,
        maxScore: item.maxScore,
        hitAction: item.hitAction,
        hitReasonTemplate: item.hitReasonTemplate
      }))
    })
  } finally {
    previewLoading.value = false
  }
}
</script>
