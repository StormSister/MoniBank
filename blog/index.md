---
layout: home

hero:
  name: MoniBank
  text: Core banking across generations
  tagline: An educational banking system connecting a modern Java and Vite application with MVS, KICKS, COBOL, VSAM and JES.
  actions:
    - theme: brand
      text: Explore the project
      link: /#project
    - theme: alt
      text: Development status
      link: /#development

features:
  - icon: 'MVS'
    title: Legacy Bank
    details: IN DEVELOPMENT · A banking environment built on MVS 3.8J, Hercules, KICKS, COBOL, VSAM and JES.
    link: /legacy/
  - icon: 'IBM Z'
    title: Modern Bank
    details: PLANNED · A future IBM Z implementation comparing legacy and contemporary mainframe approaches.
  - icon: 'API'
    title: Modern application layer
    details: Spring Boot provides the HTTP boundary and orchestration, while Vite powers the operator interface.
  - icon: '3270'
    title: A real integration laboratory
    details: Automated terminal sessions, online transactions, fixed-width records and asynchronous results.
---

<section id="project" class="project-section">

## One project, two eras of mainframe computing

MoniBank is an educational core banking project exploring how banking operations can cross the boundary between a modern web application and classic mainframe technology.

The project is not a screenshot-only simulation. Its Legacy Bank branch runs on **MVS 3.8J under Hercules**. Java controls a persistent 3270 session, KICKS dispatches online transactions, COBOL works with VSAM data, and JES returns structured results through a virtual printer connection.

Modern Bank is planned as a future implementation for IBM Z. It will allow the constraints and design choices of the legacy environment to be compared with contemporary mainframe integration patterns.

</section>

<section class="track-grid">

<article class="track-card legacy">
<div class="track-heading"><span>Legacy Bank</span><strong>IN DEVELOPMENT</strong></div>

### MVS 3.8J laboratory

- Hercules emulator and TK5 environment
- automated TN3270 and TSO session
- KICKS online transactions
- COBOL business programs and keyed VSAM
- JES virtual-printer result transport
- Java/Spring API and Vite operator panel

The first complete online flow, **GET CUSTOMER**, works end to end. The wider banking application is still being built.

[Explore the Legacy Bank architecture →](/legacy/)

</article>

<article class="track-card modern">
<div class="track-heading"><span>Modern Bank</span><strong>PLANNED</strong></div>

### Future IBM Z branch

- intended for an IBM Z environment
- contemporary online transaction processing
- modern mainframe integration interfaces
- equivalent banking use cases
- direct comparison with Legacy Bank

The architecture will be defined after the Legacy Bank foundation is sufficiently complete.
</article>

</section>

<section id="development" class="project-section">

## Current development status

MoniBank is an active portfolio and learning project. Completed flows are demonstrated with real code and logs, while unfinished parts are clearly marked.

| Area | Status | Current scope |
|---|---|---|
| Legacy environment | In development | MVS 3.8J running under Hercules |
| Java integration | In development | Spring Boot, persistent terminal worker and TCP listener |
| GET CUSTOMER | Working end to end | JSON → 3270/KICKS → COBOL/VSAM → JES → JSON |
| Banking operations | In development | customers, accounts, cards, deposits, withdrawals and statements |
| Operator interface | In development | Vite dashboard and live mainframe visibility |
| First technical article | Under review | being verified today against the completed endpoint |
| Modern Bank | Planned | future IBM Z branch |

</section>

<section class="project-section">

## Why MoniBank exists

1. **Learn mainframe engineering through a complete system**, rather than isolated COBOL exercises.
2. **Show the boundary between generations of technology**, including the awkward details polished diagrams often omit.
3. **Build a transparent technical portfolio**, where decisions, failures and fixes can be examined in code, articles and video.

The first technical article follows one real GET CUSTOMER request from Postman to MVS and back. Every class, program, record layout and log will be checked against the working implementation before publication.

</section>

<section class="project-section">

## Have a question about MoniBank?

Spotted a technical inconsistency, have a question or want to discuss the project architecture?

[Find me on LinkedIn →](https://www.linkedin.com/in/monika-gudalewska/) · [View the MoniBank repository →](https://github.com/StormSister/MoniBank)

</section>
