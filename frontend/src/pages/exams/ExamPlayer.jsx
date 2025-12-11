import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import httpClient from "../../api/httpClient";

function formatDuration(sec) {
  if (!sec && sec !== 0) return "--:--";
  const safe = Math.max(0, sec);
  const minutes = Math.floor(safe / 60)
    .toString()
    .padStart(2, "0");
  const seconds = Math.floor(safe % 60)
    .toString()
    .padStart(2, "0");
  return `${minutes}:${seconds}`;
}

function ExamMeta({ overview, lastResult }) {
  if (!overview) return null;
  const attemptsText =
    overview.attemptsUsed != null && overview.maxAttempts != null
      ? `${overview.attemptsUsed}/${overview.maxAttempts}`
      : "--";

  return (
    <div className="space-y-3">
      <h1 className="text-2xl font-semibold text-stone-900">{overview.title || "Bài kiểm tra"}</h1>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 text-sm">
        <div className="p-3 rounded-xl border border-stone-200 bg-white">
          <p className="text-stone-500">Thời lượng</p>
          <p className="text-stone-900 text-lg font-semibold">
            {Math.floor((overview.timeLimitSec || 0) / 60)} phút
          </p>
        </div>
        <div className="p-3 rounded-xl border border-stone-200 bg-white">
          <p className="text-stone-500">Số câu hỏi</p>
          <p className="text-stone-900 text-lg font-semibold">{overview.questionCount ?? "--"}</p>
        </div>
        <div className="p-3 rounded-xl border border-stone-200 bg-white">
          <p className="text-stone-500">Lượt làm</p>
          <p className="text-stone-900 text-lg font-semibold">{attemptsText}</p>
        </div>
        <div className="p-3 rounded-xl border border-stone-200 bg-white">
          <p className="text-stone-500">Điểm đạt</p>
          <p className="text-stone-900 text-lg font-semibold">{overview.passingScore ?? 0}%</p>
        </div>
      </div>
      {lastResult && (
        <div
          className={`p-4 rounded-2xl text-sm ${
            lastResult.passed
              ? "border-emerald-200 bg-emerald-50 text-emerald-700"
              : "border-amber-200 bg-amber-50 text-amber-700"
          }`}
        >
          <p>
            Lần làm gần nhất: <strong>{Math.round(lastResult.scorePercent ?? 0)}%</strong>{" "}
            {lastResult.passed ? "(Đạt)" : "(Chưa đạt)"}
          </p>
          {typeof lastResult.attemptsRemaining === "number" && (
            <p className="mt-1">Lượt còn lại: {lastResult.attemptsRemaining}</p>
          )}
          {lastResult.message && <p className="mt-1">{lastResult.message}</p>}
          {lastResult.locked && (
            <p className="mt-1 text-red-600">Khóa học đã bị khóa. Vui lòng đăng ký lại.</p>
          )}
        </div>
      )}
      {overview.blockers?.length > 0 && (
        <div className="p-4 rounded-xl border border-red-200 bg-red-50 text-sm text-red-600">
          {overview.blockers.map((b) => (
            <p key={b}>• {b}</p>
          ))}
        </div>
      )}
    </div>
  );
}

function QuestionNav({ questions, currentId, onSelect }) {
  if (!questions?.length) return null;
  return (
    <div className="grid grid-cols-8 gap-2 text-xs">
      {questions.map((q, idx) => {
        const answered = Boolean(q.selectedOptionId);
        const marked = Boolean(q.markedForReview);
        const active = q.id === currentId;
        return (
          <button
            key={q.id}
            onClick={() => onSelect(q.id)}
            className={`h-10 rounded-md border text-center ${
              active ? "border-primary-500 text-primary-700" : "border-stone-200 text-stone-600"
            } ${answered ? "bg-primary-50" : "bg-white"}`}
          >
            {idx + 1}
            {marked && <span className="block text-[10px] text-amber-600">Review</span>}
          </button>
        );
      })}
    </div>
  );
}

