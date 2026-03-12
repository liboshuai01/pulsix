<template>
  <Dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="88%"
    :fullscreen="false"
    :scroll="true"
    max-height="72vh"
  >
    <div class="mb-12px flex flex-wrap items-center gap-8px">
      <el-tag effect="plain">所属场景：{{ listContext?.sceneCode || '-' }}</el-tag>
      <el-tag effect="plain">名单编码：{{ listContext?.listCode || '-' }}</el-tag>
      <el-tag effect="plain">名单名称：{{ listContext?.listName || '-' }}</el-tag>
      <el-tag effect="plain">Redis 前缀：{{ listContext?.redisKeyPrefix || '-' }}</el-tag>
    </div>

    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :model="queryParams"
      :inline="true"
      label-width="82px"
    >
      <el-form-item label="匹配值" prop="matchValue">
        <el-input
          v-model="queryParams.matchValue"
          class="!w-220px"
          clearable
          placeholder="请输入匹配值"
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
        <el-button type="primary" plain @click="openItemForm('create')" v-hasPermi="['risk:list:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增条目
        </el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list">
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
      <el-table-column label="备注" align="center" prop="remark" min-width="220" show-overflow-tooltip />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="260" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openItemForm('update', scope.row.id)" v-hasPermi="['risk:list:update']">
            编辑
          </el-button>
          <el-button link type="primary" @click="handleUpdateStatus(scope.row)" v-hasPermi="['risk:list:update']">
            {{ scope.row.status === CommonStatusEnum.ENABLE ? '停用' : '启用' }}
          </el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['risk:list:delete']">
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

    <ListItemForm ref="formRef" @success="handleFormSuccess" />
  </Dialog>
</template>

<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { CommonStatusEnum } from '@/utils/constants'
import { dateFormatter } from '@/utils/formatTime'
import * as RiskListApi from '@/api/risk/list'
import ListItemForm from './ListItemForm.vue'
import { getRiskListItemSourceTypeLabel } from './constants'

defineOptions({ name: 'RiskListItemDialog' })

interface ListContext {
  id?: number
  sceneCode: string
  listCode: string
  listName: string
  redisKeyPrefix?: string
}

const message = useMessage()
const { t } = useI18n()

const dialogVisible = ref(false)
const dialogTitle = ref('名单条目')
const loading = ref(false)
const total = ref(0)
const list = ref<RiskListApi.ListItemVO[]>([])
const listContext = ref<ListContext>()
const queryFormRef = ref()

const createDefaultQueryParams = (): RiskListApi.ListItemPageReqVO => ({
  pageNo: 1,
  pageSize: 10,
  sceneCode: listContext.value?.sceneCode || 'TRADE_RISK',
  listCode: listContext.value?.listCode || '',
  matchValue: undefined,
  status: undefined
})

const queryParams = reactive<RiskListApi.ListItemPageReqVO>(createDefaultQueryParams())

const resetState = () => {
  list.value = []
  total.value = 0
  queryFormRef.value?.resetFields()
  Object.assign(queryParams, createDefaultQueryParams())
}

const getList = async () => {
  if (!listContext.value) {
    return
  }
  loading.value = true
  try {
    const data = await RiskListApi.getListItemPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const open = async (context: RiskListApi.ListSetVO) => {
  listContext.value = {
    id: context.id,
    sceneCode: context.sceneCode,
    listCode: context.listCode,
    listName: context.listName,
    redisKeyPrefix: context.redisKeyPrefix
  }
  dialogTitle.value = `名单条目 - ${context.listName} (${context.listCode})`
  dialogVisible.value = true
  resetState()
  await getList()
}
defineExpose({ open })

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
const openItemForm = (type: 'create' | 'update', id?: number) => {
  if (!listContext.value) {
    return
  }
  formRef.value.open(
    type,
    {
      sceneCode: listContext.value.sceneCode,
      listCode: listContext.value.listCode,
      listName: listContext.value.listName
    },
    id
  )
}

const emit = defineEmits(['success'])

const handleFormSuccess = async () => {
  await getList()
  emit('success')
}

const handleUpdateStatus = async (row: RiskListApi.ListItemVO) => {
  const nextStatus = row.status === CommonStatusEnum.ENABLE ? CommonStatusEnum.DISABLE : CommonStatusEnum.ENABLE
  const actionText = nextStatus === CommonStatusEnum.ENABLE ? '启用' : '停用'
  try {
    await message.confirm(`确认${actionText}名单条目「${row.matchValue}」吗？`)
    await RiskListApi.updateListItemStatus(row.id!, nextStatus)
    message.success(t('common.updateSuccess'))
    await getList()
    emit('success')
  } catch {}
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await RiskListApi.deleteListItem(id)
    message.success(t('common.delSuccess'))
    await getList()
    emit('success')
  } catch {}
}
</script>
