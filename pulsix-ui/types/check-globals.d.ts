import type { TableColumnCtx } from 'element-plus'

declare global {
  interface SerialReaderLike {
    read(): Promise<{ value?: Uint8Array; done: boolean }>
    cancel(): Promise<void>
    releaseLock(): void
  }

  interface SerialPortLike {
    open(options: { baudRate: number; dataBits?: number; stopBits?: number }): Promise<void>
    close(): Promise<void>
    readable?: {
      getReader(): SerialReaderLike
    }
  }

  interface Navigator {
    serial?: {
      requestPort(): Promise<SerialPortLike>
      getPorts(): Promise<SerialPortLike[]>
    }
  }

  interface Window {
    _hmt: any[]
    selectAddress?: (loc: any) => void
    bpmnInstances?: Record<string, any> | null
  }

  const _hmt: any[]

  type SummaryMethodProps<T = Record<string, any>> = {
    columns: TableColumnCtx<T>[]
    data: T[]
  }
}

export {}
