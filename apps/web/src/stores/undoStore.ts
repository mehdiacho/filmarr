import { create } from 'zustand'
import type { UndoRecord } from '@/types'

const MAX_UNDO = 10

interface UndoState {
  stack: UndoRecord[]
  push: (record: UndoRecord) => void
  pop: () => UndoRecord | undefined
  canUndo: boolean
}

export const useUndoStore = create<UndoState>((set, get) => ({
  stack: [],
  canUndo: false,

  push: (record) =>
    set((s) => {
      const next = [record, ...s.stack].slice(0, MAX_UNDO)
      return { stack: next, canUndo: next.length > 0 }
    }),

  pop: () => {
    const { stack } = get()
    if (stack.length === 0) return undefined
    const [head, ...rest] = stack
    set({ stack: rest, canUndo: rest.length > 0 })
    return head
  },
}))
