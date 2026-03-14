import type { SimulationReportVO } from '@/api/risk/simulation'

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

const getArray = (value: any): any[] => {
  return Array.isArray(value) ? value : []
}

export const extractSimulationFinalResult = (report?: Partial<SimulationReportVO>) => {
  return getObject(report?.finalResult ?? report?.resultJson?.finalResult)
}

export const extractSimulationFinalAction = (report?: Partial<SimulationReportVO>) => {
  const finalResult = extractSimulationFinalResult(report)
  const value = report?.finalAction ?? finalResult.finalAction ?? report?.resultJson?.finalAction
  return value == null ? '' : String(value)
}

export const extractSimulationMatchedRules = (report?: Partial<SimulationReportVO>) => {
  const finalResult = extractSimulationFinalResult(report)
  const value = report?.hitRules ?? finalResult.hitRules ?? report?.resultJson?.hitRules
  return getArray(value)
    .map((item) => {
      if (item && typeof item === 'object' && !Array.isArray(item)) {
        return item
      }
      return { ruleCode: String(item ?? '') }
    })
    .filter((item) => item.ruleCode)
}

export const extractSimulationHitRuleCodes = (report?: Partial<SimulationReportVO>) => {
  return extractSimulationMatchedRules(report)
    .map((item) => item.ruleCode)
    .filter(Boolean)
}

export const extractSimulationFeatureSnapshot = (report?: Partial<SimulationReportVO>) => {
  return getObject(report?.featureSnapshot ?? extractSimulationFinalResult(report).featureSnapshot)
}

export const extractSimulationTrace = (report?: Partial<SimulationReportVO>) => {
  const value = report?.trace ?? extractSimulationFinalResult(report).trace
  return getArray(value).map((item) => String(item)).filter(Boolean)
}

export const extractSimulationResults = (report?: Partial<SimulationReportVO>) => {
  return getArray(report?.results ?? report?.resultJson?.results)
}
