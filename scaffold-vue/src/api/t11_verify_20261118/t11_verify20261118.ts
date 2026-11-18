/* ============================================================
   T11增删改发布验证-v2接口封装。由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖。
   要改取数逻辑请回到设计器改模型，或在 src/views/t11_verify_20261118/overrides/ 里覆写区块；
   在这个文件里手改的内容下次发布会被整文件替换掉。

   路径与权限点由模型编码派生，与后端 T11Verify20261118Controller 上的 @RequestMapping 一一对应。
   ============================================================ */
import { httpClient } from '@/api/http'
import type { PageResult, QueryParams } from '@yuanda-lowcode-ai/scaffold-ui/types'

/** 列表行：只带勾选进列表的字段。日期是后端格式化好的规范串（yyyy-MM-dd / yyyy-MM-dd HH:mm:ss），前端不再二次转换 */
export interface T11Verify20261118Row {
  id: number
  title: string | null
  createBy?: string | null
  updateBy?: string | null
  createTime?: string | null
  updateTime?: string | null
}

/**
 * 新增与修改的入参：只带勾选了「可录入 / 可编辑」的字段。主键走路径参数，不在这里。
 *
 * <p>数字类字段一律 \`| null\`：Java 侧是 Integer / Long / BigDecimal，本来就能收 null，
 * 必填由后端 Service 校验并回「XX不能为空」—— 前端表单初值是空，类型上先允许 null 才不用骗编译器。
 */
export interface T11Verify20261118WriteRequest {
  /** 可空 */
  title: string | null
}

export const t11Verify20261118Api = {
  list: (query?: QueryParams, signal?: AbortSignal) => httpClient.get<PageResult<T11Verify20261118Row>>('/t11_verify_20261118/t11_verify20261118', query, signal),
  /** 返回新建行的主键 */
  create: (body: T11Verify20261118WriteRequest, signal?: AbortSignal) => httpClient.post<number>('/t11_verify_20261118/t11_verify20261118', body, signal),
  /** 单行详情：编辑前先取整行，列表行只有勾选进列表的字段 */
  detail: (id: number, signal?: AbortSignal) => httpClient.get<T11Verify20261118WriteRequest & { id: number }>(`/t11_verify_20261118/t11_verify20261118/${id}`, undefined, signal),
  /** 整行覆盖可编辑字段：表单提交的是整个表单，后端没勾选的字段一个都不碰 */
  update: (id: number, body: T11Verify20261118WriteRequest, signal?: AbortSignal) => httpClient.put<void>(`/t11_verify_20261118/t11_verify20261118/${id}`, body, signal),
  remove: (id: number, signal?: AbortSignal) => httpClient.delete<void>(`/t11_verify_20261118/t11_verify20261118/${id}`, signal),
  /** 后端生成 CSV，条件与列表同一套 */
  export: (query?: QueryParams, signal?: AbortSignal) => httpClient.download('/t11_verify_20261118/t11_verify20261118/export', query, signal),
}
