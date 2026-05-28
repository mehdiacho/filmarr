import { create } from 'zustand'
import { nanoid } from 'nanoid'
import type { Movie, SwipeDirection, UndoRecord } from '@/types'
import { api } from '@/lib/api'
import { useUndoStore } from './undoStore'
import { useFocusStore } from './focusStore'

interface LibraryBuildState {
  deck: Movie[]
  swipedIds: Set<number>
  isLoading: boolean
  error: string | null

  init: () => Promise<void>
  swipe: (movie: Movie, direction: SwipeDirection) => void
  restoreCard: (record: UndoRecord) => void
  fetchMore: () => Promise<void>
}

export const useLibraryBuildStore = create<LibraryBuildState>((set, get) => ({
  deck: [],
  swipedIds: new Set(),
  isLoading: false,
  error: null,

  init: async () => {
    if (get().deck.length > 0 || get().isLoading) return
    await get().fetchMore()
  },

  fetchMore: async () => {
    if (get().isLoading) return
    set({ isLoading: true, error: null })
    try {
      const movies = await api.libraryBuild(20)
      const swiped = get().swipedIds
      const fresh = movies.filter((m) => !swiped.has(m.id))
      set((s) => ({ deck: [...s.deck, ...fresh] }))
    } catch (e) {
      set({ error: 'Could not load suggestions' })
      console.error('libraryBuild fetchMore:', e)
    } finally {
      set({ isLoading: false })
    }
  },

  swipe: (movie, direction) => {
    const focusState = useFocusStore.getState()
    const rating = focusState.isActive ? focusState.rating : null
    const review = focusState.isActive ? focusState.review : ''

    const record: UndoRecord = {
      id: nanoid(),
      movie,
      feedType: 'library_build',
      direction,
      hearted: false,
      rating,
      review,
      timestamp: Date.now(),
    }

    useUndoStore.getState().push(record)
    focusState.clearData()

    set((s) => ({
      deck: s.deck.filter((m) => m.id !== movie.id),
      swipedIds: new Set([...s.swipedIds, movie.id]),
    }))

    api.interact({
      tmdbId: movie.tmdbId,
      feedType: 'library_build',
      direction,
      hearted: false,
      rating,
      review: review || null,
    }).catch((e) => console.warn('interact POST failed:', e))

    if (get().deck.length < 5) get().fetchMore()
  },

  restoreCard: (record) =>
    set((s) => {
      if (s.deck.some((m) => m.id === record.movie.id)) return s
      const nextSwiped = new Set(s.swipedIds)
      nextSwiped.delete(record.movie.id)
      return { deck: [record.movie, ...s.deck], swipedIds: nextSwiped }
    }),
}))
