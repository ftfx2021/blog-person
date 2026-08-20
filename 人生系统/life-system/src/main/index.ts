import { app } from 'electron'
import { registerApplicationLifecycle } from './bootstrap/lifecycle.js'

registerApplicationLifecycle(app)
