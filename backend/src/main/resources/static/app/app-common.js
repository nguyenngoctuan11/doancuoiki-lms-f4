export const apiBase = '';

const MENU_ICON_SVGS = {
  'teacher-courses': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
    <path d="M4 7h16M4 12h16M4 17h10" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>`,
  'teacher-new-course': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
    <rect x="4" y="4" width="16" height="16" rx="3"/>
    <path d="M12 8v8M8 12h8" stroke-linecap="round"/>
  </svg>`,
  'teacher-quiz': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
    <path d="M8 7h8M8 12h4M8 17h8" stroke-linecap="round"/>
    <path d="M16.5 4.5v15a1.5 1.5 0 0 1-1.5 1.5H7a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3h7.5a2 2 0 0 1 2 1.5Z"/>
  </svg>`,
  'teacher-notes': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
    <path d="M6 5h12a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2h-4.5L9 21v-5H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M8 9h8M8 12h5" stroke-linecap="round"/>
  </svg>`,
  'teacher-reviews': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
    <path d="M5 5h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-5l-4 4v-4H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="m8 11 2 2 4-4" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>`,
  'teacher-resources': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
    <path d="M6 4h9l5 5v11a1 1 0 0 1-1 1H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M14 4v4h4" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M8 13h8M8 17h5" stroke-linecap="round"/>
  </svg>`,
  'teacher-analytics': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
    <path d="M4 19h16" stroke-linecap="round"/>
    <path d="M7 19V9" stroke-linecap="round"/>
    <path d="M12 19V5" stroke-linecap="round"/>
    <path d="M17 19v-7" stroke-linecap="round"/>
    <circle cx="7" cy="8" r="1"/>
    <circle cx="12" cy="4" r="1"/>
    <circle cx="17" cy="11" r="1"/>
  </svg>`,
  'admin-users': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
    <circle cx="9" cy="9" r="3"/>
    <path d="M4 20c0-3 2-6 5-6s5 3 5 6" stroke-linecap="round"/>
    <path d="M17 11.5a2.5 2.5 0 1 0 0-5" />
    <path d="M18 20v-1c0-1.6-.8-3-2-3" stroke-linecap="round"/>
  </svg>`,
  'manager-review': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
    <path d="M7 4h10a2 2 0 0 1 2 2v9.5a2 2 0 0 1-.59 1.41l-2.5 2.5a2 2 0 0 1-1.41.59H7a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z"/>
    <path d="m9 12 2 2 4-4" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>`,
  analytics: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
    <path d="M4 19h16" stroke-linecap="round"/>
    <path d="M8 19V9" stroke-linecap="round"/>
    <path d="M12 19V5" stroke-linecap="round"/>
    <path d="M16 19v-7" stroke-linecap="round"/>
  </svg>`,
  'support-chat': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
    <path d="M8 9h8M8 13h4" stroke-linecap="round"/>
    <path d="M5 20v-3H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H9Z" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>`,
  'blog-approval': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
    <path d="M5 5h14v12H9l-4 4V5Z" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M10 9h6M10 13h4" stroke-linecap="round"/>
  </svg>`,
  'lesson-resource-review': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
    <path d="M5 5h10l4 4v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M9 13h6M9 17h4M15 5v4h4" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>`,
  'db-browser': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
    <ellipse cx="12" cy="5" rx="7" ry="3"/>
    <path d="M5 5v14c0 1.66 3.13 3 7 3s7-1.34 7-3V5" />
    <path d="M5 12c0 1.66 3.13 3 7 3s7-1.34 7-3" />
  </svg>`,
  default: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
    <circle cx="12" cy="12" r="3"/>
    <circle cx="12" cy="12" r="9"/>
  </svg>`,
};

function renderMenuIcon(id) {
  return MENU_ICON_SVGS[id] || MENU_ICON_SVGS.default;
}

export function token() {
  return localStorage.getItem('token');
}

