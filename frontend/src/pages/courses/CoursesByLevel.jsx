import { useEffect, useMemo, useState } from "react";
import CourseShowcaseCard from "../../components/CourseShowcaseCard";
import { resolveIsFree } from "../../utils/price";
import { API_BASE_URL } from "../../api/httpClient";

const API_BASE = API_BASE_URL;

function resolveThumb(u) {
  if (!u) return null;
  let s = String(u).trim().replace(/\\/g, "/");
  if (/^https?:\/\//i.test(s) || s.startsWith("data:")) return s;
  if (!s.startsWith("/")) s = `/${s}`;
  return `${API_BASE}${s}`;
}

function normalize(str) {
  return (str || "").toString().normalize("NFD").replace(/\p{Diacritic}/gu, "").toLowerCase().trim();
}

function levelOrder(lv) {
  if (lv == null) return 99;
  const s = normalize(lv);
  const digits = s.match(/(\d{2,3})/);
  if (digits) return parseInt(digits[1], 10);
  if (["co ban", "beginner", "basic", "foundation"].includes(s)) return 1;
  if (["trung cap", "intermediate", "middle", "medium"].includes(s)) return 2;
  if (["nang cao", "advanced", "expert"].includes(s)) return 3;
  return 50;
}

function levelLabel(lv) {
  if (lv) {
    const digits = String(lv).match(/(\d{2,3})/);
    if (digits) return ${digits[1]}+;
  }
  const o = levelOrder(lv);
  if (o === 1) return "Cơ bản";
  if (o === 2) return "Trung cấp";
  if (o === 3) return "Nâng cao";
  return lv || "Tổng hợp";
}

function formatDurationLabel(durationWeeks) {
  if (durationWeeks == null) return null;
  const num = Number(durationWeeks);
  if (!Number.isFinite(num) || num <= 0) return null;
  if (num < 1) {
    const days = Math.max(1, Math.round(num * 7));
    return `${days} ngày`;
  }
  return `${num} tuần`;
}

export default function CoursesByLevel() {
  const [courses, setCourses] = useState([]);
  const [dir, setDir] = useState("asc");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      setLoading(true);
      const endpoints = ["/api/public/courses", "/api/public/courses-sql"];
      let list = [];
      for (const ep of endpoints) {
        try {
          const res = await fetch(`${API_BASE}${ep}`, { headers: { Accept: "application/json" } });
          if (!res.ok) continue;
          const data = await res.json();
          if (Array.isArray(data)) {
            list = data.map((c) => ({
              id: c.id ?? c.course_id,
              title: c.title ?? c.name ?? c.course_title,
              slug: c.slug ?? c.course_slug ?? c.code,
              level: c.level ?? c.level_name ?? c.difficulty,
              teacherName: c.teacher_name ?? c.teacherName ?? c.created_by_name ?? null,
              durationWeeks: c.duration_weeks ?? c.durationWeeks ?? c.weeks ?? null,
              lessons: c.lessons_count ?? c.lessonsCount ?? c.total_lessons ?? null,
              thumbnail:
                c.thumbnail_url ??
                c.thumbnailUrl ??
                c.thumbnail_path ??
                c.thumbnailPath ??
                c.image_url ??
                c.imageUrl ??
                c.cover_url ??
                c.coverUrl ??
                null,
              price: c.price ?? c.tuition ?? c.amount ?? null,
              isFree: resolveIsFree(
                c.price ?? c.tuition ?? c.amount ?? null,
                c.is_free ?? c.isFree,
              ),
            }));
            break;
          }
        } catch (error) {
          console.warn("Load courses failed", error);
        }
      }
      setCourses(list);
      setLoading(false);
    })();
  }, []);

  const items = useMemo(() => {
    const arr = [...courses];
    arr.sort((a, b) => {
      const A = levelOrder(a.level);
      const B = levelOrder(b.level);
      return dir === "asc" ? A - B : B - A;
    });
    return arr;
  }, [courses, dir]);

  return (
    <section id="courses" className="bg-white py-16">
      <div className="mx-auto flex max-w-7xl flex-col gap-10 px-4">
        <div className="text-center">
          <p className="text-xs font-semibold uppercase tracking-[0.3em] text-primary-600">Lộ trình học</p>
          <h2 className="mt-3 text-3xl font-extrabold text-stone-900">Khóa học phù hợp từng cấp độ</h2>
          <p className="mt-2 text-base text-stone-600">
            Tập trung xây nền tảng vững chắc, cập nhật công nghệ mới và mentor đồng hành trong suốt hành trình.
          </p>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-stone-200 bg-stone-50/60 px-4 py-3 text-sm text-stone-700">
          <div className="flex flex-wrap items-center gap-3">
            <span className="font-medium text-stone-900">Sắp xếp theo cấp độ</span>
            <select
              value={dir}
              onChange={(e) => setDir(e.target.value)}
              className="rounded-xl border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200"
            >
              <option value="asc">Từ cơ bản đến nâng cao</option>
              <option value="desc">Từ nâng cao đến cơ bản</option>
            </select>
          </div>
          <span className="text-stone-500">{items.length} khóa học</span>
        </div>

        {loading ? (
          <div className="grid animate-pulse gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 3 }).map((_, idx) => (
              <div key={idx} className="rounded-3xl border border-stone-200 bg-stone-50 p-6" />
            ))}
          </div>
        ) : (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {items.length === 0 && (
              <div className="col-span-full rounded-2xl border border-dashed border-stone-200 bg-stone-50/60 px-6 py-12 text-center text-stone-500">
                Hiện chưa có khóa học nào sẵn sàng.
              </div>
            )}
            {items.map((it) => (
              <CourseShowcaseCard
                key={it.id ?? it.slug}
                id={it.id}
                slug={it.slug}
                title={it.title || "Khóa học"}
                thumbnail={resolveThumb(it.thumbnail)}
                level={levelLabel(it.level)}
                teacherName={it.teacherName}
                lessonsCount={it.lessons}
                durationLabel={formatDurationLabel(it.durationWeeks)}
                price={it.price}
                isFree={Boolean(it.isFree)}
              />
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
