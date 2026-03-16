export const EVENT_MODEL_DEFAULT_TOPIC = 'pulsix.event.standard'

export const EVENT_FIELD_TYPE_OPTIONS = [
  { label: '字符串', value: 'STRING' },
  { label: '整数', value: 'INTEGER' },
  { label: '长整数', value: 'LONG' },
  { label: '小数', value: 'DECIMAL' },
  { label: '布尔', value: 'BOOLEAN' },
  { label: '日期时间', value: 'DATETIME' },
  { label: 'JSON', value: 'JSON' }
]

export const getEventFieldTypeLabel = (value?: string) => {
  return EVENT_FIELD_TYPE_OPTIONS.find((item) => item.value === value)?.label || value || '-'
}
