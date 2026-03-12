<template>
  <Dialog v-model="dialogVisible" title="接入源详情" width="980px">
    <el-descriptions v-loading="detailLoading" :column="2" border>
      <el-descriptions-item label="接入源编号">{{ detailData.id }}</el-descriptions-item>
      <el-descriptions-item label="接入源编码">{{ detailData.sourceCode }}</el-descriptions-item>
      <el-descriptions-item label="接入源名称">{{ detailData.sourceName }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="detailData.status" />
      </el-descriptions-item>
      <el-descriptions-item label="接入方式">
        {{ getRiskIngestSourceTypeLabel(detailData.sourceType) }}
      </el-descriptions-item>
      <el-descriptions-item label="鉴权方式">
        {{ getRiskIngestSourceAuthTypeLabel(detailData.authType) }}
      </el-descriptions-item>
      <el-descriptions-item label="标准 Topic">
        {{ detailData.standardTopicName || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="异常 Topic">
        {{ detailData.errorTopicName || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="限流 QPS">
        {{ detailData.rateLimitQps ?? '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="场景范围">
        <template v-if="detailData.sceneScopeJson?.length">
          <el-tag v-for="item in detailData.sceneScopeJson" :key="item" class="mr-6px mb-4px" effect="plain">
            {{ item }}
          </el-tag>
        </template>
        <span v-else>全部场景（未限制）</span>
      </el-descriptions-item>
      <el-descriptions-item label="创建者">{{ detailData.creator || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">
        {{ detailData.createTime ? formatDate(detailData.createTime) : '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="更新者">{{ detailData.updater || '-' }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">
        {{ detailData.updateTime ? formatDate(detailData.updateTime) : '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="接入源说明" :span="2">
        <div class="whitespace-pre-wrap leading-22px">{{ detailData.description || '-' }}</div>
      </el-descriptions-item>
      <el-descriptions-item label="鉴权配置" :span="2">
        <JsonEditor v-model="authConfigJson" mode="view" height="280px" />
      </el-descriptions-item>
    </el-descriptions>
  </Dialog>
</template>

<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as IngestSourceApi from '@/api/risk/ingestSource'
import { getRiskIngestSourceAuthTypeLabel, getRiskIngestSourceTypeLabel } from './constants'

defineOptions({ name: 'RiskIngestSourceDetail' })

const dialogVisible = ref(false)
const detailLoading = ref(false)
const authConfigJson = ref<Record<string, any>>({})
const detailData = ref<IngestSourceApi.IngestSourceVO>({
  id: undefined,
  sourceCode: '',
  sourceName: '',
  sourceType: '',
  authType: '',
  authConfigJson: {},
  sceneScopeJson: [],
  standardTopicName: '',
  errorTopicName: '',
  rateLimitQps: 0,
  status: 0
})

const open = async (id: number) => {
  dialogVisible.value = true
  detailLoading.value = true
  try {
    detailData.value = await IngestSourceApi.getIngestSource(id)
    authConfigJson.value = detailData.value.authConfigJson || {}
  } finally {
    detailLoading.value = false
  }
}
defineExpose({ open })
</script>
