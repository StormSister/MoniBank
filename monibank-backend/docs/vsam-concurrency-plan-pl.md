# MoniBank: współbieżny dostęp KICKS do VSAM

## Stan po incydencie z 12 września 2026

Dwa równoległe żądania `ADDCUST` i `ADDACCT` zostały przydzielone do różnych
workerów, lecz oba przekroczyły około 25 sekund i zakończyły się
`TERMINAL_FAILURE`. W pliku wynikowym nie pojawiła się żadna ramka dla
`R3513166` ani `R3513167`. Obie sesje TSO pozostały w stanie oczekiwania na
ponowne połączenie.

To jest mocny objaw wzajemnego oczekiwania podczas operacji KICKS/VSAM, ale bez
śladu KICKS lub komunikatu systemowego wskazującego zasób nie jest jeszcze
ostatecznym dowodem deadlocku.

## Jak powstają blokady

Programy MoniBank nie wykonują jawnych `EXEC CICS ENQ` i `EXEC CICS DEQ`.
Rekord przeznaczony do zmiany jest pobierany przez `READ ... UPDATE`.

- udany `REWRITE` kończy update i zwalnia blokadę rekordu;
- ścieżka błędu używa `UNLOCK`;
- `WRITE` aktualizuje także zdefiniowane indeksy alternatywne z `UPGRADE`;
- Java nie powinna ponawiać automatycznie zapisu po utracie odpowiedzi, bo
  operacja mogła zakończyć się na MVS mimo timeoutu klienta.

Jawnego globalnego `ENQ` nie dodajemy, dopóki nie potwierdzimy zachowania tej
komendy w KICKS 1.5 używanym przez TK5R.

## Jedna kolejność zasobów

Dla operacji zapisujących obowiązuje kolejność:

1. `SEQFILE` (`MBANK.SEQ`),
2. `CUSTFILE` (`MBANK.CUST` i jego AIX),
3. `ACCTFILE` (`MBANK.ACCT` i jego AIX),
4. `CARDFILE` (`MBANK.CARD` i jego AIX),
5. `TXNFILE` (`MBANK.TXN` i jego AIX),
6. `DAYRPT` (`MBANK.DAYRPT`).

Program nie może wejść w zasób stojący wcześniej na tej liście, jeżeli wszedł
już w zasób późniejszy. Blokady zwalniamy w odwrotnej kolejności.

## Macierz operacji zapisujących

| Operacja | Program | Kolejność istotnych dostępów |
| --- | --- | --- |
| Dodanie klienta | `ADDCUSG` | `SEQFILE UPDATE` → `SEQFILE REWRITE` → `CUSTFILE WRITE` |
| Dodanie konta | `ADDACCT` | `SEQFILE UPDATE` → `CUSTFILE READ` → `SEQFILE REWRITE` → `ACCTFILE WRITE` |
| Dodanie karty | `ADDCARD` | `SEQFILE UPDATE` → `ACCTFILE READ` → `SEQFILE REWRITE` → `CARDFILE WRITE` |
| Księgowanie | `POSTTXN` | `SEQFILE UPDATE` → `ACCTFILE UPDATE` → `SEQFILE REWRITE` → `TXNFILE WRITE` → `ACCTFILE REWRITE` |
| Naliczanie odsetek | `POSTINT` | `ACCTFILE UPDATE` → `TXNFILE WRITE` → `ACCTFILE REWRITE` |
| Zmiana klienta | `CHGCUST` | `CUSTFILE UPDATE` → `CUSTFILE REWRITE` |
| Zmiana konta | `CHGACCT` | `ACCTFILE UPDATE` → opcjonalny `CUSTFILE READ` → `ACCTFILE REWRITE` |
| Zmiana karty | `CHGCARD` | `CARDFILE UPDATE` → opcjonalny `ACCTFILE READ` → `CARDFILE REWRITE` |
| Zamknięcie dnia | `DAYSTAT` | zakończone browse'y `TXNID` i `CUSTFILE` → `DAYRPT WRITE/UPDATE` |

`CHGACCT` i `CHGCARD` mają tylko jedną blokadę `UPDATE`; dodatkowy odczyt służy
walidacji encji nadrzędnej. Trzeba je objąć testami mieszanymi, ponieważ KICKS
działa w kilku osobnych sesjach TSO, a nie w jednym regionie CICS.

## Zmiany w tej wersji

- `ADDACCT`: przeniesiono blokadę `SEQFILE` przed odczyt `CUSTFILE`;
- `ADDCARD`: przeniesiono blokadę `SEQFILE` przed odczyt `ACCTFILE`;
- `POSTTXN`: przeniesiono blokadę `SEQFILE` przed blokadę `ACCTFILE`;
- `POSTTXN`: awaryjne zwalnianie odbywa się w odwrotnej kolejności,
  `ACCTFILE` przed `SEQFILE`;
