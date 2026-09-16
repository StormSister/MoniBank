---
title: Scenariusz filmu GETCUST
description: Scenariusz 6–8 minut pokazujący jeden prawdziwy request MoniBanku.
outline: deep
---

# Scenariusz filmu: „To API JSON działa przez mainframe i terminal 3270”

**Długość:** 6–8 minut  
**Format:** nagranie ekranu + narracja + animowany diagram  
**Odbiorcy:** Java developerzy, początkujący mainframowcy oraz doświadczeni programiści COBOL/CICS  
**Obietnica:** pokazuję jeden kompletny, prawdziwy request — nie sam slajd architektoniczny.

## Zasada redakcyjna

Główną historią jest poprawne wywołanie `GETCUST`. Błąd długości spool pojawia się dopiero w środku jako krótkie odkrycie inżynierskie wyjaśniające ramki. Debugowanie nie może zdominować początku filmu.

## Cold open — 0:00–0:25

### Obraz

1. Bliskie ujęcie Postmana.
2. Pokazuję endpoint i body:

```json
{ "customerId": "C000000000006" }
```

3. Klikam **Send**.
4. Natychmiast pokazuję czysty JSON `CustomerResponse`.

### Narracja

> „To wygląda jak zupełnie zwykłe API. Wysyłam identyfikator klienta jako JSON i otrzymuję dane klienta jako JSON — tak jak w milionach requestów wykonywanych każdego dnia. Ale ten klient nie został odczytany z chmurowej bazy. Java sterowała terminalem 3270, uruchomiła transakcję KICKS na MVS 3.8J, COBOL odczytał VSAM, a JES zwrócił wynik przez wirtualną drukarkę. Prześledzę ten request bajt po bajcie.”

### Tytuł

**JSON → 3270 → COBOL → VSAM → JES → JSON**

W narracji używam sformułowania „linia MVS wywodząca się z końca lat siedemdziesiątych”. Nie nazywam MVS 3.8J dosłownie systemem wydanym w 1978 roku.

## Akt 1: zwyczajna granica API — 0:25–1:05

### Obraz

- Postman po lewej.
- IDE po prawej: `CustomerController.getCustomer()` oraz `GetCustomerRequest`.
- Zbliżenie na regex `^C[0-9]{12}$`.

### Narracja

> „Granica HTTP jest celowo nudna. Spring sprawdza, czy ID zaczyna się od litery C i zawiera dwanaście cyfr. Błędne dane zostają odrzucone, zanim zajmę sesję terminalową. Kontroler przekazuje request do `CustomerService`, który wybiera operację online `GETCUST`.”

### Kod

```java
@PostMapping("/get")
public ResponseEntity<CustomerResponse> getCustomer(
        @Valid @RequestBody GetCustomerRequest request
)
```

```java
kicksMainframeOperationExecutor.execute(
        CustomerMainframeOperations.GET_CUSTOMER,
        request.customerId()
)
```

## Akt 2: automatyczny start i gotowa sesja KICKS — 1:05–1:40

### Obraz

- Start Spring Boot.
- Logi workera przechodzące przez TN3270, TSO, KICKS i MBGW.
- Na końcu:

```text
MBGW terminal session is READY
Persistent KICKS terminal session is ready to accept MBGW requests
```

### Narracja

> „Backend nie loguje się do TSO przy każdym requestcie. Razem ze Springiem uruchamia się stały worker terminala. Łączy się przez TN3270, loguje do TSO, wykonuje procedurę `MBKICKS`, uruchamia KICKS i otwiera ekran bramki MBGW. Dopiero wtedy aplikacja zgłasza gotowość.”

### Detal

```text
EXEC 'HERC01.CMDPROC(MBKICKS)'
```

> „Manager jest właścicielem kolejki i terminala. Kontroler ani serwis domenowy nie wpisują bezpośrednio komend 3270.”

Podczas nagrania nie otwieram drugiej interaktywnej sesji `HERC01`, ponieważ grozi to `IKJ56425I USERID IN USE`.

## Akt 3: najpierw rejestracja, potem wysłanie — 1:40–2:15

### Obraz

- Animowany diagram.
- Spring → listener TCP → worker terminalowy.
- Jeden prawdziwy request ID, np. `R2501452`, zaznaczony tym samym kolorem we wszystkich ujęciach.

### Narracja

> „Executor generuje ośmioznakowy request ID. Zanim wyśle cokolwiek, `MainframeResponseExecutor` rejestruje ten identyfikator w listenerze TCP. Chroni to przed sytuacją, w której szybki wynik przyjdzie, zanim Java zacznie na niego czekać. Dopiero potem do managera trafia `MbgwRequest`.”

### Logi

```text
KICKS [Rxxxxxxx] GETCUST started
MAINFRAME [Rxxxxxxx] Registering result listener
Executing MBGW operation GETCUST [Rxxxxxxx]
```

