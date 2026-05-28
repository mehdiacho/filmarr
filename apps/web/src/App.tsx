import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import { AuthPage } from '@/pages/AuthPage'
import { DiscoveryPage } from '@/pages/DiscoveryPage'
import { LibraryBuildPage } from '@/pages/LibraryBuildPage'
import { LibraryPage } from '@/pages/LibraryPage'
import { NavBar } from '@/components/NavBar'
import { useAuthStore } from '@/stores/authStore'

function RequireAuth({ children }: { children: ReactNode }) {
  const token = useAuthStore((s) => s.token)
  if (!token) return <Navigate to="/auth" replace />
  return <>{children}</>
}

function AppShell() {
  return (
    <div className="relative w-full h-full">
      <Routes>
        <Route path="/discover" element={<DiscoveryPage />} />
        <Route path="/build"    element={<LibraryBuildPage />} />
        <Route path="/library"  element={<LibraryPage />} />
        <Route path="*"         element={<Navigate to="/discover" replace />} />
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
          path="/*"
          element={
            <RequireAuth>
              <AppShell />
            </RequireAuth>
          }
        />
      </Routes>
    </BrowserRouter>
  )
}
