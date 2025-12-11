(() => {
  const api = (path, opts = {}) => {
    const token = localStorage.getItem('token');
    const headers = Object.assign({'Content-Type':'application/json'}, opts.headers || {});
    if (token) headers['Authorization'] = `Bearer ${token}`;
    return fetch(path, Object.assign({}, opts, { headers })).then(async r => {
      const txt = await r.text();
      let data; try { data = txt ? JSON.parse(txt) : null; } catch { data = txt; }
      if (!r.ok) throw new Error((data && data.message) || data || r.statusText);
      return data;
    });
  };

  const el = id => document.getElementById(id);
  el('env').textContent = window.location.origin;

  // DB Info
  api('/api/public/dbinfo')
    .then(d => el('dbinfo').textContent = `Server: ${d.server_name}, DB: ${d.db_name}`)
    .catch(e => el('dbinfo').textContent = `Không đọc được dbinfo: ${e.message}`);

  const updateWho = () => {
    const email = localStorage.getItem('email');
    const roles = localStorage.getItem('roles');
    el('who').textContent = email ? `${email} [${roles||''}]` : 'Chưa đăng nhập';
    el('logoutBtn').style.display = email ? 'inline-block' : 'none';
  };
  updateWho();
  el('logoutBtn').onclick = () => { localStorage.clear(); updateWho(); };

  // Register
  el('registerBtn').onclick = async () => {
    const body = {
      email: el('rEmail').value.trim(),
      fullName: el('rName').value.trim(),
      password: el('rPass').value,
      role: el('rRole').value
    };
    el('registerMsg').textContent = 'Đang gửi...';
    try {
      const res = await api('/api/auth/register', { method:'POST', body: JSON.stringify(body) });
      localStorage.setItem('token', res.accessToken);
      localStorage.setItem('email', res.email);
      localStorage.setItem('roles', (res.roles && res.roles.items || []).join(','));
      el('registerMsg').textContent = 'Đăng ký thành công';
      updateWho();
    } catch (e) {
      el('registerMsg').textContent = `Lỗi: ${e.message}`;
    }
  };

  // Login
  el('loginBtn').onclick = async () => {
    const body = { email: el('lEmail').value.trim(), password: el('lPass').value };
    el('loginMsg').textContent = 'Đang đăng nhập...';
    try {
      const res = await api('/api/auth/login', { method:'POST', body: JSON.stringify(body) });
      localStorage.setItem('token', res.accessToken);
      localStorage.setItem('email', res.email);
      localStorage.setItem('roles', (res.roles && res.roles.items || []).join(','));
      el('loginMsg').textContent = 'Đăng nhập thành công';
      updateWho();
    } catch (e) {
      el('loginMsg').textContent = `Lỗi: ${e.message}`;
    }
  };

  // Load Users
  el('loadUsers').onclick = async () => {
    const rows = el('userRows');
    rows.innerHTML = '<tr><td colspan="4" class="muted">Đang tải...</td></tr>';
    try {
      const data = await api('/api/users?limit=20');
      rows.innerHTML = data.map(u => `<tr><td>${u.id}</td><td>${u.email}</td><td>${u.fullName||''}</td><td>${(u.roles||[]).map(r=>r.code||r).join(',')}</td></tr>`).join('');
    } catch (e) {
      rows.innerHTML = `<tr><td colspan="4" class="err">Lỗi: ${e.message} (cần đăng nhập?)</td></tr>`;
    }
  };

  // Create Course
  el('createCourse').onclick = async () => {
    const body = {
      title: el('cTitle').value.trim(),
      slug: el('cSlug').value.trim(),
      shortDesc: el('cShort').value.trim(),
      language: el('cLang').value,
      level: el('cLevel').value,
      status: el('cStatus').value,
      price: el('cPrice').value ? Number(el('cPrice').value) : null
    };
    el('courseMsg').textContent = 'Đang tạo...';
    try {
      const res = await api('/api/courses', { method:'POST', body: JSON.stringify(body) });
      el('courseMsg').textContent = `Tạo thành công ID=${res.id}`;
      el('loadCourses').click();
    } catch (e) {
      el('courseMsg').textContent = `Lỗi: ${e.message} (cần quyền TEACHER/MANAGER)`;
    }
  };

  // Load Courses
  const renderCourses = async () => {
    const rows = el('courseRows');
    rows.innerHTML = '<tr><td colspan="5" class="muted">Đang tải...</td></tr>';
    try {
      const data = await api('/api/courses?limit=20');
      rows.innerHTML = data.map(c => `<tr><td>${c.id}</td><td>${c.title}</td><td>${c.slug}</td><td>${c.createdByEmail||''}</td><td>${c.status||''}</td></tr>`).join('');
    } catch (e) {
      rows.innerHTML = `<tr><td colspan="5" class="err">Lỗi: ${e.message}</td></tr>`;
    }
  };
  el('loadCourses').onclick = renderCourses;
  // load on start
  renderCourses();
})();

