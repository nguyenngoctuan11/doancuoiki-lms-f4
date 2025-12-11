/* eslint-disable */
import { Link } from "react-router-dom";

export default function CheckoutFailed() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-20 text-center">
      <div className="text-4xl font-extrabold text-red-600">Thanh toán thất bại</div>
      <p className="mt-4 text-stone-600">
        Không thể xác nhận giao dịch. Bạn vui lòng thử lại hoặc liên hệ đội hỗ trợ để được giúp đỡ nhanh nhất.
      </p>
      <div className="mt-8 flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
        <Link
          to="/checkout"
          className="inline-flex items-center justify-center rounded-xl border border-primary-200 px-5 py-2 text-primary-700 transition hover:border-primary-600 hover:bg-primary-50"
        >
          Thử thanh toán lại
        </Link>
        <Link to="/" className="inline-flex items-center justify-center rounded-xl bg-primary-600 px-5 py-2 text-white">
          Về trang chủ
        </Link>
      </div>
    </div>
  );
}
