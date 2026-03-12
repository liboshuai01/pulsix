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
    <el-table
      ref="listSetTableRef"
      v-loading="listSetLoading"
      :data="listSetList"
      highlight-current-row
      @current-change="handleCurrentSetChange"
    >
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
          <el-button link type="primary" @click="selectCurrentSet(scope.row)">条目</el-button>
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

  <ContentWrap>
    <template v-if="currentSet">
      <div class="mb-12px flex flex-wrap items-center gap-8px">
        <el-tag effect="plain">当前名单：{{ currentSet.listName }} ({{ currentSet.listCode }})</el-tag>
        <el-tag effect="plain">Redis 前缀：{{ currentSet.redisKeyPrefix }}</el-tag>
      </div>

      <el-form
        ref="itemQueryFormRef"
        class="-mb-15px"
        :model="itemQueryParams"
        :inline="true"
        label-width="82px"
      >
        <el-form-item label="匹配值" prop="matchValue">
          <el-input
            v-model="itemQueryParams.matchValue"
            class="!w-220px"
            clearable
            placeholder="请输入匹配值"
            @keyup.enter="handleItemQuery"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="itemQueryParams.status" class="!w-220px" clearable placeholder="请选择状态">
            <el-option
              v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="handleItemQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
          <el-button @click="resetItemQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
          <el-button type="primary" plain @click="openItemForm('create')" v-hasPermi="['risk:list:create']">
            <Icon icon="ep:plus" class="mr-5px" /> 新增条目
          </el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="listItemLoading" :data="listItemList">
        <el-table-column label="匹配键名" align="center" prop="matchKey" min-width="120" />
        <el-table-column label="匹配值" align="center" prop="matchValue" min-width="180" show-overflow-tooltip />
        <el-table-column label="过期时间" align="center" prop="expireAt" width="180" :formatter="dateFormatter" />
        <el-table-column label="来源类型" align="center" min-width="110">
          <template #default="scope">
            {{ getRiskListItemSourceTypeLabel(scope.row.sourceType) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="100">
          <template #default="scope">
            <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="备注" align="center" prop="remark" min-width="200" show-overflow-tooltip />
        <el-table-column label="更新时间" align="center" prop="updateTime" width="180" :formatter="dateFormatter" />
        <el-table-column label="操作" align="center" width="260" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openItemForm('update', scope.row.id)" v-hasPermi="['risk:list:update']">
              编辑
            </el-button>
            <el-button link type="primary" @click="handleUpdateItemStatus(scope.row)" v-hasPermi="['risk:list:update']">
              {{ scope.row.status === CommonStatusEnum.ENABLE ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDeleteItem(scope.row.id)" v-hasPermi="['risk:list:delete']">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        :total="listItemTotal"
        v-model:page="itemQueryParams.pageNo"
        v-model:limit="itemQueryParams.pageSize"
        @pagination="getListItemList"
      />
    </template>
    <el-empty v-else description="请先选择一个名单集合" />
  </ContentWrap>

  <ListSetForm ref="listSetFormRef" @success="handleSetFormSuccess" />
  <ListItemForm ref="listItemFormRef" @success="handleItemFormSuccess" />
</template>

<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { CommonStatusEnum } from '@/utils/constants'
import { dateFormatter } from '@/utils/formatTime'
import * as RiskListApi from '@/api/risk/list'
import ListSetForm from './ListSetForm.vue'
import ListItemForm from './ListItemForm.vue'
import {
  getRiskListItemSourceTypeLabel,
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
const listSetTableRef = ref()
const currentSet = ref<RiskListApi.ListSetVO>()

const createDefaultItemQueryParams = (): RiskListApi.ListItemPageReqVO => ({
  pageNo: 1,
  pageSize: 10,
  sceneCode: currentSet.value?.sceneCode || 'TRADE_RISK',
  listCode: currentSet.value?.listCode || '',
  matchValue: undefined,
  status: undefined
})

const listItemLoading = ref(false)
const listItemTotal = ref(0)
const listItemList = ref<RiskListApi.ListItemVO[]>([])
const itemQueryParams = reactive<RiskListApi.ListItemPageReqVO>(createDefaultItemQueryParams())
const itemQueryFormRef = ref()

const getListSetList = async () => {
  listSetLoading.value = true
  try {
    const data = await RiskListApi.getListSetPage(queryParams)
    listSetList.value = data.list
    listSetTotal.value = data.total
    syncCurrentSetSelection()
  } finally {
    listSetLoading.value = false
  }
}

const syncCurrentSetSelection = async () => {
  if (!listSetList.value.length) {
    currentSet.value = undefined
    listItemList.value = []
    listItemTotal.value = 0
    return
  }
  const nextCurrent = currentSet.value
    ? listSetList.value.find((item) => item.id === currentSet.value?.id)
    : listSetList.value[0]
  if (!nextCurrent) {
    currentSet.value = undefined
    listItemList.value = []
    listItemTotal.value = 0
    return
  }
  currentSet.value = nextCurrent
  nextTick(() => {
    listSetTableRef.value?.setCurrentRow(nextCurrent)
  })
  Object.assign(itemQueryParams, createDefaultItemQueryParams())
  await getListItemList()
}

const getListItemList = async () => {
  if (!currentSet.value) {
    return
  }
  listItemLoading.value = true
  try {
    const data = await RiskListApi.getListItemPage(itemQueryParams)
    listItemList.value = data.list
    listItemTotal.value = data.total
  } finally {
    listItemLoading.value = false
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

const handleCurrentSetChange = (row?: RiskListApi.ListSetVO) => {
  if (!row) return
  selectCurrentSet(row)
}

const selectCurrentSet = (row: RiskListApi.ListSetVO) => {
  if (!row) return
  currentSet.value = row
  Object.assign(itemQueryParams, createDefaultItemQueryParams())
  getListItemList()
}

const handleItemQuery = () => {
  itemQueryParams.pageNo = 1
  getListItemList()
}

const resetItemQuery = () => {
  itemQueryFormRef.value?.resetFields()
  Object.assign(itemQueryParams, createDefaultItemQueryParams())
  handleItemQuery()
}

const listSetFormRef = ref()
const openSetForm = (type: 'create' | 'update', id?: number) => {
  listSetFormRef.value.open(type, id)
}

const listItemFormRef = ref()
const openItemForm = (type: 'create' | 'update', id?: number) => {
  if (!currentSet.value) {
    message.warning('请先选择一个名单集合')
    return
  }
  listItemFormRef.value.open(
    type,
    {
      sceneCode: currentSet.value.sceneCode,
      listCode: currentSet.value.listCode,
      listName: currentSet.value.listName
    },
    id
  )
}

const handleSetFormSuccess = async () => {
  await getListSetList()
}

const handleItemFormSuccess = async () => {
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

const handleUpdateItemStatus = async (row: RiskListApi.ListItemVO) => {
  const nextStatus = row.status === CommonStatusEnum.ENABLE ? CommonStatusEnum.DISABLE : CommonStatusEnum.ENABLE
  const actionText = nextStatus === CommonStatusEnum.ENABLE ? '启用' : '停用'
  try {
    await message.confirm(`确认${actionText}名单条目「${row.matchValue}」吗？`)
    await RiskListApi.updateListItemStatus(row.id!, nextStatus)
    message.success(t('common.updateSuccess'))
    await getListSetList()
  } catch {}
}

const handleDeleteItem = async (id: number) => {
  try {
    await message.delConfirm()
    await RiskListApi.deleteListItem(id)
    message.success(t('common.delSuccess'))
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
