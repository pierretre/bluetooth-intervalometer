#include <SoftwareSerial.h>

#define TxD 4

#define RxD 5

SoftwareSerial mySerial(6, 5); // RX, TX for Bluetooth
String inputString = ""; 


int TIME_BETWEEN_PICS;
int SHUTTER_SPEED;
int PICS_NUMBER;
int PICS_TAKEN;
String TOTAL_TIME;
int DELAY;
boolean values_set = true;
int CAMERA = 2;

void setup() {
  // put your setup code here, to run once:
  //pinMode(CAMERA, OUTPUT);
  //avoid unecessary shot
  //digitalWrite(2, HIGH);
  Serial.begin(9600);
  mySerial.begin(9600);
}

void loop() {
  if (mySerial.available()) {
    while (mySerial.available()) {
      char inChar = (char)mySerial.read(); //Lire l'entrée
      inputString += inChar; //Construit une chaine de caractére a partir des caractére reçus
    } 
    mySerial.println("salam");
    Serial.println(inputString);
  }


  int picsNb = 0;
  int shutterSp = 0;
  int timeGap = 0;

  if (picsNb > 0 && shutterSp > 0 && timeGap > ((int) 1.5 * shutterSp)) {

    PICS_NUMBER = picsNb;
    PICS_TAKEN = 0;
    SHUTTER_SPEED = shutterSp;
    TIME_BETWEEN_PICS = timeGap;

    DELAY = SHUTTER_SPEED + TIME_BETWEEN_PICS;
    float seconds = (int)(DELAY * PICS_NUMBER) / 1000;

    Serial.println(String(seconds));
    int s = (int)seconds % 60;
    Serial.println(String(s));
    int minutes = (seconds - s) / 60;
    Serial.println(String(minutes));
    int hours = (minutes - (minutes % 60)) / 60;

    TOTAL_TIME = "" + String(hours) + "h" + (minutes % 60) + "m" + (s + 1) + "s";

    Serial.println("Take a photo every : " + String(DELAY));
    Serial.println("Total number of pictures : " + String(PICS_NUMBER));
    Serial.println("Total time : " + TOTAL_TIME);

  } else {
    //send a message to the user to tell him to put correct values

    if (picsNb <= 0) {
      //error message to tell him that he can't take 0 or less pictures
    }
    if (shutterSp <= 0 || shutterSp > 30) {
      //error message to tell him that he must fill in a positive number below 30
    }
    if (timeGap <= ((int) 1.5 * shutterSp)) {
      //error message to tell him that he must fill in a number at least equal to 1.5 times the shutter speed
    }
  }


  ///////////////////////////////////////////////////////////////////////////////////////////////////////////
  // take pictures while the number of pics taken does not exceed the number of pics filled in by the user //
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  while (PICS_TAKEN < PICS_NUMBER) {
    digitalWrite(2, LOW);
    delay(250);
    digitalWrite(2, HIGH);
    delay(DELAY - 250);

    PICS_TAKEN++;
  }
}
