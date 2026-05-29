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

export interface WatchedEntry {
  movie: Movie
  rating: number | null
  review: string | null
  watchedAt: string
}

export interface CastMember {
  name: string
  character: string | null
  profilePath: string | null
}

export interface CrewMember {
  name: string
  job: string | null
  profilePath: string | null
}

export interface SimilarMovie {
  tmdbId: number
  title: string
  year: number | null
  posterPath: string | null
}

export interface WatchProvider {
  name: string
  logoPath: string | null
}

export type ViewerFilmStatus = 'watched' | 'watching' | 'plan' | 'not_interested'

export interface ViewerStatus {
  status: ViewerFilmStatus | null
  rating: number | null
  review: string | null
  hearted: boolean
}

export interface MovieDetail extends Movie {
  director: string | null
  cast: CastMember[]
  crew: CrewMember[]
  trailerKey: string | null
  similar: SimilarMovie[]
  watchProviders: WatchProvider[]
  viewer: ViewerStatus
}

export interface GenreCount {
  name: string
  count: number
}

export interface DecadeCount {
  decade: number
  count: number
}

export interface ProfileStats {
  filmsWatched: number
  reviews: number
  avgRating: number | null
  ratingHistogram: Record<string, number>
  topGenres: GenreCount[]
  topDecades: DecadeCount[]
}

export interface UserCard {
  id: number
  username: string | null
  displayName: string | null
  avatarUrl: string | null
  isFollowing: boolean
}

export interface PublicProfile {
  id: number
  username: string | null
  displayName: string | null
  bio: string | null
  avatarUrl: string | null
  joined: string
  favorites: Movie[]
  stats: ProfileStats
  followerCount: number
  followingCount: number
  isFollowing: boolean
  isSelf: boolean
}

export interface FeedItem {
  user: UserCard
  movie: Movie
  status: 'watched' | 'watching' | 'plan'
  rating: number | null
  review: string | null
  at: string
}

export interface MyProfile extends PublicProfile {
  email: string
  dateOfBirth: string | null
  preferences: { genres?: string[]; onboarded?: boolean }
}

export interface ProfileUpdate {
  username?: string
  displayName?: string
  bio?: string
  dateOfBirth?: string | null
  favoriteFilmIds?: number[]
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
