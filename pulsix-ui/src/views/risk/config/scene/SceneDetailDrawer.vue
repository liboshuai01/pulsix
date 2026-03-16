<template>
  <el-drawer v-model="drawerVisible" title="场景详情" size="560px" destroy-on-close>
    <div v-loading="detailLoading" class="risk-scene-detail">
      <el-descriptions title="基础信息" :column="1" :label-width="120" border>
        <el-descriptions-item label="场景名称">
          {{ detail?.sceneName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="场景编码">
          {{ detail?.sceneCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="运行模式">
          {{ getSceneRuntimeModeLabel(detail?.runtimeMode) }}
        </el-descriptions-item>
        <el-descriptions-item label="默认策略编码">
          {{ detail?.defaultPolicyCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <dict-tag v-if="detail" :type="DICT_TYPE.COMMON_STATUS" :value="detail.status" />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="场景描述">
          <span class="risk-scene-detail__multiline">{{ detail?.description || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-descriptions title="审计信息" :column="1" :label-width="120" border class="mt-18px">
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
  </el-drawer>
</template>

<script setup lang="ts">
import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as SceneApi from '@/api/risk/scene'
import { getSceneRuntimeModeLabel } from './constants'

defineOptions({ name: 'RiskSceneDetailDrawer' })

const drawerVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<SceneApi.SceneVO>()

const open = async (id: number) => {
  drawerVisible.value = true
  detail.value = undefined
  detailLoading.value = true
  try {
    detail.value = await SceneApi.getScene(id)
  } finally {
    detailLoading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.risk-scene-detail {
  min-height: 200px;
}

.risk-scene-detail__multiline {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
