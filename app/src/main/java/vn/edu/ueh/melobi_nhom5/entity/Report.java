package vn.edu.ueh.melobi_nhom5.entity;

import java.util.UUID;

public class Report {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private String reportId;
    private String reporterId;
    private String targetId;
    private String targetType;
    private String reason;
    private String status;
    private long createdAt;

    public Report() {
        this.status = STATUS_PENDING;
        this.createdAt = System.currentTimeMillis();
    }

    public Report(String reporterId, String targetId, String targetType, String reason) {
        this.reportId = UUID.randomUUID().toString();
        this.reporterId = reporterId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.reason = reason;
        this.status = STATUS_PENDING;
        this.createdAt = System.currentTimeMillis();
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getReporterId() {
        return reporterId;
    }

    public void setReporterId(String reporterId) {
        this.reporterId = reporterId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
