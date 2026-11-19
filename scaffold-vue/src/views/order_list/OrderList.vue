<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Field, TextInput, type Column } from '@yuanda-lowcode-ai/scaffold-ui/components/ui'
import { FormDialog, ListPage, PermissionButton } from '@yuanda-lowcode-ai/scaffold-ui/components/business'
import { useListPage } from '@yuanda-lowcode-ai/scaffold-ui/composables/list'
import { confirm } from '@yuanda-lowcode-ai/scaffold-ui/composables/confirm'
import { pushToast, toastError } from '@yuanda-lowcode-ai/scaffold-ui/composables/toast'
import { required, validate } from '@yuanda-lowcode-ai/scaffold-ui/utils/validate'
import { orderListApi, type OrderListRow, type OrderListWriteRequest } from '@/api/order_list/order_list'
import { onBeforeUnmount, onMounted } from 'vue'
import { createGeneratedPageRuntime, resolveGeneratedTarget } from '@yuanda-lowcode-ai/scaffold-ui/composables/generated-behavior'
import type { PageDesignV2 } from '@yuanda-lowcode-ai/scaffold-ui/capabilities'

/* ============================================================
   订单列表。由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖。
   版式固定走 ListPage + useListPage（查询 / 重置 / 分页 / 导出 / 行操作都是公共能力），
   页面只声明字段、列与动作。要加区块写到 src/views/order_list/overrides/ 里；
   要长期人工接管这一页，就把它改名并让发布不再产出它 —— 现在它每次发布都会被整文件覆盖。
   ============================================================ */

type OrderListQuery = {
}

const list = useListPage<OrderListRow, OrderListQuery>({
  query: {
  },
  fetch: params => orderListApi.list(params),
  exporter: params => orderListApi.export(params),
  exportName: '订单明细.csv',
})

