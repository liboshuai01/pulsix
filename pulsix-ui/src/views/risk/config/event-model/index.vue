<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      :model="queryParams"
      :inline="true"
      class="-mb-15px"
      label-width="82px"
    >
      <el-form-item label="场景编码" prop="sceneCode">
        <el-input
          v-model="queryParams.sceneCode"
          placeholder="请输入场景编码"
          clearable
          class="!w-220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="事件编码" prop="eventCode">
        <el-input
          v-model="queryParams.eventCode"
          placeholder="请输入事件编码"
          clearable
          class="!w-220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="事件名称" prop="eventName">
        <el-input
          v-model="queryParams.eventName"
          placeholder="请输入事件名称"
          clearable
          class="!w-220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="事件类型" prop="eventType">
        <el-input
          v-model="queryParams.eventType"
          placeholder="请输入事件类型"
          clearable
          class="!w-220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="接入类型" prop="sourceType">
        <el-input
          v-model="queryParams.sourceType"
          placeholder="请输入接入类型"
          clearable
          class="!w-220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-220px">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['risk:event-model:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" />新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="场景编码" align="center" prop="sceneCode" min-width="150" />
      <el-table-column label="事件编码" align="center" prop="eventCode" min-width="170" />
      <el-table-column label="事件名称" align="center" prop="eventName" min-width="160" />
      <el-table-column label="事件类型" align="center" prop="eventType" min-width="150" />
      <el-table-column label="接入类型" align="center" prop="sourceType" min-width="120" />
      <el-table-column
        label="标准 Topic"
        align="center"
        prop="topicName"
        min-width="200"
        :show-overflow-tooltip="true"
      />
      <el-table-column label="版本" align="center" prop="version" width="90" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column
        label="更新时间"
        align="center"
        prop="updateTime"
        width="180"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" align="center" width="400" fixed="right">
        <template #default="scope">
          <div class="risk-event-model__actions">
            <el-button
              link
              type="primary"
              class="risk-event-model__action-btn"
              @click="openDetail(scope.row.id!)"
            >
              查看详情
            </el-button>
            <el-button
              link
              type="primary"
              class="risk-event-model__action-btn"
              @click="openPreview(scope.row.id!)"
            >
              预览标准事件
            </el-button>
            <el-button
              link
              type="primary"
              class="risk-event-model__action-btn"
              @click="openForm('update', scope.row.id!)"
              v-hasPermi="['risk:event-model:update']"
            >
              编辑
            </el-button>
            <el-button
              link
              type="warning"
              class="risk-event-model__action-btn"
              @click="handleStatusChange(scope.row)"
              v-hasPermi="['risk:event-model:update']"
            >
              {{ scope.row.status === CommonStatusEnum.ENABLE ? '停用' : '启用' }}
            </el-button>
            <el-button
              link
              type="danger"
              class="risk-event-model__action-btn"
              @click="handleDelete(scope.row.id!)"
              v-hasPermi="['risk:event-model:delete']"
            >
              删除
            </el-button>
          </div>
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

  <EventModelForm ref="formRef" @success="getList" />
  <EventModelDetailDialog ref="detailDialogRef" />
  <EventModelPreviewDialog ref="previewDialogRef" />
</template>

<script setup lang="ts">
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as EventModelApi from '@/api/risk/event-model'
import EventModelForm from './EventModelForm.vue'
import EventModelDetailDialog from './EventModelDetailDialog.vue'
import EventModelPreviewDialog from './EventModelPreviewDialog.vue'

defineOptions({ name: 'RiskEventModel' })

const message = useMessage()

const loading = ref(false)
const total = ref(0)
const list = ref<EventModelApi.EventModelVO[]>([])
const queryFormRef = ref()

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  sceneCode: undefined,
  eventCode: undefined,
  eventName: undefined,
  eventType: undefined,
  sourceType: undefined,
  status: undefined
})

const getList = async () => {
  loading.value = true
  try {
    const data = await EventModelApi.getEventModelPage(queryParams)
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

const detailDialogRef = ref()
const openDetail = (id: number) => {
  detailDialogRef.value.open(id)
}

const previewDialogRef = ref()
const openPreview = (id: number) => {
  previewDialogRef.value.open(id)
}

const handleStatusChange = async (row: EventModelApi.EventModelVO) => {
  const nextStatus =
    row.status === CommonStatusEnum.ENABLE ? CommonStatusEnum.DISABLE : CommonStatusEnum.ENABLE
  const actionText = nextStatus === CommonStatusEnum.ENABLE ? '启用' : '停用'
  try {
    await message.confirm(`确认要${actionText}事件模型“${row.eventName}”吗？`)
    await EventModelApi.updateEventModelStatus(row.id!, nextStatus)
    message.success('状态更新成功')
    await getList()
  } catch {}
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await EventModelApi.deleteEventModel(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.risk-event-model__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px 12px;
}

.risk-event-model__action-btn {
  margin-left: 0 !important;
  padding: 0;
}
</style>
