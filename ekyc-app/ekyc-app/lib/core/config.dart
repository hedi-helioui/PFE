/// Configuration de l'application.
///
/// L'adresse du backend se change au lancement, sans toucher au code :
///   flutter run --dart-define=API_BASE_URL=http://192.168.1.20:8080
///
/// Valeur par défaut : 10.0.2.2 = la machine hôte vue depuis l'émulateur Android.
/// (Simulateur iOS : http://localhost:8080 ; téléphone réel : l'IP de ton PC sur le Wi-Fi.)
class AppConfig {
  AppConfig._();

  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080',
  );
}
