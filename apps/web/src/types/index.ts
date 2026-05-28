export interface Movie {
  id: number
  tmdbId: number
  title: string
  year: number
  posterPath: string | null
  backdropPath: string | null
  genres: string[]
  overview: string
  voteAverage: number
  runtime: number | null
  mediaType: 'movie' | 'tv'
}

export type FeedType = 'discovery' | 'library_build'

export type SwipeDirection = 'left' | 'right' | 'up' | 'down'

export interface UndoRecord {
  id: string
  movie: Movie
  feedType: FeedType
  direction: SwipeDirection
  hearted: boolean
  rating: number | null
  review: string
  timestamp: number
}

export interface User {
  id: number
  email: string
}

export interface AuthTokens {
  accessToken: string
  tokenType: string
}
