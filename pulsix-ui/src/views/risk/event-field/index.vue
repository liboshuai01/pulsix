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
      <el-form-item label="字段编码" prop="fieldCode">
        <el-input
          v-model="queryParams.fieldCode"
          class="!w-220px"
          clearable
          placeholder="请输入字段编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="字段名称" prop="fieldName">
        <el-input
          v-model="queryParams.fieldName"
          class="!w-220px"
          clearable
          placeholder="请输入字段名称"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="字段类型" prop="fieldType">
        <el-select v-model="queryParams.fieldType" class="!w-220px" clearable placeholder="请选择字段类型">
          <el-option
            v-for="item in riskEventFieldTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['risk:event-field:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="字段编码" align="center" prop="fieldCode" min-width="140" />
      <el-table-column label="字段名称" align="center" prop="fieldName" min-width="140" />
      <el-table-column label="字段类型" align="center" min-width="120">
        <template #default="scope">
          <el-tag effect="plain">{{ getRiskEventFieldTypeLabel(scope.row.fieldType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标准字段" align="center" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.standardFieldFlag === 1 ? 'success' : 'info'" effect="plain">
            {{ getFlagLabel(scope.row.standardFieldFlag) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="必填" align="center" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.requiredFlag === 1 ? 'danger' : 'info'" effect="plain">
            {{ getFlagLabel(scope.row.requiredFlag) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="允许为空" align="center" width="110">
        <template #default="scope">
          <el-tag :type="scope.row.nullableFlag === 1 ? 'warning' : 'success'" effect="plain">
            {{ getFlagLabel(scope.row.nullableFlag, '允许', '不允许') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序号" align="center" prop="sortNo" width="90" />
      <el-table-column label="字段路径" align="center" prop="fieldPath" min-width="180" show-overflow-tooltip />
      <el-table-column label="字段说明" align="center" prop="description" min-width="220" show-overflow-tooltip />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="250" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['risk:event-field:update']">
            编辑
          </el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['risk:event-field:delete']">
            删除
          </el-button>
          <el-button link type="primary" @click="handleUpdateSort(scope.row)" v-hasPermi="['risk:event-field:sort']">
            排序
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

  <EventFieldForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as EventFieldApi from '@/api/risk/eventField'
import EventFieldForm from './EventFieldForm.vue'
import { getFlagLabel, getRiskEventFieldTypeLabel, riskEventFieldTypeOptions } from './constants'

defineOptions({ name: 'RiskEventField' })

const message = useMessage()
const { t } = useI18n()

const createDefaultQueryParams = (): EventFieldApi.EventFieldPageReqVO => ({
  pageNo: 1,
  pageSize: 10,
  sceneCode: 'TRADE_RISK',
  eventCode: 'TRADE_EVENT',
  fieldCode: undefined,
  fieldName: undefined,
  fieldType: undefined
})

const loading = ref(true)
const total = ref(0)
const list = ref<EventFieldApi.EventFieldVO[]>([])
const queryParams = reactive<EventFieldApi.EventFieldPageReqVO>(createDefaultQueryParams())
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await EventFieldApi.getEventFieldPage(queryParams)
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

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await EventFieldApi.deleteEventField(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const handleUpdateSort = async (row: EventFieldApi.EventFieldVO) => {
  try {
    const { value } = await message.prompt(`请输入字段「${row.fieldName}」的新排序号`, '调整字段排序')
    const sortNo = Number(value)
    if (!Number.isInteger(sortNo) || sortNo < 0) {
      message.warning('排序号必须是大于等于 0 的整数')
      return
    }
    await EventFieldApi.updateEventFieldSort(row.id!, sortNo)
    message.success(t('common.updateSuccess'))
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>
