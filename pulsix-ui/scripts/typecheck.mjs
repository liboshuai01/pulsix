import { lstatSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import process from 'node:process'
import { compileScript, parse } from '@vue/compiler-sfc'

const cwd = process.cwd()
const nodeBin = process.execPath
const tscBin = path.join(cwd, 'node_modules/typescript/bin/tsc')
const baseConfig = path.join(cwd, 'tsconfig.typecheck.json')
const cliArgs = process.argv.slice(2)
const tempArtifacts = []

const runNodeScript = (label, scriptPath, args) => {
  console.log(`[typecheck] ${label}`)
  const result = spawnSync(nodeBin, [scriptPath, ...args], {
    cwd,
    stdio: 'inherit'
  })

  if (result.status !== 0) {
    const error = new Error(`${label} failed`)
    error.exitCode = result.status ?? 1
    throw error
  }
}

const toPosixRelative = (filePath) => path.relative(cwd, filePath).split(path.sep).join('/')

const collectVueFiles = (inputPath, files) => {
  const absolutePath = path.resolve(cwd, inputPath)
  const stats = lstatSync(absolutePath)

  if (stats.isDirectory()) {
    for (const entry of readdirSync(absolutePath)) {
      collectVueFiles(path.join(absolutePath, entry), files)
    }
    return
  }

  if (absolutePath.endsWith('.vue')) {
    files.add(absolutePath)
  }
}

const resolveVueTargets = (args) => {
  const files = new Set()

  for (const inputPath of args) {
    collectVueFiles(inputPath, files)
  }

  return Array.from(files).sort()
}

const createVueConfig = (vueFile, index) => {
  const tempConfigPath = path.join(cwd, `.typecheck-${process.pid}-${index}.json`)
  const config = {
    extends: './tsconfig.typecheck.json',
    include: [toPosixRelative(vueFile), 'types/**/*.d.ts', 'src/types/auto-imports.d.ts'],
    exclude: ['src/types/auto-components.d.ts']
  }

  writeFileSync(tempConfigPath, `${JSON.stringify(config, null, 2)}\n`)
  tempArtifacts.push(tempConfigPath)
  return tempConfigPath
}

const createVueScriptArtifact = (vueFile, index) => {
  const relativePath = toPosixRelative(vueFile)
  const source = readFileSync(vueFile, 'utf8')
  const { descriptor, errors } = parse(source, { filename: relativePath })

  if (errors.length > 0) {
    const error = new Error(`Vue parse failed for ${relativePath}`)
    error.details = errors
    throw error
  }

  if (!descriptor.script && !descriptor.scriptSetup) {
    return null
  }

  const compiled = compileScript(descriptor, {
    id: `typecheck-${index}`,
    inlineTemplate: false
  })
  const tempScriptPath = `${vueFile}.typecheck.ts`

  writeFileSync(tempScriptPath, `${compiled.content}\n`)
  tempArtifacts.push(tempScriptPath)
  return tempScriptPath
}

let exitCode = 0

try {
  runNodeScript('TypeScript baseline', tscBin, ['--noEmit', '-p', baseConfig, '--pretty', 'false'])

  const vueTargets = resolveVueTargets(cliArgs)
  if (vueTargets.length === 0) {
    console.log(
      '[typecheck] Vue script check skipped. Pass .vue files or directories, for example: npm run ts:check -- src/views/Login/components/LoginForm.vue'
    )
  } else {
    vueTargets.forEach((vueFile, index) => {
      const relativePath = toPosixRelative(vueFile)
      const tempScriptPath = createVueScriptArtifact(vueFile, index)

      if (!tempScriptPath) {
        console.log(`[typecheck] Vue script ${relativePath} skipped (no script block)`)
        return
      }

      const tempConfigPath = createVueConfig(tempScriptPath, index)
      runNodeScript(`Vue script ${relativePath}`, tscBin, [
        '--noEmit',
        '-p',
        tempConfigPath,
        '--pretty',
        'false'
      ])
    })
  }
} catch (error) {
  if (error.details) {
    console.error(error.details)
  }
  exitCode = error.exitCode ?? 1
} finally {
  for (const tempArtifactPath of tempArtifacts) {
    rmSync(tempArtifactPath, { force: true })
  }
}

process.exit(exitCode)
