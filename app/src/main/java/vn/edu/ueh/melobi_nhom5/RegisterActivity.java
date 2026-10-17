package vn.edu.ueh.melobi_nhom5;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import vn.edu.ueh.melobi_nhom5.model.Major;
import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.service.UserService;
import vn.edu.ueh.melobi_nhom5.util.AuthHelper;
import vn.edu.ueh.melobi_nhom5.util.NetworkUtils;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etFullName;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private Spinner spinnerMajor;
    private Button btnRegister;
    private ProgressBar pbRegister;
    private TextView tvOpenLogin;

    private UserService userService;
    private List<Major> majorList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        userService = new UserService();
        initViews();
        loadMajors();
        setupActions();

        if (AuthHelper.isLoggedIn()) {
            String userId = AuthHelper.getCurrentUserId();
            if (userId == null || userId.isEmpty()) {
                return;
            }
            userService.getProfile(userId, new UserService.Callback<User>() {
                @Override
                public void onSuccess(User result) {
                    if (result != null && User.STATUS_LOCKED.equalsIgnoreCase(result.getStatus())) {
                        userService.logout();
                        Toast.makeText(RegisterActivity.this, "Tài khoản này đã bị khóa. Vui lòng liên hệ quản trị viên.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    openMainAndFinish();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void initViews() {
        etFullName = findViewById(R.id.etRegisterFullName);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        etConfirmPassword = findViewById(R.id.etRegisterConfirmPassword);
        spinnerMajor = findViewById(R.id.spinnerMajor);
        btnRegister = findViewById(R.id.btnRegister);
        pbRegister = findViewById(R.id.pbRegister);
        tvOpenLogin = findViewById(R.id.tvOpenLogin);
    }

    private void setupMajorSpinner(List<Major> majors) {
        majorList = majors;
        ArrayAdapter<Major> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                majorList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMajor.setAdapter(adapter);
    }

    private void loadMajors() {
        userService.getAllMajors(new UserService.Callback<List<Major>>() {
            @Override
            public void onSuccess(List<Major> result) {
                setupMajorSpinner(result);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupActions() {
        btnRegister.setOnClickListener(v -> attemptRegister());
        tvOpenLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        if (!NetworkUtils.isOnline()) {
            Toast.makeText(this, R.string.error_no_internet, Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";
        Major selectedMajor = (Major) spinnerMajor.getSelectedItem();

        if (selectedMajor == null) {
            Toast.makeText(this, "Vui lòng chọn chuyên ngành", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, R.string.error_password_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        userService.register(email, password, fullName, new UserService.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                userService.updateMajor(result.getUserId(), selectedMajor.getMajorId(), new UserService.Callback<Void>() {
                    @Override
                    public void onSuccess(Void ignored) {
                        Toast.makeText(RegisterActivity.this, R.string.message_register_success, Toast.LENGTH_SHORT).show();
                        openMainAndFinish();
                    }

                    @Override
                    public void onError(Exception e) {
                        setLoading(false);
                        Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        pbRegister.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
        tvOpenLogin.setEnabled(!isLoading);
    }

    private void openMainAndFinish() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
