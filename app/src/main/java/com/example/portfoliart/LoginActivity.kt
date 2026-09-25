package com.example.portfoliart

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- ESTILO VISUAL (Barra de estado limpia) ---
        window.statusBarColor = android.graphics.Color.parseColor("#F5F5F5")
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = true

        setContentView(R.layout.activity_login)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val inputUser = findViewById<EditText>(R.id.etUsuario)
        val inputPass = findViewById<EditText>(R.id.etPassword)

        val dbHelper = DatabaseHelper(this)

        btnLogin.setOnClickListener {
            val user = inputUser.text.toString()
            val pass = inputPass.text.toString()

            if (user.isNotEmpty() && pass.isNotEmpty()) {
                // 1. Ciframos la contraseña para comparar
                val passCifrada = cifrarPassword(pass)

                // 2. Intentamos obtener el ID del usuario
                val userId = dbHelper.obtenerIdUsuario(user, passCifrada)

                if (userId != -1) {
                    // 3. ¡EXITO! Guardamos el ID en SharedPreferences para que toda la app sepa quién es
                    val prefs = getSharedPreferences("PortfoliArtPrefs", Context.MODE_PRIVATE)
                    prefs.edit().putInt("USUARIO_ID", userId).apply()

                    // 4. Saltamos a la Main
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        btnRegistrar.setOnClickListener {
            val user = inputUser.text.toString()
            val pass = inputPass.text.toString()

            if (user.isNotEmpty() && pass.isNotEmpty()) {
                val passCifrada = cifrarPassword(pass)
                dbHelper.registrarUsuario(user, passCifrada)
                Toast.makeText(this, "Artista registrado. ¡Ya puedes entrar!", Toast.LENGTH_SHORT).show()

                // Limpiamos campos para que el usuario entre
                inputPass.setText("")
            } else {
                Toast.makeText(this, "Rellena datos para el registro", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función de cifrado
    private fun cifrarPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}