---
title: Legacy Bank architecture
description: How the MoniBank Legacy Bank connects Java, 3270, KICKS, COBOL, VSAM and JES.
outline: false
---

# Legacy Bank architecture

Legacy Bank is the actively developed MVS 3.8J branch of MoniBank. The command and its result use two separate integration channels.

<div class="architecture-card">

```mermaid
flowchart TB
    Request["1 · Vite / Postman"]
    Api["2 · Spring Boot API"]
    Terminal["3 · TN3270 + KICKS"]
    Customer["4 · GETCUST + VSAM"]
    Result["5 · MBRESULT + JES"]
    Listener["6 · TCP listener + parser"]
    Response["7 · CustomerResponse"]

    Request -->|"JSON request"| Api
    Api -->|"3270 command"| Terminal
    Terminal -->|"online transaction"| Customer
    Customer -->|"fixed-width result"| Result
    Result -->|"TCP printer stream"| Listener
    Listener -->|"JSON response"| Response

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

<p class="architecture-caption"><strong>Command path:</strong> Vite or Postman → Java → 3270/KICKS → COBOL/VSAM<br><strong>Result path:</strong> COBOL → MBRESULT/JES → Java → JSON response</p>

</div>

The persistent terminal channel carries the online command. The JES class-Z virtual-printer channel returns the correlated fixed-width result to the Java TCP listener, where it is parsed and mapped to the HTTP response.

The first technical article will expand this overview and trace the working **GET CUSTOMER** implementation class by class and program by program.

[View the source code on GitHub](https://github.com/StormSister/MoniBank) · [Back to the project overview](/)