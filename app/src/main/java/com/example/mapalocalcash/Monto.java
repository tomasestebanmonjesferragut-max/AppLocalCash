package com.example.mapalocalcash;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import androidx.core.content.ContextCompat;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Monto {

    // Formato chileno: punto para miles y millones, sin decimales (110000 -> "110.000")
    private static final DecimalFormat MILES = crearFormato();

    private static DecimalFormat crearFormato() {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.ROOT);
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');
        return new DecimalFormat("#,##0", simbolos);
    }

    // Numero con puntos de miles, sin signo $ (ej: 1250000 -> "1.250.000")
    public static String miles(double valor) {
        return MILES.format(Math.round(valor));
    }

    // Monto con signo $ y puntos de miles. Negativo: "-$5.000"
    public static String dinero(double valor) {
        return (Math.round(valor) < 0 ? "-$" : "$") + miles(Math.abs(valor));
    }

    // Convierte el texto a numero. Devuelve null si esta vacio o no es un numero (ej: ".").
    // Acepta puntos de miles ("110.000" = 110000) y coma decimal ("1.500,5").
    public static Double leer(String texto) {
        String limpio = texto.trim().replace("$", "").replace(" ", "");
        if (limpio.contains(",")) {
            limpio = limpio.replace(".", "").replace(",", ".");
        } else if (limpio.matches("\\d{1,3}(\\.\\d{3})+")) {
            limpio = limpio.replace(".", "");
        }
        try {
            return Double.parseDouble(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Texto normal seguido de un monto en verde (suma) o rojo (resta)
    public static CharSequence conColor(Context context, String antes, String monto, boolean verde) {
        SpannableString texto = new SpannableString(antes + monto);
        int color = ContextCompat.getColor(context, verde ? R.color.verde_oscuro : R.color.rojo);
        texto.setSpan(new ForegroundColorSpan(color), antes.length(), texto.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        texto.setSpan(new StyleSpan(Typeface.BOLD), antes.length(), texto.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return texto;
    }
}
