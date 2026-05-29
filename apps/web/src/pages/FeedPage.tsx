import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Star, Users } from 'lucide-react'
import { api } from '@/lib/api'
import { Avatar } from '@/components/Avatar'
import { Spinner } from '@/components/Loading'
import type { FeedItem } from '@/types'

const IMG = 'https://image.tmdb.org/t/p'
const VERB: Record<FeedItem['status'], string> = {
  watched: 'watched',
  watching: 'is watching',
  plan: 'wants to watch',
}

function ago(iso: string): string {
  const s = Math.floor((Date.now() - new Date(iso).getTime()) / 1000)
  if (s < 60) return 'now'
  if (s < 3600) return `${Math.floor(s / 60)}m`
  if (s < 86400) return `${Math.floor(s / 3600)}h`
  return `${Math.floor(s / 86400)}d`
}

export function FeedPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<FeedItem[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.feed()
      .then(setItems)
      .catch((e) => console.error('feed:', e))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="h-full overflow-y-auto pb-20 safe-top">
      <div className="px-4 pt-12 pb-3">
        <h1 className="text-white text-xl font-semibold tracking-tight">Activity</h1>
      </div>

      {loading ? (
        <div className="flex justify-center mt-16"><Spinner /></div>
      ) : items.length === 0 ? (
        <div className="flex flex-col items-center justify-center mt-20 px-6 text-center">
          <Users size={40} className="text-white/15" />
          <p className="text-white/50 text-sm mt-4">No activity yet</p>
          <p className="text-white/30 text-xs mt-1">Follow people to see what they're watching.</p>
          <button
            onClick={() => navigate('/search')}
            className="mt-4 px-4 py-2 rounded-full bg-white text-black text-sm font-medium"
          >
            Find people
          </button>
        </div>
      ) : (
        <div className="flex flex-col divide-y divide-white/5">
          {items.map((it, i) => {
            const name = it.user.displayName || it.user.username || 'Someone'
            return (
              <div key={`${it.user.id}-${it.movie.id}-${i}`} className="flex gap-3 px-4 py-3">
                <button
                  onClick={() => it.user.username && navigate(`/u/${it.user.username}`)}
                  className="shrink-0 active:opacity-70"
                >
                  <Avatar url={it.user.avatarUrl} name={name} size={36} />
                </button>
                <div className="flex-1 min-w-0">
                  <p className="text-white/80 text-sm leading-snug">
                    <span className="font-medium">{name}</span>{' '}
                    <span className="text-white/50">{VERB[it.status]}</span>{' '}
                    <button
                      onClick={() => navigate(`/film/${it.movie.tmdbId}`)}
                      className="font-medium text-white active:opacity-70"
                    >
                      {it.movie.title}
                    </button>
                    <span className="text-white/30"> · {ago(it.at)}</span>
                  </p>
                  {it.rating != null && (
                    <span className="inline-flex items-center gap-0.5 text-yellow-400 text-xs mt-1">
                      <Star size={11} className="fill-yellow-400" />
                      {it.rating}
                    </span>
                  )}
                  {it.review && (
                    <p className="text-white/50 text-xs mt-1 line-clamp-2 italic">{it.review}</p>
                  )}
                </div>
                <button
                  onClick={() => navigate(`/film/${it.movie.tmdbId}`)}
                  className="w-10 h-15 rounded-md overflow-hidden bg-zinc-800 shrink-0 active:scale-95"
                >
                  {it.movie.posterPath && (
                    <img
                      src={`${IMG}/w92${it.movie.posterPath}`}
                      alt={it.movie.title}
                      className="w-full h-full object-cover"
                    />
                  )}
                </button>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
