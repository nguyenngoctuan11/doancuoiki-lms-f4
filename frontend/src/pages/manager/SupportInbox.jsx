import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  managerAssignThread,
  managerChangeStatus,
  managerFetchThread,
  managerListThreads,
  managerSendMessage,
  managerTransferThread,
} from "../../services/support";
import { useAuth } from "../../context/AuthContext";
import { API_BASE_URL } from "../../api/httpClient";

const STATUS_OPTIONS = [
  { value: "ALL", label: "Tất cả" },
  { value: "NEW", label: "Chưa nhận" },
  { value: "IN_PROGRESS", label: "Đang xử lý" },
  { value: "WAITING_STUDENT", label: "Chờ học viên" },
  { value: "CLOSED", label: "Đã đóng" },
];

const STATUS_BADGE = {
  NEW: "bg-amber-100 text-amber-700",
  IN_PROGRESS: "bg-sky-100 text-sky-700",
  WAITING_STUDENT: "bg-purple-100 text-purple-700",
  CLOSED: "bg-stone-200 text-stone-600",
};

export default function SupportInbox() {
  const { user, initialised, isAuthenticated } = useAuth();
  const [filters, setFilters] = useState({ status: "ALL", keyword: "", mineOnly: false });
  const [threads, setThreads] = useState([]);
  const [meta, setMeta] = useState({ total: 0 });
  const [loadingThreads, setLoadingThreads] = useState(false);
  const [selectedThread, setSelectedThread] = useState(null);
  const [loadingThread, setLoadingThread] = useState(false);
  const [messageText, setMessageText] = useState("");
  const [sending, setSending] = useState(false);
  const [transferId, setTransferId] = useState("");
  const [error, setError] = useState("");
  const [alerts, setAlerts] = useState([]);

  const isManager = useMemo(
    () => Boolean(user?.roles?.some((role) => role?.toLowerCase() === "manager")),
    [user],
  );

  const metrics = useMemo(() => {
    const mine = threads.filter((thread) => thread.manager?.id === user?.id).length;
    return {
      total: meta.total || threads.length,
      inProgress: threads.filter((t) => t.status === "IN_PROGRESS").length,
      waiting: threads.filter((t) => t.status === "WAITING_STUDENT").length,
      mine,
    };
  }, [threads, meta, user]);

  const buildParams = useCallback(() => {
    const params = { page: 0, size: 40 };
    if (filters.status !== "ALL") params.status = filters.status;
    if (filters.keyword.trim()) params.studentKeyword = filters.keyword.trim();
    if (filters.mineOnly) params.mineOnly = true;
    return params;
  }, [filters]);

  const loadThreads = useCallback(async () => {
    if (!isAuthenticated || !isManager) return;
    setLoadingThreads(true);
    setError("");
    try {
      const { data } = await managerListThreads(buildParams());
      const list = Array.isArray(data?.data) ? data.data : [];
      setThreads(list);
      setMeta({ total: data?.totalElements ?? list.length });
    } catch (err) {
      setError(err?.response?.data?.message || "Không thể tải danh sách hội thoại.");
    } finally {
      setLoadingThreads(false);
    }
  }, [buildParams, isAuthenticated, isManager]);

  const loadThreadsRef = useRef(loadThreads);
  useEffect(() => {
    loadThreadsRef.current = loadThreads;
  }, [loadThreads]);

  const openThread = useCallback(
    async (threadId) => {
      if (!threadId || !isAuthenticated || !isManager) return;
      setLoadingThread(true);
      setError("");
      try {
        const { data } = await managerFetchThread(threadId);
        setSelectedThread(data);
      } catch (err) {
        setError(err?.response?.data?.message || "Không thể tải hội thoại.");
      } finally {
        setLoadingThread(false);
      }
    },
    [isAuthenticated, isManager],
  );

  useEffect(() => {
    if (!initialised || !isAuthenticated || !isManager) return;
    loadThreads();
  }, [initialised, isAuthenticated, isManager, loadThreads]);

  const pushAlert = useCallback((payload) => {
    const key = `${payload.id || "alert"}-${Date.now()}`;
    setAlerts((prev) => [...prev, { ...payload, key }]);
    setTimeout(() => {
      setAlerts((prev) => prev.filter((item) => item.key !== key));
    }, 6000);
  }, []);

  useEffect(() => {
    if (!isAuthenticated || !isManager) return undefined;
    const protocol = API_BASE_URL.startsWith("https") ? "wss" : "ws";
    const endpoint = `${API_BASE_URL.replace(/^https?/, protocol)}/ws-support`;
    let buffer = "";
    const socket = new WebSocket(endpoint);

    const sendFrame = (frame) => {
      if (socket.readyState === WebSocket.OPEN) {
        socket.send(frame);
      }
    };

    const handleFrame = (raw) => {
      const frame = parseStompFrame(raw);
      if (!frame) return;
      if (frame.command === "CONNECTED") {
        sendFrame("SUBSCRIBE\nid:manager-alerts\ndestination:/topic/support/manager-alerts\n\n\0");
        return;
      }
      if (frame.command !== "MESSAGE" || !frame.body) return;
      try {
        const payload = JSON.parse(frame.body);
        pushAlert({
          id: payload.id,
          title: payload.student?.fullName || "Học viên mới",
          subtitle: payload.courseTitle || payload.topic || "Yêu cầu hỗ trợ mới",
        });
        loadThreadsRef.current?.();
      } catch (err) {
        console.warn("Không thể parse thông báo mới", err);
      }
    };

    const onMessage = (event) => {
      if (typeof event.data !== "string") return;
      buffer += event.data;
      let frameEnd = buffer.indexOf("\0");
      while (frameEnd !== -1) {
        const frame = buffer.slice(0, frameEnd);
        buffer = buffer.slice(frameEnd + 1);
        if (frame.trim()) {
          handleFrame(frame);
        }
        frameEnd = buffer.indexOf("\0");
      }
    };

    socket.addEventListener("open", () => {
      sendFrame("CONNECT\naccept-version:1.2,1.1,1.0\nheart-beat:0,0\n\n\0");
    });
    socket.addEventListener("message", onMessage);
    socket.addEventListener("error", (event) => {
      console.error("Support socket error", event);
    });

    return () => {
      try {
        sendFrame("DISCONNECT\n\n\0");
      } catch {
        // ignore
      }
      socket.removeEventListener("message", onMessage);
      socket.close();
    };
  }, [isAuthenticated, isManager, pushAlert]);

  const handleAssign = async () => {
    if (!selectedThread?.id) return;
    try {
      await managerAssignThread(selectedThread.id);
      await Promise.all([openThread(selectedThread.id), loadThreads()]);
    } catch (err) {
      setError(err?.response?.data?.message || "Không thể nhận xử lý hội thoại.");
    }
  };

  const handleSendMessage = async (event) => {
    event.preventDefault();
    if (!selectedThread?.id || !messageText.trim()) return;
    setSending(true);
    try {
      const { data } = await managerSendMessage(selectedThread.id, { content: messageText.trim() });
      setSelectedThread((prev) =>
        prev
          ? {
              ...prev,
              messages: [...(prev.messages || []), data],
              lastMessagePreview: data.content,
              lastMessageAt: data.createdAt,
            }
          : prev,
      );
      setMessageText("");
      await loadThreads();
    } catch (err) {
      setError(err?.response?.data?.message || "Không thể gửi tin nhắn.");
    } finally {
      setSending(false);
    }
  };

  const handleStatusChange = async (status) => {
    if (!selectedThread?.id) return;
    try {
      await managerChangeStatus(selectedThread.id, { status });
      await Promise.all([openThread(selectedThread.id), loadThreads()]);
    } catch (err) {
      setError(err?.response?.data?.message || "Không thể cập nhật trạng thái.");
    }
  };

  const handleTransfer = async () => {
    if (!selectedThread?.id || !transferId.trim()) return;
    const newManagerId = Number(transferId);
    if (!Number.isFinite(newManagerId)) {
      setError("ID quản lý không hợp lệ.");
      return;
    }
    try {
      await managerTransferThread(selectedThread.id, { newManagerId });
      setTransferId("");
      await Promise.all([openThread(selectedThread.id), loadThreads()]);
    } catch (err) {
      setError(err?.response?.data?.message || "Không thể chuyển hội thoại.");
    }
  };

  if (!initialised) {
    return <div className="max-w-6xl mx-auto px-4 py-10 text-sm text-stone-500">Đang tải...</div>;
  }

  if (!isAuthenticated || !isManager) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-16 text-center">
        <p className="text-xl font-semibold text-stone-800">Bạn cần quyền manager để truy cập trang này.</p>
      </div>
    );
  }

  return (
    <>
      {alerts.length > 0 && (
        <div className="fixed bottom-6 left-6 z-40 flex flex-col gap-2">
          {alerts.map((alert) => (
            <div
              key={alert.key}
              className="w-64 rounded-2xl border border-primary-200 bg-white px-4 py-3 shadow-xl shadow-primary-200/40"
            >
              <p className="text-sm font-semibold text-stone-900">{alert.title}</p>
              <p className="text-xs text-stone-500">{alert.subtitle}</p>
            </div>
          ))}
        </div>
      )}

      <div className="min-h-screen bg-[#f5ede2] pb-16">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8 pt-10">
          <header className="rounded-[32px] bg-gradient-to-r from-primary-700 via-primary-600 to-[#C99E6D] text-white px-8 py-6 shadow-lg shadow-primary-900/20">
            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-5">
              <div>
                <p className="text-xs uppercase tracking-[0.5em] text-white/70">Manager Console</p>
                <h1 className="text-3xl font-semibold mt-2">Tư vấn học viên tiếng Anh</h1>
                <p className="text-sm text-white/80 mt-1">
                  Xin chào {user?.fullName || "quản lý"}, hãy đồng hành cùng học viên trong hành trình học tập.
                </p>
              </div>
              <div className="flex flex-wrap gap-3 text-sm">
                <FilterSelect filters={filters} setFilters={setFilters} loadThreads={loadThreads} />
              </div>
            </div>
          </header>

          {error && <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

          <StatsRow metrics={metrics} />

          <section className="grid gap-6 lg:grid-cols-[340px,1fr]">
            <ThreadColumn
              threads={threads}
              selectedThread={selectedThread}
              onSelect={openThread}
              loading={loadingThreads}
              filters={filters}
              setFilters={setFilters}
              loadThreads={loadThreads}
            />

            <ChatColumn
              selectedThread={selectedThread}
              loadingThread={loadingThread}
              messageText={messageText}
              setMessageText={setMessageText}
              sending={sending}
              handleSendMessage={handleSendMessage}
              handleAssign={handleAssign}
              handleStatusChange={handleStatusChange}
              handleTransfer={handleTransfer}
              transferId={transferId}
              setTransferId={setTransferId}
            />
          </section>
        </div>
      </div>
    </>
  );
}

