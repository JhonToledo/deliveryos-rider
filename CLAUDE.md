# DeliveryOS Rider (app Android del repartidor)

## Que es
Shell de Capacitor que carga el sitio en vivo: `server.url` = `https://deliveryosapp.com/rider/login?bypass=plux2026test` (`capacitor.config.json`), `android.useLegacyBridge: true`. Paquete `com.deliveryosapp.rider`, repo `github.com/JhonToledo/deliveryos-rider`.
**Toda la UI y la logica viven en el backend** (repo `deliveryos`, `templates/rider/dashboard.html` y `app.py`). Cambios de HTML/CSS/JS/backend NO requieren recompilar; solo plugins, permisos, canales, iconos o `capacitor.config.json`. Lee primero el `CLAUDE.md` del repo `deliveryos` (reglas generales, dark mode, paleta, sin guion largo).

## Que aporta lo nativo (prioridad maxima de las 3 apps)
- **GPS en segundo plano**: `@capacitor-community/background-geolocation` + `@capacitor/geolocation`. **`useLegacyBridge: true` es obligatorio**: sin el, el GPS se corta a los 5 min (bug conocido del plugin). El dashboard usa `_CAP_NATIVE` (`Capacitor.isNativePlatform()`) para cambiar entre `navigator.geolocation` y el plugin, y reporta por socket + `POST /api/rider/position` como respaldo.
- **Push FCM** (`@capacitor/push-notifications`): oferta de pedido (`rider_offer`, 60 s para aceptar) y mensajes del chat con el cliente. Token via `POST /api/rider/fcm-token`.
- `@capacitor/haptics` y permiso `VIBRATE`.
- `MainActivity.java`: crea el canal **`ofertas_v2`** (sonido `res/raw/alerta_pedido.mp3`, vibracion ~3 s), borra `ofertas_v1`, y pide la exencion del ahorro de bateria (no insiste antes de 12 h).

## Reglas que no se pueden olvidar
- **Canales Android inmutables**: si cambia sonido o vibracion, id nuevo (`ofertas_v3`) en `MainActivity.java`, `PUSH_CHANNEL_RIDER` de `app.py` (repo deliveryos) y el borrado del canal viejo.
- `res/raw`: minusculas, sin guiones, sin empezar por digito (`alerta_pedido.mp3` = `static/3_firma_doble.mp3` del servidor).
- `appendUserAgent: DeliveryOSRider/<build>` (hoy 3) para que el dashboard distinga el APK; subirlo con `versionCode`/`versionName` (hoy 3 / 1.2 segun lo documentado en `deliveryos`).
- El `?bypass=` evita el gate "Proximamente" (`COMING_SOON=1`); sin el, el rider nunca llega al login.
- `node_modules/` e `.idea/` no se versionan.
- **Ramas en GitHub (2026-10-27)**: el trabajo de las builds 2 y 3 (1.1 y 1.2) se subio a la rama `main`, mientras que la rama por defecto del repo es `master` (solo tenia el "primer commit"). `main` contiene a `master`, asi que `master` se puede adelantar sin conflicto. Usa UNA sola rama de aqui en adelante (`master`, la que ya usan admin y client) y no vuelvas a subir a `main`. Verificado contra el codigo: `versionCode 3` / `1.2`, `appendUserAgent: DeliveryOSRider/3`, canal `ofertas_v2`, permisos `VIBRATE` y `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, plugin `@capacitor/haptics`.

## Compilar (Windows, sin Android Studio)
```bash
export JAVA_HOME="$LOCALAPPDATA/Programs/jdk/jdk-21.0.12.1+1"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
npx cap sync android
cd android && ./gradlew.bat assembleDebug --no-daemon     # APK en android/app/build/outputs/apk/debug/
```
En un equipo nuevo falta `android/local.properties`: `sdk.dir=C:/Users/<usuario>/AppData/Local/Android/Sdk`. Entorno completo en el CLAUDE.md de `deliveryos`, seccion "Compilacion de las apps". Copia el APK a `deliveryos/apk/` (ignorada por git) para instalarlo. Firebase: `android/app/google-services.json` (proyecto "deliveryosapp").

## Flujo de trabajo
Documenta aqui todo cambio nativo. Commits en espanol, primera linea maximo 72 caracteres.
