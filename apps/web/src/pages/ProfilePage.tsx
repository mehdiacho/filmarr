import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Settings, Star, Film, Users } from 'lucide-react'
import { api } from '@/lib/api'
import { Avatar } from '@/components/Avatar'
import { PageLoader } from '@/components/Loading'
import { cn } from '@/lib/utils'
import type { PublicProfile, MyProfile } from '@/types'

const IMG = 'https://image.tmdb.org/t/p'

export function ProfilePage() {
  const { username } = useParams<{ username: string }>()
  const navigate = useNavigate()
  const isOwn = !username
  const [profile, setProfile] = useState<PublicProfile | MyProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [following, setFollowing] = useState(false)
  const [followerCount, setFollowerCount] = useState(0)
  const [followBusy, setFollowBusy] = useState(false)

  useEffect(() => {
    setLoading(true)
    setNotFound(false)
    const p = isOwn ? api.myProfile() : api.profileByUsername(username!)
    p.then((prof) => {
      setProfile(prof)
      setFollowing(prof.isFollowing)
      setFollowerCount(prof.followerCount)
    })
      .catch(() => setNotFound(true))
      .finally(() => setLoading(false))
  }, [username, isOwn])

  const toggleFollow = async () => {
    if (!profile?.username || followBusy) return
    setFollowBusy(true)
    const next = !following
    setFollowing(next)
    setFollowerCount((c) => c + (next ? 1 : -1))
    try {
      next ? await api.follow(profile.username) : await api.unfollow(profile.username)
    } catch {
      setFollowing(!next)
      setFollowerCount((c) => c + (next ? -1 : 1))
    } finally {
      setFollowBusy(false)
    }
  }

  if (loading) {
    return <PageLoader />
  }
  if (notFound || !profile) {
    return (
      <div className="h-full flex flex-col items-center justify-center gap-2">
        <p className="text-white/50">Profile not found.</p>
      </div>
    )
  }

  const name = profile.displayName || profile.username || 'Cinephile'
  const handle = profile.username ? `@${profile.username}` : null
  const joinedYear = new Date(profile.joined).getFullYear()
  const s = profile.stats
  const maxHist = Math.max(1, ...Object.values(s.ratingHistogram))

  return (
    <div className="h-full overflow-y-auto pb-24 safe-top">
      <div className="px-4 pt-12">
        {/* Header */}
        <div className="flex items-start gap-4">
          <Avatar url={profile.avatarUrl} name={name} size={72} />
          <div className="flex-1 min-w-0 pt-1">
            <h1 className="text-white text-lg font-bold leading-tight truncate">{name}</h1>
            {handle && <p className="text-white/40 text-sm">{handle}</p>}
            <p className="text-white/30 text-xs mt-0.5">Joined {joinedYear}</p>
          </div>
          {isOwn && (
            <div className="flex items-center">
              <button
                onClick={() => navigate('/feed')}
                aria-label="Activity"
                className="text-white/50 p-2 active:scale-95"
              >
                <Users size={20} />
              </button>
              <button
                onClick={() => navigate('/settings')}
                aria-label="Edit profile"
                className="text-white/50 p-2 active:scale-95"
              >
                <Settings size={20} />
              </button>
            </div>
          )}
        </div>

        {profile.bio && (
          <p className="text-white/70 text-sm mt-3 leading-relaxed whitespace-pre-line">
            {profile.bio}
          </p>
        )}

        {isOwn && !profile.username && (
          <button
            onClick={() => navigate('/settings')}
            className="mt-3 w-full py-2 rounded-lg bg-white/10 text-white text-sm"
          >
            Set a username to share your profile →
          </button>
        )}

        {/* Followers / following + follow button */}
        <div className="flex items-center gap-4 mt-4">
          <button
            onClick={() => profile.username && navigate(`/u/${profile.username}/followers`)}
            disabled={!profile.username}
            className="text-sm"
          >
            <span className="text-white font-semibold">{followerCount}</span>{' '}
            <span className="text-white/40">followers</span>
          </button>
          <button
            onClick={() => profile.username && navigate(`/u/${profile.username}/following`)}
            disabled={!profile.username}
            className="text-sm"
          >
            <span className="text-white font-semibold">{profile.followingCount}</span>{' '}
            <span className="text-white/40">following</span>
          </button>
          {!isOwn && profile.username && (
            <button
              onClick={toggleFollow}
              disabled={followBusy}
              className={cn(
                'ml-auto px-5 py-1.5 rounded-full text-sm font-medium transition-colors disabled:opacity-50',
                following ? 'bg-zinc-800 text-white/70' : 'bg-white text-black',
              )}
            >
              {following ? 'Following' : 'Follow'}
            </button>
          )}
        </div>

        {/* Stats row */}
        <div className="grid grid-cols-3 gap-2 mt-5">
          <Stat label="Films" value={s.filmsWatched} />
          <Stat label="Reviews" value={s.reviews} />
          <Stat label="Avg rating" value={s.avgRating != null ? s.avgRating.toFixed(1) : '—'} />
        </div>

        {/* Favorites */}
        <section className="mt-6">
          <h2 className="text-white/90 text-sm font-semibold mb-2">Favorite films</h2>
          {profile.favorites.length === 0 ? (
            <p className="text-white/30 text-xs">
              {isOwn ? 'Pick your favorites in settings.' : 'No favorites yet.'}
            </p>
          ) : (
            <div className="grid grid-cols-4 gap-2">
              {profile.favorites.slice(0, 4).map((m) => (
                <button
                  key={m.id}
                  onClick={() => navigate(`/film/${m.tmdbId}`)}
                  className="aspect-[2/3] rounded-lg overflow-hidden bg-zinc-800 active:scale-95 transition-transform"
                >
                  {m.posterPath && (
                    <img
                      src={`${IMG}/w185${m.posterPath}`}
                      alt={m.title}
                      className="w-full h-full object-cover"
                    />
                  )}
                </button>
              ))}
            </div>
          )}
        </section>

        {/* Rating distribution */}
        {s.filmsWatched > 0 && (
          <section className="mt-6">
            <h2 className="text-white/90 text-sm font-semibold mb-2">Ratings</h2>
            <div className="flex items-end gap-1 h-20">
              {Object.entries(s.ratingHistogram).map(([k, v]) => (
                <div key={k} className="flex-1 flex flex-col items-center justify-end gap-1">
                  <div
                    className="w-full bg-yellow-400/70 rounded-sm"
                    style={{ height: `${(v / maxHist) * 100}%`, minHeight: v ? 3 : 0 }}
                  />
                </div>
              ))}
            </div>
            <div className="flex items-center gap-0.5 mt-1 text-white/30 text-[10px]">
              <Star size={9} className="fill-white/30 text-white/30" /> 0.5
              <span className="flex-1" />
              5.0 <Star size={9} className="fill-white/30 text-white/30" />
            </div>
          </section>
        )}

        {/* Top genres */}
        {s.topGenres.length > 0 && (
          <section className="mt-6">
            <h2 className="text-white/90 text-sm font-semibold mb-2">Top genres</h2>
            <div className="flex flex-wrap gap-1.5">
              {s.topGenres.map((g) => (
                <span
                  key={g.name}
                  className="px-2.5 py-1 rounded-full bg-zinc-800 text-white/60 text-xs"
                >
                  {g.name} <span className="text-white/30">{g.count}</span>
                </span>
              ))}
            </div>
          </section>
        )}

        {isOwn && (
          <button
            onClick={() => navigate('/diary')}
            className="mt-6 flex items-center gap-2 text-white/50 text-sm"
          >
            <Film size={15} /> View your diary →
          </button>
        )}
      </div>
    </div>
  )
}

function Stat({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-xl bg-zinc-900 border border-white/5 py-3 text-center">
      <div className="text-white text-lg font-semibold">{value}</div>
      <div className="text-white/40 text-[11px] mt-0.5">{label}</div>
    </div>
  )
}
