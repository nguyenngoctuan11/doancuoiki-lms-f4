import React, { useMemo, useState } from "react";
import SectionHeading from "../components/SectionHeading";

const focusOptions = [
  {
    id: "vocabulary",
    label: "Từ vựng",
    caption: "Xây dựng vốn từ theo chủ đề, cụm từ giao tiếp và thành ngữ.",
  },
  {
    id: "listening",
    label: "Nghe",
    caption: "Luyện nghe native, podcast và phản xạ câu hỏi theo chủ đề.",
  },
  {
    id: "speaking",
    label: "Nói",
    caption: "Phản xạ hội thoại, phát âm chuẩn và luyện giọng trình bày chuyên nghiệp.",
  },
  {
    id: "writing",
    label: "Viết",
    caption: "Cấu trúc câu, email chuyên nghiệp và viết luận tiếng Anh học thuật.",
  },
];

const levelSegments = [
  { id: "beginner", label: "350+ / Beginner", hint: "Khởi đầu, xây dựng nền từ vựng & ngữ pháp" },
  {
    id: "intermediate",
    label: "550+ / Intermediate",
    hint: "Luyện giao tiếp tự tin, đọc hiểu và luyện đề TOEIC cơ bản",
  },
  {
    id: "upper",
    label: "750+ / Upper",
    hint: "Chuẩn bị IELTS 6.5-7.5, TOEIC 750+ và kỹ năng học thuật",
  },
  {
    id: "advanced",
    label: "850+ / Advanced",
    hint: "Phản biện, thuyết trình, viết luận chuyên sâu và giao tiếp chuyên nghiệp",
  },
];

const guidanceChips = [
  "Khoá học nền tảng",
  "Lộ trình chữa đề",
  "Speaking & Pronunciation",
  "Listening chuyên sâu",
  "Viết luận học thuật",
  "Luyện chứng chỉ (IELTS/TOEIC)",
];

export default function Survey() {
  const [focus, setFocus] = useState(focusOptions[0].id);
  const [level, setLevel] = useState(levelSegments[0].id);

  const recommendation = useMemo(() => {
    const focusLabel = focusOptions.find((item) => item.id === focus)?.label;
    const levelLabel = levelSegments.find((item) => item.id === level)?.label;
    return {
      headline: focusLabel ? `${focusLabel} chuyên sâu` : "Lộ trình tiếng Anh",
      detail: levelLabel ? `Cấp độ ${levelLabel}` : "Đề xuất trình độ phù hợp",
      badge: `${levelLabel ?? "350+"}`,
    };
  }, [focus, level]);

  const handleSubmit = (event) => {
    event.preventDefault();
    // placeholder: actual submission not implemented
  };

  return (
    <div className="min-h-screen bg-[#f8f8f8] px-4 pb-16 pt-10">
      <SectionHeading
        eyebrow="KHẢO SÁT LỘ TRÌNH"
        title="Định hướng lộ trình tiếng Anh chuyên sâu"
        subtitle="Chọn kỹ năng và trình độ hiện tại để hệ thống gợi ý lộ trình học hành của bạn."
        center
      />

      <div className="mx-auto max-w-5xl space-y-10">
        <form onSubmit={handleSubmit} className="space-y-6 rounded-[32px] border border-white bg-white p-8 shadow-[0_25px_60px_rgba(15,23,42,0.08)]">
          <div className="space-y-3">
            <p className="text-sm font-semibold text-slate-600">Bạn đang tập trung vào kỹ năng nào?</p>
            <div className="grid gap-4 sm:grid-cols-2">
              {focusOptions.map((option) => (
                <button
                  key={option.id}
                  type="button"
                  onClick={() => setFocus(option.id)}
                  className={`flex flex-col gap-2 rounded-2xl border px-6 py-4 text-left transition hover:border-slate-400 ${
                    focus === option.id ? "border-amber-500 bg-amber-50" : "border-slate-200 bg-white"
                  }`}
                >
                  <span className="text-base font-semibold text-slate-900">{option.label}</span>
                  <span className="text-sm text-slate-500">{option.caption}</span>
                </button>
              ))}
            </div>
          </div>
          <div className="space-y-3">
            <p className="text-sm font-semibold text-slate-600">Bạn đang ở trình độ nào?</p>
            <div className="grid gap-4 sm:grid-cols-2">
              {levelSegments.map((segment) => (
                <button
                  key={segment.id}
                  type="button"
                  onClick={() => setLevel(segment.id)}
                  className={`flex flex-col gap-2 rounded-2xl border px-6 py-4 text-left transition hover:border-slate-400 ${
                    level === segment.id ? "border-amber-500 bg-amber-50" : "border-slate-200 bg-white"
                  }`}
                >
                  <span className="text-base font-semibold text-slate-900">{segment.label}</span>
                  <span className="text-sm text-slate-500">{segment.hint}</span>
                </button>
              ))}
            </div>
          </div>
          <div className="flex flex-wrap items-center gap-4 pt-4">
            <button
              type="submit"
              className="inline-flex items-center justify-center rounded-full bg-stone-900 px-6 py-3 text-sm font-semibold text-white transition hover:bg-stone-800"
            >
              Đề xuất lộ trình
            </button>
            <span className="text-sm text-slate-500">Bạn có thể điều chỉnh sau khi xem kết quả.</span>
          </div>
        </form>

        <div className="rounded-[32px] border border-slate-200 bg-white/80 p-6 shadow-lg">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.4em] text-amber-500">Đề xuất ngay</p>
              <h3 className="mt-1 text-2xl font-bold text-slate-900">{recommendation.headline}</h3>
              <p className="text-sm text-slate-500">{recommendation.detail}</p>
            </div>
            <span className="rounded-2xl border border-amber-200 px-4 py-1 text-sm font-semibold text-amber-600">
              {recommendation.badge}
            </span>
          </div>
          <div className="mt-6 flex flex-wrap gap-3 text-sm text-slate-600">
            {guidanceChips.map((chip) => (
              <span key={chip} className="rounded-full border border-slate-200 px-4 py-1">
                {chip}
              </span>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
