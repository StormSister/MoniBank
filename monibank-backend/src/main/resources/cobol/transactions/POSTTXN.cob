       IDENTIFICATION DIVISION.
       PROGRAM-ID. POSTTXN.

       ENVIRONMENT DIVISION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

       01  WS-RESP                 PIC S9(8) COMP.
       01  WS-RESP2                PIC S9(8) COMP.
       01  WS-SAVED-RESP           PIC S9(8) COMP.
       01  WS-SAVED-RESP2          PIC S9(8) COMP.
       01  WS-UNLOCK-RESP          PIC S9(8) COMP.
       01  WS-SEQ-LENGTH           PIC S9(4) COMP.
       01  WS-ACCOUNT-LENGTH       PIC S9(4) COMP.
       01  WS-TXN-LENGTH           PIC S9(4) COMP.
       01  WS-SEQ-KEY-LENGTH       PIC S9(4) COMP.
       01  WS-ACCT-KEY-LENGTH      PIC S9(4) COMP.
       01  WS-TXN-KEY-LENGTH       PIC S9(4) COMP.
       01  WS-SEQ-LOCKED           PIC X.
       01  WS-ACCOUNT-LOCKED       PIC X.
       01  WS-TXN-WRITTEN          PIC X.

       01  WS-LINK-RESP            PIC S9(8) COMP.
       01  WS-LINK-RESP2           PIC S9(8) COMP.
       01  WS-MBR-CALL-LENGTH      PIC S9(4) COMP VALUE +185.

      * STAGE + RESP + RESP2 FITS RH-ERROR-CODE X(20).
       01  WS-DIAGNOSTIC.
           05 WS-DIAG-STAGE        PIC X(4).
           05 FILLER               PIC X VALUE SPACE.
           05 FILLER               PIC X(2) VALUE 'R='.
           05 WS-DIAG-RESP         PIC 9(3).
           05 FILLER               PIC X(4) VALUE ' R2='.
           05 WS-DIAG-RESP2        PIC 9(5).
           05 FILLER               PIC X VALUE SPACE.

      * INPUT: ACCOUNT X(13), TYPE X(2), AMOUNT 9(13)V99,
      * DETAIL X(34), SOURCE X(13), CREATED-AT X(14).
      * TOTAL: 91 BYTES. REQUEST ID IS CARRIED IN MBGWCA.
       01  REQUEST-RECORD.
           05 REQUEST-ACCOUNT-ID.
              10 REQUEST-ACCT-PREFIX PIC X.
              10 REQUEST-ACCT-NUMBER PIC X(12).
           05 REQUEST-TYPE          PIC X(2).
           05 REQUEST-AMOUNT        PIC 9(13)V99.
           05 REQUEST-DETAIL        PIC X(34).
           05 REQUEST-SOURCE-ID     PIC X(13).
           05 REQUEST-CREATED-AT    PIC X(14).

       01  SEQUENCE-KEY             PIC X(12)
                                     VALUE 'TRANSACTION '.

       01  SEQUENCE-RECORD.
           05 SEQUENCE-NAME         PIC X(12).
           05 SEQUENCE-NUMBER       PIC 9(12).
           05 FILLER                PIC X(8).

       01  GENERATED-TXN-ID.
           05 GENERATED-TXN-PREFIX  PIC X.
           05 GENERATED-TXN-NUMBER  PIC 9(12).

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
           05 ACCOUNT-UPDATED-AT    PIC X(14).
           05 FILLER                PIC X.

       01  WS-AVAILABLE             PIC S9(13)V99 COMP-3.
       01  WS-NEW-BALANCE           PIC S9(13)V99 COMP-3.

      * PHYSICAL MBANK.TXN RECORD - EXACTLY 119 BYTES.
       01  TRANSACTION-RECORD.
           05 TXN-STATUS            PIC X.
           05 TXN-PRIMARY-KEY.
              10 TXN-ACCOUNT-ID     PIC X(13).
              10 TXN-ID             PIC X(13).
           05 TXN-DIRECTION         PIC X.
           05 TXN-TYPE              PIC X(2).
           05 TXN-CURRENCY          PIC X(3).
           05 TXN-AMOUNT            PIC S9(13)V99 COMP-3.
           05 TXN-BALANCE-AFTER     PIC S9(13)V99 COMP-3.
           05 TXN-DETAIL            PIC X(34).
           05 TXN-SOURCE-ID         PIC X(13).
           05 TXN-REQUEST-ID        PIC X(8).
           05 TXN-CREATED-AT        PIC X(14).
           05 FILLER                PIC X.

      * PRINTABLE API PAYLOAD - EXACTLY 119 CHARACTERS.
       01  TRANSACTION-OUTPUT.
           05 TO-STATUS             PIC X.
           05 TO-ACCOUNT-ID         PIC X(13).
           05 TO-TXN-ID             PIC X(13).
           05 TO-DIRECTION          PIC X.
           05 TO-TYPE               PIC X(2).
           05 TO-CURRENCY           PIC X(3).
           05 TO-AMOUNT             PIC +9(13).99.
           05 TO-BALANCE-AFTER      PIC +9(13).99.
           05 TO-DETAIL             PIC X(20).
           05 TO-SOURCE-ID          PIC X(13).
           05 TO-CREATED-AT         PIC X(14).
           05 FILLER                PIC X(5).

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

           MOVE 'N' TO WS-SEQ-LOCKED
                       WS-ACCOUNT-LOCKED
                       WS-TXN-WRITTEN.
           MOVE ZERO TO CA-DATA-COUNT.
           MOVE SPACES TO CA-DATA-RECORD
                          CA-HEADER-RECORD
                          REQUEST-RECORD
                          ACCOUNT-RECORD
                          TRANSACTION-RECORD
                          TRANSACTION-OUTPUT
                          SEQUENCE-RECORD
                          GENERATED-TXN-ID.
           MOVE ZERO TO WS-RESP
                        WS-RESP2
                        WS-SAVED-RESP
                        WS-SAVED-RESP2
                        WS-UNLOCK-RESP
                        WS-LINK-RESP
                        WS-LINK-RESP2
                        WS-AVAILABLE
                        WS-NEW-BALANCE.
           MOVE 12 TO WS-SEQ-KEY-LENGTH.
           MOVE 13 TO WS-ACCT-KEY-LENGTH.
           MOVE 26 TO WS-TXN-KEY-LENGTH.

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

           IF CA-OPERATION NOT = 'POSTTXN'
               MOVE 'BADOPERATION' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-REQUEST-ID = SPACES
               MOVE 'BADREQUEST' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-INPUT-LENGTH NOT = '0091'
               MOVE 'BADLENGTH' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           MOVE CA-INPUT TO REQUEST-RECORD.
           MOVE REQUEST-ACCOUNT-ID TO RH-CUSTOMER-ID.

           IF REQUEST-ACCT-PREFIX NOT = 'A'
               MOVE 'BADACCOUNTID' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-ACCT-NUMBER IS NOT NUMERIC
               MOVE 'BADACCOUNTID' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-TYPE NOT = 'DP'
               IF REQUEST-TYPE NOT = 'WD'
                   MOVE 'BADTYPE' TO RH-ERROR-CODE
                   GO TO FINISH-PROGRAM.

           IF REQUEST-AMOUNT IS NOT NUMERIC
               MOVE 'BADAMOUNT' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-AMOUNT = ZERO
               MOVE 'BADAMOUNT' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-SOURCE-ID = SPACES
               MOVE 'BADSOURCE' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-CREATED-AT IS NOT NUMERIC
               MOVE 'BADDATETIME' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

      * LOCK THE ACCOUNT FOR BALANCE VALIDATION AND UPDATE.
           MOVE 119 TO WS-ACCOUNT-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS READ
               FILE('ACCTFILE')
               INTO(ACCOUNT-RECORD)
               LENGTH(WS-ACCOUNT-LENGTH)
               RIDFLD(REQUEST-ACCOUNT-ID)
               KEYLENGTH(WS-ACCT-KEY-LENGTH)
               UPDATE
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP = DFHRESP(NOTFND)
               MOVE 'ACCOUNTNOTFOUND' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF WS-RESP NOT = ZERO
               MOVE 'ACRD' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               GO TO FINISH-PROGRAM.

           MOVE 'Y' TO WS-ACCOUNT-LOCKED.

           IF WS-ACCOUNT-LENGTH NOT = 119
               MOVE 'ACCOUNTBADLENGTH' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF ACCOUNT-ID NOT = REQUEST-ACCOUNT-ID
               MOVE 'ACCOUNTKEYERROR' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF ACCOUNT-STATUS NOT = 'A'
               MOVE 'ACCOUNTINACTIVE' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           MOVE ACCOUNT-BALANCE TO WS-NEW-BALANCE.

           IF REQUEST-TYPE = 'DP'
               ADD REQUEST-AMOUNT TO WS-NEW-BALANCE.

           IF REQUEST-TYPE = 'WD'
               PERFORM CALCULATE-WITHDRAWAL
                   THRU CALCULATE-WITHDRAWAL-EXIT.

      * RESERVE THE NEXT TRANSACTION NUMBER.
           MOVE 32 TO WS-SEQ-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS READ
               FILE('SEQFILE')
               INTO(SEQUENCE-RECORD)
               RIDFLD(SEQUENCE-KEY)
               KEYLENGTH(WS-SEQ-KEY-LENGTH)
               LENGTH(WS-SEQ-LENGTH)
               UPDATE
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP NOT = ZERO
               MOVE 'SQRD' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               GO TO FINISH-PROGRAM.

           MOVE 'Y' TO WS-SEQ-LOCKED.

           IF WS-SEQ-LENGTH NOT = 32
               MOVE 'SEQBADLENGTH' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF SEQUENCE-NAME NOT = SEQUENCE-KEY
               MOVE 'SEQBADNAME' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF SEQUENCE-NUMBER IS NOT NUMERIC
               MOVE 'SEQBADNUMBER' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF SEQUENCE-NUMBER = 999999999999
               MOVE 'SEQEXHAUSTED' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           MOVE 'T' TO GENERATED-TXN-PREFIX.
           MOVE SEQUENCE-NUMBER TO GENERATED-TXN-NUMBER.

      * RESERVE BEFORE WRITING; A FAILED WRITE MAY LEAVE A GAP.
           ADD 1 TO SEQUENCE-NUMBER.
           MOVE 32 TO WS-SEQ-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS REWRITE
               FILE('SEQFILE')
               FROM(SEQUENCE-RECORD)
               LENGTH(WS-SEQ-LENGTH)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           MOVE 'N' TO WS-SEQ-LOCKED.

           IF WS-RESP NOT = ZERO
               MOVE 'SQRW' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               GO TO FINISH-PROGRAM.

           MOVE 'C' TO TXN-STATUS.
           MOVE ACCOUNT-ID TO TXN-ACCOUNT-ID.
           MOVE GENERATED-TXN-ID TO TXN-ID.
           MOVE REQUEST-TYPE TO TXN-TYPE.
           MOVE ACCOUNT-CURRENCY TO TXN-CURRENCY.
           MOVE REQUEST-AMOUNT TO TXN-AMOUNT.
           MOVE WS-NEW-BALANCE TO TXN-BALANCE-AFTER.
           MOVE REQUEST-DETAIL TO TXN-DETAIL.
           MOVE REQUEST-SOURCE-ID TO TXN-SOURCE-ID.
           MOVE CA-REQUEST-ID TO TXN-REQUEST-ID.
           MOVE REQUEST-CREATED-AT TO TXN-CREATED-AT.

           IF REQUEST-TYPE = 'DP'
               MOVE 'C' TO TXN-DIRECTION.

           IF REQUEST-TYPE = 'WD'
               MOVE 'D' TO TXN-DIRECTION.

      * WRITE THE POSTING FIRST WHILE THE ACCOUNT REMAINS LOCKED.
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

           IF WS-RESP = DFHRESP(DUPREC)
               MOVE 'DUPTXNID' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF WS-RESP NOT = ZERO
               MOVE 'TXWR' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               GO TO FINISH-PROGRAM.

           MOVE 'Y' TO WS-TXN-WRITTEN.

      * APPLY THE SAME BALANCE THAT WAS STORED IN THE POSTING.
           MOVE WS-NEW-BALANCE TO ACCOUNT-BALANCE.
           MOVE REQUEST-CREATED-AT TO ACCOUNT-UPDATED-AT.
           MOVE 119 TO WS-ACCOUNT-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS REWRITE
               FILE('ACCTFILE')
               FROM(ACCOUNT-RECORD)
               LENGTH(WS-ACCOUNT-LENGTH)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

      * REWRITE ENDS THE UPDATE REQUEST AND RELEASES ITS LOCK.
           MOVE 'N' TO WS-ACCOUNT-LOCKED.

           IF WS-RESP NOT = ZERO
               MOVE WS-RESP TO WS-SAVED-RESP
               MOVE WS-RESP2 TO WS-SAVED-RESP2
               PERFORM DELETE-TRANSACTION
                   THRU DELETE-TRANSACTION-EXIT
               GO TO HANDLE-ACCOUNT-REWRITE-ERROR.

           MOVE 'N' TO WS-TXN-WRITTEN.

           PERFORM PREPARE-TXN-OUTPUT
               THRU PREPARE-TXN-OUTPUT-EXIT.
           PERFORM PREPARE-DATA
               THRU PREPARE-DATA-EXIT.

           MOVE 1 TO CA-DATA-COUNT.
           MOVE 'S' TO RH-TYPE.
           MOVE TXN-ID TO RH-CUSTOMER-ID.
           MOVE TXN-STATUS TO RH-STATUS.
           MOVE 'OK' TO RH-ERROR-CODE.

           PERFORM WRITE-DATA-RESULT
               THRU WRITE-DATA-RESULT-EXIT.

           IF MBR-CALL-RETURN-CODE NOT = 'OK'
               MOVE ZERO TO CA-DATA-COUNT
               MOVE SPACES TO CA-DATA-RECORD
               MOVE 'E' TO RH-TYPE
               MOVE 'SPLD' TO WS-DIAG-STAGE
               PERFORM SET-SPOOL-DIAGNOSTIC
                   THRU SET-SPOOL-DIAGNOSTIC-EXIT
               GO TO RETURN-PROGRAM.

           GO TO FINISH-PROGRAM.

       HANDLE-ACCOUNT-REWRITE-ERROR.

           IF WS-TXN-WRITTEN = 'Y'
               MOVE 'CMPN' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               GO TO FINISH-PROGRAM.

           MOVE WS-SAVED-RESP TO WS-RESP.
           MOVE WS-SAVED-RESP2 TO WS-RESP2.
           MOVE 'ACRW' TO WS-DIAG-STAGE.
           PERFORM SET-CICS-DIAGNOSTIC
               THRU SET-CICS-DIAGNOSTIC-EXIT.
           GO TO FINISH-PROGRAM.

       FINISH-PROGRAM.

           IF WS-SEQ-LOCKED = 'Y'
               PERFORM RELEASE-SEQUENCE
                   THRU RELEASE-SEQUENCE-EXIT.

           IF WS-ACCOUNT-LOCKED = 'Y'
               PERFORM RELEASE-ACCOUNT
                   THRU RELEASE-ACCOUNT-EXIT.

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

       RETURN-PROGRAM.

           EXEC CICS RETURN END-EXEC.

       CALCULATE-WITHDRAWAL.

           MOVE ACCOUNT-BALANCE TO WS-AVAILABLE.
           ADD ACCOUNT-OVERDRAFT TO WS-AVAILABLE.
           SUBTRACT ACCOUNT-BLOCKED FROM WS-AVAILABLE.

           IF REQUEST-AMOUNT > WS-AVAILABLE
               MOVE 'INSUFFICIENTFUNDS' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           SUBTRACT REQUEST-AMOUNT FROM WS-NEW-BALANCE.

       CALCULATE-WITHDRAWAL-EXIT.

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

       RELEASE-SEQUENCE.

           MOVE ZERO TO WS-UNLOCK-RESP.

           EXEC CICS UNLOCK
               FILE('SEQFILE')
               RESP(WS-UNLOCK-RESP)
           END-EXEC.

           MOVE 'N' TO WS-SEQ-LOCKED.

           IF WS-UNLOCK-RESP NOT = ZERO
               MOVE WS-UNLOCK-RESP TO WS-RESP
               MOVE ZERO TO WS-RESP2
               MOVE 'SQUL' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT.

       RELEASE-SEQUENCE-EXIT.

           EXIT.

       RELEASE-ACCOUNT.

           MOVE ZERO TO WS-UNLOCK-RESP.

           EXEC CICS UNLOCK
               FILE('ACCTFILE')
               RESP(WS-UNLOCK-RESP)
           END-EXEC.

           MOVE 'N' TO WS-ACCOUNT-LOCKED.

           IF WS-UNLOCK-RESP NOT = ZERO
               MOVE WS-UNLOCK-RESP TO WS-RESP
               MOVE ZERO TO WS-RESP2
               MOVE 'ACUL' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT.

       RELEASE-ACCOUNT-EXIT.

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

       PREPARE-TXN-OUTPUT.

           MOVE SPACES TO TRANSACTION-OUTPUT.
           MOVE TXN-STATUS TO TO-STATUS.
           MOVE TXN-ACCOUNT-ID TO TO-ACCOUNT-ID.
           MOVE TXN-ID TO TO-TXN-ID.
           MOVE TXN-DIRECTION TO TO-DIRECTION.
           MOVE TXN-TYPE TO TO-TYPE.
           MOVE TXN-CURRENCY TO TO-CURRENCY.
           MOVE TXN-AMOUNT TO TO-AMOUNT.
           MOVE TXN-BALANCE-AFTER TO TO-BALANCE-AFTER.
           MOVE TXN-DETAIL TO TO-DETAIL.
           MOVE TXN-SOURCE-ID TO TO-SOURCE-ID.
           MOVE TXN-CREATED-AT TO TO-CREATED-AT.

       PREPARE-TXN-OUTPUT-EXIT.

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
           MOVE 'POSTTXN' TO RH-OPERATION.
           MOVE CA-REQUEST-ID TO RH-REQUEST-ID.
           MOVE 'INTERNAL' TO RH-ERROR-CODE.

       PREPARE-HEADER-EXIT.

           EXIT.

       PREPARE-DATA.

           MOVE 'MBR' TO RD-PREFIX.
           MOVE ';' TO RD-SEP-0
                       RD-SEP-1
                       RD-SEP-2
                       RD-SEP-3.
           MOVE 'D' TO RD-TYPE.
           MOVE 'TXN' TO RD-ENTITY.
           MOVE CA-REQUEST-ID TO RD-REQUEST-ID.
           MOVE TRANSACTION-OUTPUT TO RD-PAYLOAD.

       PREPARE-DATA-EXIT.

           EXIT.

       WRITE-DATA-RESULT.

           PERFORM PREPARE-MBR-DATA
               THRU PREPARE-MBR-DATA-EXIT.
           MOVE 'M' TO MBR-CALL-CONTROL.
           PERFORM CALL-MBRESULT
               THRU CALL-MBRESULT-EXIT.

       WRITE-DATA-RESULT-EXIT.

           EXIT.

       PREPARE-MBR-DATA.

           MOVE SPACES TO MBR-RESULT-RECORD.
           MOVE 'MBR' TO MBR-D-PREFIX.
           MOVE ';' TO MBR-D-SEP-0
                       MBR-D-SEP-1
                       MBR-D-SEP-2
                       MBR-D-SEP-3.
           MOVE 'D' TO MBR-D-TYPE.
           MOVE 'TXN' TO MBR-D-ENTITY.
           MOVE CA-REQUEST-ID TO MBR-D-REQUEST-ID.
           MOVE TRANSACTION-OUTPUT TO MBR-D-PAYLOAD.

       PREPARE-MBR-DATA-EXIT.

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
