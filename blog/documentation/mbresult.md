---
title: MBRESULT result channel
description: What MBRESULT receives, how it writes class-Z spool frames and what the Java TCP listener reconstructs.
---

# MBRESULT result channel

`MBRESULT` is the common KICKS program responsible for sending structured MoniBank results to JES. Business programs build the result; `MBRESULT` owns the spool operations.

## What it receives

The caller links `MBRESULT` with the 185-byte [`MBRSCA`](https://github.com/StormSister/MoniBank/blob/main/monibank-backend/src/main/resources/cobol/application/result/MBRSCA.cpy):

| Field | Size | Purpose |
|---|---:|---|
| Control | 1 | `M` means more records; `F` means final record |
| Token | 8 | identifies one open spool output |
| Return code | 8 | `OK` or a program-level error |
| RESP / RESP2 | 4 + 4 | binary KICKS diagnostics |
| Logical record | 160 | one `MBR` result record |

The logical record is either data (`MBR;D`) or a final success/error header (`MBR;S` / `MBR;E`). It contains the same eight-character request ID used by Java and `MBGATE`.

## What it does

1. It requires an exact COMMAREA length of 185 bytes and control `M` or `F`.
2. If the token is blank, it opens one SYSOUT in class `Z` and returns the token to the caller.
3. It splits the 160-character logical record into two 80-character payloads.
4. It writes both as physical `MBP` frames with the request ID and part number.
5. For `M`, it leaves the SYSOUT open. For final control `F`, it closes it and clears the token.

The physical format is:

```text
MBP;<request-id>;<part>;<80-character payload>
```

The leading spool carriage-control byte makes each physical line 96 bytes. Splitting was necessary because the online spool path could not accept the original 160-character logical result as one physical line.

## What comes out

`MBRESULT` does not return business JSON. Its outputs are:

- two class-Z spool frames for every logical `MBR` record;
- the spool token, so the caller can keep one response open across multiple data records;
- `OK` or a diagnostic such as `OPENERR`, `WRIT1ERR`, `WRIT2ERR`, `CLOSERR` or `BADCTRL`, plus RESP/RESP2.

Hercules exposes the class-Z printer stream on the configured TCP device. [`MainframeTcpResultListener`](https://github.com/StormSister/MoniBank/blob/main/monibank-backend/src/main/java/com/monibank/mainframe/hercules/MainframeTcpResultListener.java) joins frame 1 and frame 2, verifies the request ID and reconstructs the original 160-character record. It accumulates `D` records and completes the pending Java request only after the final `S` or `E` record.

## Responsibility boundary

The business program decides whether the result means success or error and supplies its data. `MBRESULT` does not read VSAM and does not choose an HTTP status. Its job is reliable framing and spool delivery from KICKS to the Java listener.

