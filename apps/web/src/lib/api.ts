/**
 * Thin API client. All paths are relative so Vite's proxy (dev) and
 * Vercel's rewrite (prod) both work transparently.
 */
import type {
  Movie,
  FeedType,
  SwipeDirection,
  WatchedEntry,
  MovieDetail,
  MyProfile,
  PublicProfile,
  ProfileStats,
  ProfileUpdate,
} from '@/types'
import { useAuthStore } from '@/stores/authStore'

const BASE = '/api'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = useAuthStore.getState().token
  const headers = new Headers(init?.headers)
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const res = await fetch(`${BASE}${path}`, { ...init, headers })

  if (res.status === 401) {
    // Token missing/expired — drop it so the app redirects to /auth
    useAuthStore.getState().logout()
    throw new Error('Unauthorized')
  }
  if (!res.ok) throw new Error(`API ${res.status}: ${path}`)
  return res.json() as Promise<T>
}

// ── Raw API shapes (snake_case from FastAPI) ──────────────────────────────────

interface MovieDTO {
  id: number
  tmdb_id: number
  title: string
  year: number | null
  media_type: string
  overview: string | null
  poster_path: string | null
  backdrop_path: string | null
  genres: string[]
  vote_average: number
  runtime: number | null
}

// ── Mapping ───────────────────────────────────────────────────────────────────

export function dtoToMovie(dto: MovieDTO): Movie {
  return {
    id: dto.id,
    tmdbId: dto.tmdb_id,
    title: dto.title,
    year: dto.year ?? 0,
    mediaType: (dto.media_type as 'movie' | 'tv') ?? 'movie',
    overview: dto.overview ?? '',
    posterPath: dto.poster_path,
    backdropPath: dto.backdrop_path,
    genres: dto.genres,
    voteAverage: dto.vote_average,
    runtime: dto.runtime,
  }
}

interface MovieDetailDTO extends MovieDTO {
  director: string | null
  cast: { name: string; character: string | null; profile_path: string | null }[]
  crew: { name: string; job: string | null; profile_path: string | null }[]
  trailer_key: string | null
  similar: { tmdb_id: number; title: string; year: number | null; poster_path: string | null }[]
  watch_providers: { name: string; logo_path: string | null }[]
  viewer: {
    status: 'watched' | 'watching' | 'plan' | 'not_interested' | null
    rating: number | null
    review: string | null
    hearted: boolean
  }
}

function dtoToMovieDetail(d: MovieDetailDTO): MovieDetail {
  return {
    ...dtoToMovie(d),
    director: d.director,
    cast: d.cast.map((c) => ({
      name: c.name,
      character: c.character,
      profilePath: c.profile_path,
    })),
    crew: d.crew.map((c) => ({
      name: c.name,
      job: c.job,
      profilePath: c.profile_path,
    })),
    trailerKey: d.trailer_key,
    similar: d.similar.map((s) => ({
      tmdbId: s.tmdb_id,
      title: s.title,
      year: s.year,
      posterPath: s.poster_path,
    })),
    watchProviders: d.watch_providers.map((w) => ({
      name: w.name,
      logoPath: w.logo_path,
    })),
    viewer: d.viewer,
  }
}

// ── Profile mapping ───────────────────────────────────────────────────────────

interface StatsDTO {
  films_watched: number
  reviews: number
  avg_rating: number | null
  rating_histogram: Record<string, number>
  top_genres: { name: string; count: number }[]
  top_decades: { decade: number; count: number }[]
}
interface PublicProfileDTO {
  id: number
  username: string | null
  display_name: string | null
  bio: string | null
  avatar_url: string | null
  joined: string
  favorites: MovieDTO[]
  stats: StatsDTO
}
interface MyProfileDTO extends PublicProfileDTO {
  email: string
  date_of_birth: string | null
  preferences: { genres?: string[]; onboarded?: boolean }
}

function mapStats(s: StatsDTO): ProfileStats {
  return {
    filmsWatched: s.films_watched,
    reviews: s.reviews,
    avgRating: s.avg_rating,
    ratingHistogram: s.rating_histogram,
    topGenres: s.top_genres,
    topDecades: s.top_decades,
  }
}
function mapPublicProfile(d: PublicProfileDTO): PublicProfile {
  return {
    id: d.id,
    username: d.username,
    displayName: d.display_name,
    bio: d.bio,
    avatarUrl: d.avatar_url,
    joined: d.joined,
    favorites: d.favorites.map(dtoToMovie),
    stats: mapStats(d.stats),
  }
}
function mapMyProfile(d: MyProfileDTO): MyProfile {
  return {
    ...mapPublicProfile(d),
    email: d.email,
    dateOfBirth: d.date_of_birth,
    preferences: d.preferences,
  }
}

