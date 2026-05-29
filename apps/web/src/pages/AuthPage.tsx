import { useEffect, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'

export function AuthPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { token, login, register, loading, error, clearError } = useAuthStore()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  // Where the user was trying to go before being bounced here (protected route)
  const from = (location.state as { from?: string } | null)?.from
  const dest = from && from !== '/auth' ? from : '/discover'

  // Already signed in → skip the auth screen
  useEffect(() => {
    if (token) navigate(dest, { replace: true })
  }, [token, navigate, dest])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const ok =
      mode === 'login'
        ? await login(email, password)
        : await register(email, password)
    if (ok) navigate(dest, { replace: true })
  }

  const switchMode = () => {
    clearError()
    setMode(mode === 'login' ? 'register' : 'login')
  }

  return (
    <div className="flex flex-col items-center justify-center h-full px-6 safe-top">
      <div className="w-full max-w-sm">
        <h1 className="text-white text-4xl font-black tracking-tighter mb-1">filmarr</h1>
        <p className="text-white/40 text-sm mb-8">Swipe films. Build your taste.</p>

        {from && from !== '/auth' && (
          <div className="mb-5 rounded-xl bg-white/5 border border-white/10 px-4 py-3">
            <p className="text-white/80 text-sm">Sign in to continue</p>
            <p className="text-white/40 text-xs mt-0.5">
              That page needs an account — log in or sign up to view it.
            </p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <input
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoComplete="email"
            className="w-full px-4 py-3 rounded-xl bg-zinc-800 border border-white/10 text-white placeholder:text-white/30 outline-none focus:border-white/30 transition-colors"
          />
          <input
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={6}
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            className="w-full px-4 py-3 rounded-xl bg-zinc-800 border border-white/10 text-white placeholder:text-white/30 outline-none focus:border-white/30 transition-colors"
          />

          {error && <p className="text-red-400 text-xs px-1">{error}</p>}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 rounded-xl bg-white text-black font-semibold text-sm mt-1 active:scale-95 transition-transform disabled:opacity-50"
          >
            {loading ? '…' : mode === 'login' ? 'Sign in' : 'Create account'}
          </button>
        </form>

        <button
          onClick={switchMode}
          className="w-full mt-4 text-white/40 text-sm text-center"
        >
          {mode === 'login'
            ? "Don't have an account? Sign up"
            : 'Already have an account? Sign in'}
        </button>
      </div>
    </div>
  )
}
