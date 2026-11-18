<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Field, TextInput, type Column } from '@yuanda-lowcode-ai/scaffold-ui/components/ui'
import { FormDialog, ListPage, PermissionButton } from '@yuanda-lowcode-ai/scaffold-ui/components/business'
import { useListPage } from '@yuanda-lowcode-ai/scaffold-ui/composables/list'
import { confirm } from '@yuanda-lowcode-ai/scaffold-ui/composables/confirm'
import { pushToast, toastError } from '@yuanda-lowcode-ai/scaffold-ui/composables/toast'
import { required, validate } from '@yuanda-lowcode-ai/scaffold-ui/utils/validate'
import { t11Verify20261118Api, type T11Verify20261118Row, type T11Verify20261118WriteRequest } from '@/api/t11_verify_20261118/t11_verify20261118'
import { onBeforeUnmount, onMounted } from 'vue'
import { createGeneratedPageRuntime, resolveGeneratedTarget } from '@yuanda-lowcode-ai/scaffold-ui/composables/generated-behavior'
import type { PageDesignV2 } from '@yuanda-lowcode-ai/scaffold-ui/capabilities'

/* ============================================================
   T11验收页面二次修改。由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖。
   版式固定走 ListPage + useListPage（查询 / 重置 / 分页 / 导出 / 行操作都是公共能力），
   页面只声明字段、列与动作。要加区块写到 src/views/t11_verify_20261118/overrides/ 里；
   要长期人工接管这一页，就把它改名并让发布不再产出它 —— 现在它每次发布都会被整文件覆盖。
   ============================================================ */

type T11Verify20261118Query = {
}

const list = useListPage<T11Verify20261118Row, T11Verify20261118Query>({
  query: {
  },
  fetch: params => t11Verify20261118Api.list(params),
  exporter: params => t11Verify20261118Api.export(params),
  exportName: 'T11增删改发布验证-v2.csv',
})

// 页面行为（2.0）：装配共享执行内核；页面动作接列表/提示/确认，方法实例与 operation 由 overrides 注入。
const behaviorDocument = {"flows":[{"id":"flow:page:page:page-load","name":"page-load","edges":[{"id":"flow:page:page:page-load#edge-mwldwznw-2","port":"next","source":"flow:page:page:page-load#trigger","target":"flow:page:page:page-load#api-mwldwznw-1"},{"id":"flow:page:page:page-load#edge-mwldwznw-3","port":"next","source":"flow:page:page:page-load#api-mwldwznw-1","target":"flow:page:page:page-load#end-mwkmqv5u-1"}],"nodes":[{"id":"flow:page:page:page-load#trigger","type":"trigger","editor":{"x":120,"y":100}},{"id":"flow:page:page:page-load#end-mwkmqv5u-1","type":"end","editor":{"x":120,"y":244}},{"id":"flow:page:page:page-load#api-mwldwznw-1","type":"api","editor":{"x":353,"y":159},"params":{},"operationId":"detail"}],"policy":{"reentry":"ignore"},"variables":[],"entryNodeId":"flow:page:page:page-load#trigger"}],"variables":[],"propBindings":[],"eventBindings":[{"id":"binding:page:page:page-load","event":"page-load","flowId":"flow:page:page:page-load","source":{"kind":"page"},"enabled":true}]} as unknown as PageDesignV2['behavior']
// 页面级能力与组件实例分开：表格刷新/查询是列表代理，不伪装成物料实例。
const behaviorTargets: Record<string, unknown> = {
  '39508b98-dd03-4348-b2a9-632b9be9a18c': { refresh: () => list.load(), search: () => list.search() },
}
const behaviorComponentTypes: Record<string, string> = {
  'dd1f08ee-9967-4197-b51c-d5e23d412b09': 'SearchForm',
  'f23373ff-f73a-4d1e-b942-33b07c044508': 'Toolbar',
  'ae256b5b-24d5-41f4-a61d-0f6e74a774a7': 'Button',
  '3d5c2b90-2447-4427-afd6-dc8ff2548e91': 'Button',
  'ff7b8841-ab7d-4363-9141-9cfdfe5f259b': 'SearchForm',
  '39508b98-dd03-4348-b2a9-632b9be9a18c': 'DataTable',
  '79732a73-b257-4457-838f-829a5bd7754e': 'RowActions',
  '516b3277-a7b7-4436-b5aa-ea89e38a81bd': 'Button',
  '38d4e50d-85e3-471d-93de-d4e71b5b25c8': 'Button',
  '8bb2a809-2d06-4cbf-acf2-f9b75d9d0e24': 'FormContainer',
  'e9bb666b-5a45-4cb7-85c9-daaa3e1c35da': 'TextInput',
}
const pageBehavior = createGeneratedPageRuntime({
  document: { version: '2.0', pageType: 'list', modelRef: 'generated', layout: { id: 'generated', type: 'GeneratedPage' }, behavior: behaviorDocument } as unknown as PageDesignV2,
  host: {
    componentOfNode: (nodeId: string) => behaviorComponentTypes[nodeId],
    resolveTarget: (nodeId, scope) => resolveGeneratedTarget(pageBehavior.targets, nodeId, scope, behaviorTargets),
    runPageAction: async (action: string, args: Record<string, unknown>) => {
      if (action === 'refresh') { await list.load(); return undefined }
      if (action === 'toast') { pushToast(String(args.message ?? '')); return undefined }
      if (action === 'confirm.ask') { return await confirm({ title: String(args.title ?? '确认'), message: String(args.message ?? ''), danger: Boolean(args.danger) }) }
      // 未适配动作必须失败可见，不能以 undefined 伪装执行成功。
      throw new Error('页面动作「' + action + '」尚未由生成页宿主适配')
    },
    invokeOperation: async (operationId: string, params: Record<string, unknown>, context: { nodeId: string; signal: AbortSignal }) => {
      // 约定：operation 末段是 CRUD 动作（list/detail/create/update/remove/export），复用页面自有 api 模块
      const method = operationId.split('.').pop() ?? ''
      if (method === 'list') return await t11Verify20261118Api.list(params as never, context.signal)
      if (method === 'export') return await t11Verify20261118Api.export(params as never, context.signal)
      if (method === 'create') return await t11Verify20261118Api.create(params as never, context.signal)
      if (method === 'update') return await t11Verify20261118Api.update(params.id as never, params as never, context.signal)
      if (method === 'detail') return await t11Verify20261118Api.detail(params.id as never, context.signal)
      if (method === 'remove') return await t11Verify20261118Api.remove(params.id as never, context.signal)
      throw new Error('operation「' + operationId + '」不在本页 api 模块的 CRUD 动作内，请在 overrides 中注入')
    },
  },
})
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
onMounted(() => { void pageBehavior.pageLoad() })
onBeforeUnmount(() => { disposeBehaviorTargets(); pageBehavior.dispose() })

