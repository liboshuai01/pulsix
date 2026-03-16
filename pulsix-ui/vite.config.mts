import { resolve } from 'path'
import { loadEnv } from 'vite'
import type { ConfigEnv, UserConfig } from 'vite'
import { createVitePlugins } from './build/vite'
import { exclude, include } from './build/vite/optimize'

// Keep build validation focused on project regressions instead of stale browserslist metadata noise.
process.env.BROWSERSLIST_IGNORE_OLD_DATA ??= 'true'

const root = process.cwd()

function pathResolve(dir: string) {
  return resolve(root, '.', dir)
}

export default async ({ command, mode }: ConfigEnv): Promise<UserConfig> => {
  const envMode =
    command === 'build'
      ? mode
      : process.argv[3] === '--mode'
        ? process.argv[4]
        : process.argv[3] || mode
  const env = loadEnv(envMode, root)

  return {
    base: env.VITE_BASE_PATH,
    root,
    server: {
      port: env.VITE_PORT,
      host: '0.0.0.0',
      open: env.VITE_OPEN === 'true'
    },
    plugins: await createVitePlugins(),
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: '@use "@/styles/variables.scss" as *;',
          javascriptEnabled: true,
          silenceDeprecations: ['legacy-js-api']
        }
      }
    },
    resolve: {
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.scss', '.css'],
      alias: [
        {
          find: 'vue-i18n',
          replacement: 'vue-i18n/dist/vue-i18n.cjs.js'
        },
        {
          find: /\@\//,
          replacement: `${pathResolve('src')}/`
        }
      ]
    },
    build: {
      minify: 'terser',
      outDir: env.VITE_OUT_DIR || 'dist',
      sourcemap: env.VITE_SOURCEMAP === 'true' ? 'inline' : false,
      // Keep the warning line aligned with the current known vendor baseline.
      chunkSizeWarningLimit: 11000,
      terserOptions: {
        compress: {
          drop_debugger: env.VITE_DROP_DEBUGGER === 'true',
          drop_console: env.VITE_DROP_CONSOLE === 'true'
        }
      },
      rollupOptions: {
        output: {
          manualChunks: {
            echarts: ['echarts'],
            'form-create': ['@form-create/element-ui'],
            'form-designer': ['@form-create/designer']
          }
        }
      }
    },
    optimizeDeps: { include, exclude }
  }
}
