import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function posterUrl(path: string | null, size: 'w185' | 'w342' | 'w500' | 'original' = 'w500') {
  if (!path) return null
  return `https://image.tmdb.org/t/p/${size}${path}`
}

export function backdropUrl(path: string | null) {
  if (!path) return null
  return `https://image.tmdb.org/t/p/w1280${path}`
}
