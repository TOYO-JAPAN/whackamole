#include <ArduinoBLE.h>
#include <Servo.h>

const int ledPin = LED_BUILTIN; // Set ledPin to on-board LED
const int angleMin = 10;
const int angleMax = 100;
Servo servoA;
Servo servoB;
Servo servoC;

BLEService wamService("19B10010-E8F2-537E-4F6C-D104768A1214");

BLEByteCharacteristic ledCharacteristic("19B10011-E8F2-537E-4F6C-D104768A1214", BLERead | BLEWrite);
BLEByteCharacteristic buttonCharacteristicA("19B10012-E8F2-537E-4F6C-D104768A1214", BLERead | BLEWrite);
BLEByteCharacteristic buttonCharacteristicB("19B10012-E8F2-537E-4F6C-D104768A1214", BLERead | BLEWrite);
BLEByteCharacteristic buttonCharacteristicC("19B10012-E8F2-537E-4F6C-D104768A1214", BLERead | BLEWrite);

void setup() {
  Serial.begin(9600);

  pinMode(ledPin, OUTPUT); // use the LED as an output

  // Initialization
  if (!BLE.begin()) {
    Serial.println("Starting BLE failed!");
    while (1);
  }

  // Set the local name peripheral advertises
  BLE.setLocalName("Whack A Mole");
  
  // Set the UUID for the service this peripheral advertises:
  BLE.setAdvertisedService(wamService);

  // Add the characteristics to the service
  wamService.addCharacteristic(ledCharacteristic);
  wamService.addCharacteristic(buttonCharacteristicA);
  wamService.addCharacteristic(buttonCharacteristicB);
  wamService.addCharacteristic(buttonCharacteristicC);

  // Add the service
  BLE.addService(wamService);

  ledCharacteristic.writeValue(0);
  buttonCharacteristicA.writeValue(0);
  buttonCharacteristicB.writeValue(0);
  buttonCharacteristicC.writeValue(0);

  // Start advertising
  BLE.advertise();

  // Set servos
  servoA.attach(5);
  servoB.attach(6);
  servoC.attach(7);
  servoA.write(angleMin);
  servoB.write(angleMin);
  servoC.write(angleMin);
  Serial.println("Bluetooth device active, waiting for connections...");
}

void loop() {
  // Poll for BLE events
  BLE.poll();

  if (ledCharacteristic.written()) {
    if (ledCharacteristic.value()) {
      Serial.println("LED on");
      digitalWrite(ledPin, HIGH);
    } else {
      Serial.println("LED off");
      digitalWrite(ledPin, LOW);
    }
  }
  
  if (buttonCharacteristicA.written()) {
    if (buttonCharacteristicA.value()) {
      Serial.println("ServoA on");
      digitalWrite(ledPin, HIGH);
      servoA.write(angleMax);
    } else {
      Serial.println("ServoA off");
      digitalWrite(ledPin, LOW);
      servoA.write(angleMin);
    }
  }
  
  if (buttonCharacteristicB.written()) {
    if (buttonCharacteristicB.value()) {
      Serial.println("ServoB on");
      digitalWrite(ledPin, HIGH);
      servoB.write(angleMax);
    } else {
      Serial.println("ServoB off");
      digitalWrite(ledPin, LOW);
      servoB.write(angleMin);
    }
  }
  
  if (buttonCharacteristicC.written()) {
    if (buttonCharacteristicC.value()) {
      Serial.println("ServoC on");
      digitalWrite(ledPin, HIGH);
      servoC.write(angleMax);
    } else {
      Serial.println("ServoC off");
      digitalWrite(ledPin, LOW);
      servoC.write(angleMin);
    }
  }
}
