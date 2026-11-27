/* ============================================================
   品牌与全局配置。底座这里只放中性默认：交付项目由初始化时按项目名
   改写下面的 name/shortName（见 ScaffoldTrimPolicy 的品牌交付改写）。
   导航与业务行类型随物料包提供，这里按原路径转出，宿主页面继续从 @/data 引用。
   ============================================================ */
export type { BaseRow, Dept, Menu, NavGroup, NavLeaf } from '@yuanda-lowcode-ai/scaffold-ui/types'

export interface BeianItem {
  /** 备案号显示文本，须与工信部 / 公安备案截图一致 */
  text: string
  /** 官方查询地址 */
  link: string
}

// 可变而非 Object.freeze：装配处要在挂载前 setBrand 覆盖 name/shortName。
// 平台品牌不在这里 —— 它是薄宿主的申报，底座保持中性，交付时按项目名改写。
export const APP = {
  name: '订单中台',
  shortName: '订单中台',
  subtitle: 'Admin Console',
  version: 'v1.0.0',
  /** 登录页左侧介绍位：接入具体业务平台时改这一段 */
  platform: Object.freeze({
    eyebrow: '通用后台底座',
    slogan: ['统一的后台底座，', '专注业务本身。'],
    lead: '用户、角色、菜单、部门、字典与审计开箱即用，新业务模块只需关注字段与流程。',
  }),
  /** 登录页页脚：版权主体公司名称 */
  company: '通用后台管理系统',
  /** 登录页页脚备案条目；脚手架不带备案信息，接入实际站点时按 BeianItem 填写 */
  beian: Object.freeze([] as readonly BeianItem[]),
  /** 多租户开关：底座与交付项目恒为单租户，登录页租户字段、注册租户入口与顶栏用户菜单的租户行都不展示；只有平台薄宿主经 setBrand 翻成 true */
  multiTenant: false,
}

/**
 * 装配处在 createApp / mount 之前注入自己的品牌。路由标题（document.title）与侧栏、登录页模板
 * 都在渲染时读 APP，所以只要在首次渲染前改好，这些读取点不必逐个改成响应式。
 */
export function setBrand(overrides: Partial<Pick<typeof APP, 'name' | 'shortName' | 'subtitle' | 'company' | 'multiTenant'>>): void {
  Object.assign(APP, overrides)
}
