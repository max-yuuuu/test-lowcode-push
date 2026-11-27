<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { Field, TextInput } from '@yuanda-lowcode-ai/scaffold-ui/components/ui'
import { FormDialog } from '@yuanda-lowcode-ai/scaffold-ui/components/business'
import { pushToast, toastError } from '@yuanda-lowcode-ai/scaffold-ui/composables/toast'
import { confirm } from '@yuanda-lowcode-ai/scaffold-ui/composables/confirm'
import { required, validate } from '@yuanda-lowcode-ai/scaffold-ui/utils/validate'
import { demoApi, type DemoWriteRequest } from '@/api/demo/demo'
import { createGeneratedPageRuntime, resolveGeneratedTarget } from '@yuanda-lowcode-ai/scaffold-ui/composables/generated-behavior'
import type { PageDesignV2 } from '@yuanda-lowcode-ai/scaffold-ui/capabilities'

const emit = defineEmits<{ saved: [] }>()
const saving = ref(false)
const loading = ref(false)
const editing = ref<number | null>(null)
const errors = ref<Record<string, string>>({})
type DemoForm = {
  name: string | null
}
const form = reactive<DemoForm>({
  name: '',
})
function emptyForm(): DemoForm {
  return {
    name: '',
  }
}
const behaviorDocument = {"flows":[],"variables":[],"propBindings":[],"eventBindings":[]} as unknown as PageDesignV2['behavior']
const behaviorTargets: Record<string, unknown> = {}
const pageBehavior = createGeneratedPageRuntime({
  document: { version: '2.0', pageType: 'dialog', modelRef: 'generated', layout: { id: 'generated', type: 'GeneratedPage' }, behavior: behaviorDocument } as unknown as PageDesignV2,
  host: {
    resolveTarget: (nodeId, scope) => resolveGeneratedTarget(pageBehavior.targets, nodeId, scope, behaviorTargets),
    runPageAction: async (action: string, args: Record<string, unknown>) => {
      if (action === 'state.set') {
        throw new Error('弹窗页未绑定 open 页面状态，不能执行 state.set')
        return undefined
      }
      if (action === 'toast') { pushToast(String(args.message ?? ''), args.tone === 'danger' ? 'danger' : args.tone === 'ok' ? 'ok' : 'info'); return undefined }
      if (action === 'confirm.ask') return await confirm({ title: String(args.title ?? '确认'), message: String(args.message ?? ''), danger: Boolean(args.danger) })
      throw new Error('弹窗页动作「' + action + '」尚未适配')
    },
    invokeOperation: async (operationId: string, params: Record<string, unknown>, context: { nodeId: string; signal: AbortSignal }) => {
      context.signal.throwIfAborted()
      const method = operationId.split('.').pop() ?? ''
      if (!method) throw new Error('operation「' + operationId + '」缺少接口编码')
      const requireOperationId = (params: Record<string, unknown>) => {
        const id = params.id
        if (id === undefined || id === null || id === '') throw new Error('operation「' + operationId + '」缺少 params.id')
        return id as never
      }
      if (method === 'list') return await demoApi.list(params as never, context.signal)
      if (method === 'export') return await demoApi.export(params as never, context.signal)
      if (method === 'create') return await demoApi.create(params as never, context.signal)
      if (method === 'update') return await demoApi.update(requireOperationId(params), params as never, context.signal)
      if (method === 'detail') return await demoApi.detail(requireOperationId(params), context.signal)
      if (method === 'remove') return await demoApi.remove(requireOperationId(params), context.signal)
      return await demoApi.invoke(method, params, context.signal)
    },
  },
})
const dialog = ref(false)
const behaviorTargetDisposers = new Map<string, () => void>()
const behaviorTargetRef = (nodeId: string) => (instance: unknown | null) => {
  behaviorTargetDisposers.get(nodeId)?.()
  behaviorTargetDisposers.delete(nodeId)
  if (instance != null) behaviorTargetDisposers.set(nodeId, pageBehavior.targets.register(nodeId, instance))
}
function disposeBehaviorTargets() {
  for (const dispose of behaviorTargetDisposers.values()) dispose()
  behaviorTargetDisposers.clear()
}
async function dispatchFormEvent(event: string) {
  try {
    const results = await pageBehavior.runtime.dispatch({ kind: 'component', nodeId: '37b81199-98a5-40cc-8925-1626e366b36d' }, event)
    if (results.some(result => result.status === 'failed' && !result.handled)) throw new Error('弹窗事件执行失败')
  } finally {
  }
}
async function submit() {
  if (saving.value || loading.value) return false
  errors.value = validate(form, {
  })
  if (Object.keys(errors.value).length) return false
  if (false) {
    saving.value = true
    try {
      await dispatchFormEvent('submit')
      return true
    } catch (error) {
      toastError(error, '测试模型保存失败')
      return false
    } finally {
      saving.value = false
    }
  }
  const payload: DemoWriteRequest = {
    name: '',
  }
  Object.assign(payload, loadedDetail)
  payload.name = form.name?.trim() ?? null
  saving.value = true
  try {
    if (editing.value !== null) await demoApi.update(editing.value, payload)
    else await demoApi.create(payload)
  } catch (error) {
    toastError(error, '测试模型保存失败')
    return false
  } finally {
    saving.value = false
  }
  pushToast(editing.value !== null ? '测试模型已更新' : '测试模型已创建')
  dialog.value = false
  emit('saved')
  return true
}
async function close() {
  openRequest += 1
  loading.value = false
  if (!false) dialog.value = false
}
let openRequest = 0
let loadedDetail: Partial<DemoWriteRequest> = {}
function openCreate() {
  if (saving.value) return
  openRequest += 1
  loading.value = false
  editing.value = null
  loadedDetail = {}
  Object.assign(form, emptyForm())
  errors.value = {}
  dialog.value = true
}
async function openEdit(id: number) {
  if (saving.value || loading.value) return false
  const request = ++openRequest
  loading.value = true
  try {
    const detail = await demoApi.detail(id)
    if (request !== openRequest) return false
    editing.value = id
    loadedDetail = detail
    Object.assign(form, emptyForm(), detail)
    errors.value = {}
    dialog.value = true
    return true
  } catch (error) {
    if (request !== openRequest) return false
    toastError(error, '测试模型加载失败')
    return false
  } finally {
    if (request === openRequest) loading.value = false
  }
}
onMounted(() => {
  void pageBehavior.pageLoad().then(results => {
    if (!pageBehavior.runtime.disposed && results.some(result => result.status === 'failed' && !result.handled)) {
      pushToast('页面初始化失败', 'danger')
    }
  })
})
onBeforeUnmount(() => { disposeBehaviorTargets(); pageBehavior.dispose() })
defineExpose({ openCreate, openEdit, close })
</script>

<template>
  <FormDialog :open="dialog" :title="editing !== null ? '编辑测试模型' : '新增测试模型'" :saving="saving || loading" :ref="behaviorTargetRef('37b81199-98a5-40cc-8925-1626e366b36d')" @close="close" @submit="submit">
    <Field label="名称" wide :error="errors.name">
      <TextInput v-model="form.name" :ref="behaviorTargetRef('084f8ba2-ad33-49ac-a9a6-8c9cca1b0c85')" />
    </Field>
  </FormDialog>
</template>
