import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import { initSecurity } from './utils/security'

import './styles/index.scss'

// 初始化安全模块
initSecurity()

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
