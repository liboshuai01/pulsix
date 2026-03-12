<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :model="queryParams"
      :inline="true"
      label-width="82px"
    >
      <el-form-item label="场景编码" prop="sceneCode">
        <el-input
          v-model="queryParams.sceneCode"
          class="!w-240px"
          clearable
          placeholder="请输入场景编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="场景名称" prop="sceneName">
        <el-input
          v-model="queryParams.sceneName"
          class="!w-240px"
          clearable
          placeholder="请输入场景名称"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="场景状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-240px" clearable placeholder="请选择状态">
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
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['risk:scene:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="场景编码" align="center" prop="sceneCode" min-width="140" />
      <el-table-column label="场景名称" align="center" prop="sceneName" min-width="160" />
      <el-table-column label="场景类型" align="center" min-width="120">
        <template #default="scope">
          <el-tag effect="plain">{{ getRiskSceneTypeLabel(scope.row.sceneType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="接入模式" align="center" min-width="120">
        <template #default="scope">
          <el-tag type="warning" effect="plain">
            {{ getRiskSceneAccessModeLabel(scope.row.accessMode) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="默认事件" align="center" prop="defaultEventCode" min-width="140" />
      <el-table-column label="默认策略" align="center" prop="defaultPolicyCode" min-width="170" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)" v-hasPermi="['risk:scene:get']">
            详情
          </el-button>
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['risk:scene:update']">
            编辑
          </el-button>
          <el-button
            link
            type="primary"
            @click="handleUpdateStatus(scope.row)"
            v-hasPermi="['risk:scene:update-status']"
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

  <SceneForm ref="formRef" @success="getList" />
  <SceneDetail ref="detailRef" />
</template>

<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { CommonStatusEnum } from '@/utils/constants'
import { dateFormatter } from '@/utils/formatTime'
import * as SceneApi from '@/api/risk/scene'
import SceneForm from './SceneForm.vue'
import SceneDetail from './SceneDetail.vue'
import { getRiskSceneAccessModeLabel, getRiskSceneTypeLabel } from './constants'

defineOptions({ name: 'RiskScene' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const total = ref(0)
const list = ref<SceneApi.SceneVO[]>([])
const queryParams = reactive<SceneApi.ScenePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  sceneCode: undefined,
  sceneName: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await SceneApi.getScenePage(queryParams)
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

const handleUpdateStatus = async (row: SceneApi.SceneVO) => {
  const nextStatus = row.status === CommonStatusEnum.ENABLE ? CommonStatusEnum.DISABLE : CommonStatusEnum.ENABLE
  const actionText = nextStatus === CommonStatusEnum.ENABLE ? '启用' : '停用'
  try {
    await message.confirm(`确认${actionText}场景「${row.sceneName}」吗？`)
    await SceneApi.updateSceneStatus(row.id!, nextStatus)
    message.success(t('common.updateSuccess'))
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>