function FilterSelect({ filters, setFilters, loadThreads }) {
  return (
    <>
      <select
        className="rounded-2xl bg-white/10 border border-white/30 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-white/60"
        value={filters.status}
        onChange={(e) => setFilters((prev) => ({ ...prev, status: e.target.value }))}
      >
        {STATUS_OPTIONS.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
      <input
        type="search"
        placeholder="Tìm học viên / khóa học..."
        className="rounded-2xl bg-white/10 border border-white/30 px-4 py-2 text-sm placeholder:text-white/60 focus:outline-none focus:ring-2 focus:ring-white/60"
        value={filters.keyword}
        onChange={(e) => setFilters((prev) => ({ ...prev, keyword: e.target.value }))}
      />
      <label className="flex items-center gap-2 text-xs font-medium text-white/80">
        <input
          type="checkbox"
          className="rounded border-white/60 text-white focus:ring-white"
          checked={filters.mineOnly}
          onChange={(e) => setFilters((prev) => ({ ...prev, mineOnly: e.target.checked }))}
        />
        Chỉ hội thoại của tôi
      </label>
      <button
        type="button"
        className="rounded-2xl bg-white/20 px-4 py-2 text-sm font-semibold hover:bg-white/30 transition"
        onClick={loadThreads}
      >
        Làm mới
      </button>
    </>
  );
}

function StatsRow({ metrics }) {
  const items = [
    { label: "Tổng yêu cầu", value: metrics.total || 0 },
    { label: "Đang xử lý", value: metrics.inProgress },
    { label: "Chờ học viên", value: metrics.waiting },
    { label: "Phụ trách của tôi", value: metrics.mine },
  ];
  return (
    <div className="grid gap-4 md:grid-cols-4">
      {items.map((item) => (
        <div
          key={item.label}
          className="rounded-2xl bg-white/70 border border-white shadow-sm shadow-primary-900/5 px-5 py-4 flex flex-col"
        >
          <p className="text-xs text-stone-500 uppercase tracking-[0.3em]">{item.label}</p>
          <span className="text-2xl font-semibold text-primary-700 mt-2">{item.value}</span>
        </div>
      ))}
    </div>
  );
}

function ThreadColumn({ threads, selectedThread, onSelect, loading, filters, setFilters, loadThreads }) {
  return (
    <div className="space-y-4">
      <div className="rounded-3xl bg-white shadow-lg shadow-primary-900/5 border border-stone-100">
        <div className="px-5 pt-5 pb-3 border-b border-stone-100">
          <p className="text-sm font-semibold text-stone-800">Danh sách hội thoại</p>
          <p className="text-xs text-stone-500">Chọn một hội thoại để xem chi tiết</p>
          <div className="mt-3">
            <input
              type="search"
              placeholder="Tìm nhanh..."
              className="w-full rounded-2xl border border-stone-200 px-3 py-2 text-sm"
              value={filters.keyword}
              onChange={(e) => setFilters((prev) => ({ ...prev, keyword: e.target.value }))}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  loadThreads();
                }
              }}
            />
          </div>
        </div>
        <div className="max-h-[520px] overflow-y-auto">
          {loading && <p className="p-4 text-sm text-stone-500">Đang tải danh sách...</p>}
          {!loading && threads.length === 0 && (
            <div className="p-6 text-center text-stone-400 text-sm">Chưa có yêu cầu nào phù hợp bộ lọc.</div>
          )}
          <ul className="divide-y divide-stone-100">
            {threads.map((thread) => (
              <li key={thread.id}>
                <button
                  type="button"
                  className={`w-full text-left px-4 py-3 transition ${
                    selectedThread?.id === thread.id ? "bg-primary-50" : "hover:bg-stone-50"
                  }`}
                  onClick={() => onSelect(thread.id)}
                >
                  <div className="flex items-center justify-between gap-2">
                    <p className="text-sm font-semibold text-stone-900 truncate">
                      {thread.student?.fullName || "Học viên ẩn danh"}
                    </p>
                    <span className={`text-[11px] px-2 py-0.5 rounded-full ${STATUS_BADGE[thread.status] || STATUS_BADGE.NEW}`}>
                      {statusLabel(thread.status)}
                    </span>
                  </div>
                  <p className="text-xs text-stone-500 truncate mt-0.5">
                    {(thread.courseTitle && `${thread.courseTitle} · `) || ""}
                    {thread.topic}
                  </p>
                  <p className="text-[11px] text-stone-400 truncate mt-1">
                    {thread.lastMessagePreview || "Chưa có tin nhắn"}
                  </p>
                  {thread.unreadForManager && <span className="text-[10px] text-primary-600">• Tin nhắn mới</span>}
                </button>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}

function ChatColumn({
  selectedThread,
  loadingThread,
  messageText,
  setMessageText,
  sending,
  handleSendMessage,
  handleAssign,
  handleStatusChange,
  handleTransfer,
  transferId,
  setTransferId,
}) {
  return (
    <div className="rounded-[34px] bg-white shadow-xl shadow-primary-900/10 border border-[#e2d4c4] flex flex-col min-h-[620px]">
      {loadingThread && <p className="p-4 text-sm text-stone-500">Đang tải hội thoại...</p>}
      {!loadingThread && !selectedThread && (
        <div className="flex-1 grid place-items-center text-sm text-stone-500 px-6 text-center">
          Chọn một hội thoại ở cột bên trái để bắt đầu trao đổi với học viên.
        </div>
      )}

      {selectedThread && !loadingThread && (
        <>
          <div className="px-6 py-5 border-b border-stone-100 flex flex-col gap-3 bg-gradient-to-r from-white to-[#f8f1e9] rounded-t-[34px]">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-xs uppercase tracking-[0.4em] text-primary-600">Hội thoại #{selectedThread.id}</p>
                <h2 className="text-2xl font-semibold text-stone-900 mt-1">{selectedThread.student?.fullName || "Học viên ẩn danh"}</h2>
                <p className="text-sm text-stone-500">
                  {selectedThread.courseTitle || "Không rõ khóa học"} · {selectedThread.topic}
                </p>
              </div>
              <div className="space-y-2 text-xs">
                <p className="text-stone-500">Phụ trách: {selectedThread.manager?.fullName || "Chưa gán"}</p>
                <div className="flex flex-wrap gap-2">
                  <button type="button" className="pill-btn" onClick={handleAssign}>
                    Nhận xử lý
                  </button>
                  <button type="button" className="pill-btn-muted" onClick={() => handleStatusChange("WAITING_STUDENT")}>
                    Chờ học viên
                  </button>
                  <button type="button" className="pill-btn-muted" onClick={() => handleStatusChange("CLOSED")}>
                    Đóng hội thoại
                  </button>
                </div>
              </div>
            </div>

            <div className="flex items-center gap-2 text-xs text-stone-500">
              <input
                type="number"
                placeholder="ID manager"
                className="w-32 rounded-xl border border-stone-300 px-3 py-1"
                value={transferId}
                onChange={(e) => setTransferId(e.target.value)}
              />
              <button type="button" className="text-primary-600 font-semibold" onClick={handleTransfer}>
                Chuyển hội thoại
              </button>
            </div>
          </div>

          <div className="flex-1 overflow-y-auto px-6 py-5 space-y-4 bg-[#fdf9f4]">
            {(selectedThread.messages || []).map((message) => (
              <ThreadMessage key={message.id} message={message} />
            ))}
          </div>

          <form className="px-6 py-5 border-t border-stone-100 space-y-3 bg-white rounded-b-[34px]" onSubmit={handleSendMessage}>
            <textarea
              className="w-full rounded-2xl border border-stone-200 px-4 py-3 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-primary-200 bg-stone-50"
              rows={3}
              placeholder="Nhập phản hồi cho học viên..."
              value={messageText}
              onChange={(e) => setMessageText(e.target.value)}
            />
            <div className="flex items-center justify-between text-xs text-stone-500">
              {selectedThread.rating?.rating && (
                <span>Học viên đã đánh giá {selectedThread.rating.rating}/5⭐</span>
              )}
              <button
                type="submit"
                disabled={sending || !messageText.trim()}
                className="rounded-2xl bg-gradient-to-r from-primary-600 to-[#c58d5c] px-6 py-2 text-sm font-semibold text-white disabled:opacity-50"
              >
                {sending ? "Đang gửi..." : "Gửi trả lời"}
              </button>
            </div>
          </form>
        </>
      )}
    </div>
  );
}

function ThreadMessage({ message }) {
  const isManager = message.senderType === "manager";
  const bubbleClass = isManager
    ? "bg-gradient-to-r from-primary-600 to-[#c58d5c] text-white rounded-2xl rounded-br-sm shadow-lg shadow-primary-900/30"
    : message.senderType === "system"
      ? "mx-auto bg-white border border-dashed border-stone-300 text-stone-600 text-center rounded-2xl text-xs"
      : "bg-white text-stone-800 rounded-2xl rounded-bl-sm border border-stone-100";
  return (
    <div className={`max-w-[85%] ${isManager ? "ml-auto" : ""}`}>
      <div className={`px-4 py-3 text-sm ${bubbleClass}`}>
        <p className="text-[11px] uppercase tracking-wide opacity-70 mb-1">
          {message.sender?.fullName || message.senderType}
        </p>
        <p className="whitespace-pre-line">{message.content}</p>
        {!!message.attachments?.length && (
          <div className="mt-2 space-y-2">
            {message.attachments.map((url) =>
              isImageAttachment(url) ? (
                <a
                  key={url}
                  href={url}
                  target="_blank"
                  rel="noreferrer"
                  className="block rounded-xl overflow-hidden border border-white/40 shadow"
                >
                  <img src={url} alt="Đính kèm" className="max-w-[220px]" />
                </a>
              ) : (
                <a
                  key={url}
                  href={url}
                  target="_blank"
                  rel="noreferrer"
                  className="block text-xs underline"
                >
                  Tải tệp đính kèm
                </a>
              ),
            )}
          </div>
        )}
        <p className="mt-1 text-[10px] opacity-70">{new Date(message.createdAt).toLocaleString("vi-VN")}</p>
      </div>
    </div>
  );
}

const IMAGE_EXT = /\.(png|jpe?g|gif|bmp|webp|svg)$/i;
const isImageAttachment = (url) => {
  if (!url) return false;
  const normalized = url.split(/[?#]/)[0] || "";
  return IMAGE_EXT.test(normalized);
};

function statusLabel(status) {
  switch (status) {
    case "IN_PROGRESS":
      return "Đang xử lý";
    case "WAITING_STUDENT":
      return "Chờ học viên";
    case "CLOSED":
      return "Đã đóng";
    case "NEW":
      return "Chưa nhận";
    default:
      return "Không rõ";
  }
}

function parseStompFrame(raw) {
  if (!raw) return null;
  const normalized = raw.replace(/\r/g, "");
  const lines = normalized.split("\n");
  const command = lines.shift();
  const emptyIndex = lines.indexOf("");
  const headerLines = emptyIndex >= 0 ? lines.slice(0, emptyIndex) : lines;
  const bodyLines = emptyIndex >= 0 ? lines.slice(emptyIndex + 1) : [];
  const headers = {};
  headerLines.forEach((line) => {
    if (!line) return;
    const [key, ...rest] = line.split(":");
    headers[key] = rest.join(":");
  });
  const body = bodyLines.join("\n");
  return { command, headers, body };
}
