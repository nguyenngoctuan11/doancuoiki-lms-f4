import { useEffect, useMemo, useRef, useState } from "react";
import { useSupportChat } from "../../context/SupportChatContext";
import { uploadImage } from "../../services/upload";
import { useNavigate } from "react-router-dom";
import AssistantAvatar from "../../assets/support-bot.svg";



const TOPIC_OPTIONS = [

  { value: "course_advice", label: "Tư vấn chọn khóa học" },

  { value: "lesson_issue", label: "Vấn đề trong bài học" },

  { value: "payment_issue", label: "Thanh toán & ưu đãi" },

  { value: "technical_issue", label: "Lỗi kỹ thuật" },

];


const ENGLISH_SUGGESTIONS = [
  {
    id: "ielts-roadmap",
    title: "Lộ trình IELTS 6.5+",
    message: "Mình muốn tư vấn lộ trình để đạt IELTS 6.5+ trong 6 tháng, hiện tại đang ở band 5.0.",
    topic: "course_advice",
  },
  {
    id: "schedule",
    title: "Sắp xếp lịch học",
    message: "Cho mình hỏi lịch học phù hợp với người đi làm buổi tối/ cuối tuần.",
    topic: "course_advice",
  },
  {
    id: "tuition",
    title: "Học phí & khuyến mãi",
    message: "Nhờ tư vấn học phí các khóa tiếng Anh giao tiếp và ưu đãi hiện có.",
    topic: "payment_issue",
  },
  {
    id: "speaking",
    title: "Cần lớp Speaking",
    message: "Mình cần khóa tập trung Speaking để chuẩn bị phỏng vấn bằng tiếng Anh.",
    topic: "lesson_issue",
  },
  {
    id: "placement",
    title: "Test xếp lớp",
    message: "Mình muốn đăng ký test xếp lớp online cho khóa IELTS.",
    topic: "course_advice",
  },
  {
    id: "tech",
    title: "Sự cố kỹ thuật",
    message: "Mình bị lỗi khi xem video bài học tiếng Anh, vui lòng hỗ trợ.",
    topic: "technical_issue",
  },
];


const STATUS_BADGE = {

  NEW: "bg-amber-100 text-amber-700",

  IN_PROGRESS: "bg-sky-100 text-sky-700",

  WAITING_STUDENT: "bg-purple-100 text-purple-700",

  CLOSED: "bg-stone-200 text-stone-600",

};