## Akt 4: Java steruje transakcją 3270 — 2:15–2:50

### Obraz

Najlepiej pokazać bezpieczny capture MBGW przed Enterem i po zakończeniu. Jeżeli nie da się bezpiecznie mirrorować ekranu, używam zanonimizowanego dumpu lub diagramu pól.

```text
OPERATION    GETCUST
REQUEST ID   Rxxxxxxx
INPUT LENGTH 0013
INPUT        C000000000006
```

### Narracja

> „Worker jest już zalogowany, a KICKS działa. Java wypełnia na ekranie MBGW nazwę operacji, request ID, długość danych i customer ID, po czym naciska Enter. Terminal jest kanałem poleceń, a nie kanałem zwracania danych.”

## Akt 5: MBGATE, GETCUST i VSAM — 2:50–3:35

### Obraz

- `GETCUST.cob`.
- Walidacja `EIBCALEN`, wersji, operacji i input length.
- Blok `EXEC CICS READ`.
- Grafika układu 119-bajtowego klienta.

### Narracja

> „MBGW przekazuje 855-bajtową COMMAREA do dispatchera `MBGATE`, który wykonuje LINK do programu COBOL `GETCUST`. Program sprawdza protokół i wykonuje kluczowy `EXEC CICS READ` na `CUSTFILE`. Klucz ma 13 znaków, a rekord VSAM dokładnie 119: status, ID, kraj, national ID, imię, nazwisko, datę urodzenia i timestamp utworzenia.”

### Pasek techniczny

```text
FILE('CUSTFILE') · RIDFLD(customerId) · KEYLENGTH(13) · LENGTH(119)
```

## Akt 6: jedna odpowiedź, dwa rekordy logiczne — 3:35–4:05

### Obraz

```text
MBR;D;CUSTOMER;Rxxxxxxx;<119-bajtowy payload>
MBR;S;GETCUST;Rxxxxxxx;C000000000006;A;OK
```

### Narracja

> „Sukces nie jest pojedynczą linią. GETCUST najpierw tworzy rekord D z payloadem klienta, a potem końcowy S ze statusem. Większe operacje mogą wysyłać wiele D. Rekord E zamyka odpowiedź błędną. Java nie uznaje wyniku za kompletny, dopóki nie pojawi się S albo E.”

## Akt 7: program MBRESULT i problem 27 bajtów — 4:05–4:55

### Obraz

- `GETCUST`: `PERFORM CALL-MBRESULT THRU CALL-MBRESULT-EXIT`.
- `MBRESULT`: `SPOOLOPEN`, `SPOOLWRITE`, `SPOOLCLOSE`.
- Krótkie wspomnienie `WRITEERR` i timeoutu.
- Następnie:

```text
RESP=22  RESP2=27
```

- Pasek 160 przekraczający limit 133 o 27.

### Narracja

> „Wspólny program `MBRESULT` odpowiada za wysłanie wyniku do spool. Pierwsza wersja próbowała zapisać logiczny rekord 160-znakowy jednym `SPOOLWRITE`. Java dostała timeout, ale był to tylko skutek. Po dodaniu diagnostyki KICKS zwrócił RESP 22 i RESP2 27: linia była o 27 znaków za długa. Online spool przyjmował 133, a protokół MoniBanku wymagał 160.”

> „Nie skróciłam protokołu, ponieważ batch już używał rekordów 160-bajtowych. Zmieniłam wyłącznie transport.”

## Akt 8: dwie ramki fizyczne — 4:55–5:25

### Obraz

```text
<ASA> MBP ; requestId ; 1 ; pierwsze 80 bajtów
<ASA> MBP ; requestId ; 2 ; ostatnie 80 bajtów
```

Podpis:

**96 znaków fizycznych na ramkę; 160 znaków logicznych po złożeniu.**

### Narracja

> „MBRESULT nadal przyjmuje jeden rekord MBR X(160), ale dzieli go na dwie ramki MBP z payloadem po 80 znaków. D call używa sterowania M — więcej danych nadejdzie. Końcowy S albo E używa F i zamyka raport.”

## Akt 9: JES i wirtualna drukarka — 5:25–5:55

### Obraz

- Sekwencja `SPOOLOPEN → SPOOLWRITE ×2 → SPOOLCLOSE`.
- `CLASS('Z')`.
- Log Herculesa pokazujący połączenie drukarki, bez publicznych adresów IP.

### Narracja

> „MBRESULT utrzymuje token jednego raportu przez całą odpowiedź. Po końcowym rekordzie zamyka raport klasy Z. JES przekazuje go do wirtualnej drukarki, a Hercules udostępnia ją jako socket TCP. 3270 wysłał komendę; drukarka zwraca dane.”

## Akt 10: listener składa wynik — 5:55–6:35

### Obraz