const columns: Column[] = [
  { key: 'title', title: '标题', minWidth: 160 },
  { key: 'createTime', title: '创建时间', width: 160 },
  { key: 'actions', title: '操作', width: 112, className: 'col-actions' },
]

/* ── 新增 / 编辑 ── */
type T11Verify20261118Form = {
  title: string | null
}

/** 正在编辑哪一行的主键；空着就是新增。不借表单里某个字段的值来猜：主键可能由人填（业务编号），也可能根本不进表单（自增） */
const editing = ref<number | null>(null)
const dialog = ref(false)
const saving = ref(false)
const form = reactive<T11Verify20261118Form>({
  title: '',
})
const errors = ref<Record<string, string>>({})

function emptyForm(): T11Verify20261118Form {
  return {
    title: '',
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, emptyForm())
  errors.value = {}
  dialog.value = true
}

/**
 * 编辑前先取整行：列表行只带勾选进列表的字段，直接拿 row 填表单，
 * 保存时会把没显示出来的字段当空值覆盖掉。
 */
async function openEdit(row: T11Verify20261118Row) {
  try {
    const detail = await t11Verify20261118Api.detail(row.id)
    editing.value = row.id
    Object.assign(form, emptyForm(), detail)
    errors.value = {}
    dialog.value = true
  } catch (error) {
    toastError(error, 'T11验收模型二次修改加载失败')
  }
}

async function submit() {
  errors.value = validate(form, {
  })
  if (Object.keys(errors.value).length) return
  const payload: T11Verify20261118WriteRequest = {
    title: form.title?.trim() ?? null,
  }
  saving.value = true
  try {
    if (editing.value !== null) await t11Verify20261118Api.update(editing.value, payload)
    else await t11Verify20261118Api.create(payload)
    pushToast(editing.value !== null ? 'T11验收模型二次修改已更新' : 'T11验收模型二次修改已创建')
    dialog.value = false
    await list.load()
  } catch (error) {
    toastError(error, 'T11验收模型二次修改保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: T11Verify20261118Row) {
  const ok = await confirm({
    title: '删除T11验收模型二次修改',
    message: '删除后平台不会自动回滚已发布到仓库的代码。确认删除这条T11验收模型二次修改吗？',
    confirmText: '删除',
    danger: true,
  })
  if (!ok) return
  try {
    await t11Verify20261118Api.remove(row.id)
    pushToast('T11验收模型二次修改已删除')
    await list.load()
  } catch (error) {
    toastError(error, 'T11验收模型二次修改删除失败')
  }
}
</script>

<template>
  <ListPage
    title="T11验收页面二次修改"
    desc="本页由低代码设计器按模型定义生成；要改字段回到设计器改模型再发布。"
    table-title="T11验收模型二次修改列表"
    :list="list"
    :columns="columns"
  >
    <template #actions>
      <PermissionButton perm="t11_verify_20261118:t11_verify20261118:add" variant="primary" icon="plus" @click="openCreate">新增</PermissionButton>
    </template>
    <template #cell-createTime="{ row }"><span class="tnum soft">{{ row.createTime ?? '—' }}</span></template>

    <template #cell-actions="{ row }">
      <div class="row-actions">
        <PermissionButton perm="t11_verify_20261118:t11_verify20261118:edit" icon-only icon="edit" label="编辑" :ref="behaviorTargetRef('516b3277-a7b7-4436-b5aa-ea89e38a81bd', row.id)" @click="openEdit(row)" />
        <PermissionButton perm="t11_verify_20261118:t11_verify20261118:remove" icon-only icon="trash" label="删除" danger :ref="behaviorTargetRef('38d4e50d-85e3-471d-93de-d4e71b5b25c8', row.id)" @click="remove(row)" />
      </div>
    </template>
  </ListPage>

  <FormDialog :open="dialog" :title="editing !== null ? '编辑T11验收模型二次修改' : '新增T11验收模型二次修改'" :saving="saving" @close="dialog = false" @submit="submit">
    <Field label="名称" wide data-field-node-id="e9bb666b-5a45-4cb7-85c9-daaa3e1c35da" :error="errors.title">
      <TextInput v-model="form.title" />
    </Field>
  </FormDialog>
</template>
