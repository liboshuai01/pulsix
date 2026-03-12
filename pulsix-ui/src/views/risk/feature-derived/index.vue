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
      <el-form-item label="特征编码" prop="featureCode">
        <el-input
          v-model="queryParams.featureCode"
          class="!w-220px"
          clearable
          placeholder="请输入特征编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="特征名称" prop="featureName">
        <el-input
          v-model="queryParams.featureName"
          class="!w-220px"
          clearable
          placeholder="请输入特征名称"
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
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['risk:feature-derived:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      title="当前阶段只做依赖选择、表达式校验和结果类型维护；支持 Aviator / Groovy，且 Groovy 固定启用脚本沙箱。"
      type="info"
      :closable="false"
      class="mb-16px"
    />

    <el-table v-loading="loading" :data="list">
      <el-table-column label="所属场景" align="center" prop="sceneCode" min-width="120" />
      <el-table-column label="特征编码" align="center" prop="featureCode" min-width="180" />
      <el-table-column label="特征名称" align="center" prop="featureName" min-width="180" />
      <el-table-column label="表达式引擎" align="center" min-width="160">
        <template #default="scope">
          <el-tag effect="plain" type="warning">{{ getRiskFeatureDerivedEngineTypeLabel(scope.row.engineType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="依赖项" align="center" min-width="220" show-overflow-tooltip>
        <template #default="scope">
          {{ formatRiskFeatureDerivedDependsOn(scope.row.dependsOnJson) }}
        </template>
      </el-table-column>
      <el-table-column label="表达式内容" align="center" prop="exprContent" min-width="260" show-overflow-tooltip />
      <el-table-column label="结果类型" align="center" min-width="140">
        <template #default="scope">
          {{ getRiskFeatureDerivedValueTypeLabel(scope.row.valueType) }}
        </template>
      </el-table-column>
      <el-table-column label="超时/沙箱" align="center" min-width="160">
        <template #default="scope">{{ formatRiskFeatureDerivedTimeout(scope.row) }}</template>
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
          <el-button link type="primary" @click="openDetail(scope.row.id!)" v-hasPermi="['risk:feature-derived:query']">
            详情
          </el-button>
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id!)"
            v-hasPermi="['risk:feature-derived:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row)"
            v-hasPermi="['risk:feature-derived:delete']"
          >
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

  <FeatureDerivedForm ref="formRef" @success="getList" />
  <FeatureDerivedDetail ref="detailRef" />
</template>

<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as FeatureDerivedApi from '@/api/risk/featureDerived'
import FeatureDerivedDetail from './FeatureDerivedDetail.vue'
import FeatureDerivedForm from './FeatureDerivedForm.vue'
import {
  formatRiskFeatureDerivedDependsOn,
  formatRiskFeatureDerivedTimeout,
  getRiskFeatureDerivedEngineTypeLabel,
  getRiskFeatureDerivedValueTypeLabel
} from './constants'

defineOptions({ name: 'RiskFeatureDerived' })

const { t } = useI18n()
const message = useMessage()

const createDefaultQueryParams = (): FeatureDerivedApi.FeatureDerivedPageReqVO => ({
  pageNo: 1,
  pageSize: 10,
  sceneCode: 'TRADE_RISK',
  featureCode: undefined,
  featureName: undefined,
  status: undefined
})

const loading = ref(true)
const total = ref(0)
const list = ref<FeatureDerivedApi.FeatureDerivedVO[]>([])
const queryParams = reactive<FeatureDerivedApi.FeatureDerivedPageReqVO>(createDefaultQueryParams())
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await FeatureDerivedApi.getFeatureDerivedPage(queryParams)
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

const handleDelete = async (row: FeatureDerivedApi.FeatureDerivedVO) => {
  try {
    await message.delConfirm(`确认删除派生特征「${row.featureName}」吗？`)
    await FeatureDerivedApi.deleteFeatureDerived(row.id!)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>
