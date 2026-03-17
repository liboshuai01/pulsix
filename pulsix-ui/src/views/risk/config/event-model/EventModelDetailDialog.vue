<template>
  <RiskCenterDialog
    v-model="dialogVisible"
    title="事件模型详情"
    width="1280px"
    max-height="calc(100vh - 220px)"
    scroll
  >
    <div v-loading="detailLoading" class="risk-event-model-detail">
      <el-descriptions title="基础信息" :column="2" :label-width="120" border>
        <el-descriptions-item label="场景编码">
          {{ detail?.sceneCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="事件编码">
          {{ detail?.eventCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="事件名称">
          {{ detail?.eventName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="事件类型">
          {{ detail?.eventType || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="版本">
          {{ detail?.version ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <dict-tag v-if="detail" :type="DICT_TYPE.COMMON_STATUS" :value="detail.status" />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          <span class="risk-event-model-detail__multiline">{{ detail?.description || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-card header="接入绑定" class="mt-18px" shadow="never">
        <el-empty
          v-if="!detail?.bindingSources?.length"
          description="当前事件模型暂无接入绑定"
          :image-size="60"
        />
        <el-table v-else :data="detail.bindingSources" border>
          <el-table-column label="接入源名称" prop="sourceName" min-width="160" />
          <el-table-column label="接入源编码" prop="sourceCode" min-width="180" />
          <el-table-column label="接入类型" min-width="120">
            <template #default="{ row }">
              <dict-tag :type="DICT_TYPE.RISK_ACCESS_SOURCE_TYPE" :value="row.sourceType" />
            </template>
          </el-table-column>
          <el-table-column label="标准 Topic" min-width="180">
            <template #default="{ row }">
              <dict-tag :type="DICT_TYPE.RISK_ACCESS_TOPIC_NAME" :value="row.topicName" />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="row.status" />
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card header="字段定义" class="mt-18px" shadow="never">
        <el-table :data="detail?.fields || []" border>
          <el-table-column label="排序" prop="sortNo" width="70" align="center" />
          <el-table-column label="字段名" prop="fieldName" min-width="140" />
          <el-table-column label="显示名" prop="fieldLabel" min-width="140" />
          <el-table-column label="字段类型" min-width="120">
            <template #default="{ row }">
              {{ getEventFieldTypeLabel(row.fieldType) }}
            </template>
          </el-table-column>
          <el-table-column label="必填" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.requiredFlag === 1 ? 'danger' : 'info'" effect="plain">
                {{ row.requiredFlag === 1 ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="默认值" prop="defaultValue" min-width="150" show-overflow-tooltip />
          <el-table-column label="样例值" prop="sampleValue" min-width="180" show-overflow-tooltip />
          <el-table-column label="描述" prop="description" min-width="180" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card header="样例报文" class="mt-18px" shadow="never">
        <pre class="risk-event-model-detail__json">{{ sampleJsonText }}</pre>
      </el-card>

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
import * as EventModelApi from '@/api/risk/event-model'
import RiskCenterDialog from '../../components/RiskCenterDialog.vue'
import { getEventFieldTypeLabel } from './constants'

defineOptions({ name: 'RiskEventModelDetailDialog' })

const dialogVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<EventModelApi.EventModelVO>()
const sampleJsonText = computed(() =>
  JSON.stringify(detail.value?.sampleEventJson ?? {}, null, 2)
)

const open = async (id: number) => {
  dialogVisible.value = true
  detail.value = undefined
  detailLoading.value = true
  try {
    detail.value = await EventModelApi.getEventModel(id)
  } finally {
    detailLoading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.risk-event-model-detail {
  min-height: 240px;
}

.risk-event-model-detail__multiline,
.risk-event-model-detail__json {
  white-space: pre-wrap;
  word-break: break-word;
}

.risk-event-model-detail__json {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
}
</style>