- `MainframeTcpResultListener` i logi.
- Part 1 + part 2 → MBR X(160), najpierw dla D, potem dla S.
- Krótka animacja przeplatanych request ID.

### Narracja

> „Listener przyjmuje kompletne MBR z batcha oraz ramki MBP z KICKS. Usuwa opcjonalną spację ASA, zachowuje fixed-width spaces i dopełnia każdą część do 80, jeśli drukarka obcięła końcowe spacje. Część druga kończy dokładnie 160 znaków.”

> „Rekord D jest gromadzony. S albo E kończy future oczekujące w requestcie HTTP. Stan jest przechowywany per request ID, więc w przyszłości odpowiedzi kilku workerów mogą się przeplatać.”

### Logi do nagrania

```text
MAINFRAME FRAME [Rxxxxxxx] reassembled into 160 bytes
MAINFRAME RESULT << [MBR;D;CUSTOMER;Rxxxxxxx;...]
MAINFRAME FRAME [Rxxxxxxx] reassembled into 160 bytes
MAINFRAME RESULT << [MBR;S;GETCUST;Rxxxxxxx;...;OK]
TCP SUCCESS - received 2 record(s)
```

Jeśli log zawiera national ID, zamazuję je.

## Akt 11: rekord fixed-width staje się obiektem — 6:35–7:05

### Obraz

- `MainframeResultParser.parseDataRecord()` i split limit 5.
- Offsety w `CustomerRecordParser`.
- Transformacja payloadu do pól JSON.

### Narracja

> „Wspólny `MainframeResultParser` rozumie protokół MBR, ale nie klientów. Zachowuje cały fixed-width payload. `CustomerRecordParser` mapuje 119 pozycji na `CustomerResponse`. Serwis wymaga jednego klienta i sprawdza jego ID.”

```text
A C000000000006 PL *********** MONIKA ... TESTOWA ...
                         ↓
{ customerId, countryCode, firstName, lastName, status, ... }
```

## Zakończenie — 7:05–7:35

### Obraz

- Powrót do odpowiedzi Postmana.
- Pełny diagram.
- Na końcu spokojny ekran MBGW w stanie READY.

### Narracja

> „Na zewnątrz MoniBank jest zwyczajnym API JSON. Wewnątrz każda warstwa zachowuje swoje mocne strony: Spring orkiestruje, 3270 wysyła polecenie, KICKS kieruje transakcją, COBOL czyta VSAM, JES transportuje, a Java koreluje i parsuje.”

> „Nie ukryłam mainframe’u, udając, że jest bazą danych. Zbudowałam protokół zgodny z tym, jak naprawdę działa.”

### Plansza końcowa

**Jeden request API. Pięć dekad pomysłów informatycznych. Jeden klient zwrócony poprawnie.**

## Teaser 60 sekund

**0:00–0:08** — Postman i JSON.  
„To jest zwyczajne API klienta — z wyjątkiem tego, że odczyt idzie przez automatyczną sesję 3270 na MVS.”

**0:08–0:20** — Diagram.  
„Spring tworzy request ID, steruje KICKS, a COBOL czyta kluczowy, 119-bajtowy rekord VSAM.”

**0:20–0:36** — `RESP=22, RESP2=27` i animacja podziału.  
„Pierwszy zapis spool zawiódł, bo 160 znaków było o 27 za dużo. Zachowałam protokół logiczny i podzieliłam każdy rekord na dwie ramki.”

**0:36–0:50** — Logi listenera.  
„JES wysyła ramki przez wirtualną drukarkę klasy Z. Java odtwarza 160 znaków, grupuje po request ID i czeka na końcowy S albo E.”

**0:50–1:00** — Finalny JSON.  
„Klient API nic o tym nie wie. Otrzymuje czysty JSON. To jest MoniBank.”

## Proponowane tytuły

1. **To API JSON działa przez terminal 3270 i mainframe**
2. **Spring Boot spotyka MVS: jeden request od początku do końca**
3. **Od Postmana do COBOL-a i z powrotem — przez JES**
4. **Zbudowałam REST API dla MVS 3.8J**

## Materiały do nagrania

- Postman: request i poprawny JSON;
- start Springa aż do `MBGW READY`;
- `CustomerController`, walidacja i `CustomerService`;
- generowanie request ID i register-before-send;
- bezpieczny capture MBGW;
- `MBGATE`, `GETCUST`, 855-bajtowa COMMAREA;
- `EXEC CICS READ FILE('CUSTFILE')`;
- layout 119-bajtowego klienta;
- rekordy logiczne D i S;
- `MBRESULT` oraz kontrola M/F;
- `RESP=22, RESP2=27`;
- dwie ramki MBP;
- klasa Z i połączenie drukarki Hercules;
- logi składania w `MainframeTcpResultListener`;
- `MainframeResultParser` i `CustomerRecordParser`;
- finalny JSON oraz pełny diagram.
