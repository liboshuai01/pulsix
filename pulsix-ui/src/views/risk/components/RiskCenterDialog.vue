<script setup lang="ts">
import { isNumber } from '@/utils/is'
import { propTypes } from '@/utils/propTypes'

// Risk module detail/edit popups should use this centered dialog instead of a drawer.
defineOptions({
  name: 'RiskCenterDialog',
  inheritAttrs: false
})

const attrs = useAttrs()
const slots = useSlots()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const props = defineProps({
  modelValue: propTypes.bool.def(false),
  title: propTypes.string.def(''),
  width: propTypes.oneOfType([String, Number]).def('960px'),
  maxHeight: propTypes.oneOfType([String, Number]).def('calc(100vh - 240px)'),
  fullscreen: propTypes.bool.def(true),
  scroll: propTypes.bool.def(true),
  closeOnClickModal: propTypes.bool.def(false)
})

const isFullscreen = ref(false)

const resolvedWidth = computed(() =>
  isNumber(props.width) ? `${props.width}px` : props.width
)

const resolvedMaxHeight = computed(() =>
  isNumber(props.maxHeight) ? `${props.maxHeight}px` : props.maxHeight
)

const bodyMaxHeight = computed(() => {
  if (isFullscreen.value) {
    return `calc(100vh - ${slots.footer ? 126 : 90}px)`
  }
  return resolvedMaxHeight.value
})

const updateVisible = (value: boolean) => {
  emit('update:modelValue', value)
}

const closeDialog = () => {
  updateVisible(false)
}

const toggleFullscreen = () => {
  if (!props.fullscreen) {
    return
  }
  isFullscreen.value = !isFullscreen.value
}

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) {
      isFullscreen.value = false
    }
  }
)
</script>

<template>
  <ElDialog
    v-bind="attrs"
    :model-value="modelValue"
    :width="resolvedWidth"
    :fullscreen="isFullscreen"
    :close-on-click-modal="closeOnClickModal"
    append-to-body
    align-center
    destroy-on-close
    draggable
    lock-scroll
    overflow
    class="risk-center-dialog"
    @update:model-value="updateVisible"
    @close="closeDialog"
  >
    <template #header="{ close }">
      <div class="risk-center-dialog__header">
        <div class="risk-center-dialog__title">
          <slot name="title">
            {{ title }}
          </slot>
        </div>
        <div class="risk-center-dialog__actions">
          <Icon
            v-if="fullscreen"
            class="risk-center-dialog__action"
            :icon="isFullscreen ? 'radix-icons:exit-full-screen' : 'radix-icons:enter-full-screen'"
            color="var(--el-color-info)"
            hover-color="var(--el-color-primary)"
            @click="toggleFullscreen"
          />
          <Icon
            class="risk-center-dialog__action"
            icon="ep:close"
            color="var(--el-color-info)"
            hover-color="var(--el-color-primary)"
            @click.stop="close"
          />
        </div>
      </div>
    </template>

    <ElScrollbar v-if="scroll" :max-height="bodyMaxHeight">
      <div class="risk-center-dialog__content">
        <slot ></slot>
      </div>
    </ElScrollbar>
    <div v-else class="risk-center-dialog__content">
      <slot ></slot>
    </div>

    <template v-if="slots.footer" #footer>
      <div class="risk-center-dialog__footer">
        <slot name="footer" ></slot>
      </div>
    </template>
  </ElDialog>
</template>

<style scoped lang="scss">
:deep(.risk-center-dialog) {
  margin: 0;
  max-width: calc(100vw - 32px);
}

:deep(.risk-center-dialog .el-dialog__header) {
  padding: 0;
  margin-right: 0;
  border-bottom: 1px solid var(--el-border-color);
}

:deep(.risk-center-dialog .el-dialog__body) {
  padding: 0;
}

:deep(.risk-center-dialog .el-dialog__footer) {
  padding: 0;
  border-top: 1px solid var(--el-border-color);
}

.risk-center-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 54px;
  padding: 0 16px;
  gap: 12px;
}

.risk-center-dialog__title {
  min-width: 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 22px;
  color: var(--el-text-color-primary);
}

.risk-center-dialog__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.risk-center-dialog__action {
  cursor: pointer;
}

.risk-center-dialog__content {
  padding: 16px 20px;
}

.risk-center-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
}

@media (max-width: 768px) {
  :deep(.risk-center-dialog) {
    max-width: calc(100vw - 24px);
  }

  .risk-center-dialog__content,
  .risk-center-dialog__footer {
    padding: 16px;
  }
}
</style>
