#include <SoftwareSerial.h>

SoftwareSerial mySerial(6, 5); // RX, TX for Bluetooth
int interval, start_delay, pics_number, shutter_speed;
bool bulb_mode;
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

  while (mySerial.available() == 0);

  String inputString = mySerial.readString();
  Serial.println("\r\n Message received : "); Serial.println(inputString);

  inputString.trim();

  if (inputString == "STATUS") {
    mySerial.write("WAITING\n");
  } else if (inputString == "TAKE_SINGLE_SHOT") {
    // take a single picture

    Serial.println("take single shot ");
    digitalWrite(CAMERA, LOW);
    delay(shutter_speed);
    digitalWrite(CAMERA, HIGH);

  } else if (inputString.substring(0, 3) == "RUN") {

    interval = inputString.substring(4, 9).toInt();
    start_delay = inputString.substring(10, 15).toInt();
    pics_number = inputString.substring(16, 20).toInt();
    bulb_mode = inputString.substring(21, 22).toInt();

    if (bulb_mode)
      shutter_speed = inputString.substring(22, 27).toInt();
    else
      shutter_speed = 250;

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

    mySerial.println("RUNNING|" + String(seconds) + ":" + String(pics_number));
    Serial.println("RUNNING|Take a photo every : " + String(interval)  + "s:Total number of pictures : " + String(pics_number) + ":Total time : " + String(hours) + "h" + String(minutes % 60) + "m" + String(s)
                   + "s AND shutter_speed = " + String(shutter_speed) + "ms\r\n");

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
    mySerial.println("STOP\n");
    mySerial.println("WAITING\n");
    Serial.println("END of shooting");
  }
}

/**
   function that calls recursively ta take one picture at a time
   takes the number of pictures already taken in parameters
*/
void takePictures(int taken_pics) {

  mySerial.println("RUNNING|" + String(interval * remaining_pics) + ":" + String(remaining_pics) + ":" + String(taken_pics));

  if (mySerial.available() != 0) {
    String inString = mySerial.readString();
    inString.trim();
    Serial.println("Message received while running : "); Serial.println(inString);

    if (inString == "STOP")
      return;
  }

  if (taken_pics < pics_number ) {
    digitalWrite(CAMERA, LOW);

    if (bulb_mode && shutter_speed > 5) {

      for (int i = 0; i < shutter_speed; i++) {

        delay(MILLISECS);

        if (mySerial.available() != 0) {
          String inString = mySerial.readString();
          inString.trim();
          Serial.println("Message received while running : "); Serial.println(inString);

          if (inString == "STOP") {
            digitalWrite(CAMERA, HIGH); // stop the photo
            return;
          }
        }
      }
    } else
      delay(shutter_speed);

    digitalWrite(CAMERA, HIGH);
    delay(interval * MILLISECS - shutter_speed);

    taken_pics++;

    int remaining_pics = pics_number - taken_pics;

    takePictures(taken_pics);
  }
}
