package rikka.shizuku.shell.terminal;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import rikka.shizuku.Shizuku;

public class ADBTerminalActivity extends Activity {
    private LinearLayout outputLayout;
    private EditText inputCommand;
    private ScrollView scrollView;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUI();
        checkShizuku();
    }

    private void setupUI() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(16, 16, 16, 16);
        mainLayout.setBackgroundColor(0xFF000000);

        TextView titleView = new TextView(this);
        titleView.setText("⚡ ADB Terminal");
        titleView.setTextColor(0xFF00FF00);
        titleView.setTextSize(18);
        mainLayout.addView(titleView);

        scrollView = new ScrollView(this);
        outputLayout = new LinearLayout(this);
        outputLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(outputLayout);
        mainLayout.addView(scrollView);

        LinearLayout inputLayout = new LinearLayout(this);
        inputLayout.setOrientation(LinearLayout.HORIZONTAL);

        inputCommand = new EditText(this);
        inputCommand.setHint("$ adb command...");
        inputCommand.setTextColor(0xFF00FF00);
        inputCommand.setHintTextColor(0xFF666666);
        inputCommand.setBackgroundColor(0xFF111111);
        inputCommand.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button sendButton = new Button(this);
        sendButton.setText("→");
        sendButton.setTextColor(0xFF00FF00);
        sendButton.setBackgroundColor(0xFF111111);
        sendButton.setOnClickListener(v -> executeCommand());

        inputLayout.addView(inputCommand);
        inputLayout.addView(sendButton);
        mainLayout.addView(inputLayout);

        setContentView(mainLayout);
    }

    private void checkShizuku() {
        if (Shizuku.pingBinder()) {
            appendOutput("✅ Shizuku connected");
        } else {
            appendOutput("❌ Shizuku not running");
        }
    }

    private void executeCommand() {
        String command = inputCommand.getText().toString().trim();
        if (TextUtils.isEmpty(command)) return;

        appendOutput("$ " + command);
        inputCommand.setText("");

        new Thread(() -> {
            try {
                if (!Shizuku.pingBinder()) {
                    handler.post(() -> appendOutput("❌ Shizuku disconnected"));
                    return;
                }

                String shCmd = command.replace("adb ", "");
                Process process = Shizuku.newProcess(new String[]{"sh", "-c", shCmd}, null, null);

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    final String output = line;
                    handler.post(() -> appendOutput(output));
                }

                int exitCode = process.waitFor();
                handler.post(() -> appendOutput("➥ Exit: " + exitCode + "\n"));

            } catch (Exception e) {
                handler.post(() -> appendOutput("❌ Error: " + e.getMessage()));
            }
        }).start();
    }

    private void appendOutput(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF00FF00);
        tv.setTextSize(12);
        tv.setPadding(4, 2, 4, 2);
        outputLayout.addView(tv);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}
