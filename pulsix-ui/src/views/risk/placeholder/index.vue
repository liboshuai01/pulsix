<template>
  <ContentWrap>
    <div class="flex flex-wrap items-start justify-between gap-16px">
      <div class="flex-1 min-w-320px">
        <div class="flex flex-wrap items-center gap-8px">
          <Icon icon="ep:warning-filled" class="text-22px text-[var(--el-color-warning)]" />
          <span class="text-22px font-600 text-[var(--el-text-color-primary)]">{{ page.title }}</span>
          <el-tag v-for="stage in page.stages" :key="stage" effect="plain" round>
            {{ stage }}
          </el-tag>
        </div>
        <p class="mt-12px mb-0 text-14px leading-24px text-[var(--el-text-color-secondary)]">
          {{ page.summary }}
        </p>
      </div>
      <el-alert
        title="S00 已交付风控菜单骨架；当前页面用于承接后续阶段开发。"
        type="info"
        :closable="false"
        show-icon
        class="w-full max-w-420px"
      />
    </div>
  </ContentWrap>

  <ContentWrap>
    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" header="页面信息">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="当前路由">
              {{ currentRoute.path }}
            </el-descriptions-item>
            <el-descriptions-item label="权限前缀">
              <el-tag type="warning" effect="light">{{ page.permissionPrefix }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="核心表">
              <el-space wrap>
                <el-tag v-for="table in page.tables" :key="table" effect="plain">
                  {{ table }}
                </el-tag>
              </el-space>
            </el-descriptions-item>
            <el-descriptions-item label="参考文档">
              <div class="flex flex-col gap-6px text-13px leading-20px break-all">
                <span v-for="doc in page.docRefs" :key="doc">{{ doc }}</span>
              </div>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never" header="阶段验收目标">
          <div class="flex flex-col gap-12px">
            <div class="text-14px leading-22px text-[var(--el-text-color-secondary)]">
              当前阶段只落菜单骨架、空页面入口和权限前缀，不提前实现业务 CRUD。
            </div>
            <div
              v-for="(item, index) in page.acceptance"
              :key="item"
              class="flex items-start gap-8px rounded-8px bg-[var(--el-fill-color-light)] px-12px py-10px"
            >
              <el-tag size="small" type="success" effect="plain">{{ index + 1 }}</el-tag>
              <span class="text-14px leading-22px">{{ item }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </ContentWrap>

  <ContentWrap>
    <el-card shadow="never" header="权限样例">
      <div class="flex flex-wrap gap-12px">
        <el-button
          v-for="sample in permissionSamples"
          :key="sample.permission"
          v-hasPermi="[sample.permission]"
          @click="handlePermissionSample(sample.permission)"
        >
          {{ sample.label }}
        </el-button>
      </div>
      <div class="mt-14px text-13px leading-22px text-[var(--el-text-color-secondary)]">
        按钮仅用于验证 `system_menu` 按钮权限与 `risk:*` 前缀约定是否生效，具体业务逻辑会在对应阶段补齐。
      </div>
    </el-card>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { riskPlaceholderDefault, riskPlaceholderRegistry } from './registry'

defineOptions({ name: 'RiskPlaceholderPage' })

const route = useRoute()
const message = useMessage()

const pageCode = computed(() => {
  const query = route.meta?.query as Record<string, string> | undefined
  return query?.code || 'default'
})

const page = computed(() => riskPlaceholderRegistry[pageCode.value] || riskPlaceholderDefault)

const permissionSamples = computed(() => {
  return page.value.permissionSamples.map((sample) => ({
    label: sample.label,
    permission: `${page.value.permissionPrefix}:${sample.action}`
  }))
})

const handlePermissionSample = (permission: string) => {
  message.success(`已命中占位权限：${permission}`)
}

const currentRoute = computed(() => route)
</script>

