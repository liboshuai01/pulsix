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
      <el-form-item label="事件编码" prop="eventCode">
        <el-input
          v-model="queryParams.eventCode"
          class="!w-220px"
          clearable
          placeholder="请输入事件编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="事件名称" prop="eventName">
        <el-input
          v-model="queryParams.eventName"
          class="!w-220px"
          clearable
          placeholder="请输入事件名称"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="事件类别" prop="eventType">
        <el-select v-model="queryParams.eventType" class="!w-220px" clearable placeholder="请选择事件类别">
          <el-option
            v-for="item in riskEventSchemaTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['risk:event-schema:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="所属场景" align="center" prop="sceneCode" min-width="120" />
      <el-table-column label="事件编码" align="center" prop="eventCode" min-width="150" />
      <el-table-column label="事件名称" align="center" prop="eventName" min-width="160" />
      <el-table-column label="事件类别" align="center" min-width="120">
        <template #default="scope">
          <el-tag effect="plain">{{ getRiskEventSchemaTypeLabel(scope.row.eventType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="接入方式" align="center" min-width="120">
        <template #default="scope">
          <el-tag type="warning" effect="plain">
            {{ getRiskEventSchemaSourceTypeLabel(scope.row.sourceType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="模型版本" align="center" prop="version" width="100" />
      <el-table-column label="标准 Topic" align="center" prop="standardTopicName" min-width="180" show-overflow-tooltip />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)" v-hasPermi="['risk:event-schema:get']">
            详情
          </el-button>
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['risk:event-schema:update']"
          >
            编辑
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

  <EventSchemaForm ref="formRef" @success="getList" />
  <EventSchemaDetail ref="detailRef" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as EventSchemaApi from '@/api/risk/eventSchema'
import EventSchemaForm from './EventSchemaForm.vue'
import EventSchemaDetail from './EventSchemaDetail.vue'
import {
  getRiskEventSchemaSourceTypeLabel,
  getRiskEventSchemaTypeLabel,
  riskEventSchemaTypeOptions
} from './constants'

defineOptions({ name: 'RiskEventSchema' })

const createDefaultQueryParams = (): EventSchemaApi.EventSchemaPageReqVO => ({
  pageNo: 1,
  pageSize: 10,
  sceneCode: 'TRADE_RISK',
  eventCode: undefined,
  eventName: undefined,
  eventType: undefined
})

const loading = ref(true)
const total = ref(0)
const list = ref<EventSchemaApi.EventSchemaVO[]>([])
const queryParams = reactive<EventSchemaApi.EventSchemaPageReqVO>(createDefaultQueryParams())
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await EventSchemaApi.getEventSchemaPage(queryParams)
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

onMounted(() => {
  getList()
})
</script>
