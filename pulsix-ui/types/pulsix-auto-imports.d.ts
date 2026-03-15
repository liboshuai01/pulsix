export {}

declare global {
  const computed: typeof import('vue')['computed']
  const defineAsyncComponent: typeof import('vue')['defineAsyncComponent']
  const defineComponent: typeof import('vue')['defineComponent']
  const h: typeof import('vue')['h']
  const inject: typeof import('vue')['inject']
  const nextTick: typeof import('vue')['nextTick']
  const onBeforeUnmount: typeof import('vue')['onBeforeUnmount']
  const onMounted: typeof import('vue')['onMounted']
  const provide: typeof import('vue')['provide']
  const reactive: typeof import('vue')['reactive']
  const ref: typeof import('vue')['ref']
  const shallowRef: typeof import('vue')['shallowRef']
  const toRaw: typeof import('vue')['toRaw']
  const unref: typeof import('vue')['unref']
  const useAttrs: typeof import('vue')['useAttrs']
  const watch: typeof import('vue')['watch']
  const useCrudSchemas: typeof import('@/hooks/web/useCrudSchemas')['useCrudSchemas']
  const useI18n: typeof import('@/hooks/web/useI18n')['useI18n']
  const useMessage: typeof import('@/hooks/web/useMessage')['useMessage']

  interface Ref<T = any> {
    value: T
  }
}
