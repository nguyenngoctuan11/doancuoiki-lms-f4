import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useOutletContext, useParams, useNavigate } from "react-router-dom";
import LessonSidebar from "../../components/LessonSidebar";
import httpClient, { API_BASE_URL } from "../../api/httpClient";

function PlayerHeader({ lesson }) {
  if (!lesson) return null;
  return (
    <div className="mb-4">
      <div className="text-xs uppercase tracking-wide text-stone-500">Bài học</div>
      <h1 className="text-xl md:text-2xl font-bold text-stone-900">{lesson.title}</h1>
    </div>
  );
}

function ContentTabs({ active, setActive }) {
  const tabs = [
    { id: "video", name: "Video" },
    { id: "notes", name: "Ghi chú" },
    { id: "resources", name: "Tài liệu" },
  ];
  return (
    <div className="border-b border-stone-200 flex items-center gap-4 text-sm">
      {tabs.map((tab) => (
        <button
          key={tab.id}
          onClick={() => setActive(tab.id)}
          className={`px-2 py-2 border-b-2 -mb-px ${
            active === tab.id ? "border-primary-600 text-stone-900" : "border-transparent text-stone-600 hover:text-stone-900"
          }`}
        >
          {tab.name}
        </button>
      ))}
    </div>
  );
}

