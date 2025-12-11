import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  createBlogPost,
  fetchBlogDetail,
  fetchMyPosts,
  submitBlogPost,
  updateBlogPost,
} from "../../services/blog";

const defaultForm = {
  title: "",
  summary: "",
  slug: "",
  thumbnailUrl: "",
  content: "",
};

const statusMap = {
  draft: { label: "Nháp", className: "bg-stone-100 text-stone-700" },
  pending: { label: "Chờ duyệt", className: "bg-amber-100 text-amber-700" },
  published: { label: "Đã xuất bản", className: "bg-emerald-100 text-emerald-700" },
  rejected: { label: "Bị từ chối", className: "bg-rose-100 text-rose-700" },
};

const statusTag = (status) => statusMap[status] ?? statusMap.draft;

const resolveErrorMessage = (error, fallback = "Không thể thực hiện thao tác.") => {
  if (!error) return fallback;
  const payload = error?.response?.data ?? error?.message ?? error;
  if (typeof payload === "string") return payload;
  if (typeof payload?.message === "string") return payload.message;
  if (Array.isArray(payload)) return payload.map((item) => item?.message || item).join(", ");
  return fallback;
};

export default function BlogStudio() {
  const [posts, setPosts] = useState([]);
  const [loadingPosts, setLoadingPosts] = useState(true);
  const [saving, setSaving] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [selected, setSelected] = useState(null);
  const [form, setForm] = useState(defaultForm);
  const [message, setMessage] = useState("");
  const [loadingDetailId, setLoadingDetailId] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    let cancelled = false;
    setLoadingPosts(true);
    fetchMyPosts()
      .then(({ data }) => {
        if (!cancelled) setPosts(Array.isArray(data) ? data : []);
      })
      .catch((error) => {
        if (!cancelled) setMessage(resolveErrorMessage(error, "Không thể tải danh sách bài viết."));
      })
      .finally(() => {
        if (!cancelled) setLoadingPosts(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const sortedPosts = useMemo(
    () =>
      [...posts].sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()),
    [posts],
  );

  const handleInput = (key, value) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  const resetForm = () => {
    setForm(defaultForm);
    setSelected(null);
    setMessage("");
  };

  const fillForm = (post, content = null) => {
    setSelected(post);
    setForm({
      title: post.title || "",
      summary: post.summary || "",
      slug: post.slug || "",
      thumbnailUrl: post.thumbnailUrl || "",
      content: content ?? post.content ?? "",
    });
  };

  const loadAndSelectPost = async (post) => {
    setMessage("");
    fillForm(post, post.content || "");
    if (!post.content) {
      setLoadingDetailId(post.id);
      try {
        const { data } = await fetchBlogDetail(post.id);
        setPosts((prev) => prev.map((item) => (item.id === data.id ? { ...item, ...data } : item)));
        fillForm({ ...post, ...data }, data.content || "");
      } catch (error) {
        setMessage(resolveErrorMessage(error, "Không thể tải chi tiết bài viết."));
      } finally {
        setLoadingDetailId(null);
      }
    }
  };

  const upsertPost = async () => {
    setMessage("");
    setSaving(true);
    try {
      const payload = {
        title: form.title,
        summary: form.summary,
        slug: form.slug,
        thumbnailUrl: form.thumbnailUrl,
        content: form.content,
      };
      const { data } = selected
        ? await updateBlogPost(selected.id, payload)
        : await createBlogPost(payload);
      setPosts((prev) => {
        const exists = prev.some((p) => p.id === data.id);
        if (exists) {
          return prev.map((item) => (item.id === data.id ? { ...item, ...data } : item));
        }
        return [data, ...prev];
      });
      if (!selected) {
        fillForm({ ...data, content: form.content }, form.content);
      } else {
        fillForm({ ...selected, ...data, content: form.content }, form.content);
      }
      setMessage("Đã lưu bản nháp.");
    } catch (error) {
      setMessage(resolveErrorMessage(error, "Không thể lưu bản nháp."));
    } finally {
      setSaving(false);
    }
  };

  const submitPost = async () => {
    if (!selected?.id) {
      setMessage("Hãy lưu bản nháp trước khi gửi duyệt.");
      return;
    }
    setMessage("");
    setSubmitting(true);
    try {
      const { data } = await submitBlogPost(selected.id);
      setPosts((prev) => prev.map((item) => (item.id === data.id ? { ...item, ...data } : item)));
      fillForm({ ...selected, ...data, content: form.content }, form.content);
      setMessage("Đã gửi duyệt. Quản lý sẽ phản hồi sớm nhất.");
    } catch (error) {
      setMessage(resolveErrorMessage(error, "Không thể gửi duyệt."));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="bg-gradient-to-b from-white via-slate-50 to-white">
      <div className="mx-auto max-w-6xl px-4 py-10 lg:py-14">
        <div className="rounded-[40px] bg-white/90 p-8 shadow-[0_20px_50px_rgba(15,23,42,0.12)] backdrop-blur">
          <div className="flex flex-col gap-6 lg:flex-row">
            <div className="flex-1">
              <p className="text-xs font-semibold uppercase tracking-[0.4em] text-amber-500">Studio Blog</p>
              <h1 className="mt-2 text-3xl font-black text-stone-900">Viết bài chia sẻ kinh nghiệm học tập</h1>
              <p className="mt-3 text-sm text-stone-600">
                Ghi lại bài học, mẹo luyện thi, câu chuyện truyền cảm hứng. Bài viết cần được quản lý duyệt trước khi
                xuất bản trên trang blog cộng đồng.
              </p>
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={() => navigate("/blog")}
                className="rounded-full border border-stone-200 px-4 py-2 text-sm font-semibold text-stone-700 transition hover:border-stone-300 hover:text-stone-900"
              >
                Xem blog công khai
              </button>
              <button
                onClick={resetForm}
                className="rounded-full bg-stone-900 px-5 py-2 text-sm font-semibold text-white shadow-lg shadow-stone-900/30 transition hover:-translate-y-0.5"
              >
                Viết bài mới
              </button>
            </div>
          </div>

          {message && <div className="mt-6 rounded-2xl bg-amber-50 px-4 py-3 text-sm text-amber-700">{message}</div>}

          <div className="mt-10 grid gap-8 lg:grid-cols-[1.2fr,0.8fr]">
            <div className="rounded-3xl border border-stone-100 p-6 shadow-sm">
              <h2 className="text-lg font-bold text-stone-900">
                {selected ? "Chỉnh sửa bài viết" : "Tạo bài viết mới"}
              </h2>
              <div className="mt-4 space-y-4">
                <label className="block text-sm font-medium text-stone-700">
                  Tiêu đề
                  <input
                    value={form.title}
                    onChange={(e) => handleInput("title", e.target.value)}
                    className="mt-1 w-full rounded-2xl border border-stone-200 px-4 py-2.5 text-sm focus:border-amber-500 focus:outline-none"
                    placeholder="Ví dụ: 10 mẹo học từ vựng mỗi ngày"
                  />
                </label>
                <label className="block text-sm font-medium text-stone-700">
                  Mô tả ngắn
                  <textarea
                    value={form.summary}
                    onChange={(e) => handleInput("summary", e.target.value)}
                    className="mt-1 h-20 w-full rounded-2xl border border-stone-200 px-4 py-2.5 text-sm focus:border-amber-500 focus:outline-none"
                    placeholder="Tóm tắt nội dung chính của bài viết."
                  />
                </label>
                <div className="grid gap-4 md:grid-cols-2">
                  <label className="block text-sm font-medium text-stone-700">
                    Đường dẫn (slug)
                    <input
                      value={form.slug}
                      onChange={(e) => handleInput("slug", e.target.value)}
                      className="mt-1 w-full rounded-2xl border border-stone-200 px-4 py-2.5 text-sm focus:border-amber-500 focus:outline-none"
                      placeholder="chu-de-tu-vung-chu-de"
                    />
                  </label>
                  <label className="block text-sm font-medium text-stone-700">
                    Hình minh hoạ
                    <input
                      value={form.thumbnailUrl}
                      onChange={(e) => handleInput("thumbnailUrl", e.target.value)}
                      className="mt-1 w-full rounded-2xl border border-stone-200 px-4 py-2.5 text-sm focus:border-amber-500 focus:outline-none"
                      placeholder="https://..."
                    />
                  </label>
                </div>
                <label className="block text-sm font-medium text-stone-700">
                  Nội dung
                  <textarea
                    value={form.content}
                    onChange={(e) => handleInput("content", e.target.value)}
                    className="mt-1 min-h-[220px] w-full rounded-2xl border border-stone-200 px-4 py-3 text-sm focus:border-amber-500 focus:outline-none"
                    placeholder="Chia sẻ trải nghiệm, ví dụ, hình ảnh..."
                  />
                </label>
              </div>

              <div className="mt-6 flex flex-wrap gap-3">
                <button
                  onClick={upsertPost}
                  disabled={saving}
                  className="inline-flex items-center rounded-full bg-amber-500 px-6 py-2.5 text-sm font-semibold text-white transition hover:bg-amber-600 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {saving ? "Đang lưu..." : selected ? "Cập nhật nháp" : "Lưu bản nháp"}
                </button>
                <button
                  onClick={submitPost}
                  disabled={submitting}
                  className="inline-flex items-center rounded-full border border-stone-200 px-6 py-2.5 text-sm font-semibold text-stone-700 transition hover:border-stone-300 hover:text-stone-900 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {selected?.status === "pending" ? "Đang chờ duyệt" : submitting ? "Đang gửi..." : "Gửi duyệt"}
                </button>
              </div>
            </div>

            <div className="rounded-3xl border border-stone-100 p-6 shadow-sm">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-bold text-stone-900">Bài viết của tôi</h2>
                {loadingPosts && <span className="text-xs text-stone-500">Đang tải...</span>}
              </div>
              <div className="mt-4 space-y-3">
                {sortedPosts.map((post) => {
                  const status = statusTag(post.status);
                  return (
                    <button
                      key={post.id}
                      type="button"
                      onClick={() => loadAndSelectPost(post)}
                      className={`w-full rounded-2xl border px-4 py-3 text-left transition ${
                        selected?.id === post.id
                          ? "border-amber-500 bg-amber-50/30"
                          : "border-stone-200 bg-white hover:border-amber-200"
                      }`}
                    >
                      <div className="flex items-center justify-between gap-3">
                        <p className="text-sm font-semibold text-stone-900">{post.title}</p>
                        <span className={`rounded-full px-3 py-1 text-xs font-semibold ${status.className}`}>
                          {status.label}
                        </span>
                      </div>
                      <div className="mt-2 text-xs text-stone-500">
                        Cập nhật:{" "}
                        {post.updatedAt
                          ? new Date(post.updatedAt).toLocaleDateString("vi-VN")
                          : new Date(post.createdAt || Date.now()).toLocaleDateString("vi-VN")}
                        {loadingDetailId === post.id && (
                          <span className="ml-2 text-[11px] text-amber-600">Đang tải nội dung...</span>
                        )}
                      </div>
                      {post.rejectionReason && (
                        <p className="mt-2 rounded-2xl bg-rose-50 px-3 py-2 text-xs text-rose-600">
                          Lý do từ chối: {post.rejectionReason}
                        </p>
                      )}
                    </button>
                  );
                })}

                {!loadingPosts && sortedPosts.length === 0 && (
                  <p className="rounded-2xl border border-dashed border-stone-200 p-6 text-sm text-stone-500">
                    Bạn chưa có bài viết nào. Hãy viết bài đầu tiên để truyền cảm hứng cho cộng đồng.
                  </p>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
