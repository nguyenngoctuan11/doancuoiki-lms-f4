import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { API_BASE_URL } from "../api/httpClient";

const API_BASE = API_BASE_URL;
const FALLBACK_AVATAR =
  "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=400&q=60";

function resolveAssetUrl(value) {
  if (!value) return "";
  const raw = String(value).trim();
  if (!raw) return "";
  if (/^(?:https?:|data:|blob:)/i.test(raw)) return raw;
  const normalized = raw.startsWith("/") ? raw : `/${raw}`;
  return `${API_BASE}${normalized}`;
}

function CourseCard({ course }) {
  return (
    <Link
      to={course.slug ? `/courses/${course.slug}` : `/courses/${course.id}`}
      className="flex gap-4 rounded-3xl border border-stone-200 bg-white/90 p-4 shadow-sm transition hover:-translate-y-1 hover:border-primary-200"
    >
      <div className="h-16 w-24 flex-shrink-0 overflow-hidden rounded-2xl bg-stone-100">
        {course.thumbnailUrl ? (
          <img src={resolveAssetUrl(course.thumbnailUrl)} alt={course.title} className="h-full w-full object-cover" />
        ) : (
          <div className="h-full w-full bg-gradient-to-br from-white to-stone-200" />
        )}
      </div>
      <div className="space-y-1">
        <p className="text-sm font-semibold text-stone-900">{course.title}</p>
        <p className="text-xs text-stone-500">{course.level || "Tổng hợp"}</p>
      </div>
    </Link>
  );
}

