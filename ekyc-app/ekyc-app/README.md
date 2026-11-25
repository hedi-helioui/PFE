# E-KYC : application mobile Flutter (client)

Application du **client** : connexion, ouverture d'un dossier, envoi du selfie, de la vidéo et du document d'identité,
soumission, puis suivi du statut et des scores. Elle consomme l'API du backend Spring Boot (`ekyc-backend`).

Le back-office (validation manuelle par les agents) n'est pas dans cette app : c'est le futur dashboard web.

## 1. Créer le projet Flutter

Cette archive contient uniquement le code (`lib/`, `test/`, `pubspec.yaml`), pas les dossiers `android/` et `ios/`.
Génère-les puis copie le code par-dessus :

```bash
flutter create --org com.pfe --project-name ekyc_app --platforms android,ios ekyc_app
# copie lib/, test/ et pubspec.yaml de cette archive DANS ekyc_app/ (en écrasant)
cd ekyc_app
rm test/widget_test.dart      # test d'exemple généré par flutter, il ne correspond plus à l'app
flutter pub get
flutter analyze
flutter test
```
   
## 2. Permissions et réseau

### Android : `android/app/src/main/AndroidManifest.xml`

Le backend de développement est en HTTP (pas HTTPS) : autorise-le dans la balise `<application>` :

```xml
<application
    android:usesCleartextTraffic="true"
    ... >
```

Ne déclare **pas** la permission `CAMERA` : `image_picker` utilise l'application caméra du téléphone, et déclarer la
permission sans la demander à l'exécution fait planter la prise de photo.

Dans `android/app/build.gradle` (ou `build.gradle.kts`), `flutter_secure_storage` demande `minSdk` 23 minimum.
Si Flutter génère une valeur plus basse, mets `minSdk = 23`.

### iOS : `ios/Runner/Info.plist`

```xml
<key>NSCameraUsageDescription</key>
<string>La caméra sert à photographier ton visage et ta pièce d'identité.</string>
<key>NSMicrophoneUsageDescription</key>
<string>Le micro est utilisé pour l'enregistrement vidéo de vérification.</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>Choisir une photo de ta pièce d'identité dans ta galerie.</string>
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSAllowsLocalNetworking</key>
    <true/>
</dict>
```

## 3. Lancer

Le backend doit tourner (`docker compose up -d` puis `mvn spring-boot:run` dans `ekyc-backend`).

| Cible | Commande |
|---|---|
| Émulateur Android | `flutter run` (adresse par défaut : `http://10.0.2.2:8080`) |
| Simulateur iOS | `flutter run --dart-define=API_BASE_URL=http://localhost:8080` |
| Téléphone réel | `flutter run --dart-define=API_BASE_URL=http://IP_DE_TON_PC:8080` (même Wi-Fi, port 8080 autorisé par le pare-feu) |

Pour trouver l'IP de ton PC : `ipconfig` (Windows) ou `ifconfig` / `ip a` (Mac, Linux).

## 4. Parcours de démonstration

1. **Créer un compte** (ou se connecter) : l'app n'accepte que les comptes `CLIENT`. Les comptes agent/admin sont refusés.
2. **Nouveau dossier** : nom, numéro de CIN et date de naissance. Le backend (OCR mocké) les compare au document.
3. **Selfie**, **vidéo** (10 s max), **document** (CIN ou passeport, caméra ou galerie).
4. **Soumettre** : le backend lance la vérification et renvoie le statut :
   - `Approuvé` ou `Rejeté` directement ;
   - `En revue manuelle` : un agent doit trancher via l'API back-office (Swagger).
5. L'écran affiche le statut, le commentaire de l'agent éventuel et le détail des quatre scores.

Pour simuler un rejet ou une revue manuelle, change `kyc.mock.*` dans `application.yml` du backend.

## 5. Structure

```
lib/
├── main.dart                 point d'entrée, AuthGate (connexion ou liste selon la session)
├── core/
│   ├── config.dart           adresse du backend (--dart-define=API_BASE_URL)
│   ├── api_client.dart       client Dio : toutes les routes, upload multipart, erreurs lisibles
│   └── format.dart           formats de dates
├── models/models.dart        KycCase, UserSession, VerificationResult... (miroir des DTO du backend)
├── state/providers.dart      Riverpod : authentification, client API, listes de dossiers
├── screens/
│   ├── auth_screen.dart      connexion / inscription
│   ├── cases_screen.dart     liste des dossiers (pull-to-refresh)
│   ├── new_case_screen.dart  identité déclarée
│   └── case_detail_screen.dart  étapes d'enrôlement, soumission, résultat
└── widgets/status_chip.dart
```

Points techniques :
- **Session** : le jeton JWT est stocké chiffré (`flutter_secure_storage`) et restauré au démarrage s'il n'a pas expiré.
  Un 401 renvoyé par le serveur déconnecte l'utilisateur et ferme les écrans ouverts.
- **Types de fichiers** : le backend n'accepte que JPEG/PNG (images) et MP4/WebM/MOV (vidéo). Le type MIME est déduit
  de l'extension ; la caméra iOS produit du `.mov`, accepté.
- **Erreurs** : les messages du backend (`detail`, erreurs de validation) sont affichés tels quels.

## 6. Limites actuelles (perspectives pour le mémoire)

- **Liveness** : la vidéo est enregistrée avec la caméra système, sans guidage ni contrôle en direct. Une vraie
  détection de vivacité active (clignement, mouvement de tête demandé) nécessite le package `camera` et la détection de
  visage en temps réel (ML Kit) ; ce serait l'étape suivante.
- Pas de version web ni desktop (les fichiers sont lus par chemin local).
- Pas de HTTPS ni d'épinglage de certificat (développement) ; à ajouter avant tout déploiement.
- Interface en français uniquement.
