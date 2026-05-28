import { useEffect, useState, useCallback } from 'react'
import { MoreVertical, Heart } from 'lucide-react'
import { useDiscoveryStore } from '@/stores/discoveryStore'
import { useFocusStore, shouldShowDiscardWarning } from '@/stores/focusStore'
import { SwipeStack } from '@/components/SwipeStack'
import { UndoButton } from '@/components/UndoButton'
import { DiscardWarning } from '@/components/DiscardWarning'
import type { Movie, SwipeDirection } from '@/types'

export function DiscoveryPage() {
  const { deck, hearts, swipe, toggleHeart, init } = useDiscoveryStore()
  const focusStore = useFocusStore()
  const [showWarning, setShowWarning] = useState(false)

  useEffect(() => { init() }, [init])

  const handleSwipe = useCallback((movie: Movie, direction: SwipeDirection) => {
    const hadFocusData = focusStore.isActive && (focusStore.rating !== null || focusStore.review)
    if (hadFocusData && direction !== 'right' && shouldShowDiscardWarning()) {
      setShowWarning(true)
      focusStore.incrementWarning()
      setTimeout(() => setShowWarning(false), 3000)
    }
    swipe(movie, direction)
  }, [focusStore, swipe])

  const handleDoubleTap = useCallback((movie: Movie) => {
    toggleHeart(movie.id)
  }, [toggleHeart])

  const handleHold = useCallback(() => {
    focusStore.isActive ? focusStore.exitFocus() : focusStore.enterFocus()
  }, [focusStore])

  return (
    <div className="relative flex flex-col h-full pb-16">
      {/* Header */}
      <div className="flex items-center justify-between px-4 pt-12 pb-2 safe-top">
        <h1 className="text-white text-xl font-semibold tracking-tight">Discover</h1>
        <div className="flex items-center gap-2">
          <button
            onClick={() => toggleHeart(deck[0]?.id)}
            className="w-9 h-9 rounded-full bg-zinc-800/60 flex items-center justify-center"
            aria-label="Heart current card"
          >
            <Heart
              size={17}
              className={deck[0] && hearts.has(deck[0].id) ? 'fill-rose-500 text-rose-500' : 'text-white/50'}
            />
          </button>
          <button
            onClick={focusStore.togglePersistent}
            className="w-9 h-9 rounded-full bg-zinc-800/60 flex items-center justify-center"
            aria-label="Toggle persistent focus mode"
          >
            <MoreVertical size={17} className={focusStore.isPersistent ? 'text-white' : 'text-white/50'} />
          </button>
        </div>
      </div>

      {/* Focus mode badge */}
      {focusStore.isPersistent && (
        <div className="mx-4 mb-1 px-3 py-1 bg-white/10 rounded-full text-white/60 text-xs text-center">
          Focus mode on — all cards in review mode
        </div>
      )}

      {/* Card area */}
      <div className="relative flex-1 mx-0">
        <DiscardWarning visible={showWarning} onDismiss={() => setShowWarning(false)} />
        <SwipeStack
          deck={deck}
          feedType="discovery"
          hearts={hearts}
          isFocused={focusStore.isActive}
          onSwipe={handleSwipe}
          onDoubleTap={handleDoubleTap}
          onHold={handleHold}
        />
      </div>

      <UndoButton />
    </div>
  )
}
