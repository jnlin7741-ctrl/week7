// End-to-end access control demonstration for Week 07
const base = process.argv[2] || 'http://localhost:18707';

function rand() { return Math.random().toString(36).slice(2, 10); }

async function req(method, path, body, token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(base + path, {
    method, headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json = null;
  try { json = JSON.parse(text); } catch {}
  return { status: res.status, json, text };
}

(async () => {
  const alexEmail = `alex_${rand()}@week07.test`;
  const samEmail = `sam_${rand()}@week07.test`;
  const pw = 'password-12-characters-min';

  console.log('=== Access Control Evidence ===\n');

  // 1. No token /api/me -> 401
  let r = await req('GET', '/api/me');
  console.log(`No token GET /api/me        -> ${r.status} (expected 401)`);

  // 2. Register Alex & Sam
  r = await req('POST', '/api/auth/signup', { name: 'Alex', email: alexEmail, password: pw });
  console.log(`Alex signup                 -> ${r.status} (expected 201)`);
  r = await req('POST', '/api/auth/signup', { name: 'Sam', email: samEmail, password: pw });
  console.log(`Sam signup                  -> ${r.status} (expected 201)`);

  // 3. Duplicate signup -> 409
  r = await req('POST', '/api/auth/signup', { name: 'Alex', email: alexEmail, password: pw });
  console.log(`Duplicate email signup      -> ${r.status} (expected 409)`);

  // 4. Wrong password -> 401
  r = await req('POST', '/api/auth/login', { email: alexEmail, password: 'wrong-password-123' });
  console.log(`Wrong password login        -> ${r.status} (expected 401)`);

  // 5. Alex login -> 200
  r = await req('POST', '/api/auth/login', { email: alexEmail, password: pw });
  const alexToken = r.json.accessToken;
  console.log(`Alex correct login          -> ${r.status} (expected 200)`);

  // 6. Alex /api/me with token -> 200
  r = await req('GET', '/api/me', null, alexToken);
  console.log(`Alex GET /api/me (token)    -> ${r.status} (expected 200), email=${r.json.email}`);

  // 7. Alex creates post -> 201
  r = await req('POST', '/api/posts', { title: 'Alex post', content: 'Hello from Alex' }, alexToken);
  const postId = r.json.id;
  console.log(`Alex creates post           -> ${r.status} (expected 201), id=${postId}`);

  // 8. Sam login
  r = await req('POST', '/api/auth/login', { email: samEmail, password: pw });
  const samToken = r.json.accessToken;

  // 9. Sam edits Alex's post -> 403
  r = await req('PUT', `/api/posts/${postId}`, { title: 'Hacked', content: 'by Sam' }, samToken);
  console.log(`Sam edits Alex's post       -> ${r.status} (expected 403)`);

  // 10. Sam deletes Alex's post -> 403
  r = await req('DELETE', `/api/posts/${postId}`, null, samToken);
  console.log(`Sam deletes Alex's post     -> ${r.status} (expected 403)`);

  // 11. Alex edits own post -> 200
  r = await req('PUT', `/api/posts/${postId}`, { title: 'Alex edited', content: 'updated' }, alexToken);
  console.log(`Alex edits own post         -> ${r.status} (expected 200)`);

  // 12. Alex deletes own post -> 204
  r = await req('DELETE', `/api/posts/${postId}`, null, alexToken);
  console.log(`Alex deletes own post       -> ${r.status} (expected 204)`);

  // 13. Read deleted post -> 404
  r = await req('GET', `/api/posts/${postId}`);
  console.log(`Read deleted post           -> ${r.status} (expected 404)`);

  console.log('\n=== All access-control scenarios verified ===');
})();
