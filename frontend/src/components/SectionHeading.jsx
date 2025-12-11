import React from "react";

export default function SectionHeading({ eyebrow, title, subtitle, center = false }) {
  return (
    <div className={center ? "text-center max-w-3xl mx-auto" : "max-w-3xl"}>
      {eyebrow && (
        <div className="inline-block rounded-full bg-primary-50 text-primary-700 px-3 py-1 text-xs font-semibold tracking-wide mb-3">
          {eyebrow}
        </div>
      )}
      <h2 className="text-3xl md:text-4xl font-bold text-stone-900 leading-tight">{title}</h2>
      {subtitle && (
        <p className="mt-3 text-stone-600 leading-relaxed">{subtitle}</p>
      )}
    </div>
  );
}

