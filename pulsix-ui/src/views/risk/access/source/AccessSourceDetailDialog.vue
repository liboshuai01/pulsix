<template>
  <RiskCenterDialog
    v-model="dialogVisible"
    title="接入源详情"
    width="1080px"
    max-height="calc(100vh - 220px)"
    scroll
  >
    <div v-loading="detailLoading" class="risk-access-source-detail">
      <el-descriptions title="基础信息" :column="2" :label-width="120" border>
        <el-descriptions-item label="接入源名称">
          {{ detail?.sourceName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="接入源编码">
          {{ detail?.sourceCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="接入类型">
          <dict-tag
            v-if="detail?.sourceType"
            :type="DICT_TYPE.RISK_ACCESS_SOURCE_TYPE"
            :value="detail.sourceType"
          />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="标准 Topic">
          <dict-tag
            v-if="detail?.topicName"
            :type="DICT_TYPE.RISK_ACCESS_TOPIC_NAME"
            :value="detail.topicName"
          />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="限流 QPS">
          {{ detail?.rateLimitQps ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <dict-tag v-if="detail" :type="DICT_TYPE.COMMON_STATUS" :value="detail.status" />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="允许场景" :span="2">
          <div v-if="detail?.allowedSceneCodes?.length" class="risk-access-source-detail__tag-list">
            <el-tag
              v-for="sceneCode in detail.allowedSceneCodes"
              :key="sceneCode"
              effect="plain"
              class="mr-6px mb-6px"
            >
              {{ sceneCode }}
            </el-tag>
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="IP 白名单" :span="2">
          <div v-if="detail?.ipWhitelist?.length" class="risk-access-source-detail__tag-list">
            <el-tag
              v-for="item in detail.ipWhitelist"
              :key="item"
              type="info"
              effect="plain"
              class="mr-6px mb-6px"
            >
              {{ item }}
            </el-tag>
          </div>
          <el-tag v-else type="success" effect="plain">允许所有 IP</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          <span class="risk-access-source-detail__multiline">{{ detail?.description || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div class="mt-16px flex justify-end">
        <el-button
          type="primary"
          plain
          :disabled="!detail?.sourceCode"
          @click="goToAccessMapping(detail?.sourceCode)"
          v-hasPermi="['risk:access-mapping:query']"
        >
          配置接入映射
        </el-button>
      </div>

      <el-descriptions title="审计信息" :column="2" :label-width="120" border class="mt-18px">
        <el-descriptions-item label="创建人">
          {{ detail?.creator || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ detail?.createTime ? formatDate(detail.createTime) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="更新人">
          {{ detail?.updater || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ detail?.updateTime ? formatDate(detail.updateTime) : '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </RiskCenterDialog>
</template>

<script setup lang="ts">
import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as AccessSourceApi from '@/api/risk/access-source'
import RiskCenterDialog from '../../components/RiskCenterDialog.vue'

defineOptions({ name: 'RiskAccessSourceDetailDialog' })

const router = useRouter()
const dialogVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<AccessSourceApi.AccessSourceVO>()

const open = async (id: number) => {
  dialogVisible.value = true
  detail.value = undefined
  detailLoading.value = true
  try {
    detail.value = await AccessSourceApi.getAccessSource(id)
  } finally {
    detailLoading.value = false
  }
}

const goToAccessMapping = (sourceCode?: string) => {
  if (!sourceCode) {
    return
  }
  router.push({ path: '/risk/access/mapping/index', query: { sourceCode } })
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.risk-access-source-detail {
  min-height: 240px;
}

.risk-access-source-detail__multiline {
  white-space: pre-wrap;
  word-break: break-word;
}

.risk-access-source-detail__tag-list {
  display: flex;
  flex-wrap: wrap;
}
</style>
