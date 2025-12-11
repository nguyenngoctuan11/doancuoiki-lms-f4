/* eslint-disable */
import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { API_BASE_URL } from "../../api/httpClient";
import { useSupportChat } from "../../context/SupportChatContext";

const API_BASE = API_BASE_URL;
const authToken = () => localStorage.getItem("token");

const OFFLINE_BANK_GUIDE = [
  { label: "Ngan hang", value: "Vietcombank - CN Tan Dinh" },
  { label: "So tai khoan", value: "0123 456 789" },
  { label: "Chu tai khoan", value: "CTY TNHH YOUR LMS" },
];

function useQuery() {
  const { search } = useLocation();
  return useMemo(() => new URLSearchParams(search), [search]);
}

function resolveThumb(u) {
  if (!u) return null;
  let s = String(u).trim().replace(/\\/g, "/");
  if (/^https?:\/\//i.test(s) || s.startsWith("data:")) return s;
  if (!s.startsWith("/")) s = `/${s}`;
  return (API_BASE || "") + s;
}
function money(n) {
  try {
    return `${Number(n).toLocaleString("vi-VN")} đ`;
  } catch (e) {
    return `${n} đ`;
  }
}

export default function Checkout() {
  const q = useQuery();
  const navigate = useNavigate();
  const courseKey = q.get("course");
  const [course, setCourse] = useState(null);
  const [buyerName, setBuyerName] = useState("");
  const [buyerEmail, setBuyerEmail] = useState("");
  const [method, setMethod] = useState("VNPAY");
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [offlineInfo, setOfflineInfo] = useState(null);
  const [enrollResult, setEnrollResult] = useState("");
  const { openChat, setEntryContext } = useSupportChat();

  useEffect(() => {
    (async () => {
      setLoading(true);
      setMessage("");
      setOfflineInfo(null);
      setEnrollResult("");
      if (!courseKey) {
        setMessage("Thieu tham so course.");
        setLoading(false);
        return;
      }
      const tries = [
        `/api/public/courses/detail/${encodeURIComponent(courseKey)}`,
        `/api/public/courses/detail-sql/${encodeURIComponent(courseKey)}`,
        `/api/public/course/${encodeURIComponent(courseKey)}`,
      ];
      let found = null;
      for (const p of tries) {
        try {
          const r = await fetch(API_BASE + p, { headers: { Accept: "application/json" } });
          if (!r.ok) continue;
          const j = await r.json();
          if (j) {
            found = j;
            break;
          }
        } catch (e) {}
      }
      if (!found) {
        try {
          const r = await fetch(API_BASE + "/api/public/courses");
          if (r.ok) {
            const list = await r.json();
            if (Array.isArray(list)) {
              found = list.find(
                (x) =>
                  (x.slug || x.course_slug || x.code) === courseKey || String(x.id || x.course_id) === courseKey,
              );
            }
          }
        } catch (e) {}
      }
      setCourse(found);
      setLoading(false);
      if (!found) setMessage("Khong tim thay khoa hoc.");
    })();
  }, [courseKey]);

  const view = useMemo(() => {
    const c = course || {};
    const title = c.title ?? c.name ?? c.course_title ?? "Khoa hoc";
    const desc = c.description ?? c.summary ?? c.short_desc ?? "";
    const price = c.price ?? c.tuition ?? c.amount ?? 0;
    const img = resolveThumb(c.thumbnail_url ?? c.thumbnailUrl ?? c.thumbnail_path ?? c.image_url ?? c.cover_url);
    const id = c.id ?? c.course_id ?? null;
    const slug = c.slug ?? c.course_slug ?? c.code ?? null;
    return { title, desc, price, img, id, slug };
  }, [course]);

  useEffect(() => {
    if (!view?.id) return undefined;
    setEntryContext((prev) => ({
      ...(prev || {}),
      courseId: view.id,
      courseTitle: view.title,
      origin: "checkout",
    }));
    return () => setEntryContext((prev) => (prev && prev.courseId === view.id ? null : prev));
  }, [view?.id, view?.title, setEntryContext]);

  const rememberPendingCourse = () => {
    if (view.id) {
      sessionStorage.setItem("checkout:pendingCourseId", String(view.id));
    }
    if (view.slug) {
      sessionStorage.setItem("checkout:pendingCourseSlug", String(view.slug));
    }
  };

  async function autoEnroll() {
    if (!view.id) return;
    try {
      const headers = authToken() ? { Authorization: `Bearer ${authToken()}` } : {};
      const r2 = await fetch(`${API_BASE}/api/courses/${view.id}/enroll`, { method: "POST", headers });
      if (r2.ok) {
        setEnrollResult("Da kich hoat khoa hoc trong tai khoan cua ban. Hay vao Trang sinh vien de tiep tuc hoc.");
        sessionStorage.removeItem("checkout:pendingCourseId");
        sessionStorage.removeItem("checkout:pendingCourseSlug");
      }
    } catch (e) {
      setEnrollResult("");
    }
  }

  async function onPay() {
    if (!course) return;
    if (!buyerName.trim() || !buyerEmail.trim()) {
      setMessage("Vui long nhap ho ten va email truoc khi thanh toan.");
      return;
    }
    const payload = {
      course_id: view.id,
      course_key: courseKey,
      buyer_name: buyerName,
      buyer_email: buyerEmail,
      method,
      amount: view.price,
      return_url: window.location.origin + "/checkout/success",
      cancel_url: window.location.origin + "/checkout/failed",
    };
    setMessage("Dang xu ly thanh toan...");
    setOfflineInfo(null);
    setEnrollResult("");
    try {
      const headers = { "Content-Type": "application/json" };
      if (authToken()) headers.Authorization = `Bearer ${authToken()}`;
      const r = await fetch(`${API_BASE}/api/payments/checkout`, {
        method: "POST",
        headers,
        body: JSON.stringify(payload),
      });
      if (r.ok) {
        const j = await r.json();
        if (j && j.paymentUrl) {
          rememberPendingCourse();
          window.location.href = j.paymentUrl;
          return;
        }
        if (j && (j.status === "PAID" || j.paid === true)) {
          await autoEnroll();
          if (method === "BANK") {
            setOfflineInfo({
              title: "Thong tin chuyen khoan",
              orderId: j.orderId,
              steps: OFFLINE_BANK_GUIDE,
            });
            setMessage(`Vui long chuyen khoan va ghi noi dung: ${j.orderId || "Thanh toan khoa hoc"}`);
          } else if (method === "COD") {
            setOfflineInfo({
              title: "Dat hang COD thanh cong",
              orderId: j.orderId,
              description: "Nhan vien se lien he de xac nhan va huong dan kich hoat khoa hoc.",
            });
            setMessage("Da ghi nhan don COD. Vui long theo doi dien thoai/email.");
          } else {
            setMessage("Thanh toan thanh cong.");
          }
          return;
        }
      }
      const errText = await r.text();
      setMessage(`Khong goi duoc cong thanh toan (${r.status}). ${errText || ""}`);
      return;
    } catch (e) {}
    setMessage("Khong goi duoc cong thanh toan. Vui long lien he ho tro.");
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-10">
      <h2 className="text-2xl font-bold">Thanh toan khoa hoc</h2>
      <div className="mt-2 flex justify-end">
        <button
          type="button"
          onClick={() =>
            openChat({
              origin: "checkout",
              courseId: view.id,
              courseTitle: view.title,
              topic: "payment_issue",
            })
          }
          className="text-sm font-semibold text-primary-700 hover:underline"
        >
          Cần hỗ trợ thanh toán? Chat với tư vấn viên
        </button>
      </div>
      {message && <div className="mt-2 rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-800">{message}</div>}
      {loading && <div className="mt-4 text-sm text-stone-500">Dang tai thong tin khoa hoc...</div>}
      <div className="grid md:grid-cols-2 gap-4 mt-6">
        <div className="rounded-xl border border-stone-200 p-4 bg-white">
          <div className="rounded-xl overflow-hidden border border-stone-200 aspect-video bg-stone-100">
            {view.img && <img alt={view.title} src={view.img} className="w-full h-full object-cover" />}
          </div>
          <div className="mt-3 font-semibold text-stone-900">{view.title}</div>
          <div className="text-stone-600 text-sm">{view.desc}</div>
          <div className="mt-2 text-2xl font-extrabold">{money(view.price)}</div>
        </div>
        <div className="rounded-xl border border-stone-200 p-4 bg-white">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-sm">Ho ten</label>
              <input className="w-full px-3 py-2 border border-stone-300 rounded-lg" value={buyerName} onChange={(e) => setBuyerName(e.target.value)} placeholder="Nguyen Van A" />
            </div>
            <div>
              <label className="text-sm">Email</label>
              <input className="w-full px-3 py-2 border border-stone-300 rounded-lg" value={buyerEmail} onChange={(e) => setBuyerEmail(e.target.value)} placeholder="email@example.com" />
            </div>
          </div>
          <div className="mt-4">
            <div className="font-semibold mb-2">Phuong thuc thanh toan</div>
            {['VNPAY','MOMO','BANK','COD'].map((v) => (
              <label key={v} className="flex items-center gap-2 text-sm mb-2">
                <input type="radio" name="pm" value={v} checked={method === v} onChange={() => setMethod(v)} /> {v}
              </label>
            ))}
          </div>
          <div className="mt-4 flex gap-2">
            <button onClick={onPay} className="btn btn-primary px-4 py-2 bg-primary-600 text-white rounded-lg">Thanh toan</button>
            <button onClick={() => navigate(-1)} className="px-4 py-2 border border-stone-300 rounded-lg">Quay lai</button>
          </div>
        </div>
      </div>

      {offlineInfo && (
        <div className="mt-8 rounded-2xl border border-primary-200 bg-primary-50/50 p-6 shadow-sm">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.3em] text-primary-600">Thong tin don hang</p>
              <h3 className="text-xl font-bold text-primary-800">{offlineInfo.title}</h3>
              {offlineInfo.description && <p className="text-sm text-primary-700">{offlineInfo.description}</p>}
            </div>
            {offlineInfo.orderId && (
              <span className="rounded-full bg-white px-4 py-2 text-sm font-semibold text-primary-700 shadow-sm">
                Ma don: {offlineInfo.orderId}
              </span>
            )}
          </div>
          {offlineInfo.steps && (
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              {offlineInfo.steps.map((step) => (
                <div key={step.label} className="rounded-xl border border-white bg-white/70 px-4 py-3 shadow-sm">
                  <p className="text-xs uppercase tracking-[0.25em] text-stone-400">{step.label}</p>
                  <p className="text-base font-semibold text-stone-900">{step.value}</p>
                </div>
              ))}
              <div className="rounded-xl border border-dashed border-primary-200 bg-white/60 px-4 py-3 text-sm text-primary-700 md:col-span-2">
                Noi dung chuyen khoan: <strong>{offlineInfo.orderId || 'Thanh toan khoa hoc'}</strong> · So tien {money(view.price)}
              </div>
            </div>
          )}
          {enrollResult && <p className="mt-4 text-sm text-primary-800">{enrollResult}</p>}
        </div>
      )}
    </div>
  );
}
