package com.example.appandroid;

public class BluetoothIntervalometerViewModel {

    private int interval;
    private int timerDelay;
    private int picturesNumber;

    public BluetoothIntervalometerViewModel(){
        this.interval=0;
        this.timerDelay=0;
        this.picturesNumber=0;
    }

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

    public void setPicturesNumber(String nb){
        if(nb.equals(""))
            this.picturesNumber = 0;
        else if(nb!=null)
            this.picturesNumber = Integer.parseInt(nb);
    }

    public String getRunCommand(){
        if(this.interval<=0 || this.picturesNumber<=0)
            return null;
        String ret="RUN|";
        for(int i=0; i<(5-String.valueOf(this.interval).length()); i++)
            ret+="0";
        ret+=String.valueOf(this.interval)+":";
        for(int i=0; i<(5-String.valueOf(this.timerDelay).length()); i++)
            ret+="0";
        ret+=String.valueOf(this.timerDelay)+":";
        for(int i=0; i<(4-String.valueOf(this.picturesNumber).length()); i++)
            ret+="0";
        ret+=String.valueOf(this.picturesNumber);
        return ret;
    }

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
}