export default function Mentors() {
  const [mentors, setMentors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [selectedId, setSelectedId] = useState(null);
  const [profile, setProfile] = useState({ loading: false, data: null, error: "" });

  useEffect(() => {
    let alive = true;
    setLoading(true);
    setError("");
    fetch(`${API_BASE}/api/public/teachers?limit=24`)
      .then((res) => {
        if (!res.ok) throw new Error("Không thể tải danh sách mentor");
        return res.json();
      })
      .then((data) => {
        if (!alive) return;
        const list = Array.isArray(data) ? data : [];
        setMentors(list);
        if (list.length > 0) {
          openProfile(list[0].id);
        }
      })
      .catch((err) => {
        if (!alive) return;
        setError(err?.message || "Không thể tải danh sách mentor");
        setMentors([]);
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => {
      alive = false;
    };
  }, []);

  const filtered = useMemo(() => {
    if (!query) return mentors;
    const term = query.toLowerCase();
    return mentors.filter((mentor) => (mentor.fullName || "").toLowerCase().includes(term));
  }, [mentors, query]);

  const openProfile = (mentorId) => {
    setSelectedId(mentorId);
    setProfile({ loading: true, data: null, error: "" });
    fetch(`${API_BASE}/api/public/teachers/${mentorId}`)
      .then((res) => {
        if (!res.ok) throw new Error("Không thể tải thông tin mentor");
        return res.json();
      })
      .then((data) => {
        setProfile({ loading: false, data, error: "" });
      })
      .catch((err) => {
        setProfile({ loading: false, data: null, error: err?.message || "Không thể tải thông tin mentor" });
      });
  };

  const current = profile.data;

  return (
    <div className="min-h-screen bg-[#f8f5f2]">
      <div className="mx-auto max-w-6xl px-4 py-10">
        <section className="relative overflow-hidden rounded-[36px] bg-gradient-to-r from-[#ffecd2] via-[#fcb69f] to-[#ffecd2] px-8 py-10 shadow-[0_35px_90px_rgba(251,146,60,0.25)]">
          <div className="relative z-10 flex flex-col gap-6 text-[#6b3e2e] lg:flex-row lg:items-center lg:justify-between">
            <div>
              <p className="text-sm uppercase tracking-[0.3em] text-[#b76e42]">Mentor studio</p>
              <h1 className="mt-1 text-3xl font-bold">Đội ngũ mentor đồng hành</h1>
              <p className="mt-2 max-w-2xl text-sm text-[#7a5240]">
                Chuyên gia từ doanh nghiệp, giảng viên dày dạn kinh nghiệm và mentor cá nhân hóa lộ trình cho bạn. Chọn
                mentor và khám phá những khóa học tiêu biểu của họ.
              </p>
            </div>
            <div className="rounded-3xl bg-white/90 px-6 py-5 text-center shadow-lg">
              <p className="text-xs uppercase tracking-widest text-stone-400">Số mentor</p>
              <p className="text-4xl font-bold text-[#b4693d]">{mentors.length}</p>
              <p className="text-xs text-stone-500">đang truyền cảm hứng</p>
            </div>
          </div>
          <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top,_rgba(255,255,255,0.5),_transparent_55%)]" />
        </section>

        <div className="mt-8 grid gap-6 lg:grid-cols-[320px,1fr]">
          <section className="rounded-[32px] border border-stone-200 bg-white/90 p-6 shadow-sm">
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-xs uppercase tracking-[0.3em] text-stone-400">Danh sách mentor</p>
                <h2 className="text-lg font-semibold text-stone-900">Chọn mentor để xem khóa học</h2>
              </div>
              <span className="text-sm text-stone-500">{filtered.length} mentor</span>
            </div>
            <input
              className="mt-4 w-full rounded-full border border-stone-300 px-4 py-2 text-sm text-stone-700 focus:border-primary-500 focus:outline-none"
              placeholder="Tìm mentor theo tên..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
            {error && <p className="mt-4 rounded-xl bg-red-50 px-4 py-2 text-sm text-red-600">{error}</p>}
            <div className="custom-scroll mt-4 max-h-[70vh] overflow-y-auto pr-3">
              {loading
                ? Array.from({ length: 6 }).map((_, idx) => (
                    <div key={idx} className="mb-3 h-20 animate-pulse rounded-3xl bg-stone-100" />
                  ))
                : filtered.map((mentor) => (
                    <button
                      key={mentor.id}
                      type="button"
                      onClick={() => openProfile(mentor.id)}
                      className={`mb-3 flex w-full items-center gap-4 rounded-3xl border p-4 text-left shadow-sm transition hover:-translate-y-1 hover:border-primary-200 ${
                        selectedId === mentor.id
                          ? "border-primary-200 bg-primary-50"
                          : "border-stone-200 bg-white text-stone-700"
                      }`}
                    >
                      <div className="h-16 w-16 flex-shrink-0 overflow-hidden rounded-full bg-stone-100">
                        <img
                          src={resolveAssetUrl(mentor.avatarUrl) || FALLBACK_AVATAR}
                          alt={mentor.fullName}
                          className="h-full w-full object-cover"
                        />
                      </div>
                      <div className="flex-1">
                        <p className="text-base font-semibold text-stone-900">{mentor.fullName}</p>
                        <p className="text-xs text-stone-500">
                          {mentor.courseCount} khóa • {mentor.lessonCount} bài học
                        </p>
                        {mentor.bio && <p className="mt-1 text-xs text-stone-500 line-clamp-2">{mentor.bio}</p>}
                      </div>
                    </button>
                  ))}
              {!loading && filtered.length === 0 && (
                <p className="py-6 text-center text-sm text-stone-500">Không tìm thấy mentor phù hợp.</p>
              )}
            </div>
          </section>

          <section className="rounded-[32px] border border-stone-200 bg-white p-6 shadow-sm">
            {profile.loading ? (
              <p className="py-20 text-center text-sm text-stone-500">Đang tải hồ sơ mentor...</p>
            ) : profile.error ? (
              <p className="py-20 text-center text-sm text-red-600">{profile.error}</p>
            ) : current ? (
              <div className="flex flex-col gap-6">
                <div className="flex flex-wrap items-center gap-6">
                  <div className="h-24 w-24 overflow-hidden rounded-3xl border border-stone-100 shadow">
                    <img
                      src={resolveAssetUrl(current.avatarUrl) || FALLBACK_AVATAR}
                      alt={current.fullName}
                      className="h-full w-full object-cover"
                    />
                  </div>
                  <div className="flex-1">
                    <p className="text-xs uppercase tracking-[0.3em] text-stone-400">Hồ sơ mentor</p>
                    <h2 className="text-2xl font-semibold text-stone-900">{current.fullName}</h2>
                    <p className="text-sm text-stone-600">{current.expertise || "Mentor tiếng Anh"}</p>
                    {current.bio && <p className="mt-2 text-sm text-stone-500">{current.bio}</p>}
                    <div className="mt-3 flex flex-wrap gap-3 text-xs font-semibold text-stone-500">
                      <span className="rounded-full border border-stone-200 px-3 py-1">
                        {current.courseCount} khóa đang dạy
                      </span>
                      <span className="rounded-full border border-stone-200 px-3 py-1">
                        {current.lessonCount} bài học
                      </span>
                    </div>
                  </div>
                </div>
                <div className="flex items-center justify-between border-t border-stone-100 pt-4">
                  <div>
                    <p className="text-xs uppercase tracking-[0.3em] text-stone-400">Khoá học tiêu biểu</p>
                    <h3 className="text-lg font-semibold text-stone-900">Những lớp học mang dấu ấn mentor</h3>
                  </div>
                  <Link to="/courses" className="text-sm font-semibold text-primary-600">
                    Xem tất cả
                  </Link>
                </div>
                {current.courses && current.courses.length > 0 ? (
                  <div className="custom-scroll max-h-[60vh] space-y-4 overflow-y-auto pr-2">
                    {current.courses.map((course) => (
                      <CourseCard key={course.id} course={course} />
                    ))}
                  </div>
                ) : (
                  <p className="rounded-2xl bg-stone-50 px-4 py-8 text-center text-sm text-stone-500">
                    Mentor chưa có khóa học công khai.
                  </p>
                )}
              </div>
            ) : (
              <div className="py-20 text-center text-sm text-stone-500">
                Chọn một mentor để xem chi tiết khóa học.
              </div>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
