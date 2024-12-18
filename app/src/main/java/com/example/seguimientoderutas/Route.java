package com.example.seguimientoderutas;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class Route {

    private static final String PREFS_NAME = "RouteHistoryPrefs";
    private static final String ROUTES_KEY = "routes";

    // Método para guardar una nueva ruta
    public static void saveRoute(Context context, String route) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Obtener rutas actuales y agregar la nueva
        List<String> routes = getRouteHistory(context);
        routes.add(route);

        // Guardar la lista de rutas en SharedPreferences
        editor.putString(ROUTES_KEY, String.join(",", routes));
        editor.apply();

        // Log para verificar si la ruta se ha guardado
        Log.d("Route", "Ruta guardada: " + route);
        Log.d("Route", "Historial de rutas: " + String.join(",", routes));
    }

    // Método para obtener el historial de rutas
    public static List<String> getRouteHistory(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String routesString = sharedPreferences.getString(ROUTES_KEY, "");
        List<String> routes = new ArrayList<>();

        if (!routesString.isEmpty()) {
            // Convertir la cadena separada por comas en una lista de rutas
            String[] routesArray = routesString.split(",");
            for (String route : routesArray) {
                routes.add(route);
            }
        }

        // Log para verificar que se están recuperando las rutas correctamente
        Log.d("Route", "Historial de rutas recuperado: " + routes);

        return routes;
    }
}