export function setAuth(res) {
  if (res && res.accessToken) {
    localStorage.setItem('token', res.accessToken);
    localStorage.setItem('email', res.email || '');
    localStorage.setItem('displayName', res.fullName || res.username || '');
    localStorage.setItem('avatarUrl', res.avatarUrl || '');
    const roles = res.roles?.items ? res.roles.items : [];
    localStorage.setItem('roles', roles.join(','));
  }
}

export function clearAuth() {
  localStorage.removeItem('token');
  localStorage.removeItem('email');
  localStorage.removeItem('displayName');
  localStorage.removeItem('avatarUrl');
  localStorage.removeItem('roles');
}

export function currentUser() {
  const email = localStorage.getItem('email') || '';
  const name = localStorage.getItem('displayName') || '';
  const avatarUrl = localStorage.getItem('avatarUrl') || '';
  const rawRoles = localStorage.getItem('roles') || '';
  const roles = rawRoles
    .split(',')
    .map((r) => r.trim().toUpperCase())
    .filter(Boolean);
  return { email, rawRoles, roles, name, avatarUrl };
}

function adminLinksForRoles(roles) {
  const items = [];
  const canTeach = roles.includes('TEACHER') || roles.includes('MANAGER');
  if (canTeach) {
    items.push({
      id: 'teacher-courses',
      href: '/app/admin/teacher-courses.html',
      label: 'Khóa học của tôi',
      hint: 'Theo d\u00f5i b\u1ea3n nh\u00e1p & tr\u1ea1ng th\u00e1i',
    });
    items.push({
      id: 'teacher-new-course',
      href: '/app/admin/teacher-new-course.html',
      label: 'Tạo khóa học',
      hint: 'Lên nội dung & modules',
    });
    items.push({
      id: 'teacher-quiz',
      href: '/app/admin/teacher-quiz.html',
      label: 'Tạo bài kiểm tra',
      hint: 'Thêm câu hỏi, media',
    });
    items.push({
      id: 'teacher-notes',
      href: '/app/admin/teacher-notes.html',
      label: 'Phản hồi ghi chú',
      hint: 'Theo d\u00f5i & tr\u1ea3 l\u1eddi ghi ch\u00fa h\u1ecdc vi\u00ean',
    });
    items.push({
      id: 'teacher-reviews',
      href: '/app/admin/teacher-reviews.html',
      label: 'Đánh giá khóa học',
      hint: 'Xem & duy\u1ec7t nh\u1eadn x\u00e9t h\u1ecdc vi\u00ean',
    });
    items.push({
      id: 'teacher-resources',
      href: '/app/admin/teacher-resources.html',
      label: 'Tài liệu bài học',
      hint: 'Tải lên & duyệt tài liệu',
    });
    items.push({
      id: 'teacher-analytics',
      href: '/app/admin/teacher-analytics.html',
      label: 'Thống kê giảng viên',
      hint: 'Theo d\u00f5i KPI & xu h\u01b0\u1edbng l\u1edbp h\u1ecdc',
    });
  }
  if (roles.includes('MANAGER')) {
    items.push({
      id: 'admin-users',
      href: '/app/admin/users.html',
      label: 'Người dùng',
      hint: 'Tạo / sửa / xóa tài khoản',
    });
    items.push({
      id: 'manager-review',
      href: '/app/admin/manager-review.html',
      label: 'Duyệt khóa học',
      hint: 'Phê duyệt nội dung mới',
    });
    items.push({
      id: 'analytics',
      href: '/app/admin/analytics.html',
      label: 'Báo cáo thống kê',
      hint: 'Ng\u01b0\u1eddi d\u00f9ng, kh\u00f3a h\u1ecdc, doanh thu',
    });
    items.push({
      id: 'blog-approval',
      href: '/app/admin/blog-approval.html',
      label: 'Duyệt blog',
      hint: 'Xem & duyệt bài viết cộng đồng',
    });
    items.push({
      id: 'lesson-resource-review',
      href: '/app/admin/lesson-resource-review.html',
      label: 'Duyệt tài liệu',
      hint: 'Quản lý tài liệu của giảng viên',
    });
    items.push({
      id: 'orders',
      href: '/app/admin/orders.html',
      label: 'Đơn khóa học',
      hint: 'Duyệt / hủy đơn thanh toán',
    });
    items.push({
      id: 'support-chat',
      href: '/app/admin/support-chat.html',
      label: 'Chat học viên',
      hint: 'Trả lời yêu cầu hỗ trợ',
    });
    items.push({
      id: 'db-browser',
      href: '/app/admin/db-browser.html',
      label: 'DB Browser',
      hint: 'Quan sát dữ liệu trực tiếp',
    });
  }
  return items;
}


