// Tinkercad no tiene reed switch y se sustituye por un botón
const int ledPin = 2;
const int reedPin = 4; // Botón pulsado
int lastState = HIGH;

void setup() {
  Serial.begin(9600); // Inicializar comunicación serial
  pinMode(reedPin, INPUT); // Pin 4 es entrada
  pinMode(ledPin, OUTPUT); // Pin 2 es salida
}

void loop() {
  int state = digitalRead(reedPin);
  
  if (state != lastState) {
    if (state == HIGH) {
      Serial.println("VENTANA CERRADA");
      digitalWrite(ledPin, HIGH);
    } else {
      Serial.println("VENTANA ABIERTA");
      digitalWrite(ledPin, LOW);
    }
    lastState = state; // Actualizar el último estado
  }
}
