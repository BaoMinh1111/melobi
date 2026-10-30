package vn.edu.ueh.melobi_nhom5.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.entity.Message;
import vn.edu.ueh.melobi_nhom5.model.Answer;
import vn.edu.ueh.melobi_nhom5.model.Question;

/**
 * Đọc/ghi Question và Answer xuống SQLite để xem khi mất mạng.
 */
public class ContentLocalDao {
    public static final long ONE_DAY_MS = 24L * 60L * 60L * 1000L;

    private final DatabaseHelper helper;

    public ContentLocalDao(Context context) {
        this.helper = DatabaseHelper.getInstance(context);
    }

    public void saveQuestions(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Question question : questions) {
                long viewedAt = existingLastViewedAt(db, question.getQuestionId());
                String syncStatus = existingSyncStatus(db, question.getQuestionId());
                upsertQuestion(db, question, viewedAt, syncStatus);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void saveQuestionDetail(Question question, List<Answer> answers) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            if (question != null) {
                String syncStatus = existingSyncStatus(db, question.getQuestionId());
                if (syncStatus == null || syncStatus.trim().isEmpty()) {
                    syncStatus = DatabaseHelper.SYNC_STATUS_SYNCED;
                }
                upsertQuestion(db, question, System.currentTimeMillis(), syncStatus);
            }
            if (answers != null) {
                for (Answer answer : answers) {
                    upsertAnswer(db, answer);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void savePendingQuestion(Question question) {
        if (question == null) {
            return;
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        upsertQuestion(db, question, System.currentTimeMillis(), DatabaseHelper.SYNC_STATUS_PENDING_ADD);
    }

    public boolean isViewedLastDay(String questionId) {
        if (questionId == null || questionId.trim().isEmpty()) {
            return false;
        }
        long since = System.currentTimeMillis() - ONE_DAY_MS;
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_QUESTION,
                new String[]{DatabaseHelper.COL_Q_ID},
                DatabaseHelper.COL_Q_ID + " = ? AND " + DatabaseHelper.COL_Q_LAST_VIEWED_AT + " >= ?",
                new String[]{questionId, String.valueOf(since)},
                null,
                null,
                null
        );
        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    public Question getQuestionById(String questionId) {
        if (questionId == null || questionId.trim().isEmpty()) {
            return null;
        }
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_QUESTION,
                null,
                DatabaseHelper.COL_Q_ID + " = ?",
                new String[]{questionId},
                null,
                null,
                null
        );
        try {
            if (cursor.moveToFirst()) {
                return readQuestion(cursor);
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public List<Question> getQuestionsViewedLastDay() {
        long since = System.currentTimeMillis() - ONE_DAY_MS;
        List<Question> questions = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_QUESTION,
                null,
                DatabaseHelper.COL_Q_LAST_VIEWED_AT + " >= ?",
                new String[]{String.valueOf(since)},
                null,
                null,
                DatabaseHelper.COL_Q_LAST_VIEWED_AT + " DESC"
        );
        try {
            while (cursor.moveToNext()) {
                questions.add(readQuestion(cursor));
            }
        } finally {
            cursor.close();
        }
        return questions;
    }

    public List<Question> getPendingQuestions() {
        purgeExpired();
        List<Question> questions = new ArrayList<>();
        long since = System.currentTimeMillis() - ONE_DAY_MS;
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_QUESTION,
                null,
                DatabaseHelper.COL_Q_SYNC_STATUS + " = ? AND " + DatabaseHelper.COL_Q_CREATED_AT + " >= ?",
                new String[]{DatabaseHelper.SYNC_STATUS_PENDING_ADD, String.valueOf(since)},
                null,
                null,
                DatabaseHelper.COL_Q_CREATED_AT + " DESC"
        );
        try {
            while (cursor.moveToNext()) {
                questions.add(readQuestion(cursor));
            }
        } finally {
            cursor.close();
        }
        return questions;
    }

    public void deleteQuestion(String questionId) {
        if (questionId == null || questionId.trim().isEmpty()) {
            return;
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_QUESTION, DatabaseHelper.COL_Q_ID + " = ?", new String[]{questionId});
        db.delete(DatabaseHelper.TABLE_ANSWER, DatabaseHelper.COL_A_QUESTION_ID + " = ?", new String[]{questionId});
    }

    public List<Answer> getAnswersByQuestionId(String questionId) {
        List<Answer> answers = new ArrayList<>();
        if (questionId == null || questionId.trim().isEmpty()) {
            return answers;
        }
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_ANSWER,
                null,
                DatabaseHelper.COL_A_QUESTION_ID + " = ?",
                new String[]{questionId},
                null,
                null,
                DatabaseHelper.COL_A_CREATED_AT + " DESC"
        );
        try {
            while (cursor.moveToNext()) {
                answers.add(readAnswer(cursor));
            }
        } finally {
            cursor.close();
        }
        return answers;
    }

    public void savePendingMessage(String messageId,
                                   String conversationId,
                                   String otherUserId,
                                   String senderId,
                                   String content,
                                   long createdAt) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_PM_ID, messageId);
        values.put(DatabaseHelper.COL_PM_CONVERSATION_ID, conversationId);
        values.put(DatabaseHelper.COL_PM_OTHER_USER_ID, otherUserId);
        values.put(DatabaseHelper.COL_PM_SENDER_ID, senderId);
        values.put(DatabaseHelper.COL_PM_CONTENT, content);
        values.put(DatabaseHelper.COL_PM_CREATED_AT, createdAt);
        values.put(DatabaseHelper.COL_PM_SYNC_STATUS, DatabaseHelper.SYNC_STATUS_PENDING_ADD);
        helper.getWritableDatabase().insertWithOnConflict(
                DatabaseHelper.TABLE_PENDING_MESSAGE,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    public List<PendingMessage> getPendingMessages() {
        purgeExpired();
        List<PendingMessage> messages = new ArrayList<>();
        long since = System.currentTimeMillis() - ONE_DAY_MS;
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_PENDING_MESSAGE,
                null,
                DatabaseHelper.COL_PM_SYNC_STATUS + " = ? AND " + DatabaseHelper.COL_PM_CREATED_AT + " >= ?",
                new String[]{DatabaseHelper.SYNC_STATUS_PENDING_ADD, String.valueOf(since)},
                null,
                null,
                DatabaseHelper.COL_PM_CREATED_AT + " ASC"
        );
        try {
            while (cursor.moveToNext()) {
                messages.add(readPendingMessage(cursor));
            }
        } finally {
            cursor.close();
        }
        return messages;
    }

    public List<PendingMessage> getPendingMessages(String conversationId) {
        List<PendingMessage> all = getPendingMessages();
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return all;
        }
        List<PendingMessage> filtered = new ArrayList<>();
        for (PendingMessage message : all) {
            if (conversationId.equals(message.conversationId)) {
                filtered.add(message);
            }
        }
        return filtered;
    }

    public void deletePendingMessage(String messageId) {
        if (messageId == null || messageId.trim().isEmpty()) {
            return;
        }
        helper.getWritableDatabase().delete(
                DatabaseHelper.TABLE_PENDING_MESSAGE,
                DatabaseHelper.COL_PM_ID + " = ?",
                new String[]{messageId}
        );
    }

    public void purgeExpired() {
        long expireBefore = System.currentTimeMillis() - ONE_DAY_MS;
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(
                DatabaseHelper.TABLE_QUESTION,
                DatabaseHelper.COL_Q_SYNC_STATUS + " = ? AND " + DatabaseHelper.COL_Q_CREATED_AT + " < ?",
                new String[]{DatabaseHelper.SYNC_STATUS_PENDING_ADD, String.valueOf(expireBefore)}
        );
        db.delete(
                DatabaseHelper.TABLE_PENDING_MESSAGE,
                DatabaseHelper.COL_PM_CREATED_AT + " < ?",
                new String[]{String.valueOf(expireBefore)}
        );
    }

    private long existingLastViewedAt(SQLiteDatabase db, String questionId) {
        if (questionId == null || questionId.trim().isEmpty()) {
            return 0L;
        }
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_QUESTION,
                new String[]{DatabaseHelper.COL_Q_LAST_VIEWED_AT},
                DatabaseHelper.COL_Q_ID + " = ?",
                new String[]{questionId},
                null,
                null,
                null
        );
        try {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } finally {
            cursor.close();
        }
        return 0L;
    }

