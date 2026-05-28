import { AnimatePresence, motion } from 'framer-motion'
import { AlertTriangle } from 'lucide-react'

interface Props {
  visible: boolean
  onDismiss: () => void
}

export function DiscardWarning({ visible, onDismiss }: Props) {
  return (
    <AnimatePresence>
      {visible && (
        <motion.div
          initial={{ opacity: 0, y: -16 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -16 }}
          className="absolute top-4 inset-x-8 z-50 bg-zinc-900/95 border border-yellow-500/40 rounded-xl px-4 py-3 flex items-start gap-3 shadow-xl backdrop-blur-sm"
          onClick={onDismiss}
        >
          <AlertTriangle size={18} className="text-yellow-400 mt-0.5 shrink-0" />
          <p className="text-white/80 text-sm leading-snug">
            Swiping anything other than <strong className="text-white">right</strong> will discard your rating and review.
            <span className="block text-white/40 text-xs mt-0.5">Tap to dismiss</span>
          </p>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
