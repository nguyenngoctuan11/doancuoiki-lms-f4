import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { normalizePriceValue } from "../utils/price";
import { API_BASE_URL } from "../api/httpClient";

const moneyFormatter = new Intl.NumberFormat("vi-VN");

function formatMoney(value) {
  const num = normalizePriceValue(value);
  if (num === null) return null;
  return `${moneyFormatter.format(num)}₫`;
}

const fallbackGradients = [
  "from-primary-600 to-primary-400",
  "from-stone-800 to-stone-600",
  "from-amber-500 to-orange-400",
  "from-sky-500 to-indigo-500",
];

export default function CourseShowcaseCard({
  id,
  slug,
  title,
  thumbnail,
  level,
  lessonsCount,
  durationLabel,
  studentsCount,
  price,
  isFree = false,
  teacherName,
  rating: externalRating,
  ratingCount: externalRatingCount,
}) {
  const [displayRating, setDisplayRating] = useState(externalRating ?? null);
  const [displayRatingCount, setDisplayRatingCount] = useState(externalRatingCount ?? 0);
  const API_BASE = API_BASE_URL;

  useEffect(() => {
    setDisplayRating(externalRating ?? null);
    setDisplayRatingCount(externalRatingCount ?? 0);
  }, [externalRating, externalRatingCount]);

  useEffect(() => {
    if (externalRating !== undefined || externalRatingCount !== undefined) return;
    let alive = true;
    async function loadRating() {
      if (!id) return;
      try {
        const res = await fetch(`${API_BASE}/api/course-reviews/course/${id}/summary`);
        if (!res.ok) return;
        const data = await res.json();
        if (!alive) return;
        setDisplayRating(data?.courseAverage ?? null);
        setDisplayRatingCount(data?.total ?? 0);
      } catch {
        if (!alive) return;
        setDisplayRating(null);
        setDisplayRatingCount(0);
      }
    }
    loadRating();
    return () => {
      alive = false;
    };
  }, [id, API_BASE, externalRating, externalRatingCount]);

  const href = `/courses/${encodeURIComponent(slug || id)}`;
  const formattedPrice = formatMoney(price);
  const gradientIndex = Math.abs((slug || `${id || 0}`).charCodeAt(0) || 0) % fallbackGradients.length;
  const fallbackBg = fallbackGradients[gradientIndex];

  return (
    <article className="group relative flex flex-col gap-4 rounded-3xl border border-stone-200 bg-white p-5 shadow-sm transition hover:-translate-y-1 hover:shadow-lg">
      <Link to={href} className="block overflow-hidden rounded-2xl border border-stone-100">
        <div className="relative aspect-[4/3] w-full overflow-hidden rounded-2xl bg-stone-50">
          {thumbnail ? (
            <img
              src={thumbnail}
              alt={title}
              className="h-full w-full object-cover transition duration-500 group-hover:scale-105"
              loading="lazy"
            />
          ) : (
            <div className={`h-full w-full bg-gradient-to-br ${fallbackBg}`} />
          )}
          {!isFree && (
            <div className="absolute left-4 top-4 inline-flex items-center gap-1 rounded-full border border-white/30 bg-white/80 px-3 py-1 text-xs font-semibold text-amber-600 shadow-sm backdrop-blur">
              <span aria-hidden="true">★</span>
              <span>Pro</span>
            </div>
          )}
        </div>
      </Link>

      <div className="flex flex-col gap-3">
        <div className="flex flex-wrap items-start gap-3">
          <div className="flex-1 space-y-1">
            {level && (
              <span className="inline-flex items-center rounded-full border border-primary-100 bg-primary-50 px-3 py-1 text-xs font-semibold text-primary-700">
                {level}
              </span>
            )}
            <Link to={href} className="block text-lg font-semibold text-stone-900 transition hover:text-primary-700">
              {title}
            </Link>
          </div>

          <div className="text-right">
            {isFree ? (
              <span className="inline-flex rounded-xl bg-emerald-50 px-3 py-1 text-lg font-black text-emerald-600">
                Miễn phí
              </span>
            ) : (
              <span className="text-xl font-black text-primary-600">
                {formattedPrice || (typeof price === "string" ? price : "Đang cập nhật")}
              </span>
            )}
          </div>
        </div>

        <div className="flex flex-wrap gap-4 text-sm text-stone-600">
          <span className="inline-flex items-center gap-1 font-semibold text-amber-500">
            {displayRating ? (
              <>
                <svg viewBox="0 0 24 24" className="h-4 w-4" fill="currentColor">
                  <path d="m12 2.5 2.9 6.1 6.7.9-4.9 4.7 1.2 6.8-5.9-3.2-5.9 3.2 1.2-6.8-4.9-4.7 6.7-.9z" />
                </svg>
                <span>{displayRating.toFixed(1)} / 5</span>
                <span className="text-xs text-stone-500">({displayRatingCount})</span>
              </>
            ) : (
              <span className="text-xs text-stone-400">Chưa có đánh giá</span>
            )}
          </span>
          {lessonsCount ? (
            <span className="inline-flex items-center gap-1 font-semibold text-stone-700">
              <svg
                viewBox="0 0 24 24"
                className="h-4 w-4 text-primary-600"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <path d="M4 6h16M4 12h16M4 18h10" />
              </svg>
              {lessonsCount} bài học
            </span>
          ) : null}
          {durationLabel ? (
            <span className="inline-flex items-center gap-1">
              <svg
                viewBox="0 0 24 24"
                className="h-4 w-4 text-primary-600"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <circle cx="12" cy="12" r="9" />
                <path d="m12 7 0 5 3 3" />
              </svg>
              {durationLabel}
            </span>
          ) : null}
          {studentsCount ? (
            <span className="inline-flex items-center gap-1">
              <svg
                viewBox="0 0 24 24"
                className="h-4 w-4 text-primary-600"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <path d="M12 13c2.761 0 5-2.239 5-5S14.761 3 12 3 7 5.239 7 8s2.239 5 5 5Zm0 2c-4.418 0-8 2.239-8 5v1h16v-1c0-2.761-3.582-5-8-5Z" />
              </svg>
              {studentsCount.toLocaleString("vi-VN")} học viên
            </span>
          ) : null}
        </div>
      </div>

      <div className="flex items-center justify-between border-t border-dashed border-stone-200 pt-4 text-sm">
        <span className="text-stone-500">
          {teacherName ? `GV: ${teacherName}` : "Học linh hoạt & Mentor đồng hành"}
        </span>
        <Link
          to={href}
          className="inline-flex items-center gap-1 rounded-full border border-primary-200 px-4 py-1.5 text-primary-700 transition hover:border-primary-600 hover:bg-primary-50"
        >
          Xem chi tiết
          <svg viewBox="0 0 24 24" className="h-3.5 w-3.5" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="m9 6 6 6-6 6" />
          </svg>
        </Link>
      </div>
    </article>
  );
}
