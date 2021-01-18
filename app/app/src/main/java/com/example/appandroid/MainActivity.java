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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        Toast t = Toast.makeText(getApplicationContext(), btAdapter.getBondedDevices().toString(), Toast.LENGTH_LONG);
        t.show();

        if(btAdapter == null){
            Toast toast = Toast.makeText(getApplicationContext(), "can't find bluetooth adapter", Toast.LENGTH_SHORT);
            toast.show();
        }
        if(!btAdapter.isEnabled()){
            Toast toast = Toast.makeText(getApplicationContext(), "bluetooth not enabled", Toast.LENGTH_SHORT);
            toast.show();

            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBT);
        }
        if(btAdapter.isEnabled()){
            Toast toast = Toast.makeText(getApplicationContext(), "bluetooth enabled, it's all good", Toast.LENGTH_SHORT);
            toast.show();
        }

    }
}