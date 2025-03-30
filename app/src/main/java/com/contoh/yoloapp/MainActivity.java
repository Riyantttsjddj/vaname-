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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private CameraBridgeViewBase cameraView;
    private Interpreter tflite;
    private TextView resultText;
    private Button detectButton;
    private boolean isDetecting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.camera_view);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);

        resultText = findViewById(R.id.result_text);
        detectButton = findViewById(R.id.detect_button);

        detectButton.setOnClickListener(v -> {
            isDetecting = !isDetecting;
            detectButton.setText(isDetecting ? "Stop Deteksi" : "Mulai Deteksi");
        });

        // Memastikan izin kamera sudah diberikan
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }

        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Gagal memuat OpenCV!");
        } else {
            Log.d("OpenCV", "OpenCV berhasil dimuat!");
            if (cameraView != null) {
                cameraView.enableView();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (cameraView != null) {
                cameraView.enableView();
            }
        } else {
            Log.e("Izin Kamera", "Akses kamera ditolak!");
        }
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd("best_model.tflite");
        try (FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
             FileChannel fileChannel = inputStream.getChannel()) {
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) { }

    @Override
    public void onCameraViewStopped() { }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        if (rgba.empty()) return rgba; // Hindari error jika frame kosong

        if (isDetecting) {
            Bitmap bitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rgba, bitmap);
            int benurCount = detectBenur(bitmap);
            runOnUiThread(() -> resultText.setText("Jumlah Benur: " + benurCount));
            Log.d("DeteksiBenur", "Jumlah Benur: " + benurCount);
        }
        return rgba;
    }

    private int detectBenur(Bitmap bitmap) {
        int inputSize = 640;
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true);
        int[] intValues = new int[inputSize * inputSize];
        float[][][][] inputArray = new float[1][inputSize][inputSize][3];

        scaledBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize);
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                int pixel = intValues[i * inputSize + j];
                inputArray[0][i][j][0] = ((pixel >> 16) & 0xFF) / 255.0f;
                inputArray[0][i][j][1] = ((pixel >> 8) & 0xFF) / 255.0f;
                inputArray[0][i][j][2] = (pixel & 0xFF) / 255.0f;
            }
        }

        float[][][] outputArray = new float[1][25200][6]; // [batch, jumlah prediksi, 6 atribut]
        tflite.run(inputArray, outputArray);
        int benurCount = 0;
        for (float[] detection : outputArray[0]) {
            if (detection[4] > 0.5) { // Confidence threshold
                benurCount++;
            }
        }
        return benurCount;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraView != null) {
            cameraView.disableView();
        }
        if (tflite != null) {
            tflite.close();
        }
    }
        }
            
