import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { installDebugBridge } from './core/devtools'
import { bindRouter } from './core/navigation'
import { pinia } from './stores'
import { useAuthStore } from './stores/auth'

async function bootstrap(): Promise<void> {
  const app = createApp(App)
  const authStore = useAuthStore(pinia)

  bindRouter(router)
  await authStore.restoreSession()

  app.use(ElementPlus)
  app.use(pinia)
  app.use(router)

  window.addEventListener('jqpro:session-changed', () => {
    authStore.syncFromStorage()
  })

  installDebugBridge(authStore)
  app.mount('#app')
}

void bootstrap()
