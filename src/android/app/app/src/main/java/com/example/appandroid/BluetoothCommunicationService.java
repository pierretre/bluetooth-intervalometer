package com.example.appandroid;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class BluetoothCommunicationService extends IntentService {

    private ResultReceiver receiver;
    private BluetoothSocket socket;
    private InputStream input;
    private OutputStream output;

    public BluetoothCommunicationService() {
        super("btCommService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        if(bundle.containsKey("receiver")) {
            if (bundle.getParcelable("receiver") != null) {
                this.receiver = bundle.getParcelable("receiver");
            }
        }
        this.socket=socket;
        try{
            this.input=socket.getInputStream();
            this.output=socket.getOutputStream();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }
}