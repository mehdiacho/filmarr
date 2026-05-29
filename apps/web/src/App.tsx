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
import { FeedPage } from '@/pages/FeedPage'
import { UserListPage } from '@/pages/UserListPage'
import { NavBar } from '@/components/NavBar'
import { UpdatePrompt } from '@/components/UpdatePrompt'
import { useAuthStore } from '@/stores/authStore'

/** Requires a token; sends unauthenticated users to /auth, remembering where
 *  they were headed so AuthPage can explain and redirect back after login. */
function RequireToken({ children }: { children: ReactNode }) {
  const token = useAuthStore((s) => s.token)
  const location = useLocation()
  if (!token) {
    return <Navigate to="/auth" replace state={{ from: location.pathname + location.search }} />
  }
  return <>{children}</>
}

/** Requires a token AND a completed onboarding before reaching the feeds. */
function RequireOnboarded({ children }: { children: ReactNode }) {
  const token = useAuthStore((s) => s.token)
  const user = useAuthStore((s) => s.user)
  const location = useLocation()
  if (!token) {
    return <Navigate to="/auth" replace state={{ from: location.pathname + location.search }} />
  }
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
        <Route path="/feed"         element={<FeedPage />} />
        <Route path="/u/:username"            element={<ProfilePage />} />
        <Route path="/u/:username/followers"  element={<UserListPage />} />
        <Route path="/u/:username/following"  element={<UserListPage />} />
        <Route path="/settings"     element={<SettingsPage />} />
        <Route path="/diary"        element={<DiaryPage />} />
        <Route path="*"             element={<Navigate to="/discover" replace />} />
      </Routes>
      <NavBar />
    </div>
  )
}

/** Phone-first frame: caps width and centers on desktop; deck surfaces get a
 *  wider cap so the poster card + side content fit without stretching. */
function Frame({ children }: { children: ReactNode }) {
  const { pathname } = useLocation()
  const wide = pathname === '/discover' || pathname === '/build'
  return <div className={`app-frame${wide ? ' app-frame--wide' : ''}`}>{children}</div>
}

export function App() {
  return (
    <BrowserRouter>
      <Frame>
        <UpdatePrompt />
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
      </Frame>
    </BrowserRouter>
  )
}
