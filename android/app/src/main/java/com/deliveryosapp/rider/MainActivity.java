package com.deliveryosapp.rider;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    // Debe coincidir con PUSH_CHANNEL_RIDER en app.py del servidor DeliveryOS.
    // Los canales de Android son inmutables: para cambiar sonido o vibracion hay que
    // crear un id nuevo (ofertas_v2, ...) y borrar el anterior.
    private static final String OFFERS_CHANNEL = "ofertas_v2";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createOffersChannel();
        // Un par de segundos despues de abrir pedimos al sistema excluir la app del ahorro de
        // bateria: sin esto, Samsung/Xiaomi/Huawei/Oppo "duermen" la app y se cortan el GPS en
        // segundo plano y las push de ofertas de pedido (WhatsApp viene exento de fabrica).
        new Handler(Looper.getMainLooper()).postDelayed(this::requestBatteryExemption, 2500);
    }

    private void createOffersChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;
        nm.deleteNotificationChannel("ofertas_v1");   // canal del build 2 (timbre generado)
        if (nm.getNotificationChannel(OFFERS_CHANNEL) != null) return;

        NotificationChannel ch = new NotificationChannel(OFFERS_CHANNEL, "Ofertas de pedido",
                NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Pedidos disponibles para aceptar (60 s)");
        Uri sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/raw/alerta_pedido");
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build();
        ch.setSound(sound, attrs);
        ch.enableVibration(true);
        // ~3 s de vibracion, a la par de la alerta alerta_pedido.mp3 (2,8 s)
        ch.setVibrationPattern(new long[]{0, 600, 250, 600, 250, 600, 250, 600});
        ch.enableLights(true);
        ch.setLightColor(Color.parseColor("#FF6B00"));
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        ch.setShowBadge(true);
        nm.createNotificationChannel(ch);
    }

    private void requestBatteryExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        PowerManager pm = getSystemService(PowerManager.class);
        if (pm == null || pm.isIgnoringBatteryOptimizations(getPackageName())) return;
        // Si el usuario dijo "No", no insistir mas de una vez cada 12 h
        SharedPreferences sp = getSharedPreferences("dos_rider", MODE_PRIVATE);
        long last = sp.getLong("battery_prompt_at", 0L);
        if (System.currentTimeMillis() - last < 12L * 60 * 60 * 1000) return;
        sp.edit().putLong("battery_prompt_at", System.currentTimeMillis()).apply();
        try {
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception ignored) { }
        }
    }
}
