import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, Camera, X, LogOut, Search as SearchIcon } from 'lucide-react'
import { api } from '@/lib/api'
import { Avatar } from '@/components/Avatar'
import { useAuthStore } from '@/stores/authStore'
import { cn } from '@/lib/utils'
import type { Movie, MyProfile } from '@/types'

const IMG = 'https://image.tmdb.org/t/p'
const MAX_FAVS = 4

export function SettingsPage() {
  const navigate = useNavigate()
  const logout = useAuthStore((s) => s.logout)
  const fileInput = useRef<HTMLInputElement>(null)

  const [profile, setProfile] = useState<MyProfile | null>(null)
  const [username, setUsername] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [bio, setBio] = useState('')
  const [favorites, setFavorites] = useState<Movie[]>([])
  const [genres, setGenres] = useState<string[]>([])
  const [allGenres, setAllGenres] = useState<string[]>([])
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  // change password
  const [curPw, setCurPw] = useState('')
  const [newPw, setNewPw] = useState('')
  const [pwMsg, setPwMsg] = useState<string | null>(null)
  const [pwBusy, setPwBusy] = useState(false)

  // favorites search
  const [favQuery, setFavQuery] = useState('')
  const [favResults, setFavResults] = useState<Movie[]>([])
  const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    api.myProfile().then((p) => {
      setProfile(p)
      setUsername(p.username ?? '')
      setDisplayName(p.displayName ?? '')
      setBio(p.bio ?? '')
      setFavorites(p.favorites)
      setGenres(p.preferences.genres ?? [])
    })
    api.genres().then(setAllGenres).catch(() => {})
  }, [])

  useEffect(() => {
    if (searchTimer.current) clearTimeout(searchTimer.current)
    const term = favQuery.trim()
    if (term.length < 2) {
      setFavResults([])
      return
    }
    searchTimer.current = setTimeout(async () => {
      try {
        setFavResults(await api.search(term))
      } catch {
        /* ignore */
      }
    }, 350)
  }, [favQuery])

  const addFav = (m: Movie) => {
    if (favorites.length >= MAX_FAVS || favorites.some((f) => f.id === m.id)) return
    setFavorites((prev) => [...prev, m])
    setFavQuery('')
    setFavResults([])
  }
  const removeFav = (id: number) =>
    setFavorites((prev) => prev.filter((f) => f.id !== id))

  const toggleGenre = (g: string) =>
    setGenres((prev) => (prev.includes(g) ? prev.filter((x) => x !== g) : [...prev, g]))

  const onAvatar = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    try {
      const updated = await api.uploadAvatar(file)
      setProfile(updated)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Avatar upload failed')
    }
  }

  const save = async () => {
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
      // persist genre changes too (reuses onboarding endpoint)
      if (genres.length) await useAuthStore.getState().completeOnboarding(genres)
      setSaved(true)
      setTimeout(() => setSaved(false), 2000)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not save')
    } finally {
      setSaving(false)
    }
  }

  const changePw = async () => {
    setPwMsg(null)
    setPwBusy(true)
    try {
      await api.changePassword(curPw, newPw)
      setPwMsg('Password updated ✓')
      setCurPw('')
      setNewPw('')
    } catch (err) {
      setPwMsg(err instanceof Error ? err.message : 'Could not change password')
    } finally {
      setPwBusy(false)
    }
  }

  if (!profile) {
    return <div className="h-full flex items-center justify-center text-white/30">Loading…</div>
  }

  const name = displayName || username || profile.email

  return (
    <div className="h-full overflow-y-auto pb-28 safe-top">
      <div className="px-4 pt-12 flex items-center gap-3">
        <button onClick={() => navigate(-1)} aria-label="Back" className="text-white/60 active:scale-95">
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-white text-lg font-semibold">Edit profile</h1>
      </div>

      <div className="px-4 mt-6 space-y-6">
        {/* Avatar */}
        <div className="flex flex-col items-center gap-2">
          <button onClick={() => fileInput.current?.click()} className="relative active:scale-95">
            <Avatar url={profile.avatarUrl} name={name} size={88} />
            <span className="absolute bottom-0 right-0 w-7 h-7 rounded-full bg-white text-black flex items-center justify-center">
              <Camera size={14} />
            </span>
          </button>
          <input
            ref={fileInput}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            className="hidden"
            onChange={onAvatar}
          />
        </div>

        <Field label="Display name">
          <input
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            maxLength={80}
            placeholder="Your name"
            className="input"
          />
        </Field>

        <Field label="Username" hint="lowercase letters, numbers, underscores">
          <div className="flex items-center input gap-1">
            <span className="text-white/30">@</span>
            <input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              maxLength={30}
              placeholder="username"
              className="bg-transparent outline-none flex-1 text-white"
            />
          </div>
        </Field>

        <Field label="Bio">
          <textarea
            value={bio}
            onChange={(e) => setBio(e.target.value)}
            maxLength={2000}
            rows={3}
            placeholder="Tell people about your taste…"
            className="input resize-none"
          />
        </Field>

        {/* Favorites */}
        <Field label={`Favorite films (${favorites.length}/${MAX_FAVS})`}>
          <div className="grid grid-cols-4 gap-2 mb-2">
            {favorites.map((m) => (
              <div key={m.id} className="relative aspect-[2/3] rounded-lg overflow-hidden bg-zinc-800">
                {m.posterPath && (
                  <img src={`${IMG}/w185${m.posterPath}`} alt={m.title} className="w-full h-full object-cover" />
                )}
                <button
                  onClick={() => removeFav(m.id)}
                  className="absolute top-1 right-1 w-5 h-5 rounded-full bg-black/70 flex items-center justify-center text-white"
                >
                  <X size={12} />
                </button>
              </div>
            ))}
          </div>
          {favorites.length < MAX_FAVS && (
            <>
              <div className="relative">
                <SearchIcon size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-white/30" />
                <input
                  value={favQuery}
                  onChange={(e) => setFavQuery(e.target.value)}
                  placeholder="Search to add…"
                  className="input pl-9"
                />
              </div>
              {favResults.length > 0 && (
                <div className="mt-1 flex flex-col divide-y divide-white/5 max-h-56 overflow-y-auto rounded-lg bg-zinc-900">
                  {favResults.map((m) => (
                    <button
                      key={m.id}
                      onClick={() => addFav(m)}
                      className="flex items-center gap-2 p-2 text-left active:opacity-70"
                    >
                      <div className="w-8 h-12 rounded bg-zinc-800 overflow-hidden shrink-0">
                        {m.posterPath && (
                          <img src={`${IMG}/w92${m.posterPath}`} alt="" className="w-full h-full object-cover" />
                        )}
                      </div>
                      <span className="text-white/80 text-sm">
                        {m.title} <span className="text-white/30">{m.year || ''}</span>
                      </span>
                    </button>
                  ))}
                </div>
              )}
            </>
          )}
        </Field>

        {/* Genres */}
        {allGenres.length > 0 && (
          <Field label="Favorite genres">
            <div className="flex flex-wrap gap-1.5">
              {allGenres.map((g) => (
                <button
                  key={g}
                  onClick={() => toggleGenre(g)}
                  className={cn(
                    'px-3 py-1.5 rounded-full text-xs border transition-colors',
                    genres.includes(g)
                      ? 'bg-white text-black border-white'
                      : 'bg-zinc-900 text-white/60 border-white/10',
                  )}
                >
                  {g}
                </button>
              ))}
            </div>
          </Field>
        )}

        {error && <p className="text-red-400 text-xs">{error}</p>}

        <button
          onClick={save}
          disabled={saving}
          className="w-full py-3 rounded-xl bg-white text-black font-semibold text-sm active:scale-[0.98] transition-transform disabled:opacity-50"
        >
          {saving ? 'Saving…' : saved ? 'Saved ✓' : 'Save changes'}
        </button>

        {/* Change password */}
        <div className="pt-6 border-t border-white/5 space-y-3">
          <h2 className="text-white/80 text-sm font-medium">Change password</h2>
          <input
            type="password"
            value={curPw}
            onChange={(e) => setCurPw(e.target.value)}
            placeholder="Current password"
            autoComplete="current-password"
            className="input"
          />
          <input
            type="password"
            value={newPw}
            onChange={(e) => setNewPw(e.target.value)}
            placeholder="New password"
            autoComplete="new-password"
            className="input"
          />
          {pwMsg && (
            <p className={cn('text-xs', pwMsg.includes('✓') ? 'text-green-400' : 'text-red-400')}>
              {pwMsg}
            </p>
          )}
          <button
            onClick={changePw}
            disabled={pwBusy || !curPw || newPw.length < 6}
            className="w-full py-2.5 rounded-xl bg-zinc-800 border border-white/10 text-white/80 text-sm disabled:opacity-40"
          >
            {pwBusy ? 'Updating…' : 'Update password'}
          </button>
        </div>

        <button
          onClick={logout}
          className="w-full py-3 rounded-xl text-red-400/80 text-sm flex items-center justify-center gap-2"
        >
          <LogOut size={16} /> Sign out
        </button>
      </div>
    </div>
  )
}

function Field({
  label,
  hint,
  children,
}: {
  label: string
  hint?: string
  children: React.ReactNode
}) {
  return (
    <div>
      <label className="block text-white/80 text-sm font-medium mb-1.5">
        {label}
        {hint && <span className="text-white/30 font-normal text-xs ml-2">{hint}</span>}
      </label>
      {children}
    </div>
  )
}