export async function api(path, opts = {}) {
  const headers = { 'Content-Type': 'application/json', ...(opts.headers || {}) };
  if (token()) headers.Authorization = `Bearer ${token()}`;
  let target = path || '';
  if (!/^https?:/i.test(target)) {
    target = target.startsWith('/') ? target : `/${target}`;
    target = `${apiBase}${target}`;
  }
  const response = await fetch(target, { ...opts, headers });
  const text = await response.text();
  let data;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }
  if (!response.ok) {
    throw new Error((data && (data.message || data.error)) || response.statusText);
  }
  return data;
}

export function qs(name) {
  return new URLSearchParams(location.search).get(name);
}

export function handleLogout(e) {
  if (e && typeof e.preventDefault === 'function') e.preventDefault();
  clearAuth();
  location.href = '/app/auth/login.html';
}

export function nav(variant = 'default') {
  if (variant === 'sidebar' || variant === 'none') return '';
  const { email, roles, name, avatarUrl } = currentUser();
  const currentPath = location.pathname;
  const mainLinks = [
    { href: '/app/index.html', label: 'Tổng quan' },
    { href: '/app/courses.html', label: 'Khóa học' },
  ];
  const adminLinks = variant === 'compact' ? [] : adminLinksForRoles(roles);
  const whoLabel = name || email;
  const avatarInitial = (whoLabel || email || '?').trim().charAt(0).toUpperCase();
  const resolveAvatarUrl = (url) => {
    if (!url) return '';
    if (/^https?:/i.test(url)) return url;
    if (url.startsWith('/')) return `${apiBase}${url}`;
    return `${apiBase}/${url}`;
  };
  const avatarSrc = resolveAvatarUrl(avatarUrl);
  const avatarInner = avatarSrc
    ? `<img src="${avatarSrc}" alt="${whoLabel || 'Avatar'}" />`
    : avatarInitial;
  const who = email
    ? `<div class="avatar-menu">
         <button class="avatar-button" type="button" id="avatarButton" title="${whoLabel || email}">
           ${avatarInner}
         </button>
         <div class="avatar-dropdown" id="avatarDropdown">
           <div class="who-line">
             <strong>${whoLabel || 'Người dùng'}</strong>
             ${name && email ? `<div class="muted small">${email}</div>` : ''}
             ${roles.length ? `<div class="muted tiny">${roles.join(', ')}</div>` : ''}
           </div>
           <a id="logoutLink" class="text-link" href="#">Đăng xuất</a>
         </div>
       </div>`
    : `<a class="text-link" href="/app/auth/login.html">Đăng nhập</a>
       <a class="text-link" href="/app/auth/register.html">Đăng ký</a>`;
  return `
    <header class="topbar ${variant === 'compact' ? 'topbar-compact' : ''}">
      <div class="brand">
        <div class="brand-dot" aria-label="F4">F4</div>
        <div>
          <div>LMS Admin</div>
          <small class="muted">Không gian quản trị</small>
        </div>
      </div>
      <nav class="menu">
        ${mainLinks
          .map(
            (link) =>
              `<a href="${link.href}" class="${currentPath === link.href ? 'active' : ''}">${link.label}</a>`,
          )
          .join('')}
        ${adminLinks
          .map(
            (link) =>
              `<a href="${link.href}" class="${currentPath === link.href ? 'active' : ''}">${link.label}</a>`,
          )
          .join('')}
      </nav>
      <div class="who">${who}</div>
    </header>
  `;
}

