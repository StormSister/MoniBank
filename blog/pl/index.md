---
layout: home

hero:
  name: MoniBank
  text: Core banking na styku pokoleń technologii
  tagline: Edukacyjny system bankowy łączący nowoczesną aplikację Java i Vite ze światem MVS, KICKS, COBOL-a, VSAM-u i JES-a.
  actions:
    - theme: brand
      text: Poznaj projekt
      link: /pl/#projekt
    - theme: alt
      text: Status prac
      link: /pl/#rozwoj

features:
  - icon: 'MVS'
    title: Legacy Bank
    details: IN DEVELOPMENT · Środowisko bankowe budowane na MVS 3.8J, Herculesie, KICKS, COBOL-u, VSAM-ie i JES-ie.
    link: /pl/legacy/
  - icon: 'IBM Z'
    title: Modern Bank
    details: PLANNED · Przyszła implementacja na IBM Z porównująca starsze i współczesne podejście do mainframe’u.
  - icon: 'API'
    title: Nowoczesna warstwa aplikacyjna
    details: Spring Boot tworzy granicę HTTP i orkiestrację, a Vite zasila panel operatora.
  - icon: '3270'
    title: Prawdziwe laboratorium integracji
    details: Automatyzacja terminala, transakcje online, rekordy fixed-width i asynchroniczne wyniki.
---

<section id="projekt" class="project-section">

## Jeden projekt, dwie epoki mainframe’u

MoniBank jest edukacyjnym projektem core bankingowym. Powstaje po to, aby sprawdzić w praktyce, jak operacje bankowe mogą przechodzić przez granicę pomiędzy współczesną aplikacją webową a klasyczną technologią mainframe.

To nie jest symulacja złożona wyłącznie ze zrzutów ekranu. Gałąź Legacy Bank działa na **MVS 3.8J uruchomionym w Herculesie**. Java steruje stałą sesją 3270, KICKS kieruje transakcjami online, COBOL pracuje z danymi VSAM, a JES zwraca ustandaryzowane wyniki przez połączenie wirtualnej drukarki.

Modern Bank jest planowany jako przyszła implementacja na IBM Z. Pozwoli porównać ograniczenia i decyzje architektoniczne środowiska legacy ze współczesnymi sposobami integracji mainframe’u.

</section>

<section class="track-grid">

<article class="track-card legacy">
<div class="track-heading"><span>Legacy Bank</span><strong>IN DEVELOPMENT</strong></div>

### Laboratorium MVS 3.8J

- emulator Hercules i środowisko TK5
- automatyczna sesja TN3270 i TSO
- transakcje online KICKS
- programy biznesowe COBOL i kluczowe zbiory VSAM
- transport wyników przez wirtualną drukarkę JES
- API Java/Spring i panel operatora w Vite

Pierwszy pełny przepływ online, **GET CUSTOMER**, działa end to end. Szersza aplikacja bankowa nadal jest rozwijana.
[Zobacz architekturę Legacy Banku →](/pl/legacy/)
</article>

<article class="track-card modern">
<div class="track-heading"><span>Modern Bank</span><strong>PLANNED</strong></div>

### Przyszła gałąź IBM Z

- środowisko IBM Z
- współczesne przetwarzanie transakcji online
- nowoczesne interfejsy integracyjne mainframe
- odpowiadające sobie przypadki użycia bankowego
- bezpośrednie porównanie z Legacy Bankiem

Architekturę doprecyzujemy, gdy fundament Legacy Banku będzie wystarczająco kompletny.
</article>

</section>

<section id="rozwoj" class="project-section">

## Aktualny status prac

MoniBank jest aktywnie rozwijanym projektem portfolio i nauki. Ukończone przepływy pokazujemy na prawdziwym kodzie i logach, a niedokończone elementy wyraźnie oznaczamy.

| Obszar | Status | Obecny zakres |
|---|---|---|
| Środowisko legacy | In development | MVS 3.8J uruchomiony pod Herculesem |
| Integracja Java | In development | Spring Boot, stały worker terminalowy i listener TCP |
| GET CUSTOMER | Working end to end | JSON → 3270/KICKS → COBOL/VSAM → JES → JSON |
| Operacje bankowe | In development | klienci, konta, karty, wpłaty, wypłaty i wyciągi |
| Panel operatora | In development | dashboard Vite i podgląd pracy mainframe’u |
| Pierwszy artykuł techniczny | Weryfikowany | dzisiaj sprawdzamy go z ukończonym endpointem |
| Modern Bank | Planned | przyszła gałąź IBM Z |

</section>

<section class="project-section">

## Dlaczego powstaje MoniBank

1. **Żeby uczyć się inżynierii mainframe przez budowę pełnego systemu**, a nie wyłącznie pojedyncze ćwiczenia COBOL.
2. **Żeby pokazać granicę pomiędzy pokoleniami technologii**, również z niewygodnymi detalami, które często znikają z eleganckich diagramów.
3. **Żeby stworzyć uczciwe portfolio techniczne**, w którym decyzje, awarie i poprawki można prześledzić w kodzie, artykułach i filmie.

Pierwszy artykuł techniczny przeprowadzi czytelnika przez jeden prawdziwy request GET CUSTOMER: od Postmana do MVS i z powrotem. Przed publikacją sprawdzimy każdą pokazaną klasę, program, strukturę rekordu i linię logu z działającą implementacją.

</section>

<section class="project-section">

## Masz pytanie dotyczące MoniBanku?

Zauważyłeś nieścisłość techniczną, masz pytanie albo chcesz porozmawiać o architekturze projektu?

[Znajdź mnie na LinkedIn →](https://www.linkedin.com/in/monika-gudalewska/) · [Zobacz repozytorium MoniBank →](https://github.com/StormSister/MoniBank)

</section>
