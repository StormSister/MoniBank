---
title: Dashboard system overview
description: Where every value in the MoniBank top bar and system overview panel comes from.
---

# Dashboard system overview

The upper part of the dashboard combines three different kinds of information: browser state, Java infrastructure telemetry and the backend operation journal. It does **not** execute a COBOL banking program when the page opens.

## 1. What the operator sees

The top bar shows:

- a time-dependent greeting;
- the static label `Legacy Bank (MVS 3.8j)`;
- the current overall status;
- a button that opens or closes the live mainframe console.

The first dashboard card adds:

- the system name and ID;
- the number of ready terminal workers;
- the number of requests waiting for execution or a VSAM lock;
- reader and result-printer connection states;
- the last health-check time;
- the last-24-hours operation success/error summary.

## 2. Frontend ownership

I render both surfaces from [`Topbar.jsx`](https://github.com/StormSister/MoniBank/blob/main/monibank-frontend/src/components/layout/Topbar.jsx) and [`DashboardPage.jsx`](https://github.com/StormSister/MoniBank/blob/main/monibank-frontend/src/pages/DashboardPage.jsx).

The greeting is calculated from `new Date().getHours()` in the browser. It is therefore based on the operator's local clock, not MVS time and not the backend clock. The bank selector is currently a visual label; it does not switch between cores.

Opening the live console changes React state in `AppLayout`. The console itself uses a separate SSE connection and is not part of either status response described below.

## 3. HTTP requests

Two TanStack Query hooks supply the dynamic values:

| Hook | Request | Refresh |
|---|---|---:|
| [`useMainframeStatus`](https://github.com/StormSister/MoniBank/blob/main/monibank-frontend/src/hooks/useMainframeStatus.js) | `GET /api/mainframe/status` | every 15 seconds |
| [`useOperationSummary`](https://github.com/StormSister/MoniBank/blob/main/monibank-frontend/src/hooks/useOperations.js) | `GET /api/operations/summary?hours=24` | every 15 seconds |

The status query uses a ten-second `staleTime`. The refresh intervals are frontend polling; they do not mean that MVS is queried by a business transaction every 15 seconds.

## 4. Java handling

[`MainframeStatusController`](https://github.com/StormSister/MoniBank/blob/main/monibank-backend/src/main/java/com/monibank/mainframe/api/MainframeStatusController.java) returns the last snapshot assembled by [`MainframeStatusService`](https://github.com/StormSister/MoniBank/blob/main/monibank-backend/src/main/java/com/monibank/mainframe/hercules/MainframeStatusService.java).

`MainframeStatusService` refreshes on the configured metrics interval, 15 seconds by default. It combines independent sources rather than trusting one global health flag.

[`OperationController`](https://github.com/StormSister/MoniBank/blob/main/monibank-backend/src/main/java/com/monibank/mainframe/operations/OperationController.java) delegates the 24-hour summary to [`OperationQueryService`](https://github.com/StormSister/MoniBank/blob/main/monibank-backend/src/main/java/com/monibank/mainframe/operations/OperationQueryService.java).

## 5. Does this enter the legacy core?

No COBOL program is linked for either dashboard request.

The reader check opens the configured reader transport to establish availability, the result-printer state comes from the already running TCP listener, and terminal information comes from the Java terminal-session manager. Hercules runtime metrics are collected with `docker inspect` and `docker stats`, locally or through the configured restricted SSH command.

The operation summary reads the Java operation journal. It does not issue `LISTTXN`, inspect VSAM or parse the current screen of a 3270 session.

## 6. Source of every displayed value

| UI value | Authoritative source |
|---|---|
| Greeting | Browser local hour |
| `Legacy Bank (MVS 3.8j)` | Static frontend label |
| `MVS 3.8j` and `TK5R` | Values assigned by `MainframeStatusService` |
| Overall status | Status rule calculated by `MainframeStatusService` |
| Ready/configured/busy/recovering terminals | Snapshots from `KicksTerminalSessionManager` |
| Request queue | Terminal queue size plus threads waiting in `VsamAccessCoordinator` |
| Reader | Periodic `MainframeGateway.isAvailable()` check |
| Result printer | `MainframeTcpResultListener.isConnected()` |
| Hercules CPU, memory and uptime | Docker runtime metrics |
| Successful/business/technical operations | Append-only JSONL operation journal |
| Last health check | Time at which Java assembled the snapshot |

The reader is checked at most once per minute even though the full snapshot is normally refreshed every 15 seconds. This avoids opening a new reader connection four times per minute just to repaint the dashboard.

## 7. Status and refresh semantics

The backend reports:

- `ONLINE` when the Hercules container is running, reader and printer are connected, and every configured terminal is operational;
- `DEGRADED` when at least part of the integration is available but all online requirements are not satisfied;
- `OFFLINE` when the container is known not to be running or no integration signal is available;
- `CHECKING` before the first status snapshot exists.

The frontend uses `UNAVAILABLE` when its HTTP query fails. That word is a UI fallback and is not a backend status value.

The operation ring groups journal events into `SUCCESS`, `BUSINESS_ERROR` and `TECHNICAL_ERROR`. The compact dashboard displays business and technical errors together as failed operations, while the Operations page preserves the distinction.

## 8. Failure cases and interpretation

Runtime metrics can be stale while reader, printer and terminals still work. In that case the response marks `metricsStale: true` and retains the last runtime sample if one exists.

A zero in the operation ring does not prove that MVS performed no work. It means that the selected Java journal contains no tracked events in the last 24 hours. A new or inaccessible journal volume can therefore produce `0 successful / 0 failed` even while the mainframe is online.

Similarly, `CONNECTED` for the result printer confirms an active transport connection. It does not by itself prove that a particular COBOL operation succeeded; that conclusion requires the correlated final `MBR;S` record for that request.

## What this panel is — and is not

I designed this panel as an infrastructure summary, not as a banking ledger. Customer counts, balances, transactions and the previous-day close have different data paths and will be documented separately. Keeping those paths separate makes it possible to explain whether a number originates in VSAM, a durable daily report, an MBR result or Java-only telemetry.

