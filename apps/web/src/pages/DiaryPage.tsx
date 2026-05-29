import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, Star } from 'lucide-react'
import { api } from '@/lib/api'
import { Spinner } from '@/components/Loading'
import type { WatchedEntry } from '@/types'

const IMG = 'https://image.tmdb.org/t/p'

function fmtDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

export function DiaryPage() {
  const navigate = useNavigate()
  const [entries, setEntries] = useState<WatchedEntry[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.diary()
      .then(setEntries)
      .catch((e) => console.error('diary:', e))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="h-full overflow-y-auto pb-24 safe-top">
      <div className="px-4 pt-12 flex items-center gap-3">
        <button onClick={() => navigate(-1)} aria-label="Back" className="text-white/60 active:scale-95">
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-white text-lg font-semibold">Diary</h1>
      </div>

      {loading ? (
        <div className="flex justify-center mt-16"><Spinner /></div>
      ) : entries.length === 0 ? (
        <p className="text-white/30 text-sm text-center mt-16">
          Nothing logged yet — swipe right in Build.
        </p>
      ) : (
        <div className="mt-4 flex flex-col divide-y divide-white/5">
          {entries.map((e, i) => (
            <button
              key={`${e.movie.id}-${i}`}
              onClick={() => navigate(`/film/${e.movie.tmdbId}`)}
              className="flex gap-3 px-4 py-3 text-left active:opacity-70"
            >
              <div className="w-10 h-15 rounded-md overflow-hidden bg-zinc-800 shrink-0">
                {e.movie.posterPath && (
                  <img
                    src={`${IMG}/w92${e.movie.posterPath}`}
                    alt={e.movie.title}
                    className="w-full h-full object-cover"
                  />
                )}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-white text-sm leading-tight">
                  {e.movie.title}
                  {e.movie.year ? <span className="text-white/40"> {e.movie.year}</span> : null}
                </p>
                <p className="text-white/30 text-xs mt-0.5">{fmtDate(e.watchedAt)}</p>
                {e.rating != null && (
                  <span className="inline-flex items-center gap-0.5 text-yellow-400 text-xs mt-1">
                    <Star size={11} className="fill-yellow-400" />
                    {e.rating}
                  </span>
                )}
                {e.review && (
                  <p className="text-white/50 text-xs mt-1 line-clamp-2 italic">{e.review}</p>
                )}
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