    private String existingSyncStatus(SQLiteDatabase db, String questionId) {
        if (questionId == null || questionId.trim().isEmpty()) {
            return DatabaseHelper.SYNC_STATUS_SYNCED;
        }
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_QUESTION,
                new String[]{DatabaseHelper.COL_Q_SYNC_STATUS},
                DatabaseHelper.COL_Q_ID + " = ?",
                new String[]{questionId},
                null,
                null,
                null
        );
        try {
            if (cursor.moveToFirst()) {
                String status = cursor.getString(0);
                return status != null ? status : DatabaseHelper.SYNC_STATUS_SYNCED;
            }
        } finally {
            cursor.close();
        }
        return DatabaseHelper.SYNC_STATUS_SYNCED;
    }

    private void upsertQuestion(SQLiteDatabase db, Question question, long viewedAt, String syncStatus) {
        if (question == null || question.getQuestionId() == null) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_Q_ID, question.getQuestionId());
        values.put(DatabaseHelper.COL_Q_USER_ID, question.getUserId());
        values.put(DatabaseHelper.COL_Q_MAJOR_ID, question.getMajorId());
        values.put(DatabaseHelper.COL_Q_TITLE, question.getTitle());
        values.put(DatabaseHelper.COL_Q_CONTENT, question.getContent());
        values.put(DatabaseHelper.COL_Q_IMAGE_URL, question.getImageUrl());
        values.put(DatabaseHelper.COL_Q_STATUS, question.getStatus());
        values.put(DatabaseHelper.COL_Q_LIKE_COUNT, question.getLikeCount());
        values.put(DatabaseHelper.COL_Q_ANSWER_COUNT, question.getAnswerCount());
        values.put(DatabaseHelper.COL_Q_CREATED_AT, question.getCreatedAt());
        values.put(DatabaseHelper.COL_Q_UPDATED_AT, question.getUpdatedAt());
        values.put(DatabaseHelper.COL_Q_SYNC_STATUS,
                syncStatus != null ? syncStatus : DatabaseHelper.SYNC_STATUS_SYNCED);
        values.put(DatabaseHelper.COL_Q_AUTHOR_NAME, question.getUserName());
        values.put(DatabaseHelper.COL_Q_AUTHOR_AVATAR, question.getUserAvatarUrl());
        values.put(DatabaseHelper.COL_Q_LAST_VIEWED_AT, viewedAt);
        values.put(DatabaseHelper.COL_Q_CATEGORY, question.getCategory());
        db.insertWithOnConflict(DatabaseHelper.TABLE_QUESTION, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private void upsertAnswer(SQLiteDatabase db, Answer answer) {
        if (answer == null || answer.getAnswerId() == null) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_A_ID, answer.getAnswerId());
        values.put(DatabaseHelper.COL_A_QUESTION_ID, answer.getQuestionId());
        values.put(DatabaseHelper.COL_A_USER_ID, answer.getUserId());
        values.put(DatabaseHelper.COL_A_CONTENT, answer.getContent());
        values.put(DatabaseHelper.COL_A_STATUS, answer.getStatus());
        values.put(DatabaseHelper.COL_A_LIKE_COUNT, answer.getLikes());
        values.put(DatabaseHelper.COL_A_COMMENT_COUNT, answer.getCommentCount());
        values.put(DatabaseHelper.COL_A_CREATED_AT, answer.getCreatedAt());
        values.put(DatabaseHelper.COL_A_SYNC_STATUS, DatabaseHelper.SYNC_STATUS_SYNCED);
        values.put(DatabaseHelper.COL_A_AUTHOR_NAME, answer.getUserName());
        values.put(DatabaseHelper.COL_A_AUTHOR_AVATAR, answer.getUserAvatarUrl());
        db.insertWithOnConflict(DatabaseHelper.TABLE_ANSWER, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private Question readQuestion(Cursor cursor) {
        Question question = new Question();
        question.setQuestionId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_ID)));
        question.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_USER_ID)));
        question.setMajorId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_MAJOR_ID)));
        question.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_TITLE)));
        question.setContent(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_CONTENT)));
        question.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_IMAGE_URL)));
        question.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_STATUS)));
        question.setLikeCount(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_LIKE_COUNT)));
        question.setAnswerCount(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_ANSWER_COUNT)));
        question.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_CREATED_AT)));
        question.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_UPDATED_AT)));
        question.setUserName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_AUTHOR_NAME)));
        question.setUserAvatarUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_AUTHOR_AVATAR)));
        int categoryIndex = cursor.getColumnIndex(DatabaseHelper.COL_Q_CATEGORY);
        if (categoryIndex >= 0) {
            question.setCategory(cursor.getString(categoryIndex));
        }
        if (question.getCategory() == null || question.getCategory().trim().isEmpty()) {
            question.setCategory(question.getMajorId());
        }
        return question;
    }

    private Answer readAnswer(Cursor cursor) {
        Answer answer = new Answer();
        answer.setAnswerId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_ID)));
        answer.setQuestionId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_QUESTION_ID)));
        answer.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_USER_ID)));
        answer.setContent(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_CONTENT)));
        answer.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_STATUS)));
        answer.setLikes(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_LIKE_COUNT)));
        answer.setCommentCount(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_COMMENT_COUNT)));
        answer.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_CREATED_AT)));
        answer.setUserName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_AUTHOR_NAME)));
        answer.setUserAvatarUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_AUTHOR_AVATAR)));
        return answer;
    }

    private PendingMessage readPendingMessage(Cursor cursor) {
        PendingMessage message = new PendingMessage();
        message.messageId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PM_ID));
        message.conversationId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PM_CONVERSATION_ID));
        message.otherUserId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PM_OTHER_USER_ID));
        message.senderId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PM_SENDER_ID));
        message.content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PM_CONTENT));
        message.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PM_CREATED_AT));
        return message;
    }

    public static class PendingMessage {
        public String messageId;
        public String conversationId;
        public String otherUserId;
        public String senderId;
        public String content;
        public long createdAt;

        public Message toMessage() {
            Message message = new Message(
                    messageId,
                    conversationId,
                    senderId,
                    content,
                    new Date(createdAt),
                    false
            );
            message.setPending(true);
            return message;
        }
    }
}
