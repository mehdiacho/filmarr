import { useEffect, useCallback, useState } from 'react'
import { MoreVertical } from 'lucide-react'
import { useLibraryBuildStore } from '@/stores/libraryBuildStore'
import { useFocusStore, shouldShowDiscardWarning } from '@/stores/focusStore'
import { SwipeStack } from '@/components/SwipeStack'
import { UndoButton } from '@/components/UndoButton'
import { DiscardWarning } from '@/components/DiscardWarning'
import type { Movie, SwipeDirection } from '@/types'

export function LibraryBuildPage() {
  const { deck, swipe, init } = useLibraryBuildStore()
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

  const handleHold = useCallback(() => {
    focusStore.isActive ? focusStore.exitFocus() : focusStore.enterFocus()
  }, [focusStore])

  return (
    <div className="relative flex flex-col h-full pb-16">
      <div className="flex items-center justify-between px-4 pt-12 pb-2 safe-top">
        <div>
          <h1 className="text-white text-xl font-semibold tracking-tight">Build Library</h1>
          <p className="text-white/40 text-xs mt-0.5">Have you seen these?</p>
        </div>
        <button
          onClick={focusStore.togglePersistent}
          className="w-9 h-9 rounded-full bg-zinc-800/60 flex items-center justify-center"
          aria-label="Toggle persistent focus mode"
        >
          <MoreVertical size={17} className={focusStore.isPersistent ? 'text-white' : 'text-white/50'} />
        </button>
      </div>

      {focusStore.isPersistent && (
        <div className="mx-4 mb-1 px-3 py-1 bg-white/10 rounded-full text-white/60 text-xs text-center">
          Focus mode on — log ratings for each card
        </div>
      )}

      {/* Gesture legend */}
      <div className="flex justify-around px-4 mb-2 text-xs">
        {[
          { dir: '→', label: 'Watched', color: 'text-green-400' },
          { dir: '←', label: 'Not seen', color: 'text-red-400' },
          { dir: '↑', label: 'Watching', color: 'text-blue-400' },
          { dir: '↓', label: 'Plan', color: 'text-yellow-400' },
        ].map(({ dir, label, color }) => (
          <span key={dir} className="text-white/40">
            <span className={color}>{dir}</span> {label}
          </span>
        ))}
      </div>

      <div className="relative flex-1">
        <DiscardWarning visible={showWarning} onDismiss={() => setShowWarning(false)} />
        <SwipeStack
          deck={deck}
          feedType="library_build"
          hearts={new Set()}
          isFocused={focusStore.isActive}
          onSwipe={handleSwipe}
          onDoubleTap={() => {}}
          onHold={handleHold}
        />
      </div>

      <UndoButton />
    </div>
  )
}
