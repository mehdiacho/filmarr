import { Star } from 'lucide-react'

// Placeholder — will pull from user's interactions once API is wired
const MOCK_WATCHED = [
  { id: 1, title: 'Inception', year: 2010, rating: 4.5, posterPath: '/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg' },
  { id: 2, title: 'Interstellar', year: 2014, rating: 5, posterPath: '/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg' },
  { id: 3, title: 'The Dark Knight', year: 2008, rating: 5, posterPath: '/qJ2tW6WMUDux911r6m7haRef0WH.jpg' },
]

export function LibraryPage() {
  return (
    <div className="h-full overflow-y-auto pb-20 safe-top">
      <div className="px-4 pt-12 pb-4">
        <h1 className="text-white text-xl font-semibold tracking-tight">Library</h1>
        <p className="text-white/40 text-sm mt-0.5">{MOCK_WATCHED.length} films logged</p>
      </div>

      <div className="grid grid-cols-3 gap-1 px-1">
        {MOCK_WATCHED.map((m) => (
          <div key={m.id} className="relative aspect-[2/3] rounded-lg overflow-hidden bg-zinc-900">
            <img
              src={`https://image.tmdb.org/t/p/w185${m.posterPath}`}
              alt={m.title}
              className="absolute inset-0 w-full h-full object-cover"
            />
            {m.rating && (
              <div className="absolute bottom-0 inset-x-0 bg-gradient-to-t from-black/80 to-transparent px-1.5 pb-1.5 pt-4">
                <div className="flex items-center gap-0.5">
                  <Star size={10} fill="#facc15" className="text-yellow-400" />
                  <span className="text-white text-[10px]">{m.rating}</span>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
