package com.example.appandroid;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Objects;

//import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, BluetoothIntentService.Callbacks {

    private Button connectBtn;
    private Button intervalBtn;
    private ImageButton pictureBtn;

    private TextView status;
    private TextView state;

    private Intent bluetoothIntentService;
    private BluetoothIntentService myService;

    private BluetoothIntervalometerViewModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();

        this.model = new BluetoothIntervalometerViewModel();

        bluetoothIntentService = new Intent(getApplicationContext(), BluetoothIntentService.class);

        disconnect();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.intervalBtn:
                if(String.valueOf(intervalBtn.getText()).equals("start")) {
                    if(myService!=null){
                        EditText nbPics = findViewById(R.id.nbPics);
                        this.model.setPicturesNumber(nbPics.getText().toString());

                        String run = this.model.getRunCommand(); //returns null if the values aren't right
                        if(run==null)
                            Toast.makeText(getApplicationContext(), R.string.badValuesError, Toast.LENGTH_SHORT).show();

                        Toast.makeText(getApplicationContext(), "RUN = "+run, Toast.LENGTH_SHORT).show();

                        TextView time = findViewById(R.id.totalTime);
                        time.setText(model.getTotalTime());
//                    bluetoothComThread.send("STATUS");
                    }else
                        Toast.makeText(myService, R.string.notConnectedError, Toast.LENGTH_SHORT).show();
                }else if(String.valueOf(intervalBtn.getText()).equals("stop")){
                    //TODO
                }
                break;

            case R.id.connectBtn:
                if(String.valueOf(connectBtn.getText()).equals("connect")) {
                    stopService(bluetoothIntentService);
                    startService(bluetoothIntentService);
                    bindService(bluetoothIntentService, serviceConnection, Context.BIND_AUTO_CREATE);

                }else if(String.valueOf(connectBtn.getText()).equals("disconnect")){
//                    Toast.makeText(MainActivity.this, "disconnect", Toast.LENGTH_SHORT).show();

                    disconnect();
                }
                break;

            case R.id.pictureBtn:
//                bluetoothComThread.send("TAKE_SINGLE_SHOT");
                break;

            case R.id.setIntervalBtn:
                Intent interval = new Intent(getApplicationContext(), TimePickingActivity.class);
                interval.putExtra("request", "interval");
                EditText et = findViewById(R.id.interval);
                if(et.getText()!=null)
                    interval.putExtra("value", et.getText().toString());
                startActivityForResult(interval, 1);
                break;

            case R.id.setTimerBtn:
                Intent timer = new Intent(getApplicationContext(), TimePickingActivity.class);
                timer.putExtra("request", "timer");
                EditText ett = findViewById(R.id.timer);
                if(ett.getText()!=null)
                    timer.putExtra("value", ett.getText().toString());
                startActivityForResult(timer, 1);
                break;

            default:
                break;
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothIntentService.LocalBinder binder = (BluetoothIntentService.LocalBinder) service;
            myService = binder.getServiceInstance();
            ((BluetoothIntentService) myService).registerClient(MainActivity.this);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    ((BluetoothIntentService) myService).connect();
                }
            }).start();
            Log.e("MAIN", "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.e("MAIN", "onServiceDisconnected");
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode!=RESULT_CANCELED){
            if(data.hasExtra("interval")) {
                String val = data.getExtras().getString("interval");
                EditText et = findViewById(R.id.interval);
                et.setText(null);
                if(!val.equals("00:00:00"))
                    et.setText(val);
                model.setInterval(val);
            }else if(data.hasExtra("timer")) {
                String val = data.getExtras().getString("timer");
                EditText et = findViewById(R.id.timer);
                et.setText(null);
                if(!val.equals("00:00:00"))
                    et.setText(val);
                model.setTimerDelay(val);
            }
        }
    }

    @Override
    protected void onPause() {
        Log.e("MAIN", "onPause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void disconnect(){
        if(myService!=null)
            unbindService(serviceConnection);
        stopService(bluetoothIntentService);

        intervalBtn.setEnabled(false);
        pictureBtn.setEnabled(false);
        connectBtn.setEnabled(true);
        intervalBtn.setText(R.string.startBtn);
        connectBtn.setText(R.string.connectBtn);
        state.setText(R.string.disconnectedState);
        state.setTextColor(getResources().getColor(R.color.disconnected));
    }

    private void initUI(){
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        state = findViewById(R.id.state);
//        status = findViewById(R.id.status);

        intervalBtn = findViewById(R.id.intervalBtn);
        intervalBtn.setOnClickListener(this);

        connectBtn = findViewById(R.id.connectBtn);
        connectBtn.setText(R.string.connectBtn);
        connectBtn.setOnClickListener(this);

        pictureBtn = findViewById(R.id.pictureBtn);
        pictureBtn.setOnClickListener(this);

        Button setIntervalBtn = findViewById(R.id.setIntervalBtn);
        setIntervalBtn.setOnClickListener(this);

        Button setTimerBtn = findViewById(R.id.setTimerBtn);
        setTimerBtn.setOnClickListener(this);
    }

    @Override
    public void updateUi(String action) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (action){
                    case "DISCOVERING":
                        connectBtn.setEnabled(false);
                        state.setText(R.string.discoveringState);
                        state.setTextColor(getResources().getColor(R.color.pairing));
                        break;

                    case "PAIRING":
                        state.setText(R.string.pairingState);
                        state.setTextColor(getResources().getColor(R.color.pairing));
                        break;

                    case "CONNECTING":
                        connectBtn.setEnabled(false);
                        state.setText(R.string.connectingState);
                        state.setTextColor(getResources().getColor(R.color.connecting));
                        break;

                    case "DISCONNECTED":
                        disconnect();
                        break;

                    case "CONNECTED":
                        intervalBtn.setEnabled(true);
                        pictureBtn.setEnabled(true);
                        connectBtn.setEnabled(true);
                        connectBtn.setText(R.string.disconnectBtn);
                        state.setText(R.string.connectedState);
                        state.setTextColor(getResources().getColor(R.color.connected));
                        break;

                    case "DISCOVERING_ERROR":
                        Toast.makeText(getApplicationContext(), R.string.discoverError, Toast.LENGTH_SHORT).show();
                        disconnect();
                        break;

                    case "PAIRING_ERROR":
                        Toast.makeText(getApplicationContext(), R.string.pairError, Toast.LENGTH_SHORT).show();
                        disconnect();
                        break;

                    case "CONNECTING_ERROR":
                        Toast.makeText(getApplicationContext(), R.string.connectError, Toast.LENGTH_SHORT).show();
                        disconnect();
                        break;

                    default: break;
                }
            }
        });
    }
}