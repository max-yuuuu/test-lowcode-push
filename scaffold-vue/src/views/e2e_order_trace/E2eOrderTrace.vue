<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Field, NumberInput, TextInput, type Column } from '@yuanda-lowcode-ai/scaffold-ui/components/ui'
import { FormDialog, ListPage, PermissionButton } from '@yuanda-lowcode-ai/scaffold-ui/components/business'
import { useListPage } from '@yuanda-lowcode-ai/scaffold-ui/composables/list'
import { confirm } from '@yuanda-lowcode-ai/scaffold-ui/composables/confirm'
import { pushToast, toastError } from '@yuanda-lowcode-ai/scaffold-ui/composables/toast'
import { required, validate } from '@yuanda-lowcode-ai/scaffold-ui/utils/validate'
import { e2eOrderTraceApi, type E2eOrderTraceRow, type E2eOrderTraceWriteRequest } from '@/api/e2e_order_trace/e2e_order_trace'

/* ============================================================
   轨迹。由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖。
   版式固定走 ListPage + useListPage（查询 / 重置 / 分页 / 导出 / 行操作都是公共能力），
   页面只声明字段、列与动作。要加区块写到 src/views/e2e_order_trace/overrides/ 里；
   要长期人工接管这一页，就把它改名并让发布不再产出它 —— 现在它每次发布都会被整文件覆盖。
   ============================================================ */

type E2eOrderTraceQuery = {
}

const list = useListPage<E2eOrderTraceRow, E2eOrderTraceQuery>({
  query: {
  },
  fetch: params => e2eOrderTraceApi.list(params),
  exporter: params => e2eOrderTraceApi.export(params),
  exportName: 'E2E 订单轨迹.csv',
})

const columns: Column[] = [
  { key: 'traceId', title: '轨迹号', minWidth: 110, align: 'right' },
  { key: 'createTime', title: '创建时间', width: 160 },
  { key: 'actions', title: '操作', width: 112, className: 'col-actions' },
]

/* ── 新增 / 编辑 ── */
type E2eOrderTraceForm = {
  traceId: number | null
  traceName: string | null
}

/** 正在编辑哪一行的主键；空着就是新增。不借表单里某个字段的值来猜：主键可能由人填（业务编号），也可能根本不进表单（自增） */
const editing = ref<number | null>(null)
const dialog = ref(false)
const saving = ref(false)
const form = reactive<E2eOrderTraceForm>({
  traceId: null,
  traceName: '',
})
const errors = ref<Record<string, string>>({})

function emptyForm(): E2eOrderTraceForm {
  return {
    traceId: null,
    traceName: '',
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
async function openEdit(row: E2eOrderTraceRow) {
  try {
    const detail = await e2eOrderTraceApi.detail(row.traceId)
    editing.value = row.traceId
    Object.assign(form, emptyForm(), detail)
    errors.value = {}
    dialog.value = true
  } catch (error) {
    toastError(error, 'E2E 订单轨迹加载失败')
  }
}

async function submit() {
  errors.value = validate(form, {
    traceId: [required('请输入轨迹号')],
  })
  if (Object.keys(errors.value).length) return
  const payload: E2eOrderTraceWriteRequest = {
    traceId: form.traceId,
    traceName: form.traceName?.trim() ?? null,
  }
  saving.value = true
  try {
    if (editing.value !== null) await e2eOrderTraceApi.update(editing.value, payload)
    else await e2eOrderTraceApi.create(payload)
    pushToast(editing.value !== null ? 'E2E 订单轨迹已更新' : 'E2E 订单轨迹已创建')
    dialog.value = false
    await list.load()
  } catch (error) {
    toastError(error, 'E2E 订单轨迹保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: E2eOrderTraceRow) {
  const ok = await confirm({
    title: '删除E2E 订单轨迹',
    message: '删除后平台不会自动回滚已发布到仓库的代码。确认删除这条E2E 订单轨迹吗？',
    confirmText: '删除',
    danger: true,
  })
  if (!ok) return
  try {
    await e2eOrderTraceApi.remove(row.traceId)
    pushToast('E2E 订单轨迹已删除')
    await list.load()
  } catch (error) {
    toastError(error, 'E2E 订单轨迹删除失败')
  }
}
</script>

<template>
  <ListPage
    title="轨迹"
    desc="本页由低代码设计器按模型定义生成；要改字段回到设计器改模型再发布。"
    table-title="E2E 订单轨迹列表"
    :list="list"
    :columns="columns"
  >
    <template #actions>
      <PermissionButton perm="e2e_order_trace:e2e_order_trace:add" variant="primary" icon="plus" @click="openCreate">新增</PermissionButton>
    </template>
    <template #cell-traceId="{ row }"><span class="tnum">{{ row.traceId ?? '—' }}</span></template>
    <template #cell-createTime="{ row }"><span class="tnum soft">{{ row.createTime ?? '—' }}</span></template>

    <template #cell-actions="{ row }">
      <div class="row-actions">
        <PermissionButton perm="e2e_order_trace:e2e_order_trace:edit" icon-only icon="edit" label="编辑" @click="openEdit(row)" />
        <PermissionButton perm="e2e_order_trace:e2e_order_trace:remove" icon-only icon="trash" label="删除" danger @click="remove(row)" />
      </div>
    </template>
  </ListPage>

  <FormDialog :open="dialog" :title="editing !== null ? '编辑E2E 订单轨迹' : '新增E2E 订单轨迹'" :saving="saving" @close="dialog = false" @submit="submit">
    <Field label="轨迹号" required :error="errors.traceId">
      <NumberInput v-model="form.traceId" />
    </Field>
    <Field label="文本输入" wide :error="errors.traceName">
      <TextInput v-model="form.traceName" />
    </Field>
  </FormDialog>
</template>
