import React, { useCallback, useEffect, useMemo, useState } from "react";
import { API_BASE_URL } from "../api/httpClient";
import { useSupportChat } from "../context/SupportChatContext";

const FALLBACK_THUMB =
  "https://images.unsplash.com/photo-1513258496099-48168024aec0?auto=format&fit=crop&w=480&q=60";

function StatusBadge({ value }) {
  if (!value) return <span className="text-stone-500">--</span>;
  const tone =
    value === "published"
      ? "bg-emerald-50 text-emerald-700"
      : value === "pending"
      ? "bg-amber-50 text-amber-700"
      : "bg-stone-100 text-stone-600";
  return (
    <span className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-wide ${tone}`}>{value}</span>
  );
}

function LevelChip({ value }) {
  if (!value) return null;
  return (
    <span className="rounded-full bg-primary-50 px-3 py-1 text-xs font-semibold text-primary-700 shadow-inner">
      {value}
    </span>
  );
}

function EmptyState({ onCreate }) {
  return (
    <div className="rounded-[32px] border-2 border-dashed border-stone-200 bg-stone-50 py-16 text-center text-stone-500">
      <p>Hiện chưa có khóa học nào. Bạn hãy bắt đầu tạo khóa học đầu tiên của mình nhé!</p>
      <button
        type="button"
        onClick={onCreate}
        className="mt-4 inline-flex items-center rounded-full bg-stone-900 px-6 py-2.5 text-sm font-semibold text-white shadow-lg hover:bg-stone-800"
      >
        + Tạo khóa học mới
      </button>
    </div>
  );
}

export default function MyCourses() {
  const API = API_BASE_URL;
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState(null);
  const [query, setQuery] = useState("");
  const { setEntryContext } = useSupportChat();

  const authHeaders = () => {
    const token = localStorage.getItem("token");
    return token ? { Authorization: `Bearer ${token}` } : {};
  };

  const load = useCallback(() => {
    setLoading(true);
    setErr(null);
    fetch(`${API}/api/teacher/courses/my`, { headers: { "Content-Type": "application/json", ...authHeaders() } })
      .then((r) => r.json())
      .then((data) => setItems(Array.isArray(data) ? data : []))
      .catch((e) => setErr(e?.message || String(e)))
      .finally(() => setLoading(false));
  }, [API]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    setEntryContext((prev) => ({ ...(prev || {}), origin: "my_courses" }));
    return () => setEntryContext((prev) => (prev?.origin === "my_courses" ? null : prev));
  }, [setEntryContext]);

  const submitReview = async (id) => {
    await fetch(`${API}/api/teacher/courses/${id}/submit`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...authHeaders() },
    });
    load();
  };

  const removeCourse = async (id) => {
    if (!window.confirm("Xóa khóa học này?")) return;
    await fetch(`${API}/api/teacher/courses/${id}`, { method: "DELETE", headers: { ...authHeaders() } });
    load();
  };



  const editHref = (id) => `${API}/app/admin/teacher-new-course.html?id=${id}`;
  const createHref = `${API}/app/admin/teacher-new-course.html`;

  const filtered = useMemo(() => {
    if (!query) return items;
    const term = query.toLowerCase();
    return items.filter(
      (course) =>
        course.title?.toLowerCase().includes(term) ||
        course.slug?.toLowerCase().includes(term) ||
        course.approvalStatus?.toLowerCase().includes(term),
    );
  }, [items, query]);

  const stats = useMemo(() => {
    const total = items.length;
    const published = items.filter((c) => c.status === "published").length;
    const pending = items.filter((c) => c.approvalStatus === "pending").length;
    return { total, published, pending };
  }, [items]);

  return (
    <div className="bg-gradient-to-b from-white to-stone-50">
      <div className="mx-auto max-w-7xl px-4 py-12">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm uppercase tracking-[0.3em] text-stone-400">Studio giảng viên</p>
            <h1 className="mt-1 text-3xl font-extrabold text-stone-900">Khóa học của tôi</h1>
            <p className="text-sm text-stone-500">Theo dõi bản nháp, trạng thái duyệt và thao tác nhanh.</p>
          </div>
          <div className="flex flex-wrap gap-3">
            <input
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Tìm theo tiêu đề, slug, trạng thái..."
              className="w-64 rounded-full border border-stone-300 px-4 py-2 text-sm text-stone-700 placeholder-stone-400 focus:border-primary-500 focus:outline-none"
            />
            <a
              href={createHref}
              className="rounded-full bg-stone-900 px-5 py-2.5 text-sm font-semibold text-white shadow hover:bg-stone-800"
            >
              + Tạo khóa học
            </a>
          </div>
        </div>

        {err && <div className="mt-4 rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-600">{err}</div>}

        <div className="mt-10 grid gap-6 lg:grid-cols-[280px,1fr]">
          <section className="rounded-[32px] border border-stone-200 bg-white/90 p-6 shadow-sm">
            <p className="text-xs font-semibold uppercase tracking-[0.3em] text-stone-400">Thống kê</p>
            <h2 className="mt-2 text-4xl font-extrabold text-stone-900">{stats.total}</h2>
            <p className="text-sm text-stone-500">Khóa học đang quản lý</p>
            <dl className="mt-6 space-y-4 text-sm text-stone-600">
              <div className="flex items-center justify-between">
                <dt>Đã publish</dt>
                <dd className="font-semibold text-emerald-600">{stats.published}</dd>
              </div>
              <div className="flex items-center justify-between">
                <dt>Đang chờ duyệt</dt>
                <dd className="font-semibold text-amber-600">{stats.pending}</dd>
              </div>
              <div className="flex items-center justify-between">
                <dt>Bản nháp / chỉnh sửa</dt>
                <dd className="font-semibold text-stone-700">{stats.total - stats.published}</dd>
              </div>
            </dl>
            <a
              href={createHref}
              className="mt-8 inline-flex w-full items-center justify-center rounded-2xl border border-stone-200 bg-white px-4 py-2 text-sm font-semibold text-stone-800 shadow hover:border-stone-400"
            >
              Bắt đầu khóa mới
            </a>
          </section>

          <section className="rounded-[32px] border border-stone-200 bg-white/90 p-6 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-stone-100 pb-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.3em] text-stone-400">Danh sách</p>
                <h2 className="text-lg font-semibold text-stone-900">Bản nháp & trạng thái duyệt</h2>
              </div>
              <span className="text-sm text-stone-500">{filtered.length} khóa hiển thị</span>
            </div>

            {loading ? (
              <div className="space-y-4 py-6">
                {Array.from({ length: 4 }).map((_, idx) => (
                  <div key={idx} className="h-28 rounded-3xl bg-stone-100 animate-pulse" />
                ))}
              </div>
            ) : filtered.length === 0 ? (
              <EmptyState onCreate={() => (window.location.href = createHref)} />
            ) : (
              <div className="custom-scroll -mr-3 max-h-[70vh] overflow-y-auto pr-3 py-6">
                <div className="space-y-4">
                  {filtered.map((course) => (
                    <article
                      key={course.id}
                      className="group flex gap-4 rounded-3xl border border-stone-200 bg-white/80 p-4 shadow-sm transition hover:-translate-y-1 hover:border-primary-200 hover:shadow-lg"
                    >
                      <div className="h-20 w-28 flex-shrink-0 overflow-hidden rounded-2xl bg-stone-100">
                        {course.thumbnailUrl ? (
                          <img src={course.thumbnailUrl} alt={course.title} className="h-full w-full object-cover" />
                        ) : (
                          <img src={FALLBACK_THUMB} alt="Course cover" className="h-full w-full object-cover" />
                        )}
                      </div>
                      <div className="flex flex-1 flex-col gap-3">
                        <div className="flex flex-wrap items-start justify-between gap-3">
                          <div>
                            <p className="text-xs text-stone-400">#{course.id}</p>
                            <h3 className="text-lg font-semibold text-stone-900">{course.title}</h3>
                            <p className="text-xs text-stone-500">{course.slug}</p>
                          </div>
                          <div className="flex flex-col items-end gap-2 text-right text-xs text-stone-500">
                            <LevelChip value={course.level} />
                            <span>{String(course.createdAt).replace("T", " ")}</span>
                          </div>
                        </div>
                        <div className="flex flex-wrap items-center gap-2 text-sm">
                          <StatusBadge value={course.status} />
                          <div className="text-xs text-stone-500">
                            Duyệt: <StatusBadge value={course.approvalStatus} />
                          </div>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          <a
                            href={editHref(course.id)}
                            className="rounded-full border border-stone-200 px-4 py-1.5 text-sm font-semibold text-stone-700 transition hover:border-stone-400"
                          >
                            Chỉnh sửa
                          </a>
                          <button
                            type="button"
                            onClick={() => submitReview(course.id)}
                            className="rounded-full border border-primary-200 px-4 py-1.5 text-sm font-semibold text-primary-700 transition hover:border-primary-400"
                          >
                            Gửi duyệt
                          </button>
                          <button
                            type="button"
                            onClick={() => removeCourse(course.id)}
                            className="rounded-full border border-red-200 px-4 py-1.5 text-sm font-semibold text-red-600 transition hover:border-red-400"
                          >
                            Xóa
                          </button>
                        </div>
                      </div>
                    </article>
                  ))}
                </div>
              </div>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
