import test from 'node:test'
import assert from 'node:assert/strict'
import { createHttpClient } from '../http.ts'
import { createPortalApi } from '../portal.ts'

test('portal api maps health and dashboard endpoints', async () => {
  const calls: string[] = []
  const fetcher = async (url: string | URL | Request, init?: RequestInit) => {
    calls.push(`${init?.method ?? 'GET'} ${String(url)}`)
    return new Response(JSON.stringify({ code: 200, message: 'success', data: { rows: [], total: 0 }, traceId: 't' }), {
      status: 200,
      headers: { 'content-type': 'application/json' },
    })
  }
  const api = createPortalApi(createHttpClient({ baseUrl: '/api', tokenProvider: () => 'token', fetcher }))

  await api.health()
  await api.dashboard.summary()

  assert.deepEqual(calls, [
    'GET /api/health',
    'GET /api/dashboard/summary',
  ])
})
