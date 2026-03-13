<template>
  <Dialog v-model="dialogVisible" title="审计日志详情" width="90%" :fullscreen="false" :scroll="true" max-height="78vh">
    <div v-loading="detailLoading">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="链路号">{{ detailData.traceId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属场景">{{ detailData.sceneCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="对象类型">
          <el-tag :type="getAuditBizTypeTag(detailData.bizType)" effect="plain">
            {{ getAuditBizTypeLabel(detailData.bizType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="对象编码">{{ detailData.bizCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="动作类型">
          <el-tag :type="getAuditActionTag(detailData.actionType)" effect="plain">
            {{ getAuditActionLabel(detailData.actionType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作人">
          {{ detailData.operatorName || '-' }}
          <span v-if="detailData.operatorId">（#{{ detailData.operatorId }}）</span>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ detailData.operateTime ? formatDate(detailData.operateTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createTime ? formatDate(detailData.createTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="变更说明" :span="3">
          <div class="whitespace-pre-wrap leading-22px">{{ detailData.remark || '-' }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <ContentWrap class="mt-16px">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="变更前快照" name="before">
            <JsonEditor :model-value="ensureObject(detailData.beforeJson)" mode="view" height="420px" />
          </el-tab-pane>
          <el-tab-pane label="变更后快照" name="after">
            <JsonEditor :model-value="ensureObject(detailData.afterJson)" mode="view" height="420px" />
          </el-tab-pane>
        </el-tabs>
      </ContentWrap>
    </div>
  </Dialog>
</template>

<script lang="ts" setup>
import { formatDate } from '@/utils/formatTime'
import * as AuditLogApi from '@/api/risk/audit-log'
import {
  ensureObject,
  getAuditActionLabel,
  getAuditActionTag,
  getAuditBizTypeLabel,
  getAuditBizTypeTag
} from './constants'

defineOptions({ name: 'RiskAuditLogDetailDialog' })

const dialogVisible = ref(false)
const detailLoading = ref(false)
const activeTab = ref('before')

const createDefaultDetailData = (): AuditLogApi.AuditLogDetailVO => ({
  id: undefined,
  traceId: '',
  sceneCode: '',
  operatorId: undefined,
  operatorName: '',
  bizType: '',
  bizCode: '',
  actionType: '',
  remark: '',
  operateTime: undefined,
  createTime: undefined,
  beforeJson: {},
  afterJson: {}
})

const detailData = ref<AuditLogApi.AuditLogDetailVO>(createDefaultDetailData())

const normalizeDetailData = (data?: Partial<AuditLogApi.AuditLogDetailVO>): AuditLogApi.AuditLogDetailVO => ({
  ...createDefaultDetailData(),
  ...data,
  beforeJson: ensureObject(data?.beforeJson),
  afterJson: ensureObject(data?.afterJson)
})

const open = async (id: number) => {
  dialogVisible.value = true
  detailLoading.value = true
  activeTab.value = 'before'
  try {
    detailData.value = normalizeDetailData(await AuditLogApi.getAuditLog(id))
  } finally {
    detailLoading.value = false
  }
}

defineExpose({ open })
</script>
