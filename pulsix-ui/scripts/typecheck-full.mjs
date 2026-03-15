import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import path from 'node:path'
import process from 'node:process'

const cwd = process.cwd()
const require = createRequire(import.meta.url)
const vueTscBin = path.join(cwd, 'node_modules/vue-tsc/bin/vue-tsc.js')
const projectConfig = path.join(cwd, 'tsconfig.typecheck.full.json')
const baselineFile = path.join(cwd, 'scripts/typecheck-full-baseline.json')
const updateBaseline = process.argv.includes('--update-baseline')

const parseDiagnostics = (output) => {
  const diagnostics = new Map()
  const pattern = /^([^\n(]+)\(\d+,\d+\): error (TS\d+): (.+)$/gm
  let match = pattern.exec(output)

  while (match) {
    const [, file, code, message] = match
    const key = `${file}|${code}|${message}`
    const current = diagnostics.get(key)

    if (current) {
      current.count += 1
    } else {
      diagnostics.set(key, { file, code, message, count: 1 })
    }

    match = pattern.exec(output)
  }

  return Array.from(diagnostics.values()).sort((left, right) => {
    if (left.file !== right.file) return left.file.localeCompare(right.file)
    if (left.code !== right.code) return left.code.localeCompare(right.code)
    return left.message.localeCompare(right.message)
  })
}

const toCountMap = (diagnostics) => {
  const counts = new Map()
  diagnostics.forEach((entry) => {
    counts.set(`${entry.file}|${entry.code}|${entry.message}`, entry.count)
  })
  return counts
}

const totalCount = (diagnostics) => diagnostics.reduce((sum, entry) => sum + entry.count, 0)

const formatEntry = (entry) => {
  const suffix = entry.count > 1 ? ` x${entry.count}` : ''
  return `${entry.file} ${entry.code} ${entry.message}${suffix}`
}

const printEntries = (label, entries) => {
  if (entries.length === 0) return

  console.error(label)
  entries.slice(0, 50).forEach((entry) => {
    console.error(`- ${formatEntry(entry)}`)
  })

  if (entries.length > 50) {
    console.error(`- ... and ${entries.length - 50} more`)
  }
}

const loadBaseline = () => {
  if (!existsSync(baselineFile)) {
    return []
  }

  const content = JSON.parse(readFileSync(baselineFile, 'utf8'))
  return Array.isArray(content.diagnostics) ? content.diagnostics : []
}

const saveBaseline = (diagnostics) => {
  const content = {
    schemaVersion: 1,
    config: 'tsconfig.typecheck.full.json',
    diagnostics
  }

  writeFileSync(baselineFile, `${JSON.stringify(content, null, 2)}\n`)
}

console.log(`[typecheck:full] Vue project baseline (${projectConfig})`)

const originalArgv = [...process.argv]
const originalExit = process.exit
const originalExitCode = process.exitCode
const originalStdoutWrite = process.stdout.write.bind(process.stdout)
const originalStderrWrite = process.stderr.write.bind(process.stderr)
const stdoutChunks = []
const stderrChunks = []
let vueTscExitCode = 0

const captureWrite = (chunks) => {
  return (chunk, encoding, callback) => {
    const text = Buffer.isBuffer(chunk) ? chunk.toString(typeof encoding === 'string' ? encoding : undefined) : `${chunk}`
    chunks.push(text)

    if (typeof encoding === 'function') {
      encoding()
    } else if (typeof callback === 'function') {
      callback()
    }

    return true
  }
}

process.argv = ['node', 'vue-tsc', '--noEmit', '-p', projectConfig, '--pretty', 'false']
process.exitCode = 0
process.stdout.write = captureWrite(stdoutChunks)
process.stderr.write = captureWrite(stderrChunks)
process.exit = (code = 0) => {
  vueTscExitCode = Number(code) || 0
  throw new Error('__PULSIX_VUE_TSC_EXIT__')
}

try {
  require(vueTscBin)
  vueTscExitCode = process.exitCode ?? 0
} catch (error) {
  if (!(error instanceof Error) || error.message !== '__PULSIX_VUE_TSC_EXIT__') {
    throw error
  }
} finally {
  process.argv = originalArgv
  process.exit = originalExit
  process.exitCode = originalExitCode
  process.stdout.write = originalStdoutWrite
  process.stderr.write = originalStderrWrite
}

const combinedOutput = `${stdoutChunks.join('')}${stderrChunks.join('')}`.trim()

const currentDiagnostics = parseDiagnostics(combinedOutput)

if (updateBaseline) {
  saveBaseline(currentDiagnostics)
  console.log(
    `[typecheck:full] Baseline updated with ${currentDiagnostics.length} diagnostic patterns / ${totalCount(currentDiagnostics)} occurrences.`
  )
  process.exit(0)
}

const baselineDiagnostics = loadBaseline()
const currentCounts = toCountMap(currentDiagnostics)
const baselineCounts = toCountMap(baselineDiagnostics)
const regressions = []
const resolved = []

currentDiagnostics.forEach((entry) => {
  const key = `${entry.file}|${entry.code}|${entry.message}`
  const expectedCount = baselineCounts.get(key) ?? 0

  if (entry.count > expectedCount) {
    regressions.push({ ...entry, count: entry.count - expectedCount })
  }
})

baselineDiagnostics.forEach((entry) => {
  const key = `${entry.file}|${entry.code}|${entry.message}`
  const currentCount = currentCounts.get(key) ?? 0

  if (currentCount < entry.count) {
    resolved.push({ ...entry, count: entry.count - currentCount })
  }
})

if (regressions.length > 0) {
  console.error('[typecheck:full] New vue-tsc regressions detected.')
  printEntries('[typecheck:full] New diagnostics:', regressions)
  if (combinedOutput) {
    console.error('\n[typecheck:full] Raw vue-tsc output:')
    console.error(combinedOutput)
  }
  process.exit(1)
}

if (vueTscExitCode !== 0 && currentDiagnostics.length === 0) {
  if (combinedOutput) {
    console.error(combinedOutput)
  }
  process.exit(vueTscExitCode)
}

if (resolved.length > 0) {
  printEntries('[typecheck:full] Legacy diagnostics resolved since baseline:', resolved)
  console.log('[typecheck:full] No new regressions. Refresh the baseline when you want to lock in these fixes.')
} else {
  console.log(
    `[typecheck:full] No new regressions. Legacy baseline remains at ${baselineDiagnostics.length} diagnostic patterns / ${totalCount(baselineDiagnostics)} occurrences.`
  )
}

process.exit(0)
