<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Field, TextInput, type Column } from '@yuanda-lowcode-ai/scaffold-ui/components/ui'
import { FormDialog, ListPage, PermissionButton } from '@yuanda-lowcode-ai/scaffold-ui/components/business'
import { useListPage } from '@yuanda-lowcode-ai/scaffold-ui/composables/list'
import { confirm } from '@yuanda-lowcode-ai/scaffold-ui/composables/confirm'
import { pushToast, toastError } from '@yuanda-lowcode-ai/scaffold-ui/composables/toast'
import { required, validate } from '@yuanda-lowcode-ai/scaffold-ui/utils/validate'
import { t11DemoApi, type T11DemoRow, type T11DemoWriteRequest } from '@/api/t11_demo/t11_demo'

/* ============================================================
   T11 演示。由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖。
   版式固定走 ListPage + useListPage（查询 / 重置 / 分页 / 导出 / 行操作都是公共能力），
   页面只声明字段、列与动作。要加区块写到 src/views/t11_demo/overrides/ 里；
   要长期人工接管这一页，就把它改名并让发布不再产出它 —— 现在它每次发布都会被整文件覆盖。
   ============================================================ */

type T11DemoQuery = {
  name: string
}

const list = useListPage<T11DemoRow, T11DemoQuery>({
  query: {
    name: '',
  },
  fetch: params => t11DemoApi.list(params),
})


const columns: Column[] = [
  { key: 'name', title: '名称', minWidth: 160 },
  { key: 'createTime', title: '创建时间', width: 160 },
]

/* ── 新增 / 编辑 ── */
type T11DemoForm = {
  name: string | null
}

/** 正在编辑哪一行的主键；空着就是新增。不借表单里某个字段的值来猜：主键可能由人填（业务编号），也可能根本不进表单（自增） */
const editing = ref<number | null>(null)
const dialog = ref(false)
const saving = ref(false)
const form = reactive<T11DemoForm>({
  name: '',
})
const errors = ref<Record<string, string>>({})

function emptyForm(): T11DemoForm {
  return {
    name: '',
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
async function openEdit(row: T11DemoRow) {
  try {
    const detail = await t11DemoApi.detail(row.id)
    editing.value = row.id
    Object.assign(form, emptyForm(), detail)
    errors.value = {}
    dialog.value = true
  } catch (error) {
    toastError(error, 'T11 演示加载失败')
  }
}

async function submit() {
  errors.value = validate(form, {
  })
  if (Object.keys(errors.value).length) return
  const payload: T11DemoWriteRequest = {
    name: form.name?.trim() ?? null,
  }
  saving.value = true
  try {
    if (editing.value !== null) await t11DemoApi.update(editing.value, payload)
    else await t11DemoApi.create(payload)
    pushToast(editing.value !== null ? 'T11 演示已更新' : 'T11 演示已创建')
    dialog.value = false
    await list.load()
  } catch (error) {
    toastError(error, 'T11 演示保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: T11DemoRow) {
  const ok = await confirm({
    title: '删除T11 演示',
    message: '删除后平台不会自动回滚已发布到仓库的代码。确认删除这条T11 演示吗？',
    confirmText: '删除',
    danger: true,
  })
  if (!ok) return
  try {
    await t11DemoApi.remove(row.id)
    pushToast('T11 演示已删除')
    await list.load()
  } catch (error) {
    toastError(error, 'T11 演示删除失败')
  }
}
</script>

<template>
  <ListPage
    title="T11 演示"
    desc="本页由低代码设计器按模型定义生成；要改字段回到设计器改模型再发布。"
    table-title="T11 演示列表"
    :list="list"
    :columns="columns"
  >
    <template #actions>
      <PermissionButton perm="t11_demo:t11_demo:add" icon="plus" @click="openCreate">新增</PermissionButton>
    </template>

    <template #search>
      <Field label="名称">
        <TextInput v-model="list.query.name" clearable @enter="list.search()" />
      </Field>
    </template>
    <template #cell-createTime="{ row }"><span class="tnum soft">{{ row.createTime ?? '—' }}</span></template>

  </ListPage>

  <FormDialog :open="dialog" :title="editing !== null ? '编辑T11 演示' : '新增T11 演示'" :saving="saving" @close="dialog = false" @submit="submit">
    <Field label="名称" wide :error="errors.name">
      <TextInput v-model="form.name" />
    </Field>
  </FormDialog>
</template>
