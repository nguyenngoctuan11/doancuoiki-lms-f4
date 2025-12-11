import React from "react";
import SectionHeading from "../components/SectionHeading";

export default function About() {
  return (
    <div className="bg-white">
      <div className="max-w-7xl mx-auto px-4 py-12">
        <SectionHeading
          eyebrow="Về chúng tôi"
          title="Sứ mệnh và giá trị"
          subtitle="Giúp người học bứt phá sự nghiệp qua lộ trình thực chiến"
        />
        <div className="mt-8 grid md:grid-cols-2 gap-8">
          <div className="prose prose-stone max-w-none">
            <p>Chúng tôi xây dựng chương trình học tập trung vào thực hành, phản hồi nhanh và đo lường kết quả.</p>
            <p>Đội ngũ mentor giàu kinh nghiệm, lộ trình rõ ràng và dự án bám sát thực tế giúp học viên tự tin đi làm.</p>
          </div>
          <div className="rounded-xl bg-gradient-to-br from-primary-200 to-primary-100 h-64" />
        </div>
      </div>
    </div>
  );
}

