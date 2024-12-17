package com.example.gpsmapapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener {

    private EditText txtLatitud, txtLongitud;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Marker userLocationMarker;
    private FloatingActionButton fabZoomIn, fabZoomOut, fabTraffic, fabMapType;
    private boolean trafficEnabled = false;
    private FirebaseFirestore db;

    @SuppressLint("MissingInflatedId")
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

        // Inicializar el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Inicializar Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Configurar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Crear el callback para recibir actualizaciones de ubicación
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                // Obtener la última ubicación y actualizar la UI
                for (Location location : locationResult.getLocations()) {
                    updateLocationUI(location);
                }
            }
        };

        // Configurar los botones flotantes
        configureFloatingActionButtons();
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
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // 10 segundos
        locationRequest.setFastestInterval(5000); // 5 segundos

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                updateLocationUI(location);
                saveLocationToFirebase(location);  // Guardar la ubicación en Firestore
            } else {
                Toast.makeText(getApplicationContext(), "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
            }
        });

        startLocationUpdates();
    }

    private void updateLocationUI(Location location) {
        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        txtLatitud.setText(String.valueOf(location.getLatitude()));
        txtLongitud.setText(String.valueOf(location.getLongitude()));

        if (mMap != null) {
            if (userLocationMarker != null) {
                userLocationMarker.setPosition(currentLatLng);
            } else {
                // Añadir un marcador personalizado para la ubicación
                userLocationMarker = mMap.addMarker(new MarkerOptions()
                        .position(currentLatLng)
                        .title("Tu ubicación actual")
                        .snippet("Lat: " + location.getLatitude() + ", Long: " + location.getLongitude()));
            }

            // Mover el mapa hacia la nueva ubicación
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
        }
    }

    private void saveLocationToFirebase(Location location) {
        // Crear un mapa de datos para Firebase
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());

        // Guardar la ubicación en Firestore
        db.collection("user_locations")
                .add(locationData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(MainActivity.this, "Ubicación guardada correctamente", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error al guardar la ubicación", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);

        // Verificar si los permisos de ubicación de Google están concedidos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Si los permisos de Google están concedidos, comprobar si la ubicación está habilitada
            if (isLocationEnabled()) {
                mMap.setMyLocationEnabled(true);
                getCurrentLocation();
            } else {
                // Si la ubicación no está habilitada, abrir la configuración
                Toast.makeText(this, "Por favor, habilita la ubicación", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            // Si no se han concedido los permisos, solicitarlos
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // Centrar el mapa en Ovalle (o en la ubicación inicial deseada)
        LatLng ovalle = new LatLng(-30.6039, -71.1999);
        mMap.addMarker(new MarkerOptions().position(ovalle).title("Ovalle"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ovalle, 15f));
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        txtLatitud.setText(String.valueOf(latLng.latitude));
        txtLongitud.setText(String.valueOf(latLng.longitude));

        // Añadir un marcador cuando el usuario hace clic en el mapa
        mMap.addMarker(new MarkerOptions().position(latLng).title("Marcador Añadido")
                .snippet("Lat: " + latLng.latitude + ", Long: " + latLng.longitude));

        // Puedes almacenar este marcador en Firebase si lo deseas
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        // Añadir un marcador al hacer un largo clic
        mMap.addMarker(new MarkerOptions().position(latLng).title("Marcador largo clic"));
        txtLatitud.setText(String.valueOf(latLng.latitude));
        txtLongitud.setText(String.valueOf(latLng.longitude));

        // Puedes almacenar este marcador en Firebase si lo deseas
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
