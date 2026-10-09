# Dread — mod horror per Minecraft (Fabric 1.20.1)

## Cosa contiene
- **Lo Stalker**: figura alta e pallida, occhi rossi luminosi. Si muove SOLO quando nessuno lo guarda (come un angelo
  piangente), compare alle tue spalle se resta lontano, ti acceca e ti rallenta quando colpisce. Sparisce dopo ~6 minuti.
- **Sistema della Paura (0-100)** con HUD: vignetta rossa pulsante + barra. Sale al buio, in caverna, nel Nether,
  durante la Notte del Terrore e con lo Stalker vicino. Scende alla luce, vicino a candele, con l'Amuleto Sacro.
- **Allucinazioni**: suoni spettrali da lontano, passi che si avvicinano da dietro, creeper che sfrigola, porte che
  sbattono, respiro vicinissimo, messaggi inquietanti, Oscurità, Nausea, battito cardiaco.
- **Notte del Terrore**: ogni notte ha 1 probabilità su 5 di diventare una Notte del Terrore (più paura, Stalker più frequenti).
- **Oggetti/Blocchi**: Amuleto Sacro, Frammento d'Incubo (drop dello Stalker), Candela Maledetta, Lapide, Uovo di Spawn.
- **Ricette**: Candela Maledetta = candela + sabbia delle anime; Amuleto = 4 lingotti d'oro a croce + Frammento d'Incubo;
  Lapide = 7 pietre a forma di lapide.
- Lingue: inglese e italiano.

## Comandi (operatore)
- `/dread fear <0-100>` imposta la tua paura
- `/dread stalker` fa comparire uno Stalker
- `/dread night` attiva la Notte del Terrore (10 minuti)
- `/dread calm` azzera tutto

## Compilare
Servono **JDK 17** e **Gradle 8.5**.
1. Nella cartella del progetto: `gradle wrapper --gradle-version 8.5`
2. `./gradlew build` (su Windows `gradlew.bat build`)
3. Il file `.jar` finale è in `build/libs/dreadmod-1.0.0.jar` (NON quello `-sources`).
In alternativa apri la cartella con IntelliJ IDEA (importa come progetto Gradle) e lancia `runClient` per provarla.

## Installare
Installa Fabric Loader per 1.20.1 e **Fabric API 0.92.2+1.20.1**, poi metti `dreadmod-1.0.0.jar` nella cartella `mods`.

## Personalizzare
I valori di bilanciamento (quanta paura guadagni, probabilità di spawn, ecc.) sono costanti in cima a
`src/main/java/com/dreadmod/FearManager.java`.
