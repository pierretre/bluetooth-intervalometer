package com.example.appandroid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
//import android.support.v7.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private BluetoothAdapter btAdapter;
    private BluetoothDevice hc05_device;
    private BluetoothCommunicationThread bluetoothComThread;


    private Button connectBtn;
    private Button intervalBtn;
    private TextView status;
    private TextView state;
    private Spinner shutterSpeedSpinner;

    private boolean autoConnect=false;

    @Override
    /**
     *
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothComThread=null;

        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        state = findViewById(R.id.state);
        status = findViewById(R.id.status);

        intervalBtn = findViewById(R.id.intervalBtn);
        intervalBtn.setOnClickListener(this);

        connectBtn = findViewById(R.id.connectBtn);
        connectBtn.setOnClickListener(this);

        shutterSpeedSpinner = findViewById(R.id.spinner);
        String[] textSizes = getResources().getStringArray(R.array.font_sizes);

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, textSizes){
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
//                ((TextView) v).setTextSize(30);
//                ((TextView) v).setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                ((TextView) v).setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
                return v;
            }

            public View getDropDownView(int position, View convertView,ViewGroup parent) {
                View v = super.getDropDownView(position, convertView,parent);
                ((TextView) v).setGravity(Gravity.CENTER);
                return v;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        shutterSpeedSpinner.setAdapter(adapter);
        shutterSpeedSpinner.setOnItemSelectedListener(new ShutterSpeedAdapter());

        Toast.makeText(getApplicationContext(), shutterSpeedSpinner.getSelectedItem().toString(), Toast.LENGTH_SHORT).show();
        disconnect();
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.intervalBtn:
                if(String.valueOf(intervalBtn.getText()).equals("begin shooting photos")) {
                    bluetoothComThread.send("STATUS");
                }else if(String.valueOf(intervalBtn.getText()).equals("begin shooting photos")){
                    bluetoothComThread.send("STATUS");
                }
                break;

            case R.id.connectBtn:
                if(String.valueOf(connectBtn.getText()).equals("connect")) {
                    BluetoothConnectThread btct=new BluetoothConnectThread(hc05_device, btAdapter);
                    connectBtn.setEnabled(false);
                }else if(String.valueOf(connectBtn.getText()).equals("disconnect")){
                    disconnect();
                }
                break;

            default:
                break;
        }
    }

    @Override
    protected void onPause() {
        Log.e("APP", "onPause()");
        Toast.makeText(getApplicationContext(), "PAUSE", Toast.LENGTH_SHORT).show();
        disconnect();
        super.onPause();
    }

    @Override
    protected void onResume() {
//        Toast.makeText(getApplicationContext(), "RESUME", Toast.LENGTH_SHORT).show();
        hc05_device=null;
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() != 0){
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().equals("HC-05"))
                    hc05_device=device;
            }
        }
        if(autoConnect){
            BluetoothConnectThread btct=new BluetoothConnectThread(hc05_device, btAdapter);
            connectBtn.setEnabled(false);
        }
        super.onResume();
    }

    private void disconnect(){
        if(bluetoothComThread!=null && bluetoothComThread.isAlive()){
            bluetoothComThread.closeSocket();
        }
        intervalBtn.setEnabled(false);
        connectBtn.setEnabled(true);
        intervalBtn.setText("begin shooting photos");
        connectBtn.setText("connect");
        state.setText("DISCONNECTED");
        state.setTextColor(getResources().getColor(R.color.disconnected));
    }

    private class ShutterSpeedAdapter implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            if (adapterView.getId() == R.id.spinner) {
                String shutterSpeedValue = adapterView.getItemAtPosition(i).toString();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }

    private class BluetoothPairThread extends Thread{

        private BluetoothDevice device;
        private boolean isPairing;
        private boolean isDiscovering;

        @Override
        public void run() {

            state.setText("DISCOVERING");
            state.setTextColor(getResources().getColor(R.color.pairing));

            Log.e("BT pair", "find and pair device");

            isDiscovering=true;

            btAdapter.startDiscovery();
            IntentFilter filter = new IntentFilter();

            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            registerReceiver(mReceiver, filter);

            Log.e("BT pair thread", "while");
            while(isDiscovering){
                Log.e("BT discover", String.valueOf(isDiscovering));
            }

            if(device!=null){
                state.setText("PAIRING");
                status.setText("device ON");
                state.setTextColor(getResources().getColor(R.color.pairing));

                Log.e("BT pair thread", "found device "+device.getName());
                isPairing=true;
                IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                registerReceiver(mPairReceiver, intent);
                try {
                    Method method = device.getClass().getMethod("createBond", (Class[]) null);
                    method.invoke(device, (Object[]) null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                while(isPairing){
                    Log.e("BT pairing", String.valueOf(isPairing));
                }
                Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

                if (pairedDevices.size() != 0){
                    for (BluetoothDevice device : pairedDevices) {
                        if(device.getName().equals("HC-05")){
                            new BluetoothConnectThread(device, btAdapter);
                            return;
                        }
                    }
                }
            }else{
                status.setText("device OFF");
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectBtn.setEnabled(true);
                }
            });
            Log.e("BT pair thread", "end of pairing process");
        }

        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    Log.e("BT discover", "start discovery");
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    Log.e("BT discover", "end discovery");
                    isDiscovering=false;
                } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice bt_device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    Log.e("BT device  :", bt_device.getName()+" / "+bt_device.getAddress()+" / "+(bt_device.getName()!=null && bt_device.getName().equals("HC-05")));
                    if(bt_device.getName()!=null && bt_device.getName().equals("HC-05")){
                        device = bt_device;
                        btAdapter.cancelDiscovery();
                    }
                }
            }
        };

        private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                    final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                    Log.e("BT pair", "ACTION_BOND_STATE_CHANGED");
                    if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                        Log.e("BT pair", "Paired");
                        isPairing=false;
                    } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                        Log.e("BT pair", "Unpaired");
                    }else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDING){
                        Log.e("BT pair", "Not paired");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "You must connect to the the bluetooth device !", Toast.LENGTH_SHORT).show();
                            }
                        });
                        isPairing=false;
                    }
                }
            }
        };
    }

    private class BluetoothConnectThread extends Thread{

        private BluetoothSocket btSocket;

        public BluetoothConnectThread(BluetoothDevice device, BluetoothAdapter adapter){

            if(device==null){
                BluetoothPairThread btpt=new BluetoothPairThread();
                btpt.start();

                if(hc05_device==null)
                    return;
                else
                    device=hc05_device;
            }

            BluetoothSocket tmp = null;
            UUID uuid = device.getUuids()[0].getUuid();
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "exception", Toast.LENGTH_SHORT).show();
            }
            this.btSocket=tmp;
            adapter.cancelDiscovery();
            this.start();
        }

        public void run(){
            state.setText("CONNECTING");
            state.setTextColor(getResources().getColor(R.color.connecting));
            try {
                btSocket.connect();
            } catch (IOException connectException) {
                try {
                    btSocket.close();
                } catch (IOException closeException) {
                    Log.e("Status", "Could not close the client socket", closeException);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectBtn.setEnabled(true);
                        Toast.makeText(getApplicationContext(), "Cannot connect to device, please try again", Toast.LENGTH_SHORT).show();
                        state.setText("DISCONNECTED");
                        state.setTextColor(getResources().getColor(R.color.disconnected));
                    }
                });
                return;
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    intervalBtn.setEnabled(true);
                    connectBtn.setEnabled(true);
                    connectBtn.setText("disconnect");
                }
            });
            state.setText("CONNECTED");
            state.setTextColor(getResources().getColor(R.color.connected));
            bluetoothComThread=new BluetoothCommunicationThread(this.btSocket);
            bluetoothComThread.start();
        }
    }
}