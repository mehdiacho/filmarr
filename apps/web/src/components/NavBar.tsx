import { NavLink } from 'react-router-dom'
import { Compass, Library, BookMarked, Search, User } from 'lucide-react'
import { cn } from '@/lib/utils'

const LINKS = [
  { to: '/discover', icon: Compass,    label: 'Discover' },
  { to: '/build',    icon: BookMarked, label: 'Build'    },
  { to: '/search',   icon: Search,     label: 'Search'   },
  { to: '/library',  icon: Library,    label: 'Library'  },
  { to: '/profile',  icon: User,       label: 'Profile'  },
]

export function NavBar() {
  return (
    <nav className="fixed bottom-0 inset-x-0 z-40 bg-[#0a0a0a]/90 backdrop-blur-md border-t border-white/5 safe-bottom">
      <div className="flex items-center justify-around h-16">
        {LINKS.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                'flex flex-col items-center gap-0.5 flex-1 py-2',
                isActive ? 'text-white' : 'text-white/30',
              )
            }
          >
            <Icon size={21} />
            <span className="text-[10px] tracking-wide">{label}</span>
          </NavLink>
        ))}
      </div>
    </nav>
  )
}
