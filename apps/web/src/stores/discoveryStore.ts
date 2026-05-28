import { create } from 'zustand'
import { nanoid } from 'nanoid'
import type { Movie, SwipeDirection, UndoRecord } from '@/types'
import { useUndoStore } from './undoStore'
import { useFocusStore } from './focusStore'

const REFILL_THRESHOLD = 5

// Placeholder movies — replaced by API data once backend is wired
const PLACEHOLDER_MOVIES: Movie[] = [
  {
    id: 1, tmdbId: 27205, title: 'Inception', year: 2010,
    posterPath: '/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg', backdropPath: null,
    genres: ['Action', 'Sci-Fi'], overview: 'A thief who steals corporate secrets through dream-sharing technology is given the inverse task of planting an idea.',
    voteAverage: 8.4, runtime: 148, mediaType: 'movie',
  },
  {
    id: 2, tmdbId: 157336, title: 'Interstellar', year: 2014,
    posterPath: '/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg', backdropPath: null,
    genres: ['Drama', 'Sci-Fi'], overview: 'A team of explorers travel through a wormhole in space in an attempt to ensure humanity\'s survival.',
    voteAverage: 8.7, runtime: 169, mediaType: 'movie',
  },
  {
    id: 3, tmdbId: 550, title: 'Fight Club', year: 1999,
    posterPath: '/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg', backdropPath: null,
    genres: ['Drama', 'Thriller'], overview: 'An insomniac office worker and a devil-may-care soap maker form an underground fight club.',
    voteAverage: 8.4, runtime: 139, mediaType: 'movie',
  },
  {
    id: 4, tmdbId: 13, title: 'Forrest Gump', year: 1994,
    posterPath: '/saHP97rTPS5eLmrLQEXjsI4UJU.jpg', backdropPath: null,
    genres: ['Comedy', 'Drama', 'Romance'], overview: 'The presidencies of Kennedy and Johnson, Vietnam, Watergate, and other events unfold through the perspective of an Alabama man.',
    voteAverage: 8.5, runtime: 142, mediaType: 'movie',
  },
  {
    id: 5, tmdbId: 278, title: 'The Shawshank Redemption', year: 1994,
    posterPath: '/lyQBXzOQSuE59IsHyhrp0qIiPAz.jpg', backdropPath: null,
    genres: ['Drama'], overview: 'Banker Andy Dufresne is convicted of murder and sentenced to Shawshank prison, forming an unlikely friendship.',
    voteAverage: 8.7, runtime: 142, mediaType: 'movie',
  },
  {
    id: 6, tmdbId: 238, title: 'The Godfather', year: 1972,
    posterPath: '/3bhkrj58Vtu7enYsLe1rKLb4plv.jpg', backdropPath: null,
    genres: ['Crime', 'Drama'], overview: 'The aging patriarch of an organized crime dynasty transfers control of his empire to his reluctant son.',
    voteAverage: 8.7, runtime: 175, mediaType: 'movie',
  },
  {
    id: 7, tmdbId: 424, title: "Schindler's List", year: 1993,
    posterPath: '/sF1U4EUQS8YHUYjNl3pMGNIQyr0.jpg', backdropPath: null,
    genres: ['Drama', 'History', 'War'], overview: 'In German-occupied Poland during WWII, Oskar Schindler gradually becomes concerned for his Jewish workforce.',
    voteAverage: 8.6, runtime: 195, mediaType: 'movie',
  },
  {
    id: 8, tmdbId: 389, title: '12 Angry Men', year: 1957,
    posterPath: '/ppd84D2i9W8jXmsyInGyihiSyqz.jpg', backdropPath: null,
    genres: ['Drama'], overview: 'A jury holdout attempts to prevent a miscarriage of justice by forcing his colleagues to reconsider the evidence.',
    voteAverage: 8.5, runtime: 96, mediaType: 'movie',
  },
]

interface DiscoveryState {
  deck: Movie[]
  hearts: Set<number>
  swipedIds: Set<number>
  isLoading: boolean

  init: () => void
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

  init: () => {
    const swiped = get().swipedIds
    const unseen = PLACEHOLDER_MOVIES.filter((m) => !swiped.has(m.id))
    set({ deck: unseen })
  },

  swipe: (movie, direction) => {
    const { hearts } = get()
    const focusState = useFocusStore.getState()

    const record: UndoRecord = {
      id: nanoid(),
      movie,
      feedType: 'discovery',
      direction,
      hearted: hearts.has(movie.id),
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

    if (get().deck.length < REFILL_THRESHOLD) {
      get().fetchMore()
    }
  },

  toggleHeart: (movieId) =>
    set((s) => {
      const next = new Set(s.hearts)
      next.has(movieId) ? next.delete(movieId) : next.add(movieId)
      return { hearts: next }
    }),

  restoreCard: (record) =>
    set((s) => {
      const alreadyIn = s.deck.some((m) => m.id === record.movie.id)
      if (alreadyIn) return s
      const nextSwiped = new Set(s.swipedIds)
      nextSwiped.delete(record.movie.id)
      return {
        deck: [record.movie, ...s.deck],
        swipedIds: nextSwiped,
      }
    }),

  fetchMore: async () => {
    // TODO: replace with real API call
    set({ isLoading: false })
  },
}))
