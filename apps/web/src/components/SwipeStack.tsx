import { AnimatePresence } from 'framer-motion'
import type { Ref } from 'react'
import { SwipeCard, type SwipeCardHandle } from './SwipeCard'
import type { Movie, SwipeDirection, FeedType } from '@/types'

interface Props {
  deck: Movie[]
  feedType: FeedType
  hearts: Set<number>
  isFocused: boolean
  onSwipe: (movie: Movie, direction: SwipeDirection) => void
  onDoubleTap: (movie: Movie) => void
  onHold: () => void
  topCardRef?: Ref<SwipeCardHandle>
}

const VISIBLE_STACK = 3

export function SwipeStack({
  deck,
  feedType,
  hearts,
  isFocused,
  onSwipe,
  onDoubleTap,
  onHold,
  topCardRef,
}: Props) {
  const visible = deck.slice(0, VISIBLE_STACK)

  if (visible.length === 0) {
    return (
      <div className="absolute inset-0 rounded-card bg-zinc-900/40 border border-white/5 flex items-center justify-center">
        <p className="text-white/30 text-sm">No more cards right now</p>
      </div>
    )
  }

  return (
    <AnimatePresence>
      {[...visible].reverse().map((movie, reversedIdx) => {
        const stackIndex = visible.length - 1 - reversedIdx
        return (
          <SwipeCard
            key={movie.id}
            ref={stackIndex === 0 ? topCardRef : undefined}
            movie={movie}
            stackIndex={stackIndex}
            feedType={feedType}
            isFocused={isFocused && stackIndex === 0}
            isHearted={hearts.has(movie.id)}
            onSwipe={onSwipe}
            onDoubleTap={onDoubleTap}
            onHold={onHold}
          />
        )
      })}
    </AnimatePresence>
  )
}
