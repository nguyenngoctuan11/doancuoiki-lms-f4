import React from "react";
import { Link } from "react-router-dom";

export default function NotFound() {
  return (
    <div className="min-h-[60vh] grid place-items-center bg-white">
      <div className="text-center">
        <div className="text-7xl font-extrabold text-primary-600">404</div>
        <div className="mt-2 text-stone-700">Trang bạn tìm không tồn tại.</div>
        <Link className="btn btn-primary mt-4" to="/">Về trang chủ</Link>
      </div>
    </div>
  );
}

