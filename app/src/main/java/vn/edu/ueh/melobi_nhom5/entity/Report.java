package vn.edu.ueh.melobi_nhom5.entities;

import java.util.UUID;
public class Report {
    private String reportId;
    private String reporterId;
    private String targetId;
    private String targetType;
    private String reason;
    private String status;
    private long createdAt;

    // Constructor rỗng - Bắt buộc có để hỗ trợ Firebase Realtime Database/FireStore tự động parse dữ liệu
    public Report(){}

    public Report(String reporterId, String targetId, String targerType, String reason) {
        //Tự động tạo ID ngẫu nhiên cho báo cáo
        this.reportId = UUID.randomUUID().toString();
        // Sau này dùng FireBase, có thể thay bằng ID do FireBase tạo ra bằng lệnh [push().getKey()]
        this.reporterId = reporterId;
        this.targetId = targetId;
        this.targetType = targerType;
        this.reason = reason;
        this.status = "PENDING...";     //Báo cáo mới mặc định ở trạng thái chờ xử lý
        this.createdAt = System.currentTimeMillis();
    }

    public String getReportId() {return reportId; }
    public void setReportId(String reportId) {this.reportId = reportId; }

    public String getReporterId(String reportId) {return reporterId; }
    public void setReporterId() {this.reporterId = reporterId; }

    public String getTargetId() {return targetId; }
    public void setTargetId() {this.targetId = targetId; }

    public String getTargetType() {return targetType; }
    public void setTargetType() {this.targetType = targetType; }

    public String getReason() {return reason; }
    public void setReason() {this.reason = reason; }

    public String getStatus() {return status; }
    public void setStatus() {this.status = status; }

    public long getCreatedAt() {return createdAt; }
    public void setCreatedAt(long createdAt) {this.createdAt = createdAt; }

}
/*1 số lỗi sau:
* các setter không có tham số: 'public void setReporterId() {this.reporterId = reporterId; }' Firestore cần setXxx(giá trị) để đọc dữ liệu về, nên khi toObject(Report.class),
các trường reporterId, targetId, targetType, reason, status sẽ null.
-> Cách sửa: setReporterId(String reporterId)
* thiếu getter/setter của createdAt
* status là chuỗi tiếng Việt "Đang xử lý...". ModerationService sẽ lọc theo getReports(status), nên giá trị phải là hằng số cố định,
không dấu, như PENDING, RESOLVED, REJECTED. Chữ hiển thị cho Admin thì chuyển đổi ở giao diện.*/

