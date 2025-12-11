import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { fetchPublicPost } from "../services/blog";

export default function BlogDetail() {
  const { slug } = useParams();
  const [post, setPost] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!slug) return;
    let cancelled = false;
    setLoading(true);
    fetchPublicPost(slug)
      .then(({ data }) => {
        if (!cancelled) setPost(data);
      })
      .catch(() => {
        if (!cancelled) setError("Không tìm thấy bài viết hoặc đã bị gỡ.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [slug]);

  if (loading) {
    return (
      <div className="bg-white">
        <div className="mx-auto max-w-3xl px-4 py-16">
          <div className="animate-pulse space-y-6">
            <div className="h-6 w-1/2 rounded-full bg-stone-100" />
            <div className="h-4 w-1/3 rounded-full bg-stone-100" />
            <div className="aspect-video rounded-3xl bg-stone-100" />
            <div className="space-y-4">
              {Array.from({ length: 6 }).map((_, index) => (
                <div key={index} className="h-4 rounded-full bg-stone-100" />
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error || !post) {
    return (
      <div className="bg-white py-20">
        <div className="mx-auto max-w-2xl rounded-3xl border border-rose-100 bg-rose-50 px-6 py-10 text-center text-rose-600">
          {error || "Bài viết không tồn tại."}
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white">
      <div className="mx-auto max-w-3xl px-4 py-12">
        <p className="text-xs font-semibold uppercase tracking-[0.4em] text-amber-500">Blog cộng đồng</p>
        <h1 className="mt-3 text-4xl font-black text-stone-900">{post.title}</h1>
        <div className="mt-3 text-sm text-stone-500">
          {post.authorName && <span>Tác giả: {post.authorName}</span>}
          {post.publishedAt && (
            <span className="before:mx-2 before:text-stone-400 before:content-['•']">
              {new Date(post.publishedAt).toLocaleString("vi-VN")}
            </span>
          )}
        </div>
        {post.thumbnailUrl && (
          <img
            src={post.thumbnailUrl}
            alt={post.title}
            className="mt-8 w-full rounded-[32px] border border-stone-100 object-cover"
          />
        )}
        <article className="prose prose-stone mt-8 max-w-none text-lg leading-relaxed">
          {post.content?.split("\n").map((line, index) =>
            line.trim().length === 0 ? <br key={`br-${index}`} /> : <p key={index}>{line}</p>,
          )}
        </article>
      </div>
    </div>
  );
}
