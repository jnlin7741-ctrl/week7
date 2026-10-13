import assert from 'node:assert/strict'
import { randomUUID } from 'node:crypto'
const base = (process.argv[2] || 'http://localhost:18707').replace(/\/$/, '')
const suffix = randomUUID().slice(0, 8)
const password = 'PracticeOnly123!'
async function call(method, path, body, token, expected) {
  const response = await fetch(base + path, {
    method, headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  })
  const data = response.status === 204 ? null : await response.json()
  assert.equal(response.status, expected, `${method} ${path}: expected ${expected}, received ${response.status}: ${JSON.stringify(data)}`)
  console.log(`PASS ${expected} ${method} ${path}`)
  return data
}
const aEmail = `alex-${suffix}@example.com`, bEmail = `sam-${suffix}@example.com`
await call('GET', '/actuator/health', null, null, 200)
await call('GET', '/api/posts', null, null, 200)
await call('GET', '/api/me', null, null, 401)
for (const email of [aEmail, bEmail]) await call('POST', '/api/auth/signup', { name: email === aEmail ? 'Alex' : 'Sam', email, password }, null, 201)
await call('POST', '/api/auth/signup', { name: 'Alex', email: aEmail, password }, null, 409)
await call('POST', '/api/auth/login', { email: aEmail, password: 'wrong' }, null, 401)
const a = await call('POST', '/api/auth/login', { email: aEmail, password }, null, 200)
const b = await call('POST', '/api/auth/login', { email: bEmail, password }, null, 200)
await call('GET', '/api/me', null, a.accessToken, 200)
const post = await call('POST', '/api/posts', { title: 'Smoke test', content: 'A owns this post.' }, a.accessToken, 201)
assert.equal(post.ownerId, a.user.id)
await call('PUT', `/api/posts/${post.id}`, { title: 'Unauthorized edit', content: 'B tried this.' }, b.accessToken, 403)
await call('DELETE', `/api/posts/${post.id}`, null, b.accessToken, 403)
await call('POST', '/api/posts', { title: 'Injected owner', content: 'Must fail', ownerId: b.user.id }, a.accessToken, 400)
await call('PUT', `/api/posts/${post.id}`, { title: 'Updated', content: 'A changed the post.' }, a.accessToken, 200)
await call('DELETE', `/api/posts/${post.id}`, null, a.accessToken, 204)
await call('GET', `/api/posts/${post.id}`, null, null, 404)
console.log('All checks passed. The test post was deleted. Two practice accounts remain in your database.')
