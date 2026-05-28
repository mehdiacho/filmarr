import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { AuthPage } from '@/pages/AuthPage'
import { OnboardingPage } from '@/pages/OnboardingPage'
import { DiscoveryPage } from '@/pages/DiscoveryPage'
import { LibraryBuildPage } from '@/pages/LibraryBuildPage'
import { LibraryPage } from '@/pages/LibraryPage'
import { SearchPage } from '@/pages/SearchPage'
import { FilmDetailPage } from '@/pages/FilmDetailPage'
import { ProfilePage } from '@/pages/ProfilePage'
import { SettingsPage } from '@/pages/SettingsPage'
import { DiaryPage } from '@/pages/DiaryPage'
import { NavBar } from '@/components/NavBar'
import { useAuthStore } from '@/stores/authStore'

/** Requires a token; sends unauthenticated users to /auth. */
function RequireToken({ children }: { children: ReactNode }) {
  const token = useAuthStore((s) => s.token)
  if (!token) return <Navigate to="/auth" replace />
  return <>{children}</>
}

/** Requires a token AND a completed onboarding before reaching the feeds. */
function RequireOnboarded({ children }: { children: ReactNode }) {
  const token = useAuthStore((s) => s.token)
  const user = useAuthStore((s) => s.user)
  const location = useLocation()
  if (!token) return <Navigate to="/auth" replace />
  if (!user?.preferences?.onboarded && location.pathname !== '/onboarding') {
    return <Navigate to="/onboarding" replace />
  }
  return <>{children}</>
}

function AppShell() {
  return (
    <div className="relative w-full h-full">
      <Routes>
        <Route path="/discover"     element={<DiscoveryPage />} />
        <Route path="/build"        element={<LibraryBuildPage />} />
        <Route path="/search"       element={<SearchPage />} />
        <Route path="/library"      element={<LibraryPage />} />
        <Route path="/film/:tmdbId" element={<FilmDetailPage />} />
        <Route path="/profile"      element={<ProfilePage />} />
        <Route path="/u/:username"  element={<ProfilePage />} />
        <Route path="/settings"     element={<SettingsPage />} />
        <Route path="/diary"        element={<DiaryPage />} />
        <Route path="*"             element={<Navigate to="/discover" replace />} />
      </Routes>
      <NavBar />
    </div>
  )
}

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/auth" element={<AuthPage />} />
        <Route
          path="/onboarding"
          element={
            <RequireToken>
              <OnboardingPage />
            </RequireToken>
          }
        />
        <Route
          path="/*"
          element={
            <RequireOnboarded>
              <AppShell />
            </RequireOnboarded>
          }
        />
      </Routes>
    </BrowserRouter>
  )
}
