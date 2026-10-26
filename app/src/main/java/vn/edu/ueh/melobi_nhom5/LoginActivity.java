package vn.edu.ueh.melobi_nhom5;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.service.UserService;
import vn.edu.ueh.melobi_nhom5.util.AuthHelper;
import vn.edu.ueh.melobi_nhom5.util.NetworkUtils;
import vn.edu.ueh.melobi_nhom5.util.RecaptchaHelper;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private Button btnLogin;
    private ProgressBar pbLogin;
    private TextView tvOpenRegister;

    private UserService userService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userService = new UserService();
        initViews();
        setupActions();

        if (AuthHelper.isLoggedIn()) {
            restoreSession();
        }
    }

    private void initViews() {
        tilEmail = findViewById(R.id.tilLoginEmail);
        tilPassword = findViewById(R.id.tilLoginPassword);
        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        pbLogin = findViewById(R.id.pbLogin);
        tvOpenRegister = findViewById(R.id.tvOpenRegister);
    }

    private void setupActions() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        tvOpenRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
        if (etEmail != null) {
            etEmail.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    clearFieldError(tilEmail);
                }
            });
        }
        if (etPassword != null) {
            etPassword.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    clearFieldError(tilPassword);
                }
            });
        }
    }

    private void attemptLogin() {
        if (!NetworkUtils.isOnline()) {
            Toast.makeText(this, R.string.error_no_internet, Toast.LENGTH_SHORT).show();
            return;
        }

        clearFieldError(tilEmail);
        clearFieldError(tilPassword);

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        boolean hasError = false;
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setFieldError(tilEmail, getString(R.string.error_invalid_email));
            hasError = true;
        }
        if (password.isEmpty()) {
            setFieldError(tilPassword, getString(R.string.error_empty_password));
            hasError = true;
        }
        if (hasError) {
            return;
        }

        setLoading(true);
        userService.login(email, password, new UserService.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                verifyRecaptchaThenOpenHome(result);
            }

            @Override
            public void onError(Exception e) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setLoading(false);
                String message = e != null && e.getMessage() != null
                        ? e.getMessage()
                        : getString(R.string.error_login_credentials);
                setFieldError(tilEmail, message);
                setFieldError(tilPassword, message);
            }
        });
    }

    private void verifyRecaptchaThenOpenHome(User user) {
        RecaptchaHelper.verify(this, new RecaptchaHelper.Callback() {
            @Override
            public void onSuccess(String token) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                Toast.makeText(LoginActivity.this, R.string.message_login_success, Toast.LENGTH_SHORT).show();
                openHome(user);
            }

            @Override
            public void onError(Exception e) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                userService.logout();
                setLoading(false);
                Toast.makeText(LoginActivity.this, R.string.error_recaptcha_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setFieldError(TextInputLayout layout, String message) {
        if (layout != null) {
            layout.setErrorEnabled(true);
            layout.setError(message);
        }
    }

    private void clearFieldError(TextInputLayout layout) {
        if (layout != null) {
            layout.setError(null);
            layout.setErrorEnabled(false);
        }
    }

    private void setLoading(boolean isLoading) {
        pbLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        tvOpenRegister.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
    }

    private void restoreSession() {
        String userId = AuthHelper.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            return;
        }

        setLoading(true);
        userService.getProfile(userId, new UserService.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                if (result != null && User.STATUS_LOCKED.equalsIgnoreCase(result.getStatus())) {
                    userService.logout();
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Tài khoản này đã bị khóa. Vui lòng liên hệ quản trị viên.", Toast.LENGTH_SHORT).show();
                    return;
                }
                openHome(result != null ? result : AuthHelper.getCurrentUser());
            }

            @Override
            public void onError(Exception e) {
                if (AuthHelper.isLoggedIn()) {
                    openHome(AuthHelper.getCurrentUser());
                    return;
                }
                setLoading(false);
                Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openHome(User user) {
        if (user != null && user.isAdmin()) {
            Toast.makeText(this, R.string.message_admin_signed_in, Toast.LENGTH_SHORT).show();
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
