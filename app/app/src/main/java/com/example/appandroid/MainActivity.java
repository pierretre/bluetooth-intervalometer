package com.example.appandroid;

import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
//import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    static final UUID btUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        connectBT();






    }

    private void connectBT(){
        try {
            enableBT();
            BluetoothDevice intervalometer_hc05 = btAdapter.getRemoteDevice("98:D3:21:F7:50:63");

            BluetoothSocket btSocket = intervalometer_hc05.createInsecureRfcommSocketToServiceRecord(btUUID);


            while(!btSocket.isConnected())
                btSocket.connect();
            Toast.makeText(getApplicationContext(), "connected to BT", Toast.LENGTH_LONG).show();

        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Exception : couldn't connect to BT", Toast.LENGTH_LONG).show();
        }
    }

    private void enableBT(){
        if(btAdapter == null){
            Toast.makeText(getApplicationContext(), "can't find bluetooth adapter", Toast.LENGTH_SHORT).show();
        }
        if(!btAdapter.isEnabled()){
            Toast.makeText(getApplicationContext(), "bluetooth not enabled", Toast.LENGTH_SHORT).show();

            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBT);
        }
        if(btAdapter.isEnabled()){
            Toast.makeText(getApplicationContext(), "bluetooth enabled, it's all good", Toast.LENGTH_SHORT).show();
        }
    }
}