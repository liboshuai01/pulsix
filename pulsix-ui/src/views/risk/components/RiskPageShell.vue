<template>
  <ContentWrap>
    <div class="risk-page-shell">
      <div class="risk-page-shell__header">
        <div class="risk-page-shell__copy">
          <p class="risk-page-shell__eyebrow">{{ props.groupTitle }}</p>
          <h1 class="risk-page-shell__title">{{ props.pageTitle }}</h1>
          <p class="risk-page-shell__description">{{ props.pageDescription }}</p>
        </div>
        <el-tag type="warning" effect="plain" round>
          {{ props.statusText }}
        </el-tag>
      </div>
    </div>
  </ContentWrap>

  <ContentWrap>
    <el-row :gutter="16">
      <el-col :xs="24" :md="14">
        <el-card shadow="never" class="risk-page-shell__card">
          <template #header>
            <span>当前阶段</span>
          </template>
          <p class="risk-page-shell__text">
            已完成菜单、路由和页面占位接入，当前页面可以直接作为联调入口，并承接后续真实功能开发。
          </p>
          <div class="risk-page-shell__chips">
            <el-tag>菜单可见</el-tag>
            <el-tag type="success">页面可进入</el-tag>
            <el-tag type="info">刷新不丢路由</el-tag>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="risk-page-shell__card">
          <template #header>
            <span>下一步开发</span>
          </template>
          <p class="risk-page-shell__text">
            {{ props.nextStep }}
          </p>
        </el-card>
      </el-col>
    </el-row>
  </ContentWrap>

  <ContentWrap>
    <el-card shadow="never" class="risk-page-shell__card">
      <template #header>
        <span>验收样例场景</span>
      </template>
      <div class="risk-page-shell__chips">
        <el-tag
          v-for="scene in props.sampleScenes"
          :key="scene"
          type="primary"
          effect="plain"
        >
          {{ scene }}
        </el-tag>
      </div>
      <p class="risk-page-shell__text">
        当前页面骨架默认围绕异步闭环场景验收，样例数据来自 `docs/sql/pulsix-risk.sql` 中的初始化种子。
      </p>
    </el-card>
  </ContentWrap>
</template>

<script setup lang="ts">
interface Props {
  groupTitle: string
  pageTitle: string
  pageDescription: string
  nextStep: string
  sampleScenes?: string[]
  statusText?: string
}

const props = withDefaults(defineProps<Props>(), {
  sampleScenes: () => [],
  statusText: '页面骨架已就位，业务能力待接入'
})
</script>

<style scoped lang="scss">
.risk-page-shell {
  padding: 4px 2px;
}

.risk-page-shell__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.risk-page-shell__copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
}

.risk-page-shell__eyebrow {
  margin: 0;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.risk-page-shell__title {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 28px;
  line-height: 1.2;
}

.risk-page-shell__description {
  margin: 0;
  color: var(--el-text-color-regular);
  font-size: 14px;
  line-height: 1.7;
}

.risk-page-shell__card {
  height: 100%;
}

.risk-page-shell__text {
  margin: 0;
  color: var(--el-text-color-regular);
  line-height: 1.8;
}

.risk-page-shell__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .risk-page-shell__header {
    flex-direction: column;
  }

  .risk-page-shell__title {
    font-size: 24px;
  }
}
</style>