export default function SupportChatWidget() {

  const navigate = useNavigate();

  const {

    isOpen,

    openChat,

    closeChat,

    isAuthenticated,

    entryContext,

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

    hasUnread,

    submitRating,

  } = useSupportChat();



  const [topic, setTopic] = useState(TOPIC_OPTIONS[0].value);

  const [description, setDescription] = useState("");

  const [newAttachments, setNewAttachments] = useState([]);

  const [messageText, setMessageText] = useState("");

  const [messageAttachments, setMessageAttachments] = useState([]);

  const [uploading, setUploading] = useState(false);

  const [showHistory, setShowHistory] = useState(false);

  const [rating, setRating] = useState(0);

  const [ratingComment, setRatingComment] = useState("");

  const messagesEndRef = useRef(null);



  useEffect(() => {

    if (messagesEndRef.current) {

      messagesEndRef.current.scrollIntoView({ behavior: "smooth" });

    }

  }, [messages, isOpen]);



  useEffect(() => {

    if (!isOpen) {

      setMessageText("");

      setMessageAttachments([]);

      setShowHistory(false);

    }

  }, [isOpen]);



  const headerSubtitle = useMemo(() => {

    if (!isAuthenticated) return "Đăng nhập để được tư vấn";

    if (activeThread?.courseTitle) return activeThread.courseTitle;

    if (entryContext?.courseTitle) return entryContext.courseTitle;

    return "Sẵn sàng hỗ trợ bạn";

  }, [isAuthenticated, activeThread, entryContext]);



  const handleSelectThread = async (threadId) => {

    setMessageAttachments([]);

    if (activeThread?.id === threadId) {

      setShowHistory(false);

      return;

    }

    await loadThread(threadId);

    setShowHistory(false);

  };



  const handleStartNew = () => {

    setActiveThread(null);

    setMessageAttachments([]);

    setRating(0);

    setRatingComment("");

    setShowHistory(false);

  };



  const handleCreateThread = async (e) => {

    e.preventDefault();

    if (!description.trim()) return;

    await createThread({

      topic,

      message: description,

      attachments: newAttachments.map((f) => f.url),

      courseId: entryContext?.courseId,

      subject: entryContext?.courseTitle,

      origin: entryContext?.origin,

    });

    setDescription("");

    setNewAttachments([]);

    setShowHistory(false);

  };



  const handleSendMessage = async (e) => {

    e.preventDefault();

    if (!messageText.trim()) return;

    await sendMessage({

      content: messageText,

      attachments: messageAttachments.map((f) => f.url),

    });

    setMessageText("");

    setMessageAttachments([]);

  };



  const handleUpload = async (file, targetSetter) => {

    if (!file) return;

    setUploading(true);

    try {

      const { data } = await uploadImage(file);

      targetSetter((prev) => [...prev, { url: data.url, name: file.name }]);

    } catch (error) {

      console.error("Upload failed", error);

    } finally {

      setUploading(false);

    }

  };



  const handleRatingSubmit = async (e) => {

    e.preventDefault();

    if (!rating || !activeThread?.id) return;

    await submitRating({

      rating,

      comment: ratingComment || undefined,

    });

  };



  const handleSuggestion = (suggestion) => {

    setTopic(suggestion.topic || TOPIC_OPTIONS[0].value);

    setDescription(suggestion.message);

    setShowHistory(false);

    if (!isOpen) {

      openChat();

    }

  };



  const floatingButton = (

    <button

      type="button"

      aria-label="Mở chat hỗ trợ"

      onClick={() => (isOpen ? closeChat() : openChat())}

      className="relative w-16 h-16 rounded-full shadow-[0_12px_30px_rgba(246,65,95,0.45)] bg-white hover:scale-105 transition"

    >

      <img src={AssistantAvatar} alt="Support bot" className="w-full h-full p-2 object-contain" />

      {hasUnread && (

        <span className="absolute -top-0.5 -right-0.5 inline-flex h-4 w-4 rounded-full bg-red-500 border-2 border-white" />

      )}

    </button>

  );



  return (

    <div className="fixed bottom-6 right-6 z-50 flex flex-col items-end gap-3">

      {isOpen && (

        <div

          className="w-[360px] max-w-[92vw] bg-white rounded-[32px] shadow-[0_20px_45px_rgba(15,23,42,0.25)] border border-white/80 flex flex-col overflow-hidden"

          style={{ height: "min(640px, 90vh)" }}

        >

          <header className="bg-gradient-to-r from-[#A52A2A] to-[#ffbe46] text-white px-5 py-4">

            <div className="flex items-center justify-between gap-4">

              <div className="flex items-center gap-3">

                <div className="w-12 h-12 rounded-2xl bg-white/90 p-1.5 shadow-inner">

                  <img src={AssistantAvatar} alt="English coach" className="w-full h-full object-contain" />

                </div>

                <div>

                  <p className="text-[11px] uppercase tracking-[0.3em] text-white/80">Trợ lí tiếng Anh</p>

                  <p className="text-lg font-semibold">Coach Luna</p>

                  <p className="text-[12px] flex items-center gap-1 text-white/80">

                    <span className="inline-flex h-2 w-2 rounded-full bg-emerald-300" />

                    Online : {headerSubtitle}

                  </p>

                </div>

              </div>

              <div className="flex flex-col items-end gap-2 text-xs">

                <button

                  type="button"

                  className="bg-white/15 hover:bg-white/25 px-3 py-1 rounded-full"

                  onClick={() => setShowHistory((prev) => !prev)}

                >

                  {showHistory ? "Đóng lịch sử" : "Lịch sử"}

                </button>

                <button

                  type="button"

                  className="text-white/70 hover:text-white"

                  aria-label="Đóng chat"

                  onClick={closeChat}

                >

                  X

                </button>

              </div>

            </div>

          </header>



          <div className="relative flex-1 min-h-0 bg-[#F8F9FF] flex flex-col overflow-hidden">

            {showHistory && (

              <div className="absolute inset-0 bg-white/95 backdrop-blur-sm z-20 flex flex-col min-h-0 border-b border-stone-200 overflow-hidden">

                <HistoryPanel

                  threads={threads}

                  loading={loadingThreads}

                  activeThread={activeThread}

                  onClose={() => setShowHistory(false)}

                  onSelect={handleSelectThread}

                  onStartNew={handleStartNew}

                />

              </div>

            )}



            {!isAuthenticated ? (

              <div className="flex-1 overflow-hidden">

                <LoginPromptBubble

                  onLogin={() => {

                    closeChat();

                    navigate("/login");

                  }}

                  onRegister={() => {

                    closeChat();

                    navigate("/register");

                  }}

                />

              </div>

            ) : activeThread ? (

              <ConversationView

                thread={activeThread}

                messages={messages}

                loadingMessages={loadingMessages}

                messageText={messageText}

                setMessageText={setMessageText}

                onSend={handleSendMessage}

                attachments={messageAttachments}

                setAttachments={setMessageAttachments}

                onUpload={(file) => handleUpload(file, setMessageAttachments)}

                uploading={uploading}

                messagesEndRef={messagesEndRef}

                sending={sending}

                onRate={handleRatingSubmit}

                rating={rating}

                setRating={setRating}

                ratingComment={ratingComment}

                setRatingComment={setRatingComment}

              />

            ) : (

              <NewThreadView

                topic={topic}

                setTopic={setTopic}

                description={description}

                setDescription={setDescription}

                attachments={newAttachments}

                setAttachments={setNewAttachments}

                onUpload={(file) => handleUpload(file, setNewAttachments)}

                uploading={uploading}

                onSubmit={handleCreateThread}

                creating={creating}

                suggestions={ENGLISH_SUGGESTIONS}

                onSuggestion={handleSuggestion}

              />

            )}

          </div>

        </div>

      )}

      {floatingButton}

    </div>

  );

}