// ── API calls ─────────────────────────────────────────────────────────────────

export const api = {
  discovery: async (limit = 20): Promise<Movie[]> => {
    const dtos = await request<MovieDTO[]>(`/recommend/discovery?limit=${limit}`)
    return dtos.map(dtoToMovie)
  },

  search: async (q: string): Promise<Movie[]> => {
    const dtos = await request<MovieDTO[]>(`/movies/search?q=${encodeURIComponent(q)}`)
    return dtos.map(dtoToMovie)
  },

  movieDetail: async (tmdbId: number): Promise<MovieDetail> => {
    const dto = await request<MovieDetailDTO>(`/movies/${tmdbId}/detail`)
    return dtoToMovieDetail(dto)
  },

  libraryBuild: async (limit = 20): Promise<Movie[]> => {
    const dtos = await request<MovieDTO[]>(`/recommend/build?limit=${limit}`)
    return dtos.map(dtoToMovie)
  },

  genres: (): Promise<string[]> => request<string[]>('/movies/genres'),

  watched: async (): Promise<WatchedEntry[]> => {
    const dtos = await request<
      { movie: MovieDTO; rating: number | null; review: string | null; watched_at: string }[]
    >('/library/watched')
    return dtos.map((d) => ({
      movie: dtoToMovie(d.movie),
      rating: d.rating,
      review: d.review,
      watchedAt: d.watched_at,
    }))
  },

  diary: async (): Promise<WatchedEntry[]> => {
    const dtos = await request<
      { movie: MovieDTO; rating: number | null; review: string | null; watched_at: string }[]
    >('/library/diary')
    return dtos.map((d) => ({
      movie: dtoToMovie(d.movie),
      rating: d.rating,
      review: d.review,
      watchedAt: d.watched_at,
    }))
  },

  myProfile: async (): Promise<MyProfile> =>
    mapMyProfile(await request<MyProfileDTO>('/profile/me')),

  profileByUsername: async (username: string): Promise<PublicProfile> =>
    mapPublicProfile(await request<PublicProfileDTO>(`/profile/u/${username}`)),

  updateProfile: async (patch: ProfileUpdate): Promise<MyProfile> => {
    const body: Record<string, unknown> = {}
    if (patch.username !== undefined) body.username = patch.username
    if (patch.displayName !== undefined) body.display_name = patch.displayName
    if (patch.bio !== undefined) body.bio = patch.bio
    if (patch.dateOfBirth !== undefined) body.date_of_birth = patch.dateOfBirth
    if (patch.favoriteFilmIds !== undefined) body.favorite_film_ids = patch.favoriteFilmIds
    return mapMyProfile(
      await request<MyProfileDTO>('/profile/me', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      }),
    )
  },

  uploadAvatar: async (file: File): Promise<MyProfile> => {
    const form = new FormData()
    form.append('file', file)
    return mapMyProfile(
      await request<MyProfileDTO>('/profile/me/avatar', { method: 'POST', body: form }),
    )
  },

  /** Fetches the Letterboxd CSV (auth-gated) and triggers a browser download. */
  exportCsv: async (): Promise<void> => {
    const token = useAuthStore.getState().token
    const res = await fetch(`${BASE}/letterboxd/export`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
    if (res.status === 401) {
      useAuthStore.getState().logout()
      throw new Error('Unauthorized')
    }
    if (!res.ok) throw new Error(`Export failed: ${res.status}`)
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'filmarr-letterboxd.csv'
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  },

  interact: (body: {
    tmdbId: number
    feedType: FeedType
    direction: SwipeDirection
    hearted: boolean
    rating: number | null
    review: string | null
  }) =>
    request<void>('/library/interact', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        tmdb_id: body.tmdbId,
        feed_type: body.feedType,
        direction: body.direction,
        hearted: body.hearted,
        rating: body.rating,
        review: body.review || null,
      }),
    }),
}
