<template>
  <Dialog v-model="dialogVisible" title="场景详情" width="780px">
    <el-descriptions v-loading="detailLoading" :column="2" border>
      <el-descriptions-item label="场景编号">{{ detailData.id }}</el-descriptions-item>
      <el-descriptions-item label="场景编码">{{ detailData.sceneCode }}</el-descriptions-item>
      <el-descriptions-item label="场景名称">{{ detailData.sceneName }}</el-descriptions-item>
      <el-descriptions-item label="场景状态">
        <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="detailData.status" />
      </el-descriptions-item>
      <el-descriptions-item label="场景类型">
        {{ getRiskSceneTypeLabel(detailData.sceneType) }}
      </el-descriptions-item>
      <el-descriptions-item label="接入模式">
        {{ getRiskSceneAccessModeLabel(detailData.accessMode) }}
      </el-descriptions-item>
      <el-descriptions-item label="默认事件编码">
        {{ detailData.defaultEventCode || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="默认策略编码">
        {{ detailData.defaultPolicyCode || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="标准事件 Topic">
        {{ detailData.standardTopicName || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="决策结果 Topic">
        {{ detailData.decisionTopicName || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="创建者">{{ detailData.creator || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">
        {{ detailData.createTime ? formatDate(detailData.createTime) : '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="更新者">{{ detailData.updater || '-' }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">
        {{ detailData.updateTime ? formatDate(detailData.updateTime) : '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="场景说明" :span="2">
        <div class="whitespace-pre-wrap leading-22px">{{ detailData.description || '-' }}</div>
      </el-descriptions-item>
    </el-descriptions>
  </Dialog>
</template>

<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as SceneApi from '@/api/risk/scene'
import { getRiskSceneAccessModeLabel, getRiskSceneTypeLabel } from './constants'

defineOptions({ name: 'RiskSceneDetail' })

const dialogVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<SceneApi.SceneVO>({
  id: undefined,
  sceneCode: '',
  sceneName: '',
  sceneType: '',
  accessMode: '',
  status: 0
})

const open = async (id: number) => {
  dialogVisible.value = true
  detailLoading.value = true
  try {
    detailData.value = await SceneApi.getScene(id)
  } finally {
    detailLoading.value = false
  }
}
defineExpose({ open })
</script>

