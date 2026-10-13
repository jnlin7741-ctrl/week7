// Tokens stay in memory. Reloading the page requires another sign-in.
let accessToken = ''
export function setToken(value) { accessToken = value }
export function clearToken() { accessToken = '' }
export async function request(path, { method = 'GET', body, protectedCall = false } = {}) {
  const headers = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  // Attach the token only to a protected request.
  if (protectedCall && accessToken) headers.Authorization = `Bearer ${accessToken}`
  const response = await fetch(`/api${path}`, {
    method, headers, body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (response.status === 204) return null
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    if (response.status === 401 && protectedCall) {
      clearToken()
      window.dispatchEvent(new Event('session-expired'))
    }
    const error = new Error(data.message || `Request failed (${response.status}).`)
    error.status = response.status
    throw error
  }
  return data
}
