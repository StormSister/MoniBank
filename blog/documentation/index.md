---
title: MoniBank documentation
description: An evidence-based map of the MoniBank application, from the operator interface to COBOL and VSAM.
---

# MoniBank documentation

This section explains where MoniBank data comes from and what happens between an action in the operator panel and the final result returned by MVS.

I write this documentation from the implementation that is present in the repository. I distinguish source-controlled behaviour from observations made on my running TK5R system. If a mechanism is planned, experimental or not currently connected to the application flow, I label it instead of presenting it as complete.

## The eight questions behind every workflow

I use the same structure for each screen and operation:

1. **What does the operator see?** — the field, action, status or error visible in the interface.
2. **Which frontend code owns it?** — the React component, hook, query key and refresh policy.
3. **Which HTTP boundary is used?** — the endpoint, parameters and response model.
4. **Which Java classes handle it?** — the controller, service, mapper and integration components.
5. **Does the request enter the legacy core?** — the terminal queue, `MBGW`, `MBGATE` and COMMAREA path, or an explicit statement that it does not.
6. **Where is the authoritative value stored?** — VSAM, an MVS result record, the operation journal, runtime telemetry or browser state.
7. **How does the result return and refresh?** — `MBRESULT`, JES/printer transport, TCP correlation, polling, SSE or cache behaviour.
8. **What happens when something fails?** — business errors, technical errors, stale data and UI fallbacks.

This prevents a UI label from being documented as if it were a mainframe fact when it is actually static text, browser state or Java telemetry.

## Documentation map

| Layer | Scope | Status |
|---|---|---|
| Application map | Complete request and result paths | Planned |
| Dashboard provenance | Status, operations, previous-day report and recent transactions | In progress |
| Gateway protocol | `MBGW`, `MBGATE`, 855-byte COMMAREA and operation dispatch | Planned |
| Result protocol | `MBR;D`, `MBR;S`, `MBR;E`, `MBRESULT` and physical printer frames | Planned |
| Java listener | TCP connection, frame assembly, request correlation and timeout behaviour | Planned |
| Business workflows | Add customer, accounts, cards, deposits, withdrawals and statements | Planned |
| Daily close | `POSTINT`, `DAYSTAT`, `GETSTAT` and `MBANK.DAYRPT` | Planned |
| Runtime | STEVE, SOFIA, shared queue, recovery and VSAM coordination | Planned |
| Mainframe delivery | COBOL sources, copybooks, CLISTs and ordered JCL jobs | Planned |
| Repository map | Active, supporting and currently unused code | Planned |

## Available now

- [Dashboard system overview: where the top-panel data comes from](./dashboard-system-overview)

## Source of truth

Each page links to the relevant files in the [MoniBank repository](https://github.com/StormSister/MoniBank). Runtime screenshots and logs may demonstrate that a path worked at a particular time, but they do not replace the implementation as evidence of repeatable behaviour.