- dodano test regresyjny kolejności najważniejszych bloków w źródłach COBOL.

Test z 13 września, wykonany po kompilacji i ponownym uruchomieniu obu sesji
KICKS, ponownie zakończył równoległe `ADDCUST` i `ADDACCT` po około 25 sekundach
kodem `TERMINAL_FAILURE`. Kontrolne `LISTCUST` i `LISTACCT` potwierdziły, że
żaden z dwóch rekordów nie został zapisany. Sama kolejność dostępu ogranicza
ryzyko deadlocku, ale nie zapewnia koordynacji pomiędzy niezależnymi regionami
KICKS uruchomionymi w osobnych sesjach TSO.

Dlatego Java używa teraz jednego sprawiedliwego koordynatora dostępu:

- `GETCUST`, `LISTCUST`, `LISTACCT`, `LISTCARD`, `LISTTXN` i `GETSTAT` mogą
  działać równolegle pod współdzieloną blokadą odczytu;
- każda operacja modyfikująca oraz każda nieznana operacja dostaje wyłączną
  blokadę zapisu;
- worker jest wybierany dopiero po uzyskaniu blokady, więc oczekujący zapis nie
  zajmuje bezczynnego terminala;
- pełne zamknięcie dnia obejmuje jedna blokada, także pomiędzy `POSTINT` i
  `DAYSTAT`;
- liczba oczekujących na koordynator jest doliczana do `queuedRequests` w
  statusie systemu.

Jeżeli kanał terminalowy zgłosi błąd już po wykonaniu COBOL-a, Java nie zwraca
od razu `503`. Najpierw próbuje odebrać kompletny wynik z wcześniej
zarejestrowanego kanału TCP. Poprawnie skorelowane końcowe `MBR;S` lub `MBR;E`
jest wynikiem autorytatywnym. Jeśli końcowy rekord nie nadejdzie, zachowywany
jest pierwotny błąd terminala, a błąd odbioru wyniku trafia do niego jako
diagnostyka dodatkowa. Nie powoduje to automatycznego ponowienia zapisu.

Sterownik 3270 rozpoznaje zakończenie również po skróconym nagłówku wyniku
widocznym na mapie MBGW. Akceptuje wyłącznie `MBR;S` lub `MBR;E`, które zawiera
zarówno bieżącą nazwę operacji, jak i jej unikalny `requestId`. Nie musi czekać
na osobne odświeżenie pól `STATE` i `RESULT FOR`, a wynik pozostały po starszym
żądaniu nie może zostać omyłkowo dopasowany.

Blokada jest lokalna dla procesu Java. Wdrożenie produkcyjne zakłada jedną
aktywną instancję backendu. Przy uruchomieniu kilku replik trzeba zastąpić ją
blokadą rozproszoną albo wyznaczyć jednego właściciela operacji legacy.

## Wdrożenie na TK5R

Zmiana pliku w projekcie nie zmienia programu już załadowanego przez KICKS.
Po przesłaniu źródeł trzeba:

1. wgrać `ADDACCT`, `ADDCARD` i `POSTTXN` do biblioteki źródłowej;
2. skompilować wszystkie trzy programy do właściwego `KIKRPL`;
3. zamknąć kontrolowanie sesje KICKS (`KSSF`), aby nie używały starych kopii;
4. uruchomić ponownie workery i potwierdzić `MBGW READY`;
5. sprawdzić stan danych po poprzednim timeoutie przed ponowieniem zapisu.

## Test akceptacyjny

Każda próba musi zapisać dwa różne `requestId`, dwóch executorów i końcowy stan
obu terminali `READY`.

1. `ADDCUST + ADDACCT` — powtórzenie scenariusza, który zawisł.
2. `ADDACCT + ADDACCT` dla dwóch różnych klientów.
3. `ADDCARD + POSTTXN`.
4. `POSTTXN + POSTTXN` dla różnych kont.
5. `POSTTXN + POSTTXN` dla tego samego konta — drugi zapis czeka w Javie;
   oczekiwane są dwa poprawne salda końcowe.
6. `CHGACCT(A) + ADDCUST` oraz `CHGCARD(A) + POSTTXN`.
7. Jedno żądanie z błędem biznesowym równolegle z poprawnym zapisem.

Po każdym teście sprawdzamy API, dane VSAM, dziennik operacji oraz status puli.
Timeout zapisu nie jest podstawą do automatycznego retry. Najpierw odczytujemy
stan encji po identyfikatorze biznesowym lub `requestId`.
