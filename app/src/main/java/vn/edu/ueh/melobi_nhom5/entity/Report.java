package vn.edu.ueh.melobi_nhom5.entity;

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
/*1 số lỗi:
* status = "PENDING..." (có 3 dấu chấm) 
-> Ảnh hưởng đến ModerationService: getReports("PENDING") không tìm thấy báo cáo nào, vì giá trị lưu là "PENDING...". Admin luôn thấy danh sách rỗng
* setReporterId(), setTargetId(), setTargetType(), setReason(), setStatus() không có tham số
-> Ảnh hưởng đến ModerationService: Đọc Report từ Firestore (toObject) ra các trường này đều null
* getReporterId(String reportId) có tham số thừa
-> Ảnh hưởng đến ModerationService: Firestore không nhận đây là getter, nên reporterId không được ghi lên Firestore
* Không có hằng số targetType, status*/
