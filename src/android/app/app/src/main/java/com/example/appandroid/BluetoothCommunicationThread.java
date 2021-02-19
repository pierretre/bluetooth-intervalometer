package com.example.appandroid;

import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothCommunicationThread extends Thread {

    private InputStream input;
    private OutputStream output;
    private BluetoothSocket socket;
    private TextView status;

    public BluetoothCommunicationThread(BluetoothSocket socket, TextView status){
        this.socket=socket;
        this.status=status;
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
        String[] message;

        // Listens to the input of the bluetooth socket to read incoming messages
        while(socket.isConnected()) {
            try {
                Log.e("Status", "Listening on socket input");

                messageIn[byteIndex] = (byte) this.input.read();
                if (messageIn[byteIndex] == '\n') {
                    inputString = new String(messageIn, 0, byteIndex);
                    Log.e("Arduino Message", inputString);
                    byteIndex = 0;
                } else {
                    byteIndex++;
                }

                // if the message was received entirely
                if (byteIndex == 0) {
                    message = inputString.trim().split("|");

//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            status.setText(inputString);
//                        }
//                    });

                    if (message[0].equals("STATUS")) {

                        if (message[1].equals("RUNNING")) {
                            // Sets the state of the app to running
                            // if message[2]!=null:
                            // OK : prints the message (message[2]) coming from arduino

                            if (message[2] != null)
                                Log.e("STATUS = RUNNING ", message[2]);
                            else
                                Log.e("STATUS = RUNNING ", "no message");

                        } else if (message[1].equals("WAITING")) {
                            // Sets the state of the app to waiting
                            // if message[2]!=null:
                            // ERROR : prints the error message (message[2]) coming from arduino

                            if (message[2] != null)
                                Log.e("STATUS = WAITING ", message[2]);
                            else
                                Log.e("STATUS = WAITING ", "no error");
                        }
                    } else {

                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                try{
                    socket.close();
                }catch (IOException ioe2){
                    Log.e("ERROR", ioe2.getMessage());
                }
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

    public void closeSocket(){
        try{
            socket.close();
        }catch (IOException ioe){
            Log.e("ERROR", ioe.getMessage());
        }
    }
}