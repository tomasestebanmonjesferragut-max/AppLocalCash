package com.example.mapalocalcash;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import androidx.core.content.ContextCompat;

public class Monto {

    // Convierte el texto a numero. Devuelve null si esta vacio o no es un numero (ej: ".")
    public static Double leer(String texto) {
        try {
            return Double.parseDouble(texto.trim());
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