function HistoryPanel({ threads, loading, activeThread, onSelect, onClose, onStartNew }) {

  return (

    <div className="flex flex-col h-full min-h-0">

      <div className="px-5 py-3 flex items-center justify-between">

        <div>

          <p className="text-sm font-semibold text-stone-800">Lịch sử hỗ trợ</p>

          <p className="text-xs text-stone-500">{threads.length} cuộc trò chuyện</p>

        </div>

        <button

          type="button"

          className="text-xs text-primary-600 hover:underline"

          onClick={() => {

            onStartNew();

            onClose();

          }}

        >

          + Yêu cầu mới

        </button>

      </div>

      <div className="flex-1 overflow-y-auto divide-y px-1">

        {loading && <p className="text-xs text-center py-4 text-stone-500">Đang tải...</p>}

        {!loading && threads.length === 0 && (

          <p className="text-xs text-center py-6 text-stone-400">Bạn chưa có cuộc trò chuyện nào.</p>

        )}

        {!loading &&

          threads.map((thread) => (

            <button

              type="button"

              key={thread.id}

              onClick={() => onSelect(thread.id)}

              className={`w-full text-left px-5 py-3 flex flex-col gap-1 hover:bg-stone-50 ${

                activeThread?.id === thread.id ? "bg-rose-50" : ""

              }`}

            >

              <div className="flex items-center justify-between text-sm">

                <p className="font-semibold text-stone-800 truncate">{thread.topic}</p>

                <span

                  className={`text-[11px] px-2 py-0.5 rounded-full ${STATUS_BADGE[thread.status] || "bg-stone-200 text-stone-600"}`}

                >

                  {renderStatus(thread.status)}

                </span>

              </div>

              <p className="text-xs text-stone-500 truncate">

                {thread.lastMessagePreview || "Chưa có tin nhắn"}

              </p>

            </button>

          ))}

      </div>

      <button type="button" className="py-3 text-sm text-primary-600 border-t" onClick={onClose}>

        Đóng

      </button>

    </div>

  );

}



