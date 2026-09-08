       IDENTIFICATION DIVISION.
       PROGRAM-ID. LISTTXN.

       ENVIRONMENT DIVISION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

       01  WS-RESP                 PIC S9(8) COMP.
       01  WS-RESP2                PIC S9(8) COMP.
       01  WS-ENDBR-RESP           PIC S9(8) COMP.
       01  WS-ENDBR-RESP2          PIC S9(8) COMP.
       01  WS-RECORD-LENGTH        PIC S9(4) COMP.
       01  WS-KEY-LENGTH           PIC S9(4) COMP.
       01  WS-BROWSE-MODE          PIC X.
       01  WS-BROWSE-OPEN          PIC X.
       01  WS-END-OF-FILE          PIC X.
       01  WS-SPOOL-FAILED         PIC X.
       01  WS-PROCESS-FAILED       PIC X.

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

      * INPUT IS EMPTY FOR ALL TRANSACTIONS OR ACCOUNT-ID X(13).
       01  REQUEST-ACCOUNT-ID.
           05 REQUEST-ACCT-PREFIX  PIC X.
           05 REQUEST-ACCT-NUMBER  PIC X(12).

       01  WS-BROWSE-PRIMARY-KEY.
           05 WS-BROWSE-ACCOUNT-ID PIC X(13).
           05 WS-BROWSE-TXN-ID     PIC X(13).

       01  WS-BROWSE-AIX-KEY       PIC X(13).

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

           MOVE ZERO TO CA-DATA-COUNT.
           MOVE SPACES TO CA-DATA-RECORD
                          CA-HEADER-RECORD
                          REQUEST-ACCOUNT-ID
                          TRANSACTION-RECORD
                          TRANSACTION-OUTPUT.
           MOVE LOW-VALUES TO WS-BROWSE-PRIMARY-KEY
                              WS-BROWSE-AIX-KEY.
           MOVE SPACES TO WS-BROWSE-MODE.
           MOVE 'N' TO WS-BROWSE-OPEN
                       WS-END-OF-FILE
                       WS-SPOOL-FAILED
                       WS-PROCESS-FAILED.
           MOVE 119 TO WS-RECORD-LENGTH.
           MOVE ZERO TO WS-RESP
                        WS-RESP2
                        WS-ENDBR-RESP
                        WS-ENDBR-RESP2
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

           IF CA-OPERATION NOT = 'LISTTXN'
               MOVE 'BADOPERATION' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-REQUEST-ID = SPACES
               MOVE 'BADREQUEST' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-INPUT-LENGTH = '0000'
               MOVE 'A' TO WS-BROWSE-MODE
               GO TO START-ALL-BROWSE.

           IF CA-INPUT-LENGTH NOT = '0013'
               MOVE 'BADLENGTH' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           MOVE CA-INPUT TO REQUEST-ACCOUNT-ID.

           IF REQUEST-ACCT-PREFIX NOT = 'A'
               MOVE 'BADACCOUNTID' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-ACCT-NUMBER IS NOT NUMERIC
               MOVE 'BADACCOUNTID' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           MOVE 'C' TO WS-BROWSE-MODE.
           MOVE REQUEST-ACCOUNT-ID TO WS-BROWSE-ACCOUNT-ID.
           MOVE LOW-VALUES TO WS-BROWSE-TXN-ID.
           MOVE 26 TO WS-KEY-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS STARTBR
               FILE('TXNFILE')
               RIDFLD(WS-BROWSE-PRIMARY-KEY)
               KEYLENGTH(WS-KEY-LENGTH)
               GTEQ
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           GO TO CHECK-START-BROWSE.

       START-ALL-BROWSE.

           MOVE LOW-VALUES TO WS-BROWSE-AIX-KEY.
           MOVE 13 TO WS-KEY-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

      * TXNID IS THE PATH OVER THE TRANSACTION-ID AIX.
           EXEC CICS STARTBR
               FILE('TXNID')
               RIDFLD(WS-BROWSE-AIX-KEY)
               KEYLENGTH(WS-KEY-LENGTH)
               GTEQ
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

       CHECK-START-BROWSE.

      * AN EMPTY RESULT IS A VALID EMPTY LIST.
           IF WS-RESP = DFHRESP(NOTFND)
               MOVE 'S' TO RH-TYPE
               MOVE 'OK' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF WS-RESP NOT = ZERO
               MOVE 'STRT' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               GO TO FINISH-PROGRAM.

           MOVE 'Y' TO WS-BROWSE-OPEN.

           PERFORM READ-TRANSACTIONS
               THRU READ-TRANSACTIONS-EXIT
               UNTIL WS-END-OF-FILE = 'Y'.

           IF WS-BROWSE-OPEN = 'Y'
               PERFORM END-TRANSACTION-BROWSE
                   THRU END-TRANSACTION-BROWSE-EXIT.

           IF WS-SPOOL-FAILED = 'Y'
               GO TO RETURN-PROGRAM.

           IF WS-PROCESS-FAILED = 'Y'
               GO TO FINISH-PROGRAM.

           MOVE 'S' TO RH-TYPE.
           MOVE 'OK' TO RH-ERROR-CODE.

       FINISH-PROGRAM.

           IF WS-BROWSE-OPEN = 'Y'
               PERFORM END-TRANSACTION-BROWSE
                   THRU END-TRANSACTION-BROWSE-EXIT.

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

       READ-TRANSACTIONS.

           MOVE 119 TO WS-RECORD-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           IF WS-BROWSE-MODE = 'A'
               GO TO READ-ALL-NEXT.

           EXEC CICS READNEXT
               FILE('TXNFILE')
               INTO(TRANSACTION-RECORD)
               LENGTH(WS-RECORD-LENGTH)
               RIDFLD(WS-BROWSE-PRIMARY-KEY)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           GO TO CHECK-READ-NEXT.

       READ-ALL-NEXT.

           EXEC CICS READNEXT
               FILE('TXNID')
               INTO(TRANSACTION-RECORD)
               LENGTH(WS-RECORD-LENGTH)
               RIDFLD(WS-BROWSE-AIX-KEY)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

       CHECK-READ-NEXT.

           IF WS-RESP = DFHRESP(ENDFILE)
               MOVE 'Y' TO WS-END-OF-FILE
               GO TO READ-TRANSACTIONS-EXIT.

           IF WS-RESP = DFHRESP(NOTFND)
               MOVE 'Y' TO WS-END-OF-FILE
               GO TO READ-TRANSACTIONS-EXIT.

           IF WS-RESP NOT = ZERO
               MOVE 'READ' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               MOVE 'Y' TO WS-PROCESS-FAILED
                              WS-END-OF-FILE
               GO TO READ-TRANSACTIONS-EXIT.

           IF WS-BROWSE-MODE = 'C'
               IF TXN-ACCOUNT-ID NOT = REQUEST-ACCOUNT-ID
                   MOVE 'Y' TO WS-END-OF-FILE
                   GO TO READ-TRANSACTIONS-EXIT.

           IF WS-RECORD-LENGTH NOT = 119
               MOVE 'BADRECLENGTH' TO RH-ERROR-CODE
               MOVE 'Y' TO WS-PROCESS-FAILED
                              WS-END-OF-FILE
               GO TO READ-TRANSACTIONS-EXIT.

           PERFORM PREPARE-TXN-OUTPUT
               THRU PREPARE-TXN-OUTPUT-EXIT.
           PERFORM PREPARE-MBR-DATA
               THRU PREPARE-MBR-DATA-EXIT.
           MOVE 'M' TO MBR-CALL-CONTROL.
           PERFORM CALL-MBRESULT
               THRU CALL-MBRESULT-EXIT.

           IF MBR-CALL-RETURN-CODE NOT = 'OK'
               MOVE 'E' TO RH-TYPE
               MOVE 'SPLD' TO WS-DIAG-STAGE
               PERFORM SET-SPOOL-DIAGNOSTIC
                   THRU SET-SPOOL-DIAGNOSTIC-EXIT
               MOVE 'Y' TO WS-SPOOL-FAILED
                              WS-END-OF-FILE.

       READ-TRANSACTIONS-EXIT.

           EXIT.

       END-TRANSACTION-BROWSE.

           MOVE ZERO TO WS-ENDBR-RESP
                        WS-ENDBR-RESP2.

           IF WS-BROWSE-MODE = 'A'
               GO TO END-ALL-BROWSE.

           EXEC CICS ENDBR
               FILE('TXNFILE')
               RESP(WS-ENDBR-RESP)
               RESP2(WS-ENDBR-RESP2)
           END-EXEC.

           GO TO CHECK-END-BROWSE.

       END-ALL-BROWSE.

           EXEC CICS ENDBR
               FILE('TXNID')
               RESP(WS-ENDBR-RESP)
               RESP2(WS-ENDBR-RESP2)
           END-EXEC.

       CHECK-END-BROWSE.

           MOVE 'N' TO WS-BROWSE-OPEN.

           IF WS-ENDBR-RESP NOT = ZERO
              AND WS-SPOOL-FAILED NOT = 'Y'
               MOVE WS-ENDBR-RESP TO WS-RESP
               MOVE WS-ENDBR-RESP2 TO WS-RESP2
               MOVE 'ENDB' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               MOVE 'Y' TO WS-PROCESS-FAILED.

       END-TRANSACTION-BROWSE-EXIT.

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
           MOVE 'LISTTXN' TO RH-OPERATION.
           MOVE CA-REQUEST-ID TO RH-REQUEST-ID.
           MOVE SPACES TO RH-CUSTOMER-ID
                          RH-STATUS.
           MOVE 'INTERNAL' TO RH-ERROR-CODE.

       PREPARE-HEADER-EXIT.

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
