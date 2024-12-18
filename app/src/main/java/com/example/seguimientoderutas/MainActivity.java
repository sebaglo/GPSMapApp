package com.example.seguimientoderutas;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.gpsmapapp.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener {

    private EditText txtLatitud, txtLongitud;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FloatingActionButton fabZoomIn, fabZoomOut, fabTraffic, fabMapType,fabRouteHistory;
    private MaterialButton fabStartEndRoute;
    private boolean trafficEnabled = false;
    private boolean isRouteActive = false; // Estado de la ruta
    private LatLng originPoint, destinationPoint; // Puntos de origen y destino
    private static final String API_KEY = "AIzaSyDUFrCSei9dJ12vJCc-VrdTEjFrv8Ogags"; // Clave de la API

    @SuppressLint({"MissingInflatedId", "WrongViewCast"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialización de UI
        txtLatitud = findViewById(R.id.txtLatitud);
        txtLongitud = findViewById(R.id.txtLongitud);

        // Inicializar Floating Action Buttons
        fabZoomIn = findViewById(R.id.fab_zoom_in);
        fabZoomOut = findViewById(R.id.fab_zoom_out);
        fabTraffic = findViewById(R.id.fab_traffic);
        fabMapType = findViewById(R.id.fab_map_type);
        fabStartEndRoute = findViewById(R.id.btn_start_route); // Botón para iniciar/finalizar la ruta

        // Inicializar el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Configurar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        FloatingActionButton fabRouteHistory = findViewById(R.id.fab_route_history);
        fabRouteHistory.setOnClickListener(view -> {
            // Inicia la actividad para ver el historial de rutas
            Intent intent = new Intent(MainActivity.this, RouteHistoryActivity.class);
            startActivity(intent);
        });

        // Crear el callback para recibir actualizaciones de ubicación
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    updateLocationUI(location);
                }
            }
        };

        // Configurar los botones flotantes
        configureFloatingActionButtons();

        // Verificar si la ubicación está habilitada
        checkLocationSettings();
    }

    private void updateLocationUI(Location location) {
        if (location != null) {
            LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            txtLatitud.setText(String.valueOf(location.getLatitude()));
            txtLongitud.setText(String.valueOf(location.getLongitude()));

            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f));
            }
        }
    }

    private void configureFloatingActionButtons() {
        // Zoom In
        fabZoomIn.setOnClickListener(v -> {
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });

        // Zoom Out
        fabZoomOut.setOnClickListener(v -> {
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });

        // Toggle tráfico
        fabTraffic.setOnClickListener(v -> {
            if (mMap != null) {
                trafficEnabled = !trafficEnabled;
                mMap.setTrafficEnabled(trafficEnabled);
                Toast.makeText(MainActivity.this, "Tráfico: " + (trafficEnabled ? "Activado" : "Desactivado"), Toast.LENGTH_SHORT).show();
            }
        });

        // Cambiar tipo de mapa
        fabMapType.setOnClickListener(v -> {
            if (mMap != null) {
                switch (mMap.getMapType()) {
                    case GoogleMap.MAP_TYPE_NORMAL:
                        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        break;
                    case GoogleMap.MAP_TYPE_SATELLITE:
                        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                        break;
                    case GoogleMap.MAP_TYPE_TERRAIN:
                        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                        break;
                    case GoogleMap.MAP_TYPE_HYBRID:
                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        break;
                }
            }
        });

        // Iniciar o finalizar ruta
        fabStartEndRoute.setOnClickListener(v -> {
            if (!isRouteActive) {
                startRoute();
            } else {
                endRoute();
            }
        });
    }

    private void startRoute() {
        if (originPoint != null && destinationPoint != null) {
            isRouteActive = true;
            Toast.makeText(this, "Ruta iniciada", Toast.LENGTH_SHORT).show();
            getDirections();
        } else {
            Toast.makeText(this, "Selecciona origen y destino para iniciar la ruta", Toast.LENGTH_SHORT).show();
        }
    }

    private void endRoute() {
        isRouteActive = false;
        Toast.makeText(this, "Ruta finalizada", Toast.LENGTH_SHORT).show();
        if (mMap != null) {
            mMap.clear();
            originPoint = null;
            destinationPoint = null;
        }
    }

    private void getDirections() {
        if (originPoint != null && destinationPoint != null) {
            String origin = originPoint.latitude + "," + originPoint.longitude;
            String destination = destinationPoint.latitude + "," + destinationPoint.longitude;

            String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=" + origin +
                    "&destination=" + destination +
                    "&key=" + API_KEY;

            // Realizar la solicitud HTTP en un hilo separado
            new Thread(() -> {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();

                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    StringBuilder response = new StringBuilder();
                    int data = reader.read();
                    while (data != -1) {
                        response.append((char) data);
                        data = reader.read();
                    }

                    // Parsear la respuesta JSON
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray routes = jsonResponse.getJSONArray("routes");
                    JSONObject route = routes.getJSONObject(0);
                    JSONArray legs = route.getJSONArray("legs");
                    JSONObject leg = legs.getJSONObject(0);
                    JSONArray steps = leg.getJSONArray("steps");

                    // Obtener distancia y duración total de la ruta
                    String totalDistance = leg.getJSONObject("distance").getString("text");
                    String totalDuration = leg.getJSONObject("duration").getString("text");

                    // Guardar en Firebase
                    saveRouteToFirebase(originPoint, destinationPoint, totalDistance, totalDuration);

                    PolylineOptions polylineOptions = new PolylineOptions();
                    for (int i = 0; i < steps.length(); i++) {
                        JSONObject step = steps.getJSONObject(i);
                        String polyline = step.getJSONObject("polyline").getString("points");
                        polylineOptions.addAll(decodePoly(polyline));
                    }

                    // Dibujar la ruta en el mapa en el hilo principal
                    runOnUiThread(() -> {
                        if (mMap != null) {
                            mMap.addPolyline(polylineOptions);
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void saveRouteToFirebase(LatLng origin, LatLng destination, String distance, String duration) {
        // Crear un objeto de ruta con la información relevante
        RouteData routeData = new RouteData(origin.latitude, origin.longitude,
                destination.latitude, destination.longitude,
                distance, duration);

        // Obtener una referencia a Firebase
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Guardar los datos de la ruta
        db.collection("routes")
                .add(routeData)
                .addOnSuccessListener(documentReference -> Log.d("Firebase", "Ruta guardada"))
                .addOnFailureListener(e -> Log.w("Firebase", "Error al guardar la ruta", e));
    }


    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> polyline = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dLat = (result & 0x1) != 0 ? ~(result >> 1) : (result >> 1);
            lat += dLat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dLng = (result & 0x1) != 0 ? ~(result >> 1) : (result >> 1);
            lng += dLng;

            polyline.add(new LatLng((lat / 1E5), (lng / 1E5)));
        }

        return polyline;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (isLocationEnabled()) {
                mMap.setMyLocationEnabled(true);
                getCurrentLocation();  // Llamada para obtener la ubicación actual
            } else {
                Toast.makeText(this, "Por favor, habilita la ubicación", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // Esto coloca una ubicación predeterminada (Ovalle) en el mapa al principio
        LatLng ovalle = new LatLng(-30.6039, -71.1999);
        mMap.addMarker(new MarkerOptions().position(ovalle).title("Ovalle"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ovalle, 15f));
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(createLocationRequest(), locationCallback, null);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }


    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000); // Actualizaciones cada 10 segundos
        locationRequest.setFastestInterval(5000); // La más rápida cada 5 segundos
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // Prioridad alta para obtener precisión
        return locationRequest;
    }

    private void checkLocationSettings() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            // Si la ubicación no está habilitada, mostrar un mensaje para activar
            Toast.makeText(this, "Por favor, habilita la ubicación en el dispositivo", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        } else {
            // Si la ubicación está habilitada, obtener la ubicación
            getCurrentLocation();
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        txtLatitud.setText(String.valueOf(latLng.latitude));
        txtLongitud.setText(String.valueOf(latLng.longitude));
        mMap.addMarker(new MarkerOptions().position(latLng).title("Marcador Añadido"));
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        if (originPoint == null) {
            originPoint = latLng;
            mMap.addMarker(new MarkerOptions().position(originPoint).title("Origen"));
            Toast.makeText(this, "Origen seleccionado", Toast.LENGTH_SHORT).show();
        } else if (destinationPoint == null) {
            destinationPoint = latLng;
            mMap.addMarker(new MarkerOptions().position(destinationPoint).title("Destino"));
            Toast.makeText(this, "Destino seleccionado", Toast.LENGTH_SHORT).show();

            // Iniciar la ruta si ambos puntos están seleccionados
            if (originPoint != null && destinationPoint != null) {
                startRoute();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Permisos de ubicación no concedidos", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
