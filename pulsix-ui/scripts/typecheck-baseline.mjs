import { mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawn } from 'node:child_process'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const projectRoot = path.resolve(__dirname, '..')
const baselinePath = path.resolve(__dirname, 'typecheck-baseline.json')
const mode = process.argv.includes('--update') ? 'update' : 'check'

const rawResult = await runTypecheck()
const parsedErrors = parseTypecheckOutput(rawResult.output)
const currentBaseline = buildBaseline(parsedErrors)

if (mode === 'update') {
  await mkdir(__dirname, { recursive: true })
  await writeFile(baselinePath, `${JSON.stringify(currentBaseline, null, 2)}\n`, 'utf8')
  printUpdateSummary(currentBaseline, rawResult.exitCode)
  process.exit(0)
}

const baseline = await loadBaseline()
if (!baseline) {
  console.error(`TypeScript baseline file not found: ${path.relative(projectRoot, baselinePath)}`)
  console.error('Run `pnpm --dir pulsix-ui run ts:check:baseline:update` to create it.')
  process.exit(1)
}

const diff = compareBaselines(baseline, currentBaseline)
printCheckSummary(baseline, currentBaseline, diff)
process.exit(diff.newErrorTotal > 0 ? 1 : 0)

function runTypecheck() {
  return mkdtemp(path.join(tmpdir(), 'pulsix-ui-typecheck-')).then(
    (tempDir) =>
      new Promise((resolve, reject) => {
        const outputPath = path.join(tempDir, 'vue-tsc.out')
        const command = [
          shellQuote(process.execPath),
          '--max_old_space_size=8192',
          './node_modules/vue-tsc/bin/vue-tsc',
          '--noEmit',
          '-p',
          'tsconfig.typecheck.json',
          '>',
          shellQuote(outputPath),
          '2>&1'
        ].join(' ')
        const child = spawn('bash', ['-lc', command], {
          cwd: projectRoot,
          env: process.env,
          stdio: 'ignore'
        })

        child.on('error', async (error) => {
          await rm(tempDir, { recursive: true, force: true })
          reject(error)
        })
        child.on('close', async (exitCode) => {
          const output = await readFile(outputPath, 'utf8').catch(() => '')
          await rm(tempDir, { recursive: true, force: true })
          resolve({
            exitCode: exitCode ?? 1,
            output
          })
        })
      })
  )
}

