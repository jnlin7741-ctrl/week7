<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { request, setToken, clearToken } from './api'
const user = ref(null), mode = ref('signin'), busy = ref(false), loading = ref(true)
const name = ref(''), email = ref(''), password = ref(''), confirmation = ref('')
const posts = ref([]), title = ref(''), content = ref(''), editingId = ref(null)
const message = ref(''), success = ref('')
function resetMessages() { message.value = ''; success.value = '' }
function resetForm() { title.value = ''; content.value = ''; editingId.value = null }
function changeMode() { mode.value = mode.value === 'signin' ? 'signup' : 'signin'; password.value = ''; confirmation.value = ''; resetMessages() }
function signOut() { clearToken(); user.value = null; password.value = ''; confirmation.value = ''; resetForm(); resetMessages() }
function sessionExpired() { signOut(); message.value = 'Your session expired. Sign in again.' }
onMounted(() => { loadPosts(); window.addEventListener('session-expired', sessionExpired) })
onUnmounted(() => window.removeEventListener('session-expired', sessionExpired))
async function loadPosts() {
  loading.value = true
  try { posts.value = await request('/posts') }
  catch (error) { message.value = error.message }
  finally { loading.value = false }
}
async function authenticate() {
  if (busy.value) return
  resetMessages()
  if (mode.value === 'signup' && password.value !== confirmation.value) { message.value = 'The passwords do not match.'; return }
  busy.value = true
  try {
    if (mode.value === 'signup') {
      await request('/auth/signup', { method: 'POST', body: { name: name.value.trim(), email: email.value.trim(), password: password.value } })
      mode.value = 'signin'; password.value = ''; confirmation.value = ''
      success.value = 'Account created. Sign in with your password.'
    } else {
      const result = await request('/auth/login', { method: 'POST', body: { email: email.value.trim(), password: password.value } })
      // Keep the token, then ask the server who is signed in.
      setToken(result.accessToken)
      user.value = await request('/me', { protectedCall: true })
      password.value = ''; resetForm()
      success.value = `Welcome, ${user.value.name}.`
    }
  } catch (error) { message.value = error.message }
  finally { busy.value = false }
}
function startEdit(post) { resetMessages(); editingId.value = post.id; title.value = post.title; content.value = post.content }
async function savePost() {
  if (busy.value) return
  busy.value = true; resetMessages()
  try {
    const editing = editingId.value !== null
    await request(editing ? `/posts/${editingId.value}` : '/posts', {
      method: editing ? 'PUT' : 'POST', protectedCall: true,
      body: { title: title.value.trim(), content: content.value.trim() },
    })
    resetForm(); success.value = editing ? 'Post updated.' : 'Post published.'
    await loadPosts()
  } catch (error) { message.value = error.message }
  finally { busy.value = false }
}
async function removePost(post) {
  if (busy.value || !window.confirm(`Delete “${post.title}”?`)) return
  busy.value = true; resetMessages()
  try {
    await request(`/posts/${post.id}`, { method: 'DELETE', protectedCall: true })
    if (editingId.value === post.id) resetForm()
    success.value = 'Post deleted.'; await loadPosts()
  } catch (error) { message.value = error.message }
  finally { busy.value = false }
}
const dateLabel = value => new Date(value).toLocaleString()
</script>

<template>
  <header class="site-header"><a class="brand" href="/">Campus Board<span>Web Service Design / Week 07</span></a><a class="api-link" href="/swagger-ui.html" target="_blank" rel="noopener">API explorer ↗</a></header>
  <main>
    <section class="intro"><p class="eyebrow">A place for campus ideas</p><h1>Share something<br><em>worth knowing.</em></h1><p>Read the board. Sign in to publish a post of your own.</p></section>
    <div class="status-area" aria-live="polite"><p v-if="message" class="error" role="alert">{{ message }}</p><p v-if="success" class="success">{{ success }}</p></div>
    <div class="workspace">
      <aside>
        <section v-if="!user" class="account"><p class="eyebrow">Your account</p><h2>{{ mode === 'signup' ? 'Create an account' : 'Welcome back' }}</h2>
          <form @submit.prevent="authenticate">
            <label v-if="mode === 'signup'">Name<input v-model="name" required maxlength="60" autocomplete="name" :disabled="busy"></label>
            <label>Email<input v-model="email" required type="email" maxlength="254" autocomplete="username" :disabled="busy"></label>
            <label>Password<input v-model="password" required type="password" :minlength="mode === 'signup' ? 12 : 1" maxlength="72" :autocomplete="mode === 'signup' ? 'new-password' : 'current-password'" :disabled="busy"></label>
            <label v-if="mode === 'signup'">Confirm password<input v-model="confirmation" required type="password" autocomplete="new-password" :disabled="busy"></label>
            <p class="hint">{{ mode === 'signup' ? 'Use 12 or more characters, up to 72 UTF-8 bytes.' : 'Use the password you registered. Reloading signs you out.' }}</p>
            <button class="primary" :disabled="busy">{{ busy ? 'Please wait…' : mode === 'signup' ? 'Create account' : 'Sign in' }}</button>
          </form><button class="text-button" :disabled="busy" @click="changeMode">{{ mode === 'signup' ? 'Already registered? Sign in' : 'New here? Create an account' }}</button>
        </section>
        <section v-else class="account"><p class="eyebrow">Signed in</p><h2>{{ user.name }}</h2><p class="email">{{ user.email }}</p><button class="text-button" :disabled="busy" @click="signOut">Sign out</button>
          <div class="compose"><h3>{{ editingId ? 'Edit your post' : 'A new post' }}</h3><form @submit.prevent="savePost">
            <label>Title<input v-model="title" required maxlength="120" :disabled="busy" placeholder="What would you like to share?"></label>
            <label>Message<textarea v-model="content" required maxlength="4000" rows="6" :disabled="busy" placeholder="Add the details…"></textarea></label>
            <button class="primary" :disabled="busy || !title.trim() || !content.trim()">{{ busy ? 'Saving…' : editingId ? 'Save changes' : 'Publish post' }}</button>
            <button v-if="editingId" type="button" class="text-button" :disabled="busy" @click="resetForm">Cancel editing</button>
          </form></div>
        </section>
      </aside>
      <section class="board"><div class="board-heading"><h2>The latest</h2><button class="text-button" :disabled="loading || busy" @click="loadPosts">Refresh</button></div>
        <p v-if="loading" role="status" class="empty">Loading posts…</p>
        <p v-else-if="!posts.length" class="empty">The board is ready for your first idea.</p>
        <article v-for="post in posts" :key="post.id" class="post"><div class="post-meta"><time :datetime="post.createdAt">{{ dateLabel(post.createdAt) }}</time><span v-if="user?.id === post.ownerId">Your post</span></div><h3>{{ post.title }}</h3><p class="post-content">{{ post.content }}</p>
          <div v-if="user?.id === post.ownerId" class="post-actions"><button :disabled="busy" @click="startEdit(post)">Edit</button><button :disabled="busy" @click="removePost(post)">Delete</button></div>
        </article><p class="hint">Showing the latest {{ posts.length }} posts, up to 50.</p>
      </section>
    </div>
  </main><footer>Campus Board / English section / MacsLAB</footer>
</template>