export function mountNav(targetId = 'nav', variantOverride) {
  const container = document.getElementById(targetId);
  if (!container) return;
  const variant = variantOverride || (container.dataset?.variant || 'default');
  const html = nav(variant);
  if (!html) {
    container.innerHTML = '';
    container.style.display = 'none';
    return;
  }
  container.innerHTML = html;
  container.querySelector('#logoutLink')?.addEventListener('click', handleLogout);
  const avatarBtn = container.querySelector('#avatarButton');
  const avatarDropdown = container.querySelector('#avatarDropdown');
  if (avatarBtn && avatarDropdown) {
    avatarBtn.addEventListener('click', (event) => {
      event.preventDefault();
      event.stopPropagation();
      avatarDropdown.classList.toggle('open');
    });
    if (globalThis.__avatarMenuCloser) {
      document.removeEventListener('click', globalThis.__avatarMenuCloser);
    }
    const closer = (evt) => {
      if (!avatarDropdown.contains(evt.target) && evt.target !== avatarBtn) {
        avatarDropdown.classList.remove('open');
      }
    };
    document.addEventListener('click', closer);
    globalThis.__avatarMenuCloser = closer;
  } else if (globalThis.__avatarMenuCloser) {
    document.removeEventListener('click', globalThis.__avatarMenuCloser);
    globalThis.__avatarMenuCloser = null;
  }
  const ensureAvatar = async () => {
    if (!token()) return;
    if (currentUser().avatarUrl) return;
    try {
      const res = await fetch(`${apiBase}/api/profile`, {
        headers: { Authorization: `Bearer ${token()}` },
      });
      if (!res.ok) return;
      const data = await res.json();
      if (data?.avatarUrl) {
        localStorage.setItem('avatarUrl', data.avatarUrl);
        mountNav(targetId, variantOverride);
      }
    } catch {
      /* ignore */
    }
  };
  ensureAvatar();
}

export function mountAdminMenu(targetId = 'adminMenu', activeId) {
  const host = document.getElementById(targetId);
  if (!host) return;
  const { roles } = currentUser();
  const links = adminLinksForRoles(roles);
  if (!links.length) {
    host.innerHTML = '<div class="empty">Bạn chưa có quyền quản lý</div>';
    return;
  }
  const key = activeId || location.pathname;
  host.innerHTML = links
    .map((link) => {
      const isActive = key === link.id || key === link.href || location.pathname === link.href;
      return `<a href="${link.href}" class="admin-menu-link ${isActive ? 'is-active' : ''}">
        <span class="icon" aria-hidden="true">${renderMenuIcon(link.id)}</span>
        <span class="info">
          <span class="title">${link.label}</span>
        </span>
      </a>`;
    })
    .join('');
}

export function mountAdminUser(targetId = 'adminUserCard') {
  const host = document.getElementById(targetId);
  if (!host) return;
  const { email, roles, name } = currentUser();
  if (!email) {
    host.innerHTML = `<span class="muted">Chưa đăng nhập</span>
      <a class="btn ghost small" href="/app/auth/login.html">Đăng nhập</a>`;
    return;
  }
  const displayName = name || email.split('@')[0] || email;
  host.innerHTML = `
    <span class="muted">Đang đăng nhập</span>
    <strong>${displayName}</strong>
    <span class="muted">${roles.join(', ') || 'user'}</span>
    <a href="#" id="sidebarLogout" class="text-link">Đăng xuất</a>
  `;
  host.querySelector('#sidebarLogout')?.addEventListener('click', handleLogout);
}

export function requireAuth(roles) {
  if (!token()) {
    location.href = '/app/auth/login.html';
    return false;
  }
  if (roles) {
    const myRoles = currentUser().roles;
    const needed = (Array.isArray(roles) ? roles : [roles]).map((r) => String(r).toUpperCase());
    const ok = needed.some((role) => myRoles.includes(role));
    if (!ok) {
      alert('Bạn không có quyền truy cập khu vực này');
      location.href = '/app/index.html';
      return false;
    }
  }
  return true;
}