function NewThreadView({

  topic,

  setTopic,

  description,

  setDescription,

  attachments,

  setAttachments,

  onUpload,

  uploading,

  onSubmit,

  creating,

  suggestions,

  onSuggestion,

}) {

  const fileInputRef = useRef(null);



  return (

    <form className="flex-1 min-h-0 flex flex-col" onSubmit={onSubmit}>

      <div className="flex-1 min-h-0 overflow-y-auto px-5 py-4 space-y-4 text-sm">

        <BotIntroBubble />

        <p className="text-xs uppercase tracking-wide text-stone-400">Chọn nhanh chủ đề tiếng Anh</p>

        <div className="grid grid-cols-2 gap-2">

          {suggestions.map((suggestion) => (

            <SuggestionButton key={suggestion.id} suggestion={suggestion} onClick={onSuggestion} />

          ))}

        </div>

        <label className="text-xs font-semibold text-stone-500 uppercase">Mô tả nhu cầu</label>

        <textarea

          className="w-full border border-stone-200 rounded-xl px-3 py-2 min-h-[110px] bg-white resize-none focus:outline-none focus:ring-2 focus:ring-primary-200"

          placeholder="Mô tả nhu cầu, mục tiêu hoặc vấn đề bạn đang gặp..."

          value={description}

          onChange={(e) => setDescription(e.target.value)}

        />



        <AttachmentPreview items={attachments} onRemove={(idx) => setAttachments((prev) => prev.filter((_, i) => i !== idx))} />

      </div>



      <div className="px-5 py-4 bg-white border-t flex items-center gap-3 text-sm">

        <button

          type="button"

          className="text-primary-600 font-semibold"

          onClick={() => fileInputRef.current?.click()}

          disabled={uploading}

        >

          {uploading ? "Đang tải..." : "Đính kèm"}

        </button>

        <input

          ref={fileInputRef}

          type="file"

          className="hidden"

          accept="image/*"

          onChange={(e) => onUpload(e.target.files?.[0])}

        />

        <button

          type="submit"

          className="ml-auto bg-gradient-to-r from-[#F6415F] to-[#FF8846] text-white rounded-full px-5 py-2 font-semibold disabled:opacity-40"

          disabled={creating || !description.trim()}

        >

          {creating ? "Đang gửi..." : "Bắt đầu trò chuyện"}

        </button>

      </div>

    </form>

  );

}



function ConversationView({

  thread,

  messages,

  loadingMessages,

  messageText,

  setMessageText,

  onSend,

  sending,

  attachments,

  setAttachments,

  onUpload,

  uploading,

  messagesEndRef,

  onRate,

  rating,

  setRating,

  ratingComment,

  setRatingComment,

}) {

  const fileInputRef = useRef(null);

  const isClosed = thread.status === "CLOSED";



  return (

    <div className="flex-1 flex flex-col min-h-0">

      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-3 min-h-0">

        {loadingMessages && <p className="text-center text-xs text-stone-500">Đang tải hội thoại...</p>}

        {!loadingMessages &&

          messages.map((message) => {
   const isSelf =
     message.senderType?.toLowerCase() === "student" ||
     message.senderRole?.toLowerCase() === "student";

   return (
     <MessageBubble
       key={message.id}
       message={message}
       isSelf={isSelf}
     />
   );
 })}


        <div ref={messagesEndRef} />

      </div>

      {isClosed ? (

        <div className="border-t bg-white px-4 py-6 text-sm text-center text-stone-500 space-y-2">

          <p className="font-semibold text-stone-700">Cảm ơn bạn đã liên hệ!</p>

          <p>Chúng tôi ghi nhận phản hội và hỗ trợ trong thời gian sớm nhất.</p>

        </div>

      ) : (

        <form className="border-t bg-white px-4 py-3 space-y-3" onSubmit={onSend}>

          <textarea

            className="w-full border border-stone-200 rounded-2xl px-4 py-3 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-primary-200"

            rows={2}

            placeholder="Nhập nội dung cần hỗ trợ..."

            value={messageText}

            onChange={(e) => setMessageText(e.target.value)}

          />

          <AttachmentPreview items={attachments} onRemove={(idx) => setAttachments((prev) => prev.filter((_, i) => i !== idx))} />



          <div className="flex items-center justify-between text-xs text-stone-500">

            <button

              type="button"

              onClick={() => fileInputRef.current?.click()}

              className="text-primary-600 font-semibold"

              disabled={uploading}

            >

              {uploading ? "Đang tải..." : "Đính kèm"}

            </button>

            <input

              ref={fileInputRef}

              type="file"

              className="hidden"

              accept="image/*"

              onChange={(e) => onUpload(e.target.files?.[0])}

            />

            <button

              type="submit"

              disabled={sending || !messageText.trim()}

              className="bg-primary-600 text-white px-6 py-2 rounded-full font-semibold disabled:opacity-50"

            >

              {sending ? "Đang gửi..." : "Gửi"}

            </button>

          </div>

        </form>

      )}

    </div>

  );

}



function AttachmentPreview({ items, onRemove }) {

  if (!items?.length) return null;

  return (

    <div className="flex flex-wrap gap-2 text-xs">

      {items.map((file, idx) => (

        <div

          key={`${file.url}-${idx}`}

          className="flex items-center gap-2 bg-stone-100 border border-stone-200 rounded-full pl-3 pr-1 py-1"

        >

          <span className="max-w-[160px] truncate">{file.name || "Ảnh đính kèm"}</span>

          <button

            type="button"

            onClick={() => onRemove(idx)}

            className="w-5 h-5 rounded-full bg-stone-300 text-[11px] text-white"

          >

            ?

          </button>

        </div>

      ))}

    </div>

  );

}



