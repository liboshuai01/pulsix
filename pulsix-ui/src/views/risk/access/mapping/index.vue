<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      :model="queryParams"
      :inline="true"
      class="-mb-15px"
      label-width="92px"
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
      <el-form-item label="标准类型" prop="eventType">
        <el-input
          v-model="queryParams.eventType"
          placeholder="请输入标准 eventType"
          clearable
          class="!w-220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="接入源编码" prop="sourceCode">
        <el-input
          v-model="queryParams.sourceCode"
          placeholder="请输入接入源编码"
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
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['risk:access-mapping:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" />新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="场景" align="center" prop="sceneCode" min-width="140" />
      <el-table-column label="事件" min-width="220">
        <template #default="{ row }">
          <div class="font-600">{{ row.eventName || row.eventCode }}</div>
          <div class="text-12px text-[var(--el-text-color-secondary)]">{{ row.eventCode }}</div>
        </template>
      </el-table-column>
      <el-table-column label="标准 eventType" align="center" prop="eventType" min-width="170" />
      <el-table-column label="接入源" min-width="220">
        <template #default="{ row }">
          <div class="font-600">{{ row.sourceName || row.sourceCode }}</div>
          <div class="text-12px text-[var(--el-text-color-secondary)]">{{ row.sourceCode }}</div>
        </template>
      </el-table-column>
      <el-table-column label="接入类型" align="center" min-width="120">
        <template #default="{ row }">
          <dict-tag :type="DICT_TYPE.RISK_ACCESS_SOURCE_TYPE" :value="row.sourceType" />
        </template>
      </el-table-column>
      <el-table-column label="原始字段数" align="center" prop="rawFieldCount" width="110" />
      <el-table-column label="已配置映射数" align="center" prop="mappingRuleCount" width="120" />
      <el-table-column label="更新人" align="center" prop="updater" width="120" />
      <el-table-column
        label="更新时间"
        align="center"
        prop="updateTime"
        width="180"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" align="center" width="300" fixed="right">
        <template #default="{ row }">
          <div class="risk-access-mapping__actions">
            <el-button link type="primary" @click="openDetail(row.id!)">详情</el-button>
            <el-button link type="primary" @click="openPreview(row.id!)">预览</el-button>
            <el-button
              link
              type="primary"
              @click="openForm('update', row.id!)"
              v-hasPermi="['risk:access-mapping:update']"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              @click="handleDelete(row.id!)"
              v-hasPermi="['risk:access-mapping:delete']"
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

  <AccessMappingForm ref="formRef" @success="getList" />
  <AccessMappingDetailDialog ref="detailDialogRef" />
  <AccessMappingPreviewDialog ref="previewDialogRef" />
</template>

<script setup lang="ts">
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as AccessMappingApi from '@/api/risk/access-mapping'
import AccessMappingDetailDialog from './AccessMappingDetailDialog.vue'
import AccessMappingForm from './AccessMappingForm.vue'
import AccessMappingPreviewDialog from './AccessMappingPreviewDialog.vue'

defineOptions({ name: 'RiskAccessMapping' })

const message = useMessage()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const total = ref(0)
const list = ref<AccessMappingApi.AccessMappingVO[]>([])
const queryFormRef = ref()

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  sceneCode: undefined as string | undefined,
  eventCode: undefined as string | undefined,
  eventType: undefined as string | undefined,
  sourceCode: undefined as string | undefined,
  sourceType: undefined as string | undefined
})

const normalizeQueryValue = (value: unknown) => {
  if (Array.isArray(value)) {
    return (value[0] as string) || undefined
  }
  return (value as string) || undefined
}

const getList = async () => {
  loading.value = true
  try {
    const data = await AccessMappingApi.getAccessMappingPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const applyRouteFilters = async () => {
  queryParams.pageNo = 1
  queryParams.sceneCode = normalizeQueryValue(route.query.sceneCode)
  queryParams.eventCode = normalizeQueryValue(route.query.eventCode)
  queryParams.eventType = normalizeQueryValue(route.query.eventType)
  queryParams.sourceCode = normalizeQueryValue(route.query.sourceCode)
  queryParams.sourceType = normalizeQueryValue(route.query.sourceType)
  await getList()
}

watch(
  () => route.query,
  () => {
    applyRouteFilters()
  },
  { immediate: true }
)

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = async () => {
  queryFormRef.value?.resetFields()
  if (Object.keys(route.query).length) {
    await router.replace({ path: route.path, query: {} })
    return
  }
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
  previewDialogRef.value.openById(id)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await AccessMappingApi.deleteAccessMapping(id)
    message.success('删除成功')
    await getList()
  } catch {}
}
</script>

<style scoped lang="scss">
.risk-access-mapping__actions {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
}
</style>
