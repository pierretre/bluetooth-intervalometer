#include <SoftwareSerial.h>

SoftwareSerial mySerial(6, 5); // RX, TX for Bluetooth
float time_between_pics, shutter_speed, delay_duration;
int pics_number = 0, CAMERA = 2;

void setup() {
  //pinMode(CAMERA, OUTPUT);
  //digitalWrite(CAMERA, HIGH);
  Serial.begin(9600);
  mySerial.begin(9600);
  mySerial.setTimeout(10);
}

void loop() {
  boolean OK_values = true;

  while (mySerial.available() == 0);

  String inputString = mySerial.readString();
  Serial.print("Message received : "); Serial.println(inputString);

  inputString.trim();

  if (inputString == "TAKE_SINGLE_SHOT") {
    // take a single picture
    //digitalWrite(CAMERA, LOW);
    delay(250);
    //digitalWrite(CAMERA, HIGH);

  } else {
    shutter_speed = inputString.substring(4, 10).toFloat();
    time_between_pics = inputString.substring(11, 17).toFloat();
    pics_number = inputString.substring(18, 22).toInt();

    if (shutter_speed <= 0 || shutter_speed > 35) {
      //error message to tell him that he must fill in a positive number below 35
      mySerial.write("ERROR : you must specify a shutter speed between 0.001 and 35 \n");
      OK_values = false;
    }
    if (time_between_pics <= ((int) 1.5 * shutter_speed)) {
      //error message to tell him that he must fill in a number at least equal to 1.5 times the shutter speed
      mySerial.write("ERROR : you must specify more time between pictures for your camera to process safely \n");
      OK_values = false;
    }
    if (pics_number <= 0) {
      //error message to tell him that he can't take 0 or less pictures
      mySerial.write("ERROR : you must take at least on picture \n");
      OK_values = false;
    }
  }
  if (OK_values) {

    float delay_duration = shutter_speed + time_between_pics;
    int seconds = (int)(delay_duration * pics_number);

    int s = (int)seconds % 60;
    int minutes = (seconds - s) / 60;
    int hours = (minutes - (minutes % 60)) / 60;

    mySerial.println("Take a photo every : " + String(delay_duration) + "s\n");
    mySerial.println("Total number of pictures : " + String(pics_number) + "\n");
    mySerial.println("Total time : " + String(hours) + "h" + (minutes % 60) + "m" + (s + 1) + "s\n");

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // take pictures while the number of pics taken does not exceed the number of pics filled in by the user //
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    while (pics_number > 0) {
      if (mySerial.available() != 0) {
        String inString = mySerial.readString();
        Serial.print("Message received : "); Serial.println(inString);

        inString.trim();

        if (inputString == "STOP") {
          pics_number = 0;
          mySerial.write("STOPPED !! \n");
        }
      } else {
        //digitalWrite(2, LOW);
        delay(250);
        //digitalWrite(2, HIGH);
        delay(delay_duration * 1000 - 250);

        Serial.print("i");
        pics_number--;
      }
    }
  }
}
