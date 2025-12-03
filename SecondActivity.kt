package com.example.alarmaseguridad

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.alarmaseguridad.databinding.ActivitySecondBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket

class SecondActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySecondBinding
    private var socket: Socket? = null
    private var outputWriter: PrintWriter? = null
    private var isReceiving = false
    private var estadoActual = EstadoVentana.CERRADA

    enum class EstadoVentana {
        CERRADA,
        ABIERTA
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecondBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ip = intent.getStringExtra("IP") ?: ""
        val port = intent.getIntExtra("PORT", 80)

        // Validar IP y puerto
        if (ip.isEmpty()) {
            Toast.makeText(this, "Error: IP no válida", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.tvIp.text = "Conectado a: $ip:$port"
        
        // Estado inicial: Ventana cerrada en verde
        actualizarEstado(EstadoVentana.CERRADA)
        binding.tvMensaje.visibility = View.GONE

        // Configurar botón Reset
        binding.btnReset.setOnClickListener {
            enviarTramaRst()
        }

        // Establecer conexión y comenzar a recibir datos
        connectAndReceive(ip, port)
    }

    private fun connectAndReceive(ip: String, port: Int) {
        isReceiving = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Establecer timeout de conexión (5 segundos)
                socket = Socket()
                socket?.soTimeout = 5000
                socket?.connect(java.net.InetSocketAddress(ip, port), 5000)
                val inputStream = socket?.getInputStream()
                val reader = BufferedReader(InputStreamReader(inputStream))
                
                // Configurar escritor para enviar datos
                val outputStream = socket?.getOutputStream()
                outputWriter = PrintWriter(OutputStreamWriter(outputStream), true)

                withContext(Dispatchers.Main) {
                    binding.btnReset.isEnabled = true
                }

                // Leer datos continuamente
                while (isReceiving && socket?.isConnected == true) {
                    try {
                        val line = reader.readLine()?.trim()
                        if (line != null) {
                            procesarTrama(line)
                        } else {
                            // Si readLine retorna null, la conexión se cerró
                            break
                        }
                    } catch (e: Exception) {
                        if (isReceiving) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@SecondActivity,
                                    "Error leyendo datos: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    actualizarEstado(EstadoVentana.CERRADA)
                    binding.tvMensaje.text = "Error de conexión: ${e.message}"
                    binding.tvMensaje.setTextColor(0xFFFF0000.toInt())
                    binding.tvMensaje.visibility = View.VISIBLE
                    Toast.makeText(
                        this@SecondActivity,
                        "Error al conectar: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isReceiving = false
        try {
            socket?.close()
        } catch (e: Exception) {
            // Ignorar errores al cerrar
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        isReceiving = false
        try {
            socket?.close()
        } catch (e: Exception) {
            // Ignorar errores al cerrar
        }
    }

    private suspend fun procesarTrama(trama: String) {
        withContext(Dispatchers.Main) {
            when (trama) {
                "0" -> {
                    actualizarEstado(EstadoVentana.CERRADA)
                }
                "1" -> {
                    actualizarEstado(EstadoVentana.ABIERTA)
                }
            }
        }
    }

    private fun actualizarEstado(estado: EstadoVentana) {
        estadoActual = estado
        when (estado) {
            EstadoVentana.CERRADA -> {
                binding.tvEstadoVentana.text = "Ventana cerrada"
                binding.tvEstadoVentana.setTextColor(0xFF4CAF50.toInt())
                binding.cardEstado.setCardBackgroundColor(0xFFE8F5E9.toInt())
            }
            EstadoVentana.ABIERTA -> {
                binding.tvEstadoVentana.text = "Ventana ha sido abierta"
                binding.tvEstadoVentana.setTextColor(0xFFF44336.toInt())
                binding.cardEstado.setCardBackgroundColor(0xFFFFEBEE.toInt())
            }
        }
    }

    private fun enviarTramaRst() {
        if (socket == null || socket?.isConnected != true) {
            Toast.makeText(this, "No hay conexión establecida", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                outputWriter?.println("RST")
                outputWriter?.flush()
                
                // Cambiar inmediatamente a "Ventana cerrada" según los requisitos
                withContext(Dispatchers.Main) {
                    actualizarEstado(EstadoVentana.CERRADA)
                    binding.tvMensaje.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SecondActivity,
                        "Error al enviar: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}

