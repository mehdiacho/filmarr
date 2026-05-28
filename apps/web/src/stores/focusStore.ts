import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface FocusState {
  isPersistent: boolean
  isActive: boolean
  rating: number | null
  review: string
  warningDismissCount: number

  enterFocus: () => void
  exitFocus: () => void
  togglePersistent: () => void
  setRating: (r: number | null) => void
  setReview: (r: string) => void
  clearData: () => void
  incrementWarning: () => void
}

const WARN_THRESHOLD = 3

export const useFocusStore = create<FocusState>()(
  persist(
    (set, get) => ({
      isPersistent: false,
      isActive: false,
      rating: null,
      review: '',
      warningDismissCount: 0,

      enterFocus: () => set({ isActive: true }),

      exitFocus: () => set({ isActive: false }),

      togglePersistent: () =>
        set((s) => ({
          isPersistent: !s.isPersistent,
          isActive: !s.isPersistent,
        })),

      setRating: (rating) => set({ rating }),

      setReview: (review) => set({ review }),

      clearData: () => set({ rating: null, review: '', isActive: get().isPersistent }),

      incrementWarning: () =>
        set((s) => ({ warningDismissCount: s.warningDismissCount + 1 })),
    }),
    {
      name: 'filmarr-focus',
      partialize: (s) => ({
        warningDismissCount: s.warningDismissCount,
        isPersistent: s.isPersistent,
      }),
    },
  ),
)

export const shouldShowDiscardWarning = () =>
  useFocusStore.getState().warningDismissCount < WARN_THRESHOLD