// 页面行为（2.0）：装配共享执行内核；页面动作接列表/提示/确认，方法实例与 operation 由 overrides 注入。
const behaviorDocument = {"flows":[],"variables":[],"propBindings":[],"eventBindings":[]} as unknown as PageDesignV2['behavior']
// 页面级能力与组件实例分开：表格刷新/查询是列表代理，不伪装成物料实例。
const behaviorTargets: Record<string, unknown> = {
  'd400b274-f98c-4c9f-ba78-0ed5f33912ed': { refresh: () => list.load(), search: () => list.search() },
}
const behaviorComponentTypes: Record<string, string> = {
  '374cb7b3-a7cc-48e0-a345-2e1256ab192e': 'SearchForm',
  'd98cd3b1-8fca-42c4-8be7-368fc7743962': 'Toolbar',
  '1fe36b02-04ae-448b-aa39-8b895b296ea3': 'Button',
  'e04bfb65-c2a1-4081-ba5c-0966bcec90fa': 'Button',
  'd400b274-f98c-4c9f-ba78-0ed5f33912ed': 'DataTable',
  '66a2efa0-ae35-4328-8652-e4a6188716ec': 'RowActions',
  '9ad2a296-5606-4454-a6a0-0d22ddf85151': 'Button',
  '101d4c23-228a-4f52-a799-6259a3a16463': 'Button',
  'b379c8b0-19f6-43f1-8946-4ccc8e5b5e6d': 'FormContainer',
  '16edbde0-6b66-41f4-bca7-c84bef962903': 'TextInput',
  'ffb4574e-0cca-4f53-b677-9700df7ed055': 'TextInput',
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
      const requireOperationId = (operationId: string, params: Record<string, unknown>) => {
        const id = params.id
        if (id === undefined || id === null || id === '') throw new Error('operation「' + operationId + '」缺少 params.id')
        return id as never
      }
      if (method === 'list') return await orderListApi.list(params as never, context.signal)
      if (method === 'export') return await orderListApi.export(params as never, context.signal)
      if (method === 'create') return await orderListApi.create(params as never, context.signal)
      if (method === 'update') return await orderListApi.update(requireOperationId(operationId, params), params as never, context.signal)
      if (method === 'detail') return await orderListApi.detail(requireOperationId(operationId, params), context.signal)
      if (method === 'remove') return await orderListApi.remove(requireOperationId(operationId, params), context.signal)
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
onMounted(() => {
  void pageBehavior.pageLoad().then(results => {
    if (!pageBehavior.runtime.disposed && results.some(result => result.status === 'failed' && !result.handled)) {
      pushToast('页面初始化失败', 'danger')
    }
  })
})
onBeforeUnmount(() => { disposeBehaviorTargets(); pageBehavior.dispose() })

const columns: Column[] = [
  { key: 'orderNo', title: '订单号', minWidth: 160 },
  { key: 'orderName', title: '订单名', minWidth: 160 },
  { key: 'createTime', title: '创建时间', width: 160 },
  { key: 'actions', title: '操作', width: 112, className: 'col-actions' },
]

/* ── 新增 / 编辑 ── */
type OrderListForm = {
  orderNo: string | null
  orderName: string | null
}

/** 正在编辑哪一行的主键；空着就是新增。不借表单里某个字段的值来猜：主键可能由人填（业务编号），也可能根本不进表单（自增） */
const editing = ref<number | null>(null)
const dialog = ref(false)
const saving = ref(false)
const form = reactive<OrderListForm>({
  orderNo: '',
  orderName: '',
})
const errors = ref<Record<string, string>>({})

function emptyForm(): OrderListForm {
  return {
    orderNo: '',
    orderName: '',
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
async function openEdit(row: OrderListRow) {
  try {
    const detail = await orderListApi.detail(row.id)
    editing.value = row.id
    Object.assign(form, emptyForm(), detail)
    errors.value = {}
    dialog.value = true
  } catch (error) {
    toastError(error, '订单明细加载失败')
  }
}

async function submit() {
  errors.value = validate(form, {
  })
  if (Object.keys(errors.value).length) return
  const payload: OrderListWriteRequest = {
    orderNo: form.orderNo?.trim() ?? null,
    orderName: form.orderName?.trim() ?? null,
  }
  saving.value = true
  try {
    if (editing.value !== null) await orderListApi.update(editing.value, payload)
    else await orderListApi.create(payload)
    pushToast(editing.value !== null ? '订单明细已更新' : '订单明细已创建')
    dialog.value = false
    await list.load()
  } catch (error) {
    toastError(error, '订单明细保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: OrderListRow) {
  const ok = await confirm({
    title: '删除订单明细',
    message: '删除后平台不会自动回滚已发布到仓库的代码。确认删除这条订单明细吗？',
    confirmText: '删除',
    danger: true,
  })
  if (!ok) return
  try {
    await orderListApi.remove(row.id)
    pushToast('订单明细已删除')
    await list.load()
  } catch (error) {
    toastError(error, '订单明细删除失败')
  }
}
</script>

<template>
  <ListPage
    title="订单列表"
    desc="本页由低代码设计器按模型定义生成；要改字段回到设计器改模型再发布。"
    table-title="订单明细列表"
    :list="list"
    :columns="columns"
  >
    <template #actions>
      <PermissionButton perm="order_list:order_list:add" variant="primary" icon="plus" @click="openCreate">新增</PermissionButton>
    </template>
    <template #cell-createTime="{ row }"><span class="tnum soft">{{ row.createTime ?? '—' }}</span></template>

    <template #cell-actions="{ row }">
      <div class="row-actions">
        <PermissionButton perm="order_list:order_list:edit" icon-only icon="edit" label="编辑" :ref="behaviorTargetRef('9ad2a296-5606-4454-a6a0-0d22ddf85151', row.id)" @click="openEdit(row)" />
        <PermissionButton perm="order_list:order_list:remove" icon-only icon="trash" label="删除" danger :ref="behaviorTargetRef('101d4c23-228a-4f52-a799-6259a3a16463', row.id)" @click="remove(row)" />
      </div>
    </template>
  </ListPage>

  <FormDialog :open="dialog" :title="editing !== null ? '编辑订单明细' : '新增订单明细'" :saving="saving" @close="dialog = false" @submit="submit">
    <Field label="订单号" wide data-field-node-id="16edbde0-6b66-41f4-bca7-c84bef962903" :error="errors.orderNo">
      <TextInput v-model="form.orderNo" />
    </Field>
    <Field label="订单名" wide data-field-node-id="ffb4574e-0cca-4f53-b677-9700df7ed055" :error="errors.orderName">
      <TextInput v-model="form.orderName" />
    </Field>
  </FormDialog>
</template>
