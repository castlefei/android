package com.specknet.orientandroid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends Activity {

    // test device - replace with the real BLE address of your sensor, which you can find
    // by scanning for devices with the NRF Connect App

    //region Properties
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
    private TimeView timeView;

    private final int RC_LOCATION_AND_STORAGE = 1;

    private int stepsCount = 0;

    String[] entries = new String[8];
    String[] latestEntries = new String[8];

    private RadioGroup radio_group;

    private String type_of_walk;
    private ProgressArcView progressArcView;
    static public ArrayList<Record> records;
    private Record ongoingRecord;
    private int base;
    //endregion

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start_button = findViewById(R.id.start_button);
//        start_button.setBackgroundColor(Color.parseColor("#2bb358"));
//        start_button.setText("Start");
        ctx = this;
        radio_group = findViewById(R.id.radio_group);
        radio_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (counting) {
                    String type = "";
                    switch (radioGroup.getCheckedRadioButtonId()) {
                        case R.id.walk_radioButton:
                            type = "walking";
                            break;
                        case R.id.climb_radioButton:
                            type = "climbing";
                            break;
                        case R.id.run_radioButton:
                            type = "running";
                            break;
                        default:
                            type = "default";
                            break;
                    }
                    Log.i("typeOfWalk", type_of_walk);
                    ongoingRecord.setTypeOfWalk(type);
                    progressArcView.getOnGoingRecord().setTypeOfWalk(type);
                }
            }
        });
        timeView = findViewById(R.id.captureTimetextView);
        Button plot_button = findViewById(R.id.plot_button);
        plot_button.setOnClickListener(view -> {
            Intent intent = new Intent(this, PlotActivity.class);
//            intent.putExtra("records", records);
            startActivity(intent);
        });

        // dummy data
        progressArcView = findViewById(R.id.progressArcView);
//        List<Record> records = AppDatabase.getDatabase(ctx).recordDao().getAll().getValue();

        records = new ArrayList<Record>();
//        if (records != null){
//            ary.addAll(records);
//        }

        // Dummy records
        records.add(new Record("walking", 34));
        records.add(new Record("running", 26));
        records.add(new Record("climbing", 53));
        records.add(new Record("walking", 37));

        records.add(new Record("walking", 57, -1));
        records.add(new Record("running", 72, -1));
        records.add(new Record("climbing", 44, -1));
        records.add(new Record("walking", 12, -1));

        records.add(new Record("walking", 87, -2));
        records.add(new Record("running", 36, -2));
        records.add(new Record("climbing", 58, -2));
        records.add(new Record("walking", 47, -2));
        progressArcView.setRecords(records);

        //AppDatabase db = AppDatabase.getDatabase(this);
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

        start_button.setOnClickListener(v -> {
            if (start) {    // When start clicked
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
                    timeView.setStarted(true);
                    start_button.setBackgroundColor(Color.parseColor("#e04e43"));
                    start_button.setText("Stop");

                    switch (radio_group.getCheckedRadioButtonId()) {
                        case R.id.walk_radioButton:
                            type_of_walk = "walking";
                            break;
                        case R.id.climb_radioButton:
                            type_of_walk = "climbing";
                            break;
                        case R.id.run_radioButton:
                            type_of_walk = "running";
                            break;
                        default:
                            type_of_walk = "default";
                            break;
                    }

                    ongoingRecord = new Record(type_of_walk, stepsCount);
                    progressArcView.addOnGoingRecord(ongoingRecord);
                    records.add(ongoingRecord);
            } else {  // When stop clicked
                counting = false;
                start = true;
                timeView.setStarted(false);
                start_button.setBackgroundColor(Color.parseColor("#2bb358"));
                start_button.setText("Start");
//                AppDatabase.getDatabase(ctx).recordDao().insert(new Record(type_of_walk, stepsCount));
                ongoingRecord.setCount(stepsCount);
                progressArcView.saveBase();
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
                                    start_button.setBackgroundColor(Color.parseColor("#2bb358"));
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
        ongoingRecord.setCount(stepsCount);
        progressArcView.setCountOfOnGoingRecord(base + stepsCount);
        runOnUiThread(() -> progressArcView.invalidate());
        runOnUiThread(() -> stepView.setText(Integer.toString(ongoingRecord.getCount())));
    }
}
