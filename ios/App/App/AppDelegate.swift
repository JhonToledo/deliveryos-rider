import UIKit
import Capacitor
#if canImport(FirebaseCore)
import FirebaseCore
#endif
#if canImport(FirebaseMessaging)
import FirebaseMessaging
#endif

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        #if canImport(FirebaseCore)
        // Firebase solo se activa si la app trae GoogleService-Info.plist (se descarga de la consola de Firebase al registrar la app iOS).
        // Sin el archivo la app arranca igual, pero la push entrega el token de APNs y el servidor lo rechaza (necesita el de FCM).
        if Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            FirebaseApp.configure()
        }
        #endif
        return true
    }

    func applicationWillResignActive(_ application: UIApplication) {
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
    }

    func applicationWillEnterForeground(_ application: UIApplication) {
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
    }

    func applicationWillTerminate(_ application: UIApplication) {
    }

    // MARK: - Notificaciones push
    // El plugin @capacitor/push-notifications espera estos avisos. Si Firebase esta enlazado y configurado se le entrega el token de FCM
    // (lo que guarda el servidor en fcm_token); si no, el token de APNs.
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        #if canImport(FirebaseMessaging)
        if FirebaseApp.app() != nil {
            Messaging.messaging().apnsToken = deviceToken
            Messaging.messaging().token { token, error in
                if let error = error {
                    NotificationCenter.default.post(name: .capacitorDidFailToRegisterForRemoteNotifications, object: error)
                } else if let token = token {
                    NotificationCenter.default.post(name: .capacitorDidRegisterForRemoteNotifications, object: token)
                }
            }
            return
        }
        #endif
        NotificationCenter.default.post(name: .capacitorDidRegisterForRemoteNotifications, object: deviceToken)
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        NotificationCenter.default.post(name: .capacitorDidFailToRegisterForRemoteNotifications, object: error)
    }

    func application(_ application: UIApplication,
                     configurationForConnecting connectingSceneSession: UISceneSession,
                     options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        let config = UISceneConfiguration(name: "Default Configuration",
                                          sessionRole: connectingSceneSession.role)
        config.delegateClass = SceneDelegate.self
        return config
    }
}
