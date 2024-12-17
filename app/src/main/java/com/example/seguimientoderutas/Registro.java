package com.example.seguimientoderutas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class Registro extends AppCompatActivity {

    private EditText etNombre, etEmailRegistro, etContrasenaRegistro;
    private Button btnRegistro;
    private TextView tvLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrarse);

        // Vincular elementos de la interfaz
        etNombre = findViewById(R.id.etNombre);
        etEmailRegistro = findViewById(R.id.etEmailRegistro);
        etContrasenaRegistro = findViewById(R.id.etContrasenaRegistro);
        btnRegistro = findViewById(R.id.btnRegistro);
        tvLogin = findViewById(R.id.tvLogin);

        // Inicializar Firebase Auth y Firestore
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        // Configurar acciones del botón de registro
        btnRegistro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nombre = etNombre.getText().toString().trim();
                String email = etEmailRegistro.getText().toString().trim();
                String contrasena = etContrasenaRegistro.getText().toString().trim();

                if (nombre.isEmpty() || email.isEmpty() || contrasena.isEmpty()) {
                    Toast.makeText(Registro.this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
                } else {
                    // Verificar si el correo ya está registrado
                    mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (!task.getResult().getSignInMethods().isEmpty()) {
                                // El correo electrónico ya está registrado
                                Toast.makeText(Registro.this, "Este correo electrónico ya está en uso. Por favor, usa otro.", Toast.LENGTH_SHORT).show();
                            } else {
                                // Crear un nuevo usuario en Firebase Authentication
                                mAuth.createUserWithEmailAndPassword(email, contrasena)
                                        .addOnCompleteListener(Registro.this, task1 -> {
                                            if (task1.isSuccessful()) {
                                                // Usuario registrado correctamente, ahora guardar en Firestore
                                                String userId = mAuth.getCurrentUser().getUid();

                                                // Crear el objeto User
                                                Users user = new Users(nombre, email);

                                                // Guardar en Firestore
                                                mFirestore.collection("users")
                                                        .document(userId) // Usamos el UID como identificador único
                                                        .set(user) // Guardamos el objeto User
                                                        .addOnCompleteListener(task2 -> {
                                                            if (task2.isSuccessful()) {
                                                                // Mostrar mensaje de éxito
                                                                Toast.makeText(Registro.this, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show();
                                                                // Redirigir al Login después de registrarse
                                                                Intent intent = new Intent(Registro.this, Login.class);
                                                                startActivity(intent);
                                                                finish();
                                                            } else {
                                                                // Error al guardar en Firestore
                                                                Toast.makeText(Registro.this, "Error al guardar los datos del usuario", Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                            } else {
                                                // Error al registrar el usuario
                                                Toast.makeText(Registro.this, "Error al registrar el usuario: " + task1.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            // Error al verificar el correo
                            Toast.makeText(Registro.this, "Error al verificar el correo electrónico: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        // Configurar acción para volver a la pantalla de inicio de sesión
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Registro.this, Login.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
