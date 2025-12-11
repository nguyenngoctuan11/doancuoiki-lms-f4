import { useEffect, useMemo, useRef, useState } from "react";
import { useAuth } from "../../context/AuthContext";
import {
  completePasswordOtp,
  startPasswordOtp,
  updateProfile,
} from "../../services/profile";
import { uploadImage } from "../../services/upload";
import { API_BASE_URL } from "../../api/httpClient";

const tabs = [
  { id: "profile", label: "Thông tin cá nhân", icon: "👤" },
  { id: "security", label: "Mật khẩu và bảo mật", icon: "🛡" },

];

const API_BASE = API_BASE_URL?.replace(/\/+$/, "") || "";

const resolveAssetUrl = (value) => {
  if (!value) return "";
  const raw = String(value).trim();
  if (!raw) return "";
  if (/^(?:https?:|data:|blob:)/i.test(raw)) return raw;
  if (!API_BASE) return raw;
  const normalized = raw.startsWith("/") ? raw : `/${raw}`;
  return `${API_BASE}${normalized}`;
};

export default function AccountSettings() {
  const { user, refreshProfile } = useAuth();
  const [activeTab, setActiveTab] = useState("profile");
  const [modal, setModal] = useState(null);

  useEffect(() => {
    refreshProfile();
  }, [refreshProfile]);

  useEffect(() => {
    if (modal) {
      const prev = document.body.style.overflow;
      document.body.style.overflow = "hidden";
      return () => {
        document.body.style.overflow = prev;
      };
    }
    return undefined;
  }, [modal]);

  const personalItems = useMemo(
    () => [
      { id: "fullName", label: "Họ và tên", value: user?.fullName || "Chưa cập nhật" },
      { id: "username", label: "Tên người dùng", value: user?.username || "Chưa cập nhật" },
      { id: "bio", label: "Giới thiệu", value: user?.bio || "Chưa cập nhật" },
      {
        id: "avatar",
        label: "Ảnh đại diện",
        value: user?.fullName || user?.email || "Chưa cập nhật",
        avatar: user?.avatarUrl,
      },
    ],
    [user],
  );

  const securityItems = useMemo(
    () => [
      {
        id: "password",
        label: user?.hasPassword ? "Đổi mật khẩu" : "Tạo mật khẩu",
        value: user?.hasPassword ? "Đã đặt mật khẩu" : "Chưa đặt mật khẩu",
      },
    ],
    [user],
  );

  const openModal = (type) => setModal({ type });
  const closeModal = () => setModal(null);

  const renderModal = () => {
    if (!modal) return null;
    switch (modal.type) {
      case "fullName":
        return (
          <NameModal
            initialValue={user?.fullName || ""}
            onClose={closeModal}
            onSuccess={refreshProfile}
          />
        );
      case "username":
        return (
          <UsernameModal
            initialValue={user?.username || ""}
            onClose={closeModal}
            onSuccess={refreshProfile}
          />
        );
      case "bio":
        return (
          <BioModal
            initialValue={user?.bio || ""}
            onClose={closeModal}
            onSuccess={refreshProfile}
          />
        );
      case "avatar":
        return (
          <AvatarModal
            currentAvatar={user?.avatarUrl}
            fallback={user?.fullName || user?.email || "U"}
            onClose={closeModal}
            onSuccess={refreshProfile}
          />
        );
      case "password":
        return (
          <PasswordModal
            email={user?.email || ""}
            onClose={closeModal}
            onSuccess={refreshProfile}
            hasPassword={user?.hasPassword}
          />
        );
      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#fdeee5] via-white to-[#e5f5ff] py-10 px-4">
      <div className="max-w-6xl mx-auto">
        <div className="rounded-3xl bg-white/90 backdrop-blur border border-white shadow-2xl flex flex-col lg:flex-row overflow-hidden">
          <aside className="w-full lg:w-72 bg-gradient-to-b from-[#f8f0ff] to-transparent p-6 space-y-6">
            <div>
              <div className="w-9 h-9 rounded-md bg-primary-600 text-white grid place-items-center font-bold">F4</div>
              <h1 className="text-2xl font-semibold text-stone-900 mt-2">Cài đặt tài khoản</h1>
              <p className="text-sm text-stone-500 mt-1">
                Quản lý thông tin cá nhân, bảo mật và thông báo của bạn.
              </p>
            </div>
            <nav className="space-y-2">
              {tabs.map((tab) => (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => setActiveTab(tab.id)}
                  className={`w-full text-left flex items-center gap-3 rounded-2xl px-4 py-3 text-sm font-medium transition ${
                    activeTab === tab.id
                      ? "bg-[#1f2b3b] text-white shadow-lg"
                      : "text-stone-600 hover:bg-stone-100"
                  }`}
                >
                  <span>{tab.icon}</span>
                  {tab.label}
                </button>
              ))}
            </nav>
          </aside>
          <section className="flex-1 p-6 lg:p-10 bg-white space-y-6">
            {activeTab === "profile" && (
              <PersonalSection items={personalItems} onSelect={openModal} />
            )}
            {activeTab === "security" && (
              <SecuritySection items={securityItems} onSelect={openModal} />
            )}
         
          </section>
        </div>
      </div>
      {renderModal()}
    </div>
  );
}

