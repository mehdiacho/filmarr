import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Search as SearchIcon, Star } from 'lucide-react'
import { api } from '@/lib/api'
import type { Movie } from '@/types'

const IMG = 'https://image.tmdb.org/t/p'

export function SearchPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  // Seed from ?q= so the PWA share-target (OS share sheet -> filmarr) works.
  const [q, setQ] = useState(() => searchParams.get('q') ?? '')
  const [results, setResults] = useState<Movie[]>([])
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    if (timer.current) clearTimeout(timer.current)
    const term = q.trim()
    if (term.length < 2) {
      setResults([])
      setSearched(false)
      return
    }
    timer.current = setTimeout(async () => {
      setLoading(true)
      try {
        setResults(await api.search(term))
        setSearched(true)
      } catch (e) {
        console.error('search:', e)
      } finally {
        setLoading(false)
      }
    }, 350)
    return () => {
      if (timer.current) clearTimeout(timer.current)
    }
  }, [q])

  return (
    <div className="h-full flex flex-col safe-top pb-20">
      <div className="px-4 pt-12 pb-3">
        <h1 className="text-white text-xl font-semibold tracking-tight mb-3">Search</h1>
        <div className="relative">
          <SearchIcon
            size={17}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-white/30"
          />
          <input
            autoFocus
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Films, shows…"
            className="w-full pl-9 pr-4 py-2.5 rounded-xl bg-zinc-800 border border-white/10 text-white placeholder:text-white/30 outline-none focus:border-white/30"
          />
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-4">
        {loading && <p className="text-white/30 text-sm mt-6 text-center">Searching…</p>}
        {!loading && searched && results.length === 0 && (
          <p className="text-white/30 text-sm mt-6 text-center">No results for “{q}”.</p>
        )}
        <div className="flex flex-col divide-y divide-white/5">
          {results.map((m) => (
            <button
              key={m.id}
              onClick={() => navigate(`/film/${m.tmdbId}`)}
              className="flex gap-3 py-2.5 text-left active:opacity-70"
            >
              <div className="w-11 h-16 rounded-md overflow-hidden bg-zinc-800 shrink-0">
                {m.posterPath && (
                  <img
                    src={`${IMG}/w92${m.posterPath}`}
                    alt={m.title}
                    className="w-full h-full object-cover"
                  />
                )}
              </div>
              <div className="min-w-0 flex-1 py-1">
                <p className="text-white text-sm leading-tight">{m.title}</p>
                <p className="text-white/40 text-xs mt-0.5">
                  {[m.year || null, m.mediaType === 'tv' ? 'TV' : null]
                    .filter(Boolean)
                    .join(' · ')}
                </p>
                {m.voteAverage > 0 && (
                  <span className="flex items-center gap-0.5 text-white/40 text-xs mt-1">
                    <Star size={11} className="fill-yellow-400 text-yellow-400" />
                    {m.voteAverage.toFixed(1)}
                  </span>
                )}
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
