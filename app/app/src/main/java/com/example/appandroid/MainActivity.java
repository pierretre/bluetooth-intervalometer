package com.example.appandroid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.location.LocationManagerCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
//import android.support.v7.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Set;
import java.util.UUID;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    static final UUID btUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter btAdapter;
    private BluetoothDevice hc05_device;
    private BluetoothCommunication bluetoothComThread;

    @Override
    /**
     *
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // the button that begins the take of photos 
        Button intervalBtn= (Button) findViewById(R.id.intervalBtn);
        intervalBtn.setText("begin photo shoot");

        intervalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(String.valueOf(intervalBtn.getText())){
                    case "begin photo shoot" :
                        Log.e("Status","begin");
                        bluetoothComThread.send("STATUS");
                        break;
                    case "stop photo shoot" :
                        Log.e("Status","stop");
                        break;
                    default:
                        break;
                }
            }
        });


        hc05_device=null;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        if (pairedDevices.size() != 0){
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().equals("HC-05"))
                    hc05_device=device;
            }
        }

        // if hc05 was not found in the list of paired devices :
        if(hc05_device==null){
            return;
        }
        BluetoothConnectThread btct=new BluetoothConnectThread(hc05_device);
        btct.start();
    }



    private class BluetoothConnectThread extends Thread{

        private BluetoothSocket btSocket;

        public BluetoothConnectThread(BluetoothDevice device){
            BluetoothSocket tmp = null;
            UUID uuid = device.getUuids()[0].getUuid();

            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "exception", Toast.LENGTH_SHORT).show();
            }
            this.btSocket=tmp;
        }

        public void run(){
            // try to make connection with bluetooth device

            btAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                btSocket.connect();
                Log.e("Status", "Device connected");
//                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    btSocket.close();
                    Log.e("Status", "Cannot connect to device");
//                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e("Status", "Could not close the client socket", closeException);
                }
                return;
            }
            bluetoothComThread=new BluetoothCommunication(this.btSocket);
            bluetoothComThread.start();
        }
    }

    private static class BluetoothCommunication extends Thread {

        private InputStream input;
        private OutputStream output;
        private BluetoothSocket socket;

        public BluetoothCommunication(BluetoothSocket socket){
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
            boolean isConnected=true;
            String[] message;

            // Listens to the input of the bluetooth socket to read incoming messages
            while(isConnected){
                try{
                    Log.e("Status","Listening on socket input");

                    messageIn[byteIndex] = (byte) this.input.read();
                    if (messageIn[byteIndex] == '\n'){
                        inputString = new String(messageIn,0,byteIndex);
                        Log.e("Arduino Message",inputString);
//                        handler.obtainMessage(MESSAGE_READ,readMessage).sendToTarget();
                        byteIndex = 0;
                    } else {
                        byteIndex++;
                    }

                    // if the message was received entirely
                    if(byteIndex==0){
                        message=inputString.trim().split("|");

                        if(message[0]=="STATUS"){
                            if(message[1]=="RUNNING") {
                                // Sets the state of the app to running
                                // if message[2]!=null:
                                // OK : prints the message (message[2]) coming from arduino

                                if(message[2]!=null)
                                    Log.e("STATUS = RUNNING ",message[2]);
                                else
                                    Log.e("STATUS = RUNNING ","no message");

                            }else if(message[1]=="WAITING") {
                                // Sets the state of the app to waiting
                                // if message[2]!=null:
                                // ERROR : prints the error message (message[2]) coming from arduino

                                if(message[2]!=null)
                                    Log.e("STATUS = WAITING ",message[2]);
                                else
                                    Log.e("STATUS = WAITING ","no error");
                            }
                        }else {

                        }
                    }
                }catch (IOException ioe){
                    ioe.printStackTrace();
                    isConnected=false;
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
    }


}