import { test } from 'node:test'
import assert from 'node:assert/strict'
import { request, setToken, clearToken } from '../src/api.js'
test('protected requests carry the token and serialize their body', async () => {
  setToken('example-token')
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/posts'); assert.equal(options.method, 'POST')
    assert.equal(options.headers.Authorization, 'Bearer example-token')
    assert.deepEqual(JSON.parse(options.body), { title: 'Study' })
    return new Response(JSON.stringify({ id: 't1' }), { status: 201 })
  }
  assert.deepEqual(await request('/posts', { method: 'POST', body: { title: 'Study' }, protectedCall: true }), { id: 't1' })
  clearToken()
})
test('public requests omit authentication even when a token exists', async () => {
  setToken('example-token')
  globalThis.fetch = async (url, options) => { assert.equal(options.headers.Authorization, undefined); return new Response('[]') }
  await request('/posts'); clearToken()
})
test('204 produces null without attempting JSON parsing', async () => {
  globalThis.fetch = async () => new Response(null, { status: 204 })
  assert.equal(await request('/posts/t1', { method: 'DELETE' }), null)
})
test('401 clears the in-memory token and announces expiration', async () => {
  let expired = false
  globalThis.window = { dispatchEvent: event => { expired = event.type === 'session-expired' } }
  setToken('expired')
  globalThis.fetch = async () => new Response('{"message":"Sign in again"}', { status: 401 })
  await assert.rejects(request('/me', { protectedCall: true }), { message: 'Sign in again', status: 401 })
  assert.equal(expired, true)
  globalThis.fetch = async (url, options) => { assert.equal(options.headers.Authorization, undefined); return new Response('{}') }
  await request('/me', { protectedCall: true })
})
