<template>
  <ContentWrap>
    <div class="risk-scene__hero">
      <div>
        <p class="risk-scene__eyebrow">配置中心</p>
        <h1 class="risk-scene__title">场景管理</h1>
        <p class="risk-scene__description">
          管理营销反作弊、提现审核、订单后置风控等风控场景，为事件模型、特征、规则、策略与发布提供统一组织根。
        </p>
      </div>
      <el-tag type="primary" effect="plain" round>scene_def 已接入</el-tag>
    </div>
  </ContentWrap>

  <ContentWrap>
    <el-form
      ref="queryFormRef"
      :model="queryParams"
      :inline="true"
      class="-mb-15px"
      label-width="78px"
    >
      <el-form-item label="场景名称" prop="sceneName">
        <el-input
          v-model="queryParams.sceneName"
          placeholder="请输入场景名称"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="场景编码" prop="sceneCode">
        <el-input
          v-model="queryParams.sceneCode"
          placeholder="请输入场景编码"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="场景类型" prop="sceneType">
        <el-input
          v-model="queryParams.sceneType"
          placeholder="请输入场景类型"
          clearable
          class="!w-240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-240px">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="运行模式" prop="runtimeMode">
        <el-select
          v-model="queryParams.runtimeMode"
          placeholder="请选择运行模式"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="item in SCENE_RUNTIME_MODE_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
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
          v-hasPermi="['risk:scene:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" />新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="场景名称" align="center" prop="sceneName" min-width="150" />
      <el-table-column label="场景编码" align="center" prop="sceneCode" min-width="170" />
      <el-table-column label="场景类型" align="center" prop="sceneType" min-width="120" />
      <el-table-column label="运行模式" align="center" prop="runtimeMode" min-width="140">
        <template #default="scope">
          {{ getSceneRuntimeModeLabel(scope.row.runtimeMode) }}
        </template>
      </el-table-column>
      <el-table-column
        label="默认策略编码"
        align="center"
        prop="defaultPolicyCode"
        min-width="220"
        :show-overflow-tooltip="true"
      />
      <el-table-column label="决策超时(ms)" align="center" prop="decisionTimeoutMs" width="130" />
      <el-table-column label="日志级别" align="center" prop="logLevel" width="120">
        <template #default="scope">
          {{ getSceneLogLevelLabel(scope.row.logLevel) }}
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
      <el-table-column label="操作" align="center" width="260" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)">查看详情</el-button>
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['risk:scene:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="warning"
            @click="handleStatusChange(scope.row)"
            v-hasPermi="['risk:scene:update']"
          >
            {{ scope.row.status === CommonStatusEnum.ENABLE ? '停用' : '启用' }}
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['risk:scene:delete']"
          >
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

  <SceneForm ref="formRef" @success="getList" />
  <SceneDetailDrawer ref="detailDrawerRef" />
</template>

<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { CommonStatusEnum } from '@/utils/constants'
import * as SceneApi from '@/api/risk/scene'
import SceneForm from './SceneForm.vue'
import SceneDetailDrawer from './SceneDetailDrawer.vue'
import {
  getSceneLogLevelLabel,
  getSceneRuntimeModeLabel,
  SCENE_RUNTIME_MODE_OPTIONS
} from './constants'

defineOptions({ name: 'RiskScene' })

const message = useMessage()

const loading = ref(false)
const total = ref(0)
const list = ref<SceneApi.SceneVO[]>([])
const queryFormRef = ref()

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  sceneName: undefined,
  sceneCode: undefined,
  sceneType: undefined,
  status: undefined,
  runtimeMode: undefined
})

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

const detailDrawerRef = ref()
const openDetail = (id: number) => {
  detailDrawerRef.value.open(id)
}

const handleStatusChange = async (row: SceneApi.SceneVO) => {
  const nextStatus =
    row.status === CommonStatusEnum.ENABLE ? CommonStatusEnum.DISABLE : CommonStatusEnum.ENABLE
  const actionText = nextStatus === CommonStatusEnum.ENABLE ? '启用' : '停用'
  try {
    await message.confirm(`确认要${actionText}场景“${row.sceneName}”吗？`)
    await SceneApi.updateSceneStatus(row.id!, nextStatus)
    message.success('状态更新成功')
    await getList()
  } catch {}
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await SceneApi.deleteScene(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.risk-scene__hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.risk-scene__eyebrow {
  margin: 0 0 6px;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.risk-scene__title {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 28px;
  line-height: 1.2;
}

.risk-scene__description {
  max-width: 880px;
  margin: 10px 0 0;
  color: var(--el-text-color-regular);
  line-height: 1.8;
}

@media (max-width: 768px) {
  .risk-scene__hero {
    flex-direction: column;
  }

  .risk-scene__title {
    font-size: 24px;
  }
}
</style>
