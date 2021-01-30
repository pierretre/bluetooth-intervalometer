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
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter btAdapter;
    private BluetoothDevice hc05_device;
    private BluetoothCommunicationThread bluetoothComThread;
    private Button connectBtn;
    private Button intervalBtn;

    @Override
    /**
     *
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hc05_device=null;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothComThread=null;

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() != 0){
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().equals("HC-05"))
                    hc05_device=device;
            }
        }

        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // the button that begins the take of photos
        intervalBtn= (Button) findViewById(R.id.intervalBtn);
        intervalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(String.valueOf(intervalBtn.getText()).equals("begin shooting photos")) {
                    bluetoothComThread.send("STATUS");
                }else if(String.valueOf(intervalBtn.getText()).equals("begin shooting photos")){
                    bluetoothComThread.send("STATUS");
                }
            }
        });

        connectBtn = findViewById(R.id.connect);
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(String.valueOf(connectBtn.getText()).equals("connect")) {
                    BluetoothConnectThread btct=new BluetoothConnectThread(hc05_device, btAdapter);
                    connectBtn.setEnabled(false);
                }else if(String.valueOf(connectBtn.getText()).equals("disconnect")){
                    disconnect();
                }
            }
        });

        disconnect();
    }

    private class BluetoothPairThread extends Thread{

        private BluetoothDevice device;
        private boolean isPairing;
        private boolean isDiscovering;

        @Override
        public void run() {

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

            Log.e("Connect", "beginning connection");

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
            bluetoothComThread=new BluetoothCommunicationThread(this.btSocket);
            bluetoothComThread.start();
        }
    }

    private static class BluetoothCommunicationThread extends Thread {

        private InputStream input;
        private OutputStream output;
        private BluetoothSocket socket;

        public BluetoothCommunicationThread(BluetoothSocket socket){
            this.socket=socket;
            try{
                this.input=socket.getInputStream();
                this.output=socket.getOutputStream();
            }catch(IOException ioe){
                ioe.printStackTrace();
            }
        }

        public void run(){

            send("STATUS");

            byte[] messageIn=new byte[1024];
            String inputString=null;
            int byteIndex=0;
            String[] message;

            // Listens to the input of the bluetooth socket to read incoming messages
            while(socket.isConnected()) {
                try {
                    Log.e("Status", "Listening on socket input");

                    messageIn[byteIndex] = (byte) this.input.read();
                    if (messageIn[byteIndex] == '\n') {
                        inputString = new String(messageIn, 0, byteIndex);
                        Log.e("Arduino Message", inputString);
//                        handler.obtainMessage(MESSAGE_READ,readMessage).sendToTarget();
                        byteIndex = 0;
                    } else {
                        byteIndex++;
                    }

                    // if the message was received entirely
                    if (byteIndex == 0) {
                        message = inputString.trim().split("|");

                        if (message[0] == "STATUS") {
                            if (message[1] == "RUNNING") {
                                // Sets the state of the app to running
                                // if message[2]!=null:
                                // OK : prints the message (message[2]) coming from arduino

                                if (message[2] != null)
                                    Log.e("STATUS = RUNNING ", message[2]);
                                else
                                    Log.e("STATUS = RUNNING ", "no message");

                            } else if (message[1] == "WAITING") {
                                // Sets the state of the app to waiting
                                // if message[2]!=null:
                                // ERROR : prints the error message (message[2]) coming from arduino

                                if (message[2] != null)
                                    Log.e("STATUS = WAITING ", message[2]);
                                else
                                    Log.e("STATUS = WAITING ", "no error");
                            }
                        } else {

                        }
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    try{
                        socket.close();
                    }catch (IOException ioe2){
                        Log.e("ERROR", ioe2.getMessage());
                    }
                }
            }
        }

        public void send(String command){
            try{
                byte[] bytes = command.getBytes();
                this.output.write(bytes);
            }catch(IOException ioe){
                ioe.printStackTrace();
            }
        }

        public void closeSocket(){
            try{
                socket.close();
            }catch (IOException ioe){
                Log.e("ERROR", ioe.getMessage());
            }
        }
    }

    @Override
    protected void onPause() {
        Log.e("APP", "onPause()");
        disconnect();
        super.onPause();
    }

    private void disconnect(){
        if(bluetoothComThread!=null && bluetoothComThread.isAlive()){
            bluetoothComThread.closeSocket();
        }
        intervalBtn.setEnabled(false);
        connectBtn.setEnabled(true);
        intervalBtn.setText("begin shooting photos");
        connectBtn.setText("connect");
    }
}