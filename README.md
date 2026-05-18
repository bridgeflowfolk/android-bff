# Bridge & Flow Folk – Application Android

Application Android officielle de l'association Bridge & Flow Folk (BFF), Nogent-le-Roi (28).

## Fonctionnalités

- **Événements** : liste avec image, titre, date, lieu, description – triés chronologiquement
- **Recherche** : filtre en temps réel sur titre et lieu
- **Mode hors-ligne** : Room cache tous les événements dès le premier lancement
- **Sync automatique** : toutes les 6h via WorkManager (dès connexion disponible)
- **Notifications** : nouveaux événements détectés + rappel automatique 2h avant
- **À propos** : WebView vers `https://bridgeflowfolk.github.io/apropos.html`
- **Contact** : appel téléphonique, WhatsApp, Facebook

---

## Stack technique

| Couche | Technologie |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository |
| DI | Hilt |
| BDD locale | Room |
| Réseau | Retrofit + OkHttp |
| Images | Coil |
| Background | WorkManager |
| CI/CD | GitHub Actions |

---

## Structure du projet

```
app/src/main/java/com/bridgeflowfolk/bff/
├── data/
│   ├── local/          # Room (Entity, DAO, Database)
│   ├── remote/         # Retrofit (DTO, ApiService)
│   └── EventRepositoryImpl.kt
├── di/                 # Modules Hilt
├── domain/             # Modèles + interface Repository
├── notifications/      # NotificationHelper
├── ui/
│   ├── components/     # EventCard, EmptyState
│   ├── screens/        # Events, About, Contact
│   ├── theme/          # BffTheme, couleurs, typo
│   └── EventsViewModel.kt
├── workers/            # SyncWorker, ReminderWorker, BootReceiver
├── BffApplication.kt
└── MainActivity.kt
```

---

## Installation et build

### Prérequis
- Android Studio Hedgehog (2023.1.1) ou plus récent
- JDK 17
- Android SDK 34

### Build debug

```bash
git clone https://github.com/bridgeflowfolk/bff-android.git
cd bff-android
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/
```

### Build release (signé)

1. Générer un keystore :
```bash
keytool -genkey -v -keystore bff-release.jks \
  -alias bff -keyalg RSA -keysize 2048 -validity 10000
```

2. Définir les variables d'environnement :
```bash
export KEYSTORE_PATH=bff-release.jks
export KEYSTORE_PASSWORD=votre_mot_de_passe
export KEY_ALIAS=bff
export KEY_PASSWORD=votre_mot_de_passe_cle
./gradlew assembleRelease
```

---

## Pipeline CI/CD GitHub Actions

Le workflow `.github/workflows/android.yml` :
- **PR / push `main`** → build debug APK (artefact 14j)
- **Tag `v*`** → build release signé + GitHub Release

### Secrets à configurer dans GitHub

| Secret | Description |
|---|---|
| `KEYSTORE_BASE64` | Keystore encodé en base64 (`base64 -i bff-release.jks`) |
| `KEYSTORE_PASSWORD` | Mot de passe du keystore |
| `KEY_ALIAS` | Alias de la clé |
| `KEY_PASSWORD` | Mot de passe de la clé |

---

## Format du JSON événements

Déposer à `https://bridgeflowfolk.github.io/info.json` :

```json
[
  {
    "id": "identifiant-unique-stable",
    "title": "Titre de l'événement",
    "date": "2025-06-15T10:00:00",
    "location": "Lieu complet",
    "description": "Description courte (2-3 lignes max)",
    "image": "https://url-de-l-image.jpg"
  }
]
```

> **Important** : l'`id` est la clé de détection des nouveaux événements.
> Ne jamais le modifier après publication — cela déclencherait une double notification.

---

## Assets à ajouter manuellement

Les fichiers suivants ne sont pas générables automatiquement et doivent être ajoutés :

- `app/src/main/res/mipmap-*/ic_launcher.png` – icônes de l'app aux différentes densités
- `app/src/main/res/drawable/ic_notification.xml` – icône monochrome pour les notifications
- `app/src/main/res/drawable/ic_whatsapp.xml` – icône WhatsApp (vecteur libre de droits)
- `app/src/main/res/drawable/ic_facebook.xml` – icône Facebook (vecteur)

### Générer les icônes depuis le logo fourni
Dans Android Studio : **File → New → Image Asset**
- Type : Launcher Icons
- Source : fournir le fichier `19716.png`
- Background : `#F5F0E8` (beige chaud du thème)

---

## Compatibilité

| Android | API | Statut |
|---|---|---|
| 8.0 Oreo | 26 | ✅ Min |
| 9–12 | 28–32 | ✅ |
| 13 (Tiramisu) | 33 | ✅ POST_NOTIFICATIONS géré |
| 14 (Upside Down Cake) | 34 | ✅ Target |
| 15–16 | 35–36 | ✅ Compatible |
