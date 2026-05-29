export interface CropArea {
  x: number
  y: number
  width: number
  height: number
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.addEventListener('load', () => resolve(img))
    img.addEventListener('error', reject)
    img.src = src
  })
}

/**
 * Crops `imageSrc` to `area` (pixels) and returns a square JPEG blob, capped at
 * 512px, suitable for an avatar upload.
 */
export async function getCroppedBlob(imageSrc: string, area: CropArea): Promise<Blob> {
  const image = await loadImage(imageSrc)
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('Canvas not supported')

  const out = Math.min(512, Math.round(area.width))
  canvas.width = out
  canvas.height = out

  ctx.drawImage(
    image,
    area.x,
    area.y,
    area.width,
    area.height,
    0,
    0,
    out,
    out,
  )

  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => (blob ? resolve(blob) : reject(new Error('Crop failed'))),
      'image/jpeg',
      0.9,
    )
  })
}

/** Reads a File into a data URL for cropping. */
export function fileToDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}
