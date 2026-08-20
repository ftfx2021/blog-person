import type { LifeSystemApi } from '../preload/api/index.js'

declare global {
  interface Window {
    lifeSystem: LifeSystemApi
  }
}

export {}
