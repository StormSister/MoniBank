---
title: Bramka MBGATE
description: Jak transakcja terminalowa MBGW waliduje żądanie, buduje wspólną COMMAREA i kieruje je do programu COBOL MoniBanku.
---

# Bramka MBGATE

`MBGW` i `MBGATE` tworzą wspólny punkt wejścia online używany przez workery terminalowe Javy. `MBGW` jest czteroznakowym identyfikatorem transakcji wpisywanym w KICKS, a `MBGATE` — przypisanym do niej programem COBOL.

## Dane z terminala

Mapa BMS przyjmuje następującą kopertę protokołu:

| Pole | Długość | Znaczenie |
|---|---:|---|
| Operacja | 8 | nazwa logiczna, np. `GETCUST` lub `ADDCUST` |
| Request ID | 8 | identyfikator korelacyjny utworzony przez Javę |
| Długość wejścia | 4 | wartość dziesiętna, np. `0105` |
| Wejście | 512 | payload fixed-width właściwy dla operacji |

Obszar wejścia zajmuje osiem 64-znakowych pól ekranu. `MBGATE` łączy je, zamienia niskie wartości BMS na spacje i odrzuca brak operacji, nieprawidłowy request ID, nienumeryczną długość albo wejście przekraczające 512 znaków.

## Wspólna COMMAREA

Po walidacji ekranu `MBGATE` wypełnia 855-bajtową [`MBGWCA`](https://github.com/StormSister/MoniBank/blob/main/monibank-backend/src/main/resources/jcl/application/build/kicks/PUTGWCA.jcl):

| Część | Rozmiar | Rola |
|---|---:|---|
| Wersja protokołu | 2 | obecnie `01` |
| Request ID | 8 | korelacja wyniku |
| Operacja | 8 | routing i kontrola odpowiedzi |
| Długość wejścia | 4 | liczba używanych znaków |
| Wejście | 512 | payload programu biznesowego |
| Liczba danych | 1 | zero albo jeden rekord podglądu |
| Rekord danych | 160 | opcjonalny podgląd `MBR;D` |
| Rekord nagłówka | 160 | końcowy podgląd `MBR;S` albo `MBR;E` |

Mapa jest tylko kopertą transportową. Układ danych biznesowych należy do programu docelowego i tam jest ponownie sprawdzany.

## Routing

[`MBGATE.cob`](https://github.com/StormSister/MoniBank/blob/main/monibank-backend/src/main/resources/cobol/application/gateway/MBGATE.cob) wybiera program z jawnej listy i wywołuje go przez `EXEC CICS LINK`:

| Obszar | Operacje |
|---|---|
| Klienci | `GETCUST`, `ADDCUST` → `ADDCUSG`, `LISTCUST`, `CHGCUST` |
| Konta | `LISTACCT`, `ADDACCT`, `CHGACCT` |
| Karty | `LISTCARD`, `ADDCARD`, `CHGCARD` |
| Transakcje | `POSTTXN`, `LISTTXN` |
| Przetwarzanie dnia | `POSTINT`, `DAYSTAT`, `GETSTAT` |

Nieznana nazwa nie jest wykonywana dynamicznie. Operacja spoza listy kończy się ekranem błędu.

## Kontrola odpowiedzi i ochrona przed powtórzeniem

Po powrocie z programu `MBGATE` wymaga:

- prefiksu `MBR`;
- zgodnego request ID i nazwy operacji;
- końcowego typu `S` albo `E`;
- najwyżej jednego poprawnego rekordu podglądu `D` w COMMAREA.

Pełne wyniki operacji listujących przechodzą przez `MBRESULT`, a nie przez pojedynczy podgląd terminalowy.

Przed `LINK` bramka zmienia jednobajtowy znacznik powrotu z `Y` na `R`. Ponowne zatwierdzenie tego samego ekranu wyniku rozpoczyna nowy formularz zamiast wykonywać poprzedni zapis po raz drugi. Niepoprawna odpowiedź pokazuje jednoznaczne ostrzeżenie `DO NOT RETRY WRITES`.

## Cykl terminala

Przy pierwszym wejściu `MBGATE` pokazuje `READY`. Po wykonaniu wyświetla `SUCCESS` albo `ERROR`, request ID oraz podgląd zwróconych rekordów. Enter przygotowuje nowe żądanie, a Clear wychodzi z transakcji.

Java uznaje oczekiwaną mapę `MBGW` za stan gotowości workera. STEVE albo SOFIA wypełnia pola i czeka na skorelowane potwierdzenie, zanim sesja wróci do wspólnej kolejki.

Szczegółowy przykład znajduje się w artykule [Integracja GET CUSTOMER](../articles/get-customer). Transport odpowiedzi opisuje [MBRESULT i kanał wynikowy drukarki](./mbresult).

