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
      <el-form-item label="名单编码" prop="listCode">
        <el-input
          v-model="queryParams.listCode"
          class="!w-220px"
          clearable
          placeholder="请输入名单编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="名单名称" prop="listName">
        <el-input
          v-model="queryParams.listName"
          class="!w-220px"
          clearable
          placeholder="请输入名单名称"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="匹配维度" prop="matchType">
        <el-select v-model="queryParams.matchType" class="!w-220px" clearable placeholder="请选择匹配维度">
          <el-option
            v-for="item in riskListMatchTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="名单类型" prop="listType">
        <el-select v-model="queryParams.listType" class="!w-220px" clearable placeholder="请选择名单类型">
          <el-option
            v-for="item in riskListTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="同步状态" prop="syncStatus">
        <el-select v-model="queryParams.syncStatus" class="!w-220px" clearable placeholder="请选择同步状态">
          <el-option
            v-for="item in riskListSyncStatusOptions"
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
        <el-button type="primary" plain @click="openSetForm('create')" v-hasPermi="['risk:list:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增名单
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="listSetLoading" :data="listSetList">
      <el-table-column label="名单编码" align="center" prop="listCode" min-width="160" />
      <el-table-column label="名单名称" align="center" prop="listName" min-width="160" />
      <el-table-column label="所属场景" align="center" prop="sceneCode" min-width="120" />
      <el-table-column label="匹配维度" align="center" min-width="110">
        <template #default="scope">
          <el-tag effect="plain">{{ getRiskListMatchTypeLabel(scope.row.matchType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="名单类型" align="center" min-width="110">
        <template #default="scope">
          <el-tag type="danger" effect="plain">{{ getRiskListTypeLabel(scope.row.listType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="运行时存储" align="center" min-width="130">
        <template #default="scope">
          {{ getRiskListStorageTypeLabel(scope.row.storageType) }}
        </template>
      </el-table-column>
      <el-table-column label="Redis 前缀" align="center" prop="redisKeyPrefix" min-width="210" show-overflow-tooltip />
      <el-table-column label="同步状态" align="center" min-width="110">
        <template #default="scope">
          <el-tag :type="getSyncStatusTagType(scope.row.syncStatus)" effect="plain">
            {{ getRiskListSyncStatusLabel(scope.row.syncStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近同步" align="center" prop="lastSyncTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="说明" align="center" prop="description" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="360" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openItemDialog(scope.row)">条目</el-button>
          <el-button link type="primary" @click="openSetForm('update', scope.row.id)" v-hasPermi="['risk:list:update']">
            编辑
          </el-button>
          <el-button link type="primary" @click="handleUpdateSetStatus(scope.row)" v-hasPermi="['risk:list:update']">
            {{ scope.row.status === CommonStatusEnum.ENABLE ? '停用' : '启用' }}
          </el-button>
          <el-button link type="primary" @click="handleSync(scope.row)" v-hasPermi="['risk:list:sync']">
            同步Redis
          </el-button>
          <el-button link type="danger" @click="handleDeleteSet(scope.row.id)" v-hasPermi="['risk:list:delete']">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      :total="listSetTotal"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getListSetList"
    />
  </ContentWrap>

  <ListSetForm ref="listSetFormRef" @success="handleSetFormSuccess" />
  <ListItemDialog ref="listItemDialogRef" @success="handleItemDialogSuccess" />
</template>

<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { CommonStatusEnum } from '@/utils/constants'
import { dateFormatter } from '@/utils/formatTime'
import * as RiskListApi from '@/api/risk/list'
import ListSetForm from './ListSetForm.vue'
import ListItemDialog from './ListItemDialog.vue'
import {
  getRiskListMatchTypeLabel,
  getRiskListStorageTypeLabel,
  getRiskListSyncStatusLabel,
  getRiskListTypeLabel,
  riskListMatchTypeOptions,
  riskListSyncStatusOptions,
  riskListTypeOptions
} from './constants'

defineOptions({ name: 'RiskList' })

const message = useMessage()
const { t } = useI18n()

const createDefaultQueryParams = (): RiskListApi.ListSetPageReqVO => ({
  pageNo: 1,
  pageSize: 10,
  sceneCode: 'TRADE_RISK',
  listCode: undefined,
  listName: undefined,
  matchType: undefined,
  listType: undefined,
  syncStatus: undefined,
  status: undefined
})

const listSetLoading = ref(true)
const listSetTotal = ref(0)
const listSetList = ref<RiskListApi.ListSetVO[]>([])
const queryParams = reactive<RiskListApi.ListSetPageReqVO>(createDefaultQueryParams())
const queryFormRef = ref()

const getListSetList = async () => {
  listSetLoading.value = true
  try {
    const data = await RiskListApi.getListSetPage(queryParams)
    listSetList.value = data.list
    listSetTotal.value = data.total
  } finally {
    listSetLoading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getListSetList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  Object.assign(queryParams, createDefaultQueryParams())
  handleQuery()
}

const listSetFormRef = ref()
const openSetForm = (type: 'create' | 'update', id?: number) => {
  listSetFormRef.value.open(type, id)
}

const listItemDialogRef = ref()
const openItemDialog = (row: RiskListApi.ListSetVO) => {
  listItemDialogRef.value.open(row)
}

const handleSetFormSuccess = async () => {
  await getListSetList()
}

const handleItemDialogSuccess = async () => {
  await getListSetList()
}

const handleUpdateSetStatus = async (row: RiskListApi.ListSetVO) => {
  const nextStatus = row.status === CommonStatusEnum.ENABLE ? CommonStatusEnum.DISABLE : CommonStatusEnum.ENABLE
  const actionText = nextStatus === CommonStatusEnum.ENABLE ? '启用' : '停用'
  try {
    await message.confirm(`确认${actionText}名单「${row.listName}」吗？`)
    await RiskListApi.updateListSetStatus(row.id!, nextStatus)
    message.success(t('common.updateSuccess'))
    await getListSetList()
  } catch {}
}

const handleDeleteSet = async (id: number) => {
  try {
    await message.delConfirm('删除名单集合会同时删除其所有条目，是否继续？')
    await RiskListApi.deleteListSet(id)
    message.success(t('common.delSuccess'))
    await getListSetList()
  } catch {}
}

const handleSync = async (row: RiskListApi.ListSetVO) => {
  try {
    await message.confirm(`确认同步名单「${row.listName}」到 Redis 吗？`)
    const data = await RiskListApi.syncListSet(row.id!)
    message.success(`同步成功：${data.syncedItemCount} 条，前缀 ${data.redisKeyPrefix}`)
    await getListSetList()
  } catch {}
}

const getSyncStatusTagType = (value?: string) => {
  if (value === 'SUCCESS') return 'success'
  if (value === 'FAILED') return 'danger'
  return 'warning'
}

onMounted(() => {
  getListSetList()
})
</script>
