import React from "react";
import SectionHeading from "../components/SectionHeading";

function Item({ q, a }) {
  return (
    <div className="rounded-xl border border-stone-200 p-4 bg-white">
      <div className="font-semibold text-stone-900">{q}</div>
      <div className="mt-1 text-sm text-stone-600">{a}</div>
    </div>
  );
}

export default function FAQ() {
  const faqs = [
    { q: "Khoá học phù hợp cho người mới?", a: "Có. Lộ trình từ cơ bản đến nâng cao, mentor hỗ trợ." },
    { q: "Có nhận review CV không?", a: "Có. Gói Pro có review CV và định hướng phỏng vấn." },
    { q: "Học xong có job không?", a: "Chúng tôi hỗ trợ tối đa về kiến thức và kỹ năng thực tế; kết quả phụ thuộc vào nỗ lực của bạn." },
  ];
  return (
    <div className="bg-white">
      <div className="max-w-7xl mx-auto px-4 py-12">
        <SectionHeading eyebrow="Hỏi đáp" title="Câu hỏi thường gặp" center />
        <div className="mt-8 grid md:grid-cols-2 gap-4">
          {faqs.map((f) => (
            <Item key={f.q} q={f.q} a={f.a} />
          ))}
        </div>
      </div>
    </div>
  );
}

