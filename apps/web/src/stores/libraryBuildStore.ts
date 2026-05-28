import { create } from 'zustand'
import { nanoid } from 'nanoid'
import type { Movie, SwipeDirection, UndoRecord } from '@/types'
import { useUndoStore } from './undoStore'
import { useFocusStore } from './focusStore'

// Reuses the same placeholder set — real API will serve popularity-weighted suggestions
const PLACEHOLDER_MOVIES: Movie[] = [
  {
    id: 101, tmdbId: 603, title: 'The Matrix', year: 1999,
    posterPath: '/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg', backdropPath: null,
    genres: ['Action', 'Sci-Fi'], overview: 'A computer hacker learns the true nature of reality and joins a rebellion against the machines.',
    voteAverage: 8.2, runtime: 136, mediaType: 'movie',
  },
  {
    id: 102, tmdbId: 11, title: 'Star Wars', year: 1977,
    posterPath: '/6FfCtAuVAW8XJjZ7eWeLibRLWTw.jpg', backdropPath: null,
    genres: ['Action', 'Adventure', 'Sci-Fi'], overview: 'Luke Skywalker joins forces with Jedi Knight Obi-Wan Kenobi to rescue Princess Leia.',
    voteAverage: 8.2, runtime: 121, mediaType: 'movie',
  },
  {
    id: 103, tmdbId: 120, title: 'The Lord of the Rings: The Fellowship of the Ring', year: 2001,
    posterPath: '/6oom5QYQ2yQTMJIbnvbkBL9cHo6.jpg', backdropPath: null,
    genres: ['Adventure', 'Fantasy'], overview: 'A young hobbit is tasked with destroying a powerful ring and saving Middle-earth.',
    voteAverage: 8.4, runtime: 178, mediaType: 'movie',
  },
  {
    id: 104, tmdbId: 155, title: 'The Dark Knight', year: 2008,
    posterPath: '/qJ2tW6WMUDux911r6m7haRef0WH.jpg', backdropPath: null,
    genres: ['Action', 'Crime', 'Drama'], overview: 'Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice.',
    voteAverage: 9.0, runtime: 152, mediaType: 'movie',
  },
  {
    id: 105, tmdbId: 19404, title: 'Dilwale Dulhania Le Jayenge', year: 1995,
    posterPath: '/2CAL2433ZeIihfX1Hb2139CX0pW.jpg', backdropPath: null,
    genres: ['Comedy', 'Drama', 'Romance'], overview: 'When two students fall in love on a trip across Europe, the girl\'s overprotective father does not approve.',
    voteAverage: 8.1, runtime: 189, mediaType: 'movie',
  },
  {
    id: 106, tmdbId: 348, title: 'Alien', year: 1979,
    posterPath: '/vfrQk5IPloGg1v9Rzbh2Eg3VGyM.jpg', backdropPath: null,
    genres: ['Horror', 'Sci-Fi'], overview: 'The crew of a commercial spacecraft encounters a deadly alien life form after investigating an unknown transmission.',
    voteAverage: 8.1, runtime: 117, mediaType: 'movie',
  },
]

interface LibraryBuildState {
  deck: Movie[]
  swipedIds: Set<number>
  isLoading: boolean

  init: () => void
  swipe: (movie: Movie, direction: SwipeDirection) => void
  restoreCard: (record: UndoRecord) => void
  fetchMore: () => Promise<void>
}

export const useLibraryBuildStore = create<LibraryBuildState>((set, get) => ({
  deck: [],
  swipedIds: new Set(),
  isLoading: false,

  init: () => {
    const swiped = get().swipedIds
    set({ deck: PLACEHOLDER_MOVIES.filter((m) => !swiped.has(m.id)) })
  },

  swipe: (movie, direction) => {
    const focusState = useFocusStore.getState()

    const record: UndoRecord = {
      id: nanoid(),
      movie,
      feedType: 'library_build',
      direction,
      hearted: false,
      rating: focusState.isActive ? focusState.rating : null,
      review: focusState.isActive ? focusState.review : '',
      timestamp: Date.now(),
    }

    useUndoStore.getState().push(record)
    focusState.clearData()

    set((s) => ({
      deck: s.deck.filter((m) => m.id !== movie.id),
      swipedIds: new Set([...s.swipedIds, movie.id]),
    }))

    if (get().deck.length < 5) get().fetchMore()
  },

  restoreCard: (record) =>
    set((s) => {
      if (s.deck.some((m) => m.id === record.movie.id)) return s
      const nextSwiped = new Set(s.swipedIds)
      nextSwiped.delete(record.movie.id)
      return { deck: [record.movie, ...s.deck], swipedIds: nextSwiped }
    }),

  fetchMore: async () => {
    set({ isLoading: false })
  },
}))
