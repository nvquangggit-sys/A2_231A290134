package vn.edu.vhu.ltdd.a2stopwatch;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "A2_231A290134";

    private static final String KEY_RUNNING = "running";
    private static final String KEY_ACCUMULATED = "accumulated";
    private static final String KEY_START = "start";
    private static final String KEY_RECREATE = "recreate";
    private static final String KEY_LAPS = "laps";              // NC1
    private static final String KEY_STOP_ON_BG = "stopOnBg";    // NC2

    private TextView tvTime, tvStatus, tvRecreate, tvLaps;
    private Button btnStartPause, btnReset, btnLap;
    private CheckBox cbStopOnBackground;

    private boolean running = false;
    private long accumulated = 0L;
    private long startTime = 0L;
    private int recreateCount = 0;
    private final ArrayList<String> laps = new ArrayList<>(); // NC1

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateTimeText();
            handler.postDelayed(this, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        tvTime = findViewById(R.id.tvTime);
        tvStatus = findViewById(R.id.tvStatus);
        tvRecreate = findViewById(R.id.tvRecreate);
        tvLaps = findViewById(R.id.tvLaps);
        btnStartPause = findViewById(R.id.btnStartPause);
        btnReset = findViewById(R.id.btnReset);
        btnLap = findViewById(R.id.btnLap);
        cbStopOnBackground = findViewById(R.id.cbStopOnBackground);

        if (savedInstanceState != null) {
            running = savedInstanceState.getBoolean(KEY_RUNNING);
            accumulated = savedInstanceState.getLong(KEY_ACCUMULATED);
            startTime = savedInstanceState.getLong(KEY_START);
            recreateCount = savedInstanceState.getInt(KEY_RECREATE) + 1;

            ArrayList<String> savedLaps = savedInstanceState.getStringArrayList(KEY_LAPS);
            if (savedLaps != null) {
                laps.clear();
                laps.addAll(savedLaps);
            }
            cbStopOnBackground.setChecked(savedInstanceState.getBoolean(KEY_STOP_ON_BG));

            Log.d(TAG, "onCreate: KHÔI PHỤC trạng thái, running=" + running
                    + ", accumulated=" + accumulated + "ms");
        } else {
            Log.d(TAG, "onCreate: khởi tạo mới (savedInstanceState = null)");
        }

        btnStartPause.setOnClickListener(v -> {
            if (running) {
                pauseStopwatch();
            } else {
                startStopwatch();
            }
        });
        btnReset.setOnClickListener(v -> resetStopwatch());

        // NC1 — Nút Vòng (Lap)
        btnLap.setOnClickListener(v -> {
            laps.add(0, "Vòng " + (laps.size() + 1) + ": " + tvTime.getText());
            tvLaps.setText(String.join("\n", laps));
        });

        tvLaps.setText(String.join("\n", laps));
        updateUi();
    }

    private long elapsed() {
        return running ? accumulated + (SystemClock.elapsedRealtime() - startTime) : accumulated;
    }

    private void startStopwatch() {
        running = true;
        startTime = SystemClock.elapsedRealtime();
        startTicking();
        updateUi();
        Log.i(TAG, "BẮT ĐẦU đếm giờ");
    }

    private void pauseStopwatch() {
        accumulated += SystemClock.elapsedRealtime() - startTime;
        running = false;
        stopTicking();
        updateUi();
        Log.i(TAG, "TẠM DỪNG tại " + accumulated + "ms");
    }

    private void resetStopwatch() {
        running = false;
        accumulated = 0L;
        startTime = 0L;
        stopTicking();
        updateUi();
        vibrateOnReset(); // NC3
        Log.i(TAG, "ĐẶT LẠI về 00:00.0");
    }

    private void startTicking() {
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    private void stopTicking() {
        handler.removeCallbacks(ticker);
    }

    private void updateTimeText() {
        long ms = elapsed();
        long phut = ms / 60000;
        long giay = (ms % 60000) / 1000;
        long phanMuoi = (ms % 1000) / 100;
        tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d.%d", phut, giay, phanMuoi));

        // NC3 — đổi màu khi vượt 60 giây
        if (giay + phut * 60 >= 60) {
            tvTime.setTextColor(Color.RED);
        } else {
            tvTime.setTextColor(Color.BLACK);
        }
    }

    // NC3 — rung khi Đặt lại
    private void vibrateOnReset() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
            if (vm != null) {
                vm.getDefaultVibrator().vibrate(
                        VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) {
                v.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } else {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) {
                //noinspection deprecation
                v.vibrate(150);
            }
        }
    }

    private void updateUi() {
        updateTimeText();
        btnStartPause.setText(running ? R.string.pause : R.string.start);
        tvStatus.setText(running ? R.string.status_running : R.string.status_paused);
        tvRecreate.setText(getString(R.string.recreate_count, recreateCount));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume – bật lại việc cập nhật giao diện nếu đồng hồ đang chạy");
        if (running) {
            startTicking();
        }
        updateUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTicking();
        Log.d(TAG, "onPause – tạm dừng cập nhật giao diện");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // NC2 — tự tạm dừng nếu CheckBox được tick
        if (cbStopOnBackground.isChecked() && running) {
            pauseStopwatch();
            Log.d(TAG, "onStop – tự động TẠM DỪNG vì đã tick 'Dừng khi ra nền'");
        }
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onDestroy() {
        stopTicking();
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_RUNNING, running);
        outState.putLong(KEY_ACCUMULATED, accumulated);
        outState.putLong(KEY_START, startTime);
        outState.putInt(KEY_RECREATE, recreateCount);
        outState.putStringArrayList(KEY_LAPS, laps);                      // NC1
        outState.putBoolean(KEY_STOP_ON_BG, cbStopOnBackground.isChecked()); // NC2
        Log.d(TAG, "onSaveInstanceState – đã lưu " + elapsed() + "ms vào Bundle");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState – được gọi sau onStart()");
    }
}