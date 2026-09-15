---
title: Dokumentacja MoniBanku
description: Oparta na kodzie mapa MoniBanku — od panelu operatora do programów COBOL i danych VSAM.
---

# Dokumentacja MoniBanku

W tej sekcji wyjaśniam, skąd biorą się dane widoczne w MoniBanku i co dzieje się pomiędzy akcją operatora a ostatecznym wynikiem zwróconym przez MVS.

Dokumentację opieram na implementacji znajdującej się w repozytorium. Oddzielam zachowanie potwierdzone kodem od obserwacji z mojego działającego systemu TK5R. Jeżeli mechanizm jest dopiero planowany, eksperymentalny albo nie uczestniczy w aktualnym przepływie aplikacji, zaznaczam to zamiast przedstawiać go jako ukończony.

## Osiem pytań dla każdego przepływu

Każdy ekran i każdą operację opisuję według tego samego schematu:

1. **Co widzi operator?** — pole, akcję, status albo błąd widoczny w interfejsie.
2. **Który kod frontendu za to odpowiada?** — komponent React, hook, klucz zapytania i sposób odświeżania.
3. **Jaka jest granica HTTP?** — endpoint, parametry i model odpowiedzi.
4. **Które klasy Java obsługują żądanie?** — kontroler, serwis, mapper i komponenty integracyjne.
5. **Czy żądanie wchodzi do legacy core?** — kolejka terminali, `MBGW`, `MBGATE` i COMMAREA albo jednoznaczna informacja, że ten element nie wywołuje COBOL-a.
6. **Gdzie znajduje się autorytatywna wartość?** — w VSAM-ie, rekordzie wyniku MVS, dzienniku operacji, telemetrii runtime albo stanie przeglądarki.
7. **Jak wynik wraca i jest odświeżany?** — przez `MBRESULT`, JES/drukarkę, korelację TCP, polling, SSE albo cache.
8. **Co dzieje się przy awarii?** — rozróżnienie błędu biznesowego, technicznego, nieaktualnej próbki i zachowania awaryjnego interfejsu.

Dzięki temu nie opisuję etykiety z interfejsu jako „danej z mainframe’u”, jeżeli naprawdę jest tekstem statycznym, stanem przeglądarki albo telemetrią Javy.

## Mapa dokumentacji

| Warstwa | Zakres | Status |
|---|---|---|
| Mapa aplikacji | Pełna droga żądania i wyniku | Planowana |
| Źródła dashboardu | Status, operacje, raport poprzedniego dnia i ostatnie transakcje | W trakcie |
| Protokół bramki | `MBGW`, `MBGATE`, COMMAREA 855 bajtów i wybór programu | Dostępna |
| Protokół wyniku | `MBR;D`, `MBR;S`, `MBR;E`, `MBRESULT` i ramki drukarki | Dostępna |
| Listener Java | TCP, składanie ramek, korelacja requestów i timeout | Planowana |
| Operacje biznesowe | Dodanie klienta, konta, karty, wpłaty, wypłaty i wyciągi | W trakcie |
| Zamknięcie dnia | `POSTINT`, `DAYSTAT`, `GETSTAT` i `MBANK.DAYRPT` | Planowana |
| Runtime | STEVE, SOFIA, wspólna kolejka, recovery i koordynacja VSAM | Planowana |
| Dostarczanie na MVS | Programy COBOL, copybooki, CLIST-y i joby JCL w wymaganej kolejności | Dostępna |
| Mapa repozytorium | Kod aktywny, pomocniczy i obecnie nieużywany | Planowana |

## Dostępne teraz

- [Górny panel dashboardu: skąd pochodzą wyświetlane dane](./dashboard-system-overview)
- [Podsumowanie poprzedniego dnia: jak MVS tworzy i zwraca raport](./podsumowanie-poprzedniego-dnia)
- [Ostatnie transakcje: dlaczego panel pięciu rekordów czyta pełny wynik VSAM](./ostatnie-transakcje)
- [Szybkie akcje dashboardu: najpierw trasa, operacja dopiero po wysłaniu formularza](./szybkie-akcje)
- [MBGATE: wspólna bramka KICKS i routing operacji](./mbgate)
- [MBRESULT: kanał wynikowy drukarki klasy Z](./mbresult)
- [Instalacja na MVS: mapy, copybooki, tabele KICKS i MBKICKS](./instalacja-mvs)
- [Dodanie klienta: od formularza operatora do rekordu VSAM](./dodaj-klienta)

## Stabilne linki dla aplikacji {#frontend-links}

Linki z frontendu powinny prowadzić do poniższych tras stron. Te slugi traktujemy jak publiczny kontrakt i nie zmieniamy ich bez równoczesnej aktualizacji aplikacji.

| Klucz funkcji | Ścieżka polska | Ścieżka angielska |
| --- | --- | --- |
| `system-overview` | `/blog/pl/dokumentacja/dashboard-system-overview` | `/blog/documentation/dashboard-system-overview` |
| `previous-day-close` | `/blog/pl/dokumentacja/podsumowanie-poprzedniego-dnia` | `/blog/documentation/previous-day-close-summary` |
| `recent-transactions` | `/blog/pl/dokumentacja/ostatnie-transakcje` | `/blog/documentation/recent-transactions` |
| `quick-actions` | `/blog/pl/dokumentacja/szybkie-akcje` | `/blog/documentation/quick-actions` |
| `mbgate` | `/blog/pl/dokumentacja/mbgate` | `/blog/documentation/mbgate` |
| `mbresult` | `/blog/pl/dokumentacja/mbresult` | `/blog/documentation/mbresult` |
| `mvs-installation` | `/blog/pl/dokumentacja/instalacja-mvs` | `/blog/documentation/mvs-installation` |
| `add-customer` | `/blog/pl/dokumentacja/dodaj-klienta` | `/blog/documentation/add-customer` |

Jawnych kotwic, takich jak `#refresh`, używamy tylko wtedy, gdy link ma otworzyć konkretną sekcję. Nie linkujemy automatycznie generowanych, zależnych od języka identyfikatorów nagłówków.

## Źródło prawdy

Przy każdym rozdziale wskazuję odpowiednie pliki w [repozytorium MoniBanku](https://github.com/StormSister/MoniBank). Zrzuty ekranu i logi runtime mogą potwierdzić, że dany przepływ zadziałał w konkretnym momencie, ale nie zastępują implementacji jako dowodu powtarzalnego zachowania.
