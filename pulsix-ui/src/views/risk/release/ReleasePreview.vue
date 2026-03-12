<template>
  <Dialog v-model="dialogVisible" title="发布预览" width="1180px">
    <el-descriptions v-loading="detailLoading" :column="3" border>
      <el-descriptions-item label="所属场景">{{ detailData.sceneCode }}</el-descriptions-item>
      <el-descriptions-item label="候选版本">v{{ detailData.versionNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="快照摘要">{{ detailData.checksum || '-' }}</el-descriptions-item>
      <el-descriptions-item label="发布状态">
        <el-tag :type="getReleasePublishStatusTag(detailData.publishStatus)" effect="plain">
          {{ getReleasePublishStatusLabel(detailData.publishStatus) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="预检状态">
        <el-tag :type="getReleaseValidationStatusTag(detailData.validationStatus)" effect="plain">
          {{ getReleaseValidationStatusLabel(detailData.validationStatus) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="编译统计">{{ formatReleaseCompileSummary(detailData) }}</el-descriptions-item>
      <el-descriptions-item label="编译耗时">{{ detailData.compileDurationMs ?? '-' }} ms</el-descriptions-item>
      <el-descriptions-item label="版本说明" :span="2">
        <div class="whitespace-pre-wrap leading-22px">{{ detailData.remark || '-' }}</div>
      </el-descriptions-item>
      <el-descriptions-item label="创建者">{{ detailData.creator || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">
        {{ detailData.createTime ? formatDate(detailData.createTime) : '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="更新时间">
        {{ detailData.updateTime ? formatDate(detailData.updateTime) : '-' }}
      </el-descriptions-item>
    </el-descriptions>

    <ContentWrap class="mt-16px">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="预检报告" name="validation">
          <JsonEditor :model-value="validationReportJson" mode="view" height="460px" />
        </el-tab-pane>
        <el-tab-pane label="依赖摘要" name="dependency">
          <JsonEditor :model-value="dependencyDigestJson" mode="view" height="460px" />
        </el-tab-pane>
        <el-tab-pane label="快照预览" name="snapshot">
          <JsonEditor :model-value="snapshotJson" mode="view" height="460px" />
        </el-tab-pane>
      </el-tabs>
    </ContentWrap>
  </Dialog>
</template>

<script lang="ts" setup>
import { formatDate } from '@/utils/formatTime'
import * as ReleaseApi from '@/api/risk/release'
import {
  formatReleaseCompileSummary,
  getReleasePublishStatusLabel,
  getReleasePublishStatusTag,
  getReleaseValidationStatusLabel,
  getReleaseValidationStatusTag
} from './constants'

defineOptions({ name: 'RiskReleasePreview' })

const dialogVisible = ref(false)
const detailLoading = ref(false)
const activeTab = ref('validation')
const detailData = ref<ReleaseApi.SceneReleaseVO>({
  id: undefined,
  sceneCode: '',
  versionNo: 0,
  publishStatus: 'DRAFT',
  validationStatus: 'PENDING'
})

const validationReportJson = computed(() => detailData.value.validationReportJson ?? {})
const dependencyDigestJson = computed(() => detailData.value.dependencyDigestJson ?? {})
const snapshotJson = computed(() => detailData.value.snapshotJson ?? {})

const open = async (id: number) => {
  dialogVisible.value = true
  detailLoading.value = true
  activeTab.value = 'validation'
  try {
    detailData.value = await ReleaseApi.getSceneRelease(id)
  } finally {
    detailLoading.value = false
  }
}

const openWithData = (data: ReleaseApi.SceneReleaseVO) => {
  dialogVisible.value = true
  detailLoading.value = false
  activeTab.value = 'validation'
  detailData.value = data
}

defineExpose({ open, openWithData })
</script>
