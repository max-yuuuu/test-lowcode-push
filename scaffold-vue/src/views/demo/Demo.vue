<script setup lang="ts">
import { ref } from 'vue'
import { Field, type Column } from '@yuanda-lowcode-ai/scaffold-ui/components/ui'
import { ListPage, PermissionButton, RowActions } from '@yuanda-lowcode-ai/scaffold-ui/components/business'
import { useListPage } from '@yuanda-lowcode-ai/scaffold-ui/composables/list'
import { confirm } from '@yuanda-lowcode-ai/scaffold-ui/composables/confirm'
import { pushToast, toastError } from '@yuanda-lowcode-ai/scaffold-ui/composables/toast'
import { demoApi, type DemoRow } from '@/api/demo/demo'
import DemoDialog from './dialogs/DemoDialog.vue'
type DialogPage = { openCreate(): void; openEdit(id: number): Promise<boolean>; close(): void }
const dialogPages: Record<string, DialogPage | null> = {
  '34': null,
}
function setDialogPage(pageId: string, instance: unknown | null) {
  dialogPages[pageId] = instance as DialogPage | null
}
function requireDialogPage(value: unknown): DialogPage {
  if (typeof value !== 'string' || !dialogPages[value]) throw new Error('引用的弹窗页未生成或不可用：' + String(value))
  return dialogPages[value]!
}
import { onBeforeUnmount, onMounted } from 'vue'
import { createGeneratedPageRuntime, resolveGeneratedTarget } from '@yuanda-lowcode-ai/scaffold-ui/composables/generated-behavior'
import type { PageDesignV2 } from '@yuanda-lowcode-ai/scaffold-ui/capabilities'

/* ============================================================
   由低代码设计器生成的页面。按模型定义生成，属 B 档：每次发布整文件覆盖。
   版式固定走 ListPage + useListPage（查询 / 重置 / 分页 / 导出 / 行操作都是公共能力），
   页面只声明字段、列与动作。要加区块写到 src/views/demo/overrides/ 里；
   要长期人工接管这一页，就把它改名并让发布不再产出它 —— 现在它每次发布都会被整文件覆盖。
   ============================================================ */

type DemoQuery = {
}

const list = useListPage<DemoRow, DemoQuery>({
  query: {
  },
  fetch: params => demoApi.list(params),
  exporter: params => demoApi.export(params),
  exportName: '测试模型.csv',
})

