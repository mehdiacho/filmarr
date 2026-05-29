import { Undo2 } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { useUndoStore } from '@/stores/undoStore'
import { useFocusStore } from '@/stores/focusStore'
import { useDiscoveryStore } from '@/stores/discoveryStore'
import { useLibraryBuildStore } from '@/stores/libraryBuildStore'

export function UndoButton() {
  const { canUndo, pop } = useUndoStore()
  const focusStore = useFocusStore()
  const discoveryStore = useDiscoveryStore()
  const libraryStore = useLibraryBuildStore()

  const handleUndo = () => {
    const record = pop()
    if (!record) return

    // Restore focus data if the undone action had rating/review
    if (record.rating !== null || record.review) {
      focusStore.setRating(record.rating)
      focusStore.setReview(record.review)
      focusStore.enterFocus()
    }

    if (record.feedType === 'discovery') {
      discoveryStore.restoreCard(record)
    } else {
      libraryStore.restoreCard(record)
    }
  }

  return (
    <AnimatePresence>
      {canUndo && (
        <motion.button
          initial={{ scale: 0, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          exit={{ scale: 0, opacity: 0 }}
          whileTap={{ scale: 0.9 }}
          onClick={handleUndo}
          className="absolute bottom-24 right-4 z-50 w-12 h-12 rounded-full bg-zinc-800/90 border border-white/10 shadow-xl flex items-center justify-center backdrop-blur-sm"
          aria-label="Undo last swipe"
        >
          <Undo2 size={18} className="text-white/80" />
        </motion.button>
      )}
    </AnimatePresence>
  )
}
