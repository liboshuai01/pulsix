<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :model="queryParams"
      :inline="true"
      label-width="92px"
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
      <el-form-item label="发布状态" prop="publishStatus">
        <el-select v-model="queryParams.publishStatus" class="!w-180px" clearable placeholder="请选择发布状态">
          <el-option v-for="item in publishStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="预检状态" prop="validationStatus">
        <el-select
          v-model="queryParams.validationStatus"
          class="!w-180px"
          clearable
          placeholder="请选择预检状态"
        >
          <el-option
            v-for="item in validationStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openCompileDialog" v-hasPermi="['risk:release:compile']">
          <Icon icon="ep:promotion" class="mr-5px" /> 预检 / 编译
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      title="S14 已支持正式发布、计划生效与基础回滚；运行时仍按 effectiveFrom 解析当前版本。"
      type="info"
      :closable="false"
      class="mb-16px"
    />

    <el-table v-loading="loading" :data="list">
      <el-table-column label="所属场景" align="center" prop="sceneCode" min-width="120" />
      <el-table-column label="版本号" align="center" min-width="140">
        <template #default="scope">
          <div class="inline-flex items-center gap-6px">
            <span>v{{ scope.row.versionNo }}</span>
            <el-tag v-if="scope.row.rollbackFromVersion" type="warning" effect="plain">回滚</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="发布状态" align="center" min-width="120">
        <template #default="scope">
          <el-tag :type="getReleasePublishStatusTag(scope.row.publishStatus)" effect="plain">
            {{ getReleasePublishStatusLabel(scope.row.publishStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="预检状态" align="center" min-width="120">
        <template #default="scope">
          <el-tag :type="getReleaseValidationStatusTag(scope.row.validationStatus)" effect="plain">
            {{ getReleaseValidationStatusLabel(scope.row.validationStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="编译产物" align="center" min-width="190">
        <template #default="scope">{{ formatReleaseCompileSummary(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="发布时间" align="center" prop="publishedAt" width="180" :formatter="dateFormatter" />
      <el-table-column label="生效时间" align="center" prop="effectiveFrom" width="180" :formatter="dateFormatter" />
      <el-table-column label="回滚来源" align="center" min-width="120">
        <template #default="scope">{{ formatRollbackVersion(scope.row.rollbackFromVersion) }}</template>
      </el-table-column>
      <el-table-column label="快照摘要" align="center" prop="checksum" min-width="260" show-overflow-tooltip />
      <el-table-column label="说明" align="center" prop="remark" min-width="240" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="250" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openPreview(scope.row.id!)" v-hasPermi="['risk:release:preview']">
            预览
          </el-button>
          <el-button
            v-if="isReleasePublishable(scope.row)"
            link
            type="success"
            @click="openPublishDialog(scope.row)"
            v-hasPermi="['risk:release:publish']"
          >
            发布
          </el-button>
          <el-button
            v-if="isReleaseRollbackAvailable(scope.row)"
            link
            type="warning"
            @click="openRollbackDialog(scope.row)"
            v-hasPermi="['risk:release:rollback']"
          >
            回滚到此版本
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

  <Dialog v-model="compileDialogVisible" title="发布预检 / 编译" width="620px">
    <el-form ref="compileFormRef" :model="compileFormData" :rules="compileRules" label-width="100px">
      <el-form-item label="所属场景" prop="sceneCode">
        <el-input v-model="compileFormData.sceneCode" placeholder="请输入场景编码，例如 TRADE_RISK" />
      </el-form-item>
      <el-form-item label="版本说明" prop="remark">
        <el-input
          v-model="compileFormData.remark"
          type="textarea"
          :rows="4"
          placeholder="可填写本次候选版本的预检背景，例如阈值调整或规则整理说明"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="compileDialogVisible = false">取 消</el-button>
      <el-button type="primary" :loading="compileLoading" @click="submitCompile">开始预检</el-button>
    </template>
  </Dialog>

  <Dialog v-model="publishDialogVisible" title="正式发布" width="620px">
    <el-form ref="publishFormRef" :model="publishFormData" :rules="publishRules" label-width="110px">
      <el-form-item label="所属场景">
        <el-input :model-value="publishTarget?.sceneCode || '-'" disabled />
      </el-form-item>
      <el-form-item label="候选版本">
        <el-input :model-value="publishTarget ? `v${publishTarget.versionNo}` : '-'" disabled />
      </el-form-item>
      <el-form-item label="计划生效时间" prop="effectiveFrom">
        <el-date-picker
          v-model="publishFormData.effectiveFrom"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          format="YYYY-MM-DD HH:mm:ss"
          placeholder="留空表示立即生效"
          class="!w-full"
          clearable
        />
      </el-form-item>
      <el-form-item label="发布说明" prop="remark">
        <el-input
          v-model="publishFormData.remark"
          type="textarea"
          :rows="4"
          placeholder="可填写发布原因、观察点或灰度说明；留空则沿用候选版本说明"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="publishDialogVisible = false">取 消</el-button>
      <el-button type="primary" :loading="publishLoading" @click="submitPublish">确认发布</el-button>
    </template>
  </Dialog>

  <Dialog v-model="rollbackDialogVisible" title="发布回滚" width="620px">
    <el-form ref="rollbackFormRef" :model="rollbackFormData" :rules="rollbackRules" label-width="110px">
      <el-form-item label="所属场景">
        <el-input :model-value="rollbackTarget?.sceneCode || '-'" disabled />
      </el-form-item>
      <el-form-item label="回滚来源版本">
        <el-input :model-value="rollbackTarget ? `v${rollbackTarget.versionNo}` : '-'" disabled />
      </el-form-item>
      <el-form-item label="计划生效时间" prop="effectiveFrom">
        <el-date-picker
          v-model="rollbackFormData.effectiveFrom"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          format="YYYY-MM-DD HH:mm:ss"
          placeholder="留空表示立即生效"
          class="!w-full"
          clearable
        />
      </el-form-item>
      <el-form-item label="回滚说明" prop="remark">
        <el-input
          v-model="rollbackFormData.remark"
          type="textarea"
          :rows="4"
          placeholder="可填写回滚原因、影响范围或恢复目标"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="rollbackDialogVisible = false">取 消</el-button>
      <el-button type="warning" :loading="rollbackLoading" @click="submitRollback">确认回滚</el-button>
    </template>
  </Dialog>

  <ReleasePreview ref="previewRef" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as ReleaseApi from '@/api/risk/release'
import ReleasePreview from './ReleasePreview.vue'
import {
  formatReleaseCompileSummary,
  getReleasePublishStatusLabel,
  getReleasePublishStatusTag,
  getReleaseValidationStatusLabel,
  getReleaseValidationStatusTag,
  isReleasePublishable,
  isReleaseRollbackAvailable,
  publishStatusOptions,
  validationStatusOptions
} from './constants'

defineOptions({ name: 'RiskRelease' })

const message = useMessage()

const createDefaultQueryParams = (): ReleaseApi.SceneReleasePageReqVO => ({
  pageNo: 1,
  pageSize: 10,
  sceneCode: 'TRADE_RISK',
  publishStatus: undefined,
  validationStatus: undefined
})

const loading = ref(true)
const total = ref(0)
const list = ref<ReleaseApi.SceneReleaseVO[]>([])
const queryParams = reactive<ReleaseApi.SceneReleasePageReqVO>(createDefaultQueryParams())
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ReleaseApi.getSceneReleasePage(queryParams)
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

const formatRollbackVersion = (version?: number) => {
  return version ? `v${version}` : '-'
}

const previewRef = ref()
const openPreview = (id: number) => {
  previewRef.value.open(id)
}

const compileDialogVisible = ref(false)
const compileLoading = ref(false)
const compileFormRef = ref()
const compileFormData = reactive<ReleaseApi.SceneReleaseCompileReqVO>({
  sceneCode: 'TRADE_RISK',
  remark: ''
})
const compileRules = reactive({
  sceneCode: [{ required: true, message: '所属场景不能为空', trigger: 'blur' }],
  remark: [{ max: 512, message: '版本说明长度不能超过 512 个字符', trigger: 'blur' }]
})

const openCompileDialog = () => {
  compileDialogVisible.value = true
  compileFormRef.value?.resetFields()
  Object.assign(compileFormData, {
    sceneCode: queryParams.sceneCode || 'TRADE_RISK',
    remark: ''
  })
}

const submitCompile = async () => {
  await compileFormRef.value?.validate()
  compileLoading.value = true
  try {
    const data = await ReleaseApi.compileSceneRelease({
      sceneCode: compileFormData.sceneCode.trim(),
      remark: compileFormData.remark?.trim() || undefined
    })
    compileDialogVisible.value = false
    if (data.validationStatus === 'PASSED') {
      message.success(`候选版本 v${data.versionNo} 预检通过`)
    } else {
      message.warning(`候选版本 v${data.versionNo} 预检未通过，请查看预检报告`)
    }
    await getList()
    previewRef.value.openWithData(data)
  } finally {
    compileLoading.value = false
  }
}

const publishDialogVisible = ref(false)
const publishLoading = ref(false)
const publishFormRef = ref()
const publishTarget = ref<ReleaseApi.SceneReleaseVO>()
const publishFormData = reactive<ReleaseApi.SceneReleasePublishReqVO>({
  id: 0,
  effectiveFrom: '',
  remark: ''
})
const publishRules = reactive({
  remark: [{ max: 512, message: '发布说明长度不能超过 512 个字符', trigger: 'blur' }]
})

const openPublishDialog = (row: ReleaseApi.SceneReleaseVO) => {
  publishTarget.value = row
  publishDialogVisible.value = true
  publishFormRef.value?.resetFields()
  Object.assign(publishFormData, {
    id: row.id || 0,
    effectiveFrom: '',
    remark: row.remark || ''
  })
}

const submitPublish = async () => {
  await publishFormRef.value?.validate()
  if (!publishTarget.value?.id) {
    return
  }
  await message.confirm(`确认正式发布候选版本 v${publishTarget.value.versionNo} 吗？`)
  publishLoading.value = true
  try {
    const data = await ReleaseApi.publishSceneRelease({
      id: publishTarget.value.id,
      effectiveFrom: publishFormData.effectiveFrom || undefined,
      remark: publishFormData.remark?.trim() || undefined
    })
    publishDialogVisible.value = false
    message.success(data.publishStatus === 'ACTIVE' ? `版本 v${data.versionNo} 已发布并生效` : `版本 v${data.versionNo} 已发布`)
    await getList()
    previewRef.value.openWithData(data)
  } finally {
    publishLoading.value = false
  }
}

const rollbackDialogVisible = ref(false)
const rollbackLoading = ref(false)
const rollbackFormRef = ref()
const rollbackTarget = ref<ReleaseApi.SceneReleaseVO>()
const rollbackFormData = reactive<ReleaseApi.SceneReleaseRollbackReqVO>({
  id: 0,
  effectiveFrom: '',
  remark: ''
})
const rollbackRules = reactive({
  remark: [{ max: 512, message: '回滚说明长度不能超过 512 个字符', trigger: 'blur' }]
})

const openRollbackDialog = (row: ReleaseApi.SceneReleaseVO) => {
  rollbackTarget.value = row
  rollbackDialogVisible.value = true
  rollbackFormRef.value?.resetFields()
  Object.assign(rollbackFormData, {
    id: row.id || 0,
    effectiveFrom: '',
    remark: `回滚到 v${row.versionNo}`
  })
}

const submitRollback = async () => {
  await rollbackFormRef.value?.validate()
  if (!rollbackTarget.value?.id) {
    return
  }
  await message.confirm(`确认基于历史版本 v${rollbackTarget.value.versionNo} 生成回滚版本吗？`)
  rollbackLoading.value = true
  try {
    const data = await ReleaseApi.rollbackSceneRelease({
      id: rollbackTarget.value.id,
      effectiveFrom: rollbackFormData.effectiveFrom || undefined,
      remark: rollbackFormData.remark?.trim() || undefined
    })
    rollbackDialogVisible.value = false
    message.success(
      data.publishStatus === 'ACTIVE'
        ? `回滚版本 v${data.versionNo} 已生成并立即生效`
        : `回滚版本 v${data.versionNo} 已生成`
    )
    await getList()
    previewRef.value.openWithData(data)
  } finally {
    rollbackLoading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>
