/**
 * Thin API client. All paths are relative so Vite's proxy (dev) and
 * Vercel's rewrite (prod) both work transparently.
 */
import type { Movie, FeedType, SwipeDirection } from '@/types'

const BASE = '/api'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, init)
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

// ── API calls ─────────────────────────────────────────────────────────────────

export const api = {
  discovery: async (limit = 20): Promise<Movie[]> => {
    const dtos = await request<MovieDTO[]>(`/recommend/discovery?limit=${limit}`)
    return dtos.map(dtoToMovie)
  },

  libraryBuild: async (limit = 20): Promise<Movie[]> => {
    const dtos = await request<MovieDTO[]>(`/recommend/build?limit=${limit}`)
    return dtos.map(dtoToMovie)
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
