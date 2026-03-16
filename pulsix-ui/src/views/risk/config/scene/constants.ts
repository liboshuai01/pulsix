export const SCENE_RUNTIME_MODE_OPTIONS = [
  {
    label: '异步决策',
    value: 'ASYNC_DECISION'
  }
]

export const getSceneRuntimeModeLabel = (value?: string) => {
  return SCENE_RUNTIME_MODE_OPTIONS.find((item) => item.value === value)?.label || value || '-'
}
