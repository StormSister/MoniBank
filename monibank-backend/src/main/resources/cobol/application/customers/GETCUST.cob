       IDENTIFICATION DIVISION.
       PROGRAM-ID. GETCUST.

       ENVIRONMENT DIVISION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

       01  WS-RESP                 PIC S9(4) COMP.
       01  WS-RESP2                PIC S9(4) COMP.
       01  WS-RECORD-LENGTH        PIC S9(4) COMP.
       01  WS-KEY-LENGTH           PIC S9(4) COMP.
       01  WS-CUSTOMER-ID          PIC X(13).

       01  WS-CUSTOMER-RECORD.
           05 WS-STATUS           PIC X(1).
           05 WS-ID               PIC X(13).
           05 WS-COUNTRY          PIC X(2).
           05 WS-NATIONAL-ID      PIC X(11).
           05 WS-FIRST-NAME       PIC X(30).
           05 WS-LAST-NAME        PIC X(40).
           05 WS-DATE-BIRTH       PIC X(8).
           05 WS-CREATED-AT       PIC X(14).

       LINKAGE SECTION.

      * COMMUNICATION AREA: EXACTLY 342 BYTES.
      * INPUT: REQUEST ID + CUSTOMER ID.
      * OUTPUT: DATA COUNT + DATA RECORD + FINAL HEADER.

       01  DFHCOMMAREA.
           05 CA-REQUEST-ID       PIC X(8).
           05 CA-CUSTOMER-ID      PIC X(13).
           05 CA-DATA-COUNT       PIC 9.

           05 CA-DATA-RECORD.
               10 RD-PREFIX       PIC X(3).
               10 RD-SEP-0        PIC X.
               10 RD-TYPE         PIC X.
               10 RD-SEP-1        PIC X.
               10 RD-ENTITY       PIC X(8).
               10 RD-SEP-2        PIC X.
               10 RD-REQUEST-ID   PIC X(8).
               10 RD-SEP-3        PIC X.
               10 RD-PAYLOAD      PIC X(119).
               10 FILLER          PIC X(17).

           05 CA-HEADER-RECORD.
               10 RH-PREFIX       PIC X(3).
               10 RH-SEP-0        PIC X.
               10 RH-TYPE         PIC X.
               10 RH-SEP-1        PIC X.
               10 RH-OPERATION    PIC X(7).
               10 RH-SEP-2        PIC X.
               10 RH-REQUEST-ID   PIC X(8).
               10 RH-SEP-3        PIC X.
               10 RH-CUSTOMER-ID  PIC X(13).
               10 RH-SEP-4        PIC X.
               10 RH-STATUS       PIC X.
               10 RH-SEP-5        PIC X.
               10 RH-ERROR-CODE   PIC X(20).
               10 FILLER          PIC X(101).

       PROCEDURE DIVISION.

       MAIN-PROCESS.

      * DO NOT ACCESS AN ABSENT OR INCORRECT COMMUNICATION AREA.
           IF EIBCALEN NOT = 342
               EXEC CICS RETURN END-EXEC.

           MOVE ZERO TO CA-DATA-COUNT.
           MOVE SPACES TO CA-DATA-RECORD.
           MOVE SPACES TO CA-HEADER-RECORD.
           MOVE SPACES TO WS-CUSTOMER-RECORD.

           PERFORM PREPARE-HEADER.

           IF CA-REQUEST-ID = SPACES
               MOVE 'BADREQUEST' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-CUSTOMER-ID = SPACES
               MOVE 'BADID' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           MOVE CA-CUSTOMER-ID TO WS-CUSTOMER-ID.
           MOVE 119 TO WS-RECORD-LENGTH.
           MOVE 13 TO WS-KEY-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS READ
               FILE('CUSTFILE')
               INTO(WS-CUSTOMER-RECORD)
               LENGTH(WS-RECORD-LENGTH)
               RIDFLD(WS-CUSTOMER-ID)
               KEYLENGTH(WS-KEY-LENGTH)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP = 13
               MOVE 'NOTFOUND' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF WS-RESP NOT = ZERO
               MOVE 'READERR' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF WS-RECORD-LENGTH NOT = 119
               MOVE 'BADLENGTH' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           PERFORM PREPARE-DATA.
           MOVE 1 TO CA-DATA-COUNT.
           MOVE 'S' TO RH-TYPE.
           MOVE WS-ID TO RH-CUSTOMER-ID.
           MOVE WS-STATUS TO RH-STATUS.
           MOVE 'OK' TO RH-ERROR-CODE.

       FINISH-PROGRAM.
           EXEC CICS RETURN END-EXEC.

       PREPARE-HEADER.
           MOVE 'MBR' TO RH-PREFIX.
           MOVE ';' TO RH-SEP-0 RH-SEP-1 RH-SEP-2
                       RH-SEP-3 RH-SEP-4 RH-SEP-5.
           MOVE 'E' TO RH-TYPE.
           MOVE 'GETCUST' TO RH-OPERATION.
           MOVE CA-REQUEST-ID TO RH-REQUEST-ID.
           MOVE CA-CUSTOMER-ID TO RH-CUSTOMER-ID.
           MOVE 'INTERNAL' TO RH-ERROR-CODE.

       PREPARE-DATA.
           MOVE 'MBR' TO RD-PREFIX.
           MOVE ';' TO RD-SEP-0 RD-SEP-1
                       RD-SEP-2 RD-SEP-3.
           MOVE 'D' TO RD-TYPE.
           MOVE 'CUSTOMER' TO RD-ENTITY.
           MOVE CA-REQUEST-ID TO RD-REQUEST-ID.
           MOVE WS-CUSTOMER-RECORD TO RD-PAYLOAD.