function QuestionCard({ question, onSelectOption, onToggleFlag }) {
  if (!question) return null;
  return (
    <div className="space-y-3">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm text-stone-500">Câu hỏi</p>
          <div
            className="text-lg text-stone-900 font-medium"
            dangerouslySetInnerHTML={{ __html: question.text || "" }}
          />
        </div>
        <button
          onClick={() => onToggleFlag(question)}
          className={`text-xs px-2 py-1 rounded-full border ${
            question.markedForReview ? "border-amber-400 text-amber-600" : "border-stone-200 text-stone-500"
          }`}
        >
          {question.markedForReview ? "Bỏ đánh dấu" : "Đánh dấu"}
        </button>
      </div>
      <div className="space-y-2">
        {question.options?.map((opt) => {
          const selected = question.selectedOptionId === opt.id;
          return (
            <button
              key={opt.id}
              onClick={() => onSelectOption(question, opt.id)}
              className={`w-full text-left px-4 py-3 rounded-xl border ${
                selected ? "border-primary-500 bg-primary-50 text-primary-700" : "border-stone-200 bg-white"
              }`}
            >
              {opt.text}
            </button>
          );
        })}
      </div>
    </div>
  );
}

function ExamInner() {
  const { courseId, examId } = useParams();
  const navigate = useNavigate();
  const [overview, setOverview] = useState(null);
  const [attempt, setAttempt] = useState(null);
  const [currentQuestionId, setCurrentQuestionId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [countdown, setCountdown] = useState(0);
  const [result, setResult] = useState(null);
  const [reviewAttempt, setReviewAttempt] = useState(null);
  const [autoSubmitNotice, setAutoSubmitNotice] = useState(false);
  const [autoSubmitTriggered, setAutoSubmitTriggered] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const fetchOverview = useCallback(async () => {
    if (!courseId || !examId) return;
    setLoading(true);
    setError("");
    try {
      const { data } = await httpClient.get(`/api/student/exams/courses/${courseId}/exams/${examId}`);
      setOverview(data);
      if (data.activeAttemptId) {
        const res = await httpClient.get(`/api/student/exams/attempts/${data.activeAttemptId}`);
        setAttempt(res.data);
        setCurrentQuestionId(res.data.questions?.[0]?.id || null);
        setCountdown(res.data.countdownSec || 0);
        setReviewAttempt(null);
        setAutoSubmitNotice(false);
        setAutoSubmitTriggered(false);
      } else {
        setAttempt(null);
      }
    } catch (err) {
      setError(err?.response?.data?.message || "Không thể tải thông tin bài kiểm tra");
    } finally {
      setLoading(false);
    }
  }, [courseId, examId]);

  useEffect(() => {
    fetchOverview();
  }, [fetchOverview]);

  const attemptId = attempt?.attemptId;
  const attemptCountdown = attempt?.countdownSec;
  const attemptTimeLimit = attempt?.timeLimitSec;

  useEffect(() => {
    if (!attemptId) return undefined;
    setCountdown(attemptCountdown || 0);
    const interval = setInterval(() => {
      setCountdown((prev) => Math.max(0, prev - 1));
    }, 1000);
    return () => clearInterval(interval);
  }, [attemptId, attemptCountdown]);

  const startAttempt = async () => {
    if (!overview) return;
    try {
      setLoading(true);
      const body = overview.activeAttemptId ? { resumeAttemptId: overview.activeAttemptId } : {};
      const { data } = await httpClient.post(
        `/api/student/exams/courses/${courseId}/exams/${examId}/attempts`,
        body,
      );
      setAttempt(data);
      setCurrentQuestionId(data.questions?.[0]?.id || null);
      setCountdown(data.countdownSec || 0);
      setReviewAttempt(null);
      setAutoSubmitNotice(false);
      setAutoSubmitTriggered(false);
      setResult(null);
    } catch (err) {
      setError(err?.response?.data?.message || "Không thể bắt đầu bài kiểm tra");
    } finally {
      setLoading(false);
    }
  };

  const currentQuestion = useMemo(
    () => attempt?.questions?.find((q) => q.id === currentQuestionId) || null,
    [attempt, currentQuestionId],
  );
  const currentIndex = useMemo(() => {
    if (!attempt?.questions?.length || !currentQuestionId) return 0;
    const idx = attempt.questions.findIndex((q) => q.id === currentQuestionId);
    return idx >= 0 ? idx : 0;
  }, [attempt, currentQuestionId]);

  const updateQuestionState = (questionId, updater) => {
    setAttempt((prev) => {
      if (!prev) return prev;
      const updated = prev.questions?.map((q) => {
        if (q.id !== questionId) return q;
        return { ...q, ...updater(q) };
      });
      return { ...prev, questions: updated };
    });
  };

  const saveAnswer = async (payload) => {
    setSaving(true);
    try {
      await httpClient.patch(`/api/student/exams/attempts/${attempt.attemptId}/answers`, payload);
    } catch (err) {
      console.error(err);
    } finally {
      setSaving(false);
    }
  };

  const handleSelectOption = (question, optionId) => {
    updateQuestionState(question.id, () => ({ selectedOptionId: optionId }));
    saveAnswer({
      questionId: question.id,
      selectedOptionId: optionId,
      markedForReview: question.markedForReview,
      lastSeenQuestionId: question.id,
    });
  };

  const handleToggleFlag = (question) => {
    const next = !question.markedForReview;
    updateQuestionState(question.id, () => ({ markedForReview: next }));
    saveAnswer({
      questionId: question.id,
      selectedOptionId: question.selectedOptionId,
      markedForReview: next,
      lastSeenQuestionId: question.id,
    });
  };

  const issueCertificate = async () => {
    try {
      await httpClient.post(`/api/student/exams/courses/${courseId}/certificate`);
    } catch (err) {
      console.warn("Issue certificate failed", err);
    }
  };

  const submitAttempt = useCallback(
    async (autoTriggered = false) => {
      if (!attempt?.attemptId || submitting) return;
      try {
        setSubmitting(true);
        const attemptId = attempt.attemptId;
        const { data } = await httpClient.post(`/api/student/exams/attempts/${attemptId}/submit`);
        setResult(data);
        setAutoSubmitNotice(autoTriggered);
        try {
          const detail = await httpClient.get(`/api/student/exams/attempts/${attemptId}`);
          setReviewAttempt(detail.data);
        } catch (detailError) {
          console.error("Failed to load attempt review", detailError);
          setReviewAttempt(null);
        }
        if (data.passed) {
          issueCertificate();
        }
        setAttempt(null);
        setCountdown(0);
        await fetchOverview();
      } catch (err) {
        setError(err?.response?.data?.message || "KhA'ng th??? n??Tp bA?i");
      } finally {
        setSubmitting(false);
      }
    },
    [attempt, fetchOverview, submitting],
  );

  useEffect(() => {
    if (!attemptId || !attemptTimeLimit) return;
    if (countdown > 0 || submitting || autoSubmitTriggered) return;
    setAutoSubmitTriggered(true);
    submitAttempt(true);
  }, [attemptId, attemptTimeLimit, countdown, submitting, autoSubmitTriggered, submitAttempt]);


  if (loading && !overview && !attempt) {
    return <div className="py-12 text-center text-stone-500">Đang tải dữ liệu...</div>;
  }

  if (!attempt) {
    return (
      <div className="space-y-6">
        <ExamMeta overview={overview} lastResult={result} />
        {reviewAttempt && (
          <ExamReviewPanel
            attempt={reviewAttempt}
            result={result}
            timedOut={autoSubmitNotice}
            onClose={() => {
              setReviewAttempt(null);
              setAutoSubmitNotice(false);
            }}
          />
        )}
        {error && <p className="text-sm text-red-600">{error}</p>}
        <div className="flex gap-3">
          <button onClick={startAttempt} disabled={!overview?.canAttempt} className="btn btn-primary">
            {overview?.activeAttemptId ? "Tiếp tục bài làm" : "Bắt đầu làm bài"}
          </button>
          <button onClick={() => navigate(-1)} className="btn border-stone-300">
            Quay lại
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap gap-4 items-center justify-between border border-stone-200 rounded-2xl p-4 bg-white">
        <div>
          <p className="text-sm text-stone-500">Đang làm</p>
          <p className="text-lg font-semibold text-stone-900">{attempt.examTitle}</p>
        </div>
        <div className="flex gap-4 items-center">
          <div className="text-center">
            <p className="text-xs text-stone-500">Thời gian còn lại</p>
            <p className="text-2xl font-mono text-primary-600">{formatDuration(countdown)}</p>
          </div>
          <button
            onClick={() => submitAttempt(false)}
            className="btn btn-primary"
            disabled={saving || loading || submitting}
          >
            {submitting ? "Dang nop..." : "Nop bai"}
          </button>
        </div>
      </div>
      {error && <p className="text-sm text-red-600">{error}</p>}

      {result && (
        <div
          className={`p-4 rounded-2xl text-sm ${
            result.passed
              ? "border-emerald-200 bg-emerald-50 text-emerald-800"
              : "border-amber-200 bg-amber-50 text-amber-800"
          }`}
        >
          <p>
            Điểm: <strong>{Math.round(result.scorePercent ?? 0)}%</strong>{" "}
            {result.passed ? "(Đạt)" : "(Chưa đạt)"}
          </p>
          {typeof result.attemptsRemaining === "number" && (
            <p className="mt-1">Lượt còn lại: {result.attemptsRemaining}</p>
          )}
          {result.message && <p className="mt-1">{result.message}</p>}
          {result.locked && <p className="mt-1 text-red-600">Khóa học đã bị khóa. Vui lòng đăng ký lại.</p>}
        </div>
      )}

      <div className="grid lg:grid-cols-[2fr_1fr] gap-6">
        <div className="space-y-4">
          <QuestionCard question={currentQuestion} onSelectOption={handleSelectOption} onToggleFlag={handleToggleFlag} />
          <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-stone-600">
            <span>
              Câu {currentIndex + 1}/{attempt?.questions?.length ?? 1}
            </span>
            <div className="flex gap-2">
              <button
                type="button"
                className="btn btn-xs border-stone-300"
                onClick={() => {
                  if (!attempt?.questions?.length) return;
                  const prevIdx = Math.max(0, currentIndex - 1);
                  setCurrentQuestionId(attempt.questions[prevIdx].id);
                }}
                disabled={!attempt?.questions?.length || currentIndex <= 0}
              >
                Câu trước
              </button>
              <button
                type="button"
                className="btn btn-xs border-stone-300"
                onClick={() => {
                  if (!attempt?.questions?.length) return;
                  const nextIdx = Math.min(attempt.questions.length - 1, currentIndex + 1);
                  setCurrentQuestionId(attempt.questions[nextIdx].id);
                }}
                disabled={
                  !attempt?.questions?.length || currentIndex >= (attempt?.questions?.length ?? 1) - 1
                }
              >
                Câu tiếp
              </button>
            </div>
          </div>
          {saving && <p className="text-xs text-stone-400">Đang lưu...</p>}
        </div>
        <div className="space-y-4">
          <div className="p-4 rounded-2xl border border-stone-200 bg-white">
            <p className="text-sm font-medium text-stone-700 mb-3">Danh sách câu hỏi</p>
            <QuestionNav
              questions={attempt.questions}
              currentId={currentQuestionId}
              onSelect={(id) => setCurrentQuestionId(id)}
            />
          </div>
        </div>
      </div>
    </div>
  );
}

export default function ExamPlayer() {
  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      <ExamInner />
    </div>
  );
}

function ExamReviewPanel({ attempt, result, timedOut, onClose }) {
  if (!attempt) return null;
  const questions = attempt.questions || [];
  const total = questions.length || 0;
  const correctCount = questions.reduce((sum, q) => {
    const correct =
      q?.answeredCorrectly ?? (q.correctOptionId && q.correctOptionId === q.selectedOptionId);
    return sum + (correct ? 1 : 0);
  }, 0);
  const wrongCount = Math.max(0, total - correctCount);
  const scorePercent = Math.round(result?.scorePercent ?? attempt.score ?? 0);
  const canReview = attempt.reviewEnabled && questions.length > 0;

  return (
    <section className="rounded-[32px] border border-amber-100 bg-white px-6 py-6 space-y-5 shadow-sm">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.4em] text-amber-600">Ket qua bai lam</p>
          <h2 className="text-xl font-semibold text-stone-800">{attempt.examTitle || "Bai kiem tra"}</h2>
        </div>
        <button type="button" className="text-sm text-primary-600 hover:underline" onClick={onClose}>
          Dong
        </button>
      </div>
      <div
        className={`rounded-2xl border px-4 py-3 text-sm ${
          timedOut ? "border-amber-200 bg-amber-50 text-amber-800" : "border-emerald-200 bg-emerald-50 text-emerald-800"
        }`}
      >
        {timedOut ? "Het gio! He thong da nop bai va cham diem tu dong." : "Bai lam da duoc nop va cham diem."}
      </div>
      <div className="grid gap-4 sm:grid-cols-3 text-sm">
        <div className="rounded-2xl bg-stone-50 border border-stone-200 p-4">
          <p className="text-xs text-stone-500 uppercase tracking-[0.2em]">Diem</p>
          <p className="text-2xl font-semibold text-stone-900">{scorePercent}%</p>
        </div>
        <div className="rounded-2xl bg-emerald-50 border border-emerald-100 p-4">
          <p className="text-xs text-emerald-700 uppercase tracking-[0.2em]">So cau dung</p>
          <p className="text-2xl font-semibold text-emerald-900">
            {correctCount}/{total}
          </p>
        </div>
        <div className="rounded-2xl bg-rose-50 border border-rose-100 p-4">
          <p className="text-xs text-rose-700 uppercase tracking-[0.2em]">Sai / bo trong</p>
          <p className="text-2xl font-semibold text-rose-900">{wrongCount}</p>
        </div>
      </div>
      {!canReview ? (
        <p className="text-sm text-stone-500">Bai thi nay khong cho phep xem lai dap an.</p>
      ) : (
        <div className="space-y-4 max-h-[480px] overflow-y-auto pr-1">
          {questions.map((question, idx) => {
            const selected = question.selectedOptionId;
            const correctId = question.correctOptionId;
            const answeredCorrectly = question?.answeredCorrectly ?? (correctId && selected && correctId === selected);
            return (
              <div key={question.id} className="border border-stone-200 rounded-2xl p-4 bg-white/80">
                <div className="flex items-start justify-between gap-2">
                  <p className="text-sm font-semibold text-stone-800">
                    Cau {idx + 1} • {question.points || 1} diem
                  </p>
                  <span
                    className={`text-xs px-2 py-1 rounded-full ${
                      answeredCorrectly ? "bg-emerald-100 text-emerald-700" : "bg-rose-100 text-rose-700"
                    }`}
                  >
                    {answeredCorrectly ? "Dung" : "Sai"}
                  </span>
                </div>
                <div
                  className="mt-2 text-sm text-stone-700 leading-relaxed"
                  dangerouslySetInnerHTML={{ __html: question.text || "" }}
                />
                <div className="mt-3 space-y-2">
                  {question.options?.map((opt) => {
                    const isCorrect = Boolean(opt.correct);
                    const isSelected = selected === opt.id;
                    let optionClass = "border-stone-200 bg-white text-stone-700";
                    if (isCorrect) {
                      optionClass = "border-emerald-500 bg-emerald-50 text-emerald-800";
                    } else if (isSelected && !isCorrect) {
                      optionClass = "border-rose-400 bg-rose-50 text-rose-700";
                    } else if (isSelected) {
                      optionClass = "border-stone-400 bg-stone-50";
                    }
                    return (
                      <div key={opt.id} className={`rounded-xl border px-4 py-2 text-sm ${optionClass}`}>
                        <div dangerouslySetInnerHTML={{ __html: opt.text || "" }} />
                        <div className="text-[11px] mt-1 text-stone-500">
                          {isCorrect && "Dap an dung"}
                          {isSelected && !isCorrect && "Ban da chon"}
                        </div>
                      </div>
                    );
                  })}
                </div>
                {!selected && <p className="text-xs text-stone-500 mt-2">Ban chua chon dap an cho cau nay.</p>}
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}
