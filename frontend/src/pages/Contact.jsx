import React from "react";
import SectionHeading from "../components/SectionHeading";

export default function Contact() {
  return (
    <div className="bg-white">
      <div className="max-w-7xl mx-auto px-4 py-12">
        <SectionHeading
          eyebrow="Liên hệ"
          title="Kết nối với chúng tôi"
          subtitle="Đặt câu hỏi hoặc nhận tư vấn lộ trình miễn phí"
        />
        <div className="mt-8 grid md:grid-cols-2 gap-8">
          <form className="space-y-4">
            <input className="input border-stone-300" placeholder="Họ và tên" />
            <input className="input border-stone-300" placeholder="Email" />
            <textarea className="input border-stone-300 h-32" placeholder="Nội dung" />
            <button className="btn btn-primary">Gửi</button>
          </form>
          <div className="rounded-xl border border-stone-200 p-6">
            <div className="font-semibold">Thông tin</div>
            <div className="mt-3 text-stone-600 text-sm space-y-2">
              <div>Email: support@example.com</div>
              <div>Hotline: 0123 456 789</div>
              <div>Địa chỉ: 123 Đường ABC, Quận 1</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

