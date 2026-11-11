<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Field, TextInput, type Column } from '@yuanda-lowcode-ai/scaffold-ui/components/ui'
import { FormDialog, ListPage, PermissionButton } from '@yuanda-lowcode-ai/scaffold-ui/components/business'
import { useListPage } from '@yuanda-lowcode-ai/scaffold-ui/composables/list'
import { confirm } from '@yuanda-lowcode-ai/scaffold-ui/composables/confirm'
import { pushToast, toastError } from '@yuanda-lowcode-ai/scaffold-ui/composables/toast'
import { required, validate } from '@yuanda-lowcode-ai/scaffold-ui/utils/validate'
import { orderDetailApi, type OrderDetailRow, type OrderDetailWriteRequest } from '@/api/order_detail/order_detail'

/* ============================================================
   测试列表页面。由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖。
   版式固定走 ListPage + useListPage（查询 / 重置 / 分页 / 导出 / 行操作都是公共能力），
   页面只声明字段、列与动作。要加区块写到 src/views/order_detail/overrides/ 里；
   要长期人工接管这一页，就把它改名并让发布不再产出它 —— 现在它每次发布都会被整文件覆盖。
   ============================================================ */

type OrderDetailQuery = {
}

const list = useListPage<OrderDetailRow, OrderDetailQuery>({
  query: {
  },
  fetch: params => orderDetailApi.list(params),
  exporter: params => orderDetailApi.export(params),
  exportName: '订单明细.csv',
})

const columns: Column[] = [
  { key: 'orderName', title: '订单号', minWidth: 160 },
  { key: 'createTime', title: '创建时间', width: 160 },
  { key: 'actions', title: '操作', width: 112, className: 'col-actions' },
]

/* ── 新增 / 编辑 ── */
type OrderDetailForm = {
  orderName: string | null
}

/** 正在编辑哪一行的主键；空着就是新增。不借表单里某个字段的值来猜：主键可能由人填（业务编号），也可能根本不进表单（自增） */
const editing = ref<number | null>(null)
const dialog = ref(false)
const saving = ref(false)
const form = reactive<OrderDetailForm>({
  orderName: '',
})
const errors = ref<Record<string, string>>({})

function emptyForm(): OrderDetailForm {
  return {
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
async function openEdit(row: OrderDetailRow) {
  try {
    const detail = await orderDetailApi.detail(row.id)
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
  const payload: OrderDetailWriteRequest = {
    orderName: form.orderName?.trim() ?? null,
  }
  saving.value = true
  try {
    if (editing.value !== null) await orderDetailApi.update(editing.value, payload)
    else await orderDetailApi.create(payload)
    pushToast(editing.value !== null ? '订单明细已更新' : '订单明细已创建')
    dialog.value = false
    await list.load()
  } catch (error) {
    toastError(error, '订单明细保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: OrderDetailRow) {
  const ok = await confirm({
    title: '删除订单明细',
    message: '删除后平台不会自动回滚已发布到仓库的代码。确认删除这条订单明细吗？',
    confirmText: '删除',
    danger: true,
  })
  if (!ok) return
  try {
    await orderDetailApi.remove(row.id)
    pushToast('订单明细已删除')
    await list.load()
  } catch (error) {
    toastError(error, '订单明细删除失败')
  }
}
</script>

<template>
  <ListPage
    title="测试列表页面"
    desc="这是列表测试页面"
    table-title="订单明细列表"
    :list="list"
    :columns="columns"
  >
    <template #actions>
      <PermissionButton perm="order_detail:order_detail:add" variant="primary" icon="plus" @click="openCreate">新增</PermissionButton>
    </template>
    <template #cell-createTime="{ row }"><span class="tnum soft">{{ row.createTime ?? '—' }}</span></template>

    <template #cell-actions="{ row }">
      <div class="row-actions">
        <PermissionButton perm="order_detail:order_detail:edit" icon-only icon="edit" label="编辑" @click="openEdit(row)" />
        <PermissionButton perm="order_detail:order_detail:remove" icon-only icon="trash" label="删除" danger @click="remove(row)" />
      </div>
    </template>
  </ListPage>

  <FormDialog :open="dialog" :title="editing !== null ? '编辑订单明细' : '新增订单明细'" :saving="saving" @close="dialog = false" @submit="submit">
    <Field label="订单号" wide :error="errors.orderName">
      <TextInput v-model="form.orderName" />
    </Field>
  </FormDialog>
</template>
