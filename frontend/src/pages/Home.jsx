import React, { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import SectionHeading from "../components/SectionHeading";
import { API_BASE_URL } from "../api/httpClient";

const API_BASE = API_BASE_URL;

const HERO_IMAGE =
  "https://plus.unsplash.com/premium_photo-1664195786180-23507e5048c1?auto=format&fit=crop&w=1000&q=70";
const ILLUSTRATION_GENERAL =
  "/images/s.png";
const ILLUSTRATION_CLASS =
  "/images/cs.png";
const ILLUSTRATION_TEST =
  "/images/s.png";

function resolveThumb(u) {
  if (!u) return null;
  let s = String(u).trim().replace(/\\/g, "/");
  if (/^https?:\/\//i.test(s) || s.startsWith("data:")) return s;
  if (!s.startsWith("/")) s = `/${s}`;
  return `${API_BASE}${s}`;
}

function resolveAssetUrl(u) {
  if (!u) return "";
  const raw = String(u).trim();
  if (!raw) return "";
  if (/^(?:https?:|data:|blob:)/i.test(raw)) return raw;
  const normalized = raw.startsWith("/") ? raw : `/${raw}`;
  return `${API_BASE}${normalized}`;
}

function coerceNumericId(value) {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim() !== "") {
    const parsed = Number(value.trim());
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

function normalizeEnrollmentCourse(course) {
  if (!course) return null;
  const courseId = coerceNumericId(course?.courseId ?? course?.id);
  if (!courseId) return null;
  const thumbnailCandidate =
    course.thumbnailUrl ??
    course.thumbnail ??
    course.coverImage ??
    course.thumbnail_path ??
    course.thumbnailPath ??
    null;
  return {
    courseId,
    title: course.title ?? "Khoá học",
    level: course.level ?? course.levelName ?? "",
    thumbnail: resolveThumb(thumbnailCandidate),
  };
}


function Hero({ progressSlides, loadingProgress, avgPercent }) {
  const navigate = useNavigate();
  const [activeIndex, setActiveIndex] = useState(0);
  const slides = progressSlides || [];
  const slidesCount = slides.length;
  const safeAvg = Math.min(Math.max(avgPercent || 0, 0), 100);
  const activeSlide = slidesCount > 0 ? slides[Math.min(activeIndex, slidesCount - 1)] : null;

  useEffect(() => {
    setActiveIndex(0);
  }, [slidesCount]);

  useEffect(() => {
    if (slidesCount <= 1) return undefined;
    const id = setInterval(() => {
      setActiveIndex((prev) => (prev + 1) % slidesCount);
    }, 7000);
    return () => clearInterval(id);
  }, [slidesCount]);

  const gotoPrev = () => {
    if (slidesCount === 0) return;
    setActiveIndex((prev) => (prev - 1 + slidesCount) % slidesCount);
  };

  const gotoNext = () => {
    if (slidesCount === 0) return;
    setActiveIndex((prev) => (prev + 1) % slidesCount);
  };

  const handleStartLearning = () => {
    if (activeSlide?.courseId) {
      navigate(`/learn/${activeSlide.courseId}`);
    } else {
      navigate("/learn/fullstack");
    }
  };

  const fallbackMessage = loadingProgress
    ? "Đang tải tiến độ học tập..."
    : "Đăng nhập để xem tiến độ các khoá học bạn đang theo học.";

  return (
    <section className="bg-soft">
      <div className="max-w-7xl mx-auto px-4 py-16 md:py-24 grid md:grid-cols-2 gap-10 items-center">
        <div>
          <div className="inline-flex items-center gap-2 rounded-full border border-primary-200 bg-white px-3 py-1 text-xs text-primary-700">
            <span className="w-1.5 h-1.5 rounded-full bg-primary-600" />
            Nền tảng tiếng Anh toàn diện
          </div>
          <h1 className="mt-4 text-4xl md:text-5xl font-extrabold leading-tight text-stone-900">
            Làm chủ tiếng Anh với mentor và AI song hành
          </h1>
          <p className="mt-4 text-stone-600 text-lg">
            Chọn lộ trình IELTS, TOEIC, Giao tiếp hoặc Business English với bài học tương tác, flashcard thông minh và phản hồi phát âm chuẩn quốc tế.
          </p>
          <div className="mt-6 flex flex-col sm:flex-row gap-3">
            <button
              type="button"
              onClick={handleStartLearning}
              className="btn btn-primary px-6 py-3 disabled:opacity-50"
              disabled={loadingProgress && slidesCount === 0}
            >
              Bắt đầu học ngay
            </button>
            <a href="#courses" className="btn px-6 py-3 border-stone-300 hover:border-stone-400">Xem khoá học</a>
          </div>
          <div className="mt-8 flex flex-wrap items-center gap-6 text-sm text-stone-600">
            <div className="flex -space-x-2">
              <div className="w-8 h-8 rounded-full bg-primary-200 border border-white" />
              <div className="w-8 h-8 rounded-full bg-primary-300 border border-white" />
              <div className="w-8 h-8 rounded-full bg-primary-400 border border-white" />
            </div>
            <span>65.000+ học viên đã nâng band</span>
            {slidesCount > 0 && (
              <span className="inline-flex items-center gap-2 rounded-full bg-white px-3 py-1 text-xs font-semibold text-stone-600">
                <span className="h-2 w-2 rounded-full bg-primary-500" />
                {slidesCount} khoá đang học
              </span>
            )}
          </div>
        </div>
        <div className="relative">
          <div className="relative aspect-[4/3] overflow-hidden rounded-3xl bg-gradient-to-br from-[#0d2f5d] to-[#0f498d] shadow-2xl">
            {activeSlide ? (
              <>
                <img
                  src={activeSlide.thumbnail || HERO_IMAGE}
                  alt={activeSlide.title || "English course"}
                  className="absolute inset-0 h-full w-full object-cover opacity-70"
                  loading="lazy"
                />
                <div className="absolute inset-0 bg-gradient-to-tr from-black/60 via-black/30 to-transparent" />
                <div className="relative z-10 flex h-full flex-col justify-between p-6 text-white">
                  <div>
                    <span className="inline-flex items-center rounded-full bg-white/20 px-3 py-1 text-xs font-semibold uppercase tracking-widest text-white/90">
                      {activeSlide.level || "Khoá học"}
                    </span>
                    <h3 className="mt-3 text-2xl font-bold leading-snug">{activeSlide.title}</h3>
                    <p className="mt-2 text-sm text-white/80">{activeSlide.lessonsLabel}</p>
                  </div>
                  <div>
                    <div className="flex items-center justify-between text-sm font-semibold text-white/80">
                      <span>Tiến độ khóa học</span>
                      <span>{activeSlide.percent}%</span>
                    </div>
                    <div className="mt-2 h-3 rounded-full bg-white/20">
                      <div className="h-full rounded-full bg-amber-300" style={{ width: `${activeSlide.percent}%` }} />
                    </div>
                  </div>
                </div>
                <div className="absolute right-6 top-6 hidden flex-col gap-3 rounded-3xl bg-white/15 px-4 py-3 text-white shadow-[0_15px_30px_rgba(0,0,0,0.25)] backdrop-blur lg:flex">
                  <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.3em] text-white/80">
                    <span className="inline-flex h-2 w-2 rounded-full bg-amber-300" />
                    Tiến độ hôm nay
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="h-2 w-28 rounded-full bg-white/20">
                      <div className="h-full rounded-full bg-gradient-to-r from-amber-200 to-yellow-300" style={{ width: `${safeAvg}%` }} />
                    </div>
                    <span className="text-lg font-bold text-white">{safeAvg}%</span>
                  </div>
                </div>
              </>
            ) : (
              <div className="flex h-full items-center justify-center px-6 text-center text-white/80">
                {fallbackMessage}
              </div>
            )}
          </div>
          {slidesCount > 1 && (
            <div className="absolute bottom-6 left-1/2 flex -translate-x-1/2 items-center gap-3 rounded-full border border-white/40 bg-white/80 px-4 py-2 text-xs font-semibold text-stone-700 shadow-lg backdrop-blur">
              <button
                type="button"
                className="rounded-full border border-stone-200 px-2 py-1 text-lg leading-none hover:bg-stone-100"
                onClick={gotoPrev}
                aria-label="Khoá trước"
              >
                ‹
              </button>
              <span>
                {activeIndex + 1}/{slidesCount}
              </span>
              <button
                type="button"
                className="rounded-full border border-stone-200 px-2 py-1 text-lg leading-none hover:bg-stone-100"
                onClick={gotoNext}
                aria-label="Khoá tiếp theo"
              >
                ›
              </button>
            </div>
          )}
        </div>
        {slidesCount > 0 && (
          <div className="mt-6 flex items-center justify-between rounded-3xl border border-white/70 bg-white px-5 py-4 text-sm font-semibold text-stone-700 shadow-[0_15px_35px_rgba(15,23,42,0.12)] sm:hidden">
            <div>
              <p className="text-xs uppercase tracking-[0.35em] text-stone-400">Tiến độ hôm nay</p>
              <div className="mt-2 h-2 w-40 rounded-full bg-stone-100">
                <div className="h-full rounded-full bg-gradient-to-r from-amber-300 to-orange-400" style={{ width: `${safeAvg}%` }} />
              </div>
            </div>
            <span className="text-base font-bold text-stone-900">{safeAvg}%</span>
          </div>
        )}
      </div>
    </section>
  );
}


export default function Home() {
  const token = useMemo(() => {
    try {
      return localStorage.getItem("token");
    } catch {
      return null;
    }
  }, []);

  const authHeaders = useMemo(
    () =>
      token
        ? {
            Accept: "application/json",
            Authorization: `Bearer ${token}`,
          }
        : null,
    [token],
  );

  const [heroProgress, setHeroProgress] = useState({ loading: false, slides: [], avgPercent: 0 });

  useEffect(() => {
    if (!authHeaders) {
      setHeroProgress({ loading: false, slides: [], avgPercent: 0 });
      return;
    }
    let alive = true;
    const fetchProgress = async () => {
      setHeroProgress((prev) => ({ ...prev, loading: true }));
      try {
        const res = await fetch(`${API_BASE}/api/student/enrollments`, { headers: authHeaders });
        if (!res.ok) throw new Error("Không thể tải khóa học đã đăng ký");
        const data = await res.json();
        if (!alive) return;
        const normalized = (Array.isArray(data) ? data : [])
          .map(normalizeEnrollmentCourse)
          .filter(Boolean)
          .slice(0, 6);
        if (normalized.length === 0) {
          setHeroProgress({ loading: false, slides: [], avgPercent: 0 });
          return;
        }
        const progressPairs = await Promise.all(
          normalized.map(async (course) => {
            try {
              const progressRes = await fetch(`${API_BASE}/api/student/progress/courses/${course.courseId}`, {
                headers: authHeaders,
              });
              if (!progressRes.ok) return [course.courseId, null];
              const progressData = await progressRes.json();
              return [
                course.courseId,
                {
                  completed: progressData?.completedLessons ?? 0,
                  total: progressData?.totalLessons ?? 0,
                  percent: progressData?.completionPercent ?? 0,
                },
              ];
            } catch {
              return [course.courseId, null];
            }
          }),
        );
        const progressMap = {};
        progressPairs.forEach(([courseId, info]) => {
          if (courseId) progressMap[courseId] = info;
        });
        const slides = normalized.map((course) => {
          const stats = progressMap[course.courseId] || {};
          const total = stats.total ?? 0;
          const completed = stats.completed ?? 0;
          const percent = Math.round(
            Math.min(
              Math.max(stats.percent ?? (total > 0 ? (completed / total) * 100 : 0), 0),
              100,
            ),
          );
          return {
            ...course,
            percent,
            completed,
            total,
            lessonsLabel: total ? `${completed}/${total} bài học` : `${completed} bài đã hoàn thành`,
          };
        });
        const avgPercent = slides.length
          ? Math.round(slides.reduce((sum, slide) => sum + (slide.percent || 0), 0) / slides.length)
          : 0;
        setHeroProgress({ loading: false, slides, avgPercent });
      } catch (err) {
        console.error(err);
        if (alive) setHeroProgress({ loading: false, slides: [], avgPercent: 0 });
      }
    };
    fetchProgress();
    return () => {
      alive = false;
    };
  }, [authHeaders]);

  return (
    <div className="min-h-screen bg-white">
      <style>{`
        a[href=\"/pricing\"], a[href=\"/bang-gia\"], a[data-nav=\"pricing\"] {
          display: none !important;
        }
      `}</style>
      <Hero
        progressSlides={heroProgress.slides}
        loadingProgress={heroProgress.loading}
        avgPercent={heroProgress.avgPercent}
      />
      <CourseTeaser />
      <StatsBand />
      <HeritageBanner />
      <SolutionTiles />
      <ProgramShowcase />
      <PlacementTestCTA />
      <TeacherShowcase />
      <Trusted />
      <Features />
      <Testimonials />
      <CtaBanner />
    </div>
  );
}


function CourseTeaser() {
  const categories = ["Kiến thức nền", "Kỹ năng", "Cấp độ", "Nhu cầu", "Chứng chỉ", "Học sinh & sinh viên"];
  const levels = ["350+", "450+", "550+", "650+", "750+", "850+", "950+"];

  return (
    <section className="bg-[#fffdf1] py-16">
      <div className="mx-auto max-w-6xl px-4">
        <div className="rounded-[32px] border border-slate-200 bg-white/90 p-6 shadow-[0_25px_80px_rgba(15,23,42,0.08)]">
          <h2 className="text-3xl font-bold text-stone-900">Danh sách các khóa học</h2>
          <p className="mt-2 text-sm text-stone-500">
            Lọc theo chủ đề và trình độ TOEIC phù hợp để chọn lộ trình rõ ràng hơn.
          </p>
          <div className="mt-6 flex flex-wrap gap-2">
            {categories.map((cat) => (
              <span
                key={cat}
                className="rounded-full border border-stone-200 bg-stone-50 px-4 py-2 text-xs font-semibold text-stone-500"
              >
                {cat}
              </span>
            ))}
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            {levels.map((level) => (
              <span
                key={level}
                className="rounded-full border border-slate-200 bg-white px-3 py-1 text-xs font-semibold text-stone-400"
              >
                {level}
              </span>
            ))}
          </div>
          <div className="mt-6 flex flex-wrap items-center gap-3">
            <Link
              to="/courses"
              className="inline-flex items-center rounded-full bg-stone-900 px-6 py-3 text-sm font-semibold text-white shadow-lg transition hover:bg-stone-800"
            >
              Xem toàn bộ khóa học
            </Link>
            <span className="text-sm text-stone-500">Truy cập trang Khóa học để xem đầy đủ nội dung.</span>
          </div>
        </div>
      </div>
    </section>
  );
}


function StatsBand() {
  const stats = [
    { value: "65.000+", label: "Học viên đạt mục tiêu" },
    { value: "8+", label: "Năm kinh nghiệm luyện thi" },
    { value: "120+", label: "Mentor bản ngữ & Việt Nam" },
    { value: "300+", label: "Bài học tương tác & đề thi" },
  ];
  return (
    <section className="bg-white">
      <div className="mx-auto max-w-7xl px-4 py-8">
        <div className="grid gap-4 rounded-3xl border border-white/80 bg-white/90 p-4 shadow-[0_20px_40px_rgba(15,23,42,0.06)] sm:grid-cols-2 lg:grid-cols-4">
          {stats.map((item) => (
            <div key={item.label} className="rounded-2xl bg-gradient-to-br from-primary-50 to-white px-4 py-6 text-center">
              <p className="text-2xl font-bold text-primary-600">{item.value}</p>
              <p className="mt-1 text-sm text-stone-600">{item.label}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function ProgramShowcase() {
  const programs = [
    {
      title: "IELTS Ready",
      desc: "Tăng từ 5.0 lên 7.5+ với mentor 1:1, bài Speaking & Writing được chấm hàng tuần.",
      points: ["36 bài học tương tác", "Flashcard từ vựng Cambridge", "Lịch học linh hoạt"],
      cta: "/courses?track=ielts",
      color: "from-[#fdf2e9] to-white",
    },
    {
      title: "TOEIC Express",
      desc: "Tập trung Listening & Reading, tăng 200+ điểm sau 6 tuần với đề sát ETS.",
      points: ["12 đề thi thử", "Chiến lược giải nhanh từng Part", "Kho audio song ngữ"],
      cta: "/courses?track=toeic",
      color: "from-[#e8f4ff] to-white",
    },
    {
      title: "Speaking Lab",
      desc: "Lớp giao tiếp chuyên sâu, luyện phát âm IPA và phản xạ hội thoại với AI + mentor bản ngữ.",
      points: ["AI chấm phát âm", "CLB nói chuyện hằng tuần", "Mentor sửa lỗi trực tiếp"],
      cta: "/courses?track=speaking",
      color: "from-[#f1f3ff] to-white",
    },
    {
      title: "Kids English Adventures",
      desc: "Chương trình cho bé 6-12 tuổi với trò chơi gamified, câu chuyện và giáo viên nước ngoài.",
      points: ["Nhân vật hoạt hình", "Bảng điều khiển dành cho phụ huynh", "Lớp live hàng tuần"],
      cta: "/courses?track=kids",
      color: "from-[#fff0f7] to-white",
    },
  ];
  return (
    <section className="bg-[#fdf8f1]">
      <div className="mx-auto max-w-7xl px-4 py-16">
        <div className="text-center">
          <p className="text-xs uppercase tracking-[0.3em] text-stone-400">Lộ trình nổi bật</p>
          <h2 className="mt-2 text-3xl font-semibold text-stone-900">Chọn mục tiêu tiếng Anh phù hợp với bạn</h2>
          <p className="mt-2 text-sm text-stone-500">
            Không cần hiển thị toàn bộ danh sách khóa học. Khi bạn sẵn sàng, chỉ cần nhấn “Khám phá khóa học” để tới trang chi tiết.
          </p>
        </div>
        <div className="mt-10 grid gap-6 md:grid-cols-2 lg:grid-cols-4">
          {programs.map((program) => (
            <article
              key={program.title}
              className={`flex h-full flex-col rounded-[28px] border border-white/70 bg-gradient-to-b ${program.color} p-6 shadow-[0_25px_55px_rgba(15,23,42,0.08)]`}
            >
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.35em] text-primary-500">Program</p>
                <h3 className="mt-2 text-xl font-bold text-stone-900">{program.title}</h3>
                <p className="mt-2 text-sm text-stone-600">{program.desc}</p>
              </div>
              <ul className="mt-6 space-y-2 text-sm text-stone-600">
                {program.points.map((point) => (
                  <li key={point} className="flex items-start gap-2">
                    <span className="mt-1 h-1.5 w-1.5 rounded-full bg-primary-500" />
                    <span>{point}</span>
                  </li>
                ))}
              </ul>
              <Link
                to={program.cta}
                className="mt-6 inline-flex items-center justify-center rounded-full border border-primary-200 px-4 py-2 text-sm font-semibold text-primary-700 transition hover:border-primary-500 hover:bg-white"
              >
                Khám phá khóa học
                <svg viewBox="0 0 24 24" className="ml-2 h-4 w-4" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="m9 6 6 6-6 6" />
                </svg>
              </Link>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

function HeritageBanner() {
  return (
    <section className="bg-white">
      <div className="mx-auto max-w-6xl px-4 py-16">
        <div className="flex flex-col gap-8 rounded-[40px] border border-blue-100 bg-gradient-to-b from-white to-blue-50/40 p-8 text-center lg:flex-row lg:items-center lg:text-left">
          <div className="flex-1">
            <p className="text-sm font-semibold uppercase tracking-[0.4em] text-blue-400">#1 Việt Nam</p>
            <h2 className="mt-2 text-3xl font-semibold text-stone-900">Chinh phục mục tiêu tiếng Anh với nền tảng học tập được tin dùng suốt 12 năm</h2>
            <p className="mt-4 text-sm text-stone-600">
              Phát triển từ năm 2013, nền tảng hiện đồng hành cùng hơn 2 triệu học viên và được hệ sinh thái TECHFEST bình chọn là EdTech học tiếng Anh sáng tạo.
              Lịch học linh hoạt, bài tập tương tác và hệ thống đánh giá chuẩn quốc tế giúp bạn xác định lộ trình rõ ràng.
            </p>
          </div>
          <div className="flex flex-1 items-center justify-center">
            <div className="relative rounded-full border-2 border-blue-200 px-10 py-8 text-center shadow-[0_15px_40px_rgba(37,99,235,0.15)]">
              <p className="text-sm font-semibold uppercase tracking-widest text-blue-400">XÓA BỎ RÀO CẢN</p>
              <p className="text-5xl font-black text-blue-500">12</p>
              <p className="text-sm font-semibold text-blue-400">NĂM ĐỒNG HÀNH</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function SolutionTiles() {
  const tiles = [
    {
      logo: "VOCA.VN",
      tone: "text-blue-500",
      description:
        "Nền tảng học tiếng Anh tổng quát với 500+ khóa theo kỹ năng, cấp độ. Học qua trò chơi, flashcard và AI phản hồi giúp ghi nhớ lâu.",
      cta: "Bắt đầu miễn phí",
      link: "/register",
      bg: "from-[#f1f9ff] to-white",
      media: { type: "image", src: ILLUSTRATION_GENERAL },
    },
    {
      logo: "VOCA ClassZoom",
      tone: "text-orange-500",
      description:
        "Lớp học trực tuyến 1 kèm 1 kết hợp e-learning và mentor sư phạm. Duy trì động lực, có giáo trình cá nhân và buổi kèm mỗi tuần.",
      cta: "Nhận 2 buổi học thử",
      link: "/survey",
      bg: "from-[#fff7eb] to-white",
      media: {
        type: "video",
        src: "https://cdn.coverr.co/videos/coverr-intense-language-lessons-6575/1080p.mp4",
        poster: ILLUSTRATION_CLASS,
      },
    },
  ];
  return (
    <section className="bg-white">
      <div className="mx-auto max-w-7xl px-4 py-16 space-y-12">
        {tiles.map((tile) => (
          <div
            key={tile.logo}
            className={`grid gap-6 rounded-[36px] border border-white/70 bg-gradient-to-r ${tile.bg} p-8 shadow-[0_30px_60px_rgba(15,23,42,0.08)] lg:grid-cols-2`}
          >
            <div>
              <p className={`text-2xl font-bold ${tile.tone}`}>{tile.logo}</p>
              <p className="mt-2 text-base font-semibold text-stone-900">Nền tảng học tiếng Anh vui, hiệu quả</p>
              <p className="mt-3 text-sm text-stone-600">{tile.description}</p>
              <a
                href={tile.link}
                className="mt-6 inline-flex items-center rounded-full border border-stone-200 bg-white px-5 py-2 text-sm font-semibold text-stone-700 shadow-sm hover:border-primary-400"
              >
                {tile.cta}
              </a>
            </div>
            <div className="flex items-center justify-center">
              {tile.media?.type === "video" ? (
                <video
                  controls
                  muted
                  loop
                  poster={tile.media.poster}
                  className="h-48 w-64 overflow-hidden rounded-[32px] object-cover shadow-inner"
                >
                  <source src={tile.media.src} type="video/mp4" />
                  Trình duyệt không hỗ trợ video.
                </video>
              ) : (
                <img
                  src={tile.media?.src || tile.image}
                  alt={tile.logo}
                  className="h-48 w-64 rounded-[32px] object-cover shadow-inner"
                />
              )}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}


function PlacementTestCTA() {
  return (
    <section className="bg-[#fff8f0] py-16">
      <div className="mx-auto max-w-6xl px-4">
        <div className="flex flex-col overflow-hidden rounded-[40px] bg-white shadow-[0_30px_80px_rgba(255,_161,_95,_0.25)] lg:flex-row">
          <div className="flex-1 p-8 md:p-12">
            <p className="text-xs font-semibold uppercase tracking-[0.4em] text-amber-500">Voca English Test</p>
            <h2 className="mt-3 text-3xl font-extrabold text-slate-900">Kiểm tra trình độ miễn phí trước khi đăng ký</h2>
            <p className="mt-4 text-base text-slate-600">
              Bài test dựa trên chuẩn CEFR giúp bạn định vị cấp độ (350+ → 950+), hiểu điểm mạnh/yếu ở từng kỹ năng và chọn khóa phù hợp.
            </p>
            <div className="mt-6 flex flex-wrap gap-3">
              <Link
                to="/placement-test"
                className="inline-flex items-center rounded-full bg-amber-500 px-6 py-3 text-sm font-semibold text-white transition hover:bg-amber-600"
              >
                Làm bài kiểm tra ngay →
              </Link>
              <p className="text-sm text-slate-400">Thời lượng ~ 8 phút · 12 câu hỏi</p>
            </div>
          </div>
          <div className="flex flex-1 flex-col justify-between bg-[#fff3e1] p-8">
            <div className="rounded-3xl bg-white/80 p-6 text-center shadow-lg">
              <p className="text-sm font-semibold text-amber-500">Trình độ của bạn</p>
              <div className="mt-4 grid grid-cols-4 gap-3 text-xs font-semibold text-slate-500">
                {['A1', 'A2', 'B1', 'B2'].map((label) => (
                  <div key={label} className="rounded-2xl border border-amber-100 bg-amber-50 py-2">
                    {label}
                  </div>
                ))}
              </div>
              <p className="mt-4 text-xs text-slate-400">Mỗi cấp độ gồm nghe, đọc, nói và vốn từ.</p>
            </div>
            <div className="mt-6 rounded-3xl bg-white/90 p-6 text-sm text-slate-600">
              <p className="font-semibold text-slate-900">Giáo viên có thể:</p>
              <ul className="mt-2 space-y-2">
                <li>• Đăng bộ đề dưới dạng hình ảnh, audio.</li>
                <li>• Thiết lập câu hỏi trắc nghiệm, mô tả ngắn.</li>
                <li>• Nhận kết quả và gợi ý khóa học theo band 350+ → 950+.</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function TeacherShowcase() {
  return (
    <section className="bg-gradient-to-br from-indigo-50 via-white to-blue-50 py-16">
      <div className="mx-auto max-w-6xl px-4">
        <div className="grid gap-8 rounded-[36px] border border-white bg-white/90 p-10 shadow-[0_35px_80px_rgba(37,99,235,0.15)] lg:grid-cols-2">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.4em] text-blue-500">Đội ngũ mentor</p>
            <h2 className="mt-3 text-3xl font-bold text-slate-900">Giảng viên đồng hành cùng bạn</h2>
            <p className="mt-4 text-sm text-slate-600">
              Hơn 100 giảng viên bản ngữ và Việt Nam thiết kế bài học, chấm bài và tổ chức lớp trực tuyến mỗi tuần. Học
              viên nhận feedback cá nhân và lịch luyện tập theo mục tiêu.
            </p>
            <div className="mt-6 grid grid-cols-3 gap-4 text-center text-sm">
              <div className="rounded-2xl bg-blue-50 p-4">
                <p className="text-3xl font-extrabold text-blue-600">12</p>
                <p className="mt-2 text-xs text-slate-500">Năm đồng hành</p>
              </div>
              <div className="rounded-2xl bg-blue-50 p-4">
                <p className="text-3xl font-extrabold text-blue-600">2M+</p>
                <p className="mt-2 text-xs text-slate-500">Học viên</p>
              </div>
              <div className="rounded-2xl bg-blue-50 p-4">
                <p className="text-3xl font-extrabold text-blue-600">500+</p>
                <p className="mt-2 text-xs text-slate-500">Khóa theo kỹ năng</p>
              </div>
            </div>
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            {["https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=400&q=60",
              "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=400&q=60"].map((src, idx) => (
              <div key={idx} className="rounded-3xl border border-slate-200 bg-white p-4 shadow-lg">
                <img src={src} alt={`mentor-${idx}`} className="h-32 w-full rounded-2xl object-cover" />
                <p className="mt-3 text-base font-semibold text-slate-900">{idx ? "Đào Quỳnh Anh" : "Linh Nguyễn"}</p>
                <p className="text-xs text-slate-500">{idx ? "IELTS 8.0 · Pronunciation" : "TOEIC 950 · Communication"}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

function Trusted() {
  const logos = ["DanTri", "VnExpress", "Techfest", "Google for Startups"];
  return (
    <section className="bg-white py-12">
      <div className="mx-auto max-w-6xl px-4">
        <p className="text-center text-xs font-semibold uppercase tracking-[0.4em] text-slate-400">Được tin tưởng bởi</p>
        <div className="mt-6 grid grid-cols-2 gap-6 text-center text-sm text-slate-500 sm:grid-cols-4">
          {logos.map((logo) => (
            <div key={logo} className="rounded-2xl border border-slate-100 bg-slate-50 py-4">
              {logo}
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function Features() {
  const items = [
    "Lộ trình cá nhân hóa bởi AI & mentor",
    "Hơn 2.000 bài học tương tác",
    "Ứng dụng ghi nhớ từ vựng, kiểm tra phát âm",
    "Feedback trực tiếp từ giảng viên",
  ];
  return (
    <section className="bg-slate-950 py-16 text-white">
      <div className="mx-auto max-w-6xl px-4">
        <div className="rounded-[36px] border border-white/10 bg-gradient-to-br from-slate-900 to-slate-800 p-10">
          <p className="text-xs font-semibold uppercase tracking-[0.4em] text-amber-400">Voca platform</p>
          <h2 className="mt-3 text-3xl font-bold">Mọi công cụ bạn cần để làm chủ tiếng Anh</h2>
          <div className="mt-6 grid gap-6 md:grid-cols-2">
            {items.map((item) => (
              <div key={item} className="rounded-2xl border border-white/15 bg-white/5 p-5">
                <p className="font-semibold">{item}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

function Testimonials() {
  const data = [
    { name: "Ngọc Anh · Product Manager", feedback: "Sau 3 tháng luyện IELTS với mentor, mình tăng 1.5 band nhờ lộ trình và feedback rõ ràng." },
    { name: "Huyền My · Sinh viên", feedback: "Các bài flashcard, listening thực tế giúp mình tự tin phỏng vấn học bổng." },
  ];
  return (
    <section className="bg-white py-16">
      <div className="mx-auto max-w-6xl px-4">
        <SectionHeading eyebrow="Cảm nhận" title="Học viên nói gì" subtitle="Những chia sẻ thật từ cộng đồng VOCA." center />
        <div className="mt-8 grid gap-6 md:grid-cols-2">
          {data.map((item) => (
            <div key={item.name} className="rounded-3xl border border-slate-100 bg-white p-6 shadow-[0_25px_60px_rgba(15,23,42,0.08)]">
              <p className="text-sm text-slate-600">“{item.feedback}”</p>
              <p className="mt-3 text-sm font-semibold text-slate-900">{item.name}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function CtaBanner() {
  return (
    <section className="bg-gradient-to-r from-primary-600 to-amber-500 py-16 text-white">
      <div className="mx-auto flex max-w-6xl flex-col items-center gap-6 px-4 text-center">
        <p className="text-sm font-semibold uppercase tracking-[0.4em] text-white/80">Bắt đầu hành trình</p>
        <h2 className="text-3xl font-bold">Sẵn sàng đồng hành cùng bạn</h2>
        <p className="max-w-3xl text-sm text-white/80">
          Đăng ký tài khoản để nhận lộ trình học cá nhân, mentor theo sát từng tuần và kho bài luyện tập phong phú.
        </p>
        <div className="flex flex-wrap gap-3">
          <Link to="/register" className="rounded-full bg-white px-6 py-3 text-sm font-semibold text-primary-600 shadow-lg">
            Tạo tài khoản miễn phí
          </Link>
          <Link to="/courses" className="rounded-full border border-white/70 px-6 py-3 text-sm font-semibold text-white">
            Khám phá khóa học
          </Link>
        </div>
      </div>
    </section>
  );
}
