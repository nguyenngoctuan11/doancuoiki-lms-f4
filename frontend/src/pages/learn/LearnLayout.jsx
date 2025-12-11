import React, { useCallback, useEffect, useState } from "react";
import { Link, Outlet, useLocation, useNavigate, useParams } from "react-router-dom";
import httpClient from "../../api/httpClient";
import { useAuth } from "../../context/AuthContext";

export default function LearnLayout() {
  const { courseId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, initialised } = useAuth();
  const [progress, setProgress] = useState(null);
  const [loadingProgress, setLoadingProgress] = useState(false);
  const [error, setError] = useState("");

  const fetchProgress = useCallback(async () => {
    if (!courseId) return;
    setLoadingProgress(true);
    setError("");
    try {
      const { data } = await httpClient.get(`/api/student/progress/courses/${courseId}`);
      setProgress(data);
    } catch (err) {
      console.error(err);
      const status = err?.response?.status;
      const message = err?.response?.data?.message;
      if (status === 401 || status === 403) {
        setError(message || "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        navigate("/login", { state: { from: location.pathname } });
      } else {
        setError(message || "Không thể tải tiến độ học tập. Vui lòng thử lại sau.");
      }
      setProgress(null);
    } finally {
      setLoadingProgress(false);
    }
  }, [courseId, navigate, location.pathname]);

  useEffect(() => {
    if (!courseId || !initialised) return;
    if (!isAuthenticated) {
      setProgress(null);
      setError("Bạn cần đăng nhập để xem tiến độ học tập.");
      navigate("/login", { state: { from: location.pathname } });
      return;
    }
    fetchProgress();
  }, [courseId, initialised, isAuthenticated, fetchProgress, navigate, location.pathname]);

  const completedLessons = progress?.completedLessons ?? 0;
  const totalLessons = progress?.totalLessons ?? 0;
  const completionPercent = progress?.completionPercent ?? 0;
  const progressMessage =
    totalLessons === 0
      ? "Khóa học hiện chưa có bài học nào. Hãy quay lại khi nội dung được cập nhật."
      : `Bạn đã hoàn thành ${completedLessons}/${totalLessons} bài học (~${completionPercent}%).`;

  return (
    <div className="bg-white min-h-[calc(100vh-64px-64px)]">
      <div className="border-b border-stone-200 bg-white">
        <div className="max-w-7xl mx-auto px-4 py-3 flex flex-wrap gap-4 items-center justify-between">
          <div className="flex items-center gap-3 text-sm">
            <Link to="/courses" className="text-stone-600 hover:text-stone-900">
              Khóa học
            </Link>
            <span className="text-stone-400">/</span>
            <span className="text-stone-900 font-medium">{courseId || "Khóa học"}</span>
          </div>
          <div className="flex items-center gap-3">
            <Link to="/" className="btn btn-primary">
              Thoát học
            </Link>
          </div>
        </div>
        <div className="px-4 pb-3 text-sm text-stone-600 max-w-7xl mx-auto w-full">
          {loadingProgress && <p>Đang kiểm tra tiến độ...</p>}
          {error && <p className="text-red-500">{error}</p>}
          {!loadingProgress && !error && <p>{progressMessage}</p>}
        </div>
        <div className="h-1 bg-stone-200">
          <div
            className="h-full bg-primary-600 transition-all"
            style={{
              width: `${Math.min(completionPercent || 0, 100)}%`,
            }}
          />
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 py-6">
        <Outlet context={{ progress, refreshProgress: fetchProgress }} />
      </div>
    </div>
  );
}
