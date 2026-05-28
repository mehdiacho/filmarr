import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export interface AuthUser {
  id: number
  email: string
}

interface AuthState {
  token: string | null
  user: AuthUser | null
  loading: boolean
  error: string | null

  login: (email: string, password: string) => Promise<boolean>
  register: (email: string, password: string) => Promise<boolean>
  logout: () => void
  clearError: () => void
}

/** Auth endpoints are called directly (not via lib/api.ts) to avoid a circular
 *  import — lib/api.ts reads the token from this store. */
async function postAuth(path: string, body: unknown): Promise<{ access_token: string }> {
  const res = await fetch(`/api/auth${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    const data = (await res.json().catch(() => ({}))) as { detail?: string }
    throw new Error(data.detail || 'Authentication failed')
  }
  return res.json() as Promise<{ access_token: string }>
}

async function fetchMe(token: string): Promise<AuthUser> {
  const res = await fetch('/api/auth/me', {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!res.ok) throw new Error('Could not load profile')
  return res.json() as Promise<AuthUser>
}

async function authenticate(
  path: '/login' | '/register',
  email: string,
  password: string,
  set: (partial: Partial<AuthState>) => void,
): Promise<boolean> {
  set({ loading: true, error: null })
  try {
    const { access_token } = await postAuth(path, { email, password })
    const user = await fetchMe(access_token)
    set({ token: access_token, user, loading: false })
    return true
  } catch (e) {
    set({ error: (e as Error).message, loading: false })
    return false
  }
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      loading: false,
      error: null,

      login: (email, password) => authenticate('/login', email, password, set),
      register: (email, password) => authenticate('/register', email, password, set),
      logout: () => set({ token: null, user: null, error: null }),
      clearError: () => set({ error: null }),
    }),
    {
      name: 'filmarr-auth',
      partialize: (s) => ({ token: s.token, user: s.user }),
    },
  ),
)
