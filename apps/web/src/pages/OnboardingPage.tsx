import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { cn } from '@/lib/utils'
import { api } from '@/lib/api'
import { useAuthStore } from '@/stores/authStore'

const MIN_PICKS = 3

export function OnboardingPage() {
  const navigate = useNavigate()
  const { user, completeOnboarding, loading } = useAuthStore()
  const [genres, setGenres] = useState<string[]>([])
  const [selected, setSelected] = useState<Set<string>>(new Set())

  // Already onboarded → skip straight to the feed
  useEffect(() => {
    if (user?.preferences?.onboarded) navigate('/discover', { replace: true })
  }, [user, navigate])

  useEffect(() => {
    api.genres().then(setGenres).catch((e) => console.error('load genres:', e))
  }, [])

  const toggle = (g: string) =>
    setSelected((prev) => {
      const next = new Set(prev)
      next.has(g) ? next.delete(g) : next.add(g)
      return next
    })

  const handleContinue = async () => {
    const ok = await completeOnboarding([...selected])
    if (ok) navigate('/discover', { replace: true })
  }

  const enough = selected.size >= MIN_PICKS

  return (
    <div className="h-full overflow-y-auto safe-top pb-28">
      <div className="px-6 pt-14 max-w-lg mx-auto">
        <h1 className="text-white text-2xl font-bold tracking-tight">
          What are you into?
        </h1>
        <p className="text-white/40 text-sm mt-1">
          Pick at least {MIN_PICKS} genres so we can line up your first films.
        </p>

        <div className="flex flex-wrap gap-2 mt-7">
          {genres.map((g) => {
            const on = selected.has(g)
            return (
              <button
                key={g}
                onClick={() => toggle(g)}
                className={cn(
                  'px-4 py-2 rounded-full text-sm border transition-colors active:scale-95',
                  on
                    ? 'bg-white text-black border-white font-medium'
                    : 'bg-zinc-900 text-white/70 border-white/10',
                )}
              >
                {g}
              </button>
            )
          })}
          {genres.length === 0 && (
            <p className="text-white/30 text-sm">Loading genres…</p>
          )}
        </div>
      </div>

      <div className="fixed bottom-0 inset-x-0 p-6 bg-gradient-to-t from-[#0a0a0a] to-transparent safe-bottom">
        <div className="max-w-lg mx-auto">
          <button
            onClick={handleContinue}
            disabled={!enough || loading}
            className="w-full py-3 rounded-xl bg-white text-black font-semibold text-sm active:scale-[0.98] transition-transform disabled:opacity-40"
          >
            {loading
              ? 'Saving…'
              : enough
                ? 'Start swiping'
                : `Pick ${MIN_PICKS - selected.size} more`}
          </button>
        </div>
      </div>
    </div>
  )
}
