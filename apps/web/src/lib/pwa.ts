import { registerSW } from 'virtual:pwa-register'

/**
 * Registers the service worker and wires up update handling.
 *
 * PWAs stay open for days, so we poll for a new service worker on an interval
 * and whenever the app regains focus. When one is found, `onNeedRefresh` fires
 * and we surface a "new version" prompt; calling the returned updater reloads
 * with fresh assets (skipWaiting + reload). This is the manual refresh path the
 * standalone PWA otherwise lacks.
 */
const UPDATE_POLL_MS = 60_000

export function initPwa(onNeedRefresh: (reload: () => void) => void): void {
  const updateSW = registerSW({
    immediate: true,
    onNeedRefresh() {
      onNeedRefresh(() => updateSW(true))
    },
    onRegisteredSW(_swUrl, registration) {
      if (!registration) return
      const poll = () => {
        if (navigator.onLine) registration.update().catch(() => {})
      }
      setInterval(poll, UPDATE_POLL_MS)
      document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible') poll()
      })
    },
  })
}
