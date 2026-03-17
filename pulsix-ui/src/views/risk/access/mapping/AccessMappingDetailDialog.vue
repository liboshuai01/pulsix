<template>
  <RiskCenterDialog
    v-model="dialogVisible"
    title="接入映射详情"
    width="min(1480px, calc(100vw - 48px))"
    max-height="calc(100vh - 220px)"
    scroll
  >
    <div v-loading="detailLoading" class="risk-access-mapping-detail">
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
        <el-descriptions-item label="标准 eventType">
          {{ detail?.eventType || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="接入源编码">
          {{ detail?.sourceCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="接入源名称">
          {{ detail?.sourceName || '-' }}
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
          {{ detail?.topicName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="原始字段数">
          {{ detail?.rawFieldCount ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="映射规则数">
          {{ detail?.mappingRuleCount ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          <span class="risk-access-mapping-detail__multiline">
            {{ detail?.description || '-' }}
          </span>
        </el-descriptions-item>
      </el-descriptions>

      <el-row :gutter="16" class="mt-18px">
        <el-col :xs="24" :lg="12">
          <el-card header="原始样例报文" shadow="never">
            <pre class="risk-access-mapping-detail__json">{{ rawSampleText }}</pre>
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="12">
          <el-card header="样例请求头" shadow="never">
            <pre class="risk-access-mapping-detail__json">{{ sampleHeadersText }}</pre>
          </el-card>
        </el-col>
      </el-row>

      <el-card header="原始字段定义" class="mt-18px" shadow="never">
        <el-table :data="detail?.rawFields || []" border>
          <el-table-column label="排序" prop="sortNo" width="72" align="center" />
          <el-table-column label="字段名" prop="fieldName" min-width="130" />
          <el-table-column label="显示名" prop="fieldLabel" min-width="130" />
          <el-table-column label="字段路径" prop="fieldPath" min-width="180" />
          <el-table-column label="字段类型" min-width="120">
            <template #default="{ row }">
              {{ getEventFieldTypeLabel(row.fieldType) }}
            </template>
          </el-table-column>
          <el-table-column label="必填" width="86" align="center">
            <template #default="{ row }">
              <el-tag :type="row.requiredFlag === 1 ? 'danger' : 'info'" effect="plain">
                {{ row.requiredFlag === 1 ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="样例值" prop="sampleValue" min-width="160" show-overflow-tooltip />
          <el-table-column label="描述" prop="description" min-width="180" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card header="标准化规则" class="mt-18px" shadow="never">
        <el-table :data="detail?.mappingRules || []" border>
          <el-table-column label="目标字段" prop="targetFieldName" min-width="140" />
          <el-table-column label="映射方式" min-width="110">
            <template #default="{ row }">
              {{ getAccessMappingTypeLabel(row.mappingType) }}
            </template>
          </el-table-column>
          <el-table-column label="源字段路径" prop="sourceFieldPath" min-width="160" show-overflow-tooltip />
          <el-table-column label="常量值" prop="constantValue" min-width="140" show-overflow-tooltip />
          <el-table-column label="脚本引擎" min-width="120">
            <template #default="{ row }">
              {{ getAccessScriptEngineLabel(row.scriptEngine) }}
            </template>
          </el-table-column>
          <el-table-column label="脚本内容" prop="scriptContent" min-width="220" show-overflow-tooltip />
          <el-table-column label="时间格式" prop="timePattern" min-width="150" show-overflow-tooltip />
          <el-table-column label="枚举映射" min-width="180">
            <template #default="{ row }">
              <span class="risk-access-mapping-detail__multiline">
                {{ formatJsonInline(row.enumMappingJson) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="描述" prop="description" min-width="180" show-overflow-tooltip />
        </el-table>
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
import * as AccessMappingApi from '@/api/risk/access-mapping'
import RiskCenterDialog from '../../components/RiskCenterDialog.vue'
import {
  getAccessMappingTypeLabel,
  getAccessScriptEngineLabel,
  getEventFieldTypeLabel
} from './constants'

defineOptions({ name: 'RiskAccessMappingDetailDialog' })

const dialogVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<AccessMappingApi.AccessMappingVO>()

const rawSampleText = computed(() => JSON.stringify(detail.value?.rawSampleJson ?? {}, null, 2))
const sampleHeadersText = computed(() =>
  JSON.stringify(detail.value?.sampleHeadersJson ?? {}, null, 2)
)

const formatJsonInline = (value?: Record<string, string>) => {
  if (!value || !Object.keys(value).length) {
    return '-'
  }
  return JSON.stringify(value)
}

const open = async (id: number) => {
  dialogVisible.value = true
  detail.value = undefined
  detailLoading.value = true
  try {
    detail.value = await AccessMappingApi.getAccessMapping(id)
  } finally {
    detailLoading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.risk-access-mapping-detail {
  min-height: 280px;
}

.risk-access-mapping-detail__json,
.risk-access-mapping-detail__multiline {
  white-space: pre-wrap;
  word-break: break-word;
}

.risk-access-mapping-detail__json {
  margin: 0;
  min-height: 180px;
  font-size: 13px;
  line-height: 1.6;
}
</style>
