import React, { useEffect, useMemo, useRef, useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { API_BASE_URL } from "../api/httpClient";

const navItems = [
  { path: "/courses", label: "Khóa học" },
  { path: "/mentors", label: "Giảng Viên" },
  { path: "/blog", label: "Bài Viết" },
  { path: "/about", label: "Về chúng tôi" },
  { path: "/contact", label: "Liên hệ" },
];

const bellIcon = (
  <svg viewBox="0 0 24 24" className="w-5 h-5" fill="none" stroke="currentColor">
    <path
      strokeWidth="1.6"
      d="M6 9a6 6 0 0 1 12 0c0 4 1 6 2 7H4c1-1 2-3 2-7Z"
    />
    <path strokeWidth="1.6" d="M9 18c0 1.66 1.34 3 3 3s3-1.34 3-3" />
  </svg>
);

const API_BASE = API_BASE_URL?.replace(/\/+$/, "") || "";

const resolveAssetUrl = (value) => {
  if (!value) return "";
  const raw = String(value).trim();
  if (!raw) return "";
  if (/^(?:https?:|data:|blob:)/i.test(raw)) return raw;
  const normalized = raw.startsWith("/") ? raw : `/${raw}`;
  return `${API_BASE}${normalized}`;
};

const resolveAvatarUrl = (value) => resolveAssetUrl(value);

export default function NavBar() {
  const navigate = useNavigate();
  const { isAuthenticated, user, logout } = useAuth();

  const linkCls = ({ isActive }) =>
    `text-sm ${isActive ? "text-stone-900" : "text-stone-700 hover:text-stone-900"}`;

  const avatarUrl = useMemo(() => resolveAvatarUrl(user?.avatarUrl), [user?.avatarUrl]);
  const userInitial = (user?.fullName || user?.email || "U").trim().charAt(0).toUpperCase();

  return (
    <header className="sticky top-0 z-30 bg-white/80 backdrop-blur border-b border-stone-200">
      <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between gap-4">
        <Link to="/" className="flex items-center gap-2">
          <div className="w-9 h-9 rounded-md bg-primary-600 text-white grid place-items-center font-bold">F4</div>
          {/* <span className="font-semibold text-stone-900">F4</span> */}
        </Link>
        <div className="hidden lg:flex items-center gap-4 flex-1 ml-4">
          <nav className="flex items-center gap-6">
            {navItems.map((item) => (
              <NavLink key={item.path} to={item.path} className={linkCls}>
                {item.label}
              </NavLink>
            ))}
            {(user?.roles?.includes("teacher") || user?.roles?.includes("manager")) && (
              <NavLink to="/my-courses" className={linkCls}>
                Khóa học của tôi
              </NavLink>
            )}
          </nav>
          <SearchSuggest className="flex-1 max-w-md" />
        </div>
        {!isAuthenticated ? (
          <div className="flex items-center gap-2">
            <Link to="/login" className="btn">
              Đăng nhập
            </Link>
            <Link to="/register" className="btn btn-primary">
              Bắt đầu miễn phí
            </Link>
          </div>
        ) : (
          <div className="flex items-center gap-2">
            {/* <span className="hidden md:inline text-sm font-medium text-stone-700">Khóa học của tôi</span> */} 
            <UserDropdown
              user={user}
              avatarUrl={avatarUrl}
              fallbackInitial={userInitial}
              onLogout={() => {
                logout();
                navigate("/login");
              }}
            />
          </div>
        )}
      </div>
    </header>
  );
}

function UserDropdown({ user, avatarUrl, fallbackInitial, onLogout }) {
  const [open, setOpen] = useState(false);
  const hoverTimeout = useRef(null);
  const usernameDisplay = user?.username ? `@${user.username}` : user?.email || "";

  const displayInitial = (fallbackInitial || "U").trim().charAt(0).toUpperCase();

  const quickLinks = useMemo(
    () => [
      { label: "Trang cá nhân", to: "/student" },
      { label: "Viết bài viết", to: "/blog" },
      { label: "Cài đặt", to: "/account/settings" },
    ],
    [],
  );

  const handleOpen = () => {
    if (hoverTimeout.current) {
      clearTimeout(hoverTimeout.current);
      hoverTimeout.current = null;
    }
    setOpen(true);
  };

  const handleClose = () => {
    if (hoverTimeout.current) clearTimeout(hoverTimeout.current);
    hoverTimeout.current = setTimeout(() => setOpen(false), 120);
  };

  const handleToggle = () => {
    if (hoverTimeout.current) {
      clearTimeout(hoverTimeout.current);
      hoverTimeout.current = null;
    }
    setOpen((prev) => !prev);
  };

  const handleLogout = () => {
    setOpen(false);
    onLogout();
  };

  return (
    <div
      className="relative"
      onMouseEnter={handleOpen}
      onMouseLeave={handleClose}
    >
      <button
        type="button"
        onClick={handleToggle}
        className={`w-10 h-10 rounded-full overflow-hidden grid place-items-center shadow focus:outline-none focus:ring-2 focus:ring-teal-300 ${avatarUrl ? "" : "bg-teal-600 text-white font-semibold"}`}
      >
        {avatarUrl ? (
          <img src={avatarUrl} alt={user?.fullName || "Avatar"} className="w-full h-full object-cover" />
        ) : (
          <span>{displayInitial}</span>
        )}
      </button>
      {open && (
        <div className="absolute right-0 mt-3 w-64 rounded-3xl bg-white shadow-2xl border border-stone-100">
          <div className="p-4 flex items-center gap-3">
            <div className="w-12 h-12 rounded-full overflow-hidden bg-teal-100 text-teal-700 grid place-items-center font-semibold text-lg">
              {avatarUrl ? (
                <img src={avatarUrl} alt={user?.fullName || "Avatar"} className="w-full h-full object-cover" />
              ) : (
                <span>{displayInitial}</span>
              )}
            </div>
            <div>
              <p className="font-semibold text-stone-900">{user?.fullName || "Chưa cập nhật"}</p>
              <p className="text-sm text-stone-500">{usernameDisplay}</p>
            </div>
          </div>
          <div className="border-t border-stone-100">
            {quickLinks.map((item) => (
              <Link
                key={item.label}
                to={item.to}
                onClick={() => setOpen(false)}
                className="block px-5 py-2.5 text-sm text-stone-700 hover:bg-stone-50"
              >
                {item.label}
              </Link>
            ))}
            <button
              type="button"
              onClick={handleLogout}
              className="w-full text-left px-5 py-2.5 text-sm text-red-600 hover:bg-red-50 rounded-b-3xl"
            >
              Đăng xuất
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function SearchSuggest({ className = "" }) {
  const [term, setTerm] = useState("");
  const [open, setOpen] = useState(false);
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const containerRef = useRef(null);

  useEffect(() => {
    const handler = (evt) => {
      if (containerRef.current && !containerRef.current.contains(evt.target)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  useEffect(() => {
    const query = term.trim();
    if (!query) {
      setResults([]);
      setError("");
      setLoading(false);
      return;
    }
    setLoading(true);
    setError("");
    const controller = new AbortController();
    const timeout = setTimeout(async () => {
      try {
        const resp = await fetch(
          `${API_BASE}/api/public/courses-sql?limit=5&status=published&q=${encodeURIComponent(query)}`,
          { signal: controller.signal },
        );
        if (!resp.ok) throw new Error("failed");
        const data = await resp.json();
        if (!controller.signal.aborted) {
          const normalized = Array.isArray(data) ? data : [];
          setResults(normalized);
          if (normalized.length === 0) {
            setError("Không tìm thấy khóa học phù hợp.");
          }
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setError("Không tải được gợi ý.");
          setResults([]);
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }, 250);
    return () => {
      clearTimeout(timeout);
      controller.abort();
    };
  }, [term]);

  const handleFocus = () => setOpen(true);

  const renderBody = () => {
    const query = term.trim();
    if (!query) {
      return <p className="px-4 py-3 text-sm text-stone-500">Nhập từ khóa để tìm khóa học.</p>;
    }
    if (loading) {
      return <p className="px-4 py-3 text-sm text-stone-500">Đang tìm kiếm...</p>;
    }
    if (error) {
      return <p className="px-4 py-3 text-sm text-amber-700">{error}</p>;
    }
    return (
      <>
        <div className="flex items-center justify-between px-4 py-2 text-xs uppercase tracking-[0.3em] text-stone-400">
          <span>Kết quả cho “{query}”</span>
          <Link to={`/courses?q=${encodeURIComponent(query)}`} className="text-primary-600">
            Xem thêm
          </Link>
        </div>
        <ul className="divide-y divide-stone-100">
          {results.map((item) => (
            <li key={item.id} className="hover:bg-stone-50 transition">
              <Link
                to={`/courses/${item.slug || item.id}`}
                className="flex items-center gap-3 px-4 py-3"
                onClick={() => setOpen(false)}
              >
                <div className="h-12 w-12 flex-shrink-0 overflow-hidden rounded-xl bg-stone-100">
                  {item.thumbnail_url || item.thumbnailUrl ? (
                    <img
                      src={resolveAssetUrl(item.thumbnail_url || item.thumbnailUrl)}
                      alt={item.title}
                      className="h-full w-full object-cover"
                    />
                  ) : (
                    <div className="h-full w-full bg-gradient-to-br from-primary-200 to-primary-50" />
                  )}
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-stone-900 line-clamp-1">{item.title}</p>
                  <p className="text-xs text-stone-500 line-clamp-1">
                    {item.level || "Mọi cấp độ"} · {item.lessons_count ?? item.lessonsCount ?? 0} bài học
                  </p>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      </>
    );
  };

  return (
    <div ref={containerRef} className={`relative ${className}`}>
      <div className="flex items-center gap-2 rounded-full border border-stone-200 bg-white px-4 py-2 shadow-sm focus-within:border-primary-400 focus-within:ring-2 focus-within:ring-primary-100">
        <svg viewBox="0 0 24 24" className="h-4 w-4 text-stone-400" fill="none" stroke="currentColor" strokeWidth="1.8">
          <circle cx="11" cy="11" r="7" />
          <path d="m16 16 4 4" />
        </svg>
        <input
          type="text"
          value={term}
          onFocus={handleFocus}
          onChange={(e) => setTerm(e.target.value)}
          placeholder="Tìm kiếm khóa học..."
          className="w-full border-none bg-transparent text-sm text-stone-800 placeholder:text-stone-400 focus:outline-none"
        />
        {term && (
          <button
            type="button"
            className="rounded-full bg-stone-100 p-1 text-xs text-stone-500 hover:bg-stone-200"
            onClick={() => {
              setTerm("");
              setResults([]);
              setError("");
            }}
            aria-label="Xóa tìm kiếm"
          >
            ✕
          </button>
        )}
      </div>
      {open && (
        <div className="absolute left-0 right-0 mt-2 rounded-3xl border border-stone-100 bg-white shadow-2xl">
          {renderBody()}
        </div>
      )}
    </div>
  );
}
