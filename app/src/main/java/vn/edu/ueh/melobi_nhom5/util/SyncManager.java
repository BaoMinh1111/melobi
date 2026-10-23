package vn.edu.ueh.melobi_nhom5.util;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.database.ContentLocalDao;
import vn.edu.ueh.melobi_nhom5.model.Answer;
import vn.edu.ueh.melobi_nhom5.model.Question;

/**
 * Đồng bộ nội dung đã xem xuống SQLite để dùng khi mất mạng.
 */
public class SyncManager {
    private static SyncManager instance;
    private final ContentLocalDao contentLocalDao;

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

    public List<Question> getViewedQuestionsLastDay() {
        List<Question> cached = contentLocalDao.getQuestionsViewedLastDay();
        return cached != null ? cached : new ArrayList<>();
    }

    public List<Answer> getCachedAnswers(String questionId) {
        return contentLocalDao.getAnswersByQuestionId(questionId);
    }

    public void syncAll() {
        // Firestore persistence tự đẩy tin nhắn/ghi chờ khi có mạng lại.
    }
}
