import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { useAuth } from "./AuthContext";
import {
  createSupportThread,
  fetchStudentThreads,
  fetchSupportThread,
  sendSupportMessage,
  submitSupportRating,
} from "../services/support";

const SupportChatContext = createContext(null);

export function SupportChatProvider({ children }) {
  const { isAuthenticated, user } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [threads, setThreads] = useState([]);
  const [activeThread, setActiveThread] = useState(null);
  const [messages, setMessages] = useState([]);
  const [loadingThreads, setLoadingThreads] = useState(false);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [sending, setSending] = useState(false);
  const [creating, setCreating] = useState(false);
  const [entryContext, setEntryContext] = useState(null);
  const [hasUnread, setHasUnread] = useState(false);
  const pollTimer = useRef(null);

  const refreshThreads = useCallback(async () => {
    if (!isAuthenticated) {
      setThreads([]);
      setHasUnread(false);
      return;
    }
    setLoadingThreads(true);
    try {
      const { data } = await fetchStudentThreads({ page: 0, size: 25 });
      const list = Array.isArray(data?.data) ? data.data : Array.isArray(data) ? data : [];
      setThreads(list);
      setHasUnread(Boolean(list.some((t) => t.unreadForStudent)));
    } catch (err) {
      console.error("Failed to load support threads", err);
    } finally {
      setLoadingThreads(false);
    }
  }, [isAuthenticated]);

  const loadThread = useCallback(
    async (threadId) => {
      if (!threadId || !isAuthenticated) return null;
      setLoadingMessages(true);
      try {
        const { data } = await fetchSupportThread(threadId);
        setActiveThread(data);
        setMessages(data?.messages || []);
        return data;
      } catch (err) {
        console.error("Failed to load thread detail", err);
        return null;
      } finally {
        setLoadingMessages(false);
      }
    },
    [isAuthenticated],
  );

  const createThread = useCallback(
    async (payload) => {
      if (!isAuthenticated) throw new Error("Bạn cần đăng nhập để sử dụng chat hỗ trợ.");
      setCreating(true);
      try {
        const request = {
          ...(entryContext || {}),
          ...payload,
        };
        const { data } = await createSupportThread(request);
        setActiveThread(data);
        setMessages(data?.messages || []);
        await refreshThreads();
        setIsOpen(true);
        return data;
      } finally {
        setCreating(false);
      }
    },
    [entryContext, isAuthenticated, refreshThreads],
  );

  const sendMessage = useCallback(
    async ({ content, attachments }) => {
      if (!activeThread?.id) return null;
      setSending(true);
      try {
        const { data } = await sendSupportMessage(activeThread.id, {
          content,
          attachments,
        });
        setMessages((prev) => [...prev, data]);
        setActiveThread((prev) =>
          prev
            ? {
                ...prev,
                messages: [...(prev.messages || []), data],
                lastMessagePreview: data.content,
                lastMessageAt: data.createdAt,
              }
            : prev,
        );
        await refreshThreads();
        return data;
      } finally {
        setSending(false);
      }
    },
    [activeThread, refreshThreads],
  );

  const fetchNewMessages = useCallback(async () => {
    if (!activeThread?.id) return;
    try {
      const { data } = await fetchSupportThread(activeThread.id);
      if (data) {
        setActiveThread(data);
        setMessages(data.messages || []);
      }
    } catch (err) {
      console.error("Failed to poll messages", err);
    }
  }, [activeThread]);

  useEffect(() => {
    if (!isAuthenticated) {
      setActiveThread(null);
      setMessages([]);
      setThreads([]);
      setHasUnread(false);
      return;
    }
    refreshThreads();
  }, [isAuthenticated, refreshThreads]);

  useEffect(() => {
    if (!isOpen || !activeThread?.id) {
      if (pollTimer.current) {
        clearInterval(pollTimer.current);
      }
      return;
    }
    pollTimer.current = setInterval(fetchNewMessages, 8000);
    return () => {
      if (pollTimer.current) clearInterval(pollTimer.current);
    };
  }, [isOpen, activeThread, fetchNewMessages]);

  const openChat = useCallback(
    (context) => {
      if (context) {
        setEntryContext((prev) => ({ ...(prev || {}), ...context }));
      }
      setIsOpen(true);
      if (!activeThread && threads.length > 0) {
        loadThread(threads[0].id);
      }
    },
    [activeThread, loadThread, threads],
  );

  const closeChat = useCallback(() => setIsOpen(false), []);

  const submitRating = useCallback(
    async (ratingPayload) => {
      if (!activeThread?.id) return null;
      const { data } = await submitSupportRating(activeThread.id, ratingPayload);
      setActiveThread((prev) => (prev ? { ...prev, rating: data } : prev));
      return data;
    },
    [activeThread],
  );

  const value = useMemo(
    () => ({
      isOpen,
      openChat,
      closeChat,
      isAuthenticated,
      user,
      entryContext,
      setEntryContext,
      threads,
      loadingThreads,
      activeThread,
      setActiveThread,
      loadThread,
      createThread,
      messages,
      loadingMessages,
      sendMessage,
      sending,
      creating,
      refreshThreads,
      hasUnread,
      submitRating,
    }),
    [
      isOpen,
      openChat,
      closeChat,
      isAuthenticated,
      user,
      entryContext,
      threads,
      loadingThreads,
      activeThread,
      loadThread,
      createThread,
      messages,
      loadingMessages,
      sendMessage,
      sending,
      creating,
      refreshThreads,
      hasUnread,
      submitRating,
    ],
  );

  return <SupportChatContext.Provider value={value}>{children}</SupportChatContext.Provider>;
}

export const useSupportChat = () => {
  const ctx = useContext(SupportChatContext);
  if (!ctx) {
    throw new Error("useSupportChat must be used within SupportChatProvider");
  }
  return ctx;
};
