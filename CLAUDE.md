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

## iOS (fase F7, 2026-11-24): proyecto creado en Windows, SIN compilar ni probar
**Estado**: la carpeta `ios/` existe (`npx cap add ios`: Capacitor 8 con Swift Package Manager, iOS 15 o superior) y está configurada, pero **nunca se ha abierto en Xcode ni compilado**: esta PC es Windows y no hay Mac. Bundle id `com.deliveryosapp.rider`, nombre en pantalla «DeliveryOS Rider», versión 1.0 (compilación 1). Nada de esto toca el APK de Android (excepto lo que se avisa abajo).
**Qué se configuró**:
- `ios/App/App/Info.plist`: permisos con texto en español (ubicación mientras se usa y siempre, cámara y fotos (el registro sube foto, cédula y licencia)), modos en segundo plano (`location` y `remote-notification`) y `ITSAppUsesNonExemptEncryption = false`.
- `ios/App/App/App.entitlements` (`aps-environment` y `com.apple.developer.usernotifications.time-sensitive`: las ofertas de pedido atraviesan No molestar), enlazado en `project.pbxproj` (Debug y Release). El `aps-environment` queda en `development`; Xcode lo pasa a `production` al firmar para la App Store.
- `ios/App/App/AppDelegate.swift`: entrega al plugin de push el token (y el error) de registro; usa Firebase solo si está enlazado (ver «Push» abajo).
- `capacitor.config.json`: bloque `ios` (`contentInset: never`, porque las zonas seguras las maneja el sitio con `viewport-fit=cover`, y `appendUserAgent: DeliveryOSRider/ios-1`).
- Icono (1024x1024 sin transparencia, como exige la App Store) y pantalla de carga con el logo sobre `#1A202C`, generados con `scripts/gen_ios_assets.py` (`python scripts/gen_ios_assets.py ios/App/App/Assets.xcassets`).
- `codemagic.yaml`: plantilla **sin probar** para compilar y subir a TestFlight desde la nube.
- Comprobado desde Windows (87 comprobaciones estáticas entre los 3 proyectos): llaves del `project.pbxproj` balanceadas, `Info.plist` y entitlements legibles, imágenes de las medidas correctas, `Package.swift` con Capacitor 8 y `codemagic.yaml` con sintaxis válida. **No se compiló nada.**
**Regla**: `ios/App/CapApp-SPM/Package.swift` lo regenera Capacitor en cada `npx cap sync` (no editarlo a mano) y apunta a los plugins dentro de `node_modules`: antes de compilar siempre `npm ci`. `ios/App/App/public` y `ios/App/App/capacitor.config.json` se generan con `npx cap sync ios` y no se versionan.

**Lo que falta (en este orden; nada de esto se puede hacer desde Windows)**:
1. **Cuenta de Apple Developer Program** (99 USD al año) y crear la app en App Store Connect con el bundle id `com.deliveryosapp.rider` (de ahí sale el «Apple ID» numérico que pide `codemagic.yaml`).
2. **Compilar sin Mac**: Codemagic. Crear la integración con App Store Connect (clave de API `.p8` con rol App Manager), ponerle el nombre `DeliveryOS_ASC` (o cambiarlo en `codemagic.yaml`), poner `APP_STORE_APPLE_ID` y lanzar el flujo `ios-testflight`. La plantilla no se ha corrido nunca: el primer intento puede pedir ajustes (versión de Xcode, número de compilación, firma).
3. **Push (FCM)**: en Firebase (proyecto `deliveryosapp`) agregar una app iOS con el bundle id `com.deliveryosapp.rider` y descargar `GoogleService-Info.plist`; subir la llave APNs (`.p8`) en Firebase > Configuración del proyecto > Cloud Messaging > Configuración de la app de Apple; y agregar al target `App` el paquete `https://github.com/firebase/firebase-ios-sdk` con el producto `FirebaseMessaging` y el plist como recurso (en Xcode: File > Add Package Dependencies; sin Mac se puede hacer editando `ios/App/App.xcodeproj/project.pbxproj`, trabajo que se hace desde Windows cuando se tenga el plist). **El `AppDelegate.swift` ya está listo**: usa Firebase solo si el paquete está enlazado y el plist existe (`#if canImport`). Sin esto la push entrega el token de APNs y el servidor lo rechaza con 400 `apns_token` (necesita el de FCM).
4. **Sonido de la alerta**: convertir `static/3_firma_doble.mp3` del repo `deliveryos` a `alerta_pedido.caf` (en una Mac `afconvert -f caff -d LEI16 3_firma_doble.mp3 alerta_pedido.caf`; con ffmpeg `-f caf`, máximo 30 s) y agregarlo al paquete de la app; sin él las ofertas y los pedidos nuevos suenan con el sonido normal de iOS (`send_push` arma el bloque APNs con `<sonido>.caf`).
5. **Probar** en el simulador y después en un iPhone real con TestFlight: isla y barra de inicio (el sitio usa `viewport-fit=cover` y `env(safe-area-inset-*)`; si se ve doble margen, revisar `contentInset`), teclado abierto (cómo reduce la pantalla), giro a horizontal, modo oscuro, notificación con la app cerrada y su sonido, **GPS con la pantalla bloqueada** (no es fiel en el simulador: solo se prueba en un iPhone real) y la subida de documentos.
6. **App Store**: textos de privacidad («Privacidad de la app»), capturas, cuenta de prueba para el revisor y las guías de abajo.

**Riesgos propios de esta app**:
- `@capacitor-community/background-geolocation` declara Capacitor 7 en su `package.json` (el `cap sync` avisa «built for Capacitor 7») pero su `Package.swift` pide `from: 8.0.0` y se registra con `CAPBridgedPlugin`: debería resolver y funcionar; hay que confirmarlo en un iPhone real. En iOS el plugin muestra la barra azul de ubicación en segundo plano.
- `useLegacyBridge` es solo de Android: en iOS no aplica.
- Guía 4.2 de la App Store (apps que solo envuelven un sitio): explicar en las notas de revisión que usa GPS en segundo plano y notificaciones nativas, y dar una cuenta de repartidor aprobada. Guía 5.1.1(v): hay que poder **eliminar la cuenta dentro de la app** si se puede crear una (el repartidor se registra en la app y hoy esa opción NO existe: falta de producto).
