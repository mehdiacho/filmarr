import { useEffect, useState } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { api } from '@/lib/api'
import { UserRow } from '@/components/UserRow'
import { Spinner } from '@/components/Loading'
import type { UserCard } from '@/types'

/** Renders a follower or following list, chosen by the route suffix. */
export function UserListPage() {
  const { username } = useParams<{ username: string }>()
  const navigate = useNavigate()
  const location = useLocation()
  const mode: 'followers' | 'following' = location.pathname.endsWith('/following')
    ? 'following'
    : 'followers'

  const [users, setUsers] = useState<UserCard[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!username) return
    setLoading(true)
    const load = mode === 'followers' ? api.followers(username) : api.followingList(username)
    load
      .then(setUsers)
      .catch((e) => console.error('user list:', e))
      .finally(() => setLoading(false))
  }, [username, mode])

  return (
    <div className="h-full overflow-y-auto pb-20 safe-top">
      <div className="px-4 pt-12 flex items-center gap-3">
        <button onClick={() => navigate(-1)} aria-label="Back" className="text-white/60 active:scale-95">
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-white text-lg font-semibold capitalize">{mode}</h1>
      </div>

      <div className="px-4 mt-2">
        {loading ? (
          <div className="flex justify-center mt-12"><Spinner /></div>
        ) : users.length === 0 ? (
          <p className="text-white/30 text-sm text-center mt-12">
            {mode === 'followers' ? 'No followers yet.' : 'Not following anyone yet.'}
          </p>
        ) : (
          <div className="flex flex-col divide-y divide-white/5">
            {users.map((u) => (
              <UserRow key={u.id} user={u} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
