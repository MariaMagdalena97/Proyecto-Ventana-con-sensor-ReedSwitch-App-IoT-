package com.example.alarmaseguridad

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.alarmaseguridad.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Socket

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnConectar.setOnClickListener {
            val ip = binding.etIp.text.toString().trim()
            val portText = binding.etPort.text.toString().trim()
            
            if (ip.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese una IP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar formato básico de IP
            if (!isValidIp(ip)) {
                Toast.makeText(this, "IP no válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val port = if (portText.isEmpty()) {
                80
            } else {
                portText.toIntOrNull() ?: run {
                    Toast.makeText(this, "Puerto no válido", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            if (port < 1 || port > 65535) {
                Toast.makeText(this, "Puerto debe estar entre 1 y 65535", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnConectar.isEnabled = false
            binding.btnConectar.text = "Conectando..."

            // Establecer conexión en un hilo de fondo
            connectToESP32(ip, port)
        }
    }

    private fun isValidIp(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            val num = part.toIntOrNull()
            num != null && num in 0..255
        }
    }

    private fun connectToESP32(ip: String, port: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Establecer timeout de conexión (5 segundos)
                val socket = Socket()
                socket.soTimeout = 5000
                socket.connect(java.net.InetSocketAddress(ip, port), 5000)
                
                // Si la conexión es exitosa, pasar el socket a la segunda activity
                runOnUiThread {
                    binding.btnConectar.isEnabled = true
                    binding.btnConectar.text = "Establecer Conexión"
                    
                    val intent = Intent(this@MainActivity, SecondActivity::class.java)
                    intent.putExtra("IP", ip)
                    intent.putExtra("PORT", port)
                    startActivity(intent)
                }
                
                // Cerrar el socket aquí ya que lo abriremos de nuevo en SecondActivity
                socket.close()
                
            } catch (e: Exception) {
                runOnUiThread {
                    binding.btnConectar.isEnabled = true
                    binding.btnConectar.text = "Establecer Conexión"
                    Toast.makeText(
                        this@MainActivity,
                        "Error al conectar: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}

