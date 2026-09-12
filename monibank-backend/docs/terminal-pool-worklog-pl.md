# MoniBank: od jednego terminala 3270 do puli trzech terminali

Dokument roboczy do przyszłego wpisu na blogu. Zapisujemy tutaj stan wyjściowy,
każdy krok wykonany na MVS i w Javie, napotkane błędy oraz sposób ich rozwiązania.

## Cel etapu

- trzy niezależne terminale 3270 obsługiwane przez Spring Boot;
- osobny użytkownik TSO dla każdego terminala;
- wspólna kolejka żądań przydzielająca pracę pierwszemu wolnemu terminalowi;
- automatyczne odtworzenie sesji po błędzie terminala;
- poprawny `KSSF` i `LOGOFF` podczas kontrolowanego restartu backendu;
- widoczność stanu każdego terminala w zakładce `System Status`;
- awaria jednej sesji nie może zatrzymać pozostałych terminali.

Pierwszy praktyczny kamień milowy to dwa jednocześnie działające terminale. Kod i
konfiguracja od początku przewidują trzy.

## Stan wyjściowy: jeden terminal

Przepływ operacji online:

1. Spring Boot rejestruje `requestId` w listenerze wyniku.
2. Żądanie trafia do jednej kolejki `KicksTerminalSessionManager`.
3. Jedyny worker używa trwałej sesji `KicksTerminalSession`.
4. Java uruchamia emulator 3270 i łączy go z TN3270.
5. Automat wykonuje `LOGON`, przechodzi przez ekrany TSO/ISPF i osiąga `READY`.
6. Uruchamia KICKS poleceniem `EXEC 'HERC01.CMDPROC(MBKICKS)'`.
7. Otwiera transakcję `MBGW` i wypełnia wspólną mapę API.
8. Program COBOL wykonuje operację na VSAM i wywołuje `MBRESULT`.
9. Wynik `D...S` albo `E` wraca przez spool i drukarkę TCP 5001.
10. Listener składa ramki `MBP` do rekordów `MBR` i koreluje je po `requestId`.
11. Terminal wraca do mapy `READY` i obsługuje następne żądanie.

Kontrolowane zamknięcie:

1. rozpoznanie bieżącego ekranu;
2. `Clear` tylko na rozpoznanym ekranie KICKS;
3. `KSSF`;
4. powrót do TSO `READY`;
5. `LOGOFF`;
6. potwierdzenie powrotu do ekranu logowania;
7. dopiero potem zamknięcie emulatora.

### Ograniczenie starego managera

Manager miał jeden worker, jedną sesję i jedną kolejkę. Dowolny wyjątek podczas
wykonywania żądania kończył worker, wyłączał przyjmowanie requestów i oznaczał
terminal jako niedostępny aż do ponownego uruchomienia całego backendu.

## Dostępne urządzenia Hercules

Na stronie urządzeń Hercules potwierdzono wiele urządzeń `DSP 3270`, między
innymi `00C2`, `00C3`, `00C4`, `00C5`, `00C6` oraz `03C0`–`03C7`. Urządzenie
`00C7` ma typ `3287` i jest drukarką, więc nie należy do puli terminali.

Pierwsze trzy połączenia aplikacji powinny otrzymać wolne urządzenia 3270.
Rzeczywisty przydział adresów trzeba potwierdzić po uruchomieniu sesji.

## Nowa architektura puli

- jedna globalna kolejka requestów;
- trzy workery terminalowe;
- każdy worker ma własny proces emulatora, port sterujący i użytkownika TSO;
- gotowy worker pobiera następne żądanie z kolejki;
- błąd requestu uszkadza tylko przypisaną sesję;
- bieżący request kończy się błędem, ale pozostałe requesty pozostają w kolejce;
- worker próbuje bezpiecznie zamknąć KICKS i wykonać `LOGOFF`;
- tworzy nowy emulator, wykonuje pełny start sesji i wraca do stanu `READY`;
- nieudany start jest ponawiany po konfigurowanym opóźnieniu;
- bezczynny terminal co 15 sekund potwierdza, że nadal widzi mapę `MBGW READY`;
- negatywny health-check zamyka tylko wadliwą sesję i uruchamia jej pełne
  odtworzenie;
- pozostałe terminale pracują podczas recovery jednego workera.

Planowane porty lokalnego sterowania emulatorami:

| Terminal | Port sterujący | Użytkownik TSO | TN3270 |
| --- | ---: | --- | ---: |
| `TERM-1` | `13270` | `MBKSRV1` | `13271` |
| `TERM-2` | `13272` | `MBKSRV2` | `13271` |
| `TERM-3` | `13274` | `MBKSRV3` | `13271` |

Port `13271` jest wspólnym tunelem do serwera TN3270. Porty sterujące muszą być
unikalne i nie mogą kolidować z portem tunelu.

Liczbę aktywnych workerów ustawia `MB_TERMINAL_POOL_SIZE`. Zaczynamy od wartości
`2`; po potwierdzeniu współdzielenia VSAM przełączamy ją na `3` bez zmiany listy
sesji.

## Status systemu i dashboard

Dashboard pozostaje ekranem biznesowym:

- podsumowanie zamknięcia poprzedniego dnia;
- ostatnie transakcje;
- szybkie akcje operatora.

Zakładka `System Status` otrzymuje dane techniczne:

