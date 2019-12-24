package com.specknet.boarderOriginal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends Activity {

    // test device - replace with the real BLE address of your sensor, which you can find
    // by scanning for devices with the NRF Connect App

    private static final String ORIENT_BLE_ADDRESS = "D7:97:CB:0D:78:BD";

    private static final String STEP_COUNT_CHARACTERISTIC = "000a001-0000-1000-8000-00805f9b34fb";

    private Disposable scanSubscription;
    private RxBleClient rxBleClient;
    private ByteBuffer packetData;
    private Observable<RxBleConnection> connection;

    boolean connected = false;
    boolean start = true;
    boolean counting = false;

    private Button start_button;
    private Context ctx;
    private TextView stepView;

    private final int RC_LOCATION_AND_STORAGE = 1;

    private int stepsCount = 0;

    @SuppressLint("SetTextI18n")
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
            runApp();

        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.location_and_storage_rationale),
                    RC_LOCATION_AND_STORAGE, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @SuppressLint("SetTextI18n")
    private void runApp() {
        start_button = findViewById(R.id.start_button);
        stepView = findViewById(R.id.steps);

        start_button.setOnClickListener(v-> {
            if (start) {
                counting = true;
                start = false;
                stepView.setText(Integer.toString(0));
                connection
                        .flatMapSingle(rxBleConnection -> rxBleConnection.writeCharacteristic(UUID.fromString(STEP_COUNT_CHARACTERISTIC), ByteBuffer.allocate(4).putInt(0).array()))
                        .subscribe(
                                characteristicValue -> {
                                    // Characteristic value confirmed.
                                    Log.d("WRITE CHARACTERISTIC", "Write successful.");
                                },
                                throwable -> {
                                    // Handle an error here.
                                    Log.d("WRITE CHARACTERISTIC", throwable.getMessage());
                                }
                        );
                start_button.setBackgroundColor(Color.parseColor("#e04e43"));
                start_button.setText("Stop");
            }
            else {
                counting = false;
                start = true;
                start_button.setBackgroundColor(Color.parseColor("#2bb358"));
                start_button.setText("Start");
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
                                runOnUiThread(() -> Toast.makeText(ctx, "Found " + scanResult.getBleDevice().getName() + ", " +
                                                scanResult.getBleDevice().getMacAddress(),
                                        Toast.LENGTH_SHORT).show());
                                connectToOrient();
                                scanSubscription.dispose();
                            }
                        },
                        throwable -> {
                            // Handle an error here.
                            runOnUiThread(() -> Toast.makeText(ctx, "BLE scanning error",
                                    Toast.LENGTH_SHORT).show());
                        }
                );
    }

    @SuppressLint("CheckResult")
    private void connectToOrient() {
        RxBleDevice orient_device = rxBleClient.getBleDevice(ORIENT_BLE_ADDRESS);
        connection = orient_device.establishConnection(false).compose(ReplayingShare.instance());
        connection
                .flatMap(rxBleConnection -> rxBleConnection.setupNotification(UUID.fromString(STEP_COUNT_CHARACTERISTIC)))
                .doOnNext(notificationObservable -> {
                    // Notification has been set up
                })
                .flatMap(notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
                .subscribe(
                        bytes -> {
                            if (!connected) {
                                connected = true;
                                runOnUiThread(() -> {
                                    Toast.makeText(ctx, "Connected.",
                                            Toast.LENGTH_SHORT).show();
                                    start_button.setEnabled(true);
                                });
                            }
                            if (counting)
                                handleRawPacket(bytes);
                        },
                        throwable -> {
                            // Handle an error here.
                            Log.e("OrientAndroid", "Error: " + throwable.toString());
                        }
                );
    }


    @SuppressLint("SetTextI18n")
    private void handleRawPacket(final byte[] bytes) {
        packetData.clear();
        packetData.put(bytes);
        packetData.position(0);

        stepsCount = packetData.getInt();
        runOnUiThread(() -> stepView.setText(Integer.toString(stepsCount)));
    }
}
