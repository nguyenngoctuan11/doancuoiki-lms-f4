/* eslint-disable */
import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import SectionHeading from "../components/SectionHeading";
import CourseReviewsPanel from "../components/CourseReviewsPanel";
import { normalizePriceValue, resolveIsFree } from "../utils/price";
import httpClient, { API_BASE_URL } from "../api/httpClient";
import { useAuth } from "../context/AuthContext";
import { useSupportChat } from "../context/SupportChatContext";

const API_BASE = API_BASE_URL;
const moneyFormatter = new Intl.NumberFormat("vi-VN");

function formatMoney(value) {
  const num = normalizePriceValue(value);
  if (num === null) return null;
  return `${moneyFormatter.format(Math.round(num))}đ`;
}

function secondsToLabel(seconds) {
  if (!seconds) return null;
  const total = Number(seconds);
  if (!Number.isFinite(total) || total <= 0) return null;
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  if (hours === 0) return `${minutes} phút`;
  if (minutes === 0) return `${hours} giờ`;
  return `${hours} giờ ${minutes} phút`;
}

function normalize(str) {
  return (str || "").toString().normalize("NFD").replace(/\p{Diacritic}/gu, "").toLowerCase().trim();
}

function levelLabel(lv) {
  const s = normalize(lv);
  if (["co ban", "beginner", "basic", "foundation"].includes(s)) return "Cơ bản";
  if (["trung cap", "intermediate", "middle", "medium"].includes(s)) return "Trung cấp";
  if (["nang cao", "advanced", "expert"].includes(s)) return "Nâng cao";
  return lv || "Tổng hợp";
}

