#include <SoftwareSerial.h>

SoftwareSerial mySerial(6, 5); // RX, TX for Bluetooth
int interval, start_delay, pics_number, shutter_speed;
bool bulb_mode;
const int CAMERA = 2, MILLISECS = 1000;

// This number is used to set the delay to the exact number it should be when set by the user. In reality, the delay is increased by the time used by the program to execute code.
// Check the millis() method to see the time elapsed in the loop by comparing the values.
// The offset is set to 26 ms, it's not dynamically set yet
const int TIMER_OFFSET = 26;

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

  if (inputString == "STATUS" || inputString == "STOP")
    mySerial.println("WAITING");
  else if (inputString == "TAKE_SINGLE_SHOT") {
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

    if (interval <= 0 || start_delay < 0 || pics_number <= 0) {
      Serial.println("Values sent by the user are not ok.");
      OK_values = false;
    }
  }
  if (OK_values) {
    int seconds = interval * pics_number + start_delay;

    int s = (int)seconds % 60;
    int minutes = (seconds - s) / 60;
    int hours = (minutes - (minutes % 60)) / 60;

    mySerial.println("RUNNING:" + String(seconds) + ":" + String(pics_number) + ":0");
    Serial.println("Device will take a photo every : " + String(interval)  + "\n  > Total number of pictures : " + String(pics_number) + "\n  > Total time : " + String(hours) + "h" + String(minutes % 60) + "m" + String(s)
                   + "s" + "\n  > Shutter_speed = " + String(shutter_speed) + "ms");

    for (int i = 0; i < start_delay; i++) {

      delay(MILLISECS);
      if (getBluetoothStopCommand(0))return;
    }
    takePictures(0);
    mySerial.println("WAITING");
    Serial.println("END of shooting");
  }
}

/*
   function that calls recursively to take one picture at a time
   takes the number of pictures already taken in parameters
*/
void takePictures(int taken_pics) {

  Serial.println(millis());
  mySerial.println("RUNNING:" + String(interval * pics_number) + ":" + String(pics_number) + ":" + String(taken_pics));

  if (getBluetoothStopCommand(taken_pics)) return;

  if (pics_number > 0) {
    digitalWrite(CAMERA, LOW);

    if (bulb_mode && shutter_speed > 5) {

      for (int i = 0; i < shutter_speed; i++) {

        delay(MILLISECS);

        if (getBluetoothStopCommand(taken_pics)) return;
      }
    } else
      delay(shutter_speed);

    digitalWrite(CAMERA, HIGH);
    delay(interval * MILLISECS - shutter_speed - TIMER_OFFSET);

    taken_pics++;
    pics_number--;

    takePictures(taken_pics);
  }
}

/*
   method used to get and process messages received from the android app while running
   called periodically to check if a new stop command has been received
   if the command is "status", sends the running status to the android app
*/
bool getBluetoothStopCommand(int taken_pics) {
  if (mySerial.available() != 0) {
    String inString = mySerial.readString();
    inString.trim();
    Serial.println("Message received : "); Serial.println(inString);

    if (inString == "STATUS")
      mySerial.println("RUNNING:" + String(interval * pics_number) + ":" + String(pics_number) + ":" + String(taken_pics));
    if (inString == "STOP")
      return true;
    return false;
  }
}
