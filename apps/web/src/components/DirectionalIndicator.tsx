import { motion, type MotionValue } from 'framer-motion'
import { Check, X, Clock, Bookmark } from 'lucide-react'
import type { FeedType, SwipeDirection } from '@/types'

const CONFIG: Record<
  FeedType,
  Record<SwipeDirection, { label: string; color: string; Icon: React.ElementType; position: string }>
> = {
  discovery: {
    right:  { label: 'INTERESTED', color: '#22c55e', Icon: Check,    position: 'top-6 right-6 rotate-[-20deg]' },
    left:   { label: 'SKIP',       color: '#ef4444', Icon: X,        position: 'top-6 left-6 rotate-[20deg]' },
    up:     { label: 'WATCHING',   color: '#3b82f6', Icon: Clock,    position: 'bottom-24 right-6 rotate-[-20deg]' },
    down:   { label: 'MAYBE',      color: '#eab308', Icon: Bookmark, position: 'bottom-24 left-6 rotate-[20deg]' },
  },
  library_build: {
    right:  { label: 'WATCHED',       color: '#22c55e', Icon: Check,    position: 'top-6 right-6 rotate-[-20deg]' },
    left:   { label: "HAVEN'T SEEN",  color: '#ef4444', Icon: X,        position: 'top-6 left-6 rotate-[20deg]' },
    up:     { label: 'WATCHING',      color: '#3b82f6', Icon: Clock,    position: 'bottom-24 right-6 rotate-[-20deg]' },
    down:   { label: 'PLAN TO WATCH', color: '#eab308', Icon: Bookmark, position: 'bottom-24 left-6 rotate-[20deg]' },
  },
}

interface Props {
  direction: SwipeDirection
  opacity: MotionValue<number>
  feedType: FeedType
}

export function DirectionalIndicator({ direction, opacity, feedType }: Props) {
  const { label, color, Icon, position } = CONFIG[feedType][direction]

  return (
    <motion.div
      style={{ opacity, borderColor: color, color }}
      className={`absolute ${position} flex items-center gap-1.5 border-[3px] rounded-lg px-3 py-1.5 font-black text-sm tracking-widest select-none pointer-events-none`}
    >
      <Icon size={16} strokeWidth={3} />
      {label}
    </motion.div>
  )
}
