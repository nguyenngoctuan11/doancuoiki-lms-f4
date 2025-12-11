import { useEffect, useMemo, useState } from "react";
import CourseShowcaseCard from "../components/CourseShowcaseCard";
import { resolveIsFree } from "../utils/price";
import { API_BASE_URL } from "../api/httpClient";

const API_BASE = API_BASE_URL;

function resolveThumb(u) {
  if (!u) return null;
  let s = String(u).trim().replace(/\\/g, "/");
  if (/^https?:\/\//i.test(s) || s.startsWith("data:")) return s;
  if (!s.startsWith("/")) s = `/${s}`;
  return `${API_BASE}${s}`;
}

function formatDurationFromMinutes(minutes) {
  if (minutes == null) return null;
  const totalMinutes = Number(minutes);
  if (!Number.isFinite(totalMinutes) || totalMinutes <= 0) return null;
  const hours = Math.floor(totalMinutes / 60);
  const mins = Math.round(totalMinutes % 60);
  if (hours === 0) return `${mins} phút`;
  if (mins === 0) return `${hours} giờ`;
  return `${hours}h${String(mins).padStart(2, "0")}p`;
}

function normalizeText(str) {
  return (str || "")
    .toString()
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .toLowerCase()
    .trim();
}

const LEGACY_LEVEL_MAP = {
  beginner: 350,
  "co ban": 350,
  basic: 350,
  foundation: 350,
  intermediate: 550,
  "trung cap": 550,
  medium: 550,
  advanced: 750,
  "nang cao": 750,
  expert: 850,
};

function parseLevelScore(level) {
  if (!level) return null;
  const match = String(level).match(/(\d{2,3})/);
  if (match) {
    const score = Number(match[1]);
    if (Number.isFinite(score)) return score;
  }
  const normalized = normalizeText(level);
  return LEGACY_LEVEL_MAP[normalized] ?? null;
}

const LEVEL_SEGMENTS = [
  { id: "350", label: "350+", hint: "Mất gốc, làm quen", min: 350, max: 450 },
  { id: "450", label: "450+", hint: "Sơ trung cấp, tăng điểm nhanh", min: 450, max: 550 },
  { id: "550", label: "550+", hint: "Trung cấp, tự tin thi TOEIC", min: 550, max: 650 },
  { id: "650", label: "650+", hint: "Trung cao, mục tiêu IELTS 6.5", min: 650, max: 750 },
  { id: "750", label: "750+", hint: "Nâng cao, giao tiếp công sở", min: 750, max: 850 },
  { id: "850", label: "850+", hint: "Chuyên sâu, tiếng Anh học thuật", min: 850, max: 950 },
  { id: "950", label: "950+", hint: "Làm chủ song ngữ", min: 950, max: 1000 },
];

const DISCOVERY_TAGS = [
  {
    id: "foundation",
    label: "Kiến thức nền",
    hint: "Grammar, vocabulary, mất gốc",
    matcher: (course) => {
      const score = parseLevelScore(course.level);
      if (score && score <= 450) return true;
      const txt = normalizeText(`${course.title || ""} ${course.shortDesc || ""}`);
      return /nen tang|co ban|foundation|grammar|vocabulary/.test(txt);
    },
  },
  {
    id: "skills",
    label: "Kỹ năng",
    hint: "Speaking, listening, presentation",
    matcher: (course) => {
      const txt = normalizeText(`${course.title || ""} ${course.shortDesc || ""}`);
      return /ky nang|skill|speaking|listening|writing|reading|communication|giao tiep|presentation/.test(txt);
    },
  },
  {
    id: "advanced",
    label: "Cấp độ",
    hint: "Khóa học nâng cao",
    matcher: (course) => {
      const score = parseLevelScore(course.level);
      if (score && score >= 650) return true;
      const txt = normalizeText(`${course.title || ""} ${course.shortDesc || ""} ${course.level || ""}`);
      return /advanced|nang cao|master|cap do/.test(txt);
    },
  },
  {
    id: "needs",
    label: "Nhu cầu",
    hint: "Business, du lịch, đi làm",
    matcher: (course) => {
      const txt = normalizeText(`${course.title || ""} ${course.shortDesc || ""}`);
      return /giao tiep|business|cong viec|du hoc|di lam|meeting|van phong|travel/.test(txt);
    },
  },
  {
    id: "certificate",
    label: "Chứng chỉ",
    hint: "TOEIC, IELTS, CEFR",
    matcher: (course) => {
      const txt = normalizeText(`${course.title || ""} ${course.shortDesc || ""}`);
      return /toeic|ielts|toefl|chung chi|certificate|cefr/.test(txt);
    },
  },
  {
    id: "student",
    label: "Học sinh",
    hint: "Thi THPT, sinh viên",
    matcher: (course) => {
      const txt = normalizeText(`${course.title || ""} ${course.shortDesc || ""}`);
      return /hoc sinh|sinh vien|teen|thpt|thi dai hoc|cap 3/.test(txt);
    },
  },
];

const SKILL_FOCUS_TAGS = [
  {
    id: "vocabulary",
    label: "Từ vựng",
    matcher: (course) => {
      if (course.categories && course.categories.includes("vocabulary")) return true;
      const txt = normalizeText(`${course.title || ""} ${course.shortDesc || ""}`);
      return /(tu vung|tu vuong|từ vựng|vocabulary|lexical)/.test(txt);
    },
  },
  {
    id: "listening",
    label: "Nghe",
    matcher: (course) => {
      if (course.categories && course.categories.includes("listening")) return true;
      const txt = normalizeText(`${course.title || ""} ${course.shortDesc || ""}`);
      return /(listen|nghe|audio|listening)/.test(txt);
    },
  },
  {
    id: "speaking",
    label: "Nói",
    matcher: (course) => {
      if (course.categories && course.categories.includes("speaking")) return true;
      const txt = normalizeText(`${course.title || ""} ${course.shortDesc || ""}`);
      return /(speaking|noi|nói|conversation|pronounce|pronunciation)/.test(txt);
    },
  },
  {
    id: "writing",
    label: "Viết",
    matcher: (course) => {
      if (course.categories && course.categories.includes("writing")) return true;
      const txt = normalizeText(`${course.title || ""} ${course.shortDesc || ""}`);
      return /(writing|viet|viết|essay|composition)/.test(txt);
    },
  },
];

export default function Courses() {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState("");
  const [selectedTag, setSelectedTag] = useState(null);
  const [selectedLevel, setSelectedLevel] = useState(null);
  const [selectedSkillFocus, setSelectedSkillFocus] = useState(null);
  const [courseRatings, setCourseRatings] = useState({});
  const [ratingsLoading, setRatingsLoading] = useState(false);

  useEffect(() => {
    async function load() {
      setLoading(true);
      try {
        const res = await fetch(`${API_BASE}/api/public/courses-sql?status=published&limit=50`, {
          headers: { Accept: "application/json" },
        });
        if (!res.ok) throw new Error("Không thể tải danh sách khóa học");
        const data = await res.json();
        if (Array.isArray(data)) {
          const normalized = data.map((c) => ({
            id: c.id,
            slug: c.slug,
            title: c.title,
            level: c.level,
            teacherName: c.teacher_name ?? c.teacherName ?? c.created_by_name ?? null,
            lessons: c.lessons_count ?? c.lessonsCount ?? 0,
            thumbnail: resolveThumb(
              c.thumbnail_url ?? c.thumbnailUrl ?? c.thumbnail_path ?? c.thumbnailPath ?? null,
            ),
            price: c.price ?? null,
            isFree: resolveIsFree(c.price, c.is_free ?? c.isFree),
            durationLabel: formatDurationFromMinutes(
              c.total_minutes ?? c.totalMinutes ?? c.duration_minutes ?? c.durationMinutes ?? null,
            ),
            shortDesc: c.short_desc ?? c.shortDesc ?? "",
            categories: (() => {
              const raw = c.categories ?? c.category_slugs ?? c.categorySlugs ?? null;
              if (!raw) return [];
              if (Array.isArray(raw)) return raw.map((x) => normalizeText(x));
              return String(raw)
                .split(",")
                .map((x) => normalizeText(x))
                .filter(Boolean);
            })(),
          }));
          setCourses(normalized);
        } else {
          setCourses([]);
        }
      } catch (error) {
        console.error(error);
        setCourses([]);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  useEffect(() => {
    let alive = true;
    async function preloadRatings(courseList) {
      if (!courseList.length) {
        if (alive) setCourseRatings({});
        return;
      }
      setRatingsLoading(true);
      const responses = await Promise.allSettled(
        courseList.map(async (course) => {
          const res = await fetch(`${API_BASE}/api/course-reviews/course/${course.id}/summary`, {
            headers: { Accept: "application/json" },
          });
          if (!res.ok) throw new Error("Không thể tải đánh giá");
          const data = await res.json();
          return [course.id, { rating: data?.courseAverage ?? 0, ratingCount: data?.total ?? 0 }];
        }),
      );
      if (!alive) return;
      const next = {};
      responses.forEach((entry) => {
        if (entry.status === "fulfilled" && entry.value) {
          const [id, meta] = entry.value;
          next[id] = meta;
        }
      });
      setCourseRatings(next);
      setRatingsLoading(false);
    }
    preloadRatings(courses);
    return () => {
      alive = false;
    };
  }, [courses]);

  const filteredCourses = useMemo(() => {
    const normalizedQuery = normalizeText(query);
    return courses.filter((course) => {
      const searchable = normalizeText(
        `${course.title || ""} ${course.shortDesc || ""} ${course.level || ""} ${course.teacherName || ""}`,
      );
      if (normalizedQuery && !searchable.includes(normalizedQuery)) {
        return false;
      }
      if (selectedTag) {
        const tag = DISCOVERY_TAGS.find((t) => t.id === selectedTag);
        if (tag && !tag.matcher(course)) return false;
      }
      if (selectedSkillFocus) {
        const skill = SKILL_FOCUS_TAGS.find((s) => s.id === selectedSkillFocus);
        if (skill && !skill.matcher(course)) return false;
      }
      if (selectedLevel) {
        const segment = LEVEL_SEGMENTS.find((seg) => seg.id === selectedLevel);
        if (segment) {
          const score = parseLevelScore(course.level);
          if (score == null) return false;
          if (segment.min != null && score < segment.min) return false;
          if (segment.max != null && score >= segment.max) return false;
        }
      }
      return true;
    });
  }, [courses, query, selectedLevel, selectedTag, selectedSkillFocus]);

  const decoratedCourses = useMemo(() => {
    return filteredCourses.map((course) => {
      const meta = courseRatings[course.id] || {};
      return {
        ...course,
        rating: meta.rating ?? null,
        ratingCount: meta.ratingCount ?? 0,
      };
    });
  }, [filteredCourses, courseRatings]);

  const { featuredCourses, proCourses, freeCourses } = useMemo(() => {
    const featured = [];
    const pro = [];
    const free = [];
    decoratedCourses.forEach((course) => {
      const avg = course.rating ?? 0;
      const total = course.ratingCount ?? 0;
      if (avg >= 4 && total > 0) {
        featured.push(course);
      } else if (!course.isFree) {
        pro.push(course);
      } else {
        free.push(course);
      }
    });
    return { featuredCourses: featured, proCourses: pro, freeCourses: free };
  }, [decoratedCourses]);

  const handleSearchSubmit = (event) => {
    event.preventDefault();
    setQuery((prev) => prev.trim());
  };

  const toggleTag = (id) => {
    setSelectedTag((prev) => (prev === id ? null : id));
  };

  const toggleLevel = (id) => {
    setSelectedLevel((prev) => (prev === id ? null : id));
  };

  const clearFilters = () => {
    setSelectedTag(null);
    setSelectedLevel(null);
    setSelectedSkillFocus(null);
    setQuery("");
  };

  const activeFilters = Boolean(query || selectedTag || selectedLevel || selectedSkillFocus);

  const renderCourseSection = (title, description, coursesList, badgeClass) => (
    <section className="mt-12 rounded-3xl border border-stone-200 bg-white/80 p-6 shadow-lg shadow-stone-900/5">
      <div className="flex flex-col gap-2 pb-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.3em] text-stone-400">{description}</p>
          <h2 className="text-2xl font-bold text-stone-900">{title}</h2>
        </div>
        <span className={`inline-flex items-center gap-2 rounded-full px-4 py-1.5 text-sm font-semibold ${badgeClass}`}>
          {coursesList.length} khóa học
        </span>
      </div>
      {coursesList.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-stone-200 bg-stone-50 px-6 py-10 text-center text-stone-500">
          Danh sách hiện trống. Hãy thử điều chỉnh bộ lọc hoặc quay lại sau nhé!
        </div>
      ) : (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {coursesList.map((course) => (
            <CourseShowcaseCard
              key={course.id}
              id={course.id}
              slug={course.slug}
              title={course.title}
              level={course.level}
              teacherName={course.teacherName}
              lessonsCount={course.lessons}
              thumbnail={course.thumbnail}
              price={course.price}
              isFree={course.isFree}
              durationLabel={course.durationLabel}
              rating={course.rating}
              ratingCount={course.ratingCount}
            />
          ))}
        </div>
      )}
    </section>
  );

  return (
    <section className="bg-gradient-to-b from-amber-50 via-white to-white">
      <div className="mx-auto max-w-7xl px-4 py-16">
        <div className="rounded-[40px] bg-gradient-to-r from-amber-500 via-orange-400 to-pink-400 p-8 text-white shadow-2xl">
          <p className="text-sm font-semibold uppercase tracking-[0.5em]">Lộ trình cá nhân hóa</p>
          <h1 className="mt-4 text-4xl font-black leading-tight sm:text-5xl">Danh sách khóa học</h1>
          <p className="mt-3 max-w-3xl text-lg text-white/90">
            Chọn cùng lúc nhiều bộ lọc, xem đánh giá sao và tìm khóa học phù hợp với mục tiêu ngôn ngữ của bạn.
            Các thầy cô đã sẵn sàng đồng hành với hành trình mới.
          </p>
          <div className="mt-6 flex flex-wrap gap-4 text-sm font-semibold">
            <span className="inline-flex items-center gap-2 rounded-full bg-white/15 px-4 py-1.5">
              🔥 {featuredCourses.length} khóa học nổi bật
            </span>
            <span className="inline-flex items-center gap-2 rounded-full bg-white/15 px-4 py-1.5">
              💼 {proCourses.length} khóa học Pro
            </span>
            <span className="inline-flex items-center gap-2 rounded-full bg-white/15 px-4 py-1.5">
              🎁 {freeCourses.length} khóa miễn phí
            </span>
          </div>
        </div>

        <div className="mt-10 grid gap-4 rounded-[32px] border border-white/60 bg-white/90 p-6 shadow-[0_30px_90px_-40px_rgba(15,23,42,0.35)] backdrop-blur">
          <form onSubmit={handleSearchSubmit} className="flex flex-col gap-4">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
              <div className="flex flex-1 items-center gap-3 rounded-2xl border border-stone-300 bg-stone-50 px-4 py-3 focus-within:border-primary-500 focus-within:ring-2 focus-within:ring-primary-100">
                <svg viewBox="0 0 24 24" className="h-5 w-5 text-stone-500" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="11" cy="11" r="7" />
                  <path d="m20 20-4.35-4.35" />
                </svg>
                <input
                  type="search"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Tìm khóa học..."
                  className="flex-1 border-none bg-transparent text-base text-stone-800 placeholder-stone-400 outline-none"
                />
              </div>
              <button type="submit" className="btn btn-primary min-w-[140px]">
                Tìm kiếm
              </button>
            </div>
            <div className="flex flex-wrap gap-2">
              {DISCOVERY_TAGS.map((tag) => {
                const active = selectedTag === tag.id;
                return (
                  <button
                    type="button"
                    key={tag.id}
                    onClick={() => toggleTag(tag.id)}
                    title={tag.hint}
                    className={`rounded-full border px-4 py-1.5 text-sm font-medium transition ${
                      active
                        ? "border-primary-500 bg-primary-50 text-primary-700 shadow-sm"
                        : "border-stone-200 text-stone-600 hover:border-stone-300"
                    }`}
                  >
                    {tag.label}
                  </button>
                );
              })}
            </div>
            <div className="flex flex-wrap gap-2 pb-2">
              {SKILL_FOCUS_TAGS.map((skill) => {
                const activeSkill = selectedSkillFocus === skill.id;
                return (
                  <button
                    key={skill.id}
                    type="button"
                    className={`rounded-full border px-4 py-1.5 text-sm font-medium transition ${
                      activeSkill
                        ? "border-primary-500 bg-primary-50 text-primary-700 shadow-sm"
                        : "border-stone-200 text-stone-600 hover:border-stone-300"
                    }`}
                    onClick={() => setSelectedSkillFocus((prev) => (prev === skill.id ? null : skill.id))}
                  >
                    {skill.label}
                  </button>
                );
              })}
            </div>
            <div className="flex flex-wrap items-center gap-2 text-sm text-stone-600">
              <span className="text-xs font-semibold uppercase tracking-[0.3em] text-stone-500">Trình độ TOEIC</span>
              {LEVEL_SEGMENTS.map((segment) => {
                const active = selectedLevel === segment.id;
                return (
                  <button
                    type="button"
                    key={segment.id}
                    onClick={() => toggleLevel(segment.id)}
                    title={segment.hint}
                    className={`rounded-full px-3 py-1.5 text-sm font-semibold transition ${
                      active ? "bg-emerald-100 text-emerald-700 shadow-inner" : "bg-stone-100 text-stone-600 hover:bg-stone-200"
                    }`}
                  >
                    {segment.label}
                  </button>
                );
              })}
              {activeFilters && (
                <button
                  type="button"
                  onClick={clearFilters}
                  className="rounded-full px-3 py-1.5 text-xs font-semibold text-primary-600 hover:bg-primary-50"
                >
                  Xóa bộ lọc
                </button>
              )}
              <span className="ml-auto text-xs text-stone-500">
                {filteredCourses.length} khóa học phù hợp
              </span>
            </div>
          </form>
        </div>

        {loading ? (
          <div className="mt-12 grid animate-pulse gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, idx) => (
              <div key={idx} className="h-72 rounded-3xl border border-stone-200 bg-stone-50" />
            ))}
          </div>
        ) : (
          <>
            {renderCourseSection(
              "Khóa học nổi bật",
              "Đánh giá từ 4 sao trở lên",
              featuredCourses,
              "bg-white text-amber-600 shadow-inner",
            )}
            {renderCourseSection(
              "Khóa học Pro",
              "Có học phí - trải nghiệm chuyên sâu",
              proCourses,
              "bg-amber-100 text-amber-700",
            )}
            {renderCourseSection(
              "Khóa học miễn phí",
              "Học thử thoải mái, mở rộng kiến thức",
              freeCourses,
              "bg-emerald-100 text-emerald-700",
            )}
            {ratingsLoading && (
              <p className="mt-6 text-center text-sm text-stone-500">Đang cập nhật điểm đánh giá...</p>
            )}
          </>
        )}
      </div>
    </section>
  );
}
