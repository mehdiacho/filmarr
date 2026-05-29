import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, X, Search as SearchIcon, Plus, Check } from 'lucide-react'
import { api } from '@/lib/api'
import { AvatarEditor } from '@/components/AvatarEditor'
import { EditableField } from '@/components/EditableField'
import { PageLoader } from '@/components/Loading'
import { useAuthStore } from '@/stores/authStore'
import { cn } from '@/lib/utils'
import type { Movie, MyProfile } from '@/types'

const IMG = 'https://image.tmdb.org/t/p'
const MAX_FAVS = 4

type Availability = 'idle' | 'checking' | 'ok' | 'current' | 'taken' | 'invalid'

export function EditProfilePage() {
  const navigate = useNavigate()
  const [profile, setProfile] = useState<MyProfile | null>(null)

  const [displayName, setDisplayName] = useState('')
  const [username, setUsername] = useState('')
  const [bio, setBio] = useState('')
  const [favorites, setFavorites] = useState<Movie[]>([])
  const [genres, setGenres] = useState<string[]>([])
  const [knownGenres, setKnownGenres] = useState<string[]>([])

  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [avail, setAvail] = useState<Availability>('idle')

  // favorites + genre search
  const [favQuery, setFavQuery] = useState('')
  const [favResults, setFavResults] = useState<Movie[]>([])
  const [genreQuery, setGenreQuery] = useState('')
  const [addingGenre, setAddingGenre] = useState(false)
  const favTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const userTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    api.myProfile().then((p) => {
      setProfile(p)
      setDisplayName(p.displayName ?? '')
      setUsername(p.username ?? '')
      setBio(p.bio ?? '')
      setFavorites(p.favorites)
      setGenres(p.preferences.genres ?? [])
    })
    api.genres().then(setKnownGenres).catch(() => {})
  }, [])

  // live username availability
  useEffect(() => {
    if (userTimer.current) clearTimeout(userTimer.current)
    const u = username.trim().toLowerCase()
    if (!profile) return
    if (u === (profile.username ?? '')) {
      setAvail('current')
      return
    }
    if (u.length < 3) {
      setAvail('invalid')
      return
    }
    setAvail('checking')
    userTimer.current = setTimeout(async () => {
      try {
        const r = await api.usernameAvailable(u)
        setAvail(r.available ? 'ok' : (r.reason as Availability) ?? 'taken')
      } catch {
        setAvail('idle')
      }
    }, 400)
  }, [username, profile])

  // favorites search
  useEffect(() => {
    if (favTimer.current) clearTimeout(favTimer.current)
    const t = favQuery.trim()
    if (t.length < 2) return setFavResults([])
    favTimer.current = setTimeout(async () => {
      try {
        setFavResults(await api.search(t))
      } catch {
        /* ignore */
      }
    }, 350)
  }, [favQuery])

  if (!profile) return <PageLoader />

  const isCustomGenre = (g: string) => !knownGenres.includes(g)
  const usernameBlocks = ['taken', 'invalid'].includes(avail)

  const addFav = (m: Movie) => {
    if (favorites.length >= MAX_FAVS || favorites.some((f) => f.id === m.id)) return
    setFavorites((p) => [...p, m])
    setFavQuery('')
    setFavResults([])
  }
  const addGenre = (g: string) => {
    const v = g.trim()
    if (!v || genres.includes(v)) return
    setGenres((p) => [...p, v])
    setGenreQuery('')
    setAddingGenre(false)
  }

  const genreSuggestions = knownGenres
    .filter((g) => !genres.includes(g))
    .filter((g) => g.toLowerCase().includes(genreQuery.trim().toLowerCase()))
    .slice(0, 8)
  const canAddCustom =
    genreQuery.trim().length > 1 &&
    !knownGenres.some((g) => g.toLowerCase() === genreQuery.trim().toLowerCase()) &&
    !genres.some((g) => g.toLowerCase() === genreQuery.trim().toLowerCase())

  const save = async () => {
    if (usernameBlocks) return
    setSaving(true)
    setError(null)
    setSaved(false)
    try {
      await api.updateProfile({
        username: username.trim().toLowerCase() || undefined,
        displayName,
        bio,
        favoriteFilmIds: favorites.map((f) => f.tmdbId),
      })
      if (genres.length) await useAuthStore.getState().completeOnboarding(genres)
      setSaved(true)
      setTimeout(() => setSaved(false), 2000)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not save')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="h-full overflow-y-auto pb-10">
      {/* Sticky save bar */}
      <div className="sticky top-0 z-20 bg-[#0a0a0a]/95 backdrop-blur safe-top">
        <div className="flex items-center justify-between px-4 py-3">
          <button onClick={() => navigate(-1)} aria-label="Back" className="text-white/60 active:scale-95">
            <ArrowLeft size={20} />
          </button>
          <h1 className="text-white text-base font-semibold">Edit profile</h1>
          <button
            onClick={save}
            disabled={saving || usernameBlocks}
            className="text-sm font-semibold text-black bg-white rounded-full px-4 py-1.5 disabled:opacity-40 active:scale-95"
          >
            {saving ? '…' : saved ? 'Saved ✓' : 'Save'}
          </button>
        </div>
      </div>

      <div className="px-4 pt-4 space-y-6">
        <AvatarEditor
          avatarUrl={profile.avatarUrl}
          name={displayName || username || profile.email}
          onChange={setProfile}
        />

        <EditableField label="Display name" value={displayName} onChange={setDisplayName} placeholder="Your name" maxLength={80} />

        <EditableField
          label="Username"
          value={username}
          onChange={setUsername}
          prefix="@"
          placeholder="username"
          maxLength={30}
          hint="lowercase, numbers, _"
          status={<UsernameStatus state={avail} />}
        />

        <EditableField label="Bio" value={bio} onChange={setBio} multiline maxLength={2000} placeholder="Tell people about your taste…" />

        {/* Favorites */}
        <div>
          <label className="block text-white/80 text-sm font-medium mb-1.5">
            Favorite films <span className="text-white/30 font-normal">({favorites.length}/{MAX_FAVS})</span>
          </label>
          <div className="grid grid-cols-4 gap-2 mb-2">
            {favorites.map((m) => (
              <div key={m.id} className="relative aspect-[2/3] rounded-lg overflow-hidden bg-zinc-800">
                {m.posterPath && <img src={`${IMG}/w185${m.posterPath}`} alt={m.title} className="w-full h-full object-cover" />}
                <button onClick={() => setFavorites((p) => p.filter((f) => f.id !== m.id))} className="absolute top-1 right-1 w-5 h-5 rounded-full bg-black/70 flex items-center justify-center text-white">
                  <X size={12} />
                </button>
              </div>
            ))}
          </div>
          {favorites.length < MAX_FAVS && (
            <>
              <div className="relative">
                <SearchIcon size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-white/30" />
                <input value={favQuery} onChange={(e) => setFavQuery(e.target.value)} placeholder="Search to add…" className="input pl-9" />
              </div>
              {favResults.length > 0 && (
                <div className="mt-1 flex flex-col divide-y divide-white/5 max-h-56 overflow-y-auto rounded-lg bg-zinc-900">
                  {favResults.map((m) => (
                    <button key={m.id} onClick={() => addFav(m)} className="flex items-center gap-2 p-2 text-left active:opacity-70">
                      <div className="w-8 h-12 rounded bg-zinc-800 overflow-hidden shrink-0">
                        {m.posterPath && <img src={`${IMG}/w92${m.posterPath}`} alt="" className="w-full h-full object-cover" />}
                      </div>
                      <span className="text-white/80 text-sm">{m.title} <span className="text-white/30">{m.year || ''}</span></span>
                    </button>
                  ))}
                </div>
              )}
            </>
          )}
        </div>

        {/* Favorite genres */}
        <div>
          <label className="block text-white/80 text-sm font-medium mb-1.5">Favorite genres</label>
          <div className="flex flex-wrap gap-1.5">
            {genres.map((g) => (
              <span
                key={g}
                className={cn(
                  'flex items-center gap-1 px-3 py-1.5 rounded-full text-xs border',
                  isCustomGenre(g)
                    ? 'bg-amber-500/10 text-amber-300 border-amber-400/40'
                    : 'bg-white text-black border-white',
                )}
              >
                {g}
                <button onClick={() => setGenres((p) => p.filter((x) => x !== g))} aria-label={`Remove ${g}`}>
                  <X size={12} />
                </button>
              </span>
            ))}
            <button
              onClick={() => setAddingGenre((v) => !v)}
              className="flex items-center gap-1 px-3 py-1.5 rounded-full text-xs bg-zinc-800 text-white/70 border border-white/10"
            >
              <Plus size={13} /> Add
            </button>
          </div>
          <p className="text-white/25 text-[11px] mt-1.5">Amber chips are custom genres only you defined.</p>

          {addingGenre && (
            <div className="mt-2 rounded-xl bg-zinc-900 border border-white/10 p-2">
              <input
                autoFocus
                value={genreQuery}
                onChange={(e) => setGenreQuery(e.target.value)}
                placeholder="Search or type a custom genre…"
                className="input mb-2"
              />
              <div className="flex flex-wrap gap-1.5">
                {genreSuggestions.map((g) => (
                  <button key={g} onClick={() => addGenre(g)} className="px-3 py-1.5 rounded-full text-xs bg-zinc-800 text-white/70 border border-white/10">
                    {g}
                  </button>
                ))}
                {canAddCustom && (
                  <button
                    onClick={() => addGenre(genreQuery)}
                    className="flex items-center gap-1 px-3 py-1.5 rounded-full text-xs bg-amber-500/15 text-amber-300 border border-amber-400/40"
                  >
                    <Plus size={12} /> Add “{genreQuery.trim()}” (custom)
                  </button>
                )}
              </div>
            </div>
          )}
        </div>

        {error && <p className="text-red-400 text-xs">{error}</p>}
      </div>
    </div>
  )
}

function UsernameStatus({ state }: { state: Availability }) {
  if (state === 'checking') return <span className="text-white/30 text-xs">checking…</span>
  if (state === 'ok') return <span className="text-green-400 text-xs flex items-center gap-0.5"><Check size={12} /> available</span>
  if (state === 'current') return <span className="text-white/30 text-xs">your username</span>
  if (state === 'taken') return <span className="text-red-400 text-xs">taken</span>
  if (state === 'invalid') return <span className="text-white/30 text-xs">3+ chars, a–z 0–9 _</span>
  return null
}
