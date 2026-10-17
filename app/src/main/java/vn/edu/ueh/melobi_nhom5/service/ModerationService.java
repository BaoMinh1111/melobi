package vn.edu.ueh.melobi_nhom5.service;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
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
        // Khởi tạo đối tượng Report.
        // Lớp Report đã để TỰ ĐỘNG sinh ra ID, trạng thái PENDING và Thời gian.
        Report newReport = new Report(reporterId, targetId, targetType, reason);
        // Đẩy lên Firebase Firestore.
        // Dùng getReportId() đã được tự động tạo để làm tên của Document
        return db.collection(COLLECTION_REPORTS).document(newReport.getReportId()).set(newReport);
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
/*lỗi trong submitReport:
* Không chặn báo cáo trùng và tự báo cáo mình. Mỗi lần bấm gửi tạo thêm một document (ID là UUID ngẫu nhiên), nên một người báo cáo cùng một bài 50 lần 
sẽ ra 50 báo cáo. Đặt ID theo reporterId_targetType_targetId để mỗi người chỉ báo cáo một nội dung một lần, và chặn trường hợp người dùng báo cáo chính mình 
(reporterId.equals(targetId) khi targetType = USER).
* Gửi lại báo cáo sẽ ghi đè báo cáo đã xử lý. Nếu dùng ID cố định mà gọi set(), báo cáo đã RESOLVED sẽ bị đặt lại PENDING. Cần kiểm tra trước 
(trong transaction) rằng document chưa tồn tại.
* Không kiểm tra đầu vào. reason rỗng, targetType ngoài {QUESTION, ANSWER, USER}, hoặc reporterId null đều được ghi thẳng lên Firestore.*/
