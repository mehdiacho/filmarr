import { Star } from 'lucide-react'
import { cn } from '@/lib/utils'

interface Props {
  value: number | null
  onChange: (v: number) => void
}

export function StarRating({ value, onChange }: Props) {
  const stars = [1, 2, 3, 4, 5]

  return (
    <div className="flex items-center justify-center gap-1">
      {stars.map((star) => {
        const filled = value !== null && value >= star
        const halfFilled = value !== null && value >= star - 0.5 && value < star
        return (
          <button
            key={star}
            type="button"
            onClick={() => onChange(value === star ? star - 0.5 : star)}
            className="p-1 touch-manipulation"
          >
            <Star
              size={28}
              className={cn(
                'transition-colors',
                filled ? 'fill-yellow-400 text-yellow-400' :
                halfFilled ? 'fill-yellow-400/50 text-yellow-400' :
                'fill-transparent text-white/30',
              )}
            />
          </button>
        )
      })}
    </div>
  )
}
