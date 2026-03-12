<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :model="queryParams"
      :inline="true"
      label-width="82px"
    >
      <el-form-item label="接入源" prop="sourceCode">
        <el-input
          v-model="queryParams.sourceCode"
          class="!w-220px"
          clearable
          placeholder="请输入接入源编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="所属场景" prop="sceneCode">
        <el-input
          v-model="queryParams.sceneCode"
          class="!w-220px"
          clearable
          placeholder="请输入场景编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="所属事件" prop="eventCode">
        <el-input
          v-model="queryParams.eventCode"
          class="!w-220px"
          clearable
          placeholder="请输入事件编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="目标字段" prop="targetFieldCode">
        <el-input
          v-model="queryParams.targetFieldCode"
          class="!w-220px"
          clearable
          placeholder="请输入目标字段编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="转换类型" prop="transformType">
        <el-select v-model="queryParams.transformType" class="!w-220px" clearable placeholder="请选择转换类型">
          <el-option
            v-for="item in riskIngestMappingTransformTypeOptions"
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
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['risk:ingest-mapping:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button type="primary" plain @click="openPreview()" v-hasPermi="['risk:ingest-mapping:preview']">
          <Icon icon="ep:view" class="mr-5px" /> 预览
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="接入源" align="center" prop="sourceCode" min-width="140" />
      <el-table-column label="场景" align="center" prop="sceneCode" min-width="120" />
      <el-table-column label="事件" align="center" prop="eventCode" min-width="120" />
      <el-table-column label="目标字段" align="center" min-width="180">
        <template #default="scope">
          {{ scope.row.targetFieldName }} ({{ scope.row.targetFieldCode }})
        </template>
      </el-table-column>
      <el-table-column label="原始字段路径" align="center" prop="sourceFieldPath" min-width="180" show-overflow-tooltip />
      <el-table-column label="转换类型" align="center" min-width="150">
        <template #default="scope">
          <el-tag effect="plain">{{ getRiskIngestMappingTransformTypeLabel(scope.row.transformType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="默认值" align="center" prop="defaultValue" min-width="140" show-overflow-tooltip />
      <el-table-column label="清洗规则" align="center" min-width="150" show-overflow-tooltip>
        <template #default="scope">
          {{ summarizeCleanRule(scope.row.cleanRuleJson) }}
        </template>
      </el-table-column>
      <el-table-column label="必填" align="center" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.requiredFlag === 1 ? 'danger' : 'info'" effect="plain">
            {{ getFlagLabel(scope.row.requiredFlag) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="排序号" align="center" prop="sortNo" width="90" />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openPreview(scope.row)" v-hasPermi="['risk:ingest-mapping:preview']">
            预览
          </el-button>
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['risk:ingest-mapping:update']">
            编辑
          </el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['risk:ingest-mapping:delete']">
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

  <IngestMappingForm ref="formRef" @success="getList" />
  <IngestMappingPreview ref="previewRef" />
</template>

<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as IngestMappingApi from '@/api/risk/ingestMapping'
import IngestMappingForm from './IngestMappingForm.vue'
import IngestMappingPreview from './IngestMappingPreview.vue'
import {
  getFlagLabel,
  getRiskIngestMappingTransformTypeLabel,
  riskIngestMappingTransformTypeOptions,
  summarizeCleanRule
} from './constants'

defineOptions({ name: 'RiskIngestMapping' })

const message = useMessage()
const { t } = useI18n()

const createDefaultQueryParams = (): IngestMappingApi.IngestMappingPageReqVO => ({
  pageNo: 1,
  pageSize: 10,
  sourceCode: 'trade_http_demo',
  sceneCode: 'TRADE_RISK',
  eventCode: 'TRADE_EVENT',
  targetFieldCode: undefined,
  transformType: undefined,
  status: undefined
})

const loading = ref(true)
const total = ref(0)
const list = ref<IngestMappingApi.IngestMappingVO[]>([])
const queryParams = reactive<IngestMappingApi.IngestMappingPageReqVO>(createDefaultQueryParams())
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await IngestMappingApi.getIngestMappingPage(queryParams)
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

const previewRef = ref()
const openPreview = (row?: Partial<IngestMappingApi.IngestMappingVO>) => {
  previewRef.value.open({
    sourceCode: row?.sourceCode || queryParams.sourceCode || 'trade_http_demo',
    sceneCode: row?.sceneCode || queryParams.sceneCode || 'TRADE_RISK',
    eventCode: row?.eventCode || queryParams.eventCode || 'TRADE_EVENT'
  })
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await IngestMappingApi.deleteIngestMapping(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>
