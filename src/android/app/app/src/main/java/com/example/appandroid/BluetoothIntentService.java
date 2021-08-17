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
import java.time.chrono.HijrahChronology;
import java.util.Set;
import java.util.UUID;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class BluetoothIntentService extends IntentService {

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
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        hc05_device=null;
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() != 0){
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().equals("HC-05"))
                    hc05_device=device;
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.e("SERVICE", "onDestroy");
        super.onDestroy();
        unregisterReceiver(connectionStatusReceiver);
        try{
            if(btSocket!=null && btSocket.isConnected())
                 btSocket.close();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    public void connect(){
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() != 0){
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().equals("HC-05"))
                    hc05_device=device;
            }
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        registerReceiver(connectionStatusReceiver, filter);

        if(hc05_device==null) {
            isDiscovering = true;
            activity.updateUi(R.integer.DISCOVERING);
            btAdapter.startDiscovery();

        }else{
            activity.updateUi(R.integer.CONNECTING);

            setupBtConnection();
        }
    }

    private void setupBtConnection(){
        try {
            btSocket = hc05_device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            btSocket.connect();
            Log.e("setupBtConnection", "socket connected ");
            input = btSocket.getInputStream();
            output = btSocket.getOutputStream();

            activity.updateUi(R.integer.CONNECTED);
            readFeedbackFromDevice();

        } catch (IOException ioe) {
            Log.e("setupBtConnection", "catch");
            ioe.printStackTrace();
            try {
                btSocket.close();
            } catch (IOException ioe2) {
                ioe2.printStackTrace();
            }
            activity.updateUi(R.integer.CONNECTING_ERROR);
        }
    }

    private void readFeedbackFromDevice(){
        while(btSocket.isConnected()) {
            try {
                BufferedReader bf = new BufferedReader(new InputStreamReader(input));
                String line = null;
                if((line = bf.readLine()) != null){

                    Log.e("line",line);

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
                    btSocket.close();
                }catch (IOException ioe2){
                    Log.e("ERROR", ioe2.getMessage());
                }
            }
        }
    }

    public void send(String str){
        try{
            byte[] bytes = str.getBytes();
            output.write(bytes);
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
        public void updateUi(int state);
    }

    private final BroadcastReceiver connectionStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
            BluetoothDevice bt_device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                Log.e("BT STATUS RECEIVER", "action bond changed");
                final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    Log.e("BT pair", "Paired");
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    Log.e("BT pair", "Unpaired");
                }else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDING){
                    Log.e("BT pair", "Not paired");
                    activity.updateUi(R.integer.PAIRING_ERROR);
                }
            }
            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Device found
                Log.e("BT STATUS RECEIVER", "device found");

                assert bt_device != null;
                if(bt_device.getName()!=null && bt_device.getName().equals("HC-05")){
                    hc05_device = bt_device;
                    btAdapter.cancelDiscovery();
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action) && isDiscovering) {
                //Done searching
                Log.e("BT STATUS RECEIVER", "done searching");
                 if(hc05_device==null)
                    activity.updateUi(R.integer.DISCOVERING_ERROR);
                 else{
                     activity.updateUi(R.integer.PAIRING);
                     try {
                         Method method = hc05_device.getClass().getMethod("createBond", (Class[]) null);
                         method.invoke(hc05_device, (Object[]) null);
                     } catch (Exception e) {
                         e.printStackTrace();
                     }
                     isDiscovering = false;
                 }
            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Device is now connected
                Log.e("BT STATUS RECEIVER", "now connected to "+bt_device.getName());

                if(btSocket == null) {
                    setupBtConnection();
                }
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action) && btSocket != null) {
                //Device has disconnected
                Log.e("BT STATUS RECEIVER", "disconnected");
                activity.updateUi(R.integer.DISCONNECTED);
            }
        }
    };
}