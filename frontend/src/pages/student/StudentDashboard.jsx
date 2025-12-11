import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { resolveIsFree } from "../../utils/price";
import { API_BASE_URL } from "../../api/httpClient";

const EXAM_LOAD_ERROR = "Không thể tải bài kiểm tra";

const coerceNumericId = (value) => {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim() !== "") {
    const parsed = Number(value.trim());
    if (Number.isFinite(parsed)) return parsed;
  }
  return null;
};

const selectPriceValue = (course) => {
  if (course?.price !== undefined && course?.price !== null) return course.price;
  if (course?.tuitionFee !== undefined && course?.tuitionFee !== null) return course.tuitionFee;
  if (course?.priceValue !== undefined && course?.priceValue !== null) return course.priceValue;
  if (course?.priceRaw !== undefined && course?.priceRaw !== null) return course.priceRaw;
  return null;
};

const formatPriceLabel = (price, isFree) => {
  if (isFree) return "Miễn phí";
  if (price === null || price === undefined) return "Đang cập nhật";
  if (typeof price === "number" && Number.isFinite(price)) {
    return `${price.toLocaleString("vi-VN")} đ`;
  }
  const parsed = Number(price);
  if (Number.isFinite(parsed)) {
    return `${parsed.toLocaleString("vi-VN")} đ`;
  }
  return price;
};

const toCourseViewModel = (course) => {
  const courseId = coerceNumericId(course?.courseId ?? course?.id);
  const slug = course?.slug ?? course?.courseSlug ?? null;
  const priceValue = selectPriceValue(course);
  const isFree = resolveIsFree(priceValue, course?.is_free ?? course?.isFree);
  return {
    ...course,
    courseId,
    slug,
    isFree,
    priceLabel: formatPriceLabel(priceValue, isFree),
  };
};