function isImageUrl(url) {
  if (!url) return false;
  const normalized = url.split(/[?#]/)[0] || "";
  return /\.(png|jpe?g|gif|bmp|webp|svg)$/i.test(normalized);
}



function MessageBubble({ message, isSelf }) {
  const bubbleClass = isSelf
    ? "bg-[#8B5E34] text-white rounded-[24px] rounded-br-sm shadow-md"
    : message.senderType === "system"
      ? "bg-white border border-dashed border-stone-200 text-stone-600 rounded-[24px] text-xs text-center"
      : "bg-white border border-stone-200 text-stone-800 rounded-[24px] rounded-bl-sm";
  return (
    <div className={`flex flex-col ${isSelf ? "items-end" : "items-start"} text-sm`}>
      <div className={`px-4 py-3 max-w-[85%] shadow-sm ${bubbleClass}`}>
        <div className="space-y-3 max-h-[240px] overflow-y-auto pr-1">
          <p className="whitespace-pre-line leading-relaxed break-words">{message.content}</p>
          {message.attachments?.map((url) =>
            isImageUrl(url) ? (
              <a key={url} href={url} target="_blank" rel="noreferrer" className="block">
                <img
                  src={url}
                  alt="Attachment"
                  className="max-w-[200px] rounded-xl border border-white/40 shadow"
                />
              </a>
            ) : (
              <a
                key={url}
                href={url}
                target="_blank"
                rel="noreferrer"
                className={`block text-xs underline ${isSelf ? "text-white/90" : "text-primary-600"}`}
              >
                Tải tệp
              </a>
            ),
          )}
        </div>
      </div>
      <span className="text-[10px] text-stone-400 mt-1">
        {new Date(message.createdAt).toLocaleString("vi-VN")}
      </span>
    </div>
  );
}

function SuggestionButton({ suggestion, onClick }) {

  return (

    <button

      type="button"

      onClick={() => onClick(suggestion)}

      className="bg-white border border-stone-200 rounded-2xl px-3 py-3 text-left shadow-sm hover:border-primary-200 hover:-translate-y-0.5 transition min-h-[92px]"

    >

      <p className="text-sm font-semibold text-stone-800">{suggestion.title}</p>

      <p className="text-xs text-stone-500 mt-1 leading-snug line-clamp-2">{suggestion.message}</p>

    </button>

  );

}



function BotIntroBubble() {

  return (

    <div className="bg-white rounded-3xl p-4 shadow-sm border border-white">

      <p className="text-sm text-stone-700">

        Chào mừng bạn đến với trợ lí tiếng anh. Hãy chọn các chủ đề phù hợp hoặc chia sẻ mong muốn của bạn để mình vấn

        lộ trình học tập, cách học hay hỗ trợ kĩ thuật nhé!

      </p>

    </div>

  );

}



function LoginPromptBubble({ onLogin, onRegister }) {

  return (

    <div className="space-y-4 text-sm text-stone-600 overflow-y-auto px-5 py-6">

      <div className="flex items-start gap-3">

        <div className="w-10 h-10 rounded-full bg-white shadow flex items-center justify-center">

          <img src={AssistantAvatar} alt="assistant" className="w-7 h-7" />

        </div>

        <div className="bg-white border border-stone-200 rounded-[26px] px-4 py-3 shadow-sm flex-1">

          <p className="text-stone-700">Anh chị vui lòng đăng nhập để thực hiện chức năng này</p>

          <div className="flex flex-wrap gap-2 mt-3">

            <button

              type="button"

              className="rounded-full border border-stone-300 px-4 py-1.5 text-sm font-semibold text-stone-700 hover:bg-stone-50"

              onClick={onLogin}

            >

              Đăng nhập

            </button>

            <button

              type="button"

              className="rounded-full border border-primary-500 px-4 py-1.5 text-sm font-semibold text-primary-600 hover:bg-primary-50"

              onClick={onRegister}

            >

              Đăng kí

            </button>

          </div>

        </div>

      </div>

    </div>

  );

}



function renderStatus(status) {

  switch (status) {

    case "NEW":

      return "Mới";

    case "IN_PROGRESS":

      return "Đang xử lý";

    case "WAITING_STUDENT":

      return "Chờ học viên";

    case "CLOSED":

      return "Đã đóng";

    default:

      return status;

  }

}








