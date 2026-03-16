import { execFileSync } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const projectRoot = path.resolve(__dirname, '..')

export const FILE_COUNT_THRESHOLD = 15

const EXACT_HIGH_RISK_PATHS = new Set([
  'pulsix-ui/package.json',
  'pulsix-ui/pnpm-lock.yaml',
  'pulsix-ui/index.html',
  'pulsix-ui/src/permission.ts'
])

const PREFIX_HIGH_RISK_PATHS = [
  'pulsix-ui/build/',
  'pulsix-ui/src/router/',
  'pulsix-ui/src/store/',
  'pulsix-ui/src/plugins/'
]

const REGEX_HIGH_RISK_PATHS = [
  /^pulsix-ui\/vite\.config\.[^/]+$/,
  /^pulsix-ui\/src\/main\.[^/]+$/,
  /^pulsix-ui\/src\/App\.[^/]+$/
]

export function getRepoRoot() {
  try {
    return execFileSync('git', ['-C', projectRoot, 'rev-parse', '--show-toplevel'], {
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe']
    }).trim()
  } catch {
    return path.resolve(projectRoot, '..')
  }
}

export function getChangedFrontendFiles(repoRoot = getRepoRoot()) {
  const files = new Set()
  const commandArgs = [
    ['diff', '--name-only', '--diff-filter=ACMRD', '--', 'pulsix-ui'],
    ['diff', '--cached', '--name-only', '--diff-filter=ACMRD', '--', 'pulsix-ui'],
    ['ls-files', '--others', '--exclude-standard', '--', 'pulsix-ui']
  ]

  for (const args of commandArgs) {
    for (const file of runGitLines(repoRoot, args)) {
      files.add(normalizePath(file))
    }
  }

  return [...files].sort()
}

export function assessBuildRisk(files) {
  const normalizedFiles = [...new Set(files.map(normalizePath).filter(Boolean))].sort()
  const reasons = []

  if (normalizedFiles.length >= FILE_COUNT_THRESHOLD) {
    reasons.push(
      `changed frontend file count (${normalizedFiles.length}) reached the threshold ${FILE_COUNT_THRESHOLD}`
    )
  }

  for (const file of normalizedFiles) {
    if (EXACT_HIGH_RISK_PATHS.has(file)) {
      reasons.push(`matched high-risk file: ${file}`)
      continue
    }
    if (PREFIX_HIGH_RISK_PATHS.some((prefix) => file.startsWith(prefix))) {
      reasons.push(`matched high-risk path: ${file}`)
      continue
    }
    if (REGEX_HIGH_RISK_PATHS.some((pattern) => pattern.test(file))) {
      reasons.push(`matched high-risk file: ${file}`)
    }
  }

  return {
    shouldRunBuild: reasons.length > 0,
    files: normalizedFiles,
    reasons
  }
}

export function formatRiskReport(result) {
  const lines = [
    `shouldRunBuild=${result.shouldRunBuild ? 'yes' : 'no'}`,
    `frontendChangedFiles=${result.files.length}`
  ]
  if (result.reasons.length === 0) {
    lines.push('reasons=none')
  } else {
    lines.push('reasons:')
    result.reasons.forEach((reason) => lines.push(`- ${reason}`))
  }
  return lines.join('\n')
}

function runGitLines(repoRoot, args) {
  try {
    const output = execFileSync('git', ['-C', repoRoot, ...args], {
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe']
    }).trim()
    return output ? output.split('\n') : []
  } catch {
    return []
  }
}

function normalizePath(file) {
  return file.replaceAll(path.sep, '/').trim()
}

function parseOverrideFiles(args) {
  const flagIndex = args.indexOf('--files')
  if (flagIndex === -1) {
    return null
  }
  return args.slice(flagIndex + 1)
}

function isDirectExecution() {
  return process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href
}

if (isDirectExecution()) {
  const overrideFiles = parseOverrideFiles(process.argv.slice(2))
  const result = assessBuildRisk(overrideFiles ?? getChangedFrontendFiles())
  console.log(formatRiskReport(result))
  process.exit(result.shouldRunBuild ? 10 : 0)
}
