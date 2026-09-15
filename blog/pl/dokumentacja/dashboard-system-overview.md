---
title: Górny panel dashboardu
description: Skąd pochodzi każda wartość w górnym pasku i panelu stanu systemu MoniBanku.
---

# Górny panel dashboardu

Górna część dashboardu łączy trzy różne rodzaje informacji: stan przeglądarki, telemetrię infrastruktury zebraną przez Javę oraz dziennik operacji backendu. Otwarcie tej strony **nie uruchamia programu biznesowego COBOL**.

## 1. Co widzi operator

W górnym pasku pokazuję:

- powitanie zależne od pory dnia;
- statyczną etykietę `Legacy Bank (MVS 3.8j)`;
- aktualny ogólny status;
- przycisk otwierający lub zamykający konsolę live.

Pierwsza karta dashboardu dodaje:

- nazwę i identyfikator systemu;
- liczbę gotowych workerów terminalowych;
- liczbę żądań czekających na wykonanie albo blokadę VSAM;
- stan readera i drukarki wynikowej;
- czas ostatniego sprawdzenia;
- podsumowanie powodzeń i błędów operacji z ostatnich 24 godzin.

## 2. Kod frontendu

Oba elementy renderuję w [`Topbar.jsx`](https://github.com/StormSister/MoniBank/blob/main/monibank-frontend/src/components/layout/Topbar.jsx) i [`DashboardPage.jsx`](https://github.com/StormSister/MoniBank/blob/main/monibank-frontend/src/pages/DashboardPage.jsx).

Powitanie obliczam na podstawie `new Date().getHours()` w przeglądarce. Korzysta więc z lokalnego zegara operatora, a nie z czasu MVS ani backendu. Selektor banku jest obecnie etykietą wizualną — nie przełącza jeszcze pomiędzy różnymi systemami core.

Otwarcie konsoli live zmienia stan Reacta w `AppLayout`. Sama konsola korzysta z osobnego połączenia SSE i nie jest częścią żadnej z dwóch odpowiedzi opisanych poniżej.

## 3. Żądania HTTP

Dane dynamiczne pobierają dwa hooki TanStack Query:

| Hook | Żądanie | Odświeżanie |
|---|---|---:|
| [`useMainframeStatus`](https://github.com/StormSister/MoniBank/blob/main/monibank-frontend/src/hooks/useMainframeStatus.js) | `GET /api/mainframe/status` | co 15 sekund |
| [`useOperationSummary`](https://github.com/StormSister/MoniBank/blob/main/monibank-frontend/src/hooks/useOperations.js) | `GET /api/operations/summary?hours=24` | co 15 sekund |

Zapytanie o status ma dziesięciosekundowe `staleTime`. Interwały są pollingiem frontendu; nie oznaczają wywoływania transakcji biznesowej na MVS co 15 sekund.

## 4. Obsługa w Javie

[`MainframeStatusController`](https://github.com/StormSister/MoniBank/blob/main/monibank-backend/src/main/java/com/monibank/mainframe/api/MainframeStatusController.java) zwraca ostatni snapshot przygotowany przez [`MainframeStatusService`](https://github.com/StormSister/MoniBank/blob/main/monibank-backend/src/main/java/com/monibank/mainframe/hercules/MainframeStatusService.java).

`MainframeStatusService` odświeża go według skonfigurowanego interwału metryk — domyślnie co 15 sekund. Łączy kilka niezależnych źródeł zamiast ufać jednej globalnej fladze zdrowia.

[`OperationController`](https://github.com/StormSister/MoniBank/blob/main/monibank-backend/src/main/java/com/monibank/mainframe/operations/OperationController.java) przekazuje podsumowanie 24 godzin do [`OperationQueryService`](https://github.com/StormSister/MoniBank/blob/main/monibank-backend/src/main/java/com/monibank/mainframe/operations/OperationQueryService.java).

## 5. Czy żądanie wchodzi do legacy core

Żadne z tych dwóch żądań dashboardu nie wykonuje `LINK` do programu COBOL.

Sprawdzenie readera otwiera skonfigurowany transport, żeby potwierdzić dostępność. Stan drukarki pochodzi z już działającego listenera TCP, a informacje o terminalach z menedżera sesji w Javie. Metryki Herculesa pobieram przez `docker inspect` i `docker stats` — lokalnie albo przez skonfigurowane, ograniczone polecenie SSH.

Podsumowanie operacji czyta dziennik Javy. Nie uruchamia `LISTTXN`, nie przegląda VSAM-u i nie odczytuje bieżącego ekranu sesji 3270.

## 6. Źródło każdej wartości

| Wartość w UI | Autorytatywne źródło |
|---|---|
| Powitanie | Lokalna godzina przeglądarki |
| `Legacy Bank (MVS 3.8j)` | Statyczna etykieta frontendu |
| `MVS 3.8j` i `TK5R` | Wartości przypisane przez `MainframeStatusService` |
| Ogólny status | Reguła obliczana przez `MainframeStatusService` |
| Terminale ready/configured/busy/recovering | Snapshoty z `KicksTerminalSessionManager` |
| Kolejka | Rozmiar kolejki terminali plus wątki czekające w `VsamAccessCoordinator` |
| Reader | Okresowy test `MainframeGateway.isAvailable()` |
| Drukarka wynikowa | `MainframeTcpResultListener.isConnected()` |
| CPU, pamięć i uptime Herculesa | Metryki runtime Dockera |
| Operacje successful/business/technical | Dopisywany dziennik JSONL |
| Ostatnie sprawdzenie | Chwila złożenia snapshotu przez Javę |

Reader jest sprawdzany najwyżej raz na minutę, mimo że cały snapshot jest zwykle odświeżany co 15 sekund. Dzięki temu dashboard nie otwiera czterech połączeń z readerem na minutę tylko po to, żeby odmalować status.

## 7. Znaczenie statusów i odświeżania

Backend zwraca:

- `ONLINE`, gdy kontener Herculesa działa, reader i drukarka są połączone, a wszystkie skonfigurowane terminale są operacyjne;
- `DEGRADED`, gdy działa przynajmniej część integracji, ale nie są spełnione wszystkie warunki online;
- `OFFLINE`, gdy kontener na pewno nie działa albo nie jest dostępny żaden sygnał integracji;
- `CHECKING`, zanim powstanie pierwszy snapshot.

Frontend używa `UNAVAILABLE`, gdy nie powiedzie się zapytanie HTTP. To zachowanie awaryjne interfejsu, a nie status zwracany przez backend.

Pierścień operacji grupuje wpisy jako `SUCCESS`, `BUSINESS_ERROR` i `TECHNICAL_ERROR`. Mały panel łączy dwa rodzaje błędów w jedną liczbę failed, natomiast strona Operations zachowuje ich rozróżnienie.

## 8. Awarie i prawidłowa interpretacja

Metryki runtime mogą być nieaktualne, mimo że reader, drukarka i terminale nadal działają. Odpowiedź ma wtedy `metricsStale: true` i — jeżeli istnieje — zachowuje poprzednią próbkę runtime.

Zero w pierścieniu operacji nie dowodzi, że MVS nie wykonał żadnej pracy. Oznacza, że wybrany dziennik Javy nie zawiera śledzonych zdarzeń z ostatnich 24 godzin. Nowy albo niedostępny wolumen dziennika może więc dać `0 successful / 0 failed`, mimo że mainframe jest online.

Podobnie `CONNECTED` przy drukarce potwierdza aktywne połączenie transportowe. Nie dowodzi powodzenia konkretnej operacji COBOL — do tego potrzebny jest skorelowany z requestem końcowy rekord `MBR;S`.

## Czym ten panel jest — a czym nie jest

Zaprojektowałam go jako skrót stanu infrastruktury, a nie księgę bankową. Liczba klientów, salda, transakcje i podsumowanie poprzedniego dnia mają inne ścieżki danych i opiszę je osobno. To rozdzielenie pozwala jednoznacznie wskazać, czy liczba pochodzi z VSAM-u, trwałego raportu dziennego, rekordu MBR czy wyłącznie z telemetrii Javy.

