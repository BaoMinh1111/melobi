package vn.edu.ueh.melobi_nhom5.database;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.entity.Answer;
import vn.edu.ueh.melobi_nhom5.entity.Question;

public class SyncManager {

    private static final String TAG = "SyncManager";
    private final FirebaseFirestore db;
    private final ContentLocalDao localDao;

    private static final String COLLECTION_QUESTIONS = "questions";
    private static final String COLLECTION_ANSWERS = "answers";

    public SyncManager(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.localDao = new ContentLocalDao(context);
    }

    // LUỒNG 1: PUSH (ĐẨY DỮ LIỆU OFFLINE LÊN FIREBASE)
    public void pushPendingChanges() {

        List<Question> pendingQuestions = localDao.getPendingQuestions();
        for (Question q : pendingQuestions) {
            db.collection(COLLECTION_QUESTIONS).document(q.getQuestionId())
                    .set(q)
                    .addOnSuccessListener(aVoid -> {
                        localDao.markAsSynced(q.getQuestionId());
                        Log.d(TAG, "Đã PUSH thành công Question: " + q.getQuestionId());
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Lỗi PUSH Question: " + q.getQuestionId(), e));
        }

        List<Answer> pendingAnswers = localDao.getPendingAnswers();
        for (Answer a : pendingAnswers) {
            db.collection(COLLECTION_ANSWERS).document(a.getAnswerId())
                    .set(a)
                    .addOnSuccessListener(aVoid -> {
                        localDao.saveAnswer(a, DatabaseHelper.SYNC_STATUS_SYNCED);
                        Log.d(TAG, "Đã PUSH thành công Answer: " + a.getAnswerId());
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Lỗi PUSH Answer: " + a.getAnswerId(), e));
        }
    }

    // LUỒNG 2: PULL (KÉO DỮ LIỆU MỚI TỪ FIREBASE VỀ SQLITE - Cập nhật likeCount)
    public void syncQuestions() {

        db.collection(COLLECTION_QUESTIONS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Question> serverQuestions = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        serverQuestions.add(doc.toObject(Question.class));
                    }
                    localDao.saveQuestionsList(serverQuestions);
                    Log.d(TAG, "Đã PULL thành công " + serverQuestions.size() + " Questions.");
                })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi PULL Questions", e));
    }

    public void syncAnswers() {

        db.collection(COLLECTION_ANSWERS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Answer> serverAnswers = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        serverAnswers.add(doc.toObject(Answer.class));
                    }
                    localDao.saveAnswers(serverAnswers);
                    Log.d(TAG, "Đã PULL thành công " + serverAnswers.size() + " Answers.");
                })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi PULL Answers", e));
    }

    public void syncAll() {
        pushPendingChanges();
        syncQuestions();
        syncAnswers();
        localDao.clearOldCache();
    }
}