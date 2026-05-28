import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthPage } from '@/pages/AuthPage'
import { DiscoveryPage } from '@/pages/DiscoveryPage'
import { LibraryBuildPage } from '@/pages/LibraryBuildPage'
import { LibraryPage } from '@/pages/LibraryPage'
import { NavBar } from '@/components/NavBar'

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
        <Route path="/*"    element={<AppShell />} />
      </Routes>
    </BrowserRouter>
  )
}
