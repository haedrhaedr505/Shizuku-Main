package moe.shizuku.power;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import moe.shizuku.api.Shizuku;

public class PowerActivity extends AppCompatActivity {
    private TextView tvOutput;
    private PowerShellExecutor executor;
    private static final int REQUEST_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_power);
        
        tvOutput = findViewById(R.id.tvOutput);
        executor = PowerShellExecutor.getInstance();
        
        // التحقق من صلاحيات Shizuku
        checkShizukuPermission();
        
        // أزرار التحكم
        findViewById(R.id.btnListApps).setOnClickListener(v -> listApps());
        findViewById(R.id.btnDisableApp).setOnClickListener(v -> showDisableDialog());
        findViewById(R.id.btnForceStop).setOnClickListener(v -> showForceStopDialog());
        findViewById(R.id.btnSystemSettings).setOnClickListener(v -> showSystemSettings());
        findViewById(R.id.btnCustomCommand).setOnClickListener(v -> showCustomCommandDialog());
        findViewById(R.id.btnClear).setOnClickListener(v -> tvOutput.setText(""));
    }
    
    private void checkShizukuPermission() {
        if (Shizuku.pingBinder()) {
            if (!Shizuku.checkSelfPermission()) {
                Shizuku.requestPermission(REQUEST_CODE);
            }
        }
    }
    
    private void listApps() {
        tvOutput.setText("جارٍ تحميل قائمة التطبيقات...");
        executor.listApps(new PowerShellExecutor.CommandCallback() {
            @Override
            public void onResult(String output) {
                runOnUiThread(() -> tvOutput.setText(output));
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> tvOutput.setText("خطأ: " + error));
            }
        });
    }
    
    private void showDisableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("تعطيل تطبيق");
        
        final EditText input = new EditText(this);
        input.setHint("package.name (مثال: com.android.chrome)");
        builder.setView(input);
        
        builder.setPositiveButton("تعطيل", (dialog, which) -> {
            String packageName = input.getText().toString().trim();
            if (!packageName.isEmpty()) {
                tvOutput.setText("جارٍ تعطيل: " + packageName);
                executor.disableApp(packageName, new PowerShellExecutor.CommandCallback() {
                    @Override
                    public void onResult(String output) {
                        runOnUiThread(() -> tvOutput.setText(output));
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> tvOutput.setText("خطأ: " + error));
                    }
                });
            }
        });
        
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }
    
    private void showForceStopDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("إجبار التطبيق على التوقف");
        
        final EditText input = new EditText(this);
        input.setHint("package.name (مثال: com.android.chrome)");
        builder.setView(input);
        
        builder.setPositiveButton("إيقاف", (dialog, which) -> {
            String packageName = input.getText().toString().trim();
            if (!packageName.isEmpty()) {
                tvOutput.setText("جارٍ إيقاف: " + packageName);
                executor.forceStop(packageName, new PowerShellExecutor.CommandCallback() {
                    @Override
                    public void onResult(String output) {
                        runOnUiThread(() -> tvOutput.setText("تم الإيقاف بنجاح"));
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> tvOutput.setText("خطأ: " + error));
                    }
                });
            }
        });
        
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }
    
    private void showSystemSettings() {
        String[] settings = {
            "عرض إعدادات النظام",
            "تغيير سطوع الشاشة",
            "تغيير مهلة الشاشة",
            "وضع الطيران"
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("إعدادات النظام");
        builder.setItems(settings, (dialog, which) -> {
            switch (which) {
                case 0:
                    executor.execute("settings list system", new PowerShellExecutor.CommandCallback() {
                        @Override
                        public void onResult(String output) {
                            runOnUiThread(() -> tvOutput.setText(output));
                        }
                        
                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> tvOutput.setText("خطأ: " + error));
                        }
                    });
                    break;
                case 1:
                    showBrightnessDialog();
                    break;
                case 2:
                    showScreenTimeoutDialog();
                    break;
                case 3:
                    toggleAirplaneMode();
                    break;
            }
        });
        builder.show();
    }
    
    private void showBrightnessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("تغيير السطوع");
        
        final EditText input = new EditText(this);
        input.setHint("قيمة من 0 إلى 255");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);
        
        builder.setPositiveButton("تطبيق", (dialog, which) -> {
            String value = input.getText().toString().trim();
            if (!value.isEmpty()) {
                executor.execute("settings put system screen_brightness " + value, 
                    new PowerShellExecutor.CommandCallback() {
                        @Override
                        public void onResult(String output) {
                            runOnUiThread(() -> tvOutput.setText("تم تغيير السطوع إلى " + value));
                        }
                        
                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> tvOutput.setText("خطأ: " + error));
                        }
                    });
            }
        });
        
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }
    
    private void showScreenTimeoutDialog() {
        String[] timeouts = {
            "15 ثانية", "30 ثانية", "1 دقيقة", "2 دقائق", "5 دقائق", "10 دقائق", "30 دقيقة"
        };
        
        int[] values = {15000, 30000, 60000, 120000, 300000, 600000, 1800000};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("مهلة الشاشة");
        builder.setItems(timeouts, (dialog, which) -> {
            executor.execute("settings put system screen_off_timeout " + values[which],
                new PowerShellExecutor.CommandCallback() {
                    @Override
                    public void onResult(String output) {
                        runOnUiThread(() -> tvOutput.setText("تم تغيير مهلة الشاشة"));
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> tvOutput.setText("خطأ: " + error));
                    }
                });
        });
        builder.show();
    }
    
    private void toggleAirplaneMode() {
        executor.execute("settings put global airplane_mode_on 1 && am broadcast -a android.intent.action.AIRPLANE_MODE",
            new PowerShellExecutor.CommandCallback() {
                @Override
                public void onResult(String output) {
                    runOnUiThread(() -> tvOutput.setText("تم تفعيل وضع الطيران"));
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> tvOutput.setText("خطأ: " + error));
                }
            });
    }
    
    private void showCustomCommandDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("أمر مخصص");
        
        final EditText input = new EditText(this);
        input.setHint("اكتب أي أمر شل");
        builder.setView(input);
        
        builder.setPositiveButton("تنفيذ", (dialog, which) -> {
            String command = input.getText().toString().trim();
            if (!command.isEmpty()) {
                tvOutput.setText("تنفيذ: " + command + "\n\n");
                executor.execute(command, new PowerShellExecutor.CommandCallback() {
                    @Override
                    public void onResult(String output) {
                        runOnUiThread(() -> tvOutput.append("\n" + output));
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> tvOutput.append("\nخطأ: " + error));
                    }
                });
            }
        });
        
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }
}

    // إضافة زر تفعيل Device Admin
    private void setupDeviceAdminButton() {
        Button btnDeviceAdmin = findViewById(R.id.btnDeviceAdmin);
        btnDeviceAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, 
                new ComponentName(this, DeviceAdminReceiver.class));
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                "صلاحيات مالك الجهاز تمكنك من التحكم الكامل");
            startActivity(intent);
        });
    }
