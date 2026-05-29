import { useRef, useState } from 'react'
import Cropper, { type Area } from 'react-easy-crop'
import { Camera, Eye, ImageUp, Trash2, Download } from 'lucide-react'
import { Avatar } from '@/components/Avatar'
import { Spinner } from '@/components/Loading'
import { api } from '@/lib/api'
import { getCroppedBlob, fileToDataUrl } from '@/lib/cropImage'
import type { MyProfile } from '@/types'

interface Props {
  avatarUrl: string | null
  name: string
  onChange: (profile: MyProfile) => void
}

export function AvatarEditor({ avatarUrl, name, onChange }: Props) {
  const fileInput = useRef<HTMLInputElement>(null)
  const [menuOpen, setMenuOpen] = useState(false)
  const [viewing, setViewing] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // crop state
  const [cropSrc, setCropSrc] = useState<string | null>(null)
  const [crop, setCrop] = useState({ x: 0, y: 0 })
  const [zoom, setZoom] = useState(1)
  const [area, setArea] = useState<Area | null>(null)

  const pickFile = () => {
    setMenuOpen(false)
    fileInput.current?.click()
  }

  const onFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    e.target.value = '' // allow re-selecting the same file
    if (!file) return
    try {
      setCropSrc(await fileToDataUrl(file))
      setCrop({ x: 0, y: 0 })
      setZoom(1)
    } catch {
      setError('Could not read that image')
    }
  }

  const confirmCrop = async () => {
    if (!cropSrc || !area) return
    setUploading(true)
    setError(null)
    try {
      const blob = await getCroppedBlob(cropSrc, area)
      const updated = await api.uploadAvatar(blob)
      onChange(updated)
      setCropSrc(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed')
    } finally {
      setUploading(false)
    }
  }

  const remove = async () => {
    setMenuOpen(false)
    setUploading(true)
    try {
      onChange(await api.removeAvatar())
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not remove')
    } finally {
      setUploading(false)
    }
  }

  const downloadImage = async () => {
    if (!avatarUrl) return
    try {
      const res = await fetch(avatarUrl)
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'avatar.jpg'
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(url)
    } catch {
      setError('Could not download image')
    }
  }

  return (
    <div className="flex flex-col items-center gap-2">
      <button
        onClick={() => setMenuOpen(true)}
        className="relative active:scale-95 transition-transform"
        aria-label="Change avatar"
      >
        <Avatar url={avatarUrl} name={name} size={88} />
        {uploading && (
          <span className="absolute inset-0 flex items-center justify-center rounded-full bg-black/50">
            <Spinner size={28} />
          </span>
        )}
        <span className="absolute bottom-0 right-0 w-7 h-7 rounded-full bg-white text-black flex items-center justify-center">
          <Camera size={14} />
        </span>
      </button>
      {error && <p className="text-red-400 text-xs">{error}</p>}

      <input
        ref={fileInput}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        className="hidden"
        onChange={onFile}
      />

      {/* Action menu */}
      {menuOpen && (
        <Overlay onClose={() => setMenuOpen(false)}>
          <div className="bg-zinc-900 rounded-2xl p-2 w-64 max-w-[80vw]" onClick={(e) => e.stopPropagation()}>
            {avatarUrl && (
              <MenuItem
                icon={<Eye size={18} />}
                label="View photo"
                onClick={() => {
                  setMenuOpen(false)
                  setViewing(true)
                }}
              />
            )}
            <MenuItem icon={<ImageUp size={18} />} label="Change photo" onClick={pickFile} />
            {avatarUrl && (
              <MenuItem
                icon={<Trash2 size={18} />}
                label="Remove photo"
                destructive
                onClick={remove}
              />
            )}
          </div>
        </Overlay>
      )}

      {/* View photo */}
      {viewing && avatarUrl && (
        <Overlay onClose={() => setViewing(false)}>
          <div className="flex flex-col items-center gap-4" onClick={(e) => e.stopPropagation()}>
            <img
              src={avatarUrl}
              alt={name}
              className="max-w-[80vw] max-h-[70vh] rounded-2xl object-contain"
            />
            <button
              onClick={downloadImage}
              className="flex items-center gap-2 bg-white text-black rounded-full px-4 py-2 text-sm font-medium active:scale-95"
            >
              <Download size={16} /> Save
            </button>
          </div>
        </Overlay>
      )}

      {/* Crop */}
      {cropSrc && (
        <div className="fixed inset-0 z-[60] bg-black flex flex-col">
          <div className="relative flex-1">
            <Cropper
              image={cropSrc}
              crop={crop}
              zoom={zoom}
              aspect={1}
              cropShape="round"
              showGrid={false}
              onCropChange={setCrop}
              onZoomChange={setZoom}
              onCropComplete={(_, px) => setArea(px)}
            />
          </div>
          <div className="p-5 safe-bottom flex flex-col gap-4 bg-black">
            <input
              type="range"
              min={1}
              max={3}
              step={0.01}
              value={zoom}
              onChange={(e) => setZoom(Number(e.target.value))}
              className="w-full accent-white"
              aria-label="Zoom"
            />
            <div className="flex gap-3">
              <button
                onClick={() => setCropSrc(null)}
                disabled={uploading}
                className="flex-1 py-3 rounded-xl bg-zinc-800 text-white/80 text-sm"
              >
                Cancel
              </button>
              <button
                onClick={confirmCrop}
                disabled={uploading}
                className="flex-1 py-3 rounded-xl bg-white text-black font-semibold text-sm disabled:opacity-50"
              >
                {uploading ? 'Uploading…' : 'Use photo'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function Overlay({ children, onClose }: { children: React.ReactNode; onClose: () => void }) {
  return (
    <div
      className="fixed inset-0 z-[60] bg-black/70 backdrop-blur-sm flex items-center justify-center p-6"
      onClick={onClose}
    >
      {children}
    </div>
  )
}

function MenuItem({
  icon,
  label,
  onClick,
  destructive,
}: {
  icon: React.ReactNode
  label: string
  onClick: () => void
  destructive?: boolean
}) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center gap-3 w-full px-3 py-3 rounded-xl text-sm active:bg-white/5 ${
        destructive ? 'text-red-400' : 'text-white/90'
      }`}
    >
      {icon}
      {label}
    </button>
  )
}
