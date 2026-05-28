import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

export function AuthPage() {
  const navigate = useNavigate()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    // TODO: call auth API
    navigate('/discover')
  }

  return (
    <div className="flex flex-col items-center justify-center h-full px-6 safe-top">
      <div className="w-full max-w-sm">
        <h1 className="text-white text-4xl font-black tracking-tighter mb-1">filmarr</h1>
        <p className="text-white/40 text-sm mb-8">Swipe films. Build your taste.</p>

        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <input
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className="w-full px-4 py-3 rounded-xl bg-zinc-800 border border-white/10 text-white placeholder:text-white/30 outline-none focus:border-white/30 transition-colors"
          />
          <input
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className="w-full px-4 py-3 rounded-xl bg-zinc-800 border border-white/10 text-white placeholder:text-white/30 outline-none focus:border-white/30 transition-colors"
          />
          <button
            type="submit"
            className="w-full py-3 rounded-xl bg-white text-black font-semibold text-sm mt-1 active:scale-95 transition-transform"
          >
            {mode === 'login' ? 'Sign in' : 'Create account'}
          </button>
        </form>

        <button
          onClick={() => setMode(mode === 'login' ? 'register' : 'login')}
          className="w-full mt-4 text-white/40 text-sm text-center"
        >
          {mode === 'login' ? "Don't have an account? Sign up" : 'Already have an account? Sign in'}
        </button>

        {/* Skip for now during dev */}
        <button
          onClick={() => navigate('/discover')}
          className="w-full mt-2 text-white/20 text-xs text-center"
        >
          Continue without account →
        </button>
      </div>
    </div>
  )
}
