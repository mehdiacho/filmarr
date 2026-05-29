import { cn } from '@/lib/utils'

/** The app's single loading style: a thin spinning ring. */
export function Spinner({ size = 24, className }: { size?: number; className?: string }) {
  return (
    <div
      role="status"
      aria-label="Loading"
      className={cn(
        'animate-spin rounded-full border-2 border-white/15 border-t-white/70',
        className,
      )}
      style={{ width: size, height: size }}
    />
  )
}

/** Centered full-area loader for page/section loads. */
export function PageLoader({ className }: { className?: string }) {
  return (
    <div className={cn('h-full flex items-center justify-center', className)}>
      <Spinner size={28} />
    </div>
  )
}

/** Pulsing placeholder block for skeleton layouts. */
export function Skeleton({ className }: { className?: string }) {
  return <div className={cn('animate-pulse bg-white/5 rounded-lg', className)} />
}

/** A grid of poster-shaped skeletons (library/profile grids). */
export function PosterGridSkeleton({ count = 9 }: { count?: number }) {
  return (
    <div className="grid grid-cols-3 gap-1 px-1">
      {Array.from({ length: count }).map((_, i) => (
        <Skeleton key={i} className="aspect-[2/3]" />
      ))}
    </div>
  )
}
