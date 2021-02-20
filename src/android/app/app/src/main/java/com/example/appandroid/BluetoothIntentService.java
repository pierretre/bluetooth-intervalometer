package com.example.appandroid;

import android.app.Activity;
import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class BluetoothIntentService extends IntentService {

//    private ResultReceiver receiver;

    private boolean isPairing;
    private boolean isDiscovering;

    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private BluetoothDevice hc05_device;
    private InputStream input;
    private OutputStream output;

    private final IBinder mBinder = new LocalBinder();
    Callbacks activity;

    public BluetoothIntentService() {
        super("test");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        super.onStartCommand(intent, startId, startId);
        Log.e("SERVICE", "onStartCommand");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e("SERVICE", "onBind");
        return mBinder;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.e("SERVICE", "onHandleIntent");
        this.btAdapter = BluetoothAdapter.getDefaultAdapter();
        this.hc05_device=null;
        Set<BluetoothDevice> pairedDevices = this.btAdapter.getBondedDevices();
        if (pairedDevices.size() != 0){
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().equals("HC-05"))
                    this.hc05_device=device;
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.e("SERVICE", "onDestroy");
        super.onDestroy();
        try{
            if(this.btSocket!=null && this.btSocket.isConnected())
                 this.btSocket.close();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    private void pair(){
        Log.e("SERVICE", "pair");
        this.activity.updateUi("DISCOVERING");

        isDiscovering=true;
        this.btAdapter.startDiscovery();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoverReceiver, filter);

        while(isDiscovering){
            Log.e("BT discover", String.valueOf(isDiscovering));
        }

        if(this.hc05_device!=null){
            this.activity.updateUi("PAIRING");
            unregisterReceiver(discoverReceiver);
            isPairing=true;
            IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            registerReceiver(pairReceiver, intent);
            try {
                Method method = this.hc05_device.getClass().getMethod("createBond", (Class[]) null);
                method.invoke(this.hc05_device, (Object[]) null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            while(isPairing){
                Log.e("BT pairing", String.valueOf(isPairing));
            }
            unregisterReceiver(pairReceiver);
            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

            if (pairedDevices.size() != 0){
                for (BluetoothDevice device : pairedDevices) {
                    if(device.getName().equals("HC-05")){
                        this.connect();
                        return;
                    }
                }
            }
        }
        Log.e("BT pair thread", "end of pairing process");
    }

    public void connect(){
        if(this.hc05_device==null)
            this.pair();
        else{
            this.btAdapter.cancelDiscovery();
            this.activity.updateUi("CONNECTING");

            try {
                UUID uuid = this.hc05_device.getUuids()[0].getUuid();
                this.btSocket = this.hc05_device.createRfcommSocketToServiceRecord(uuid);
                this.btSocket.connect();
                this.input = this.btSocket.getInputStream();
                this.output = this.btSocket.getOutputStream();
                this.activity.updateUi("CONNECTED");

            } catch (IOException ioe) {
                ioe.printStackTrace();
                try {
                    this.btSocket.close();
                } catch (IOException ioe2) {
                    ioe2.printStackTrace();
                }
                this.activity.updateUi("CONNECTING_ERROR");
            }
        }
    }

    private void getBtInput(){
        while(this.btSocket.isConnected()) {
            try {
                BufferedReader bf = new BufferedReader(new InputStreamReader(this.input));
                String line = null;
                if((line = bf.readLine())!=null){
                    String [] inputString = line.split("|");
                    if (inputString[0].equals("RUNNING")) {
                        // Sets the state of the app to running
                        // if message[2]!=null:
                        // OK : prints the message (message[2]) coming from arduino

                        if (inputString[2] != null){
                            Log.e("STATUS = RUNNING ", inputString[2]);

                            //String[] message = inputString[2].split(":");

                            // get information here about the current status of the intervalometer :
                            // can get the number of pictures and the time remaining before the timelapse stops
                        }else
                            Log.e("STATUS = RUNNING ", "no message");
                    }else if (inputString[0].equals("WAITING")) {
                        // Sets the state of the app to waiting
                        // if message[2]!=null:
                        // ERROR : prints the error message (message[2]) coming from arduino

                        if (inputString[2] != null)
                            Log.e("STATUS = WAITING ", inputString[2]);
                        else
                            Log.e("STATUS = WAITING ", "no error");
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                try{
                    this.btSocket.close();
                }catch (IOException ioe2){
                    Log.e("ERROR", ioe2.getMessage());
                }
            }
        }
    }

    public void send(String str){
        try{
            byte[] bytes = str.getBytes();
            this.output.write(bytes);
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }

    public void registerClient(Activity activity){
        this.activity = (Callbacks)activity;
    }

    public class LocalBinder extends Binder {
        public BluetoothIntentService getServiceInstance(){
            return BluetoothIntentService.this;
        }
    }

    public interface Callbacks{
        public void updateUi(String state);
    }

    private final BroadcastReceiver discoverReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.e("BT discover", "start discovery");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.e("BT discover", "end discovery");
                isDiscovering=false;
                if(hc05_device==null)
                    activity.updateUi("DISCOVERING_ERROR");
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice bt_device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                assert bt_device != null;
                Log.e("BT device  :", "found : "+bt_device.getName()+" / "+bt_device.getAddress()+" / "+(bt_device.getName()!=null && bt_device.getName().equals("HC-05")));
                if(bt_device.getName()!=null && bt_device.getName().equals("HC-05")){
                    hc05_device = bt_device;
                    btAdapter.cancelDiscovery();
                }
            }
        }
    };

    private final BroadcastReceiver pairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.e("BT pair", "onReceive");
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
                    isPairing=false;
                    activity.updateUi("PAIRING_ERROR");
                }
            }
        }
    };
}