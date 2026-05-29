import { useEffect, useState, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Star, Play, Check, Clock, Heart, Pencil, Trash2 } from 'lucide-react'
import { api } from '@/lib/api'
import { cn } from '@/lib/utils'
import { StarRating } from '@/components/StarRating'
import type { MovieDetail, ViewerFilmStatus } from '@/types'

const IMG = 'https://image.tmdb.org/t/p'

const STATUS_LABEL: Record<ViewerFilmStatus, string> = {
  watched: 'Watched',
  watching: 'Watching',
  plan: 'On your watchlist',
  not_interested: 'Not interested',
}

export function FilmDetailPage() {
  const { tmdbId } = useParams<{ tmdbId: string }>()
  const navigate = useNavigate()
  const [film, setFilm] = useState<MovieDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [acting, setActing] = useState(false)
  const [editing, setEditing] = useState(false)
  const [rating, setRating] = useState<number | null>(null)
  const [review, setReview] = useState('')

  const load = useCallback(async () => {
    if (!tmdbId) return
    try {
      const f = await api.movieDetail(Number(tmdbId))
      setFilm(f)
      setRating(f.viewer.rating)
      setReview(f.viewer.review ?? '')
    } catch (e) {
      console.error('load film:', e)
    } finally {
      setLoading(false)
    }
  }, [tmdbId])

  useEffect(() => {
    setLoading(true)
    window.scrollTo(0, 0)
    load()
  }, [load])

  const setStatus = async (status: 'watched' | 'plan') => {
    if (!film) return
    setActing(true)
    try {
      await api.logFilm({ tmdbId: film.tmdbId, status, rating, review })
      await load()
    } finally {
      setActing(false)
    }
  }

  const saveReview = async () => {
    if (!film) return
    setActing(true)
    try {
      await api.logFilm({ tmdbId: film.tmdbId, status: 'watched', rating, review })
      setEditing(false)
      await load()
    } finally {
      setActing(false)
    }
  }

  const remove = async () => {
    if (!film) return
    setActing(true)
    try {
      await api.unlogFilm(film.tmdbId)
      setRating(null)
      setReview('')
      await load()
    } finally {
      setActing(false)
    }
  }

  const heart = async () => {
    if (!film) return
    setActing(true)
    try {
      await api.interact({
        tmdbId: film.tmdbId,
        feedType: 'discovery',
        direction: 'down',
        hearted: true,
        rating: null,
        review: null,
      })
      await load()
    } finally {
      setActing(false)
    }
  }

  if (loading) {
    return <div className="h-full flex items-center justify-center text-white/30">Loading…</div>
  }
  if (!film) {
    return (
      <div className="h-full flex flex-col items-center justify-center gap-3">
        <p className="text-white/50">Couldn't load this film.</p>
        <button onClick={() => navigate(-1)} className="text-white/70 text-sm underline">
          Go back
        </button>
      </div>
    )
  }

  const v = film.viewer
  const logged = v.status === 'watched' || v.status === 'plan'

  return (
    <div className="h-full overflow-y-auto pb-24">
      {/* Hero */}
      <div className="relative h-56">
        {film.backdropPath ? (
          <img
            src={`${IMG}/w780${film.backdropPath}`}
            alt=""
            className="absolute inset-0 w-full h-full object-cover"
          />
        ) : (
          <div className="absolute inset-0 bg-zinc-900" />
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-[#0a0a0a] via-[#0a0a0a]/40 to-transparent" />
        <button
          onClick={() => navigate(-1)}
          aria-label="Back"
          className="absolute top-4 left-4 safe-top w-9 h-9 rounded-full bg-black/50 backdrop-blur flex items-center justify-center text-white active:scale-95"
        >
          <ArrowLeft size={18} />
        </button>
      </div>

      <div className="px-4 -mt-16 relative">
        <div className="flex gap-4">
          {film.posterPath && (
            <img
              src={`${IMG}/w185${film.posterPath}`}
              alt={film.title}
              className="w-28 rounded-xl shadow-lg shrink-0"
            />
          )}
          <div className="pt-16 min-w-0">
            <h1 className="text-white text-xl font-bold leading-tight">{film.title}</h1>
            <p className="text-white/50 text-sm mt-1">
              {[film.year, film.runtime ? `${film.runtime} min` : null]
                .filter(Boolean)
                .join(' · ')}
            </p>
            {film.director && (
              <p className="text-white/40 text-xs mt-1">Directed by {film.director}</p>
            )}
            <div className="flex items-center gap-1 mt-2">
              <Star size={14} className="fill-yellow-400 text-yellow-400" />
              <span className="text-white/70 text-sm">{film.voteAverage.toFixed(1)}</span>
            </div>
          </div>
        </div>

        {/* Genres */}
        {film.genres.length > 0 && (
          <div className="flex flex-wrap gap-1.5 mt-4">
            {film.genres.map((g) => (
              <span
                key={g}
                className="px-2.5 py-1 rounded-full bg-zinc-800 text-white/60 text-xs"
              >
                {g}
              </span>
            ))}
          </div>
        )}

        {/* Your status */}
        {v.status && !editing && (
          <div className="mt-4 flex items-center gap-2 text-sm">
            <span className="text-white/70">{STATUS_LABEL[v.status]}</span>
            {v.rating != null && (
              <span className="flex items-center gap-0.5 text-yellow-400">
                <Star size={13} className="fill-yellow-400" />
                {v.rating}
              </span>
            )}
            {v.hearted && <Heart size={13} className="fill-red-500 text-red-500" />}
          </div>
        )}
        {v.review && !editing && (
          <p className="mt-2 text-white/70 text-sm italic border-l-2 border-white/10 pl-3">
            {v.review}
          </p>
        )}

        {/* Quick actions */}
        <div className="grid grid-cols-3 gap-2 mt-5">
          <ActionButton
            icon={<Check size={16} />}
            label="Watched"
            active={v.status === 'watched'}
            disabled={acting}
            onClick={() => setStatus('watched')}
          />
          <ActionButton
            icon={<Clock size={16} />}
            label="Watchlist"
            active={v.status === 'plan'}
            disabled={acting}
            onClick={() => setStatus('plan')}
          />
          <ActionButton
            icon={<Heart size={16} className={v.hearted ? 'fill-red-500 text-red-500' : ''} />}
            label="Interested"
            active={v.hearted}
            disabled={acting}
            onClick={heart}
          />
        </div>

        {/* Rate & review */}
        <div className="mt-3">
          {!editing ? (
            <div className="flex items-center gap-4">
              <button
                onClick={() => setEditing(true)}
                className="flex items-center gap-1.5 text-white/60 text-sm active:scale-95"
              >
                <Pencil size={14} /> {v.rating != null || v.review ? 'Edit rating & review' : 'Rate & review'}
              </button>
              {logged && (
                <button
                  onClick={remove}
                  disabled={acting}
                  className="flex items-center gap-1.5 text-red-400/70 text-sm active:scale-95 disabled:opacity-50"
                >
                  <Trash2 size={14} /> Remove
                </button>
              )}
            </div>
          ) : (
            <div className="rounded-xl bg-zinc-900 border border-white/10 p-4">
              <StarRating value={rating} onChange={setRating} />
              <textarea
                value={review}
                onChange={(e) => setReview(e.target.value)}
                rows={3}
                placeholder="Write a review…"
                className="input mt-3 resize-none"
              />
              <div className="flex gap-2 mt-3">
                <button
                  onClick={saveReview}
                  disabled={acting}
                  className="flex-1 py-2.5 rounded-xl bg-white text-black font-semibold text-sm active:scale-[0.98] disabled:opacity-50"
                >
                  {acting ? 'Saving…' : 'Save'}
                </button>
                <button
                  onClick={() => {
                    setEditing(false)
                    setRating(v.rating)
                    setReview(v.review ?? '')
                  }}
                  className="px-4 py-2.5 rounded-xl bg-zinc-800 text-white/70 text-sm"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Trailer */}
        {film.trailerKey && (
          <a
            href={`https://www.youtube.com/watch?v=${film.trailerKey}`}
            target="_blank"
            rel="noreferrer"
            className="mt-4 flex items-center justify-center gap-2 w-full py-2.5 rounded-xl bg-zinc-800 text-white/80 text-sm active:scale-[0.98] transition-transform"
          >
            <Play size={15} className="fill-white/80" /> Watch trailer
          </a>
        )}

        {/* Synopsis */}
        {film.overview && (
          <section className="mt-6">
            <h2 className="text-white/90 text-sm font-semibold mb-2">Synopsis</h2>
            <p className="text-white/60 text-sm leading-relaxed">{film.overview}</p>
          </section>
        )}

        {/* Cast */}
        {film.cast.length > 0 && (
          <section className="mt-6">
            <h2 className="text-white/90 text-sm font-semibold mb-3">Cast</h2>
            <div className="flex gap-3 overflow-x-auto -mx-4 px-4 pb-1">
              {film.cast.map((c, i) => (
                <div key={`${c.name}-${i}`} className="w-16 shrink-0 text-center">
                  <div className="w-16 h-16 rounded-full overflow-hidden bg-zinc-800 mb-1">
                    {c.profilePath && (
                      <img
                        src={`${IMG}/w185${c.profilePath}`}
                        alt={c.name}
                        className="w-full h-full object-cover"
                      />
                    )}
                  </div>
                  <p className="text-white/70 text-[10px] leading-tight truncate">{c.name}</p>
                  {c.character && (
                    <p className="text-white/30 text-[9px] leading-tight truncate">
                      {c.character}
                    </p>
                  )}
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Where to watch */}
        {film.watchProviders.length > 0 && (
          <section className="mt-6">
            <h2 className="text-white/90 text-sm font-semibold mb-3">Where to watch</h2>
            <div className="flex gap-2 flex-wrap">
              {film.watchProviders.map((p) => (
                <div
                  key={p.name}
                  className="flex items-center gap-2 px-2.5 py-1.5 rounded-lg bg-zinc-800"
                >
                  {p.logoPath && (
                    <img
                      src={`${IMG}/w92${p.logoPath}`}
                      alt={p.name}
                      className="w-5 h-5 rounded"
                    />
                  )}
                  <span className="text-white/70 text-xs">{p.name}</span>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Similar */}
        {film.similar.length > 0 && (
          <section className="mt-6">
            <h2 className="text-white/90 text-sm font-semibold mb-3">Similar films</h2>
            <div className="flex gap-2 overflow-x-auto -mx-4 px-4 pb-1">
              {film.similar.map((s) => (
                <button
                  key={s.tmdbId}
                  onClick={() => navigate(`/film/${s.tmdbId}`)}
                  className="w-24 shrink-0 text-left active:scale-95 transition-transform"
                >
                  <div className="aspect-[2/3] rounded-lg overflow-hidden bg-zinc-800">
                    {s.posterPath && (
                      <img
                        src={`${IMG}/w185${s.posterPath}`}
                        alt={s.title}
                        className="w-full h-full object-cover"
                      />
                    )}
                  </div>
                  <p className="text-white/60 text-[10px] mt-1 truncate">{s.title}</p>
                </button>
              ))}
            </div>
          </section>
        )}
      </div>
    </div>
  )
}

function ActionButton({
  icon,
  label,
  active,
  disabled,
  onClick,
}: {
  icon: React.ReactNode
  label: string
  active: boolean
  disabled: boolean
  onClick: () => void
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={cn(
        'flex flex-col items-center gap-1 py-2.5 rounded-xl border text-xs transition-colors active:scale-95 disabled:opacity-50',
        active
          ? 'bg-white/10 border-white/30 text-white'
          : 'bg-zinc-900 border-white/10 text-white/60',
      )}
    >
      {icon}
      {label}
    </button>
  )
}
