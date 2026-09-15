---
title: Kanał wynikowy MBRESULT
description: Co otrzymuje MBRESULT, jak tworzy ramki spool klasy Z i co odtwarza listener TCP w Javie.
---

# Kanał wynikowy MBRESULT

`MBRESULT` jest wspólnym programem KICKS wysyłającym ustrukturyzowane wyniki MoniBanku do JES. Program biznesowy buduje wynik, natomiast `MBRESULT` odpowiada za operacje na spoolu.

## Co otrzymuje

Program biznesowy wywołuje `MBRESULT` przez `LINK` z 185-bajtową [`MBRSCA`](https://github.com/StormSister/MoniBank/blob/main/monibank-backend/src/main/resources/cobol/application/result/MBRSCA.cpy):

| Pole | Rozmiar | Rola |
|---|---:|---|
| Sterowanie | 1 | `M` oznacza kolejne rekordy, `F` — rekord końcowy |
| Token | 8 | identyfikuje jeden otwarty wydruk |
| Kod zwrotny | 8 | `OK` albo błąd programu |
| RESP / RESP2 | 4 + 4 | binarne dane diagnostyczne KICKS |
| Rekord logiczny | 160 | jeden rekord wyniku `MBR` |

Rekord logiczny jest rekordem danych (`MBR;D`) albo końcowym nagłówkiem sukcesu/błędu (`MBR;S` / `MBR;E`). Zawiera ten sam ośmioznakowy request ID, którego używają Java i `MBGATE`.

## Co robi

1. Wymaga COMMAREA o dokładnej długości 185 bajtów oraz sterowania `M` albo `F`.
2. Jeśli token jest pusty, otwiera jeden SYSOUT klasy `Z` i zwraca token wywołującemu programowi.
3. Dzieli logiczny rekord 160-znakowy na dwa payloady po 80 znaków.
4. Zapisuje oba jako fizyczne ramki `MBP` z request ID i numerem części.
5. Dla `M` pozostawia SYSOUT otwarty. Końcowe `F` zamyka go i czyści token.

Format fizyczny wygląda tak:

```text
MBP;<request-id>;<część>;<80-znakowy payload>
```

Wraz z początkowym znakiem sterowania wydrukiem fizyczna linia ma 96 bajtów. Podział był konieczny, ponieważ ścieżka online spool nie przyjmowała pierwotnego 160-znakowego wyniku logicznego jako jednej linii fizycznej.

## Co z niego wychodzi

`MBRESULT` nie zwraca biznesowego JSON-u. Jego wynikiem są:

- dwie ramki spool klasy Z dla każdego logicznego rekordu `MBR`;
- token pozwalający utrzymać jeden wynik otwarty przez wiele rekordów danych;
- `OK` albo diagnostyka, np. `OPENERR`, `WRIT1ERR`, `WRIT2ERR`, `CLOSERR` lub `BADCTRL`, wraz z RESP/RESP2.

Hercules udostępnia strumień drukarki klasy Z na skonfigurowanym urządzeniu TCP. [`MainframeTcpResultListener`](https://github.com/StormSister/MoniBank/blob/main/monibank-backend/src/main/java/com/monibank/mainframe/hercules/MainframeTcpResultListener.java) łączy część 1 i 2, sprawdza request ID i odtwarza pierwotny rekord 160-znakowy. Gromadzi rekordy `D`, a oczekujące żądanie Javy kończy dopiero po rekordzie `S` albo `E`.

## Granica odpowiedzialności

Program biznesowy decyduje, czy wynik oznacza sukces czy błąd, i dostarcza dane. `MBRESULT` nie czyta VSAM-u i nie wybiera statusu HTTP. Jego zadaniem jest ramkowanie i dostarczenie wyniku ze środowiska KICKS do listenera Javy.

