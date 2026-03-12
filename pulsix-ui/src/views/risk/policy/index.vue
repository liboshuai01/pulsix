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
      <el-form-item label="策略编码" prop="policyCode">
        <el-input
          v-model="queryParams.policyCode"
          class="!w-220px"
          clearable
          placeholder="请输入策略编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="策略名称" prop="policyName">
        <el-input
          v-model="queryParams.policyName"
          class="!w-220px"
          clearable
          placeholder="请输入策略名称"
          @keyup.enter="handleQuery"
        />
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
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['risk:policy:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      title="当前阶段只做 FIRST_HIT 策略；页面支持维护默认动作，并对规则执行顺序进行保存。"
      type="info"
      :closable="false"
      class="mb-16px"
    />

    <el-table v-loading="loading" :data="list">
      <el-table-column label="所属场景" align="center" prop="sceneCode" min-width="120" />
      <el-table-column label="策略编码" align="center" prop="policyCode" min-width="160" />
      <el-table-column label="策略名称" align="center" prop="policyName" min-width="180" />
      <el-table-column label="决策模式" align="center" min-width="170">
        <template #default="scope">{{ getRiskPolicyDecisionModeLabel(scope.row.decisionMode) }}</template>
      </el-table-column>
      <el-table-column label="默认动作" align="center" min-width="150">
        <template #default="scope">
          <el-tag :type="getRiskPolicyDefaultActionTag(scope.row.defaultAction)" effect="plain">
            {{ getRiskPolicyDefaultActionLabel(scope.row.defaultAction) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="规则数量" align="center" min-width="100">
        <template #default="scope">{{ formatRiskPolicyRuleCount(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="规则顺序" align="center" min-width="280" show-overflow-tooltip>
        <template #default="scope">
          {{ formatRiskPolicyRuleOrder(scope.row.ruleRefs) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="版本" align="center" prop="version" width="90" />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="280" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id!)" v-hasPermi="['risk:policy:query']">
            详情
          </el-button>
          <el-button link type="primary" @click="openForm('update', scope.row.id!)" v-hasPermi="['risk:policy:update']">
            编辑
          </el-button>
          <el-button link type="primary" @click="openSort(scope.row.id!)" v-hasPermi="['risk:policy:sort']">
            排序
          </el-button>
          <el-button link type="danger" @click="handleDelete(scope.row)" v-hasPermi="['risk:policy:delete']">
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

  <PolicyForm ref="formRef" @success="getList" />
  <PolicyDetail ref="detailRef" />
  <PolicySortDialog ref="sortRef" @success="getList" />
</template>

<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as PolicyApi from '@/api/risk/policy'
import PolicyDetail from './PolicyDetail.vue'
import PolicyForm from './PolicyForm.vue'
import PolicySortDialog from './PolicySortDialog.vue'
import {
  formatRiskPolicyRuleCount,
  formatRiskPolicyRuleOrder,
  getRiskPolicyDecisionModeLabel,
  getRiskPolicyDefaultActionLabel,
  getRiskPolicyDefaultActionTag
} from './constants'

defineOptions({ name: 'RiskPolicy' })

const { t } = useI18n()
const message = useMessage()

const createDefaultQueryParams = (): PolicyApi.PolicyPageReqVO => ({
  pageNo: 1,
  pageSize: 10,
  sceneCode: 'TRADE_RISK',
  policyCode: undefined,
  policyName: undefined,
  status: undefined
})

const loading = ref(true)
const total = ref(0)
const list = ref<PolicyApi.PolicyVO[]>([])
const queryParams = reactive<PolicyApi.PolicyPageReqVO>(createDefaultQueryParams())
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await PolicyApi.getPolicyPage(queryParams)
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

const sortRef = ref()
const openSort = (id: number) => {
  sortRef.value.open(id)
}

const handleDelete = async (row: PolicyApi.PolicyVO) => {
  try {
    await message.delConfirm(`确认删除策略「${row.policyName}」吗？`)
    await PolicyApi.deletePolicy(row.id!)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>
