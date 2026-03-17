export const EVENT_FIELD_TYPE_OPTIONS = [
  { label: '字符串', value: 'STRING' },
  { label: '整数', value: 'INTEGER' },
  { label: '长整数', value: 'LONG' },
  { label: '小数', value: 'DECIMAL' },
  { label: '布尔', value: 'BOOLEAN' },
  { label: '日期时间', value: 'DATETIME' },
  { label: 'JSON', value: 'JSON' }
]

export const ACCESS_MAPPING_TYPE_OPTIONS = [
  { label: '源字段', value: 'SOURCE_FIELD' },
  { label: '常量', value: 'CONSTANT' },
  { label: '表达式', value: 'SCRIPT' }
]

export const ACCESS_SCRIPT_ENGINE_OPTIONS = [
  { label: 'SpEL 表达式', value: 'EXPRESSION' },
  { label: 'Groovy（预留）', value: 'GROOVY', disabled: true }
]

export const FIELD_SOURCE_LABEL_MAP: Record<string, string> = {
  MAPPING: '映射规则',
  EVENT_DEFAULT: '事件默认值',
  SYSTEM_FILL: '系统补齐'
}

export const getEventFieldTypeLabel = (value?: string) => {
  return EVENT_FIELD_TYPE_OPTIONS.find((item) => item.value === value)?.label || value || '-'
}

export const getAccessMappingTypeLabel = (value?: string) => {
  return ACCESS_MAPPING_TYPE_OPTIONS.find((item) => item.value === value)?.label || value || '-'
}

export const getAccessScriptEngineLabel = (value?: string) => {
  return ACCESS_SCRIPT_ENGINE_OPTIONS.find((item) => item.value === value)?.label || value || '-'
}

export const getFieldSourceLabel = (value?: string) => {
  return FIELD_SOURCE_LABEL_MAP[value || ''] || value || '-'
}
