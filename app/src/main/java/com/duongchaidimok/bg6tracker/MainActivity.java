package com.duongchaidimok.bg6tracker;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView statusText;
    private TextView bluetoothText;

    private static final int REQUEST_BLUETOOTH = 1001;
    private static final int REQUEST_CAMERA = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createUserInterface();
        checkBluetooth();
        requestCameraPermission();
    }

    private void createUserInterface() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("BG6 Tracker");
        title.setTextSize(28);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 24);

        TextView description = new TextView(this);
        description.setText(
                "Lightweight visual tracking system\n" +
                "Camera → Processing → BLE"
        );
        description.setTextSize(16);
        description.setTextColor(Color.DKGRAY);
        description.setGravity(Gravity.CENTER);
        description.setPadding(0, 0, 0, 24);

        statusText = new TextView(this);
        statusText.setText("Trạng thái: Đang khởi động...");
        statusText.setTextSize(16);
        statusText.setTextColor(Color.DKGRAY);
        statusText.setPadding(0, 16, 0, 16);

        bluetoothText = new TextView(this);
        bluetoothText.setText("Bluetooth: Đang kiểm tra...");
        bluetoothText.setTextSize(16);
        bluetoothText.setTextColor(Color.DKGRAY);
        bluetoothText.setPadding(0, 16, 0, 16);

        Button cameraButton = new Button(this);
        cameraButton.setText("MỞ CAMERA");

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });

        Button bluetoothButton = new Button(this);
        bluetoothButton.setText("KIỂM TRA BLUETOOTH");

        bluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkBluetooth();
            }
        });

        Button aboutButton = new Button(this);
        aboutButton.setText("THÔNG TIN");

        aboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(
                        MainActivity.this,
                        "BG6 Tracker\nLightweight tracking system",
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        root.addView(title);
        root.addView(description);
        root.addView(statusText);
        root.addView(bluetoothText);
        root.addView(cameraButton);
        root.addView(bluetoothButton);
        root.addView(aboutButton);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(root);

        setContentView(scrollView);
    }

    private void checkBluetooth() {

        BluetoothAdapter bluetoothAdapter =
                BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            bluetoothText.setText("Bluetooth: Thiết bị không hỗ trợ");
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= 31) {

            if (checkSelfPermission(
                    Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        },
                        REQUEST_BLUETOOTH
                );

                return;
            }
        }

        if (bluetoothAdapter.isEnabled()) {
            bluetoothText.setText("Bluetooth: Đã bật");
        } else {
            bluetoothText.setText("Bluetooth: Đang tắt");

            try {
                Intent intent =
                        new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                startActivity(intent);

            } catch (SecurityException e) {
                bluetoothText.setText(
                        "Bluetooth: Chưa được cấp quyền"
                );
            }
        }
    }

    private void requestCameraPermission() {

        if (android.os.Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(
                    Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.CAMERA
                        },
                        REQUEST_CAMERA
                );
            } else {
                statusText.setText("Trạng thái: Camera đã sẵn sàng");
            }

        } else {
            statusText.setText("Trạng thái: Camera đã sẵn sàng");
        }
    }

    private void openCamera() {

        if (android.os.Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(
                    Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.CAMERA
                        },
                        REQUEST_CAMERA
                );

                return;
            }
        }

        Intent intent = new Intent(
                android.provider.MediaStore.ACTION_IMAGE_CAPTURE
        );

        if (intent.resolveActivity(getPackageManager()) != null) {

            try {
                startActivity(intent);

            } catch (SecurityException e) {

                Toast.makeText(
                        this,
                        "Không thể mở camera",
                        Toast.LENGTH_SHORT
                ).show();
            }

        } else {

            Toast.makeText(
                    this,
                    "Không tìm thấy ứng dụng camera",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == REQUEST_CAMERA) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                statusText.setText(
                        "Trạng thái: Camera đã sẵn sàng"
                );

            } else {

                statusText.setText(
                        "Trạng thái: Chưa cấp quyền Camera"
                );
            }
        }

        if (requestCode == REQUEST_BLUETOOTH) {

            checkBluetooth();
        }
    }
  }
