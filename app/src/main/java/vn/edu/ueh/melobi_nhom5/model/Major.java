package vn.edu.ueh.melobi_nhom5.model;

public class Major {
    private String majorId;
    private String majorName;
    private String description;

    // Bắt buộc: Firestore cần constructor rỗng để chuyển document thành object
    public Major() {
    }

    public Major(String majorId, String majorName, String description) {
        this.majorId = majorId;
        this.majorName = majorName;
        this.description = description;
    }

    public String getMajorId() { return majorId; }
    public void setMajorId(String majorId) { this.majorId = majorId; }

    public String getMajorName() { return majorName; }
    public void setMajorName(String majorName) { this.majorName = majorName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // Để Spinner/ArrayAdapter hiển thị tên chuyên ngành
    @Override
    public String toString() {
        return majorName;
    }
}