function resolveAssetUrl(u) {
  if (!u) return null;
  let s = String(u).trim().replace(/\\/g, "/");
  if (!s) return null;
  if (/^https?:\/\//i.test(s) || s.startsWith("data:") || s.startsWith("blob:")) return s;
  if (!s.startsWith("/")) s = `/${s}`;
  return `${API_BASE}${s}`;
}

function resolveThumb(u) {
  return resolveAssetUrl(u);
}

function parseTimeLike(value) {
  if (value == null) return null;
  if (typeof value === "number") {
    const num = Number(value);
    return Number.isFinite(num) ? num : null;
  }
  const str = String(value).trim();
  if (!str) return null;
  if (/^\d+$/.test(str)) {
    const num = Number(str);
    return Number.isFinite(num) ? num : null;
  }
  if (str.includes(":")) {
    const parts = str.split(":").map((part) => Number(part));
    if (parts.every((n) => Number.isFinite(n))) {
      if (parts.length === 3) {
        return parts[0] * 3600 + parts[1] * 60 + parts[2];
      }
      if (parts.length === 2) {
        return parts[0] * 60 + parts[1];
      }
      if (parts.length === 1) {
        return parts[0];
      }
    }
  }
  return null;
}

function extractLessonDurationSeconds(lesson) {
  if (!lesson) return 0;
  const secondCandidates = [
    lesson.durationSeconds,
    lesson.duration_seconds,
    lesson.durationInSeconds,
    lesson.video_duration,
    lesson.videoDuration,
    lesson.total_seconds,
    lesson.seconds,
    lesson.duration,
  ];
  for (const candidate of secondCandidates) {
    const parsed = parseTimeLike(candidate);
    if (parsed && parsed > 0) return parsed;
  }
  const minuteCandidates = [lesson.duration_minutes, lesson.durationMinutes, lesson.minutes];
  for (const candidate of minuteCandidates) {
    const parsed = parseTimeLike(candidate);
    if (parsed && parsed > 0) return parsed * 60;
  }
  return 0;
}

function formatLessonDurationLabel(seconds) {
  const total = Number(seconds);
  if (!Number.isFinite(total) || total <= 0) return null;
  if (total < 60) return `${Math.round(total)}s`;
  const minutes = Math.floor(total / 60);
  const remainSeconds = total % 60;
  if (minutes < 60) {
    return remainSeconds ? `${minutes}p${String(Math.round(remainSeconds)).padStart(2, "0")}s` : `${minutes} phút`;
  }
  const hours = Math.floor(minutes / 60);
  const remainMinutes = minutes % 60;
  return remainMinutes ? `${hours}h ${remainMinutes}p` : `${hours}h`;
}

function stripHtmlTags(value) {
  if (!value) return "";
  return String(value).replace(/<[^>]*>/g, "").trim();
}

function splitDescriptionParagraphs(value) {
  if (!value) return [];
  const normalized = String(value)
    .replace(/<\s*br\s*\/?>/gi, "\n")
    .replace(/<\/p>/gi, "</p>\n");
  const plain = stripHtmlTags(normalized);
  return plain
    .split(/\n+/)
    .map((chunk) => chunk.trim())
    .filter(Boolean);
}

function normalizeModuleList(modules) {
  if (!Array.isArray(modules)) return [];
  return modules.map((module, mIdx) => {
    const lessonsRaw = Array.isArray(module.lessons) ? module.lessons : module.items ?? [];
    const lessons = lessonsRaw.map((lesson, lIdx) => {
      const durationSeconds = extractLessonDurationSeconds(lesson);
      const rawVideo =
        lesson.video_url ?? lesson.videoUrl ?? lesson.video ?? lesson.video_link ?? lesson.videoLink ?? null;
      return {
        ...lesson,
        id: lesson.id ?? lesson.lesson_id ?? `lesson-${mIdx}-${lIdx}`,
        title: lesson.title ?? lesson.name ?? `B?i ${lIdx + 1}`,
        description: stripHtmlTags(lesson.description ?? lesson.summary ?? lesson.sub_title ?? lesson.subtitle ?? ""),
        content: stripHtmlTags(lesson.content ?? lesson.details ?? ""),
        durationSeconds,
        durationLabel: formatLessonDurationLabel(durationSeconds),
        video_url: resolveAssetUrl(rawVideo),
      };
    });
    const durationSeconds = lessons.reduce((sum, lesson) => sum + (lesson.durationSeconds || 0), 0);
    return {
      ...module,
      id: module.id ?? module.module_id ?? module.chapter_id ?? `module-${mIdx}`,
      title: module.title ?? module.name ?? module.chapter_title ?? `Chương ${mIdx + 1}`,
      lessons,
      durationSeconds,
    };
  });
}

function summarizeModules(modules) {
  let totalLessons = 0;
  let totalSeconds = 0;
  let firstLessonWithVideo = null;
  for (const module of modules || []) {
    const lessons = Array.isArray(module.lessons) ? module.lessons : [];
    totalLessons += lessons.length;
    for (const lesson of lessons) {
      const seconds = extractLessonDurationSeconds(lesson);
      if (Number.isFinite(seconds)) totalSeconds += seconds;
      const rawVideo =
        lesson.video_url ?? lesson.videoUrl ?? lesson.video ?? lesson.video_link ?? lesson.videoLink ?? null;
      const videoUrl = resolveAssetUrl(rawVideo);
      if (!firstLessonWithVideo && videoUrl) {
        firstLessonWithVideo = { ...lesson, video_url: videoUrl };
      }
    }
  }
  return { totalLessons, totalSeconds, firstLessonWithVideo };
}

function formatFileSize(bytes) {
  if (bytes == null) return null;
  const num = Number(bytes);
  if (!Number.isFinite(num) || num <= 0) return null;
  const units = ["B", "KB", "MB", "GB", "TB"];
  let idx = 0;
  let current = num;
  while (current >= 1024 && idx < units.length - 1) {
    current /= 1024;
    idx += 1;
  }
  const value = idx === 0 ? Math.round(current) : current.toFixed(1);
  return `${value} ${units[idx]}`;
}

function resolveResourceUrl(resource) {
  if (!resource) return null;
  const candidate = resource.externalUrl || resource.fileUrl || resource.downloadUrl;
  if (!candidate) return null;
  if (/^https?:\/\//i.test(candidate) || candidate.startsWith("data:")) {
    return candidate;
  }
  if (candidate.startsWith("/")) return `${API_BASE}${candidate}`;
  return `${API_BASE}/${candidate}`;
}

export default function CourseDetail() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showPreview, setShowPreview] = useState(false);
  const previewRef = useRef(null);
  const [enrolling, setEnrolling] = useState(false);
  const [enrolled, setEnrolled] = useState(false);
  const [enrollError, setEnrollError] = useState("");
  const [syllabus, setSyllabus] = useState([]);
  const [syllabusLoading, setSyllabusLoading] = useState(false);
  const [syllabusError, setSyllabusError] = useState("");
  const [publicResources, setPublicResources] = useState([]);
  const [resourceLoading, setResourceLoading] = useState(false);
  const [resourceError, setResourceError] = useState("");
  const { openChat, setEntryContext } = useSupportChat();

  useEffect(() => {
    let alive = true;
    async function load() {
      setLoading(true);
      setError("");
      const paths = [
        `/api/public/courses/${encodeURIComponent(slug)}/detail-sql`,
        `/api/public/courses/${encodeURIComponent(slug)}/detail`,
        `/api/public/course/${encodeURIComponent(slug)}`,
        `/api/public/courses/detail/${encodeURIComponent(slug)}`,
        `/api/public/courses/detail-sql/${encodeURIComponent(slug)}`,
      ];
      let found = null;
      for (const p of paths) {
        try {
          const r = await fetch(API_BASE + p, { headers: { Accept: "application/json" } });
          if (!r.ok) continue;
          const j = await r.json();
          if (j) {
            found = j;
            break;
          }
        } catch (e) {}
      }
      if (!found) {
        try {
          const r = await fetch(API_BASE + "/api/public/courses", { headers: { Accept: "application/json" } });
          if (r.ok) {
            const list = await r.json();
            if (Array.isArray(list)) {
              const it = list.find((x) => (x.slug || x.course_slug || x.code) === slug);
              if (it) found = it;
            }
          }
        } catch (e) {}
      }
      if (alive) {
        setData(found);
        setLoading(false);
        if (!found) setError("Không tìm thấy khóa học");
      }
    }
    if (slug) load();
    return () => {
      alive = false;
    };
  }, [slug]);

  const view = useMemo(() => {
    const c = data || {};
    const courseId = c.id ?? c.course_id ?? c.courseId ?? c.course?.id ?? null;
    const enrollmentStatusRaw =
      c.user_enrollment_status ??
      c.userEnrollmentStatus ??
      c.enrollment_status ??
      c.enrollmentStatus ??
      c.userEnrollmentState ??
      null;
    const normalizedStatus =
      typeof enrollmentStatusRaw === "string" ? enrollmentStatusRaw.trim().toUpperCase() : enrollmentStatusRaw;
    const derivedEnrolled =
      c.enrolled ??
      c.is_enrolled ??
      c.isEnrolled ??
      c.has_enrolled ??
      c.hasEnrolled ??
      (typeof normalizedStatus === "string" && normalizedStatus === "ACTIVE");
    const modules = Array.isArray(c.modules) && c.modules.length ? c.modules : c.chapters ?? [];
    const stats = summarizeModules(modules);
    const totalDurationLabel = secondsToLabel(stats.totalSeconds);
    const priceRaw = c.price ?? c.tuition ?? c.amount ?? null;
    const numericPrice = normalizePriceValue(priceRaw);
    const isFree = resolveIsFree(priceRaw, c.is_free ?? c.isFree);
    return {
      id: courseId,
      slug: c.slug ?? c.course_slug ?? slug,
      title: c.title ?? c.name ?? c.course_title ?? slug,
      desc: c.description ?? c.long_desc ?? c.short_desc ?? c.summary ?? "",
      level: c.level ?? c.level_name ?? c.difficulty ?? "",
      image: resolveThumb(
        c.thumbnail_url ??
          c.thumbnailUrl ??
          c.thumbnail_path ??
          c.thumbnailPath ??
          c.image_url ??
          c.imageUrl ??
          c.cover_url ??
          c.coverUrl ??
          null
      ),
      modules,
      stats: {
        totalLessons: stats.totalLessons,
        totalDurationLabel,
        moduleCount: modules.length,
      },
      previewLesson: stats.firstLessonWithVideo,
      price: numericPrice,
      isFree,
      enrolled: Boolean(derivedEnrolled),
      isEnrolled: Boolean(derivedEnrolled),
      userEnrollmentStatus: normalizedStatus,
    };
  }, [data, slug]);

  const normalizedBaseModules = useMemo(() => normalizeModuleList(view.modules || []), [view.modules]);

  useEffect(() => {
    if (!view?.id) return undefined;
    setEntryContext((prev) => ({ ...(prev || {}), courseId: view.id, courseTitle: view.title, origin: "course_detail" }));
    return () =>
      setEntryContext((prev) => (prev && prev.courseId === view.id ? null : prev));
  }, [view?.id, view?.title, setEntryContext]);

  useEffect(() => {
    if (!view.id) {
      setSyllabus([]);
      return;
    }
    let cancelled = false;
    setSyllabus(normalizedBaseModules);
    setSyllabusLoading(true);
    setSyllabusError("");
    fetch(`${API_BASE}/api/public/courses/${view.id}/detail-sql`, { headers: { Accept: "application/json" } })
      .then((res) => {
        if (!res.ok) throw new Error("Không tải được nội dung khóa học");
        return res.json();
      })
      .then((payload) => {
        if (cancelled) return;
        const rawModules = payload?.modules?.length ? payload.modules : payload?.chapters || [];
        const normalized = normalizeModuleList(rawModules);
        setSyllabus(normalized.length ? normalized : normalizedBaseModules);
      })
      .catch((err) => {
        if (cancelled) return;
        setSyllabusError(err?.message || "Không tải được nội dung khóa học");
        setSyllabus(normalizedBaseModules);
      })
      .finally(() => {
        if (!cancelled) setSyllabusLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [view.id, normalizedBaseModules]);

  useEffect(() => {
    if (!view?.id) {
      setPublicResources([]);
      setResourceError("");
      setResourceLoading(false);
      return;
    }
    let cancelled = false;
    setResourceLoading(true);
    setResourceError("");
    const resourceUrl = `${API_BASE}/api/public/courses/resources?courseId=${encodeURIComponent(view.id)}`;
    fetch(resourceUrl, { headers: { Accept: "application/json" } })
      .then((res) => {
        if (!res.ok) {
          throw new Error("Không tải được tài liệu công khai");
        }
        return res.json();
      })
      .then((payload) => {
        if (cancelled) return;
        setPublicResources(Array.isArray(payload) ? payload : []);
      })
      .catch((err) => {
        if (cancelled) return;
        setPublicResources([]);
        setResourceError(err?.message || "Không tải được tài liệu công khai");
      })
      .finally(() => {
        if (!cancelled) {
          setResourceLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [view?.id]);

  const modulesForDisplay = useMemo(
    () => (syllabus.length ? syllabus : normalizedBaseModules),
    [syllabus, normalizedBaseModules],
  );

  const modulesStats = useMemo(() => summarizeModules(modulesForDisplay), [modulesForDisplay]);
  const totalLessons = modulesStats.totalLessons || view.stats.totalLessons;
  const totalDurationLabel = secondsToLabel(modulesStats.totalSeconds) || view.stats.totalDurationLabel;
  const moduleCount = modulesForDisplay.length || view.stats.moduleCount;
  const previewLesson = modulesStats.firstLessonWithVideo || view.previewLesson;
  const descriptionParagraphs = useMemo(() => splitDescriptionParagraphs(view.desc), [view.desc]);
  const heroSubtitle =
    descriptionParagraphs[0] ||
    "Xây dựng nền tảng vững chắc , đồng hành cùng giảng viên xuyên suốt khóa học";
  const groupedPublicResources = useMemo(() => {
    if (!publicResources.length) return [];
    const order = [];
    const map = new Map();
    publicResources.forEach((item) => {
      const key = item.lessonId ? `lesson-${item.lessonId}` : item.lessonTitle ? `lesson-${item.lessonTitle}` : "general";
      if (!map.has(key)) {
        map.set(key, {
          key,
          lessonId: item.lessonId,
          lessonTitle: item.lessonTitle,
          items: [],
        });
        order.push(map.get(key));
      }
      map.get(key).items.push(item);
    });
    return order;
  }, [publicResources]);
  const hasPublicResourceSection = resourceLoading || resourceError || groupedPublicResources.length > 0;
  const descriptionBodyParagraphs = descriptionParagraphs.length ? descriptionParagraphs : [heroSubtitle];

  const isFreeCourse = view.isFree;
  const priceLabel = isFreeCourse ? "Miễn phí" : formatMoney(view.price) || "Đang cập nhật";

  const handleStartCourse = () => {
    if (!view.id) return;
    if (!isAuthenticated) {
      navigate("/login", { state: { from: `/courses/${slug}` } });
      return;
    }
    navigate(`/learn/${view.id}`, { state: { from: `/courses/${slug}` } });
  };

  const handleCheckout = () => {
    navigate(`/checkout?course=${encodeURIComponent(slug)}&id=${view.id ?? ""}`);
  };

  const handlePreview = () => {
    setShowPreview(true);
    setTimeout(() => {
      previewRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
    }, 150);
  };

  const handleFreeEnroll = async () => {
    if (!view.id) return;
    if (!isAuthenticated) {
      navigate("/login", { state: { from: `/courses/${slug}` } });
      return;
    }
    setEnrollError("");
    setEnrolling(true);
    try {
      await httpClient.post(`/api/courses/${view.id}/enroll`);
      setEnrolled(true);
      handleStartCourse();
    } catch (err) {
      const msg = err?.response?.data?.message || "Ghi danh không thành công. Vui lòng thử lại.";
      setEnrollError(msg);
    } finally {
      setEnrolling(false);
    }
  };

  useEffect(() => {
    setEnrolling(false);
    setEnrolled(false);
    setEnrollError("");
  }, [slug]);

  useEffect(() => {
    if (view?.enrolled || view?.isEnrolled || (view?.userEnrollmentStatus || "").toUpperCase() === "ACTIVE") {
      setEnrolled(true);
      return;
    }
    if (!isAuthenticated || !view?.id) return undefined;
    let cancelled = false;
    httpClient
      .get("/api/student/enrollments")
      .then(({ data: enrollments }) => {
        if (cancelled) return;
        const list = Array.isArray(enrollments) ? enrollments : [];
        const match = list.some((item) => {
          const itemId = Number(item.courseId ?? item.course_id ?? item.id);
          if (itemId && view.id && Number(view.id) === itemId) return true;
          if (view.slug && item.slug && String(item.slug).toLowerCase() === String(view.slug).toLowerCase()) return true;
          return false;
        });
        if (match) setEnrolled(true);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [isAuthenticated, view?.id, view?.slug, view?.enrolled, view?.isEnrolled, view?.userEnrollmentStatus]);

  if (loading) {
    return <div className="max-w-7xl mx-auto px-4 py-12 text-stone-600">Đang tải chi tiết khóa học…</div>;
  }
  if (error) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-12 text-red-600">
        {error}
      </div>
    );
  }

  const statCards = [
    {
      label: "Bài học",
      value: totalLessons || "Đang cập nhật",
      icon: (
        <svg viewBox="0 0 24 24" className="h-6 w-6 text-primary-600" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M4 6h16M4 12h16M4 18h10" />
        </svg>
      ),
    },
    {
      label: "Thời lượng",
      value: totalDurationLabel || "Đang cập nhật",
      icon: (
        <svg viewBox="0 0 24 24" className="h-6 w-6 text-primary-600" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="12" cy="12" r="9" />
          <path d="M12 7v5l3 3" />
        </svg>
      ),
    },
    {
      label: "Chương học",
      value: moduleCount || "Đang cập nhật",
      icon: (
        <svg viewBox="0 0 24 24" className="h-6 w-6 text-primary-600" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M5 4h14v16H5z" />
          <path d="M5 9h14" />
        </svg>
      ),
    },
  ];

  return (
    <section className="bg-white">
      <div className="mx-auto max-w-7xl px-4 py-12">
        <div className="grid gap-10 lg:grid-cols-[1.75fr,1fr]">
          <div className="space-y-10">
            <SectionHeading eyebrow="Khóa học" title={view.title} subtitle={heroSubtitle} />


            <div className="rounded-3xl border border-stone-200 bg-stone-50 p-2 shadow-inner">
              {view.image ? (
                <img src={view.image} alt={view.title} className="h-full w-full rounded-[24px] object-cover" />
              ) : (
                <div className="aspect-video w-full rounded-[24px] bg-gradient-to-br from-primary-200 to-primary-50" />
              )}
            </div>

            <div className="grid gap-4 sm:grid-cols-3">
              {statCards.map((stat) => (
                <div key={stat.label} className="rounded-2xl border border-stone-200 bg-white p-4 shadow-sm">
                  <div className="flex items-center gap-3">
                    <div className="rounded-full bg-primary-50 p-2">{stat.icon}</div>
                    <div>
                      <p className="text-xs uppercase tracking-[0.2em] text-stone-400">{stat.label}</p>
                      <p className="text-lg font-semibold text-stone-900">{stat.value}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>


            {descriptionBodyParagraphs.length > 0 && (
              <div className="rounded-3xl border border-amber-100/70 bg-gradient-to-br from-amber-50 via-white to-rose-50 p-8 shadow-lg shadow-amber-100/50">
                <div className="flex flex-wrap items-center justify-between gap-4">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.35em] text-amber-500">Mô tả chi tiết</p>
                    <h3 className="mt-1 text-3xl font-bold text-stone-900">Trải nghiệm khóa học</h3>
                  </div>
                  <span className="rounded-full bg-white/80 px-4 py-2 text-sm font-semibold text-stone-600 shadow-inner">
                    Cập nhật {moduleCount || "???"} chương
                  </span>
                </div>
                <div className="mt-5 space-y-4 text-base leading-relaxed text-stone-700">
                  {descriptionBodyParagraphs.map((paragraph, idx) => (
                    <p key={idx} className="text-lg text-stone-700">
                      {paragraph}
                    </p>
                  ))}
                </div>
              </div>
            )}

            {hasPublicResourceSection && (
              <div className="rounded-3xl border border-primary-100/80 bg-gradient-to-br from-primary-50 via-white to-amber-50 p-6 shadow-lg shadow-primary-100/50">
                <div className="flex flex-wrap items-center justify-between gap-4">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.35em] text-primary-600">Tài liệu công khai</p>
                    <h3 className="mt-1 text-2xl font-bold text-stone-900">Dành cho học viên</h3>
                    <p className="mt-1 text-sm text-stone-600">
                      Đánh dấu những tài liệu được phép truy cập chung, giúp học viên dễ dàng bổ sung kiến thức cho từng mô-đun trọng tâm.
                    </p>
                  </div>
                  <span className="rounded-full bg-white/90 px-4 py-2 text-sm font-semibold text-primary-600 shadow-inner">
                    {publicResources.length} tệp
                  </span>
                </div>
                {resourceLoading && (
                  <div className="mt-4 rounded-2xl border border-dashed border-primary-200 bg-white/70 px-4 py-3 text-sm text-primary-700">
                    Đang tải danh sách tài liệu...
                  </div>
                )}
                {!resourceLoading && resourceError && (
                  <p className="mt-4 text-sm text-red-600">{resourceError}</p>
                )}
                {!resourceLoading && !resourceError && groupedPublicResources.length === 0 && (
                  <p className="mt-4 text-sm text-stone-500">Chưa có tài liệu công khai cho khóa học này.</p>
                )}
                {groupedPublicResources.length > 0 && (
                  <div className="mt-6 space-y-6">
                    {groupedPublicResources.map((group) => (
                      <div key={group.key} className="rounded-2xl border border-white/60 bg-white/90 p-5 shadow-md shadow-primary-50/60">
                        <div className="flex flex-wrap items-center justify-between gap-3">
                          <div>
                            <p className="text-xs font-semibold uppercase tracking-[0.3em] text-primary-500">Bài học</p>
                            <h4 className="text-lg font-semibold text-stone-900">
                              {group.lessonTitle || "Tài liệu chung"}
                            </h4>
                          </div>
                          <span className="rounded-full bg-primary-50 px-4 py-1 text-xs font-semibold text-primary-700">
                            {group.items.length} tài liệu
                          </span>
                        </div>
                        <ul className="mt-4 space-y-3">
                          {group.items.map((resource) => {
                            const resourceUrl = resolveResourceUrl(resource);
                            const sizeLabel = formatFileSize(resource.fileSize);
                            const typeLabel = resource.sourceType === "link" ? "Liên kết" : "Tệp đính kèm";
                            return (
                              <li
                                key={resource.id}
                                className="flex flex-col gap-4 rounded-2xl border border-stone-100 bg-stone-50/70 px-4 py-4 md:flex-row md:items-center md:justify-between"
                              >
                                <div>
                                  <p className="text-base font-semibold text-stone-900">{resource.title}</p>
                                  {resource.description && (
                                    <p className="mt-1 text-sm text-stone-500">{resource.description}</p>
                                  )}
                                  <div className="mt-2 flex flex-wrap gap-2 text-xs font-semibold text-stone-500">
                                    <span className="inline-flex items-center gap-1 rounded-full bg-white px-3 py-1 shadow-sm shadow-stone-100">
                                      {typeLabel}
                                    </span>
                                    {resource.fileType && (
                                      <span className="inline-flex items-center rounded-full bg-white px-3 py-1 shadow-sm shadow-stone-100">
                                        {resource.fileType.toUpperCase()}
                                      </span>
                                    )}
                                    {sizeLabel && (
                                      <span className="inline-flex items-center rounded-full bg-white px-3 py-1 shadow-sm shadow-stone-100">
                                        {sizeLabel}
                                      </span>
                                    )}
                                  </div>
                                </div>
                                <div className="flex flex-wrap items-center gap-3">
                                  {resourceUrl ? (
                                    <a
                                      href={resourceUrl}
                                      target="_blank"
                                      rel="noreferrer"
                                      className="inline-flex items-center gap-2 rounded-full bg-gradient-to-r from-primary-500 via-rose-500 to-amber-500 px-5 py-2 text-sm font-semibold text-white shadow-lg shadow-rose-100/60 transition hover:translate-y-0.5"
                                    >
                                      <svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M12 5v14M5 12h14" />
                                      </svg>
                                      Mô tả tài liệu
                                    </a>
                                  ) : (
                                    <span className="text-xs text-stone-400">Liên hệ hỗ trợ trước để nhận file</span>
                                  )}
                                </div>
                              </li>
                            );
                          })}
                        </ul>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}



            {showPreview && (
              <div ref={previewRef} className="rounded-3xl border border-stone-200 bg-white p-6 shadow-sm">
                <p className="text-sm font-semibold text-primary-600">Học thử</p>
                <h3 className="mt-2 text-2xl font-bold text-stone-900">
                  {previewLesson?.title || "Video giới thiệu khóa học"}
                </h3>
                <p className="mt-2 text-sm text-stone-600">
                  {descriptionBodyParagraphs[0] ||
                    "Tận mắt xem cách giảng viên đồng hành, bài giảng được trình bày ra sao trước khi quyết định."}
                </p>
                <div className="mt-4 aspect-video overflow-hidden rounded-2xl border border-stone-200 bg-stone-50">
                  {previewLesson?.video_url ? (
                    <video
                      controls
                      poster={view.image || undefined}
                      src={previewLesson.video_url}
                      className="h-full w-full object-cover"
                    />
                  ) : (
                    <div className="grid h-full place-items-center text-stone-500">Chưa có video học thử.</div>
                  )}
                </div>
              </div>
            )}

            <div className="rounded-3xl border border-stone-200 bg-white p-6 shadow-sm">
              <div className="flex items-center justify-between">
                <h3 className="text-2xl font-bold text-stone-900">Nội dung khóa học</h3>
                <span className="text-sm text-stone-500">
                  {moduleCount} chương · {totalLessons} bài học · {totalDurationLabel || "Đang cập nhật"}
                </span>
              </div>
              {syllabusLoading && (
                <div className="mt-4 rounded-2xl border border-dashed border-primary-200 bg-primary-50/40 px-4 py-3 text-sm text-primary-700">
                  Đang tải nội dung và cấu trúc bài giảng...
                </div>
              )}
              {syllabusError && (
                <p className="mt-3 text-sm text-red-600">{syllabusError}</p>
              )}
              <div className="mt-6 space-y-4">
                {modulesForDisplay.map((module, idx) => {
                  const lessons = Array.isArray(module.lessons) ? module.lessons : [];
                  const moduleDurationLabel =
                    secondsToLabel(module.durationSeconds) ||
                    secondsToLabel(lessons.reduce((sum, lesson) => sum + extractLessonDurationSeconds(lesson), 0)) ||
                    null;
                  return (
                    <div key={module.id ?? idx} className="rounded-2xl border border-stone-100 bg-white/90 p-4 shadow-sm">
                      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                        <div>
                          <p className="text-xs font-semibold uppercase tracking-[0.25em] text-primary-600">
                            Chương {idx + 1}
                          </p>
                          <h4 className="text-lg font-semibold text-stone-900">{module.title || `Chương ${idx + 1}`}</h4>
                          {module.description && (
                            <p className="mt-1 text-sm text-stone-500">{stripHtmlTags(module.description)}</p>
                          )}
                        </div>
                        <span className="text-xs font-semibold text-stone-500">
                          {lessons.length} bài · {moduleDurationLabel || "Đang cập nhật"}
                        </span>
                      </div>
                      {lessons.length > 0 ? (
                        <ul className="mt-4 space-y-3">
                          {lessons.map((lesson, lessonIdx) => (
                            <li
                              key={lesson.id ?? `${module.id}-${lessonIdx}`}
                              className="flex flex-col gap-2 rounded-2xl border border-stone-100 bg-stone-50/80 px-3 py-3 sm:flex-row sm:items-center sm:justify-between"
                            >
                              <div className="flex items-start gap-3">
                                <span className="mt-0.5 rounded-full bg-primary-100 px-2 py-1 text-xs font-semibold text-primary-700">
                                  {String(lessonIdx + 1).padStart(2, "0")}
                                </span>
                                <div>
                                  <p className="font-semibold leading-snug text-stone-900">
                                    {lesson.title || `Bài ${lessonIdx + 1}`}
                                  </p>
                                  {lesson.description ? (
                                    <p className="text-xs text-stone-500">{lesson.description}</p>
                                  ) : lesson.content ? (
                                    <p className="text-xs text-stone-500">{lesson.content}</p>
                                  ) : null}
                                </div>
                              </div>
                              <div className="flex items-center gap-2 text-xs font-semibold text-stone-500">
                                {lesson.durationLabel && (
                                  <span className="inline-flex items-center gap-1 rounded-full bg-white px-3 py-1 shadow-sm">
                                    <svg
                                      viewBox="0 0 24 24"
                                      className="h-3.5 w-3.5 text-primary-600"
                                      fill="none"
                                      stroke="currentColor"
                                      strokeWidth="2"
                                    >
                                      <circle cx="12" cy="12" r="9" />
                                      <path d="M12 7v5l3 3" />
                                    </svg>
                                    {lesson.durationLabel}
                                  </span>
                                )}
                                {lesson.video_url && (
                                  <span className="inline-flex items-center gap-1 rounded-full bg-primary-50 px-3 py-1 text-primary-600">
                                    <svg
                                      viewBox="0 0 24 24"
                                      className="h-3.5 w-3.5"
                                      fill="none"
                                      stroke="currentColor"
                                      strokeWidth="2"
                                    >
                                      <path d="M8 5v14l11-7z" />
                                    </svg>
                                    Video
                                  </span>
                                )}
                              </div>
                            </li>
                          ))}
                        </ul>
                      ) : (
                        <p className="mt-3 text-sm text-stone-500">Chương này đang cập nhật bài học.</p>
                      )}
                    </div>
                  );
                })}
                {modulesForDisplay.length === 0 && !syllabusLoading && (
                  <p className="text-sm text-stone-500">Nội dung khóa học đang được cập nhật.</p>
                )}
              </div>
            </div>

            <CourseReviewsPanel
              courseId={view?.id}
              canReview={Boolean(enrolled || view?.enrolled || view?.isEnrolled || view?.userEnrollmentStatus === "ACTIVE")}
            />
          </div>

          <aside className="space-y-6">
            <div className="rounded-3xl border border-primary-100 bg-primary-50/50 p-6 shadow-lg shadow-primary-100/50">
              <p className="text-xs font-semibold uppercase tracking-[0.3em] text-primary-600">Chi phí</p>
              <div className="mt-2 text-4xl font-black text-primary-700">{priceLabel}</div>
              <p className="mt-2 text-sm text-stone-600">
                Quyền truy cập trọn đời, cập nhật miễn phí và mentor hỗ trợ trong suốt khóa học.
              </p>

              {isFreeCourse ? (
                <>
                  <button className="btn btn-primary mt-6 w-full" onClick={handleFreeEnroll} disabled={enrolling}>
                    {enrolling ? "Đang ghi danh..." : enrolled ? "Vào học ngay" : "Đăng ký học ngay"}
                  </button>
                  {enrollError && <p className="mt-2 text-sm text-red-600">{enrollError}</p>}
                </>
              ) : (
                <>
                  <button className="btn btn-primary mt-6 w-full" onClick={handleCheckout}>
                    Mua khóa học
                  </button>
                  <button
                    className="btn mt-3 w-full border-primary-200 text-primary-700 hover:border-primary-400"
                    onClick={handlePreview}
                  >
                    Học thử
                  </button>
                </>
              )}

              <button
                type="button"
                className="btn mt-4 w-full border border-dashed border-primary-300 text-primary-700 hover:border-primary-500"
                onClick={() =>
                  openChat({
                    origin: "course_detail",
                    courseId: view.id,
                    courseTitle: view.title,
                    topic: "course_advice",
                  })
                }
              >
                Cần tư vấn? Chat với hỗ trợ
              </button>

              <ul className="mt-6 space-y-3 text-sm text-stone-700">
                <li className="flex items-center gap-2">
                  <span className="text-primary-600">•</span>
                  Trình độ: {levelLabel(view.level)}
                </li>
                <li className="flex items-center gap-2">
                  <span className="text-primary-600">•</span>
                  Tổng {totalLessons || "—"} bài học · {totalDurationLabel || "Đang cập nhật"}
                </li>
                <li className="flex items-center gap-2">
                  <span className="text-primary-600">•</span>
                  Học trên mọi thiết bị, không giới hạn
                </li>
              </ul>
            </div>

            <div className="rounded-3xl border border-stone-200 bg-white p-6 shadow-sm">
              <h4 className="text-lg font-semibold text-stone-900">Lời giới thiệu</h4>
              <p className="mt-2 text-sm text-stone-600">
                {descriptionBodyParagraphs[0] ||
                  "KhoÃ¡ há»c cung cáº¥p kiáº¿n thá»©c trá»ng tÃ¢m Äi kÃ¨m dá»± Ã¡n thá»±c táº¿ Äá» báº¡n cÃ³ thá» tá»± tin á»©ng tuyá»n vá» trÃ­ mÆ¡ Æ°á»c."}
              </p>
              {!isFreeCourse && (
                <p className="mt-4 text-sm text-primary-700">
                  Bạn chưa chắc chắn? Hãy chọn “Học thử” để xem ngay bài giảng đầu tiên hoàn toàn miễn phí.
                </p>
              )}
            </div>
          </aside>
        </div>
      </div>
    </section>
  );
}
