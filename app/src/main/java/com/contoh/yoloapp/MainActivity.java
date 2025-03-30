package com.contoh.yoloapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private JavaCameraView cameraView;
    private Interpreter tflite;
    private TextView resultText;
    private Button detectButton;
    private boolean isDetecting = false;

    private static final int CAMERA_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🔹 Cek apakah OpenCV berhasil dimuat
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "❌ Gagal memuat OpenCV!");
            Toast.makeText(this, "Gagal memuat OpenCV!", Toast.LENGTH_LONG).show();
            return;
        } else {
            Log.d("OpenCV", "✅ OpenCV berhasil dimuat.");
        }

        // 🔹 Inisialisasi elemen UI dari activity_main.xml
        cameraView = findViewById(R.id.camera_view);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);
        cameraView.setCameraIndex(0); // 0 = Kamera belakang

        resultText = findViewById(R.id.result_text);
        detectButton = findViewById(R.id.detect_button);

        detectButton.setOnClickListener(v -> {
            isDetecting = !isDetecting;
            detectButton.setText(isDetecting ? "Stop Deteksi" : "Deteksi Benur");
        });

        // 🔹 Periksa izin kamera sebelum mengaktifkan kamera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            cameraView.enableView();
        }

        // 🔹 Muat model YOLO
        try {
            tflite = new Interpreter(loadModelFile());
            Log.d("YOLO", "✅ Model YOLO berhasil dimuat.");
        } catch (IOException e) {
            Log.e("YOLO", "❌ Gagal memuat model YOLO!", e);
        }
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd("best_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            cameraView.enableView();
        } else {
            Toast.makeText(this, "Izin kamera ditolak!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d("Camera", "✅ Kamera dimulai dengan resolusi: " + width + "x" + height);
    }

    @Override
    public void onCameraViewStopped() {
        Log.d("Camera", "⏹ Kamera dihentikan.");
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        Log.d("CameraFrame", "📸 Frame diterima dari kamera.");

        if (isDetecting) {
            Bitmap bitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
            org.opencv.android.Utils.matToBitmap(rgba, bitmap);
            int benurCount = detectBenur(bitmap);

            runOnUiThread(() -> resultText.setText("Jumlah Benur: " + benurCount));
            Log.d("DeteksiBenur", "🐟 Jumlah Benur: " + benurCount);
        }
        return rgba;
    }

    private int detectBenur(Bitmap bitmap) {
        return 0; // Gantilah dengan logika deteksi YOLO
    }
            }
    
