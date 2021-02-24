package com.example.appandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

public class TimePickingActivity extends AppCompatActivity {

    private NumberPicker hours;
    private NumberPicker minutes;
    private NumberPicker seconds;

    private String type;
    private String value;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_picking);

        Bundle bundle = getIntent().getExtras();

        TextView message = findViewById(R.id.userMessage);
        hours = findViewById(R.id.hoursPick);
        minutes = findViewById(R.id.minutesPick);
        seconds = findViewById(R.id.secondsPick);

        hours.setMaxValue(24);
        hours.setMinValue(0);
        minutes.setMaxValue(59);
        minutes.setMinValue(0);
        seconds.setMaxValue(59);
        seconds.setMinValue(0);

        String startValue=null;

        if(bundle.containsKey("request") && bundle.getString("request")!=null){
            type=bundle.getString("request");
            if(type.equals("interval"))
                message.setText(R.string.intervalMessage);
            else if(type.equals("timer"))
                message.setText(R.string.timerMessage);
            else
                Log.e("time picking activity","ERROR no parameter was set when launching activity");
        } else {
            finish();
        }
        if(bundle.containsKey("value") && bundle.getString("value")!=null) {
            startValue = bundle.getString("value");
            String [] hms = bundle.getString("value").split(":");
            try {
                hours.setValue(Integer.parseInt(hms[0]));
                minutes.setValue(Integer.parseInt(hms[1]));
                seconds.setValue(Integer.parseInt(hms[2]));
            } catch(NumberFormatException nfe) {
                nfe.printStackTrace();
            }
        }

        Button ok = findViewById(R.id.okBtn);
        String finalStartValue = startValue;
        ok.setOnClickListener(view -> {
            String hms = "";
            if(String.valueOf(hours.getValue()).length()<2)
                hms+="0";
            hms+=hours.getValue()+":";
            if(String.valueOf(minutes.getValue()).length()<2)
                hms+="0";
            hms+=minutes.getValue()+":";
            if(String.valueOf(seconds.getValue()).length()<2)
                hms+="0";
            hms+=String.valueOf(seconds.getValue());

            if(!finalStartValue.equals(hms))
                value = hms;
            finish();
        });

        Button reset = findViewById(R.id.resetBtn);
        reset.setOnClickListener(view -> {
            hours.setValue(0);
            minutes.setValue(0);
            seconds.setValue(0);
        });
    }

    @Override
    public void finish() {

        if(value==null)
            setResult(RESULT_CANCELED);
        else{
            Intent intent = new Intent();
            if(type.equals("interval")) {
                intent.putExtra("interval", value);
                Toast.makeText(getApplicationContext(), "The interval delay was set to : " +""+ value, Toast.LENGTH_SHORT).show();
            }else if(type.equals("timer")){
                intent.putExtra("timer", value);
                Toast.makeText(getApplicationContext(), "The timer delay was set to : "+ value, Toast.LENGTH_SHORT).show();
            }
            setResult(RESULT_OK, intent);
        }
        super.finish();
    }
}