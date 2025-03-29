package com.contoh.yoloapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
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

        if (!OpenCVLoader.initDebug()) {
            throw new RuntimeException("Gagal memuat OpenCV");
        }

        cameraView = findViewById(R.id.camera_view);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);

        resultText = findViewById(R.id.result_text);
        detectButton = findViewById(R.id.detect_button);

        detectButton.setOnClickListener(v -> {
            isDetecting = !isDetecting;
            detectButton.setText(isDetecting ? "Stop Deteksi" : "Mulai Deteksi");
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }

        try {
            tflite = new Interpreter(loadModelFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MappedByteBuffer loadModelFile() throws Exception {
        FileInputStream fileInputStream = new FileInputStream(getAssets().openFd("best_model.tflite").getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileInputStream.available());
    }

    @Override public void onCameraViewStarted(int width, int height) { }
    @Override public void onCameraViewStopped() { }
    
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        if (isDetecting) {
            Bitmap bitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
            org.opencv.android.Utils.matToBitmap(rgba, bitmap);
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

        float[][] outputArray = new float[1][25200];
        tflite.run(inputArray, outputArray);
        int benurCount = 0;
        for (float value : outputArray[0]) {
            if (value > 0.5) {
                benurCount++;
            }
        }
        return benurCount;
    }
}
