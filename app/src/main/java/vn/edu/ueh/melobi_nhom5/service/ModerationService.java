package vn.edu.ueh.melobi_nhom5.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;
import java.util.UUID;
import vn.edu.ueh.melobi_nhom5.entity.Report;

public class ModerationService {

    private final FirebaseFirestore db;
    // Tên collection trên Firebase
    private static final String COLLECTION_REPORTS = "reports";

    public ModerationService() {
        // Khởi tạo kết nối tới Firebase Firestore
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * 1. HÀM DÀNH CHO USER: Gửi báo cáo vi phạm
     * Theo thiết kế, hàm cần nhận: reporterId, targetId, targetType, reason
     */
    public Task<Void> submitReport(String reporterId, String targetId, String targetType, String reason) {
        if (reporterId == null || reporterId.trim().isEmpty()) {
            return Tasks.<Void>forException(new IllegalArgumentException("ID người báo cáo không hợp lệ"));
        }
        if (targetId == null || targetId.trim().isEmpty()) {
            return Tasks.<Void>forException(new IllegalArgumentException("ID đối tượng bị báo cáo không hợp lệ"));
        }
        if (reason == null || reason.trim().isEmpty()) {
            return Tasks.<Void>forException(new IllegalArgumentException("Lý do báo cáo không được để trống"));
        }

        // Đảm bảo targetType chỉ nằm trong 3 loại được quy định
        if (!Report.TARGET_TYPE_QUESTION.equals(targetType) &&
                !Report.TARGET_TYPE_ANSWER.equals(targetType) &&
                !Report.TARGET_TYPE_USER.equals(targetType)) {
            return Tasks.<Void>forException(new IllegalArgumentException("Loại đối tượng báo cáo không hợp lệ"));
        }

        if (Report.TARGET_TYPE_USER.equals(targetType) && reporterId.equals(targetId)) {
            return Tasks.<Void>forException(new IllegalStateException("Bạn không thể tự báo cáo chính mình"));
        }

        // Cấu trúc ID: reporterId_targetType_targetId
        String compositeReportId = reporterId + "_" + targetType + "_" + targetId;

        DocumentReference reportRef = db.collection(COLLECTION_REPORTS).document(compositeReportId);

        // runTransaction giúp khóa (lock) document trên server để kiểm tra trước khi ghi
        return db.runTransaction(new Transaction.Function<Void>() {
            @Override
            public Void apply(Transaction transaction) throws FirebaseFirestoreException {
                // Đọc dữ liệu hiện tại từ Firebase
                DocumentSnapshot snapshot = transaction.get(reportRef);

                if (snapshot.exists()) {
                    // Nếu báo cáo đã tồn tại, lập tức ngắt Transaction và ném ra lỗi.
                    // Điều này bảo vệ các báo cáo đã bị xử lý (RESOLVED/IGNORED) khỏi việc bị đưa về PENDING
                    throw new FirebaseFirestoreException(
                            "Bạn đã gửi báo cáo cho nội dung này rồi. Vui lòng chờ quản trị viên xử lý.",
                            FirebaseFirestoreException.Code.ALREADY_EXISTS
                    );
                }

                // Nếu chưa tồn tại, khởi tạo đối tượng Report mới
                Report newReport = new Report(reporterId, targetId, targetType, reason);

                // Ghi đè lại cái UUID ngẫu nhiên bằng Khóa phức hợp ta vừa tạo
                newReport.setReportId(compositeReportId);

                // Đẩy dữ liệu lên
                transaction.set(reportRef, newReport);

                return null; // Giao dịch thành công
            }
        });
    }

    /**
     * Admin xem danh sách báo cáo theo trạng thái (PENDING, RESOLVED...)
     */
    public void getReports(String status) {
        // TODO: Viết logic truy vấn Firebase lấy danh sách report có status tương ứng
    }

    /**
     * Admin xử lý báo cáo (Ví dụ: action = "IGNORE" hoặc "BAN")
     */
    public void resolveReport(String reportId, String action) {
        // TODO: Viết logic cập nhật trạng thái của Report trên Firebase
    }

    /**
     * Gọi QuestionService hoặc AnswerService để ẩn nội dung vi phạm
     */
    public void hideContent(String targetId, String targetType) {
        // TODO: Viết logic gọi hàm hideQuestion hoặc hideAnswer
    }

    /**
     * Gọi UserService để khóa hoặc mở khóa tài khoản
     */
    public void lockUser(String userId) {
        // TODO: Cập nhật status của User thành "LOCKED"
    }

    public void unlockUser(String userId) {
        // TODO: Cập nhật status của User thành "ACTIVE"
    }
}