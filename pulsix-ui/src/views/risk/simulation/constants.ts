export const simulationVersionSelectModeOptions = [
  { label: '最新已发布', value: 'LATEST' },
  { label: '固定版本', value: 'FIXED' }
]

export const riskActionOptions = [
  { label: '通过', value: 'PASS' },
  { label: '复核', value: 'REVIEW' },
  { label: '拒绝', value: 'REJECT' }
]

export const getSimulationVersionSelectModeLabel = (value?: string) => {
  return simulationVersionSelectModeOptions.find((item) => item.value === value)?.label ?? value ?? '-'
}

export const getSimulationVersionSelectModeTag = (value?: string) => {
  switch (value) {
    case 'FIXED':
      return 'warning'
    case 'LATEST':
    default:
      return 'success'
  }
}

export const getRiskActionLabel = (value?: string) => {
  return riskActionOptions.find((item) => item.value === value)?.label ?? value ?? '-'
}

export const getRiskActionTag = (value?: string) => {
  switch (value) {
    case 'PASS':
      return 'success'
    case 'REVIEW':
      return 'warning'
    case 'REJECT':
      return 'danger'
    default:
      return 'info'
  }
}

export const getSimulationPassLabel = (value?: number | null) => {
  if (value === 1) {
    return '符合预期'
  }
  if (value === 0) {
    return '不符合预期'
  }
  return '-'
}

export const getSimulationPassTag = (value?: number | null) => {
  if (value === 1) {
    return 'success'
  }
  if (value === 0) {
    return 'danger'
  }
  return 'info'
}

const getObject = (value: any): Record<string, any> => {
  return value && typeof value === 'object' && !Array.isArray(value) ? value : {}
}

export const extractSimulationFinalResult = (resultJson?: Record<string, any>) => {
  return getObject(resultJson?.finalResult)
}

export const extractSimulationFinalAction = (resultJson?: Record<string, any>) => {
  const finalResult = extractSimulationFinalResult(resultJson)
  const value = finalResult.finalAction ?? resultJson?.finalAction
  return value == null ? '' : String(value)
}

export const extractSimulationMatchedRules = (resultJson?: Record<string, any>) => {
  const finalResult = extractSimulationFinalResult(resultJson)
  const value = finalResult.hitRules ?? resultJson?.hitRules
  if (!Array.isArray(value)) {
    return []
  }
  return value
    .map((item) => {
      if (item && typeof item === 'object' && !Array.isArray(item)) {
        return item
      }
      return { ruleCode: String(item ?? '') }
    })
    .filter((item) => item.ruleCode)
}

export const extractSimulationHitRuleCodes = (resultJson?: Record<string, any>) => {
  return extractSimulationMatchedRules(resultJson)
    .map((item) => item.ruleCode)
    .filter(Boolean)
}

export const extractSimulationFeatureSnapshot = (resultJson?: Record<string, any>) => {
  return getObject(extractSimulationFinalResult(resultJson).featureSnapshot)
}

export const extractSimulationTrace = (resultJson?: Record<string, any>) => {
  const value = extractSimulationFinalResult(resultJson).trace
  if (!Array.isArray(value)) {
    return []
  }
  return value.map((item) => String(item)).filter(Boolean)
}

export const extractSimulationResults = (resultJson?: Record<string, any>) => {
  return Array.isArray(resultJson?.results) ? resultJson?.results : []
}
