package com.example.mapalocalcash;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

// Busca la direccion (calle y numero) de un punto del mapa
public class Direccion {

    // Texto de respaldo cuando no hay internet o no se encuentra la direccion
    public static String coordenadas(LatLng punto) {
        return String.format(Locale.US, "%.5f, %.5f", punto.latitude, punto.longitude);
    }

    // La busqueda usa internet, por eso va en otro hilo; el resultado vuelve a la pantalla
    @SuppressWarnings("deprecation")
    public static void buscar(Activity activity, LatLng punto, Consumer<String> alEncontrar) {
        new Thread(() -> {
            String texto = coordenadas(punto);
            try {
                List<Address> resultados = new Geocoder(activity, Locale.getDefault())
                        .getFromLocation(punto.latitude, punto.longitude, 1);
                if (resultados != null && !resultados.isEmpty() && resultados.get(0).getAddressLine(0) != null) {
                    texto = resultados.get(0).getAddressLine(0);
                }
            } catch (Exception e) {
                // Sin internet: se queda con las coordenadas
            }
            String resultado = texto;
            activity.runOnUiThread(() -> alEncontrar.accept(resultado));
        }).start();
    }
}