function PersonalSection({ items, onSelect }) {
  return (
    <div className="space-y-4">
      <header>
        <h2 className="text-2xl font-semibold text-stone-900">Thông tin cá nhân</h2>
        <p className="text-sm text-stone-500">
          Quản lý tên hiển thị, tên người dùng, bio và avatar của bạn.
        </p>
      </header>
      <div className="rounded-3xl border border-stone-100 divide-y divide-stone-100 shadow-sm overflow-hidden">
        {items.map((item) => (
          <button
            key={item.id}
            type="button"
            onClick={() => onSelect(item.id)}
            className="w-full flex items-center justify-between px-5 py-4 hover:bg-stone-50 transition text-left"
          >
            <div>
              <p className="text-xs uppercase tracking-[0.3em] text-stone-400">{item.label}</p>
              {item.avatar ? (
                <div className="flex items-center gap-3 mt-1">
                  <div className="w-12 h-12 rounded-full bg-stone-100 overflow-hidden">
                    {item.avatar ? (
                      <img src={item.avatar} alt="Avatar" className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full grid place-items-center text-stone-500 text-lg">
                        {(item.value || "U").trim().charAt(0).toUpperCase()}
                      </div>
                    )}
                  </div>
                  <span className="font-medium text-stone-900">{item.value}</span>
                </div>
              ) : (
                <p className="text-base font-medium text-stone-900 mt-1">{item.value}</p>
              )}
            </div>
            <ChevronRightIcon />
          </button>
        ))}
      </div>
    </div>
  );
}

function SecuritySection({ items, onSelect }) {
  return (
    <div className="space-y-4">
      <header>
        <h2 className="text-2xl font-semibold text-stone-900">Mật khẩu và bảo mật</h2>
        <p className="text-sm text-stone-500">
          Quản lý mật khẩu và xác minh hai bước để bảo vệ tài khoản của bạn.
        </p>
      </header>
      <div className="rounded-3xl border border-stone-100 divide-y divide-stone-100 shadow-sm overflow-hidden">
        {items.map((item) => (
          <button
            key={item.id}
            type="button"
            onClick={() => !item.disabled && onSelect(item.id)}
            className={`w-full flex items-center justify-between px-5 py-4 text-left transition ${
              item.disabled ? "opacity-60 cursor-not-allowed" : "hover:bg-stone-50"
            }`}
          >
            <div>
              <p className="text-base font-semibold text-stone-900">{item.label}</p>
              <p className="text-sm text-stone-500">{item.value}</p>
            </div>
            <ChevronRightIcon />
          </button>
        ))}
      </div>
    </div>
  );
}
function ModalShell({ title, description, children, onClose }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center px-4">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="relative w-full max-w-lg rounded-[32px] bg-gradient-to-b from-[#fef3ff] via-white to-[#e8f8ff] p-8 shadow-2xl">
        <button
          type="button"
          className="absolute top-4 right-4 w-8 h-8 rounded-full text-stone-500 hover:bg-white"
          onClick={onClose}
        >
          ✕
        </button>
        <div className="space-y-2 mb-6">
          <h3 className="text-2xl font-semibold text-stone-900">{title}</h3>
          <p className="text-sm text-stone-500">{description}</p>
        </div>
        {children}
      </div>
    </div>
  );
}

