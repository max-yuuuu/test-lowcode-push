import test from 'node:test'
import assert from 'node:assert/strict'
import { resolveMenuIcon } from '../menu-icon.ts'

test('maps the legacy AI services icon to the registered workflow icon', () => {
  assert.equal(resolveMenuIcon('lowcode/ai-services', 'sparkles'), 'workflow')
})

test('leaves other menu icons and missing-icon fallback unchanged', () => {
  assert.equal(resolveMenuIcon('/system/users', 'sparkles'), 'sparkles')
  assert.equal(resolveMenuIcon('/lowcode/ai-services', undefined), 'file')
})
