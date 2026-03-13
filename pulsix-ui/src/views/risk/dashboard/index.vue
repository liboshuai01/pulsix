<template>
  <ContentWrap>
    <el-form ref="queryFormRef" class="-mb-15px" :model="queryParams" :inline="true" label-width="82px">
      <el-form-item label="所属场景" prop="sceneCode">
        <el-input v-model="queryParams.sceneCode" class="!w-220px" clearable placeholder="请输入场景编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="统计粒度" prop="statGranularity">
        <el-select v-model="queryParams.statGranularity" class="!w-180px" placeholder="请选择统计粒度">
          <el-option
            v-for="item in dashboardGranularityOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="统计时间" prop="statTime">
        <el-date-picker
          v-model="queryParams.statTime"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="!w-360px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="getSummary" v-hasPermi="['risk:dashboard:refresh']">
          <Icon icon="ep:refresh-right" class="mr-5px" /> 刷新
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['risk:dashboard:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap v-loading="loading">
    <el-alert
      :title="`S19 基础监控：展示事件量、决策量、动作占比与 P95 延迟；当前最新统计点为 ${latestStatTimeText}。`"
      type="info"
      :closable="false"
      class="mb-16px"
    />

    <div class="grid grid-cols-4 gap-12px">
      <el-card shadow="hover">
        <div class="text-14px text-#909399">最新事件量</div>
        <div class="mt-12px text-28px font-700">{{ formatDashboardCount(summary.latestEventInTotal) }}</div>
      </el-card>
      <el-card shadow="hover">
        <div class="text-14px text-#909399">最新决策量</div>
        <div class="mt-12px text-28px font-700">{{ formatDashboardCount(summary.latestDecisionTotal) }}</div>
      </el-card>
      <el-card shadow="hover">
        <div class="text-14px text-#909399">动作占比</div>
        <div class="mt-12px text-20px font-700 leading-32px">
          P {{ formatDashboardPercent(summary.latestPassRate) }} / Rv {{ formatDashboardPercent(summary.latestReviewRate) }} / Rj {{ formatDashboardPercent(summary.latestRejectRate) }}
        </div>
      </el-card>
      <el-card shadow="hover">
        <div class="text-14px text-#909399">P95 延迟</div>
        <div class="mt-12px text-28px font-700">{{ formatDashboardLatency(summary.latestP95LatencyMs) }}</div>
      </el-card>
    </div>
  </ContentWrap>

  <el-row :gutter="16">
    <el-col :span="16">
      <ContentWrap v-loading="loading">
        <template #header>
          <span>趋势图</span>
        </template>
        <Echart :height="420" :options="trendChartOptions" />
      </ContentWrap>
    </el-col>
    <el-col :span="8">
      <ContentWrap v-loading="loading">
        <template #header>
          <span>动作占比</span>
        </template>
        <Echart :height="420" :options="actionRatioChartOptions" />
      </ContentWrap>
    </el-col>
  </el-row>
</template>

<script lang="ts" setup>
import type {
  DefaultLabelFormatterCallbackParams,
  EChartsOption,
  TooltipComponentFormatterCallbackParams
} from 'echarts'
import { formatDate } from '@/utils/formatTime'
import download from '@/utils/download'
import * as DashboardApi from '@/api/risk/dashboard'
import {
  dashboardGranularityOptions,
  formatDashboardCount,
  formatDashboardLatency,
  formatDashboardPercent
} from './constants'

defineOptions({ name: 'RiskDashboard' })

const message = useMessage()

const loading = ref(true)
const exportLoading = ref(false)
const queryFormRef = ref()
const queryParams = reactive<DashboardApi.DashboardQueryReqVO>({
  sceneCode: 'TRADE_RISK',
  statGranularity: '1m',
  statTime: undefined
})
const summary = ref<DashboardApi.DashboardSummaryVO>({
  sceneCode: 'TRADE_RISK',
  statGranularity: '1m',
  latestEventInTotal: 0,
  latestDecisionTotal: 0,
  latestPassRate: 0,
  latestReviewRate: 0,
  latestRejectRate: 0,
  latestP95LatencyMs: 0,
  trends: []
})

const latestStatTimeText = computed(() => {
  return summary.value.latestStatTime ? formatDate(summary.value.latestStatTime, 'YYYY-MM-DD HH:mm:ss') : '暂无数据'
})

const resolveChartNumericValue = (value: unknown): number => {
  if (typeof value === 'number') {
    return value
  }
  if (typeof value === 'string') {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : 0
  }
  if (Array.isArray(value)) {
    return resolveChartNumericValue(value[0])
  }
  if (value && typeof value === 'object' && 'value' in value) {
    return resolveChartNumericValue((value as { value?: unknown }).value)
  }
  return 0
}

const trendChartOptions = computed<EChartsOption>(() => {
  const trends = summary.value.trends || []
  return {
    grid: { left: 20, right: 30, bottom: 20, top: 50, containLabel: true },
    legend: { top: 10, data: ['事件量', '决策量', 'P95 延迟'] },
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trends.map((item) => formatDate(item.statTime, 'HH:mm'))
    },
    yAxis: [
      { type: 'value', name: '数量' },
      { type: 'value', name: 'ms' }
    ],
    series: [
      {
        name: '事件量',
        type: 'line',
        smooth: true,
        areaStyle: {},
        data: trends.map((item) => Number(item.eventInTotal || 0))
      },
      {
        name: '决策量',
        type: 'line',
        smooth: true,
        areaStyle: {},
        data: trends.map((item) => Number(item.decisionTotal || 0))
      },
      {
        name: 'P95 延迟',
        type: 'bar',
        yAxisIndex: 1,
        data: trends.map((item) => Number(item.p95LatencyMs || 0))
      }
    ]
  }
})

const actionRatioChartOptions = computed<EChartsOption>(() => {
  const passRate = Number(summary.value.latestPassRate || 0)
  const reviewRate = Number(summary.value.latestReviewRate || 0)
  const rejectRate = Number(summary.value.latestRejectRate || 0)
  return {
    tooltip: {
      trigger: 'item',
      formatter: (params: TooltipComponentFormatterCallbackParams) => {
        const item = Array.isArray(params) ? params[0] : params
        const value = resolveChartNumericValue(item?.value)
        return `${item?.name || '-'}: ${formatDashboardPercent(value / 100)}`
      }
    },
    legend: {
      bottom: 10,
      left: 'center'
    },
    series: [
      {
        name: '动作占比',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        label: {
          show: true,
          formatter: (params: DefaultLabelFormatterCallbackParams) => {
            const value = resolveChartNumericValue(params.value)
            return `${params.name}\n${value.toFixed(2)}%`
          }
        },
        data: [
          { name: 'PASS', value: passRate * 100 },
          { name: 'REVIEW', value: reviewRate * 100 },
          { name: 'REJECT', value: rejectRate * 100 }
        ]
      }
    ]
  }
})

const getSummary = async () => {
  loading.value = true
  try {
    summary.value = await DashboardApi.getDashboardSummary(queryParams)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  getSummary()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  Object.assign(queryParams, {
    sceneCode: 'TRADE_RISK',
    statGranularity: '1m',
    statTime: undefined
  })
  getSummary()
}

const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await DashboardApi.exportDashboard(queryParams)
    download.excel(data, '监控大盘趋势.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => {
  getSummary()
})
</script>
