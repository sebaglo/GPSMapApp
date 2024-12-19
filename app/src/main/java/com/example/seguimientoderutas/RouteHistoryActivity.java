package com.example.seguimientoderutas;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gpsmapapp.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RouteHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RouteHistoryAdapter adapter;
    private FirebaseFirestore db;
    private List<String> routeHistory; // Lista de rutas

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_history);

        // Inicializar RecyclerView y Firestore
        recyclerView = findViewById(R.id.routeHistoryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        db = FirebaseFirestore.getInstance();
        routeHistory = new ArrayList<>();

        // Cargar historial desde Firestore
        loadRouteHistory();
    }

    private void loadRouteHistory() {
        // Referencia a la colección de rutas en Firestore
        CollectionReference routesRef = db.collection("rutas");

        routesRef.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Obtén los datos de la ruta
                            String routeInfo = document.getString("informacionRuta"); // Cambia "informacionRuta" al campo que tengas
                            if (routeInfo != null) {
                                routeHistory.add(routeInfo); // Agrega la ruta a la lista
                            }
                        }

                        // Configurar el adaptador con los datos obtenidos
                        adapter = new RouteHistoryAdapter(routeHistory);
                        recyclerView.setAdapter(adapter);
                    } else {
                        Log.e("FirestoreError", "Error al obtener los datos", task.getException());
                        Toast.makeText(this, "Error al cargar el historial", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
