import { createApp } from 'vue'
import App from './App.vue'
import { createScaffoldApp } from './shell'
import { installScaffoldUi } from '@yuanda-lowcode-ai/scaffold-ui'
import { installHoverLift, installPressFeedback, installRipple } from '@yuanda-lowcode-ai/scaffold-ui/composables/motion'
import { scaffoldDict, scaffoldHttp, scaffoldPermission } from './api/scaffold-providers'

// 珍珠玻璃主题：设计令牌 → 基础 → 材质 → 控件 → 外壳 → 页面，层叠顺序由包内 theme.css 固定
import '@yuanda-lowcode-ai/scaffold-ui/theme.css'

// 外观设置在挂载前写入 data-theme / data-density，避免首屏闪色
import '@yuanda-lowcode-ai/scaffold-ui/composables/appearance'

/* 底座 app 壳独立装配：只有自己那张视图、路由与外壳插槽表。 */
const { router, warnings } = createScaffoldApp()

if (warnings.length) {
  // 申报被挡不是致命错，但静默吞掉会让人以为自己的页面没生效 —— 至少控制台要说清
  console.warn('[scaffold] 部分宿主申报未生效：\n' + warnings.join('\n'))
}

const app = createApp(App)
// 三项能力必须在挂载前注入：包内组件与 composable 首次取数时就会读取
installScaffoldUi({ http: scaffoldHttp, permission: scaffoldPermission, dict: scaffoldDict })
app.use(router).mount('#app')

installPressFeedback()
installHoverLift()
installRipple()
