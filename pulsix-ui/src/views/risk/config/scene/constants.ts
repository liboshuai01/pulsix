export const SCENE_RUNTIME_MODE_OPTIONS = [
  {
    label: '异步决策',
    value: 'ASYNC_DECISION'
  }
]

export const SCENE_LOG_LEVEL_OPTIONS = [
  {
    label: '完整日志',
    value: 'FULL'
  }
]

export const getSceneRuntimeModeLabel = (value?: string) => {
  return SCENE_RUNTIME_MODE_OPTIONS.find((item) => item.value === value)?.label || value || '-'
}

export const getSceneLogLevelLabel = (value?: string) => {
  return SCENE_LOG_LEVEL_OPTIONS.find((item) => item.value === value)?.label || value || '-'
}
