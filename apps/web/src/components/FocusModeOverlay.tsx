import { motion } from 'framer-motion'
import { StarRating } from './StarRating'
import { useFocusStore } from '@/stores/focusStore'

export function FocusModeOverlay() {
  const { rating, review, setRating, setReview } = useFocusStore()

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: 20 }}
      className="absolute bottom-0 left-0 right-0 p-4 pb-6 bg-gradient-to-t from-black via-black/90 to-transparent"
    >
      <p className="text-white/50 text-xs text-center mb-3 tracking-widest uppercase">
        Swipe right to save · any other direction discards
      </p>
      <StarRating value={rating} onChange={setRating} />
      <textarea
        value={review}
        onChange={(e) => setReview(e.target.value)}
        placeholder="Write a review… (optional)"
        rows={3}
        className="mt-3 w-full resize-none rounded-xl bg-white/10 border border-white/20 text-white placeholder:text-white/30 text-sm px-3 py-2 outline-none focus:border-white/40 transition-colors"
      />
    </motion.div>
  )
}
