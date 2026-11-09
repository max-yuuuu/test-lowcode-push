import { test } from 'node:test'
import assert from 'node:assert/strict'
import { APP, setBrand } from '../index.ts'

test('APP.multiTenant 默认 false：底座与交付项目都是单租户，登录页不展示租户字段', () => {
  assert.equal(APP.multiTenant, false)
})

test('setBrand 能注入 multiTenant：平台薄宿主在挂载前翻成 true', () => {
  const before = APP.multiTenant
  try {
    setBrand({ multiTenant: true })
    assert.equal(APP.multiTenant, true)
  } finally {
    setBrand({ multiTenant: before })
  }
})