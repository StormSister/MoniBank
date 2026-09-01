---
title: Architektura Legacy Banku
description: Jak Legacy Bank w MoniBanku łączy Javę, terminal 3270, KICKS, COBOL, VSAM i JES.
outline: false
---

# Architektura Legacy Banku

Legacy Bank jest aktywnie rozwijaną gałęzią MoniBanku działającą na MVS 3.8J. Polecenie i jego wynik korzystają z dwóch osobnych kanałów integracji.

<div class="architecture-card">

```mermaid
flowchart TB
    Request["1 · Vite / Postman"]
    Api["2 · API Spring Boot"]
    Terminal["3 · TN3270 + KICKS"]
    Customer["4 · GETCUST + VSAM"]
    Result["5 · MBRESULT + JES"]
    Listener["6 · Listener TCP + parser"]
    Response["7 · CustomerResponse"]

    Request -->|"request JSON"| Api
    Api -->|"polecenie 3270"| Terminal
    Terminal -->|"transakcja online"| Customer
    Customer -->|"wynik fixed-width"| Result
    Result -->|"strumień drukarki TCP"| Listener
    Listener -->|"odpowiedź JSON"| Response

    classDef boundary fill:#2a210e,stroke:#f0c66f,color:#ffffff,stroke-width:2px;
    classDef java fill:#08272c,stroke:#38e8d0,color:#ffffff,stroke-width:2px;
    classDef mainframe fill:#071f16,stroke:#62f178,color:#ffffff,stroke-width:2px;
    classDef transport fill:#122509,stroke:#b8f36a,color:#ffffff,stroke-width:2px;

    class Request,Response boundary;
    class Api,Listener java;
    class Terminal,Customer mainframe;
    class Result transport;

    linkStyle 0,1 stroke:#38e8d0,stroke-width:3px;
    linkStyle 2,3 stroke:#62f178,stroke-width:3px;
    linkStyle 4,5 stroke:#b8f36a,stroke-width:3px;
```

<p class="architecture-caption"><strong>Ścieżka polecenia:</strong> Vite lub Postman → Java → 3270/KICKS → COBOL/VSAM<br><strong>Ścieżka wyniku:</strong> COBOL → MBRESULT/JES → Java → odpowiedź JSON</p>

</div>

Stały kanał terminalowy przekazuje polecenie online. Kanał wirtualnej drukarki JES klasy Z zwraca skorelowany wynik fixed-width do listenera TCP w Javie, gdzie dane są parsowane i mapowane na odpowiedź HTTP.

Pierwszy artykuł techniczny rozwinie ten widok i prześledzi działającą implementację **GET CUSTOMER** — klasa po klasie i program po programie.

[Zobacz kod źródłowy na GitHubie](https://github.com/StormSister/MoniBank) · [Wróć do opisu projektu](/pl/)