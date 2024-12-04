package com.example.my_bluetooth10;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_BLUETOOTH_PERMISSIONS = 1;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ListView deviceListView;
    private AlertDialog bluetoothDeviceDialog;

    // Bluetooth
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> deviceListAdapter;
    private ArrayList<String> deviceNames = new ArrayList<>();
    private List<BluetoothDevice> deviceList = new ArrayList<>();
    private BluetoothSocket clientSocket;
    private DatagramSocket datagramSocket;
    private InetAddress udpAddress;
    private AcceptThread serverThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        initBluetooth();
    }
    @SuppressLint("MissingPermission")
    private void initUI() {
        Button addBluetoothButton = findViewById(R.id.ADD_BLUETOOTH);
        Button closeBluetoothButton = findViewById(R.id.CLOSE_BLUETOOTH);
        Button button_1=findViewById(R.id.Scene_1);
        Button button_2=findViewById(R.id.Scene_2);
        button_1.setOnClickListener(v -> sendData("bai"));
        button_2.setOnClickListener(v -> sendData("cai"));
        View bluetoothView = getLayoutInflater().inflate(R.layout.activity_bluetooth1, null);
        deviceListView = bluetoothView.findViewById(R.id.btList);
        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);
        deviceListView.setAdapter(deviceListAdapter);
        bluetoothDeviceDialog = new AlertDialog.Builder(this)
                .setTitle("蓝牙设备列表\n")
                .setIcon(R.mipmap.ic_launcher)
                .setView(bluetoothView)
                .setPositiveButton("取消", (dialog, id) -> mBluetoothAdapter.cancelDiscovery())
                .create();
        addBluetoothButton.setOnClickListener(v -> discoverBluetoothDevices());
        closeBluetoothButton.setOnClickListener(v -> closeBluetoothConnection());
    }
    private void sendData(String message){
        if(clientSocket!=null&&clientSocket.isConnected()) {
            try {
                OutputStream outputStream = clientSocket.getOutputStream();
                outputStream.write(message.getBytes());
                outputStream.flush();
                Log.d(TAG, "发送数据" + message);
            } catch (IOException e) {
                Log.e(TAG, "发送数据失败", e);
                Toast.makeText(this, "发送失败", Toast.LENGTH_SHORT).show();

            }
        } else{
            Toast.makeText(this,"未连接蓝牙设备",Toast.LENGTH_SHORT).show();
        }

        }
        @SuppressLint("MissingPermission")
        private void onDeviceSelected(BluetoothDevice device){
        try{
           BluetoothSocket socket=device.createRfcommSocketToServiceRecord(MY_UUID);
            socket.connect();
            clientSocket=socket;
            new AcceptThread().start();
            Toast.makeText(this,"连接成功",Toast.LENGTH_SHORT).show();
        }
        catch (IOException e){
            Log.e(TAG,"连接失败",e);
            Toast.makeText(this,"连接失败，请重试",Toast.LENGTH_SHORT).show();
        }
    }
    @SuppressLint("MissingPermission")
    private void initBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            new AlertDialog.Builder(this)
                    .setMessage("蓝牙未开启，是否开启？")
                    .setPositiveButton("开启", (dialog, id) -> mBluetoothAdapter.enable())
                    .setNegativeButton("取消", (dialog, id) -> finish())
                    .create()
                    .show();
        }

        registerBluetoothReceiver();
    }
    private void checkBluetoothPermissions(){
        if(Build.VERSION_CODES.S <= Build.VERSION.SDK_INT) {

            // Android 12 及更高版本
            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, 1);
            }
        } else {
            // Android 12 以下版本
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_BLUETOOTH_PERMISSIONS);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_BLUETOOTH_PERMISSIONS) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "需要蓝牙权限才能正常工作", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            // 权限已授予，可以执行相关操作
            discoverBluetoothDevices();
        }
    }
    @SuppressLint("MissingPermission")
    private void discoverBluetoothDevices() {
        // 检查蓝牙权限
        checkBluetoothPermissions();
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        // 开始扫描
        mBluetoothAdapter.startDiscovery();
        bluetoothDeviceDialog.show();
        deviceListView.setOnItemClickListener((parent,view,position,id)->{
            BluetoothDevice selectedDevice = deviceList.get(position);
            onDeviceSelected(selectedDevice);
            bluetoothDeviceDialog.dismiss();
        });
    }
    private void registerBluetoothReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, intentFilter);
    }
    @SuppressLint("MissingPermission")
    private void closeBluetoothConnection() {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            mBluetoothAdapter.disable();
        }
        closeSocket();
        Toast.makeText(this, "蓝牙已关闭", Toast.LENGTH_SHORT).show();
    }
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !deviceNames.contains(device.getName())) {
                    deviceNames.add("设备: " + device.getName() + " (" + device.getAddress() + ")");
                    deviceList.add(device);
                    deviceListAdapter.notifyDataSetChanged();//更新设备列表
                }
            }
        }
    };
    public class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;

        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            try {
                serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("BTService", MY_UUID);
                BluetoothSocket socket = serverSocket.accept();
                manageConnection(socket);
            } catch (IOException e) {
                Log.e(TAG, "连接错误", e);
            } finally {
                closeServerSocket();
            }
        }
        private void manageConnection(BluetoothSocket socket) {
            try (InputStream is = socket.getInputStream();
                 OutputStream os = socket.getOutputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    String message = new String(buffer, 0, bytesRead);
                    Log.d(TAG,"收到数据"+message);
                    handler.obtainMessage(0, message).sendToTarget();
                }
            } catch (IOException e) {
                Log.e(TAG, "连接数据处理错误", e);
            }
        }

        private void closeServerSocket() {
            try {
                if (serverSocket != null) serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "关闭服务器套接字错误", e);
            }
        }
    }

    private final Handler handler = new Handler(msg -> {
        String message = (String) msg.obj;
        Toast.makeText(MainActivity.this, "收到数据: " + message, Toast.LENGTH_SHORT).show();
        return true;
    });

    private void closeSocket() {
        try {
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "关闭套接字错误", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothReceiver);
        closeBluetoothConnection();
    }
}



