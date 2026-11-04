<script setup lang="ts">
import { reactive, ref } from 'vue'
import { DateInput, Field, NumberInput, TextInput, Textarea, type Column } from '@yuanda-lowcode-ai/scaffold-ui/components/ui'
import { DictSelect, DictTag, FormDialog, ListPage, PermissionButton } from '@yuanda-lowcode-ai/scaffold-ui/components/business'
import { useListPage } from '@yuanda-lowcode-ai/scaffold-ui/composables/list'
import { confirm } from '@yuanda-lowcode-ai/scaffold-ui/composables/confirm'
import { pushToast, toastError } from '@yuanda-lowcode-ai/scaffold-ui/composables/toast'
import { required, validate } from '@yuanda-lowcode-ai/scaffold-ui/utils/validate'
import { salesOrderApi, type SalesOrderRow, type SalesOrderWriteRequest } from '@/api/sales_order/sales_order'

/* ============================================================
   销售订单主表管理。由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖。
   版式固定走 ListPage + useListPage（查询 / 重置 / 分页 / 导出 / 行操作都是公共能力），
   页面只声明字段、列与动作。要加区块写到 src/views/sales_order/overrides/ 里；
   要长期人工接管这一页，就把它改名并让发布不再产出它 —— 现在它每次发布都会被整文件覆盖。
   ============================================================ */

type SalesOrderQuery = {
  orderNo: string
  amountStart: number | null
  amountEnd: number | null
  status: number | null
}

const list = useListPage<SalesOrderRow, SalesOrderQuery>({
  query: {
    orderNo: '',
    amountStart: null,
    amountEnd: null,
    status: null,
  },
  fetch: params => salesOrderApi.list(params),
  exporter: params => salesOrderApi.export(params),
  exportName: '销售订单主表.csv',
})

const columns: Column[] = [
  { key: 'id', title: '主键', minWidth: 110, align: 'right' },
  { key: 'orderNo', title: '订单编号', minWidth: 160 },
  { key: 'amount', title: '订单金额', minWidth: 110, align: 'right' },
  { key: 'status', title: '状态', minWidth: 110, align: 'right' },
  { key: 'deliveryDate', title: '交货日期', minWidth: 160 },
  { key: 'remark', title: '备注', minWidth: 160 },
  { key: 'createTime', title: '创建时间', width: 160 },
  { key: 'actions', title: '操作', width: 112, className: 'col-actions' },
]

/* ── 新增 / 编辑 ── */
type SalesOrderForm = {
  orderNo: string
  amount: number | null
  status: number | null
  deliveryDate: string | null
  remark: string | null
}

/** 正在编辑哪一行的主键；空着就是新增。不借表单里某个字段的值来猜：主键可能由人填（业务编号），也可能根本不进表单（自增） */
const editing = ref<number | null>(null)
const dialog = ref(false)
const saving = ref(false)
const form = reactive<SalesOrderForm>({
  orderNo: '',
  amount: null,
  status: null,
  deliveryDate: '',
  remark: '',
})
const errors = ref<Record<string, string>>({})

function emptyForm(): SalesOrderForm {
  return {
    orderNo: '',
    amount: null,
    status: null,
    deliveryDate: '',
    remark: '',
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
async function openEdit(row: SalesOrderRow) {
  try {
    const detail = await salesOrderApi.detail(row.id)
    editing.value = row.id
    Object.assign(form, emptyForm(), detail)
    errors.value = {}
    dialog.value = true
  } catch (error) {
    toastError(error, '销售订单加载失败')
  }
}

async function submit() {
  errors.value = validate(form, {
    orderNo: [required('请输入订单编号')],
    status: [required('请选择状态')],
  })
  if (Object.keys(errors.value).length) return
  const payload: SalesOrderWriteRequest = {
    orderNo: form.orderNo.trim(),
    amount: form.amount,
    status: form.status,
    deliveryDate: form.deliveryDate?.trim() ?? null,
    remark: form.remark?.trim() ?? null,
  }
  saving.value = true
  try {
    if (editing.value !== null) await salesOrderApi.update(editing.value, payload)
    else await salesOrderApi.create(payload)
    pushToast(editing.value !== null ? '销售订单已更新' : '销售订单已创建')
    dialog.value = false
    await list.load()
  } catch (error) {
    toastError(error, '销售订单保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: SalesOrderRow) {
  const ok = await confirm({
    title: '删除销售订单',
    message: '删除后平台不会自动回滚已发布到仓库的代码。确认删除这条销售订单吗？',
    confirmText: '删除',
    danger: true,
  })
  if (!ok) return
  try {
    await salesOrderApi.remove(row.id)
    pushToast('销售订单已删除')
    await list.load()
  } catch (error) {
    toastError(error, '销售订单删除失败')
  }
}
</script>

<template>
  <ListPage
    title="销售订单主表管理"
    desc="本页由低代码设计器按模型定义生成；要改字段回到设计器改模型再发布。"
    table-title="销售订单列表"
    :list="list"
    :columns="columns"
  >
    <template #actions>
      <PermissionButton perm="sales_order:sales_order:add" variant="primary" icon="plus" @click="openCreate">新增销售订单</PermissionButton>
    </template>

    <template #search>
      <Field label="订单编号">
        <TextInput v-model="list.query.orderNo" clearable @enter="list.search()" />
      </Field>
      <Field label="订单金额（起）">
        <NumberInput v-model="list.query.amountStart" :precision="2" />
      </Field>
      <Field label="订单金额（止）">
        <NumberInput v-model="list.query.amountEnd" :precision="2" />
      </Field>
      <Field label="状态">
        <DictSelect v-model="list.query.status" type="sys_normal_disable" all-label="全部" />
      </Field>
    </template>
    <template #cell-id="{ row }"><span class="tnum">{{ row.id ?? '—' }}</span></template>
    <template #cell-amount="{ row }"><span class="tnum">{{ row.amount ?? '—' }}</span></template>
    <template #cell-status="{ row }"><DictTag type="sys_normal_disable" :value="row.status" /></template>
    <template #cell-deliveryDate="{ row }"><span class="tnum soft">{{ row.deliveryDate ?? '—' }}</span></template>
    <template #cell-createTime="{ row }"><span class="tnum soft">{{ row.createTime ?? '—' }}</span></template>

    <template #cell-actions="{ row }">
      <div class="row-actions">
        <PermissionButton perm="sales_order:sales_order:edit" icon-only icon="edit" label="编辑" @click="openEdit(row)" />
        <PermissionButton perm="sales_order:sales_order:remove" icon-only icon="trash" label="删除" danger @click="remove(row)" />
      </div>
    </template>
  </ListPage>

  <FormDialog :open="dialog" :title="editing !== null ? '编辑销售订单' : '新增销售订单'" :saving="saving" @close="dialog = false" @submit="submit">
    <Field label="订单编号" required wide :error="errors.orderNo">
      <TextInput v-model="form.orderNo" />
    </Field>
    <Field label="订单金额" :error="errors.amount">
      <NumberInput v-model="form.amount" :precision="2" />
    </Field>
    <Field label="状态" required :error="errors.status">
      <DictSelect v-model="form.status" type="sys_normal_disable" />
    </Field>
    <Field label="交货日期" :error="errors.deliveryDate">
      <DateInput v-model="form.deliveryDate" type="date" />
    </Field>
    <Field label="备注" wide :error="errors.remark">
      <Textarea v-model="form.remark" :rows="3" />
    </Field>
  </FormDialog>
</template>