function NameModal({ initialValue, onClose, onSuccess }) {
  const [value, setValue] = useState(initialValue);
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!value.trim()) {
      setStatus({ type: "error", message: "Họ tên không được để trống." });
      return;
    }
    try {
      setLoading(true);
      await updateProfile({ fullName: value.trim() });
      await onSuccess();
      setStatus({ type: "success", message: "Đã cập nhật họ tên." });
      setTimeout(onClose, 700);
    } catch (err) {
      const msg = err?.response?.data;
      setStatus({ type: "error", message: typeof msg === "string" ? msg : msg?.message || "Không thể cập nhật." });
    } finally {
      setLoading(false);
    }
  };

  return (
    <ModalShell
      title="Cập nhật tên của bạn"
      description="Tên sẽ hiển thị trên trang cá nhân, bình luận và bài viết của bạn."
      onClose={onClose}
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <label className="block text-sm text-stone-600">
          Họ và tên
          <input
            className="mt-2 w-full rounded-2xl border border-stone-200 px-4 py-3"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            placeholder="Nhập họ tên"
          />
        </label>
        {status && (
          <p className={`text-sm ${status.type === "success" ? "text-emerald-600" : "text-red-500"}`}>{status.message}</p>
        )}
        <button type="submit" className="btn btn-primary w-full" disabled={loading}>
          {loading ? "Đang lưu..." : "Lưu lại"}
        </button>
      </form>
    </ModalShell>
  );
}

function UsernameModal({ initialValue, onClose, onSuccess }) {
  const [value, setValue] = useState(initialValue);
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!value.trim()) {
      setStatus({ type: "error", message: "Tên người dùng không được để trống." });
      return;
    }
    try {
      setLoading(true);
      await updateProfile({ username: value.trim() });
      await onSuccess();
      setStatus({ type: "success", message: "Đã cập nhật tên người dùng." });
      setTimeout(onClose, 700);
    } catch (err) {
      const msg = err?.response?.data;
      setStatus({ type: "error", message: typeof msg === "string" ? msg : msg?.message || "Không thể cập nhật." });
    } finally {
      setLoading(false);
    }
  };

  return (
    <ModalShell
      title="Chỉnh sửa tên người dùng"
      description="Tên người dùng quyết định URL trang cá nhân của bạn."
      onClose={onClose}
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <label className="block text-sm text-stone-600">
          Tên người dùng
          <div className="mt-2 flex items-center rounded-2xl border border-stone-200 px-3">
            <span className="text-stone-400">@</span>
            <input
              className="flex-1 px-2 py-3 outline-none"
              value={value}
              onChange={(e) => setValue(e.target.value.replace(/\s+/g, ""))}
              placeholder="vinhduong12"
            />
          </div>
        </label>
        {status && (
          <p className={`text-sm ${status.type === "success" ? "text-emerald-600" : "text-red-500"}`}>{status.message}</p>
        )}
        <button type="submit" className="btn btn-primary w-full" disabled={loading}>
          {loading ? "Đang lưu..." : "Lưu lại"}
        </button>
      </form>
    </ModalShell>
  );
}

function BioModal({ initialValue, onClose, onSuccess }) {
  const [value, setValue] = useState(initialValue);
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    try {
      setLoading(true);
      await updateProfile({ bio: value });
      await onSuccess();
      setStatus({ type: "success", message: "Đã cập nhật giới thiệu." });
      setTimeout(onClose, 700);
    } catch (err) {
      const msg = err?.response?.data;
      setStatus({ type: "error", message: typeof msg === "string" ? msg : msg?.message || "Không thể cập nhật." });
    } finally {
      setLoading(false);
    }
  };

  return (
    <ModalShell
      title="Chỉnh sửa phần giới thiệu"
      description="Giới thiệu giúp mọi người hiểu rõ hơn về bạn."
      onClose={onClose}
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <label className="block text-sm text-stone-600">
          Giới thiệu
          <textarea
            className="mt-2 w-full rounded-2xl border border-stone-200 px-4 py-3 min-h-[120px]"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            maxLength={2000}
            placeholder="Chia sẻ đôi điều về bạn..."
          />
        </label>
        <p className="text-xs text-stone-400 text-right">{value.length}/2000 ký tự</p>
        {status && (
          <p className={`text-sm ${status.type === "success" ? "text-emerald-600" : "text-red-500"}`}>{status.message}</p>
        )}
        <button type="submit" className="btn btn-primary w-full" disabled={loading}>
          {loading ? "Đang lưu..." : "Lưu lại"}
        </button>
      </form>
    </ModalShell>
  );
}