// 页面行为（2.0）：装配共享执行内核；页面动作接列表/提示/确认，方法实例与 operation 由 overrides 注入。
const behaviorDocument = {"flows":[{"id":"flow:component:372f7992-553f-439c-a2fc-87eeb006c709:click","name":"click","edges":[{"id":"flow:component:372f7992-553f-439c-a2fc-87eeb006c709:click#edge-mwyx3hhz-2","port":"next","source":"flow:component:372f7992-553f-439c-a2fc-87eeb006c709:click#trigger","target":"flow:component:372f7992-553f-439c-a2fc-87eeb006c709:click#pageAction-mwyx3hhz-1"},{"id":"flow:component:372f7992-553f-439c-a2fc-87eeb006c709:click#edge-mwyx3hhz-3","port":"next","source":"flow:component:372f7992-553f-439c-a2fc-87eeb006c709:click#pageAction-mwyx3hhz-1","target":"flow:component:372f7992-553f-439c-a2fc-87eeb006c709:click#end-mwyaa4sr-5"}],"nodes":[{"id":"flow:component:372f7992-553f-439c-a2fc-87eeb006c709:click#trigger","type":"trigger","editor":{"x":164,"y":65}},{"id":"flow:component:372f7992-553f-439c-a2fc-87eeb006c709:click#end-mwyaa4sr-5","type":"end","editor":{"x":164,"y":389.3602166154087}},{"id":"flow:component:372f7992-553f-439c-a2fc-87eeb006c709:click#pageAction-mwyx3hhz-1","type":"pageAction","action":"openPage","editor":{"x":164,"y":209},"params":{"page":{"source":"literal","value":"34"}}}],"policy":{"reentry":"ignore"},"variables":[],"entryNodeId":"flow:component:372f7992-553f-439c-a2fc-87eeb006c709:click#trigger"},{"id":"flow:component:5d710e3a-4e77-4791-8f78-2e42d8fc15dd:click","name":"click","edges":[{"id":"flow:component:5d710e3a-4e77-4791-8f78-2e42d8fc15dd:click#edge-mwyx2tbd-2","port":"next","source":"flow:component:5d710e3a-4e77-4791-8f78-2e42d8fc15dd:click#trigger","target":"flow:component:5d710e3a-4e77-4791-8f78-2e42d8fc15dd:click#pageAction-mwyx2tbc-1"},{"id":"flow:component:5d710e3a-4e77-4791-8f78-2e42d8fc15dd:click#edge-mwyx33hf-4","port":"next","source":"flow:component:5d710e3a-4e77-4791-8f78-2e42d8fc15dd:click#pageAction-mwyx2tbc-1","target":"flow:component:5d710e3a-4e77-4791-8f78-2e42d8fc15dd:click#end-mwyx33hf-3"}],"nodes":[{"id":"flow:component:5d710e3a-4e77-4791-8f78-2e42d8fc15dd:click#trigger","type":"trigger","editor":{"x":120,"y":100}},{"id":"flow:component:5d710e3a-4e77-4791-8f78-2e42d8fc15dd:click#pageAction-mwyx2tbc-1","type":"pageAction","action":"openPage","editor":{"x":120,"y":244},"params":{"page":{"source":"literal","value":"34"}}},{"id":"flow:component:5d710e3a-4e77-4791-8f78-2e42d8fc15dd:click#end-mwyx33hf-3","type":"end","editor":{"x":120,"y":388}}],"policy":{"reentry":"ignore"},"variables":[],"entryNodeId":"flow:component:5d710e3a-4e77-4791-8f78-2e42d8fc15dd:click#trigger"}],"variables":[],"propBindings":[],"eventBindings":[{"id":"binding:component:372f7992-553f-439c-a2fc-87eeb006c709:click","event":"click","flowId":"flow:component:372f7992-553f-439c-a2fc-87eeb006c709:click","source":{"kind":"component","nodeId":"372f7992-553f-439c-a2fc-87eeb006c709"},"enabled":true},{"id":"binding:component:5d710e3a-4e77-4791-8f78-2e42d8fc15dd:click","event":"click","flowId":"flow:component:5d710e3a-4e77-4791-8f78-2e42d8fc15dd:click","source":{"kind":"component","nodeId":"5d710e3a-4e77-4791-8f78-2e42d8fc15dd"},"enabled":true}]} as unknown as PageDesignV2['behavior']
// 页面级能力与组件实例分开：表格刷新/查询是列表代理，不伪装成物料实例。
const behaviorTargets: Record<string, unknown> = {
  '549e6471-c0a0-4eb1-9649-bcc05b7c9ef8': { refresh: () => list.load(), search: () => list.search() },
}
const behaviorComponentTypes: Record<string, string> = {
  'd14d93a4-773f-4f19-88d1-5cb38d4d3552': 'SearchForm',
  'af31a406-8a12-42e1-8e10-5f422a8fa398': 'Toolbar',
  '5d710e3a-4e77-4791-8f78-2e42d8fc15dd': 'Button',
  '0494f595-b0fb-4105-accc-c29ddf5326c9': 'Button',
  '549e6471-c0a0-4eb1-9649-bcc05b7c9ef8': 'DataTable',
  '3f89af6a-2b08-4e28-a2ef-55b0ca7391b2': 'RowActions',
  '372f7992-553f-439c-a2fc-87eeb006c709': 'Button',
  '63d05275-124a-4e59-af7f-6c1151037bcf': 'Button',
}
const pageBehavior = createGeneratedPageRuntime({
  document: { version: '2.0', pageType: 'list', modelRef: 'generated', layout: { id: 'generated', type: 'GeneratedPage' }, behavior: behaviorDocument } as unknown as PageDesignV2,
  host: {
    componentOfNode: (nodeId: string) => behaviorComponentTypes[nodeId],
    resolveTarget: (nodeId, scope) => resolveGeneratedTarget(pageBehavior.targets, nodeId, scope, behaviorTargets),
    runPageAction: async (action: string, args: Record<string, unknown>) => {
      if (action === 'openPage') {
        const page = requireDialogPage(args.page)
        if (args.id === undefined || args.id === null) { page.openCreate(); return true }
        const opened = await page.openEdit(requireActionId(args.id))
        if (!opened) throw new Error('编辑测试模型未成功')
        return opened
      }
      if (action === 'create') {
        const target = args.dialog ?? args.nodeId
        if (typeof target === 'string' && target in dialogPages) { requireDialogPage(target).openCreate(); return true }
        throw new Error('页面动作 create 未配置可用的新增弹窗')
      }
      if (action === 'edit') {
        const target = args.dialog ?? args.nodeId
        if (typeof target === 'string' && target in dialogPages) {
          const opened = await requireDialogPage(target).openEdit(requireActionId(args.id))
          if (!opened) throw new Error('编辑测试模型未成功')
          return opened
        }
        throw new Error('页面动作 edit 未配置可用的编辑弹窗')
      }
      if (action === 'delete') {
        const removed = await remove({ id: requireActionId(args.id) }, args.confirm !== false)
        if (!removed) throw new Error('删除测试模型未成功')
        return removed
      }
      if (action === 'state.set') {
        throw new Error('页面状态「' + String(args.key) + '」未由本页适配')
        return undefined
      }
      if (action === 'refresh') { await list.load(); return undefined }
      if (action === 'toast') { pushToast(String(args.message ?? ''), args.tone === 'danger' ? 'danger' : args.tone === 'ok' ? 'ok' : 'info'); return undefined }
      if (action === 'confirm.ask') { return await confirm({ title: String(args.title ?? '确认'), message: String(args.message ?? ''), danger: Boolean(args.danger) }) }
      // 未适配动作必须失败可见，不能以 undefined 伪装执行成功。
      throw new Error('页面动作「' + action + '」尚未由生成页宿主适配')
    },
    invokeOperation: async (operationId: string, params: Record<string, unknown>, context: { nodeId: string; signal: AbortSignal }) => {
      context.signal.throwIfAborted()
      // 约定：operation 末段是 CRUD 动作或同模型自定义接口编码
      const method = operationId.split('.').pop() ?? ''
      if (!method) throw new Error('operation「' + operationId + '」缺少接口编码')
      const requireOperationId = (operationId: string, params: Record<string, unknown>) => {
        const id = params.id
        if (id === undefined || id === null || id === '') throw new Error('operation「' + operationId + '」缺少 params.id')
        return id as never
      }
      if (method === 'list') return await demoApi.list(params as never, context.signal)
      if (method === 'export') return await demoApi.export(params as never, context.signal)
      if (method === 'create') return await demoApi.create(params as never, context.signal)
      if (method === 'update') return await demoApi.update(requireOperationId(operationId, params), params as never, context.signal)
      if (method === 'detail') return await demoApi.detail(requireOperationId(operationId, params), context.signal)
      if (method === 'remove') return await demoApi.remove(requireOperationId(operationId, params), context.signal)
      return await demoApi.invoke(method, params, context.signal)
    },
  },
})
async function dispatchPageEvent(nodeId: string, event: string, scope: { row?: DemoRow } = {}) {
  const result = await pageBehavior.runtime.dispatch({ kind: 'component', nodeId }, event, scope as never)
  return result
}
async function runDefaultThenFlow(defaultClick: () => unknown, nodeId: string, event: string, scope: { row?: DemoRow } = {}) {
  if (await defaultClick() === false) return
  await dispatchPageEvent(nodeId, event, scope)
}
function requireActionId(value: unknown): number {
  if (typeof value === 'string' && /^[0-9]+$/.test(value)) value = Number(value)
  if (typeof value !== 'number' || !Number.isSafeInteger(value) || value <= 0) throw new Error('页面动作缺少有效主键 id')
  return value
}
// Vue ref 回调把真实组件实例注册到共享运行时；null/卸载时立即注销。
const behaviorTargetDisposers = new Map<string, () => void>()
const behaviorTargetRef = (nodeId: string, key?: string | number) => (instance: unknown | null) => {
  const slot = JSON.stringify([nodeId, key === undefined ? null : [typeof key, key]])
  behaviorTargetDisposers.get(slot)?.()
  behaviorTargetDisposers.delete(slot)
  if (instance != null) behaviorTargetDisposers.set(slot, pageBehavior.targets.register(nodeId, instance, key))
}
function disposeBehaviorTargets() {
  for (const dispose of behaviorTargetDisposers.values()) dispose()
  behaviorTargetDisposers.clear()
}
onMounted(() => {
  void pageBehavior.pageLoad().then(results => {
    if (!pageBehavior.runtime.disposed && results.some(result => result.status === 'failed' && !result.handled)) {
      pushToast('页面初始化失败', 'danger')
    }
  })
})
onBeforeUnmount(() => { disposeBehaviorTargets(); pageBehavior.dispose() })

