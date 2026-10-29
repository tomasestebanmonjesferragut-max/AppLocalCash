package com.example.mapalocalcash;

import java.util.ArrayList;
import java.util.List;

// Calculos para dividir un gasto compartido. Todo en pesos enteros.
public class Reparto {

    // Divide el total en n partes iguales en pesos enteros.
    // Lo que sobra por redondeo lo paga la ultima persona, asi la suma da exacto el total.
    // Ej: 16000 entre 3 -> 5333, 5333, 5334
    public static List<Double> partesIguales(double total, int personas) {
        if (personas < 1) {
            throw new IllegalArgumentException("Debe haber al menos una persona");
        }
        long totalPesos = Math.round(total);
        long parte = totalPesos / personas;
        List<Double> montos = new ArrayList<>();
        for (int i = 0; i < personas - 1; i++) {
            montos.add((double) parte);
        }
        montos.add((double) (totalPesos - parte * (personas - 1)));
        return montos;
    }

    // Cuanto falta asignar para llegar al total (negativo si se pasaron)
    public static double falta(double total, List<Double> montos) {
        double suma = 0;
        for (double monto : montos) {
            suma += monto;
        }
        return total - suma;
    }

    // Los montos personalizados suman el total (con tolerancia de medio peso)
    public static boolean cuadra(double total, List<Double> montos) {
        return Math.abs(falta(total, montos)) < 0.5;
    }
}
