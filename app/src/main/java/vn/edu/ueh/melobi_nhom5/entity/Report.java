package vn.edu.ueh.melobi_nhom5.entity;

import java.io.Serializable;
import java.util.UUID;
public class Report implements Serializable {

    // Các loại đối tượng có thể bị báo cáo (Target Types)
    public static final String TARGET_TYPE_QUESTION = "QUESTION";
    public static final String TARGET_TYPE_ANSWER = "ANSWER";
    public static final String TARGET_TYPE_USER = "USER";

    // Các trạng thái của một báo cáo
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_IGNORED = "IGNORED";

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
        this.status = STATUS_PENDING;     //Báo cáo mới mặc định ở trạng thái chờ xử lý
        this.createdAt = System.currentTimeMillis();
    }

    public String getReportId() {return reportId; }
    public void setReportId(String reportId) {this.reportId = reportId; }

    public String getReporterId() {return reporterId; }
    public void setReporterId(String reporterId) {this.reporterId = reporterId; }

    public String getTargetId() {return targetId; }
    public void setTargetId(String targetId) {this.targetId = targetId; }

    public String getTargetType() {return targetType; }
    public void setTargetType(String targetType) {this.targetType = targetType; }

    public String getReason() {return reason; }
    public void setReason(String reason) {this.reason = reason; }

    public String getStatus() {return status; }
    public void setStatus(String status) {this.status = status; }

    public long getCreatedAt() {return createdAt; }
    public void setCreatedAt(long createdAt) {this.createdAt = createdAt; }

}