function AvatarModal({ currentAvatar, fallback, onClose, onSuccess }) {
  const [linkValue, setLinkValue] = useState(currentAvatar || "");
  const [preview, setPreview] = useState(resolveAssetUrl(currentAvatar || ""));
  const [selectedFile, setSelectedFile] = useState(null);
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(false);
  const fileRef = useRef(null);
  const objectUrlRef = useRef(null);

  const revokePreviewUrl = () => {
    if (objectUrlRef.current) {
      URL.revokeObjectURL(objectUrlRef.current);
      objectUrlRef.current = null;
    }
  };

  useEffect(() => () => revokePreviewUrl(), []);

  const handleFileChange = (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith("image/")) {
      setStatus({ type: "error", message: "Vui lòng chọn ảnh hợp lệ." });
      event.target.value = "";
      return;
    }

    setSelectedFile(file);
    revokePreviewUrl();
    const objectUrl = URL.createObjectURL(file);
    objectUrlRef.current = objectUrl;
    setPreview(objectUrl);
    setLinkValue("");
    setStatus({ type: "success", message: "Ảnh mới đang được xem trước, bấm Lưu để xác nhận." });
    event.target.value = "";
  };

  const handleLinkChange = (event) => {
    const value = event.target.value;
    setLinkValue(value);
    setSelectedFile(null);
    revokePreviewUrl();
    setPreview(resolveAssetUrl(value));
    setStatus(null);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!selectedFile && !linkValue.trim()) {
      setStatus({ type: "error", message: "Vui lòng chọn ảnh hoặc dán đường dẫn." });
      return;
    }

    try {
      setLoading(true);
      let avatarUrl = linkValue.trim();

      if (selectedFile) {
        const { data } = await uploadImage(selectedFile);
        avatarUrl = data.url;
      }
      if (!avatarUrl) {
        setStatus({ type: "error", message: "Không tìm thấy đường dẫn ảnh hợp lệ." });
        return;
      }

      await updateProfile({ avatarUrl });
      await onSuccess();

      revokePreviewUrl();
      setSelectedFile(null);
      setPreview(resolveAssetUrl(avatarUrl));
      setLinkValue(avatarUrl);
      setStatus({ type: "success", message: "Đã cập nhật ảnh đại diện." });
      setTimeout(onClose, 700);
    } catch (err) {
      const msg = err?.response?.data;
      setStatus({ type: "error", message: typeof msg === "string" ? msg : msg?.message || "Không thể cập nhật." });
    } finally {
      setLoading(false);
    }
  };

  return (
    <ModalShell
      title="Ảnh đại diện"
      description="Ảnh đại diện giúp mọi người nhận biết bạn dễ dàng hơn."
      onClose={onClose}
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="w-32 h-32 mx-auto rounded-full bg-stone-100 overflow-hidden shadow-inner">
          {preview ? (
            <img src={preview} alt="Avatar preview" className="w-full h-full object-cover" />
          ) : (
            <div className="w-full h-full grid place-items-center text-3xl text-stone-500">
              {fallback.trim().charAt(0).toUpperCase()}
            </div>
          )}
        </div>
        <div className="grid gap-3">
          <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={handleFileChange} />
          <button
            type="button"
            className="btn w-full"
            onClick={() => fileRef.current?.click()}
            disabled={loading}
          >
            {loading ? "Đang xử lý..." : "Tải ảnh mới lên"}
          </button>
          <label className="text-sm text-stone-600">
            Hoặc dán đường dẫn ảnh
            <input
              className="mt-2 w-full rounded-2xl border border-stone-200 px-4 py-2.5"
              value={linkValue}
              onChange={handleLinkChange}
              placeholder="https://..."
            />
          </label>
        </div>
        {status && (
          <p className={`text-sm ${status.type === "success" ? "text-emerald-600" : "text-red-500"}`}>{status.message}</p>
        )}
        <button type="submit" className="btn btn-primary w-full" disabled={loading}>
          {loading ? "Đang lưu..." : "Lưu ảnh đại diện"}
        </button>
      </form>
    </ModalShell>
  );
}

