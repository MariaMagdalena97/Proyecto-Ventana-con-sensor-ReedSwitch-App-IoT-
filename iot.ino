#include <WiFi.h>
#include <WiFiClient.h>
#include <WiFiServer.h>

// --- Configuración de WiFi ---
const char* ssid = "Zflip5";
const char* password = "49375639";

//configuracion de los pines del esp32
int led = 1;        
int sensor = 10;   
int estado_alarma = 0;
int activacion_alarma = 0;

// --- Configuración del Servidor ---
const int serverPort = 8080;
WiFiServer server(serverPort);
WiFiClient client;

void setup() {
  Serial.begin(115200);
  pinMode(sensor, INPUT);
  pinMode(led, OUTPUT);

  Serial.print("Conectando a ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.print(".");
  }

  Serial.println("\nWiFi conectado!");
  Serial.print("IP del Servidor: ");
  Serial.println(WiFi.localIP());

  server.begin();
  Serial.print("Servidor iniciado en el puerto: ");
  Serial.println(serverPort);
}

void loop() {

  // Si no hay cliente conectado, espera uno nuevo
  if (!client.connected()) {
    WiFiClient newClient = server.available();
    if (newClient) {
      client = newClient;
      Serial.print("Nuevo cliente conectado desde IP: ");
      Serial.println(client.remoteIP());
    }
  }

  estado_alarma = digitalRead(sensor);


  if (estado_alarma == 1) {
    activacion_alarma = 1;
  }

  if (client.connected()) {

    while (client.available()) {
      String receivedData = client.readStringUntil('\n');
      receivedData.trim();

      Serial.print("Trama recibida: ");
      Serial.println(receivedData);

      if (receivedData == "RST") {
        activacion_alarma = 0;
      }

      client.print("Trama recibida OK\n");
    }

    static unsigned long lastSendTime = 0;

    if (millis() - lastSendTime > 1000) {
      String tramaEnvio = String(activacion_alarma) + "\n";  
      client.print(tramaEnvio);
      Serial.print("Enviando trama a cliente: ");
      Serial.println(tramaEnvio);
      lastSendTime = millis();
    }
  }

  if (activacion_alarma == 1) {
    digitalWrite(led, HIGH);
  } else {
    digitalWrite(led, LOW);
  }

  delay(5);
}
