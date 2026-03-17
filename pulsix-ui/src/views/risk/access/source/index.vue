<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      :model="queryParams"
      :inline="true"
      class="-mb-15px"
      label-width="82px"
    >
      <el-form-item label="接入源编码" prop="sourceCode">
        <el-input
          v-model="queryParams.sourceCode"
          placeholder="请输入接入源编码"
          clearable
          class="!w-220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="接入源名称" prop="sourceName">
        <el-input
          v-model="queryParams.sourceName"
          placeholder="请输入接入源名称"
          clearable
          class="!w-220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="接入类型" prop="sourceType">
        <el-select
          v-model="queryParams.sourceType"
          placeholder="请选择接入类型"
          clearable
          class="!w-220px"
        >
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.RISK_ACCESS_SOURCE_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="标准 Topic" prop="topicName">
        <el-select
          v-model="queryParams.topicName"
          placeholder="请选择标准 Topic"
          clearable
          class="!w-220px"
        >
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.RISK_ACCESS_TOPIC_NAME)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
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
          v-hasPermi="['risk:access-source:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" />新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="接入源名称" align="center" prop="sourceName" min-width="180" />
      <el-table-column label="接入源编码" align="center" prop="sourceCode" min-width="180" />
      <el-table-column label="接入类型" align="center" prop="sourceType" min-width="120">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.RISK_ACCESS_SOURCE_TYPE" :value="scope.row.sourceType" />
        </template>
      </el-table-column>
      <el-table-column label="标准 Topic" align="center" prop="topicName" min-width="180">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.RISK_ACCESS_TOPIC_NAME" :value="scope.row.topicName" />
        </template>
      </el-table-column>
      <el-table-column label="允许场景" align="center" min-width="220">
        <template #default="scope">
          <div v-if="scope.row.allowedSceneCodes?.length" class="risk-access-source__tag-list">
            <el-tag
              v-for="sceneCode in scope.row.allowedSceneCodes.slice(0, 3)"
              :key="sceneCode"
              effect="plain"
              class="mr-6px mb-6px"
            >
              {{ sceneCode }}
            </el-tag>
            <el-tag
              v-if="scope.row.allowedSceneCodes.length > 3"
              type="info"
              effect="plain"
              class="mr-6px mb-6px"
            >
              +{{ scope.row.allowedSceneCodes.length - 3 }}
            </el-tag>
          </div>
          <span v-else>-</span>
        </template>
      </el-table-column>
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
      <el-table-column label="操作" align="center" width="320" fixed="right">
        <template #default="scope">
          <div class="risk-access-source__actions">
            <el-button
              link
              type="primary"
              class="risk-access-source__action-btn"
              @click="openDetail(scope.row.id!)"
            >
              详情
            </el-button>
            <el-button
              link
              type="primary"
              class="risk-access-source__action-btn"
              @click="openForm('update', scope.row.id!)"
              v-hasPermi="['risk:access-source:update']"
            >
              编辑
            </el-button>
            <el-button
              link
              type="warning"
              class="risk-access-source__action-btn"
              @click="handleStatusChange(scope.row)"
              v-hasPermi="['risk:access-source:update']"
            >
              {{ scope.row.status === CommonStatusEnum.ENABLE ? '停用' : '启用' }}
            </el-button>
            <el-button
              link
              type="danger"
              class="risk-access-source__action-btn"
              @click="handleDelete(scope.row.id!)"
              v-hasPermi="['risk:access-source:delete']"
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

  <AccessSourceForm ref="formRef" @success="getList" />
  <AccessSourceDetailDialog ref="detailDialogRef" />
</template>

<script setup lang="ts">
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getIntDictOptions, getStrDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as AccessSourceApi from '@/api/risk/access-source'
import AccessSourceForm from './AccessSourceForm.vue'
import AccessSourceDetailDialog from './AccessSourceDetailDialog.vue'

defineOptions({ name: 'RiskAccessSource' })

const message = useMessage()

const loading = ref(false)
const total = ref(0)
const list = ref<AccessSourceApi.AccessSourceVO[]>([])
const queryFormRef = ref()

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  sourceCode: undefined,
  sourceName: undefined,
  sourceType: undefined,
  topicName: undefined,
  status: undefined
})

const getList = async () => {
  loading.value = true
  try {
    const data = await AccessSourceApi.getAccessSourcePage(queryParams)
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

const handleStatusChange = async (row: AccessSourceApi.AccessSourceVO) => {
  const nextStatus =
    row.status === CommonStatusEnum.ENABLE ? CommonStatusEnum.DISABLE : CommonStatusEnum.ENABLE
  const actionText = nextStatus === CommonStatusEnum.ENABLE ? '启用' : '停用'
  try {
    await message.confirm(`确认要${actionText}接入源“${row.sourceName}”吗？`)
    await AccessSourceApi.updateAccessSourceStatus(row.id!, nextStatus)
    message.success('状态更新成功')
    await getList()
  } catch {}
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await AccessSourceApi.deleteAccessSource(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.risk-access-source__actions {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
}

.risk-access-source__action-btn {
  margin-left: 0 !important;
}

.risk-access-source__tag-list {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
}
</style>
