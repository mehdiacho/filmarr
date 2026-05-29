import { useRef, useState, type ReactNode } from 'react'
import { Pencil } from 'lucide-react'

interface Props {
  label: string
  value: string
  onChange: (v: string) => void
  placeholder?: string
  prefix?: string
  multiline?: boolean
  maxLength?: number
  hint?: string
  status?: ReactNode
}

/** A field that's read-only until you tap the pencil — prevents accidental edits.
 *  Value is held by the parent; persistence happens via the page's Save button. */
export function EditableField({
  label,
  value,
  onChange,
  placeholder,
  prefix,
  multiline,
  maxLength,
  hint,
  status,
}: Props) {
  const [editing, setEditing] = useState(false)
  const inputRef = useRef<HTMLInputElement | HTMLTextAreaElement>(null)

  const startEdit = () => {
    setEditing(true)
    requestAnimationFrame(() => inputRef.current?.focus())
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-1.5">
        <label className="text-white/80 text-sm font-medium">
          {label}
          {hint && <span className="text-white/30 font-normal text-xs ml-2">{hint}</span>}
        </label>
        <div className="flex items-center gap-2">
          {status}
          {!editing && (
            <button onClick={startEdit} aria-label={`Edit ${label}`} className="text-white/40 active:scale-90">
              <Pencil size={15} />
            </button>
          )}
        </div>
      </div>
      <div
        className={`flex items-center gap-1 rounded-xl border px-4 ${
          editing ? 'bg-zinc-800 border-white/30' : 'bg-zinc-900 border-white/5'
        }`}
      >
        {prefix && <span className="text-white/30">{prefix}</span>}
        {multiline ? (
          <textarea
            ref={inputRef as React.RefObject<HTMLTextAreaElement>}
            value={value}
            onChange={(e) => onChange(e.target.value)}
            onBlur={() => setEditing(false)}
            readOnly={!editing}
            rows={3}
            maxLength={maxLength}
            placeholder={placeholder}
            className="flex-1 bg-transparent outline-none text-white py-3 resize-none placeholder:text-white/25 read-only:text-white/70"
          />
        ) : (
          <input
            ref={inputRef as React.RefObject<HTMLInputElement>}
            value={value}
            onChange={(e) => onChange(e.target.value)}
            onBlur={() => setEditing(false)}
            readOnly={!editing}
            maxLength={maxLength}
            placeholder={placeholder}
            className="flex-1 bg-transparent outline-none text-white py-3 placeholder:text-white/25 read-only:text-white/70"
          />
        )}
      </div>
    </div>
  )
}
