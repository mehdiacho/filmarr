import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, LogOut, Mail } from 'lucide-react'
import { api } from '@/lib/api'
import { EditableField } from '@/components/EditableField'
import { PageLoader } from '@/components/Loading'
import { useAuthStore } from '@/stores/authStore'
import { cn } from '@/lib/utils'
import type { MyProfile } from '@/types'

export function AccountSettingsPage() {
  const navigate = useNavigate()
  const logout = useAuthStore((s) => s.logout)
  const [profile, setProfile] = useState<MyProfile | null>(null)

  const [phone, setPhone] = useState('')
  const [savingPhone, setSavingPhone] = useState(false)
  const [phoneSaved, setPhoneSaved] = useState(false)

  const [curPw, setCurPw] = useState('')
  const [newPw, setNewPw] = useState('')
  const [pwMsg, setPwMsg] = useState<string | null>(null)
  const [pwBusy, setPwBusy] = useState(false)

  useEffect(() => {
    api.myProfile().then((p) => {
      setProfile(p)
      setPhone(p.phone ?? '')
    })
  }, [])

  const savePhone = async () => {
    setSavingPhone(true)
    setPhoneSaved(false)
    try {
      await api.updateProfile({ phone })
      setPhoneSaved(true)
      setTimeout(() => setPhoneSaved(false), 2000)
    } catch {
      /* ignore */
    } finally {
      setSavingPhone(false)
    }
  }

  const changePw = async () => {
    setPwMsg(null)
    setPwBusy(true)
    try {
      await api.changePassword(curPw, newPw)
      setPwMsg('Password updated ✓')
      setCurPw('')
      setNewPw('')
    } catch (err) {
      setPwMsg(err instanceof Error ? err.message : 'Could not change password')
    } finally {
      setPwBusy(false)
    }
  }

  if (!profile) return <PageLoader />

  return (
    <div className="h-full overflow-y-auto pb-20 safe-top">
      <div className="px-4 pt-12 flex items-center gap-3">
        <button onClick={() => navigate(-1)} aria-label="Back" className="text-white/60 active:scale-95">
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-white text-lg font-semibold">Account</h1>
      </div>

      <div className="px-4 mt-6 space-y-8">
        {/* Account info */}
        <section className="space-y-4">
          <h2 className="text-white/50 text-xs font-medium uppercase tracking-wide">Account info</h2>
          <div>
            <label className="block text-white/80 text-sm font-medium mb-1.5">Email</label>
            <div className="flex items-center gap-2 rounded-xl bg-zinc-900 border border-white/5 px-4 py-3">
              <Mail size={16} className="text-white/30" />
              <span className="text-white/70 text-sm">{profile.email}</span>
            </div>
          </div>
          <EditableField label="Phone" value={phone} onChange={setPhone} placeholder="Add a phone number" hint="optional" />
          <button
            onClick={savePhone}
            disabled={savingPhone}
            className="w-full py-2.5 rounded-xl bg-zinc-800 border border-white/10 text-white/80 text-sm disabled:opacity-40"
          >
            {savingPhone ? 'Saving…' : phoneSaved ? 'Saved ✓' : 'Save contact info'}
          </button>
        </section>

        {/* Password */}
        <section className="space-y-3">
          <h2 className="text-white/50 text-xs font-medium uppercase tracking-wide">Password</h2>
          <input type="password" value={curPw} onChange={(e) => setCurPw(e.target.value)} placeholder="Current password" autoComplete="current-password" className="input" />
          <input type="password" value={newPw} onChange={(e) => setNewPw(e.target.value)} placeholder="New password" autoComplete="new-password" className="input" />
          {pwMsg && <p className={cn('text-xs', pwMsg.includes('✓') ? 'text-green-400' : 'text-red-400')}>{pwMsg}</p>}
          <button
            onClick={changePw}
            disabled={pwBusy || !curPw || newPw.length < 6}
            className="w-full py-2.5 rounded-xl bg-zinc-800 border border-white/10 text-white/80 text-sm disabled:opacity-40"
          >
            {pwBusy ? 'Updating…' : 'Update password'}
          </button>
        </section>

        <button onClick={logout} className="w-full py-3 rounded-xl text-red-400/80 text-sm flex items-center justify-center gap-2">
          <LogOut size={16} /> Sign out
        </button>
      </div>
    </div>
  )
}
