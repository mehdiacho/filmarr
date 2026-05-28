import { useRef, useCallback } from 'react'
import {
  motion,
  useMotionValue,
  useTransform,
  useAnimation,
  AnimatePresence,
} from 'framer-motion'
import { useDrag } from '@use-gesture/react'
import { Heart } from 'lucide-react'
import type { Movie, SwipeDirection, FeedType } from '@/types'
import { DirectionalIndicator } from './DirectionalIndicator'
import { FocusModeOverlay } from './FocusModeOverlay'
import { posterUrl } from '@/lib/utils'
import { cn } from '@/lib/utils'

const SWIPE_THRESHOLD = 100
const VELOCITY_THRESHOLD = 0.5
const HOLD_DELAY_MS = 500
const DOUBLE_TAP_MS = 300

interface Props {
  movie: Movie
  stackIndex: number
  feedType: FeedType
  isFocused: boolean
  isHearted: boolean
  onSwipe: (movie: Movie, direction: SwipeDirection) => void
  onDoubleTap: (movie: Movie) => void
  onHold: () => void
}

export function SwipeCard({
  movie,
  stackIndex,
  feedType,
  isFocused,
  isHearted,
  onSwipe,
  onDoubleTap,
  onHold,
}: Props) {
  const controls = useAnimation()
  const x = useMotionValue(0)
  const y = useMotionValue(0)
  const rotate = useTransform(x, [-250, 0, 250], [-18, 0, 18])

  // Directional indicator opacities — clamped at 1
  const rightOpacity  = useTransform(x, [0, SWIPE_THRESHOLD + 20], [0, 1])
  const leftOpacity   = useTransform(x, [-(SWIPE_THRESHOLD + 20), 0], [1, 0])
  const upOpacity     = useTransform(y, [-(SWIPE_THRESHOLD + 20), 0], [1, 0])
  const downOpacity   = useTransform(y, [0, SWIPE_THRESHOLD + 20], [0, 1])

  const holdTimer   = useRef<ReturnType<typeof setTimeout> | null>(null)
  const hasMoved    = useRef(false)
  const lastTap     = useRef(0)

  const isTop = stackIndex === 0

  const triggerSwipe = useCallback(
    async (dir: SwipeDirection) => {
      const targets: Record<SwipeDirection, { x: number; y: number; rotate: number }> = {
        right: { x:  window.innerWidth  + 300, y: y.get(), rotate:  25 },
        left:  { x: -(window.innerWidth + 300), y: y.get(), rotate: -25 },
        up:    { x: x.get(), y: -(window.innerHeight + 300), rotate:  0 },
        down:  { x: x.get(), y:  window.innerHeight + 300,   rotate:  0 },
      }
      await controls.start({
        ...targets[dir],
        transition: { duration: 0.28, ease: 'easeOut' },
      })
      onSwipe(movie, dir)
    },
    [controls, x, y, movie, onSwipe],
  )

  const snapBack = useCallback(() => {
    controls.start({
      x: 0, y: 0, rotate: 0,
      transition: { type: 'spring', stiffness: 420, damping: 32 },
    })
  }, [controls])

  const bind = useDrag(
    ({ offset: [ox, oy], velocity: [vx, vy], first, last }) => {
      if (!isTop) return

      if (first) {
        hasMoved.current = false
        holdTimer.current = setTimeout(() => {
          if (!hasMoved.current) onHold()
        }, HOLD_DELAY_MS)
      }

      if (Math.abs(ox) > 10 || Math.abs(oy) > 10) {
        hasMoved.current = true
        if (holdTimer.current) { clearTimeout(holdTimer.current); holdTimer.current = null }
      }

      x.set(ox)
      y.set(oy)

      if (last) {
        if (holdTimer.current) { clearTimeout(holdTimer.current); holdTimer.current = null }

        const absX = Math.abs(ox)
        const absY = Math.abs(oy)

        if (absX >= absY) {
          absX > SWIPE_THRESHOLD || Math.abs(vx) > VELOCITY_THRESHOLD
            ? triggerSwipe(ox > 0 ? 'right' : 'left')
            : snapBack()
        } else {
          absY > SWIPE_THRESHOLD || Math.abs(vy) > VELOCITY_THRESHOLD
            ? triggerSwipe(oy > 0 ? 'down' : 'up')
            : snapBack()
        }
      }
    },
    { filterTaps: true, pointer: { touch: true } },
  )

  const handleClick = useCallback(() => {
    if (!isTop) return
    const now = Date.now()
    if (now - lastTap.current < DOUBLE_TAP_MS) {
      lastTap.current = 0
      onDoubleTap(movie)
    } else {
      lastTap.current = now
    }
  }, [isTop, movie, onDoubleTap])

  // Stack visual offsets for cards sitting behind the top card
  const stackY      = stackIndex * 14
  const stackScale  = 1 - stackIndex * 0.04

  return (
    <motion.div
      {...(isTop ? bind() : {})}
      animate={isTop ? controls : { y: stackY, scale: stackScale }}
      initial={{ y: stackY, scale: stackScale }}
      style={isTop ? { x, y, rotate, zIndex: 30 - stackIndex } : { zIndex: 30 - stackIndex }}
      onClick={handleClick}
      className={cn(
        'absolute inset-x-4 top-2 bottom-0 rounded-card overflow-hidden',
        'bg-zinc-900 shadow-[0_8px_40px_rgba(0,0,0,0.6)]',
        isTop ? 'cursor-grab active:cursor-grabbing touch-none' : 'pointer-events-none',
      )}
    >
      {/* Poster */}
      {movie.posterPath ? (
        <img
          src={posterUrl(movie.posterPath) ?? undefined}
          alt={movie.title}
          className="absolute inset-0 w-full h-full object-cover select-none"
          draggable={false}
        />
      ) : (
        <div className="absolute inset-0 bg-zinc-800 flex items-center justify-center">
          <span className="text-white/20 text-6xl font-bold">{movie.title[0]}</span>
        </div>
      )}

      {/* Gradient */}
      <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/20 to-transparent" />

      {/* Heart badge */}
      <AnimatePresence>
        {isHearted && (
          <motion.div
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            exit={{ scale: 0 }}
            className="absolute top-4 right-4 bg-rose-500/90 rounded-full p-1.5"
          >
            <Heart size={16} fill="white" className="text-white" />
          </motion.div>
        )}
      </AnimatePresence>

      {/* Directional indicators */}
      {isTop && (
        <>
          <DirectionalIndicator direction="right" opacity={rightOpacity} feedType={feedType} />
          <DirectionalIndicator direction="left"  opacity={leftOpacity}  feedType={feedType} />
          <DirectionalIndicator direction="up"    opacity={upOpacity}    feedType={feedType} />
          <DirectionalIndicator direction="down"  opacity={downOpacity}  feedType={feedType} />
        </>
      )}

      {/* Movie info */}
      <div className={cn('absolute left-0 right-0 bottom-0 px-5 pb-5', isFocused ? 'pb-52' : 'pb-5')}>
        <h2 className="text-white text-2xl font-bold leading-snug">{movie.title}</h2>
        <div className="flex items-center gap-2 mt-1 text-white/60 text-sm">
          <span>{movie.year}</span>
          {movie.genres.length > 0 && <><span className="text-white/30">·</span><span>{movie.genres.slice(0, 2).join(', ')}</span></>}
          {movie.voteAverage > 0 && <><span className="text-white/30">·</span><span>★ {movie.voteAverage.toFixed(1)}</span></>}
        </div>
      </div>

      {/* Focus mode overlay */}
      <AnimatePresence>
        {isTop && isFocused && <FocusModeOverlay />}
      </AnimatePresence>
    </motion.div>
  )
}