const columns: Column[] = [
  { key: 'name', title: '名称', minWidth: 160 },
  { key: 'createTime', title: '创建时间', width: 160 },
  { key: 'actions', title: '操作', width: 112, className: 'col-actions' },
]

async function remove(row: Pick<DemoRow, 'id'>, ask = true) {
  if (ask) {
    const ok = await confirm({
      title: '删除测试模型',
      message: '删除后无法恢复。确认删除这条测试模型吗？',
      confirmText: '删除',
      danger: true,
    })
    if (!ok) return false
  }
  try {
    await demoApi.remove(row.id)
    pushToast('测试模型已删除')
    await list.load()
    return true
  } catch (error) {
    toastError(error, '测试模型删除失败')
    return false
  }
}
</script>

<template>
  <ListPage
    title="测试模型列表页"
    desc="本页由低代码设计器按模型定义生成；要改字段回到设计器改模型再发布。"
    table-title="测试模型列表"
    :list="list"
    :columns="columns"
  >
    <template #actions>
      <PermissionButton perm="demo:demo:add" variant="primary" icon="plus" @click="dispatchPageEvent('5d710e3a-4e77-4791-8f78-2e42d8fc15dd', 'click')">新增</PermissionButton>
    </template>
    <template #cell-createTime="{ row }"><span class="tnum soft">{{ row.createTime ?? '—' }}</span></template>

    <template #cell-actions="{ row }">
      <RowActions label="更多">
        <PermissionButton perm="demo:demo:edit" icon-only icon="edit" label="编辑" :ref="behaviorTargetRef('372f7992-553f-439c-a2fc-87eeb006c709', row.id)" @click="dispatchPageEvent('372f7992-553f-439c-a2fc-87eeb006c709', 'click', { row })" />
        <PermissionButton perm="demo:demo:remove" icon-only icon="trash" label="删除" danger :ref="behaviorTargetRef('63d05275-124a-4e59-af7f-6c1151037bcf', row.id)" @click="remove(row)" />
      </RowActions>
    </template>
  </ListPage>

  <DemoDialog :ref="instance => setDialogPage('34', instance)" @saved="list.load()" />
</template>
