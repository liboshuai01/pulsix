<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :model="queryParams"
      :inline="true"
      label-width="82px"
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
      <el-form-item label="所属事件" prop="eventCode">
        <el-input
          v-model="queryParams.eventCode"
          class="!w-220px"
          clearable
          placeholder="请输入事件编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="样例编码" prop="sampleCode">
        <el-input
          v-model="queryParams.sampleCode"
          class="!w-220px"
          clearable
          placeholder="请输入样例编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="样例名称" prop="sampleName">
        <el-input
          v-model="queryParams.sampleName"
          class="!w-220px"
          clearable
          placeholder="请输入样例名称"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="样例类型" prop="sampleType">
        <el-select v-model="queryParams.sampleType" class="!w-220px" clearable placeholder="请选择样例类型">
          <el-option
            v-for="item in riskEventSampleTypeOptions"
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
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['risk:event-sample:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="样例编码" align="center" prop="sampleCode" min-width="160" />
      <el-table-column label="样例名称" align="center" prop="sampleName" min-width="180" />
      <el-table-column label="样例类型" align="center" min-width="120">
        <template #default="scope">
          <el-tag effect="plain">{{ getRiskEventSampleTypeLabel(scope.row.sampleType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源编码" align="center" prop="sourceCode" min-width="140" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="排序号" align="center" prop="sortNo" width="90" />
      <el-table-column label="样例说明" align="center" prop="description" min-width="220" show-overflow-tooltip />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="240" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openPreview(scope.row.id)" v-hasPermi="['risk:event-sample:preview']">
            预览
          </el-button>
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['risk:event-sample:update']">
            编辑
          </el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['risk:event-sample:delete']">
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

  <EventSampleForm ref="formRef" @success="getList" />
  <EventSamplePreview ref="previewRef" />
</template>

<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as EventSampleApi from '@/api/risk/eventSample'
import EventSampleForm from './EventSampleForm.vue'
import EventSamplePreview from './EventSamplePreview.vue'
import { getRiskEventSampleTypeLabel, riskEventSampleTypeOptions } from './constants'

defineOptions({ name: 'RiskEventSample' })

const message = useMessage()
const { t } = useI18n()

const createDefaultQueryParams = (): EventSampleApi.EventSamplePageReqVO => ({
  pageNo: 1,
  pageSize: 10,
  sceneCode: 'TRADE_RISK',
  eventCode: 'TRADE_EVENT',
  sampleCode: undefined,
  sampleName: undefined,
  sampleType: undefined,
  status: undefined
})

const loading = ref(true)
const total = ref(0)
const list = ref<EventSampleApi.EventSampleVO[]>([])
const queryParams = reactive<EventSampleApi.EventSamplePageReqVO>(createDefaultQueryParams())
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await EventSampleApi.getEventSamplePage(queryParams)
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
const openPreview = (id: number) => {
  previewRef.value.open(id)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await EventSampleApi.deleteEventSample(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>
