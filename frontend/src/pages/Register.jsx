import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { API_BASE_URL } from "../api/httpClient";

export default function Register() {
  const nav = useNavigate();
  const [showPass, setShowPass] = useState(false);
  const [showPass2, setShowPass2] = useState(false);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [otpPhase, setOtpPhase] = useState(false);
  const [otpInfo, setOtpInfo] = useState({ email: "", devCode: "" });
  const { setAuthFromResponse } = useAuth();

  const API = API_BASE_URL;

  const onSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    const form = new FormData(e.currentTarget);
    const name = String(form.get("name") || "").trim();
    const email = String(form.get("email") || "").trim().toLowerCase();
    const password = String(form.get("password") || "");
    const confirm = String(form.get("confirm") || "");
    const roleSel = String(form.get("role") || "STUDENT");
    if (name.length < 2) return setError("Họ tên phải có ít nhất 2 ký tự");
    if (!/^\S+@\S+\.\S+$/.test(email)) return setError("Email không hợp lệ");
    if (password.length < 6) return setError("Mật khẩu phải >= 6 ký tự");
    if (password !== confirm) return setError("Nhập lại mật khẩu không khớp");
    const role = roleSel === "TEACHER" ? "teacher" : "student";
    try {
      setSubmitting(true);
      const res = await fetch(`${API}/api/auth/otp/register/start`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ fullName: name, email, password, role }),
      });
      const json = await res.json();
      if (!res.ok) throw new Error(typeof json === "string" ? json : json?.message || "Không thể gửi OTP");
      setOtpPhase(true);
      setOtpInfo({ email, devCode: json.devCode || "" });
    } catch (err) {
      setError(err.message || "Không thể gửi OTP");
    } finally {
      setSubmitting(false);
    }
  };

  const onVerify = async (e) => {
    e.preventDefault();
    setError(null);
    const form = new FormData(e.currentTarget);
    const code = String(form.get("code") || "").trim();
    if (!/^[0-9]{6}$/.test(code)) return setError("Mã OTP gồm 6 chữ số");
    try{
      setSubmitting(true);
      const res = await fetch(`${API}/api/auth/otp/register/verify`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: otpInfo.email, code })
      });
      const json = await res.json();
      if(!res.ok) throw new Error(typeof json === "string" ? json : json?.message || "Xác thực thất bại");
      setAuthFromResponse(json);
      nav("/courses", { replace: true });
    }catch(err){ setError(err.message || "Xác thực thất bại"); }
    finally{ setSubmitting(false); }
  };

  return (
    <div className="min-h-screen bg-soft flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-white/90 backdrop-blur rounded-3xl shadow-xl p-8 md:p-10 border border-[#eadfd1]">
        <div className="w-14 h-14 mx-auto -mt-14 mb-6 rounded-full bg-[#d9b991] text-white grid place-items-center shadow-md">
          <svg xmlns="http://www.w3.org/2000/svg" className="w-7 h-7" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path strokeWidth="1.6" d="M12 21c4.97 0 9-4.03 9-9s-4.03-9-9-9-9 4.03-9 9 4.03 9 9 9Z" />
            <path strokeWidth="1.6" d="M8.5 10.5s.75 1.25 3.5 1.25 3.5-1.25 3.5-1.25M9 15c1.2 1 2.8 1 4 0" />
          </svg>
        </div>

        <div className="text-center space-y-1 mb-6">
          <h1 className="text-2xl font-semibold text-[#6e4f3b]">Đăng ký</h1>
         
        </div>

        {error && (
          <div className="mb-4 rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>
        )}

        {!otpPhase ? (
        <form onSubmit={onSubmit} className="space-y-4">
          <label className="block">
            <span className="sr-only">Họ tên</span>
            <div className="relative">
              <input name="name" type="text" placeholder="Họ tên" required autoComplete="name" className="w-full rounded-2xl border border-[#eadfd1] bg-white px-4 py-3 pl-11 outline-none focus:ring-2 focus:ring-[#d9b991]/50 placeholder:text-[#b8a692] text-[#6e4f3b]" />
              <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-[#b8a692]" viewBox="0 0 24 24" fill="none" stroke="currentColor"><circle cx="12" cy="8" r="3.2" strokeWidth="1.6" /><path d="M5 18c0-3.2 3.1-5 7-5s7 1.8 7 5" strokeWidth="1.6" /></svg>
            </div>
          </label>
          <label className="block">
            <span className="sr-only">Email</span>
            <div className="relative">
              <input name="email" type="email" placeholder="Email" required className="w-full rounded-2xl border border-[#eadfd1] bg-white px-4 py-3 pl-11 outline-none focus:ring-2 focus:ring-[#d9b991]/50 placeholder:text-[#b8a692] text-[#6e4f3b]" />
            </div>
          </label>
          <label className="block">
            <span className="sr-only">Mật khẩu</span>
            <div className="relative">
              <input name="password" type={showPass ? "text" : "password"} placeholder="Mật khẩu" required className="w-full rounded-2xl border border-[#eadfd1] bg-white px-4 py-3 pl-11 pr-11 outline-none focus:ring-2 focus:ring-[#d9b991]/50 placeholder:text-[#b8a692] text-[#6e4f3b]" />
              <button type="button" onClick={() => setShowPass((s) => !s)} className="absolute right-3 top-1/2 -translate-y-1/2 text-[#b8a692] hover:text-[#8b6d57]" aria-label="Toggle password">👁</button>
            </div>
          </label>
          <label className="block">
            <span className="sr-only">Nhập lại mật khẩu</span>
            <div className="relative">
              <input name="confirm" type={showPass2 ? "text" : "password"} placeholder="Nhập lại mật khẩu" required className="w-full rounded-2xl border border-[#eadfd1] bg-white px-4 py-3 pl-11 pr-11 outline-none focus:ring-2 focus:ring-[#d9b991]/50 placeholder:text-[#b8a692] text-[#6e4f3b]" />
              <button type="button" onClick={() => setShowPass2((s) => !s)} className="absolute right-3 top-1/2 -translate-y-1/2 text-[#b8a692] hover:text-[#8b6d57]" aria-label="Toggle confirm password">👁</button>
            </div>
          </label>
          <label className="block">
            <span className="text-sm font-medium text-[#6e4f3b]">Vai trò</span>
            <select name="role" defaultValue="STUDENT" className="mt-1 w-full rounded-2xl border border-[#eadfd1] bg-white px-4 py-3 outline-none focus:ring-2 focus:ring-[#d9b991]/50 text-[#6e4f3b]">
              <option value="STUDENT">Student</option>
              <option value="TEACHER">Teacher</option>
            </select>
          </label>
          <button type="submit" disabled={submitting} className={`w-full rounded-2xl bg-[#caa877] hover:bg-[#c39f6f] text-white py-3 font-medium shadow-md transition-colors ${submitting ? "opacity-70 cursor-not-allowed" : ""}`}>
            {submitting ? "Creating..." : "Create account"}
          </button>
        </form>
        ) : (
        <form onSubmit={onVerify} className="space-y-4">
          <div className="text-sm text-[#8b6d57]">Chúng tôi đã gửi mã OTP đến email: <b>{otpInfo.email}</b>{otpInfo.devCode ? ` (devCode: ${otpInfo.devCode})` : ''}</div>
          <label className="block">
            <span className="sr-only">OTP</span>
            <div className="relative">
              <input name="code" type="text" placeholder="Nhập mã OTP 6 số" required className="w-full rounded-2xl border border-[#eadfd1] bg-white px-4 py-3 outline-none focus:ring-2 focus:ring-[#d9b991]/50 placeholder:text-[#b8a692] text-[#6e4f3b]" />
            </div>
          </label>
          <div className="flex items-center gap-3">
            <button type="submit" disabled={submitting} className={`rounded-2xl bg-[#caa877] hover:bg-[#c39f6f] text-white px-4 py-3 font-medium shadow-md transition-colors ${submitting ? "opacity-70 cursor-not-allowed" : ""}`}>{submitting? 'Verifying...' : 'Xác thực & tạo tài khoản'}</button>
            <button type="button" disabled={submitting} onClick={(e)=>{ e.preventDefault(); setOtpPhase(false); }} className="px-4 py-3 text-sm text-[#a07f63] hover:underline">Sửa thông tin</button>
          </div>
        </form>
        )}

        <p className="text-center text-sm text-[#8b6d57] mt-6">
          Bạn đã có tài khoản rồi ? <Link to="/login" className="text-[#a07f63] font-medium hover:underline">Đăng nhập</Link>
        </p>
      </div>
    </div>
  );
}