function PasswordModal({ email, onClose, onSuccess, hasPassword }) {
  const [form, setForm] = useState({
    email,
    code: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [status, setStatus] = useState(null);
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const handleSendCode = async () => {
    if (!form.email || !/\S+@\S+\.\S+/.test(form.email)) {
      setStatus({ type: "error", message: "Email không hợp lệ." });
      return;
    }
    try {
      setSending(true);
      await startPasswordOtp({ email: form.email });
      setSent(true);
      setStatus({ type: "success", message: "Đã gửi mã xác minh tới email của bạn." });
    } catch (err) {
      const msg = err?.response?.data;
      setStatus({ type: "error", message: typeof msg === "string" ? msg : msg?.message || "Không thể gửi mã." });
    } finally {
      setSending(false);
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!sent) {
      setStatus({ type: "error", message: "Hãy xác thực email trước khi tạo mật khẩu." });
      return;
    }
    if (!form.code.trim()) {
      setStatus({ type: "error", message: "Vui lòng nhập mã xác nhận." });
      return;
    }
    if (form.newPassword.length < 6) {
      setStatus({ type: "error", message: "Mật khẩu mới tối thiểu 6 ký tự." });
      return;
    }
    if (form.newPassword !== form.confirmPassword) {
      setStatus({ type: "error", message: "Xác nhận mật khẩu không khớp." });
      return;
    }
    try {
      setSubmitting(true);
      await completePasswordOtp({
        email: form.email,
        code: form.code.trim(),
        newPassword: form.newPassword,
      });
      await onSuccess();
      setStatus({ type: "success", message: "Đã cập nhật mật khẩu. Bạn có thể đăng nhập bằng mật khẩu này." });
      setTimeout(onClose, 900);
    } catch (err) {
      const msg = err?.response?.data;
      setStatus({ type: "error", message: typeof msg === "string" ? msg : msg?.message || "Không thể xác minh." });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <ModalShell
      title={hasPassword ? "Đổi mật khẩu" : "Tạo mật khẩu"}
      description="Xác minh email của bạn trước khi đặt mật khẩu mới."
      onClose={onClose}
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <label className="block text-sm text-stone-600">
          Email
          <input
            className="mt-2 w-full rounded-2xl border border-stone-200 px-4 py-2.5 bg-stone-100"
            value={form.email}
            readOnly
          />
          <p className="text-xs text-stone-500 mt-1">
            Mã OTP sẽ được gửi tới email đăng ký tài khoản này.
          </p>
        </label>
        <div className="flex items-center gap-3">
          <input
            className="flex-1 rounded-2xl border border-stone-200 px-4 py-2.5 disabled:bg-stone-100"
            placeholder="Nhập mã xác nhận"
            value={form.code}
            onChange={(e) => setForm((prev) => ({ ...prev, code: e.target.value }))}
            disabled={!sent}
          />
          <button type="button" className="btn btn-primary" onClick={handleSendCode} disabled={sending}>
            {sending ? "Đang gửi..." : "Gửi mã"}
          </button>
        </div>
        <label className="block text-sm text-stone-600">
          Mật khẩu mới
          <input
            type="password"
            className="mt-2 w-full rounded-2xl border border-stone-200 px-4 py-2.5"
            value={form.newPassword}
            onChange={(e) => setForm((prev) => ({ ...prev, newPassword: e.target.value }))}
            placeholder="••••••••"
          />
        </label>
        <label className="block text-sm text-stone-600">
          Nhập lại mật khẩu
          <input
            type="password"
            className="mt-2 w-full rounded-2xl border border-stone-200 px-4 py-2.5"
            value={form.confirmPassword}
            onChange={(e) => setForm((prev) => ({ ...prev, confirmPassword: e.target.value }))}
            placeholder="••••••••"
          />
        </label>
        {status && (
          <p className={`text-sm ${status.type === "success" ? "text-emerald-600" : "text-red-500"}`}>{status.message}</p>
        )}
        <button type="submit" className="btn btn-primary w-full" disabled={submitting}>
          {submitting ? "Đang xác minh..." : "Xác minh & tạo mật khẩu"}
        </button>
      </form>
    </ModalShell>
  );
}

function ChevronRightIcon() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      className="w-5 h-5 text-stone-400"
      fill="none"
      stroke="currentColor"
    >
      <path d="M9 6l6 6-6 6" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
