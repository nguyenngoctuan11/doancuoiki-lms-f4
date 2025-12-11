import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { completeForgotPassword, startForgotPassword } from "../services/auth";

const resolveError = (error, fallback = "Không thể thực hiện yêu cầu.") => {
  if (!error) return fallback;
  const payload = error?.response?.data ?? error?.message ?? error;
  if (typeof payload === "string") return payload;
  if (typeof payload?.message === "string") return payload.message;
  return fallback;
};

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [step, setStep] = useState(1);
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleSendOtp = async () => {
    if (!email.trim()) {
      setError("Vui lòng nhập email đã đăng ký.");
      return;
    }
    setLoading(true);
    setError("");
    setStatus("");
    try {
      await startForgotPassword({ email: email.trim() });
      setStep(2);
      setStatus("Mã OTP đã được gửi đến email của bạn. Kiểm tra hộp thư và nhập mã bên dưới.");
    } catch (err) {
      setError(resolveError(err, "Không thể gửi mã OTP. Vui lòng thử lại."));
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async () => {
    if (!code.trim() || !newPassword) {
      setError("Vui lòng nhập đầy đủ mã OTP và mật khẩu mới.");
      return;
    }
    if (newPassword.length < 6) {
      setError("Mật khẩu mới phải dài ít nhất 6 ký tự.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("Mật khẩu xác nhận không khớp.");
      return;
    }
    setLoading(true);
    setError("");
    try {
      await completeForgotPassword({
        email: email.trim(),
        code: code.trim(),
        newPassword,
      });
      setStatus("Đổi mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới.");
      setTimeout(() => navigate("/login"), 1500);
    } catch (err) {
      setError(resolveError(err, "Không thể đổi mật khẩu. Vui lòng kiểm tra lại OTP."));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-soft flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-lg rounded-3xl border border-[#eadfd1] bg-white/90 p-8 shadow-[0_25px_70px_rgba(54,31,14,0.08)] backdrop-blur">
        <div className="text-center">
          <p className="text-xs font-semibold uppercase tracking-[0.4em] text-[#caa877]">Quên mật khẩu</p>
          <h1 className="mt-2 text-3xl font-black text-[#6e4f3b]">Khôi phục tài khoản</h1>
          <p className="mt-3 text-sm text-[#8b6d57]">
            Nhập email đã đăng ký để nhận mã OTP. Sau khi xác thực thành công, bạn có thể đặt lại mật khẩu mới.
          </p>
        </div>

        <div className="mt-8 space-y-5">
          <label className="block text-sm font-medium text-[#6e4f3b]">
            Email đã đăng ký
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="mt-1 w-full rounded-2xl border border-[#eadfd1] bg-white px-4 py-2.5 text-sm text-[#6e4f3b] placeholder:text-[#b8a692] focus:outline-none focus:ring-2 focus:ring-[#d9b991]/60"
              placeholder="you@example.com"
              disabled={step === 2}
            />
          </label>

          {step === 2 && (
            <>
              <label className="block text-sm font-medium text-[#6e4f3b]">
                Mã OTP
                <input
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  className="mt-1 w-full rounded-2xl border border-[#eadfd1] bg-white px-4 py-2.5 text-sm text-[#6e4f3b] placeholder:text-[#b8a692] focus:outline-none focus:ring-2 focus:ring-[#d9b991]/60"
                  placeholder="Nhập mã gồm 6 chữ số"
                />
              </label>
              <label className="block text-sm font-medium text-[#6e4f3b]">
                Mật khẩu mới
                <input
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  className="mt-1 w-full rounded-2xl border border-[#eadfd1] bg-white px-4 py-2.5 text-sm text-[#6e4f3b] placeholder:text-[#b8a692] focus:outline-none focus:ring-2 focus:ring-[#d9b991]/60"
                  placeholder="Ít nhất 6 ký tự"
                />
              </label>
              <label className="block text-sm font-medium text-[#6e4f3b]">
                Nhập lại mật khẩu
                <input
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="mt-1 w-full rounded-2xl border border-[#eadfd1] bg-white px-4 py-2.5 text-sm text-[#6e4f3b] placeholder:text-[#b8a692] focus:outline-none focus:ring-2 focus:ring-[#d9b991]/60"
                  placeholder="Nhập lại mật khẩu mới"
                />
              </label>
            </>
          )}
        </div>

        {error && <p className="mt-4 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-600">{error}</p>}
        {status && !error && <p className="mt-4 rounded-2xl bg-[#f7f1eb] px-4 py-3 text-sm text-[#6e4f3b]">{status}</p>}

        <div className="mt-8 flex flex-col gap-3">
          {step === 1 ? (
            <button
              onClick={handleSendOtp}
              disabled={loading}
              className="inline-flex items-center justify-center rounded-full bg-[#caa877] px-6 py-3 text-sm font-semibold text-white shadow-lg shadow-[#caa877]/40 transition hover:bg-[#c39f6f] disabled:cursor-not-allowed disabled:opacity-60"
            >
              {loading ? "Đang gửi..." : "Gửi mã OTP"}
            </button>
          ) : (
            <button
              onClick={handleResetPassword}
              disabled={loading}
              className="inline-flex items-center justify-center rounded-full bg-[#8b6d57] px-6 py-3 text-sm font-semibold text-white shadow-lg shadow-[#8b6d57]/30 transition hover:bg-[#74543f] disabled:cursor-not-allowed disabled:opacity-60"
            >
              {loading ? "Đang đặt lại..." : "Đặt lại mật khẩu"}
            </button>
          )}
          {step === 2 && (
            <button
              type="button"
              className="text-sm font-semibold text-[#a07f63] hover:text-[#8b6d57]"
              onClick={() => {
                setStep(1);
                setCode("");
                setNewPassword("");
                setConfirmPassword("");
                setStatus("");
              }}
              disabled={loading}
            >
              Gửi lại mã OTP
            </button>
          )}
        </div>

        <div className="mt-6 text-center text-sm text-[#8b6d57]">
          <span>Đã nhớ mật khẩu?</span>{" "}
          <Link className="font-semibold text-[#a07f63] hover:text-[#8b6d57]" to="/login">
            Quay lại đăng nhập
          </Link>
        </div>
      </div>
    </div>
  );
}
