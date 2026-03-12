<template>
  <Dialog v-model="dialogVisible" title="标准事件预览" width="1100px">
    <el-alert
      title="当前阶段预览仅基于事件字段定义、同名字段/fieldPath/defaultValue 生成；原始字段映射将在 S06 实现。"
      type="info"
      show-icon
      class="mb-12px"
    />

    <div class="mb-12px flex flex-wrap gap-8px">
      <el-tag effect="plain">样例编码：{{ previewData?.sampleCode || '-' }}</el-tag>
      <el-tag effect="plain">样例名称：{{ previewData?.sampleName || '-' }}</el-tag>
      <el-tag type="warning" effect="plain">样例类型：{{ getRiskEventSampleTypeLabel(previewData?.sampleType) }}</el-tag>
    </div>

    <el-alert
      v-if="previewData?.missingRequiredFields?.length"
      :title="`缺失必填字段：${previewData.missingRequiredFields.join('、')}`"
      type="warning"
      show-icon
      class="mb-12px"
    />
    <el-alert
      v-if="previewData?.defaultedFields?.length"
      :title="`已用默认值补齐字段：${previewData.defaultedFields.join('、')}`"
      type="success"
      show-icon
      class="mb-12px"
    />

    <el-row :gutter="16">
      <el-col :span="12">
        <div class="mb-8px font-600">样例原文</div>
        <JsonEditor v-model="rawSampleJson" mode="view" height="420px" />
      </el-col>
      <el-col :span="12">
        <div class="mb-8px font-600">标准事件预览</div>
        <JsonEditor v-model="standardEventJson" mode="view" height="420px" />
      </el-col>
    </el-row>
  </Dialog>
</template>

<script lang="ts" setup>
import * as EventSampleApi from '@/api/risk/eventSample'
import { getRiskEventSampleTypeLabel } from './constants'

defineOptions({ name: 'RiskEventSamplePreview' })

const dialogVisible = ref(false)
const previewData = ref<EventSampleApi.EventSamplePreviewVO>()
const rawSampleJson = ref<Record<string, any>>({})
const standardEventJson = ref<Record<string, any>>({})

const open = async (id: number) => {
  dialogVisible.value = true
  const data = await EventSampleApi.previewEventSample(id)
  previewData.value = data
  rawSampleJson.value = data.sampleJson || {}
  standardEventJson.value = data.standardEventJson || {}
}

defineExpose({ open })
</script>
