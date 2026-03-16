import { spawn } from 'node:child_process'
import { performance } from 'node:perf_hooks'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  assessBuildRisk,
  formatRiskReport,
  getChangedFrontendFiles,
  getRepoRoot
} from './ai-risk-check.mjs'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const projectRoot = path.resolve(__dirname, '..')
const repoRoot = getRepoRoot()
const mode = process.argv[2] || 'auto'
const pnpmCommand = process.platform === 'win32' ? 'pnpm.cmd' : 'pnpm'

const startedAt = performance.now()

try {
  if (mode === 'fast') {
    await runQuickChecks()
  } else if (mode === 'auto') {
    await runQuickChecks()
    const riskResult = assessBuildRisk(getChangedFrontendFiles(repoRoot))
    console.log(`[verify:ai:auto] ${formatRiskReport(riskResult).replace(/\n/g, '\n[verify:ai:auto] ')}`)
    if (riskResult.shouldRunBuild) {
      console.log('[verify:ai:auto] High-risk frontend changes detected, running build:dev.')
      await runBuild()
    } else {
      console.log('[verify:ai:auto] No high-risk frontend changes detected, skipping build:dev.')
    }
  } else if (mode === 'full') {
    await runQuickChecks()
    console.log('[verify:ai:full] Running build:dev by explicit request.')
    await runBuild()
  } else {
    console.error(`Unknown mode: ${mode}`)
    console.error('Usage: node ./scripts/verify-ai.mjs <fast|auto|full>')
    process.exit(1)
  }

  console.log(
    `[verify:ai:${mode}] Completed in ${formatDuration(performance.now() - startedAt)}.`
  )
} catch (error) {
  console.error(`[verify:ai:${mode}] Failed.`)
  if (error instanceof Error) {
    console.error(error.message)
  } else {
    console.error(String(error))
  }
  process.exit(1)
}

async function runQuickChecks() {
  console.log(`[verify:ai:${mode}] Running quick checks...`)
  await runCommand('git diff --check', 'git', ['-C', repoRoot, 'diff', '--check'], {
    cwd: repoRoot
  })
  await runCommand('pnpm run ts:check', pnpmCommand, ['run', 'ts:check'], {
    cwd: projectRoot
  })
}

async function runBuild() {
  await runCommand('pnpm run build:dev', pnpmCommand, ['run', 'build:dev'], {
    cwd: projectRoot
  })
}

async function runCommand(label, command, args, options) {
  const commandStart = performance.now()
  console.log(`[verify:ai:${mode}] -> ${label}`)

  await new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      cwd: options.cwd,
      stdio: 'inherit',
      env: process.env
    })

    child.on('error', reject)
    child.on('close', (code) => {
      if (code === 0) {
        resolve()
        return
      }
      reject(new Error(`${label} exited with code ${code ?? 1}`))
    })
  })

  console.log(
    `[verify:ai:${mode}] <- ${label} (${formatDuration(performance.now() - commandStart)})`
  )
}

function formatDuration(ms) {
  if (ms < 1000) {
    return `${Math.round(ms)}ms`
  }
  return `${(ms / 1000).toFixed(1)}s`
}
