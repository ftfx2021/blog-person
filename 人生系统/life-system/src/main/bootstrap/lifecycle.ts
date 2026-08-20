import type { App } from 'electron'
import { BrowserWindow } from 'electron'
import { installSecurityPolicy } from './security.js'
import { createMainWindow } from './window.js'

export function registerApplicationLifecycle(application: App): void {
  void application.whenReady().then(() => {
    installSecurityPolicy()
    createMainWindow()

    application.on('activate', () => {
      if (BrowserWindow.getAllWindows().length === 0) createMainWindow()
    })
  })

  application.on('window-all-closed', () => {
    if (process.platform !== 'darwin') application.quit()
  })
}
