import { useNavigate } from 'react-router-dom'
import { Clapperboard } from 'lucide-react'

export function NotFoundPage() {
  const navigate = useNavigate()
  return (
    <div className="h-full flex flex-col items-center justify-center px-8 text-center safe-top">
      <Clapperboard size={48} className="text-white/15" />
      <h1 className="text-white text-2xl font-bold mt-5">Page not found</h1>
      <p className="text-white/40 text-sm mt-2 max-w-xs">
        That reel isn't in our archive. The link may be broken or the page moved.
      </p>
      <button
        onClick={() => navigate('/discover', { replace: true })}
        className="mt-6 px-5 py-2.5 rounded-xl bg-white text-black font-semibold text-sm active:scale-95 transition-transform"
      >
        Back to Discover
      </button>
    </div>
  )
}
