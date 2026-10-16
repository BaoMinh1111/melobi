package vn.edu.ueh.melobi_nhom5.service;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.model.Major;

/** Đọc danh sách ngành từ collection "majors". Nếu thành viên làm Major đã có service riêng thì bỏ file này. */
public class MajorService {
    private static final String COL_MAJORS = "majors";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public Task<List<Major>> getAllMajors() {
        return db.collection(COL_MAJORS).get().continueWith(task -> {
            List<Major> result = new ArrayList<>();
            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                Major m = doc.toObject(Major.class);
                if (m == null) continue;
                if (m.getMajorId() == null || m.getMajorId().isEmpty()) m.setMajorId(doc.getId());
                if (m.getMajorName() == null || m.getMajorName().isEmpty()) continue;
                result.add(m);
            }
            return result;
        });
    }
}