package vn.edu.ueh.melobi_nhom5.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import vn.edu.ueh.melobi_nhom5.database.ContentLocalDao;
import vn.edu.ueh.melobi_nhom5.entity.Message;
import vn.edu.ueh.melobi_nhom5.model.Answer;
import vn.edu.ueh.melobi_nhom5.model.Question;
import vn.edu.ueh.melobi_nhom5.service.ChatService;
import vn.edu.ueh.melobi_nhom5.service.QuestionService;

/**
 * Đồng bộ nội dung đã xem xuống SQLite để dùng khi mất mạng.
 */
public class SyncManager {
    private static final String TAG = "SyncManager";
    private static SyncManager instance;
    private final ContentLocalDao contentLocalDao;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean syncing;

    private SyncManager(Context context) {
        this.contentLocalDao = new ContentLocalDao(context.getApplicationContext());
    }

    public static synchronized SyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new SyncManager(context);
        }
        return instance;
    }

    public void cacheQuestions(List<Question> questions) {
        contentLocalDao.saveQuestions(questions);
    }

    public void cacheQuestionDetail(Question question, List<Answer> answers) {
        contentLocalDao.saveQuestionDetail(question, answers);
    }

    public boolean isQuestionCachedLastDay(String questionId) {
        return contentLocalDao.isViewedLastDay(questionId);
    }

    public Question getCachedQuestion(String questionId) {
        Question question = contentLocalDao.getQuestionById(questionId);
        if (question == null || !contentLocalDao.isViewedLastDay(questionId)) {
            return null;
        }
        return question;
    }

    public List<Question> getViewedQuestionsLastDay() {
        contentLocalDao.purgeExpired();
        LinkedHashMap<String, Question> unique = new LinkedHashMap<>();
        for (Question question : contentLocalDao.getQuestionsViewedLastDay()) {
            if (question.getQuestionId() != null) {
                unique.put(question.getQuestionId(), question);
            }
        }
        for (Question question : contentLocalDao.getPendingQuestions()) {
            if (question.getQuestionId() != null) {
                unique.put(question.getQuestionId(), question);
            }
        }
        return new ArrayList<>(unique.values());
    }

    public List<Answer> getCachedAnswers(String questionId) {
        return contentLocalDao.getAnswersByQuestionId(questionId);
    }

    public Question queueQuestion(Question question) {
        if (question == null) {
            return null;
        }
        if (question.getQuestionId() == null || question.getQuestionId().trim().isEmpty()) {
            question.setQuestionId("local_" + UUID.randomUUID());
        }
        long now = System.currentTimeMillis();
        if (question.getCreatedAt() <= 0) {
            question.setCreatedAt(now);
        }
        question.setUpdatedAt(now);
        if (question.getStatus() == null || question.getStatus().trim().isEmpty()) {
            question.setStatus(Question.STATUS_ACTIVE);
        }
        contentLocalDao.savePendingQuestion(question);
        return question;
    }

    public Message queueMessage(String conversationId,
                                String otherUserId,
                                String senderId,
                                String content) {
        String messageId = "local_" + UUID.randomUUID();
        long now = System.currentTimeMillis();
        contentLocalDao.savePendingMessage(
                messageId,
                conversationId,
                otherUserId,
                senderId,
                content,
                now
        );
        Message message = new Message(messageId, conversationId, senderId, content, new java.util.Date(now), false);
        message.setPending(true);
        return message;
    }

    public List<Message> getPendingMessages(String conversationId) {
        List<Message> messages = new ArrayList<>();
        for (ContentLocalDao.PendingMessage pending : contentLocalDao.getPendingMessages(conversationId)) {
            messages.add(pending.toMessage());
        }
        return messages;
    }

    public synchronized void syncAll() {
        if (syncing || !NetworkUtils.isOnline()) {
            return;
        }
        syncing = true;
        contentLocalDao.purgeExpired();
        syncPendingQuestions(() -> syncPendingMessages(() -> {
            syncing = false;
            Log.d(TAG, "Da dong bo hang doi offline");
        }));
    }

    private void syncPendingQuestions(Runnable after) {
        List<Question> pending = contentLocalDao.getPendingQuestions();
        if (pending.isEmpty()) {
            after.run();
            return;
        }
        QuestionService questionService = new QuestionService();
        syncNextQuestion(questionService, pending, 0, after);
    }

    private void syncNextQuestion(QuestionService questionService,
                                  List<Question> pending,
                                  int index,
                                  Runnable after) {
        if (index >= pending.size() || !NetworkUtils.isOnline()) {
            after.run();
            return;
        }
        Question question = pending.get(index);
        String localId = question.getQuestionId();
        questionService.publishQuestion(question)
                .addOnSuccessListener((DocumentReference ref) -> {
                    contentLocalDao.deleteQuestion(localId);
                    syncNextQuestion(questionService, pending, index + 1, after);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Khong dong bo duoc cau hoi " + localId, e);
                    syncNextQuestion(questionService, pending, index + 1, after);
                });
    }

    private void syncPendingMessages(Runnable after) {
        List<ContentLocalDao.PendingMessage> pending = contentLocalDao.getPendingMessages();
        if (pending.isEmpty()) {
            mainHandler.post(after);
            return;
        }
        ChatService chatService = new ChatService();
        syncNextMessage(chatService, pending, 0, after);
    }

    private void syncNextMessage(ChatService chatService,
                                 List<ContentLocalDao.PendingMessage> pending,
                                 int index,
                                 Runnable after) {
        if (index >= pending.size() || !NetworkUtils.isOnline()) {
            mainHandler.post(after);
            return;
        }
        ContentLocalDao.PendingMessage item = pending.get(index);
        chatService.ensureConversationThenSend(item.senderId, item.otherUserId, item.conversationId, item.content)
                .addOnSuccessListener(unused -> {
                    contentLocalDao.deletePendingMessage(item.messageId);
                    syncNextMessage(chatService, pending, index + 1, after);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Khong dong bo duoc tin nhan " + item.messageId, e);
                    syncNextMessage(chatService, pending, index + 1, after);
                });
    }
}