export default function StudentDashboard() {
  const API = API_BASE_URL;
  const navigate = useNavigate();
  const [me, setMe] = useState(null);
  const [courses, setCourses] = useState([]);
  const [certs, setCerts] = useState([]);
  const [error, setError] = useState("");
  const [examModal, setExamModal] = useState({ open: false, loading: false, exams: [], course: null, error: "" });
  const [progressMap, setProgressMap] = useState({});

  const token = useMemo(() => localStorage.getItem("token"), []);
  const headers = useMemo(() => (token ? { "Content-Type": "application/json", Authorization: `Bearer ${token}` } : null), [token]);

  useEffect(() => {
    if (!token) {
      window.location.href = "/login";
      return;
    }

    fetch(`${API}/api/student/me`, { headers })
      .then((r) => (r.ok ? r.json() : null))
      .then((data) => data && setMe(data))
      .catch(() => {});

    fetch(`${API}/api/student/enrollments`, { headers })
      .then((r) => {
        if (!r.ok) throw new Error("Không thể tải danh sách khóa học");
        return r.json();
      })
      .then((data) => {
        const normalized = Array.isArray(data) ? data.map(toCourseViewModel) : [];
        setCourses(normalized);
      })
      .catch((e) => setError(e?.message || "Không thể tải danh sách khóa học"));

    fetch(`${API}/api/student/exams/certificates`, { headers })
      .then((r) => (r.ok ? r.json() : []))
      .then((data) => setCerts(Array.isArray(data) ? data : []))
      .catch(() => setCerts([]));
  }, [API, token, headers]);

  useEffect(() => {
    if (!headers) {
      setProgressMap({});
      return;
    }
    const coursesWithId = courses.filter((course) => Number.isFinite(course.courseId));
    if (coursesWithId.length === 0) {
      setProgressMap({});
      return;
    }
    let cancelled = false;

    const loadProgress = async () => {
      const entries = await Promise.all(
        coursesWithId.map(async (course) => {
          try {
            const res = await fetch(`${API}/api/student/progress/courses/${course.courseId}`, { headers });
            if (!res.ok) return null;
            const data = await res.json();
            return [
              course.courseId,
              {
                completed: data?.completedLessons ?? 0,
                total: data?.totalLessons ?? 0,
                percent: data?.completionPercent ?? 0,
              },
            ];
          } catch {
            return null;
          }
        }),
      );
      if (!cancelled) {
        const next = {};
        entries.forEach((entry) => {
          if (entry && entry[0]) {
            next[entry[0]] = entry[1];
          }
        });
        setProgressMap(next);
      }
    };

    loadProgress();
    return () => {
      cancelled = true;
    };
  }, [API, headers, courses]);

  const proCourses = useMemo(() => courses.filter((course) => !course.isFree), [courses]);
  const freeCourses = useMemo(() => courses.filter((course) => course.isFree), [courses]);

  const openExamModal = async (course) => {
    if (!headers) return;
    if (!course?.courseId) {
      setExamModal({ open: true, loading: false, exams: [], course, error: "Khoá học chưa có mã hợp lệ để tải bài kiểm tra." });
      return;
    }
    setExamModal({ open: true, loading: true, exams: [], course, error: "" });
    try {
      const res = await fetch(`${API}/api/student/exams/courses/${course.courseId}`, { headers });
      if (!res.ok) throw new Error(EXAM_LOAD_ERROR);
      const data = await res.json();
      if (!Array.isArray(data) || data.length === 0) {
        setExamModal({ open: false, loading: false, exams: [], course: null, error: "" });
        navigate(`/learn/${course.courseId}`);
        return;
      }
      setExamModal((prev) => ({ ...prev, loading: false, exams: data }));
    } catch (err) {
      setExamModal((prev) => ({ ...prev, loading: false, error: err?.message || EXAM_LOAD_ERROR }));
    }
  };

  const closeExamModal = () => setExamModal({ open: false, loading: false, exams: [], course: null, error: "" });

  const renderCourseCard = (course) => {
    const key = course.courseId ?? course.slug ?? course.title;
    const progress = course.courseId ? progressMap[course.courseId] : null;
    const badgeStyles = course.isFree
      ? { badge: "bg-emerald-50 text-emerald-700", chip: "bg-emerald-500 text-white", gradient: "from-emerald-300 via-emerald-400 to-emerald-500" }
      : { badge: "bg-indigo-50 text-indigo-700", chip: "bg-[#f97316] text-white", gradient: "from-indigo-400 via-indigo-500 to-purple-500" };
    const imageSrc =
      course.thumbnailUrl && typeof course.thumbnailUrl === "string"
        ? course.thumbnailUrl.startsWith("http") || course.thumbnailUrl.startsWith("/") || course.thumbnailUrl.startsWith("data:")
          ? course.thumbnailUrl.startsWith("/") && !course.thumbnailUrl.startsWith("//")
            ? `${API}${course.thumbnailUrl}`
            : course.thumbnailUrl
          : `${API}/${course.thumbnailUrl}`
        : null;
    const safePercent = Math.min(Math.max(progress?.percent ?? 0, 0), 100);
    const progressLabel = progress
      ? `Tiến độ: ${progress.completed}/${progress.total} bài (${Math.round(safePercent)}%)`
      : course.courseId
      ? "Đang tải tiến độ..."
      : "Chưa hỗ trợ thống kê tiến độ";

    return (
      <article
        key={key}
        className="group flex h-full flex-col overflow-hidden rounded-[28px] border border-white/80 bg-white/90 shadow-[0_35px_65px_rgba(15,23,42,0.1)] transition duration-300 hover:-translate-y-2 hover:shadow-[0_45px_85px_rgba(15,23,42,0.25)]"
      >
        <div className={`relative w-full bg-gradient-to-br ${badgeStyles.gradient}`}>
          <div
            role="button"
            tabIndex={0}
            onClick={() => course.courseId && navigate(`/learn/${course.courseId}`)}
            onKeyDown={(evt) => {
              if (evt.key === "Enter" && course.courseId) navigate(`/learn/${course.courseId}`);
            }}
            className={`relative block aspect-[16/10] w-full cursor-pointer overflow-hidden ${course.courseId ? "focus:outline-none focus-visible:ring-2 focus-visible:ring-white/80" : ""}`}
          >
            {imageSrc ? (
              <img src={imageSrc} alt={course.title} className="h-full w-full object-cover opacity-90 transition duration-300 group-hover:scale-105" />
            ) : (
              <div className="flex h-full w-full items-center justify-center text-sm text-white/80">Chưa có ảnh</div>
            )}
            <span className={`absolute left-4 top-4 rounded-full px-3 py-1 text-xs font-semibold ${badgeStyles.badge}`}>{course.level || "Tổng quát"}</span>
            <span className="absolute right-4 top-4 rounded-full bg-black/40 px-3 py-1 text-xs font-semibold text-white">{course.status || "Đang học"}</span>
          </div>
        </div>
        <div className="flex flex-1 flex-col gap-4 px-5 pb-5 pt-4">
          <div className="flex items-center justify-between gap-3">
            <span className={`rounded-full px-4 py-1 text-xs font-semibold uppercase tracking-wide ${badgeStyles.chip}`}>
              {course.isFree ? "Miễn phí" : "Pro"}
            </span>
            <span className="text-sm font-semibold text-[#8b5e3c]">{course.priceLabel}</span>
          </div>
          <div>
            <h3 className="text-lg font-semibold text-stone-900">{course.title}</h3>
            <p className="mt-1 text-sm text-stone-500 line-clamp-2">{course.subtitle || course.shortDescription || "Học linh hoạt cùng mentor đồng hành"}</p>
          </div>
          <div className="rounded-2xl bg-stone-50 p-3">
            <div className="flex items-center justify-between text-xs font-medium text-stone-500">
              <span>{progressLabel}</span>
              <span>{safePercent}%</span>
            </div>
            <div className="mt-2 h-2 rounded-full bg-white">
              <div className="h-full rounded-full bg-gradient-to-r from-[#f97316] to-[#facc15]" style={{ width: `${safePercent}%` }} />
            </div>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <button
              type="button"
              className="flex-1 rounded-2xl border border-stone-200 px-4 py-2 text-sm font-semibold text-stone-600 transition hover:border-stone-400"
              onClick={() => course.courseId && navigate(`/learn/${course.courseId}`)}
              disabled={!course.courseId}
            >
              Vào lớp
            </button>
            <button
              type="button"
              className="flex-1 rounded-2xl bg-[#b4693d] px-4 py-2 text-sm font-semibold text-white shadow-lg transition hover:bg-[#9d5427] disabled:cursor-not-allowed disabled:opacity-60"
              onClick={() => openExamModal(course)}
              disabled={!course.courseId}
            >
              Tiếp tục học
            </button>
          </div>
        </div>
      </article>
    );
  };

  const renderSection = (title, subtitle, items, emptyMessage) => (
    <section key={title} className="mt-12">
      <div className="flex flex-col gap-1 rounded-3xl bg-white/80 px-6 py-5 shadow-[0_18px_45px_rgba(15,23,42,0.08)] sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.3em] text-stone-400">Danh mục</p>
          <h2 className="text-2xl font-semibold text-stone-900">{title}</h2>
          <p className="text-sm text-stone-500">{subtitle}</p>
        </div>
        {!!items.length && <span className="rounded-full bg-stone-100 px-4 py-2 text-sm font-semibold text-stone-600">{items.length} khóa học</span>}
      </div>
      {items.length === 0 ? (
        <p className="mt-4 text-sm text-stone-500">{emptyMessage}</p>
      ) : (
        <div className="mt-6 grid gap-8 lg:grid-cols-2 xl:grid-cols-3">{items.map((course) => renderCourseCard(course))}</div>
      )}
    </section>
  );

  const renderCertificates = () => {
    if (!certs.length) return null;
    return (
      <section className="mt-12">
        <div className="rounded-3xl bg-white/80 px-6 py-5 shadow-[0_18px_45px_rgba(15,23,42,0.08)]">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-xs uppercase tracking-[0.3em] text-stone-400">Chứng nhận</p>
              <h2 className="text-2xl font-semibold text-stone-900">Tín chỉ đã nhận</h2>
              <p className="text-sm text-stone-500">Hiển thị các khóa học bạn đã hoàn thành.</p>
            </div>
            <span className="rounded-full bg-stone-100 px-4 py-2 text-sm font-semibold text-stone-600">{certs.length} chứng nhận</span>
          </div>
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            {certs.map((c) => (
              <div key={c.id || `${c.courseId}-${c.attemptId}`} className="flex items-center justify-between rounded-2xl border border-stone-200 bg-white p-4 shadow-sm">
                <div>
                  <p className="text-sm font-semibold text-stone-900">{c.courseTitle || `Khóa #${c.courseId}`}</p>
                  <p className="text-xs text-stone-500">
                    Điểm: {c.scorePercent != null ? Math.round(c.scorePercent) + "%" : "Đã hoàn thành"} ·{" "}
                    {c.issuedAt ? new Date(c.issuedAt).toLocaleDateString("vi-VN") : "Đã cấp"}
                  </p>
                </div>
                {c.courseId && (
                  <button className="btn btn-primary btn-sm" onClick={() => navigate(`/learn/${c.courseId}`)}>
                    Xem khóa
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      </section>
    );
  };

  return (
    <div className="min-h-screen bg-[#f8f5f2] px-4 py-10">
      <div className="mx-auto max-w-6xl">
        <section className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-[#ffecd2] via-[#fcb69f] to-[#ffecd2] px-8 py-10 shadow-[0_25px_65px_rgba(251,146,60,0.25)]">
          <div className="relative z-10 flex flex-col gap-4 text-stone-800 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-sm uppercase tracking-[0.2em] text-[#b4693d]">Learner Dashboard</p>
              <h1 className="mt-1 text-3xl font-bold text-[#6b3e2e]">Xin chào, {me?.fullName || "học viên"} 👋</h1>
              <p className="mt-2 max-w-2xl text-sm text-[#7a5240]">
                Theo dõi tiến độ học tập, khám phá các khóa học Pro và miễn phí bạn đã ghi danh. Mọi tính năng vẫn giữ nguyên, bạn có thể tiếp tục học ngay.
              </p>
            </div>
            <div className="rounded-2xl bg-white/80 px-6 py-5 text-center shadow-lg backdrop-blur">
              <p className="text-xs uppercase tracking-widest text-stone-400">Tổng số khóa</p>
              <p className="text-4xl font-bold text-[#b4693d]">{courses.length}</p>
              <p className="text-xs text-stone-500">Đang theo học</p>
            </div>
          </div>
          <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top,_rgba(255,255,255,0.45),_transparent_55%)]" />
        </section>

        <div className="mt-8 flex flex-wrap gap-3 rounded-2xl border border-white/70 bg-white/70 p-4 shadow-sm backdrop-blur">
          <div className="flex flex-1 items-center gap-3 rounded-2xl bg-white px-4 py-3 shadow-sm">
            <span className="rounded-full bg-[#ffe8d6] px-3 py-1 text-xs font-semibold text-[#b2683d]">Tài khoản</span>
            <div className="text-sm text-stone-600">
              <p className="font-semibold text-stone-800">{me?.fullName || "Chưa cập nhật"}</p>
              <p>{me?.email || "-"}</p>
            </div>
          </div>
          <div className="flex flex-1 items-center gap-3 rounded-2xl bg-white px-4 py-3 shadow-sm">
            <span className="rounded-full bg-[#e0f2f1] px-3 py-1 text-xs font-semibold text-[#00796b]">Vai trò</span>
            <p className="text-sm text-stone-600">{(me?.roles || []).join(", ") || "Student"}</p>
          </div>
          <div className="flex flex-1 items-center justify-between rounded-2xl bg-white px-4 py-3 shadow-sm">
            <div>
              <p className="text-xs uppercase tracking-widest text-stone-400">Thông báo</p>
              {error ? (
                <p className="text-sm font-semibold text-red-500">{error}</p>
              ) : (
                <p className="text-sm text-stone-600">Bạn có {courses.length} khóa đang học</p>
              )}
            </div>
            <button
              type="button"
              className="rounded-full border border-stone-200 px-4 py-2 text-xs font-semibold text-stone-600 hover:border-stone-400"
              onClick={() => window.scrollTo({ top: document.body.scrollHeight, behavior: "smooth" })}
            >
              Xem tất cả
            </button>
          </div>
        </div>

        <div className="mt-10 text-stone-500">
          <p className="text-sm font-medium uppercase tracking-widest">Gợi ý</p>
          <div className="mt-2 flex flex-wrap items-center gap-3 rounded-2xl bg-white px-4 py-3 text-xs font-semibold uppercase text-stone-500 shadow-sm">
            <span className="rounded-full border border-stone-200 px-4 py-2 text-stone-700">Sắp xếp theo cấp độ</span>
            <span className="rounded-full border border-stone-200 px-4 py-2 text-stone-700">Tiến độ cao nhất</span>
            <span className="rounded-full border border-stone-200 px-4 py-2 text-stone-700">Đề xuất cho bạn</span>
          </div>
        </div>

        {renderSection(
          "Khóa học Pro",
          "Các khóa học Pro với mentor đồng hành và nội dung chuyên sâu.",
          proCourses,
          "Bạn chưa sở hữu khóa học Pro nào."
        )}

        {renderSection("Khóa học miễn phí", "Tổng hợp khóa miễn phí giúp bạn khởi động nhanh.", freeCourses, "Bạn chưa tham gia khóa miễn phí nào.")}

        {renderCertificates()}

        {examModal.open && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4 py-8">
            <div className="relative w-full max-w-3xl rounded-2xl bg-white p-6 shadow-2xl">
              <button className="absolute right-3 top-3 text-2xl text-stone-400 hover:text-stone-600" onClick={closeExamModal} aria-label="Đóng">
                x
              </button>
              <h2 className="text-xl font-semibold text-stone-900">Bài kiểm tra - {examModal.course?.title || `#${examModal.course?.courseId}`}</h2>
              {examModal.loading && <p className="mt-4 text-sm text-stone-500">Đang tải danh sách bài kiểm tra...</p>}
              {examModal.error && <p className="mt-4 text-sm text-red-600">{examModal.error}</p>}
              {!examModal.loading && examModal.exams.length === 0 && !examModal.error && (
                <div className="mt-4 space-y-4 text-sm text-stone-600">
                  <p>Khóa học này chưa có bài kiểm tra. Bạn có thể tiếp tục xem bài học.</p>
                  <button
                    className="btn btn-primary"
                    onClick={() => {
                      const cId = examModal.course?.courseId;
                      closeExamModal();
                      if (cId) navigate(`/learn/${cId}`);
                    }}
                  >
                    Vào lớp học
                  </button>
                </div>
              )}
              {examModal.exams.length > 0 && (
                <div className="mt-4 space-y-3">
                  {examModal.exams.map((exam) => {
                    const cId = examModal.course?.courseId;
                    return (
                      <div key={exam.id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-stone-200 p-4">
                        <div>
                          <p className="font-semibold text-stone-900">{exam.title || `Bài kiểm tra #${exam.id}`}</p>
                          <p className="text-sm text-stone-500">
                            {exam.questionCount ?? 0} câu · {exam.timeLimitSec ? `${Math.ceil(exam.timeLimitSec / 60)} phút` : "Không giới hạn thời gian"} · Điểm đạt {exam.passingScore ?? 0}%
                          </p>
                        </div>
                        <div className="flex gap-3">
                          <button
                            className="btn border-stone-300 disabled:cursor-not-allowed disabled:opacity-60"
                            disabled={!cId}
                            onClick={() => cId && window.open(`/courses/${cId}/exams/${exam.id}`, "_self")}
                          >
                            Làm bài
                          </button>
                          <button
                            className="btn btn-primary disabled:cursor-not-allowed disabled:opacity-60"
                            disabled={!cId}
                            onClick={() => {
                              closeExamModal();
                              if (cId) navigate(`/learn/${cId}`);
                            }}
                          >
                            Vào lớp
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
