<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :model="queryParams"
      :inline="true"
      label-width="92px"
    >
      <el-form-item label="所属场景" prop="sceneCode">
        <el-input
          v-model="queryParams.sceneCode"
          class="!w-220px"
          clearable
          placeholder="请输入场景编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="规则编码" prop="ruleCode">
        <el-input
          v-model="queryParams.ruleCode"
          class="!w-220px"
          clearable
          placeholder="请输入规则编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="规则名称" prop="ruleName">
        <el-input
          v-model="queryParams.ruleName"
          class="!w-220px"
          clearable
          placeholder="请输入规则名称"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="命中动作" prop="hitAction">
        <el-select v-model="queryParams.hitAction" class="!w-220px" clearable placeholder="请选择命中动作">
          <el-option
            v-for="item in riskRuleHitActionOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-220px" clearable placeholder="请选择状态">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['risk:rule:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      title="当前阶段先维护单条规则的条件表达式、优先级、动作和命中原因；策略排序与 FIRST_HIT 收敛放到 S12。"
      type="info"
      :closable="false"
      class="mb-16px"
    />

    <el-table v-loading="loading" :data="list">
      <el-table-column label="所属场景" align="center" prop="sceneCode" min-width="120" />
      <el-table-column label="规则编码" align="center" prop="ruleCode" min-width="120" />
      <el-table-column label="规则名称" align="center" prop="ruleName" min-width="180" />
      <el-table-column label="规则类型" align="center" min-width="170">
        <template #default="scope">{{ getRiskRuleTypeLabel(scope.row.ruleType) }}</template>
      </el-table-column>
      <el-table-column label="表达式引擎" align="center" min-width="160">
        <template #default="scope">
          <el-tag effect="plain" type="warning">{{ getRiskRuleEngineTypeLabel(scope.row.engineType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="命中动作" align="center" min-width="150">
        <template #default="scope">
          <el-tag :type="getRiskRuleHitActionTag(scope.row.hitAction)" effect="plain">
            {{ getRiskRuleHitActionLabel(scope.row.hitAction) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="优先级/分值" align="center" min-width="160">
        <template #default="scope">{{ formatRiskRuleScore(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="规则表达式" align="center" prop="exprContent" min-width="260" show-overflow-tooltip />
      <el-table-column label="命中原因模板" align="center" prop="hitReasonTemplate" min-width="240" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.hitReasonTemplate || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="版本" align="center" prop="version" width="90" />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id!)" v-hasPermi="['risk:rule:query']">
            详情
          </el-button>
          <el-button link type="primary" @click="openForm('update', scope.row.id!)" v-hasPermi="['risk:rule:update']">
            编辑
          </el-button>
          <el-button link type="danger" @click="handleDelete(scope.row)" v-hasPermi="['risk:rule:delete']">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <RuleForm ref="formRef" @success="getList" />
  <RuleDetail ref="detailRef" />
</template>

<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as RuleApi from '@/api/risk/rule'
import RuleDetail from './RuleDetail.vue'
import RuleForm from './RuleForm.vue'
import {
  formatRiskRuleScore,
  getRiskRuleEngineTypeLabel,
  getRiskRuleHitActionLabel,
  getRiskRuleHitActionTag,
  getRiskRuleTypeLabel,
  riskRuleHitActionOptions
} from './constants'

defineOptions({ name: 'RiskRule' })

const { t } = useI18n()
const message = useMessage()

const createDefaultQueryParams = (): RuleApi.RulePageReqVO => ({
  pageNo: 1,
  pageSize: 10,
  sceneCode: 'TRADE_RISK',
  ruleCode: undefined,
  ruleName: undefined,
  hitAction: undefined,
  status: undefined
})

const loading = ref(true)
const total = ref(0)
const list = ref<RuleApi.RuleVO[]>([])
const queryParams = reactive<RuleApi.RulePageReqVO>(createDefaultQueryParams())
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await RuleApi.getRulePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  Object.assign(queryParams, createDefaultQueryParams())
  handleQuery()
}

const formRef = ref()
const openForm = (type: 'create' | 'update', id?: number) => {
  formRef.value.open(type, id)
}

const detailRef = ref()
const openDetail = (id: number) => {
  detailRef.value.open(id)
}

const handleDelete = async (row: RuleApi.RuleVO) => {
  try {
    await message.delConfirm(`确认删除规则「${row.ruleName}」吗？`)
    await RuleApi.deleteRule(row.id!)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>
