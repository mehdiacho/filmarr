import { useEffect, useState } from 'react'
import { RefreshCw } from 'lucide-react'
import { initPwa } from '@/lib/pwa'

let started = false

export function UpdatePrompt() {
  const [reload, setReload] = useState<(() => void) | null>(null)

  useEffect(() => {
    if (started) return
    started = true
    initPwa((doReload) => setReload(() => doReload))
  }, [])

  if (!reload) return null

  return (
    <div className="fixed inset-x-0 bottom-20 z-50 flex justify-center px-4 pointer-events-none">
      <div className="pointer-events-auto flex items-center gap-3 bg-white text-black rounded-full pl-4 pr-1.5 py-1.5 shadow-lg max-w-sm w-full sm:w-auto">
        <span className="text-sm font-medium flex-1">New version available</span>
        <button
          onClick={reload}
          className="flex items-center gap-1.5 bg-black text-white text-sm font-medium rounded-full px-3 py-1.5 active:scale-95"
        >
          <RefreshCw size={14} /> Reload
        </button>
      </div>
    </div>
  )
}