export default function CoursePlayer() {
  const { courseId } = useParams();
  const navigate = useNavigate();
  const { progress, refreshProgress } = useOutletContext() || {};

  const [modules, setModules] = useState([]);
  const [current, setCurrent] = useState(null);
  const [tab, setTab] = useState("video");
  const [videoUnlocked, setVideoUnlocked] = useState(true);
  const [navigating, setNavigating] = useState(false);
  const [exams, setExams] = useState([]);
  const [loadingExams, setLoadingExams] = useState(false);
  const pendingCompleteRef = useRef(new Set());
  const videoElementRef = useRef(null);
  const [resources, setResources] = useState([]);
  const [resourcesLoading, setResourcesLoading] = useState(false);
  const [resourceError, setResourceError] = useState("");
  const [notes, setNotes] = useState([]);
  const [notesLoading, setNotesLoading] = useState(false);
  const [notesError, setNotesError] = useState("");
  const [newNote, setNewNote] = useState("");
  const [noteSubmitting, setNoteSubmitting] = useState(false);
  const [noteFormError, setNoteFormError] = useState("");
  const [commentDrafts, setCommentDrafts] = useState({});
  const [commentErrors, setCommentErrors] = useState({});
  const [commentSubmitting, setCommentSubmitting] = useState({});

  const completedLessonIds = progress?.completedLessonIds || [];
  const [localCompleted, setLocalCompleted] = useState(() => new Set(completedLessonIds));

  useEffect(() => {
    setLocalCompleted(new Set(completedLessonIds));
  }, [completedLessonIds]);

  const completedSet = useMemo(() => localCompleted, [localCompleted]);

  const resolveVideoSource = useCallback((url) => {
    if (!url) return { type: "none", url: null };
    const normalized = url.trim();
    const lower = normalized.toLowerCase();
    if (lower.includes("youtube.com") || lower.includes("youtu.be")) {
      let embed = normalized;
      if (lower.includes("watch?v=")) {
        const idPart = normalized.split("watch?v=")[1]?.split("&")[0];
        if (idPart) embed = `https://www.youtube.com/embed/${idPart}`;
      } else if (lower.includes("youtu.be/")) {
        const idPart = normalized.split("youtu.be/")[1]?.split("?")[0];
        if (idPart) embed = `https://www.youtube.com/embed/${idPart}`;
      }
      return { type: "youtube", url: embed };
    }
    const absolute = normalized.startsWith("/") ? `${API_BASE_URL}${normalized}` : normalized;
    return { type: "video", url: absolute };
  }, []);

  useEffect(() => {
    if (!courseId) return;
    const API = API_BASE_URL;
    const asset = (u) => (u && u.startsWith("/") ? `${API}${u}` : u);
    fetch(`${API}/api/public/courses/${courseId}/detail-sql`)
      .then((r) => r.json())
      .then((data) => {
        const mods = (data?.modules || []).map((m) => ({
          id: m.id,
          title: m.title,
          lessons: (m.lessons || []).map((l) => ({
            id: l.id,
            title: l.title,
            time: l.duration_seconds
              ? `${Math.floor(l.duration_seconds / 60)}:${String(l.duration_seconds % 60).padStart(2, "0")}`
              : "",
            video_url: asset(l.video_url),
          })),
        }));
        setModules(mods);
      })
      .catch((e) => console.error("Load course detail failed", e));
  }, [courseId]);

  const flatLessons = useMemo(() => modules.flatMap((m) => m.lessons || []), [modules]);

  useEffect(() => {
    if (!current && flatLessons.length) {
      const firstWithVideo = flatLessons.find((l) => !!l.video_url);
      setCurrent(firstWithVideo || flatLessons[0]);
    }
  }, [flatLessons, current]);

  useEffect(() => {
    if (!courseId) return;
    setLoadingExams(true);
    httpClient
      .get(`/api/student/exams/courses/${courseId}`)
      .then(({ data }) => setExams(Array.isArray(data) ? data : []))
      .catch(() => setExams([]))
      .finally(() => setLoadingExams(false));
  }, [courseId]);

  const loadLessonResources = useCallback(
    async (lessonId) => {
      if (!courseId || !lessonId) {
        setResources([]);
        setResourceError("");
        return;
      }
      setResourcesLoading(true);
      setResourceError("");
      try {
        const { data } = await httpClient.get(`/api/lesson-resources/courses/${courseId}/lessons/${lessonId}`);
        setResources(Array.isArray(data) ? data : []);
      } catch (err) {
        const message = err?.response?.data?.message || "Không thể tải tài liệu.";
        setResourceError(message);
        setResources([]);
      } finally {
        setResourcesLoading(false);
      }
    },
    [courseId],
  );

  useEffect(() => {
    if (!current) {
      setVideoUnlocked(true);
      setResources([]);
      setResourceError("");
      return;
    }
    const completed = completedSet.has(current.id);
    const isVideo = resolveVideoSource(current.video_url).type !== "none";
    setVideoUnlocked(!isVideo || completed);
    loadLessonResources(current.id);
  }, [current, completedSet, resolveVideoSource, loadLessonResources]);

  const loadLessonNotes = useCallback(
    async (lessonId) => {
      if (!courseId || !lessonId) {
        setNotes([]);
        setNotesError("");
        return;
      }
      setNotesLoading(true);
      setNotesError("");
      try {
        const { data } = await httpClient.get(`/api/lesson-notes/courses/${courseId}/lessons/${lessonId}`);
        setNotes(Array.isArray(data) ? data : []);
      } catch (err) {
        const message = err?.response?.data?.message || "Không tải được ghi chú.";
        setNotesError(message);
        setNotes([]);
      } finally {
        setNotesLoading(false);
      }
    },
    [courseId],
  );

  useEffect(() => {
    if (!current?.id) {
      setNotes([]);
      setNotesError("");
      return;
    }
    loadLessonNotes(current.id);
  }, [current, loadLessonNotes]);

  const handleCreateNote = async () => {
    if (!current?.id) return;
    const content = newNote.trim();
    if (!content) {
      setNoteFormError("Vui lòng nhập nội dung ghi chú.");
      return;
    }
    setNoteSubmitting(true);
    setNoteFormError("");
    try {
      const { data } = await httpClient.post(`/api/lesson-notes/courses/${courseId}/lessons/${current.id}`, { content });
      setNewNote("");
      if (data) {
        setNotes((prev) => {
          const next = prev.filter((item) => item.id !== data.id);
          return [...next, data].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
        });
      } else {
        await loadLessonNotes(current.id);
      }
    } catch (err) {
      const message = err?.response?.data?.message || "Không thể gửi ghi chú.";
      setNoteFormError(message);
    } finally {
      setNoteSubmitting(false);
    }
  };

  const handleCommentChange = (noteId, value) => {
    setCommentDrafts((prev) => ({ ...prev, [noteId]: value }));
    if (commentErrors[noteId]) {
      setCommentErrors((prev) => {
        const next = { ...prev };
        delete next[noteId];
        return next;
      });
    }
  };

  const handleSubmitComment = async (noteId) => {
    const content = (commentDrafts[noteId] || "").trim();
    if (!content) {
      setCommentErrors((prev) => ({ ...prev, [noteId]: "Vui lòng nhập nội dung phản hồi." }));
      return;
    }
    setCommentSubmitting((prev) => ({ ...prev, [noteId]: true }));
    try {
      const { data } = await httpClient.post(`/api/lesson-notes/${noteId}/comments`, { content });
      if (data) {
        setNotes((prev) => prev.map((item) => (item.id === data.id ? data : item)));
        setCommentDrafts((prev) => ({ ...prev, [noteId]: "" }));
      } else {
        await loadLessonNotes(current?.id);
      }
    } catch (err) {
      const message = err?.response?.data?.message || "Không thể gửi phản hồi.";
      setCommentErrors((prev) => ({ ...prev, [noteId]: message }));
    } finally {
      setCommentSubmitting((prev) => ({ ...prev, [noteId]: false }));
    }
  };

  const formatDateTime = (value) => {
    if (!value) return "";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "";
    return date.toLocaleString("vi-VN", { hour12: false });
  };

  const markLessonComplete = useCallback(
    async (lesson, { silent = false, onSuccess } = {}) => {
      if (!lesson?.id) return true;
      if (completedSet.has(lesson.id) || pendingCompleteRef.current.has(lesson.id)) {
        onSuccess?.();
        return true;
      }
      pendingCompleteRef.current.add(lesson.id);
      try {
        await httpClient.post(`/api/student/progress/lessons/${lesson.id}/complete`);
        onSuccess?.();
        setLocalCompleted((prev) => {
          const next = new Set(prev);
          next.add(lesson.id);
          return next;
        });
        await refreshProgress?.();
        return true;
      } catch (err) {
        if (!silent) {
          const msg = err?.response?.data?.message || "Không thể lưu tiến độ học tập. Vui lòng thử lại.";
          alert(msg);
        }
        return false;
      } finally {
        pendingCompleteRef.current.delete(lesson.id);
      }
    },
    [completedSet, refreshProgress],
  );

  const handleVideoProgress = async (event) => {
    if (!current?.video_url) return;
    if (completedSet.has(current.id)) return;
    if (pendingCompleteRef.current.has(current.id)) return;
    const video = event.currentTarget;
    videoElementRef.current = video;
    const duration = video.duration || 0;
    if (!duration) return;
    if (video.currentTime / duration >= 0.97) {
      await markLessonComplete(current, { silent: true, onSuccess: () => setVideoUnlocked(true) });
    }
  };

  const handleVideoEnded = async () => {
    if (!current?.video_url) return;
    await markLessonComplete(current, { silent: true, onSuccess: () => setVideoUnlocked(true) });
  };

  const handleManualComplete = async () => {
    if (!current || completedSet.has(current.id)) return;
    await markLessonComplete(current, { silent: true, onSuccess: () => setVideoUnlocked(true) });
  };

  const goPrev = () => {
    const index = flatLessons.findIndex((l) => l.id === current?.id);
    if (index > 0) setCurrent(flatLessons[index - 1]);
  };

  const goNext = async () => {
    if (!current || navigating) return;
    const isVideo = resolveVideoSource(current.video_url).type !== "none";
    if (isVideo && !(completedSet.has(current.id) || videoUnlocked)) return;
    try {
      setNavigating(true);
      if (!isVideo && !completedSet.has(current.id)) {
        const ok = await markLessonComplete(current);
        if (!ok) return;
      }
      const index = flatLessons.findIndex((l) => l.id === current.id);
      if (index < flatLessons.length - 1) setCurrent(flatLessons[index + 1]);
    } finally {
      setNavigating(false);
    }
  };

  const videoSource = resolveVideoSource(current?.video_url);
  const lessonCompleted = current ? completedSet.has(current.id) : false;
  const disableNext = videoSource.type !== "none" && !lessonCompleted && !videoUnlocked;
  const totalLessons = progress?.totalLessons ?? flatLessons.length;
  const completedLessons = completedSet.size || progress?.completedLessons || 0;
  const completionPercent = totalLessons ? Math.round((completedLessons / totalLessons) * 100) : 0;
  const canShowExams = completionPercent >= 100 && exams.length > 0;

  const canAccessLesson = (lessonId) => {
    if (!lessonId) return false;
    const idx = flatLessons.findIndex((l) => l.id === lessonId);
    if (idx <= 0) return true;
    for (let i = 0; i < idx; i += 1) {
      if (!completedSet.has(flatLessons[i].id)) {
        return false;
      }
    }
    return true;
  };

  const handleSelectLesson = (lesson) => {
    if (!lesson?.id) return;
    if (!canAccessLesson(lesson.id)) {
      alert("Bạn cần hoàn thành bài trước để mở bài này.");
      return;
    }
    setCurrent(lesson);
  };

  const goToExam = (examId) => {
    navigate(`/courses/${courseId}/exams/${examId}`);
  };

  const formatExamMeta = (exam) => {
    const minutes = exam?.timeLimitSec ? Math.round(exam.timeLimitSec / 60) : null;
    const timeLabel = minutes ? `${minutes} phút` : "Không giới hạn";
    return `${exam?.questionCount ?? 0} câu · ${timeLabel}`;
  };

  return (
    <div className="grid md:grid-cols-[1fr_320px] gap-6">
      <div>
        <PlayerHeader lesson={current} />

        <div className="mb-6 rounded-xl border border-stone-200 bg-white p-4 shadow-sm flex flex-wrap items-center gap-4">
          <div>
            <p className="text-xs uppercase tracking-[0.3em] text-stone-400">Tiến độ khóa học</p>
            <p className="text-2xl font-semibold text-stone-900">
              {completedLessons}/{totalLessons} bài ({completionPercent}%)
            </p>
          </div>
          <div className="flex-1 h-2 rounded-full bg-stone-100">
            <div className="h-full rounded-full bg-primary-600 transition-all" style={{ width: `${Math.min(100, completionPercent)}%` }} />
          </div>
          <div className="text-sm text-primary-700 font-medium">
            {completionPercent >= 100 ? "Bạn đã hoàn thành toàn bộ khóa học!" : "Tiếp tục học để mở bài kiểm tra."}
          </div>
        </div>

        {canShowExams && (
          <div className="mb-6 border border-primary-100 bg-primary-50 rounded-xl p-4 space-y-3">
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-sm text-primary-600 font-semibold uppercase">Bài kiểm tra</p>
                <p className="text-stone-800">Hoàn thành bài kiểm tra để nhận chứng chỉ.</p>
              </div>
              {loadingExams && <span className="text-xs text-stone-500">Đang tải...</span>}
            </div>
            <div className="space-y-2">
              {exams.map((exam) => (
                <div key={exam.id} className="flex flex-wrap items-center justify-between gap-3 bg-white rounded-lg px-4 py-3 shadow-sm border border-primary-100">
                  <div>
                    <p className="font-medium text-stone-900">{exam.title || `Bài kiểm tra #${exam.id}`}</p>
                    <p className="text-sm text-stone-500">{formatExamMeta(exam)}</p>
                  </div>
                  <button onClick={() => goToExam(exam.id)} className="btn btn-primary">
                    Làm bài
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {videoSource.type === "video" ? (
          <video
            src={videoSource.url}
            controls
            className="aspect-video w-full rounded-xl bg-black"
            onTimeUpdate={handleVideoProgress}
            onEnded={handleVideoEnded}
            ref={videoElementRef}
          />
        ) : videoSource.type === "youtube" ? (
          <div className="aspect-video w-full overflow-hidden rounded-xl bg-black">
            <iframe
              src={videoSource.url}
              title={current?.title || "Lesson video"}
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
              allowFullScreen
              className="h-full w-full border-0"
            />
          </div>
        ) : (
          <div className="aspect-video rounded-xl bg-gradient-to-br from-primary-200 to-primary-100 flex items-center justify-center text-stone-500">
            Chưa có video cho bài học này
          </div>
        )}

        <div className="mt-4">
          <ContentTabs active={tab} setActive={setTab} />
          <div className="mt-4 text-sm text-stone-700">
            {tab === "video" && <p>Nội dung tóm tắt bài học, ghi chú quan trọng và liên kết hữu ích.</p>}

            {tab === "notes" && (
              <div className="space-y-4">
                <div className="rounded-2xl border border-stone-200 bg-white p-4 shadow-sm">
                  <h4 className="font-semibold text-stone-900 mb-2">Thêm ghi chú</h4>
                  <textarea
                    className="input border-stone-300 w-full min-h-[140px]"
                    placeholder="Ghi chú của bạn..."
                    value={newNote}
                    onChange={(e) => setNewNote(e.target.value)}
                  />
                  {noteFormError && <p className="text-sm text-red-500 mt-2">{noteFormError}</p>}
                  <div className="text-right mt-3">
                    <button
                      className="btn btn-primary"
                      onClick={handleCreateNote}
                      disabled={noteSubmitting}
                    >
                      {noteSubmitting ? "Đang gửi..." : "Gửi ghi chú"}
                    </button>
                  </div>
                </div>

                <div className="space-y-3">
                  {notesLoading && <p>Đang tải ghi chú...</p>}
                  {!notesLoading && notesError && <p className="text-red-500">{notesError}</p>}
                  {!notesLoading && !notesError && notes.length === 0 && (
                    <div className="rounded-2xl border border-dashed border-stone-300 bg-white p-6 text-center text-stone-500">
                      <p>Chưa có ghi chú nào cho bài học này.</p>
                    </div>
                  )}
                  {!notesLoading &&
                    !notesError &&
                    notes.map((note) => {
                      const status = (note.status || "").toLowerCase();
                      const resolved = status === "resolved";
                      return (
                        <div key={note.id} className="rounded-2xl border border-stone-200 bg-white p-4 shadow-sm space-y-3">
                          <div className="flex flex-wrap items-center justify-between gap-3">
                            <div>
                              <p className="font-semibold text-stone-900">{note.author?.name || "Học viên"}</p>
                              <p className="text-xs text-stone-500">
                                Gửi lúc {formatDateTime(note.createdAt)}
                                {note.mine && <span className="ml-2 text-primary-600 font-medium">• Của bạn</span>}
                              </p>
                            </div>
                            <span
                              className={`text-xs font-semibold px-3 py-1 rounded-full ${
                                resolved ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700"
                              }`}
                            >
                              {resolved ? "Đã phản hồi" : "Chờ phản hồi"}
                            </span>
                          </div>
                          <p className="text-stone-800 whitespace-pre-line">{note.content}</p>

                          {note.comments && note.comments.length > 0 && (
                            <div className="bg-stone-50 rounded-xl p-3 space-y-3">
                              {note.comments.map((comment) => (
                                <div key={comment.id} className="text-sm border-b border-stone-200 pb-2 last:border-b-0 last:pb-0">
                                  <div className="flex justify-between text-xs text-stone-500 mb-1">
                                    <span>
                                      {comment.author?.name || "Giảng viên"} •{" "}
                                      {comment.author?.role === "teacher"
                                        ? "Giảng viên"
                                        : comment.author?.role === "manager"
                                          ? "Quản lý"
                                          : "Học viên"}
                                    </span>
                                    <span>{formatDateTime(comment.createdAt)}</span>
                                  </div>
                                  <p className="text-stone-700 whitespace-pre-line">{comment.content}</p>
                                </div>
                              ))}
                            </div>
                          )}

                          {note.mine && (
                            <div className="space-y-2">
                              <textarea
                                className="input border-stone-300 w-full min-h-[100px]"
                                placeholder="Phản hồi lại giảng viên..."
                                value={commentDrafts[note.id] || ""}
                                onChange={(e) => handleCommentChange(note.id, e.target.value)}
                              />
                              {commentErrors[note.id] && <p className="text-sm text-red-500">{commentErrors[note.id]}</p>}
                              <div className="text-right">
                                <button
                                  className="btn btn-primary"
                                  onClick={() => handleSubmitComment(note.id)}
                                  disabled={commentSubmitting[note.id]}
                                >
                                  {commentSubmitting[note.id] ? "Đang gửi..." : "Gửi phản hồi"}
                                </button>
                              </div>
                            </div>
                          )}
                        </div>
                      );
                    })}
                </div>
              </div>
            )}

            {tab === "resources" && (
              <div className="space-y-3">
                {resourcesLoading && <p>Đang tải tài liệu...</p>}
                {!resourcesLoading && resourceError && <p className="text-red-500">{resourceError}</p>}
                {!resourcesLoading && !resourceError && resources.length === 0 && (
                  <div className="rounded-2xl border border-dashed border-stone-300 bg-white p-6 text-center text-stone-500">
                    <p>Chưa có tài liệu cho bài học này.</p>
                  </div>
                )}
                {!resourcesLoading &&
                  !resourceError &&
                  resources.map((item) => (
                    <div
                      key={item.id}
                      className="rounded-2xl border border-stone-200 bg-white p-4 shadow-sm flex flex-wrap items-center justify-between gap-3"
                    >
                      <div>
                        <p className="font-semibold text-stone-900">{item.title}</p>
                        {item.description && <p className="text-sm text-stone-600">{item.description}</p>}
                        <div className="text-xs text-stone-500 flex gap-2">
                          <span>{item.sourceType === "link" ? "Liên kết" : "Tệp tin"}</span>
                          <span>• Phạm vi: {item.visibility || "Học viên đã ghi danh"}</span>
                          <span>• Tải xuống: {item.downloadCount || 0}</span>
                        </div>
                      </div>
                      <button
                        className="btn border-stone-300 hover:border-stone-400"
                        onClick={() => {
                          httpClient
                            .get(`/api/lesson-resources/${item.id}/download`)
                            .then(({ data }) => {
                              const url = data?.redirectUrl || data?.url;
                              if (url) {
                                const absolute = url.startsWith("http") ? url : `${API_BASE_URL}${url}`;
                                window.open(absolute, "_blank");
                              } else {
                                alert("Không tìm thấy đường dẫn tải về.");
                              }
                            })
                            .catch((err) => {
                              const msg = err?.response?.data?.message || "Không thể mở tài liệu.";
                              alert(msg);
                            });
                        }}
                      >
                        Tải về
                      </button>
                    </div>
                  ))}
              </div>
            )}
          </div>
        </div>

        <div className="mt-6 flex flex-wrap items-center gap-3 justify-between">
          <div className="flex gap-3">
            <button onClick={goPrev} className="btn border-stone-300 hover:border-stone-400">
              Bài trước
            </button>
            <button
              onClick={goNext}
              disabled={disableNext || navigating}
              className={`btn border-stone-300 hover:border-stone-400 ${disableNext ? "opacity-60 cursor-not-allowed" : ""}`}
            >
              Bài tiếp theo
            </button>
          </div>
          {videoSource.type !== "none" && !lessonCompleted && !videoUnlocked && (
            <div className="flex flex-col gap-1 text-xs text-stone-500">
              <span>Hãy xem hết video để mở bài tiếp theo.</span>
              {videoSource.type === "youtube" && (
                <button onClick={handleManualComplete} className="btn btn-xs border-stone-300 text-stone-600">
                  Đánh dấu đã xem
                </button>
              )}
            </div>
          )}
        </div>

        <div className="mt-4 text-sm text-stone-600">
          Tiến độ khóa học: {completedLessons}/{totalLessons} bài
        </div>
      </div>

      <LessonSidebar modules={modules} activeLessonId={current?.id} completedLessonIds={completedLessonIds} onSelect={handleSelectLesson} />
    </div>
  );
}
