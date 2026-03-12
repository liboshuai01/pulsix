<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :model="queryParams"
      :inline="true"
      label-width="82px"
    >
      <el-form-item label="来源编码" prop="sourceCode">
        <el-input
          v-model="queryParams.sourceCode"
          class="!w-220px"
          clearable
          placeholder="请输入来源编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="来源名称" prop="sourceName">
        <el-input
          v-model="queryParams.sourceName"
          class="!w-220px"
          clearable
          placeholder="请输入来源名称"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="接入方式" prop="sourceType">
        <el-select v-model="queryParams.sourceType" class="!w-220px" clearable placeholder="请选择接入方式">
          <el-option
            v-for="item in riskIngestSourceTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="鉴权方式" prop="authType">
        <el-select v-model="queryParams.authType" class="!w-220px" clearable placeholder="请选择鉴权方式">
          <el-option
            v-for="item in riskIngestSourceAuthTypeOptions"
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
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['risk:ingest-source:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="来源编码" align="center" prop="sourceCode" min-width="160" />
      <el-table-column label="来源名称" align="center" prop="sourceName" min-width="180" />
      <el-table-column label="接入方式" align="center" min-width="100">
        <template #default="scope">
          <el-tag effect="plain">{{ getRiskIngestSourceTypeLabel(scope.row.sourceType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="鉴权方式" align="center" min-width="110">
        <template #default="scope">
          <el-tag type="warning" effect="plain">{{ getRiskIngestSourceAuthTypeLabel(scope.row.authType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标准 Topic" align="center" prop="standardTopicName" min-width="170" show-overflow-tooltip />
      <el-table-column label="异常 Topic" align="center" prop="errorTopicName" min-width="150" show-overflow-tooltip />
      <el-table-column label="限流 QPS" align="center" prop="rateLimitQps" width="100" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="280" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)" v-hasPermi="['risk:ingest-source:get']">
            详情
          </el-button>
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['risk:ingest-source:update']">
            编辑
          </el-button>
          <el-button
            link
            type="primary"
            @click="handleUpdateStatus(scope.row)"
            v-hasPermi="['risk:ingest-source:update-status']"
          >
            {{ scope.row.status === CommonStatusEnum.ENABLE ? '停用' : '启用' }}
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

  <IngestSourceForm ref="formRef" @success="getList" />
  <IngestSourceDetail ref="detailRef" />
</template>

<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { CommonStatusEnum } from '@/utils/constants'
import { dateFormatter } from '@/utils/formatTime'
import * as IngestSourceApi from '@/api/risk/ingestSource'
import IngestSourceForm from './IngestSourceForm.vue'
import IngestSourceDetail from './IngestSourceDetail.vue'
import {
  getRiskIngestSourceAuthTypeLabel,
  getRiskIngestSourceTypeLabel,
  riskIngestSourceAuthTypeOptions,
  riskIngestSourceTypeOptions
} from './constants'

defineOptions({ name: 'RiskIngestSource' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const total = ref(0)
const list = ref<IngestSourceApi.IngestSourceVO[]>([])
const queryParams = reactive<IngestSourceApi.IngestSourcePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  sourceCode: undefined,
  sourceName: undefined,
  sourceType: undefined,
  authType: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await IngestSourceApi.getIngestSourcePage(queryParams)
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

const handleUpdateStatus = async (row: IngestSourceApi.IngestSourceVO) => {
  const nextStatus = row.status === CommonStatusEnum.ENABLE ? CommonStatusEnum.DISABLE : CommonStatusEnum.ENABLE
  const actionText = nextStatus === CommonStatusEnum.ENABLE ? '启用' : '停用'
  try {
    await message.confirm(`确认${actionText}接入源「${row.sourceName}」吗？`)
    await IngestSourceApi.updateIngestSourceStatus(row.id!, nextStatus)
    message.success(t('common.updateSuccess'))
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>
