package com.example.appandroid;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Objects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, BluetoothIntentService.Callbacks {

    private Button connectBtn;
    private Button startStopBtn;
    private ImageButton pictureBtn;
    private TextView bulbShutterText ;
    private EditText bulbShutterField;
    private Switch bulbSwitch;
    private EditText intervalField;
    private EditText timerField;
    private ProgressBar spinner;
    private TextView blDeviceProgressIndicator;
    private TextView blDeviceState;

    private Intent bluetoothIntentService;
    private BluetoothIntentService myService;
    private BluetoothIntervalometerViewModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();

        this.model = new BluetoothIntervalometerViewModel();

        this.bluetoothIntentService = new Intent(getApplicationContext(), BluetoothIntentService.class);

        disconnect();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.startStopBtn:
                if(String.valueOf(startStopBtn.getText()).equals("start")) {
                    String run = null;
                    if(myService == null)
                        Toast.makeText(getApplicationContext(), R.string.notConnectedError, Toast.LENGTH_SHORT).show();
                    else if((run = this.model.getRunCommand()) != null){
                        Log.e("RUN", run);
                        myService.send(run);
//                       startStopBtn.setText(R.string.stopBtn);
                    }else
                        Toast.makeText(getApplicationContext(), R.string.badValuesError, Toast.LENGTH_SHORT).show();

                }else if(String.valueOf(startStopBtn.getText()).equals("stop")){
                    myService.send("STOP");
//                   startStopBtn.setText(R.string.startBtn);
                }
                break;

            case R.id.connectBtn:
                if(String.valueOf(connectBtn.getText()).equals("connect")) {
                    stopService(bluetoothIntentService);
                    if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                        startService(bluetoothIntentService);
                        bindService(bluetoothIntentService, serviceConnection, Context.BIND_AUTO_CREATE);
                    }else{
                        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),0);
                    }
                }else if(String.valueOf(connectBtn.getText()).equals("disconnect"))
                    disconnect();

                break;

            case R.id.pictureBtn:
                myService.send("TAKE_SINGLE_SHOT");
                break;

            case R.id.intervalEdit:
            case R.id.setIntervalBtn:
            case R.id.setIntervalLayout:
                Intent interval = new Intent(getApplicationContext(), TimePickingActivity.class);
                interval.putExtra("request", "interval");

                if(intervalField.getText()!=null)
                    interval.putExtra("value", intervalField.getText().toString());
                startActivityForResult(interval, 1);
                break;

            case R.id.setTimerBtn:
            case R.id.timerEdit:
            case R.id.setTimerLayout:
                Intent timer = new Intent(getApplicationContext(), TimePickingActivity.class);
                timer.putExtra("request", "timer");

                if(timerField.getText()!=null)
                    timer.putExtra("value", timerField.getText().toString());
                startActivityForResult(timer, 1);
                break;

            case R.id.bulbSwitch:
                bulbShutterText.setEnabled(bulbSwitch.isChecked());
                bulbShutterField.setEnabled(bulbSwitch.isChecked());

                this.model.toggleBulbMode();
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
        Log.e("onResult", String.valueOf(resultCode)+ " " +String.valueOf(requestCode));
        switch (requestCode){
            case 0:
                if(resultCode == RESULT_CANCELED){
                    Toast.makeText(this, R.string.bluetoothEnableError, Toast.LENGTH_SHORT).show();
                }else{startService(bluetoothIntentService);
                    bindService(bluetoothIntentService, serviceConnection, Context.BIND_AUTO_CREATE);
                }
                break;

            case 1:
                if(resultCode != RESULT_CANCELED){
                    if(data != null){
                        if(data.hasExtra("interval")) {
                            String val = data.getExtras().getString("interval");
                            intervalField.setText(null);
                            if(!val.equals("00:00:00"))
                                intervalField.setText(val);
                            model.setInterval(val);
                        }else if(data.hasExtra("timer")) {
                            String val = data.getExtras().getString("timer");
                            timerField.setText(null);
                            if(!val.equals("00:00:00"))
                                timerField.setText(val);
                            model.setTimerDelay(val);
                        }
                    }
                    updateIndicatorMessage();
                }
                break;

            default:
                break;
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

        myService = null;

       startStopBtn.setEnabled(false);
        pictureBtn.setEnabled(false);
        connectBtn.setEnabled(true);
//       startStopBtn.setText(R.string.startBtn);
        connectBtn.setText(R.string.connectBtn);
        blDeviceState.setText(R.string.disconnectedState);
        blDeviceState.setTextColor(getResources().getColor(R.color.disconnected));

        updateIndicatorMessage();
        spinner.setVisibility(View.GONE);
    }

    /**
     * private method that inits the ui elements when the app starts
     */
    private void initUI(){
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        EditText nbPics = findViewById(R.id.nbPics);
        nbPics.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                model.setPicturesNumber(nbPics.getText().toString());
                updateIndicatorMessage();
            }
            @Override
            public void afterTextChanged(Editable editable) { }
        });

        blDeviceState = findViewById(R.id.state);

        spinner = (ProgressBar)findViewById(R.id.progressBar);
        spinner.setVisibility(View.GONE);

       startStopBtn = findViewById(R.id.startStopBtn);
       startStopBtn.setOnClickListener(this);

        connectBtn = findViewById(R.id.connectBtn);
        connectBtn.setText(R.string.connectBtn);
        connectBtn.setOnClickListener(this);

        pictureBtn = findViewById(R.id.pictureBtn);
        pictureBtn.setOnClickListener(this);

        timerField = findViewById(R.id.timerEdit);
        timerField.setOnClickListener(this);

        intervalField = findViewById(R.id.intervalEdit);
        intervalField.setOnClickListener(this);

        bulbShutterField = findViewById(R.id.bulbShutterEdit);
        bulbShutterField.setEnabled(false);
        bulbShutterField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                model.setShutterSpeed(bulbShutterField.getText().toString());
                updateIndicatorMessage();
            }
            @Override
            public void afterTextChanged(Editable editable) { }
        });

        bulbShutterText = findViewById(R.id.bulbShutterText);
        bulbShutterText.setEnabled(false);

        bulbSwitch = findViewById(R.id.bulbSwitch);
        bulbSwitch.setOnClickListener(this);

        Button setIntervalBtn = findViewById(R.id.setIntervalBtn);
        LinearLayout setIntervalLayout = findViewById(R.id.setIntervalLayout);
        setIntervalBtn.setOnClickListener(this);
        setIntervalLayout.setOnClickListener(this);

        Button setTimerBtn = findViewById(R.id.setTimerBtn);
        LinearLayout setTimerLayout = findViewById(R.id.setTimerLayout);
        setTimerBtn.setOnClickListener(this);
        setTimerLayout.setOnClickListener(this);

        blDeviceProgressIndicator = findViewById(R.id.timeProgressField);
    }

    /**
     * method that updates the indicator message under the fields to tell the user what the settings he set will result in
     */
    private void updateIndicatorMessage(){
        if(myService==null)
            blDeviceProgressIndicator.setText(model.getTotalTime()+"\r\n(You must connect to device)");
        else
            blDeviceProgressIndicator.setText(model.getTotalTime());
    }

    /**
     * method used by the bluetooth intent service to update ui elements when the bluetooth state changes
     * @param action an int corresponding to a certain bluetooth state to display
     */
    @Override
    public void updateUi(int action) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (action) {
                    case R.integer.DISCOVERING:
                        connectBtn.setEnabled(false);
                        blDeviceState.setText(R.string.discoveringState);
                        blDeviceState.setTextColor(getResources().getColor(R.color.pairing));
                        spinner.setVisibility(View.VISIBLE);
                        break;

                    case R.integer.PAIRING:
                        blDeviceState.setText(R.string.pairingState);
                        blDeviceState.setTextColor(getResources().getColor(R.color.pairing));
                        spinner.setVisibility(View.VISIBLE);
                        break;

                    case R.integer.CONNECTING:
                        connectBtn.setEnabled(false);
                        blDeviceState.setText(R.string.connectingState);
                        blDeviceState.setTextColor(getResources().getColor(R.color.connecting));
                        spinner.setVisibility(View.VISIBLE);
                        break;

                    case R.integer.DISCONNECTED:
                        disconnect();
                        break;

                    case R.integer.CONNECTED:
                       startStopBtn.setEnabled(true);
                        pictureBtn.setEnabled(true);
                        connectBtn.setEnabled(true);
                        connectBtn.setText(R.string.disconnectBtn);
                        blDeviceState.setText(R.string.connectedState);
                        blDeviceState.setTextColor(getResources().getColor(R.color.connected));
                        updateIndicatorMessage();
                        spinner.setVisibility(View.GONE);
                        break;

                    case R.integer.DISCOVERING_ERROR:
                        Toast.makeText(getApplicationContext(), R.string.discoverError, Toast.LENGTH_SHORT).show();
                        disconnect();
                        break;

                    case R.integer.PAIRING_ERROR:
                        Toast.makeText(getApplicationContext(), R.string.pairError, Toast.LENGTH_SHORT).show();
                        disconnect();
                        break;

                    case R.integer.CONNECTING_ERROR:
                        Toast.makeText(getApplicationContext(), R.string.connectError, Toast.LENGTH_SHORT).show();
                        disconnect();
                        break;

                    case R.integer.BL_DEVICE_RUNNING:
                        startStopBtn.setText(R.string.stopBtn);

                        break;

                    case R.integer.BL_DEVICE_WAITING:
                        startStopBtn.setText(R.string.startBtn);
                        updateIndicatorMessage();
                        break;

                    default:
                        break;
                }
            }
        });
    }

    @Override
    public void updatePhotoshootProgress(int remaining_time, int remaining_pics, int taken_pics){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                blDeviceProgressIndicator.setText("Time remaining : " + remaining_time +"s"+
                        "\nNumber of remaining pictures : " + remaining_pics +
                        "\nNumber of taken pictures : " + taken_pics);

            }
        });
    }
}