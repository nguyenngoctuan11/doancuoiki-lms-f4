import React, { useMemo } from "react";

export default function LessonSidebar({ modules = [], activeLessonId, onSelect, completedLessonIds = [] }) {
  const completedSet = useMemo(() => new Set(completedLessonIds || []), [completedLessonIds]);
  return (
    <aside className="w-full md:w-80 border border-stone-200 rounded-xl overflow-hidden bg-white h-fit">
      <div className="px-4 py-3 border-b border-stone-200 font-semibold text-stone-900 bg-stone-50">Nội dung khóa học</div>
      <div className="divide-y divide-stone-200">
        {modules.map((m, i) => (
          <div key={m.id}>
            <div className="px-4 py-3 text-sm font-medium text-stone-700 bg-white">
              {i + 1}. {m.title}
            </div>
            <ul className="bg-white">
              {m.lessons.map((l) => {
                const active = l.id === activeLessonId;
                const completed = completedSet.has(l.id);
                return (
                  <li key={l.id}>
                    <button
                      onClick={() => onSelect?.(l)}
                      className={`w-full text-left px-4 py-2 text-sm flex items-center gap-2 ${
                        active ? "bg-primary-50 text-primary-700" : "hover:bg-stone-50 text-stone-700"
                      }`}
                    >
                      <span className={`w-1.5 h-1.5 rounded-full ${active ? "bg-primary-600" : "bg-stone-300"}`} />
                      <span className="flex-1 truncate">{l.title}</span>
                      {completed && <span className="text-xs text-green-600 font-medium">Hoàn thành</span>}
                      <span className="text-xs text-stone-500">{l.time}</span>
                    </button>
                  </li>
                );
              })}
            </ul>
          </div>
        ))}
      </div>
    </aside>
  );
}