function parseTypecheckOutput(output) {
  const lines = output.replaceAll('\r\n', '\n').split('\n')
  const errors = []
  let current = null

  const pushCurrent = () => {
    if (!current) return
    errors.push({
      file: current.file,
      code: current.code,
      line: current.line,
      column: current.column,
      message: normalizeMessage(current.parts)
    })
    current = null
  }

  for (const line of lines) {
    const match = line.match(/^(src\/[^(]+)\((\d+),(\d+)\): error (TS\d+): (.*)$/)
    if (match) {
      pushCurrent()
      current = {
        file: match[1],
        line: Number(match[2]),
        column: Number(match[3]),
        code: match[4],
        parts: [match[5]]
      }
      continue
    }

    if (current && /^\s+/.test(line) && line.trim()) {
      current.parts.push(line.trim())
      continue
    }

    if (current && line.trim() === '') {
      continue
    }

    pushCurrent()
  }

  pushCurrent()
  return errors
}

function normalizeMessage(parts) {
  return parts.join(' ').replace(/\s+/g, ' ').trim()
}

function buildBaseline(errors) {
  const counter = new Map()
  for (const error of errors) {
    const key = serializeKey(error.file, error.code, error.message)
    const existing = counter.get(key)
    if (existing) {
      existing.count += 1
    } else {
      counter.set(key, {
        file: error.file,
        code: error.code,
        message: error.message,
        count: 1
      })
    }
  }

  const entries = [...counter.values()].sort(compareEntries)
  return {
    version: 1,
    generatedAt: new Date().toISOString(),
    command: `${process.execPath} --max_old_space_size=8192 ./node_modules/vue-tsc/bin/vue-tsc --noEmit -p tsconfig.typecheck.json`,
    summary: {
      totalErrors: errors.length,
      totalFiles: new Set(errors.map((error) => error.file)).size,
      totalSignatures: entries.length
    },
    entries
  }
}

async function loadBaseline() {
  try {
    const content = await readFile(baselinePath, 'utf8')
    return JSON.parse(content)
  } catch (error) {
    if (error && typeof error === 'object' && 'code' in error && error.code === 'ENOENT') {
      return null
    }
    throw error
  }
}

function compareBaselines(previous, current) {
  const previousMap = toEntryMap(previous.entries)
  const currentMap = toEntryMap(current.entries)
  const newEntries = []
  const resolvedEntries = []
  let newErrorTotal = 0
  let resolvedErrorTotal = 0

  for (const entry of current.entries) {
    const key = serializeKey(entry.file, entry.code, entry.message)
    const previousCount = previousMap.get(key)?.count ?? 0
    if (entry.count > previousCount) {
      const delta = entry.count - previousCount
      newEntries.push({ ...entry, delta })
      newErrorTotal += delta
    }
  }

  for (const entry of previous.entries) {
    const key = serializeKey(entry.file, entry.code, entry.message)
    const currentCount = currentMap.get(key)?.count ?? 0
    if (entry.count > currentCount) {
      const delta = entry.count - currentCount
      resolvedEntries.push({ ...entry, delta })
      resolvedErrorTotal += delta
    }
  }

  return {
    newEntries,
    resolvedEntries,
    newErrorTotal,
    resolvedErrorTotal
  }
}

function toEntryMap(entries) {
  const map = new Map()
  for (const entry of entries) {
    map.set(serializeKey(entry.file, entry.code, entry.message), entry)
  }
  return map
}

function serializeKey(file, code, message) {
  return `${file}::${code}::${message}`
}

function shellQuote(value) {
  return `'${String(value).replaceAll("'", "'\"'\"'")}'`
}

function compareEntries(left, right) {
  return (
    left.file.localeCompare(right.file) ||
    left.code.localeCompare(right.code) ||
    left.message.localeCompare(right.message)
  )
}

function printUpdateSummary(baseline, exitCode) {
  console.log('TypeScript baseline updated.')
  console.log(`- File: ${path.relative(projectRoot, baselinePath)}`)
  console.log(`- Errors captured: ${baseline.summary.totalErrors}`)
  console.log(`- Files captured: ${baseline.summary.totalFiles}`)
  console.log(`- Signatures captured: ${baseline.summary.totalSignatures}`)
  console.log(`- Raw vue-tsc exit code: ${exitCode}`)
}

function printCheckSummary(previous, current, diff) {
  if (diff.newErrorTotal === 0) {
    console.log('TypeScript baseline check passed.')
    console.log(`- Current raw errors: ${current.summary.totalErrors} across ${current.summary.totalFiles} files`)
    console.log(`- Stored baseline: ${previous.summary.totalErrors} across ${previous.summary.totalFiles} files`)
    console.log(`- New errors: 0`)
    if (diff.resolvedErrorTotal > 0) {
      console.log(`- Resolved baseline errors since snapshot: ${diff.resolvedErrorTotal}`)
      console.log('- Run `pnpm --dir pulsix-ui run ts:check:baseline:update` to refresh the stored baseline.')
    }
    return
  }

  console.error('TypeScript baseline check failed: new errors detected.')
  console.error(`- New error count: ${diff.newErrorTotal}`)
  console.error(`- Current raw errors: ${current.summary.totalErrors} across ${current.summary.totalFiles} files`)
  console.error(`- Stored baseline: ${previous.summary.totalErrors} across ${previous.summary.totalFiles} files`)
  console.error('- New signatures:')
  for (const entry of diff.newEntries.slice(0, 30)) {
    console.error(`  - [${entry.delta}] ${entry.file} ${entry.code}: ${entry.message}`)
  }
  if (diff.newEntries.length > 30) {
    console.error(`  - ... and ${diff.newEntries.length - 30} more signatures`)
  }
}
