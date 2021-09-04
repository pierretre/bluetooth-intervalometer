package com.example.appandroid;

import android.util.Log;

/**
 * class BluetoothIntervalometerViewModel :
 * Saves the settings set by the user and formats it in a message sent to the arduino device.
 * @author pierrtre
 */
public class BluetoothIntervalometerViewModel {

    private int interval;
    private int timerDelay;
    private int picturesNumber;
    private boolean bulbMode;
    private int shutterSpeed;

    private static final int INTERVAL_TIMER_FIELD_SIZE = 5;
    private static final int PICS_SHUTTER_FIELD_SIZE = 4;

    /**
     * Constructor of the class
     */
    public BluetoothIntervalometerViewModel(){
        this.interval = 0;
        this.timerDelay = 0;
        this.picturesNumber = 0;
        this.bulbMode = false;
        this.shutterSpeed = 0;
    }

    /**
     * sets the interval between to pictures
     * @param str the interval
     */
    public void setInterval(String str){
        String [] hms = str.split(":");
        int ret=0;
        try {
            ret+=Integer.parseInt(hms[2])+Integer.parseInt(hms[1])*60+Integer.parseInt(hms[0])*3600;
        } catch(NumberFormatException nfe) {
            return;
        }
        this.interval=ret;
    }

    /**
     * sets the timer delay before taking pictures
     * @param str the timer delay
     */
    public void setTimerDelay(String str){
        String [] hms = str.split(":");
        int ret=0;
        try {
            if(!str.equals(""))
                ret+=Integer.parseInt(hms[2])+Integer.parseInt(hms[1])*60+Integer.parseInt(hms[0])*3600;
        } catch(NumberFormatException nfe) {
            return;
        }
        this.timerDelay=ret;
    }

    /**
     * sets the number of pictures to take
     * @param nb the number of pictures
     */
    public void setPicturesNumber(String nb){
        if(nb.equals(""))
            this.picturesNumber = 0;
        else if(nb!=null)
            this.picturesNumber = Integer.parseInt(nb);
    }

    /**
     * sets the shutter speed for bulb mode
     * @param str the shutter speed
     */
    public void setShutterSpeed(String str){
        int ret=0;
        try {
            if(!str.equals(""))
                ret+=Integer.parseInt(str);
        } catch(NumberFormatException nfe) {
            return;
        }
        this.shutterSpeed = ret;
    }

    /**
     * method that builds the message to send to the arduino with the values set by the user
     * @return the String containing the following fields separated by ":" :
     *      command "RUN"
     *      interval value in seconds
     *      timer delay value in seconds
     *      number of pictures to take
     *      bit representing the use of bulb mode or not
     *      the shutter speed value in seconds : used if bulb mode has been activated by the user, otherwise this value is not used
     */
    public String getRunCommand(){
        if(this.interval<=0 || this.picturesNumber<=0)
            return null;

        String ret="RUN|";

        for(int i=0; i<(INTERVAL_TIMER_FIELD_SIZE-String.valueOf(this.interval).length()); i++)
            ret+="0";
        ret+=String.valueOf(this.interval)+":";

        for(int i=0; i<(INTERVAL_TIMER_FIELD_SIZE-String.valueOf(this.timerDelay).length()); i++)
            ret+="0";
        ret+=String.valueOf(this.timerDelay)+":";

        for(int i=0; i<(PICS_SHUTTER_FIELD_SIZE-String.valueOf(this.picturesNumber).length()); i++)
            ret+="0";
        ret+=String.valueOf(this.picturesNumber)+":";

        ret+=String.valueOf(this.bulbMode ? 1 : 0)+":";

        for(int i=0; i<(PICS_SHUTTER_FIELD_SIZE-String.valueOf(this.shutterSpeed).length()); i++)
            ret+="0";
        ret+=String.valueOf(this.shutterSpeed);

        return ret;
    }

    /**
     * @return a string to display the total time taken by the device to take all pictures
     * if information is missing, this returns a message to tell the user to fill all required fields
     */
    public String getTotalTime(){
        if(interval<=0)
            return "You must set the interval";
        if(picturesNumber<=0)
            return "You must set the number of pictures";

        int totalTime = this.interval*this.picturesNumber+this.timerDelay;

        String time = "";

        int seconds = totalTime % 60;
        int minutes = (totalTime - seconds) / 60;
        int hours = (minutes - (minutes % 60)) / 60;

        if(String.valueOf(hours).length()<2)
            time+="0";
        time+=String.valueOf(hours)+":";
        if(String.valueOf(minutes).length()<2)
            time+="0";
        time+=String.valueOf(minutes)+":";
        if(String.valueOf(seconds).length()<2)
            time+="0";
        time+=String.valueOf(seconds);
        float videoTime = ((float)this.picturesNumber)/(float)25;
        return "Finished in "
                +String.valueOf(time)+" : \r\n"
                +String.valueOf(videoTime+" seconds of 25 FPS timelapse");
    }

    /**
     * toggles the bulb mode ON/OFF
     */
    public void toggleBulbMode(){
        bulbMode ^= true;
        Log.e("toggle Bulb Mode", "new value = "+String.valueOf(bulbMode));
    }
}