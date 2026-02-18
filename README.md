# ⚓ Battaglia Navale — Android

Gioco di Battaglia Navale multiplayer locale per Android.  
**Kotlin + Jetpack Compose + NSD (Network Service Discovery)**

---

## 📥 Come ottenere l'APK (senza installare nulla)

### Metodo 1 — GitHub Actions (automatico, RACCOMANDATO)

**Passo 1 — Crea un account GitHub (gratis)**
- Vai su [github.com](https://github.com) e registrati

**Passo 2 — Crea un nuovo repository**
1. Clicca **"+"** in alto a destra → **"New repository"**
2. Nome: `BattagliaNavale`
3. Visibilità: **Public** (le Actions sono gratis per repository pubblici)
4. **Non** spuntare nessuna opzione extra
5. Clicca **"Create repository"**

**Passo 3 — Carica i file**

Opzione A — via browser (più semplice):
1. Nella pagina del tuo repository, clicca **"uploading an existing file"**
2. Trascina TUTTI i file e cartelle di questo progetto
3. Clicca **"Commit changes"**

> ⚠️ **Importante:** GitHub non accetta il caricamento di cartelle via drag-and-drop direttamente.  
> Usa l'opzione B (git) oppure carica i file mantenendo la struttura a mano.

Opzione B — via Git (se hai Git installato):
```bash
cd BattagliaNavaleAndroid
git init
git add .
git commit -m "Initial commit - Battaglia Navale"
git branch -M main
git remote add origin https://github.com/TUO_USERNAME/BattagliaNavale.git
git push -u origin main
```

**Passo 4 — Scarica l'APK**
1. Vai al tuo repository su GitHub
2. Clicca sul tab **"Actions"**
3. Vedrai il workflow **"Build APK"** in esecuzione (circa 5-7 minuti)
4. Quando diventa ✅ verde, cliccaci sopra
5. In fondo alla pagina, sotto **"Artifacts"**, scarica **"BattagliaNavale-APK"**
6. Trovi anche la **Release** con l'APK nella sezione "Releases" del repository

**Passo 5 — Installa sul telefono**
1. Trasferisci il file `.apk` sul telefono Android (via USB, email, Drive...)
2. **Impostazioni → Sicurezza** (o **Installa app sconosciute**) → Abilita per il tuo browser/file manager
3. Apri il file APK e installa
4. Ripeti su entrambi i dispositivi

---

## 📁 Struttura del progetto

```
BattagliaNavaleAndroid/
├── .github/
│   └── workflows/
│       └── build.yml                    ← 🤖 GitHub Actions build
├── gradle/
│   ├── libs.versions.toml               ← Versioni dipendenze
│   └── wrapper/
│       └── gradle-wrapper.properties
├── app/
│   ├── build.gradle.kts                 ← Configurazione app
│   └── src/main/
│       ├── AndroidManifest.xml          ← Permessi rete
│       ├── res/values/
│       │   ├── strings.xml
│       │   └── themes.xml
│       └── java/com/battaglianavale/
│           ├── MainActivity.kt          ← Entry point + navigazione
│           ├── models/
│           │   └── Models.kt            ← Strutture dati
│           ├── network/
│           │   └── NetworkManager.kt    ← NSD + TCP socket
│           ├── game/
│           │   └── GameViewModel.kt     ← Logica di gioco
│           └── ui/
│               ├── theme/Theme.kt       ← Colori carta e inchiostro
│               ├── components/
│               │   └── HandDrawnGrid.kt ← Griglia Canvas
│               └── screens/
│                   ├── SetupScreen.kt   ← Fase 1: connessione
│                   ├── PlacementScreen.kt ← Fase 2: piazza navi
│                   └── GameScreen.kt    ← Fase 3: partita
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew
```

---

## 🎮 Come si gioca

### Fase 1 — Connessione e impostazioni
- Entrambi i giocatori aprono l'app con i dispositivi sulla **stessa rete Wi-Fi**
- L'app cerca automaticamente altri dispositivi con l'app installata
- Tocca il nome dell'avversario per connettersi
- L'**host** (chi ha effettuato la connessione) configura:
  - Dimensione della griglia (6×6 fino a 15×15)
  - Numero e tipo di navi
- Premi **"Invia impostazioni"** poi **"Posiziona le navi"**

### Fase 2 — Posizionamento navi
- Tocca la griglia per posizionare la nave selezionata
- Premi **"→ Orizzontale / ↓ Verticale"** per ruotare
- **"↩ Annulla"** rimuove l'ultima nave piazzata
- Le navi non possono toccarsi (nemmeno in diagonale)
- Quando tutte le navi sono piazzate → **"Conferma e inizia!"**

### Fase 3 — Partita
- Schermata divisa: **griglia tua (sinistra)** e **griglia nemica (destra)**
- Banner superiore indica di chi è il turno
- Quando è **il tuo turno** (bordo griglia destra evidenziato):
  - Tocca una cella per sparare
  - **💧 Acqua**: cerchio blu → turno avversario
  - **💥 Colpito**: X rossa → spara ancora!
  - **⚓ Affondata**: X rossa scura
- Il pannello flotta mostra lo stato di tutte le navi nemiche
- Vince chi affonda **tutta** la flotta avversaria

---

## 🔧 Tecnologie usate

| Tecnologia | Utilizzo |
|-----------|---------|
| Kotlin 2.0 | Linguaggio principale |
| Jetpack Compose | UI dichiarativa |
| Compose Canvas | Griglia disegnata a mano |
| Android NSD | Scoperta dispositivi in rete locale |
| TCP Sockets | Comunicazione tra i dispositivi |
| ViewModel + StateFlow | Gestione stato reattiva |
| Coroutines | Networking asincrono |
| Gson | Serializzazione messaggi JSON |

---

## 📋 Requisiti

- **Android 8.0+** (API 26+)
- Due dispositivi Android sulla **stessa rete Wi-Fi**
- Permesso rete locale (richiesto al primo avvio)

---

## 🐛 Troubleshooting

| Problema | Soluzione |
|----------|-----------|
| I dispositivi non si trovano | Verifica che siano sulla stessa rete Wi-Fi. Il Wi-Fi deve essere attivo. |
| Build fallita su GitHub | Controlla il log della Action → riga rossa → espandi per vedere l'errore |
| "Installa da sorgenti sconosciute" | Impostazioni → Sicurezza → Abilita l'installazione da fonti sconosciute |
| App crasha all'avvio | Verifica di avere Android 8.0+ |
| La griglia non risponde | Controlla il banner in alto: deve dire "È IL TUO TURNO" |

---

## 🔄 Personalizzazioni

**Regola colpito/continua** — in `GameViewModel.kt`:
```kotlin
ShotOutcome.HIT -> {
    _isMyTurn.value = true   // colpito → spara ancora
    // oppure:
    _isMyTurn.value = false  // turni alternati sempre
}
```

**Porta TCP** — in `NetworkManager.kt`:
```kotlin
private const val PORT = 47832  // cambia con qualsiasi porta libera
```
