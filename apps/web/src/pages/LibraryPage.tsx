import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Star, LogOut, Download, Film } from 'lucide-react'
import { useAuthStore } from '@/stores/authStore'
import { api } from '@/lib/api'
import { cn } from '@/lib/utils'
import type { WatchedEntry } from '@/types'

type Tab = 'watched' | 'watchlist'

export function LibraryPage() {
  const navigate = useNavigate()
  const { user, logout } = useAuthStore()
  const [tab, setTab] = useState<Tab>('watched')
  const [entries, setEntries] = useState<WatchedEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [exporting, setExporting] = useState(false)

  useEffect(() => {
    let active = true
    setLoading(true)
    const load = tab === 'watched' ? api.watched() : api.watchlist()
    load
      .then((e) => active && setEntries(e))
      .catch((err) => console.error('load library:', err))
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
  }, [tab])

  const handleExport = async () => {
    setExporting(true)
    try {
      await api.exportCsv()
    } catch (e) {
      console.error('export failed:', e)
    } finally {
      setExporting(false)
    }
  }

  return (
    <div className="h-full overflow-y-auto pb-20 safe-top">
      <div className="px-4 pt-12 pb-3 flex items-start justify-between">
        <div className="min-w-0">
          <h1 className="text-white text-xl font-semibold tracking-tight">Library</h1>
          <p className="text-white/40 text-sm mt-0.5 truncate">
            {user?.email ?? `${entries.length} films logged`}
          </p>
        </div>
        <button
          onClick={logout}
          aria-label="Sign out"
          className="flex items-center gap-1.5 text-white/40 text-xs px-3 py-2 rounded-lg active:scale-95 transition-transform shrink-0"
        >
          <LogOut size={16} />
          Sign out
        </button>
      </div>

      {/* Tabs */}
      <div className="px-4 flex gap-2">
        {(['watched', 'watchlist'] as Tab[]).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={cn(
              'px-4 py-1.5 rounded-full text-sm capitalize transition-colors',
              tab === t ? 'bg-white text-black font-medium' : 'bg-zinc-800 text-white/60',
            )}
          >
            {t}
          </button>
        ))}
      </div>

      {tab === 'watched' && (
        <div className="px-4 py-3">
          <button
            onClick={handleExport}
            disabled={exporting || entries.length === 0}
            className="flex items-center justify-center gap-2 w-full py-2.5 rounded-xl bg-zinc-800 border border-white/10 text-white/80 text-sm font-medium active:scale-[0.98] transition-transform disabled:opacity-40"
          >
            <Download size={16} />
            {exporting ? 'Exporting…' : 'Export to Letterboxd'}
          </button>
        </div>
      )}

      {loading ? (
        <p className="text-white/30 text-sm text-center mt-16">Loading…</p>
      ) : entries.length === 0 ? (
        <div className="flex flex-col items-center justify-center mt-20 px-6 text-center">
          <Film size={40} className="text-white/15" />
          <p className="text-white/50 text-sm mt-4">
            {tab === 'watched' ? 'No films logged yet' : 'Your watchlist is empty'}
          </p>
          <p className="text-white/30 text-xs mt-1">
            {tab === 'watched'
              ? "Swipe right in Build to add films you've seen."
              : 'Swipe down on a film, or tap Watchlist on its page.'}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-3 gap-1 px-1">
          {entries.map(({ movie, rating }) => (
            <button
              key={movie.id}
              onClick={() => navigate(`/film/${movie.tmdbId}`)}
              className="relative aspect-[2/3] rounded-lg overflow-hidden bg-zinc-900 active:scale-95 transition-transform"
            >
              {movie.posterPath ? (
                <img
                  src={`https://image.tmdb.org/t/p/w185${movie.posterPath}`}
                  alt={movie.title}
                  className="absolute inset-0 w-full h-full object-cover"
                />
              ) : (
                <div className="absolute inset-0 flex items-center justify-center p-2 text-center">
                  <span className="text-white/40 text-[11px] leading-tight">{movie.title}</span>
                </div>
              )}
              {rating != null && (
                <div className="absolute bottom-0 inset-x-0 bg-gradient-to-t from-black/80 to-transparent px-1.5 pb-1.5 pt-4">
                  <div className="flex items-center gap-0.5">
                    <Star size={10} fill="#facc15" className="text-yellow-400" />
                    <span className="text-white text-[10px]">{rating}</span>
                  </div>
                </div>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
