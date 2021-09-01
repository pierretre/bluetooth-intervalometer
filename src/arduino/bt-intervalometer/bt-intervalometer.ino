#include <SoftwareSerial.h>

SoftwareSerial mySerial(6, 5); // RX, TX for Bluetooth
int interval, start_delay, pics_number, bulb_shutter_speed;
const int CAMERA = 2, MILLISECS = 1000;

void setup() {
  pinMode(CAMERA, OUTPUT);
  digitalWrite(CAMERA, HIGH);
  Serial.begin(9600);
  mySerial.begin(9600);
  mySerial.setTimeout(10);
}

void loop() {
  boolean OK_values = false;
  //interval = 0;
  //start_delay = 0;
  //pics_number = 0;

  while (mySerial.available() == 0);

  String inputString = mySerial.readString();
  Serial.println("\r\n Message received : "); Serial.println(inputString);

  inputString.trim();

  if (inputString == "STATUS") {
    mySerial.write("STATUS|WAITING\n");
  } else if (inputString == "TAKE_SINGLE_SHOT") {
    // take a single picture

    Serial.println("take single shot ");
    digitalWrite(CAMERA, LOW);
    delay(bulb_shutter_speed);
    digitalWrite(CAMERA, HIGH);

  } else if (inputString.substring(0, 3) == "RUN") {

    interval = inputString.substring(4, 9).toInt();
    start_delay = inputString.substring(10, 15).toInt();
    pics_number = inputString.substring(16, 20).toInt();
    bulb_shutter_speed = inputString.substring(21, 26).toInt();

    if (bulb_shutter_speed == 0) bulb_shutter_speed = 250;

    OK_values = true;

    if (interval <= 0) {
      Serial.println("STATUS|WAITING|You must specify an interval of 1 sec at least");
      OK_values = false;
    }
    if (start_delay < 0) {
      Serial.println("STATUS|WAITING|You must specify a timer delay of 1 sec at least");
      OK_values = false;
    }
    if (pics_number <= 0) {
      Serial.println("STATUS|WAITING|You must take at least on picture");
      OK_values = false;
    }
  }
  if (OK_values) {
    int seconds = interval * pics_number + start_delay;

    int s = (int)seconds % 60;
    int minutes = (seconds - s) / 60;
    int hours = (minutes - (minutes % 60)) / 60;

    mySerial.println("STATUS|RUNNING|" + String(seconds) + ":" + String(pics_number));
    Serial.println("STATUS|RUNNING|Take a photo every : " + String(interval)  + "s:Total number of pictures : " + String(pics_number) + ":Total time : " + String(hours) + "h" + String(minutes % 60) + "m" + String(s)
                   + "s AND bulb_shutter_speed = " + String(bulb_shutter_speed) + "ms\r\n");

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // take pictures while the number of pics taken does not exceed the number of pics filled in by the user
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////


    for (int i = 0; i < start_delay; i++) {

      delay(MILLISECS);

      if (mySerial.available() != 0) {
        String inString = mySerial.readString();
        inString.trim();
        Serial.println("Message received while running : "); Serial.println(inString);

        if (inString == "STOP")
          return;
      }
    }
    
    takePictures(0);
    mySerial.println("STATUS|WAITING");
    Serial.println("END of shooting");
  }
}

void takePictures(int taken_pics) {

  if (mySerial.available() != 0) {
    String inString = mySerial.readString();
    inString.trim();
    Serial.println("Message received while running : "); Serial.println(inString);

    if (inString == "STOP")
      return;
  }

  if (taken_pics < pics_number ) {
    digitalWrite(2, LOW);
    delay(bulb_shutter_speed);
    digitalWrite(2, HIGH);
    delay(interval * MILLISECS - bulb_shutter_speed);

    taken_pics++;

    int remaining_pics = pics_number - taken_pics;

    mySerial.println("STATUS|RUNNING|" + String(interval * remaining_pics) + ":" + String(remaining_pics) + ":" + String(taken_pics));

    takePictures(taken_pics);
  }
}
