import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '@/lib/api'
import { Avatar } from '@/components/Avatar'
import { cn } from '@/lib/utils'
import type { UserCard } from '@/types'

export function UserRow({ user }: { user: UserCard }) {
  const navigate = useNavigate()
  const [following, setFollowing] = useState(user.isFollowing)
  const [busy, setBusy] = useState(false)
  const name = user.displayName || user.username || 'Cinephile'

  const toggle = async (e: React.MouseEvent) => {
    e.stopPropagation()
    if (!user.username || busy) return
    setBusy(true)
    const next = !following
    setFollowing(next) // optimistic
    try {
      next ? await api.follow(user.username) : await api.unfollow(user.username)
    } catch {
      setFollowing(!next) // revert
    } finally {
      setBusy(false)
    }
  }

  return (
    <button
      onClick={() => user.username && navigate(`/u/${user.username}`)}
      className="flex items-center gap-3 w-full py-2.5 text-left active:opacity-70"
    >
      <Avatar url={user.avatarUrl} name={name} size={40} />
      <div className="flex-1 min-w-0">
        <p className="text-white text-sm leading-tight truncate">{name}</p>
        {user.username && <p className="text-white/40 text-xs truncate">@{user.username}</p>}
      </div>
      <span
        onClick={toggle}
        className={cn(
          'px-3 py-1.5 rounded-full text-xs font-medium shrink-0 transition-colors',
          following ? 'bg-zinc-800 text-white/60' : 'bg-white text-black',
          busy && 'opacity-50',
        )}
      >
        {following ? 'Following' : 'Follow'}
      </span>
    </button>
  )
}
