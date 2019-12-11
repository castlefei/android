package com.specknet.orientandroid;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.opencsv.CSVWriter;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.reactivex.disposables.Disposable;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends Activity implements AdapterView.OnItemSelectedListener {

    // test device - replace with the real BLE address of your sensor, which you can find
    // by scanning for devices with the NRF Connect App

//    private static final String ORIENT_BLE_ADDRESS = "DA:26:F7:87:FA:38";
    private static final String ORIENT_BLE_ADDRESS = "F1:5D:0F:10:91:85";

    private static final String ORIENT_QUAT_CHARACTERISTIC = "00001526-1212-efde-1523-785feabcd125";
    private static final String ORIENT_RAW_CHARACTERISTIC = "ef680406-9b35-4933-9b10-52ffa9740042";

    private static final boolean raw = true;
    private RxBleDevice orient_device;
    private Disposable scanSubscription;
    private RxBleClient rxBleClient;
    private ByteBuffer packetData;

    //private int n = 0;
    private Long connected_timestamp = null;
    private Long capture_started_timestamp = null;
    boolean connected = false;
    boolean start = true;
    private float freq = 0.f;

    private int counter = 0;
    private CSVWriter writer;
    private CSVWriter writer_temp;
    private File path;
    private File file;
    private File file_temp;
    private boolean logging = false;

    private Button start_button;
    private Context ctx;
    private TextView captureTimetextView;
    private TextView stepView;

    private String group_str = null;

    private final int RC_LOCATION_AND_STORAGE = 1;

    private int stepsCount = 0;
    double threshold = 0.1;

    String[] entries = new String[8];
    String[] latestEntries = new String[8];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start_button = findViewById(R.id.start_button);
        start_button.setBackgroundColor(Color.parseColor("#2bb358"));
        start_button.setText("Start");
        ctx = this;

        getPermissions();
    }


    @AfterPermissionGranted(RC_LOCATION_AND_STORAGE)
    private void getPermissions() {
        String[] perms = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            // Already have permission, do the thing
            // ...
            runApp();

        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.location_and_storage_rationale),
                    RC_LOCATION_AND_STORAGE, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private void runApp() {
        path = Environment.getExternalStorageDirectory();

        start_button = findViewById(R.id.start_button);
        captureTimetextView = findViewById(R.id.captureTimetextView);
        stepView = findViewById(R.id.steps);

        start_button.setOnClickListener(v-> {
            if (start) {
                start = false;
                stepView.setText(Integer.toString(0));

                start_button.setBackgroundColor(Color.parseColor("#e04e43"));
                start_button.setText("Stop");

                // make a new filename based on the start timestamp
                String file_ts = new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date());
                file = new File(path, "PDIoT_" + group_str + "_" + file_ts + ".csv");
                file_temp = new File(path, "temp.csv");

                try {
                    writer = new CSVWriter(new FileWriter(file), ',');
                    writer_temp = new CSVWriter(new FileWriter(file_temp), ',');
                } catch (IOException e) {
                    Log.e("MainActivity", "Caught IOException: " + e.getMessage());
                }

                String[] entries = "timestamp#seq#accel_x#accel_y#accel_z#gyro_x#gyro_y#gyro_z".split("#");
                writer.writeNext(entries);
                writer_temp.writeNext(entries);

                logging = true;
                capture_started_timestamp = System.currentTimeMillis();
                counter = 0;
                Toast.makeText(this, "Start logging",
                        Toast.LENGTH_SHORT).show();
            }
            else {
                start = true;
                start_button.setBackgroundColor(Color.parseColor("#2bb358"));
                start_button.setText("Start");

                logging = false;
                try {
                    writer.flush();
                    writer.close();
                    writer_temp.flush();
                    writer_temp.close();
                    Toast.makeText(this,"Recording saved",
                            Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Log.e("MainActivity", "Caught IOException: " + e.getMessage());
                }
                stepsCount = 0;
            }
        });

        packetData = ByteBuffer.allocate(18);
        packetData.order(ByteOrder.LITTLE_ENDIAN);

        rxBleClient = RxBleClient.create(this);

        scanSubscription = rxBleClient.scanBleDevices(
                new ScanSettings.Builder()
                        // .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // change if needed
                        // .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // change if needed
                        .build()
                // add filters if needed
        )
                .subscribe(
                        scanResult -> {
                            Log.i("OrientAndroid", "FOUND: " + scanResult.getBleDevice().getName() + ", " +
                                    scanResult.getBleDevice().getMacAddress());
                            // Process scan result here.
                            if (scanResult.getBleDevice().getMacAddress().equals(ORIENT_BLE_ADDRESS)) {
                                runOnUiThread(() -> {
                                    Toast.makeText(ctx, "Found " + scanResult.getBleDevice().getName() + ", " +
                                                    scanResult.getBleDevice().getMacAddress(),
                                            Toast.LENGTH_SHORT).show();
                                });
                                connectToOrient(ORIENT_BLE_ADDRESS);
                                scanSubscription.dispose();
                            }
                        },
                        throwable -> {
                            // Handle an error here.
                            runOnUiThread(() -> {
                                Toast.makeText(ctx, "BLE scanning error",
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                );
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
                               long id) {
        // TODO Auto-generated method stub
        if (parent.getItemAtPosition(position).toString().compareTo("---") == 0) return;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub
    }

    private void connectToOrient(String addr) {
        orient_device = rxBleClient.getBleDevice(addr);
        String characteristic;
        if (raw) characteristic = ORIENT_RAW_CHARACTERISTIC; else characteristic = ORIENT_QUAT_CHARACTERISTIC;

        orient_device.establishConnection(false)
                .flatMap(rxBleConnection -> rxBleConnection.setupNotification(UUID.fromString(characteristic)))
                .doOnNext(notificationObservable -> {
                    // Notification has been set up
                })
                .flatMap(notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
                .subscribe(
                        bytes -> {
                            //n += 1;
                            // Given characteristic has been changes, here is the value.

                            //Log.i("OrientAndroid", "Received " + bytes.length + " bytes");
                            if (!connected) {
                                connected = true;
                                connected_timestamp = System.currentTimeMillis();
                                runOnUiThread(() -> {
                                    Toast.makeText(ctx, "Receiving sensor data",
                                            Toast.LENGTH_SHORT).show();
                                    start_button.setEnabled(true);
                                });
                            }
                            if (raw) handleRawPacket(bytes); else handleQuatPacket(bytes);
                        },
                        throwable -> {
                            // Handle an error here.
                            Log.e("OrientAndroid", "Error: " + throwable.toString());
                        }
                );
    }

    private void handleQuatPacket(final byte[] bytes) {
        packetData.clear();
        packetData.put(bytes);
        packetData.position(0);

        int w = packetData.getInt();
        int x = packetData.getInt();
        int y = packetData.getInt();
        int z = packetData.getInt();

        double dw = w / 1073741824.0;  // 2^30
        double dx = x / 1073741824.0;
        double dy = y / 1073741824.0;
        double dz = z / 1073741824.0;

        Log.i("OrientAndroid", "QuatInt: (w=" + w + ", x=" + x + ", y=" + y + ", z=" + z + ")");
        Log.i("OrientAndroid", "QuatDbl: (w=" + dw + ", x=" + dx + ", y=" + dy + ", z=" + dz + ")");
    }

    private void handleRawPacket(final byte[] bytes) {
        long ts = System.currentTimeMillis();
        packetData.clear();
        packetData.put(bytes);
        packetData.position(0);

        float accel_x = packetData.getShort() / 1024.f;  // integer part: 6 bits, fractional part 10 bits, so div by 2^10
        float accel_y = packetData.getShort() / 1024.f;
        float accel_z = packetData.getShort() / 1024.f;

        float gyro_x = packetData.getShort() / 32.f;  // integer part: 11 bits, fractional part 5 bits, so div by 2^5
        float gyro_y = packetData.getShort() / 32.f;
        float gyro_z = packetData.getShort() / 32.f;

        //float mag_x = packetData.getShort() / 16.f;  // integer part: 12 bits, fractional part 4 bits, so div by 2^4
        //float mag_y = packetData.getShort() / 16.f;
        //float mag_z = packetData.getShort() / 16.f;

        //Log.i("OrientAndroid", "Accel:(" + accel_x + ", " + accel_y + ", " + accel_z + ")");
        //Log.i("OrientAndroid", "Gyro:(" + gyro_x + ", " + gyro_y + ", " + gyro_z + ")");
        //if (mag_x != 0f || mag_y != 0f || mag_z != 0f)
        //Log.i("OrientAndroid", "Mag:(" + mag_x + ", " + mag_y + ", " + mag_z + ")");

        if (logging) {
            //String[] entries = "first#second#third".split("#");
            latestEntries = entries;
            entries = new String[]{Long.toString(ts),
                    Integer.toString(counter),
                    Float.toString(accel_x),
                    Float.toString(accel_y),
                    Float.toString(accel_z),
                    Float.toString(gyro_x),
                    Float.toString(gyro_y),
                    Float.toString(gyro_z),
            };
            writer.writeNext(entries);
            writer_temp.writeNext(entries);

            if (counter % 12 == 0) {
                long elapsed_time = System.currentTimeMillis() - capture_started_timestamp;
                int total_secs = (int)elapsed_time / 1000;
                int s = total_secs % 60;
                int m = total_secs / 60;

                String m_str = Integer.toString(m);
                if (m_str.length() < 2) {
                    m_str = "0" + m_str;
                }

                String s_str = Integer.toString(s);
                if (s_str.length() < 2) {
                    s_str = "0" + s_str;
                }


                Long elapsed_capture_time = System.currentTimeMillis() - capture_started_timestamp;
                float connected_secs = elapsed_capture_time / 1000.f;
                freq = counter / connected_secs;
                //Log.i("OrientAndroid", "Packet count: " + Integer.toString(n) + ", Freq: " + Float.toString(freq));

                String time_str = m_str + ":" + s_str;

                String accel_str = "Accel: (" + accel_x + ", " + accel_y + ", " + accel_z + ")";
                String gyro_str = "Gyro: (" + gyro_x + ", " + gyro_y + ", " + gyro_z + ")";
                String freq_str = "Freq: " + freq;

                runOnUiThread(() -> {
                    stepView.setText(Integer.toString(stepsCount));
                    captureTimetextView.setText(time_str);
                });
            }

            if (counter % 10 == 2) {
                if (counter > 2) {
                    try {
                        writer_temp.flush();

                        Python py = Python.getInstance();
                        PyObject mod = py.getModule("test");
                        String[] peaks_threshold = mod.callAttr("count_peaks", threshold).toJava(String.class).split(",");
                        int peaks = Integer.parseInt(peaks_threshold[0]);
                        threshold = Double.parseDouble(peaks_threshold[1]);
                        stepsCount += peaks;

                        writer_temp = new CSVWriter(new FileWriter(file_temp), ',');
                        writer_temp.writeNext(latestEntries);
                        writer_temp.writeNext(entries);
                    } catch (IOException e) {
                        Log.e("MainActivity", "Caught IOException: " + e.getMessage());
                    }
                }
            }

            counter += 1;
        }
    }
}