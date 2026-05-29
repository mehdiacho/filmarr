import { useEffect, useRef, useState } from 'react'
import { X, ArrowUp, ArrowDown, Heart, Check } from 'lucide-react'
import { SwipeStack } from './SwipeStack'
import type { SwipeCardHandle } from './SwipeCard'
import type { Movie, SwipeDirection, FeedType } from '@/types'

const POSTER_RATIO = 2 / 3 // width / height

/** Fits a 2:3 box inside its container (contain), so the full poster is always
 *  visible — letterboxed with empty space rather than cropped. */
function useContainedSize() {
  const ref = useRef<HTMLDivElement>(null)
  const [size, setSize] = useState<{ w: number; h: number }>({ w: 0, h: 0 })
  useEffect(() => {
    const el = ref.current
    if (!el) return
    const ro = new ResizeObserver(() => {
      const { width: W, height: H } = el.getBoundingClientRect()
      let w = W
      let h = W / POSTER_RATIO
      if (h > H) {
        h = H
        w = H * POSTER_RATIO
      }
      setSize({ w: Math.floor(w), h: Math.floor(h) })
    })
    ro.observe(el)
    return () => ro.disconnect()
  }, [])
  return { ref, size }
}

interface Props {
  deck: Movie[]
  feedType: FeedType
  hearts: Set<number>
  isFocused: boolean
  onSwipe: (movie: Movie, direction: SwipeDirection) => void
  onDoubleTap: (movie: Movie) => void
  onHold: () => void
  onHeart: () => void
}

// Action labels differ a little between the two feeds.
const LABELS: Record<FeedType, Record<SwipeDirection, string>> = {
  discovery: { left: 'Pass', down: 'Maybe', up: 'Watching', right: 'Interested' },
  library_build: { left: 'Not seen', down: 'Plan', up: 'Watching', right: 'Seen' },
}

export function SwipeDeck({
  deck,
  feedType,
  hearts,
  isFocused,
  onSwipe,
  onDoubleTap,
  onHold,
  onHeart,
}: Props) {
  const { ref, size } = useContainedSize()
  const topRef = useRef<SwipeCardHandle>(null)
  const hasCards = deck.length > 0

  const fire = (dir: SwipeDirection) => topRef.current?.swipe(dir)

  // Keyboard controls (desktop): arrows / WASD for swipes, F focus, H heart.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const tag = (document.activeElement?.tagName || '').toLowerCase()
      if (tag === 'input' || tag === 'textarea') return
      const map: Record<string, SwipeDirection> = {
        arrowleft: 'left', a: 'left',
        arrowright: 'right', d: 'right',
        arrowup: 'up', w: 'up',
        arrowdown: 'down', s: 'down',
      }
      const key = e.key.toLowerCase()
      if (map[key]) {
        e.preventDefault()
        fire(map[key])
      } else if (key === 'f') {
        onHold()
      } else if (key === 'h' || key === 'l') {
        onHeart()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onHold, onHeart])

  const labels = LABELS[feedType]
  const topHearted = hasCards && hearts.has(deck[0].id)

  return (
    <div className="flex-1 min-h-0 flex flex-col items-center justify-center gap-4 px-3">
      {/* Aspect-locked card area */}
      <div ref={ref} className="flex-1 min-h-0 w-full flex items-center justify-center">
        <div style={{ width: size.w || undefined, height: size.h || undefined }} className="relative">
          <SwipeStack
            deck={deck}
            feedType={feedType}
            hearts={hearts}
            isFocused={isFocused}
            onSwipe={onSwipe}
            onDoubleTap={onDoubleTap}
            onHold={onHold}
            topCardRef={topRef}
          />
        </div>
      </div>

      {/* Action buttons (work on desktop + mobile) */}
      <div className="flex items-center justify-center gap-3 pb-2">
        <ActionBtn label={labels.left} onClick={() => fire('left')} disabled={!hasCards}>
          <X size={22} />
        </ActionBtn>
        <ActionBtn label={labels.down} onClick={() => fire('down')} disabled={!hasCards} small>
          <ArrowDown size={18} />
        </ActionBtn>
        {feedType === 'discovery' && (
          <ActionBtn label="Heart" onClick={onHeart} disabled={!hasCards} small>
            <Heart size={18} className={topHearted ? 'fill-rose-500 text-rose-500' : ''} />
          </ActionBtn>
        )}
        <ActionBtn label={labels.up} onClick={() => fire('up')} disabled={!hasCards} small>
          <ArrowUp size={18} />
        </ActionBtn>
        <ActionBtn
          label={labels.right}
          onClick={() => fire('right')}
          disabled={!hasCards}
          accent
        >
          {feedType === 'discovery' ? <Heart size={22} /> : <Check size={22} />}
        </ActionBtn>
      </div>

      {/* Keyboard hint — desktop / pointer devices only */}
      <p className="hidden md:block text-white/25 text-[11px] pb-1">
        ← → ↑ ↓ or W A S D to swipe · F to review · H to heart
      </p>
    </div>
  )
}

function ActionBtn({
  children,
  label,
  onClick,
  disabled,
  accent,
  small,
}: {
  children: React.ReactNode
  label: string
  onClick: () => void
  disabled: boolean
  accent?: boolean
  small?: boolean
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      title={label}
      className={[
        'rounded-full flex items-center justify-center border transition-transform active:scale-90 disabled:opacity-30',
        small ? 'w-11 h-11' : 'w-14 h-14',
        accent
          ? 'bg-white text-black border-white'
          : 'bg-zinc-800/80 text-white/80 border-white/10',
      ].join(' ')}
    >
      {children}
    </button>
  )
}
