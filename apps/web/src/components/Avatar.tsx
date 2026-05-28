import { cn } from '@/lib/utils'

interface Props {
  url: string | null
  name: string
  size?: number
  className?: string
}

/** Avatar image with an initials fallback (deterministic color from the name). */
export function Avatar({ url, name, size = 64, className }: Props) {
  const initials = name.trim().slice(0, 2).toUpperCase() || '?'
  const hue = [...name].reduce((a, c) => a + c.charCodeAt(0), 0) % 360

  return (
    <div
      className={cn('rounded-full overflow-hidden shrink-0 flex items-center justify-center', className)}
      style={{ width: size, height: size, background: `hsl(${hue} 40% 25%)` }}
    >
      {url ? (
        <img src={url} alt={name} className="w-full h-full object-cover" />
      ) : (
        <span className="text-white/80 font-semibold" style={{ fontSize: size * 0.36 }}>
          {initials}
        </span>
      )}
    </div>
  )
}
