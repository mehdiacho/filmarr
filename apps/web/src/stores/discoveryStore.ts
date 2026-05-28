import { create } from 'zustand'
import { nanoid } from 'nanoid'
import type { Movie, SwipeDirection, UndoRecord } from '@/types'
import { api } from '@/lib/api'
import { useUndoStore } from './undoStore'
import { useFocusStore } from './focusStore'

const REFILL_THRESHOLD = 5

interface DiscoveryState {
  deck: Movie[]
  hearts: Set<number>
  swipedIds: Set<number>
  isLoading: boolean
  error: string | null

  init: () => Promise<void>
  swipe: (movie: Movie, direction: SwipeDirection) => void
  toggleHeart: (movieId: number) => void
  restoreCard: (record: UndoRecord) => void
  fetchMore: () => Promise<void>
}

export const useDiscoveryStore = create<DiscoveryState>((set, get) => ({
  deck: [],
  hearts: new Set(),
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
      const movies = await api.discovery(20)
      const swiped = get().swipedIds
      const fresh = movies.filter((m) => !swiped.has(m.id))
      set((s) => ({ deck: [...s.deck, ...fresh] }))
    } catch (e) {
      set({ error: 'Could not load recommendations' })
      console.error('discovery fetchMore:', e)
    } finally {
      set({ isLoading: false })
    }
  },

  swipe: (movie, direction) => {
    const { hearts } = get()
    const focusState = useFocusStore.getState()
    const rating = focusState.isActive ? focusState.rating : null
    const review = focusState.isActive ? focusState.review : ''

    const record: UndoRecord = {
      id: nanoid(),
      movie,
      feedType: 'discovery',
      direction,
      hearted: hearts.has(movie.id),
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

    // Persist to backend (fire-and-forget — don't block the swipe)
    api.interact({
      tmdbId: movie.tmdbId,
      feedType: 'discovery',
      direction,
      hearted: hearts.has(movie.id),
      rating,
      review: review || null,
    }).catch((e) => console.warn('interact POST failed:', e))

    if (get().deck.length < REFILL_THRESHOLD) get().fetchMore()
  },

  toggleHeart: (movieId) =>
    set((s) => {
      const next = new Set(s.hearts)
      next.has(movieId) ? next.delete(movieId) : next.add(movieId)
      return { hearts: next }
    }),

  restoreCard: (record) =>
    set((s) => {
      if (s.deck.some((m) => m.id === record.movie.id)) return s
      const nextSwiped = new Set(s.swipedIds)
      nextSwiped.delete(record.movie.id)
      return { deck: [record.movie, ...s.deck], swipedIds: nextSwiped }
    }),
}))