- ogólny stan systemu;
- CPU, pamięć, uptime i stan kontenera Hercules;
- połączenie readera 3505 i drukarki wynikowej 5001;
- rozmiar wspólnej kolejki terminali;
- stan każdego terminala;
- użytkownik TSO, bieżący `requestId`, licznik recovery, ostatni błąd i czas
  ostatniego osiągnięcia `READY`.

`Audit Log` i `Settings` zostały usunięte z bieżącej nawigacji. Statystyki jobów,
liczba sukcesów i błędów z ostatnich 24 godzin oraz analiza logów będą docelowo
pochodzić ze Splunka, dlatego nie implementujemy teraz drugiego mechanizmu
analitycznego w aplikacji.

## Kroki na MVS — dziennik wykonania

### 1. Utworzenie użytkowników TSO

Status: JCL przygotowany, oczekuje na uruchomienie na MVS.

Planowane identyfikatory: `MBKSRV1`, `MBKSRV2`, `MBKSRV3`. Każdy terminal musi
mieć osobnego użytkownika. Przed wysłaniem JCL trzeba potwierdzić składnię
`ACCOUNT ADD` na TK5R i sposób ustawienia hasła.

Przygotowane zasoby:

- `jcl/application/setup/security/MBKUSERS.jcl` — trzy niezależne kroki
  `ACCOUNT ADD` oraz `ACCOUNT LIST`;
- `jcl/tools/diagnostics/security/MBKUSRCK.jcl` — kontrolne `ACCOUNT LIST` dla
  trzech użytkowników.

Do zapisania po wykonaniu:

- nazwa joba i numer JES;
- RC każdego kroku;
- wynik `ACCOUNT LIST` dla trzech użytkowników;
- pierwszy ręczny `LOGON` każdego użytkownika;
- potwierdzenie dostępu do `HERC01.CMDPROC(MBKICKS)`.

### 2. Uruchomienie dwóch sesji KICKS

Status: oczekuje na użytkowników TSO.

Sprawdzamy:

- oba emulatory łączą się jednocześnie;
- każdy dostaje inne urządzenie 3270;
- obaj użytkownicy dochodzą do mapy `MBGW READY`;
- druga instancja KICKS potrafi otworzyć pliki VSAM;
- równoległe `GETCUST` są wykonywane przez dwa różne terminale.

### 3. Uruchomienie trzeciej sesji

Status: oczekuje na wynik testu dwóch terminali.

### 4. Testy recovery

Status: oczekuje.

Scenariusze:

1. błąd rozpoznania ekranu podczas requestu;
2. zamknięcie jednego procesu emulatora;
3. utrata połączenia TN3270;
4. kontrolowany restart Spring Boot;
5. restart Hercules/MVS;
6. użytkownik pozostawiony jako `IN USE` lub `STARTINGS`;
7. niedostępny jeden użytkownik przy dwóch zdrowych terminalach.

Oczekiwany wynik: zdrowe terminale nadal obsługują kolejkę, a uszkodzony worker
przechodzi przez `RECOVERING` i sam wraca do `READY`.

## Otwarte ryzyka

### Współdzielenie VSAM przez kilka instancji KICKS

Obecne klastry VSAM są zdefiniowane z `SHAREOPTIONS(2 3)`. Trzy sesje TSO
oznaczają trzy osobne instancje KICKS. Trzeba praktycznie sprawdzić, czy druga
instancja otworzy wszystkie pliki używane przez MoniBank. Nie zmieniamy
`SHAREOPTIONS` bez wyniku tego testu, ponieważ dopuszczenie wielu writerów ma
konsekwencje dla integralności danych i synchronizacji zapisów.

### Sesja TSO pozostawiona jako `IN USE`

Zamknięcie emulatora nie jest dowodem wykonania `LOGOFF`. Standardowe recovery
najpierw próbuje kontrolowanego `KSSF` i `LOGOFF`. Jeśli MVS nadal pokazuje
użytkownika jako aktywnego, potrzebny będzie osobny, ograniczony mechanizm
operatorski: sprawdzenie stanu, `C U=<USER>`, ponowna weryfikacja i ewentualnie
`FORCE U=<USER>`. Restart całego TCP/IP nie będzie standardowym recovery
pojedynczego terminala.

Kod Hercules potwierdza, że formularz `tasks/syslog` przyjmuje parametr
`command` i przekazuje go do konsoli. Nie włączamy automatycznego `C`/`FORCE`
przed testem na żywo: najpierw rejestrujemy dokładny komunikat TK5R po zerwaniu
jednego dedykowanego użytkownika i ustalamy bezpieczny warunek wywołania.

### Korelacja wyników

Listener TCP już przechowuje oczekujące wyniki w mapie współbieżnej i rozdziela
rekordy po `requestId`. Trzeba przetestować przeplatanie ramek z dwóch lub trzech
jednoczesnych operacji, szczególnie sekwencje wielu rekordów `D` zakończone `S`.

## Źródła zewnętrzne do późniejszego wpisu

- dokumentacja Hercules: konfiguracja wielu urządzeń 3270 i HTTP server;
- dokumentacja IBM TSO `ACCOUNT`: kolejność pól `user ID`, `password`,
  `account number`, `LOGON procedure`;
- dokumentacja VSAM dotycząca `SHAREOPTIONS`;
- dokumentacja KICKS dotycząca uruchamiania wielu sesji i dostępu do plików.
