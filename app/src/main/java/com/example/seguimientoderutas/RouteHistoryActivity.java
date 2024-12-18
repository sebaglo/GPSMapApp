package com.example.seguimientoderutas;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gpsmapapp.R;

import java.util.List;

public class RouteHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RouteHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_history);

        recyclerView = findViewById(R.id.routeHistoryRecyclerView);

        // Obtener historial de rutas
        List<String> routeHistory = Route.getRouteHistory(this);

        // Verificar si el historial de rutas no está vacío
        if (routeHistory.isEmpty()) {
            Log.d("RouteHistory", "No hay rutas guardadas.");
        } else {
            Log.d("RouteHistory", "Rutas recuperadas: " + routeHistory);
        }

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RouteHistoryAdapter(routeHistory);
        recyclerView.setAdapter(adapter);
    }
}
