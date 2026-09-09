       IDENTIFICATION DIVISION.
       PROGRAM-ID. POSTINT.

       ENVIRONMENT DIVISION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

       01  WS-RESP                 PIC S9(8) COMP.
       01  WS-RESP2                PIC S9(8) COMP.
       01  WS-SAVED-RESP           PIC S9(8) COMP.
       01  WS-SAVED-RESP2          PIC S9(8) COMP.
       01  WS-ENDBR-RESP           PIC S9(8) COMP.
       01  WS-UNLOCK-RESP          PIC S9(8) COMP.
       01  WS-ACCOUNT-LENGTH       PIC S9(4) COMP.
       01  WS-TXN-LENGTH           PIC S9(4) COMP.
       01  WS-ACCT-KEY-LENGTH      PIC S9(4) COMP.
       01  WS-TXN-KEY-LENGTH       PIC S9(4) COMP.
       01  WS-BROWSE-OPEN          PIC X.
       01  WS-END-OF-FILE          PIC X.
       01  WS-ACCOUNT-LOCKED       PIC X.
       01  WS-TXN-WRITTEN          PIC X.
       01  WS-PROCESS-FAILED       PIC X.

       01  WS-LINK-RESP            PIC S9(8) COMP.
       01  WS-LINK-RESP2           PIC S9(8) COMP.
       01  WS-MBR-CALL-LENGTH      PIC S9(4) COMP VALUE +185.

       01  WS-DIAGNOSTIC.
           05 WS-DIAG-STAGE        PIC X(4).
           05 FILLER               PIC X VALUE SPACE.
           05 FILLER               PIC X(2) VALUE 'R='.
           05 WS-DIAG-RESP         PIC 9(3).
           05 FILLER               PIC X(4) VALUE ' R2='.
           05 WS-DIAG-RESP2        PIC 9(5).
           05 FILLER               PIC X VALUE SPACE.

      * INPUT: BUSINESS DATE X(8), CURRENCY X(3), RATE BPS 9(5).
      * 00500 BASIS POINTS MEANS 5.00 PERCENT PER YEAR.
       01  REQUEST-RECORD.
           05 REQUEST-BUSINESS-DATE PIC X(8).
           05 REQUEST-CURRENCY      PIC X(3).
           05 REQUEST-RATE-BPS      PIC 9(5).

       01  WS-BROWSE-KEY           PIC X(13).
       01  WS-UPDATE-KEY           PIC X(13).
       01  WS-ELIGIBLE-COUNT       PIC 9(4) COMP.
       01  WS-ELIGIBLE-INDEX       PIC 9(4) COMP.
       01  WS-ELIGIBLE-ACCOUNTS.
           05 WS-ELIGIBLE-ID       PIC X(13) OCCURS 1000 TIMES.
       01  WS-POSTED-COUNT         PIC 9(9) COMP-3.
       01  WS-POSTED-COUNT-OUT     PIC 9(13).
       01  WS-INTEREST             PIC S9(13)V99 COMP-3.
       01  WS-NEW-BALANCE          PIC S9(13)V99 COMP-3.

      * PHYSICAL MBANK.ACCT RECORD - EXACTLY 119 BYTES.
       01  ACCOUNT-RECORD.
           05 ACCOUNT-STATUS        PIC X.
           05 ACCOUNT-ID            PIC X(13).
           05 ACCOUNT-CUSTOMER-ID   PIC X(13).
           05 ACCOUNT-IBAN          PIC X(34).
           05 ACCOUNT-TYPE          PIC X(2).
           05 ACCOUNT-CURRENCY      PIC X(3).
           05 ACCOUNT-BALANCE       PIC S9(13)V99 COMP-3.
           05 ACCOUNT-OVERDRAFT     PIC S9(13)V99 COMP-3.
           05 ACCOUNT-BLOCKED       PIC S9(13)V99 COMP-3.
           05 ACCOUNT-CREATED-AT    PIC X(14).
           05 ACCOUNT-UPDATED-AT.
              10 ACCOUNT-UPDATED-DATE PIC X(8).
              10 ACCOUNT-UPDATED-TIME PIC X(6).
           05 FILLER                PIC X.

      * PHYSICAL MBANK.TXN RECORD - EXACTLY 119 BYTES.
       01  TRANSACTION-RECORD.
           05 TXN-STATUS            PIC X.
           05 TXN-PRIMARY-KEY.
              10 TXN-ACCOUNT-ID     PIC X(13).
              10 TXN-ID.
                 15 TXN-ID-PREFIX   PIC X.
                 15 TXN-ID-DATE     PIC X(8).
                 15 TXN-ID-SUFFIX   PIC X(4).
           05 TXN-DIRECTION         PIC X.
           05 TXN-TYPE              PIC X(2).
           05 TXN-CURRENCY          PIC X(3).
           05 TXN-AMOUNT            PIC S9(13)V99 COMP-3.
           05 TXN-BALANCE-AFTER     PIC S9(13)V99 COMP-3.
           05 TXN-DETAIL.
              10 TXN-DETAIL-TEXT    PIC X(15).
              10 TXN-DETAIL-DATE    PIC X(8).
              10 FILLER             PIC X(11).
           05 TXN-SOURCE-ID         PIC X(13).
           05 TXN-REQUEST-ID        PIC X(8).
           05 TXN-CREATED-AT.
              10 TXN-CREATED-DATE   PIC X(8).
              10 TXN-CREATED-TIME   PIC X(6).
           05 FILLER                PIC X.

       01  WS-MBR-CALL-AREA.
           COPY MBRSCA.

           COPY MBRREC.

       LINKAGE SECTION.

       01  DFHCOMMAREA.
           COPY MBGWCA.

       PROCEDURE DIVISION.

       MAIN-PROCESS.

           IF EIBCALEN NOT = 855
               EXEC CICS RETURN END-EXEC.

           MOVE 'N' TO WS-BROWSE-OPEN
                       WS-END-OF-FILE
                       WS-ACCOUNT-LOCKED
                       WS-TXN-WRITTEN
                       WS-PROCESS-FAILED.
           MOVE ZERO TO CA-DATA-COUNT
                       WS-ELIGIBLE-COUNT
                       WS-ELIGIBLE-INDEX
                       WS-POSTED-COUNT
                        WS-POSTED-COUNT-OUT
                        WS-INTEREST
                        WS-NEW-BALANCE.
           MOVE SPACES TO CA-DATA-RECORD
                          CA-HEADER-RECORD
                          REQUEST-RECORD
                          ACCOUNT-RECORD
                          TRANSACTION-RECORD
                          WS-ELIGIBLE-ACCOUNTS
                          WS-UPDATE-KEY.
           MOVE LOW-VALUES TO WS-BROWSE-KEY.
           MOVE 13 TO WS-ACCT-KEY-LENGTH.
           MOVE 26 TO WS-TXN-KEY-LENGTH.
           MOVE ZERO TO WS-RESP
                        WS-RESP2
                        WS-SAVED-RESP
                        WS-SAVED-RESP2
                        WS-ENDBR-RESP
                        WS-UNLOCK-RESP
                        WS-LINK-RESP
                        WS-LINK-RESP2.

           MOVE SPACES TO MBR-CALL-CONTROL
                          MBR-CALL-TOKEN
                          MBR-CALL-RETURN-CODE
                          MBR-CALL-RECORD
                          MBR-RESULT-RECORD.
           MOVE ZERO TO MBR-CALL-RESP
                        MBR-CALL-RESP2
                        WS-DIAG-RESP
                        WS-DIAG-RESP2.

           PERFORM PREPARE-HEADER
               THRU PREPARE-HEADER-EXIT.

           IF CA-VERSION NOT = '01'
               MOVE 'BADVERSION' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-OPERATION NOT = 'POSTINT'
               MOVE 'BADOPERATION' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-REQUEST-ID = SPACES
               MOVE 'BADREQUEST' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-INPUT-LENGTH NOT = '0016'
               MOVE 'BADLENGTH' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           MOVE CA-INPUT TO REQUEST-RECORD.

           IF REQUEST-BUSINESS-DATE IS NOT NUMERIC
               MOVE 'BADDATE' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-CURRENCY = SPACES
               MOVE 'BADCURRENCY' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-RATE-BPS IS NOT NUMERIC
               MOVE 'BADRATE' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-RATE-BPS = ZERO
               MOVE 'BADRATE' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS STARTBR
               FILE('ACCTFILE')
               RIDFLD(WS-BROWSE-KEY)
               KEYLENGTH(WS-ACCT-KEY-LENGTH)
               GTEQ
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP = DFHRESP(NOTFND)
               MOVE 'S' TO RH-TYPE
               MOVE 'C' TO RH-STATUS
               MOVE 'OK' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF WS-RESP NOT = ZERO
               MOVE 'STRT' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               GO TO FINISH-PROGRAM.

           MOVE 'Y' TO WS-BROWSE-OPEN.

      * FIRST PASS ONLY COLLECTS IDS. ACCTFILE IS NOT UPDATED WHILE
      * ITS STARTBR/READNEXT BROWSE IS ACTIVE.
           PERFORM COLLECT-ACCOUNTS
               THRU COLLECT-ACCOUNTS-EXIT
               UNTIL WS-END-OF-FILE = 'Y'.

           IF WS-BROWSE-OPEN = 'Y'
               PERFORM END-ACCOUNT-BROWSE
                   THRU END-ACCOUNT-BROWSE-EXIT.

           IF WS-PROCESS-FAILED = 'Y'
               GO TO FINISH-PROGRAM.

      * SECOND PASS LOCKS AND UPDATES ONE ACCOUNT AT A TIME.
           PERFORM POST-ACCOUNT-INTEREST
               THRU POST-ACCOUNT-INTEREST-EXIT
               VARYING WS-ELIGIBLE-INDEX FROM 1 BY 1
               UNTIL WS-ELIGIBLE-INDEX > WS-ELIGIBLE-COUNT
                  OR WS-PROCESS-FAILED = 'Y'.

           IF WS-PROCESS-FAILED = 'Y'
               GO TO FINISH-PROGRAM.

           MOVE WS-POSTED-COUNT TO WS-POSTED-COUNT-OUT.
           MOVE WS-POSTED-COUNT-OUT TO RH-CUSTOMER-ID.
           MOVE 'S' TO RH-TYPE.
           MOVE 'C' TO RH-STATUS.
           MOVE 'OK' TO RH-ERROR-CODE.

       FINISH-PROGRAM.

           IF WS-ACCOUNT-LOCKED = 'Y'
               PERFORM RELEASE-ACCOUNT
                   THRU RELEASE-ACCOUNT-EXIT.

           IF WS-BROWSE-OPEN = 'Y'
               PERFORM END-ACCOUNT-BROWSE
                   THRU END-ACCOUNT-BROWSE-EXIT.

           PERFORM PREPARE-MBR-HEADER
               THRU PREPARE-MBR-HEADER-EXIT.
           MOVE 'F' TO MBR-CALL-CONTROL.
           PERFORM CALL-MBRESULT
               THRU CALL-MBRESULT-EXIT.

           IF MBR-CALL-RETURN-CODE NOT = 'OK'
               MOVE 'E' TO RH-TYPE
               MOVE 'SPLF' TO WS-DIAG-STAGE
               PERFORM SET-SPOOL-DIAGNOSTIC
                   THRU SET-SPOOL-DIAGNOSTIC-EXIT.

           EXEC CICS RETURN END-EXEC.

       COLLECT-ACCOUNTS.

           MOVE 119 TO WS-ACCOUNT-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS READNEXT
               FILE('ACCTFILE')
               INTO(ACCOUNT-RECORD)
               LENGTH(WS-ACCOUNT-LENGTH)
               RIDFLD(WS-BROWSE-KEY)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP = DFHRESP(ENDFILE)
               MOVE 'Y' TO WS-END-OF-FILE
               GO TO COLLECT-ACCOUNTS-EXIT.

           IF WS-RESP NOT = ZERO
               MOVE 'BRRD' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               MOVE 'Y' TO WS-PROCESS-FAILED
                              WS-END-OF-FILE
               GO TO COLLECT-ACCOUNTS-EXIT.

           IF WS-ACCOUNT-LENGTH NOT = 119
               MOVE 'BADRECLENGTH' TO RH-ERROR-CODE
               MOVE 'Y' TO WS-PROCESS-FAILED
                              WS-END-OF-FILE
               GO TO COLLECT-ACCOUNTS-EXIT.

           IF ACCOUNT-STATUS NOT = 'A'
               GO TO COLLECT-ACCOUNTS-EXIT.

           IF ACCOUNT-CURRENCY NOT = REQUEST-CURRENCY
               GO TO COLLECT-ACCOUNTS-EXIT.

           IF ACCOUNT-BALANCE NOT GREATER THAN ZERO
               GO TO COLLECT-ACCOUNTS-EXIT.

           IF WS-ELIGIBLE-COUNT = 1000
               MOVE 'TOOMANYACCOUNTS' TO RH-ERROR-CODE
               MOVE 'Y' TO WS-PROCESS-FAILED
                              WS-END-OF-FILE
               GO TO COLLECT-ACCOUNTS-EXIT.

           ADD 1 TO WS-ELIGIBLE-COUNT.
           MOVE ACCOUNT-ID TO
               WS-ELIGIBLE-ID (WS-ELIGIBLE-COUNT).

       COLLECT-ACCOUNTS-EXIT.

           EXIT.

       POST-ACCOUNT-INTEREST.

           MOVE WS-ELIGIBLE-ID (WS-ELIGIBLE-INDEX)
               TO WS-UPDATE-KEY.
           MOVE SPACES TO ACCOUNT-RECORD
                          TRANSACTION-RECORD.
           MOVE 119 TO WS-ACCOUNT-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2
                        WS-INTEREST WS-NEW-BALANCE.
           MOVE 'N' TO WS-ACCOUNT-LOCKED
                       WS-TXN-WRITTEN.

           EXEC CICS READ
               FILE('ACCTFILE')
               INTO(ACCOUNT-RECORD)
               LENGTH(WS-ACCOUNT-LENGTH)
               RIDFLD(WS-UPDATE-KEY)
               KEYLENGTH(WS-ACCT-KEY-LENGTH)
               UPDATE
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP NOT = ZERO
               MOVE 'ACRD' TO WS-DIAG-STAGE
               PERFORM FAIL-CURRENT-ACCOUNT
                   THRU FAIL-CURRENT-ACCOUNT-EXIT
               GO TO POST-ACCOUNT-INTEREST-EXIT.

           MOVE 'Y' TO WS-ACCOUNT-LOCKED.

      * RECHECK AFTER LOCKING BECAUSE ONLINE DATA MAY HAVE CHANGED.
           IF ACCOUNT-STATUS NOT = 'A'
               PERFORM RELEASE-ACCOUNT
                   THRU RELEASE-ACCOUNT-EXIT
               GO TO POST-ACCOUNT-INTEREST-EXIT.

           IF ACCOUNT-CURRENCY NOT = REQUEST-CURRENCY
               PERFORM RELEASE-ACCOUNT
                   THRU RELEASE-ACCOUNT-EXIT
               GO TO POST-ACCOUNT-INTEREST-EXIT.

           IF ACCOUNT-BALANCE NOT GREATER THAN ZERO
               PERFORM RELEASE-ACCOUNT
                   THRU RELEASE-ACCOUNT-EXIT
               GO TO POST-ACCOUNT-INTEREST-EXIT.

      * DAILY INTEREST = BALANCE * ANNUAL BPS / 10000 / 365.
           COMPUTE WS-INTEREST ROUNDED =
               ACCOUNT-BALANCE * REQUEST-RATE-BPS / 3650000.

           IF WS-INTEREST = ZERO
               PERFORM RELEASE-ACCOUNT
                   THRU RELEASE-ACCOUNT-EXIT
               GO TO POST-ACCOUNT-INTEREST-EXIT.

           MOVE ACCOUNT-BALANCE TO WS-NEW-BALANCE.
           ADD WS-INTEREST TO WS-NEW-BALANCE.

      * DETERMINISTIC ID MAKES A RERUN IDEMPOTENT PER ACCOUNT/DAY.
           MOVE 'C' TO TXN-STATUS.
           MOVE ACCOUNT-ID TO TXN-ACCOUNT-ID.
           MOVE 'T' TO TXN-ID-PREFIX.
           MOVE REQUEST-BUSINESS-DATE TO TXN-ID-DATE.
           MOVE '0000' TO TXN-ID-SUFFIX.
           MOVE 'C' TO TXN-DIRECTION.
           MOVE 'IN' TO TXN-TYPE.
           MOVE ACCOUNT-CURRENCY TO TXN-CURRENCY.
           MOVE WS-INTEREST TO TXN-AMOUNT.
           MOVE WS-NEW-BALANCE TO TXN-BALANCE-AFTER.
           MOVE 'DAILY INTEREST ' TO TXN-DETAIL-TEXT.
           MOVE REQUEST-BUSINESS-DATE TO TXN-DETAIL-DATE.
           MOVE 'EODINTEREST' TO TXN-SOURCE-ID.
           MOVE CA-REQUEST-ID TO TXN-REQUEST-ID.
           MOVE REQUEST-BUSINESS-DATE TO TXN-CREATED-DATE.
           MOVE '235959' TO TXN-CREATED-TIME.

           MOVE 119 TO WS-TXN-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS WRITE
               FILE('TXNFILE')
               FROM(TRANSACTION-RECORD)
               RIDFLD(TXN-PRIMARY-KEY)
               KEYLENGTH(WS-TXN-KEY-LENGTH)
               LENGTH(WS-TXN-LENGTH)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

      * DUPREC MEANS THIS ACCOUNT/DAY WAS ALREADY POSTED.
           IF WS-RESP = DFHRESP(DUPREC)
               PERFORM RELEASE-ACCOUNT
                   THRU RELEASE-ACCOUNT-EXIT
               GO TO POST-ACCOUNT-INTEREST-EXIT.

           IF WS-RESP NOT = ZERO
               MOVE 'TXWR' TO WS-DIAG-STAGE
               PERFORM FAIL-CURRENT-ACCOUNT
                   THRU FAIL-CURRENT-ACCOUNT-EXIT
               GO TO POST-ACCOUNT-INTEREST-EXIT.

           MOVE 'Y' TO WS-TXN-WRITTEN.
           MOVE WS-NEW-BALANCE TO ACCOUNT-BALANCE.
           MOVE REQUEST-BUSINESS-DATE TO ACCOUNT-UPDATED-DATE.
           MOVE '235959' TO ACCOUNT-UPDATED-TIME.
           MOVE 119 TO WS-ACCOUNT-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS REWRITE
               FILE('ACCTFILE')
               FROM(ACCOUNT-RECORD)
               LENGTH(WS-ACCOUNT-LENGTH)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           MOVE 'N' TO WS-ACCOUNT-LOCKED.

           IF WS-RESP NOT = ZERO
               MOVE WS-RESP TO WS-SAVED-RESP
               MOVE WS-RESP2 TO WS-SAVED-RESP2
               PERFORM DELETE-TRANSACTION
                   THRU DELETE-TRANSACTION-EXIT
               MOVE WS-SAVED-RESP TO WS-RESP
               MOVE WS-SAVED-RESP2 TO WS-RESP2
               MOVE 'ACRW' TO WS-DIAG-STAGE
               PERFORM FAIL-CURRENT-ACCOUNT
                   THRU FAIL-CURRENT-ACCOUNT-EXIT
               GO TO POST-ACCOUNT-INTEREST-EXIT.

           MOVE 'N' TO WS-TXN-WRITTEN.
           ADD 1 TO WS-POSTED-COUNT.

       POST-ACCOUNT-INTEREST-EXIT.

           EXIT.

       FAIL-CURRENT-ACCOUNT.

           PERFORM SET-CICS-DIAGNOSTIC
               THRU SET-CICS-DIAGNOSTIC-EXIT.
           MOVE 'Y' TO WS-PROCESS-FAILED
                          WS-END-OF-FILE.

           IF WS-ACCOUNT-LOCKED = 'Y'
               PERFORM RELEASE-ACCOUNT
                   THRU RELEASE-ACCOUNT-EXIT.

       FAIL-CURRENT-ACCOUNT-EXIT.

           EXIT.

       DELETE-TRANSACTION.

           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS DELETE
               FILE('TXNFILE')
               RIDFLD(TXN-PRIMARY-KEY)
               KEYLENGTH(WS-TXN-KEY-LENGTH)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP = ZERO
               MOVE 'N' TO WS-TXN-WRITTEN.

       DELETE-TRANSACTION-EXIT.

           EXIT.

       RELEASE-ACCOUNT.

           MOVE ZERO TO WS-UNLOCK-RESP.

           EXEC CICS UNLOCK
               FILE('ACCTFILE')
               RESP(WS-UNLOCK-RESP)
           END-EXEC.

           MOVE 'N' TO WS-ACCOUNT-LOCKED.

       RELEASE-ACCOUNT-EXIT.

           EXIT.

       END-ACCOUNT-BROWSE.

           MOVE ZERO TO WS-ENDBR-RESP.

           EXEC CICS ENDBR
               FILE('ACCTFILE')
               RESP(WS-ENDBR-RESP)
           END-EXEC.

           MOVE 'N' TO WS-BROWSE-OPEN.

           IF WS-ENDBR-RESP NOT = ZERO
               MOVE WS-ENDBR-RESP TO WS-RESP
               MOVE ZERO TO WS-RESP2
               MOVE 'ENDB' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               MOVE 'Y' TO WS-PROCESS-FAILED.

       END-ACCOUNT-BROWSE-EXIT.

           EXIT.

       SET-CICS-DIAGNOSTIC.

           MOVE WS-RESP TO WS-DIAG-RESP.
           MOVE WS-RESP2 TO WS-DIAG-RESP2.
           MOVE WS-DIAGNOSTIC TO RH-ERROR-CODE.

       SET-CICS-DIAGNOSTIC-EXIT.

           EXIT.

       SET-SPOOL-DIAGNOSTIC.

           MOVE MBR-CALL-RESP TO WS-DIAG-RESP.
           MOVE MBR-CALL-RESP2 TO WS-DIAG-RESP2.
           MOVE WS-DIAGNOSTIC TO RH-ERROR-CODE.

       SET-SPOOL-DIAGNOSTIC-EXIT.

           EXIT.

       PREPARE-HEADER.

           MOVE 'MBR' TO RH-PREFIX.
           MOVE ';' TO RH-SEP-0
                       RH-SEP-1
                       RH-SEP-2
                       RH-SEP-3
                       RH-SEP-4
                       RH-SEP-5.
           MOVE 'E' TO RH-TYPE.
           MOVE 'POSTINT' TO RH-OPERATION.
           MOVE CA-REQUEST-ID TO RH-REQUEST-ID.
           MOVE 'INTERNAL' TO RH-ERROR-CODE.

       PREPARE-HEADER-EXIT.

           EXIT.

       PREPARE-MBR-HEADER.

           MOVE SPACES TO MBR-RESULT-RECORD.
           MOVE 'MBR' TO MBR-H-PREFIX.
           MOVE ';' TO MBR-H-SEP-0
                       MBR-H-SEP-1
                       MBR-H-SEP-2
                       MBR-H-SEP-3
                       MBR-H-SEP-4
                       MBR-H-SEP-5.
           MOVE RH-TYPE TO MBR-H-TYPE.
           MOVE RH-OPERATION TO MBR-H-OPERATION.
           MOVE CA-REQUEST-ID TO MBR-H-REQUEST-ID.
           MOVE RH-CUSTOMER-ID TO MBR-H-ENTITY-ID.
           MOVE RH-STATUS TO MBR-H-STATUS.
           MOVE RH-ERROR-CODE TO MBR-H-CODE.

       PREPARE-MBR-HEADER-EXIT.

           EXIT.

       CALL-MBRESULT.

           MOVE MBR-RESULT-RECORD TO MBR-CALL-RECORD.
           MOVE SPACES TO MBR-CALL-RETURN-CODE.
           MOVE ZERO TO MBR-CALL-RESP
                        MBR-CALL-RESP2
                        WS-LINK-RESP
                        WS-LINK-RESP2.

           EXEC CICS LINK
               PROGRAM('MBRESULT')
               COMMAREA(WS-MBR-CALL-AREA)
               LENGTH(WS-MBR-CALL-LENGTH)
               RESP(WS-LINK-RESP)
               RESP2(WS-LINK-RESP2)
           END-EXEC.

           IF WS-LINK-RESP NOT = ZERO
               MOVE 'LINKERR' TO MBR-CALL-RETURN-CODE
               MOVE WS-LINK-RESP TO MBR-CALL-RESP
               MOVE WS-LINK-RESP2 TO MBR-CALL-RESP2.

       CALL-MBRESULT-EXIT.

           EXIT.
