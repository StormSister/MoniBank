       IDENTIFICATION DIVISION.
       PROGRAM-ID. DAYSTAT.

       ENVIRONMENT DIVISION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

       01  WS-RESP                 PIC S9(8) COMP.
       01  WS-RESP2                PIC S9(8) COMP.
       01  WS-ENDBR-RESP           PIC S9(8) COMP.
       01  WS-ENDBR-RESP2          PIC S9(8) COMP.
       01  WS-RECORD-LENGTH        PIC S9(4) COMP.
       01  WS-KEY-LENGTH           PIC S9(4) COMP.
       01  WS-BROWSE-OPEN          PIC X.
       01  WS-END-OF-FILE          PIC X.
       01  WS-PROCESS-FAILED       PIC X.
       01  WS-SPOOL-FAILED         PIC X.
       01  WS-BROWSE-TYPE          PIC X.

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

      * INPUT: BUSINESS DATE X(8), CURRENCY X(3).
       01  REQUEST-RECORD.
           05 REQUEST-BUSINESS-DATE PIC X(8).
           05 REQUEST-CURRENCY      PIC X(3).

       01  WS-TXN-BROWSE-KEY       PIC X(13).
       01  WS-CUST-BROWSE-KEY      PIC X(13).

       01  WS-COUNTERS COMP-3.
           05 WS-TXN-COUNT          PIC 9(9) VALUE ZERO.
           05 WS-DEPOSIT-COUNT      PIC 9(9) VALUE ZERO.
           05 WS-WITHDRAWAL-COUNT   PIC 9(9) VALUE ZERO.
           05 WS-INTEREST-COUNT     PIC 9(9) VALUE ZERO.
           05 WS-CUSTOMER-COUNT     PIC 9(9) VALUE ZERO.
           05 WS-ACTIVE-COUNT       PIC 9(9) VALUE ZERO.
           05 WS-INACTIVE-COUNT     PIC 9(9) VALUE ZERO.
           05 WS-NEW-CUSTOMER-COUNT PIC 9(9) VALUE ZERO.

       01  WS-AMOUNTS COMP-3.
           05 WS-DEPOSIT-AMOUNT     PIC S9(13)V99 VALUE ZERO.
           05 WS-WITHDRAWAL-AMOUNT  PIC S9(13)V99 VALUE ZERO.
           05 WS-INTEREST-AMOUNT    PIC S9(13)V99 VALUE ZERO.

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
           05 TXN-CREATED-AT.
              10 TXN-CREATED-DATE   PIC X(8).
              10 TXN-CREATED-TIME   PIC X(6).
           05 FILLER                PIC X.

      * PHYSICAL MBANK.CUST RECORD - EXACTLY 119 BYTES.
       01  CUSTOMER-RECORD.
           05 CUSTOMER-STATUS       PIC X.
           05 CUSTOMER-ID           PIC X(13).
           05 CUSTOMER-COUNTRY      PIC X(2).
           05 CUSTOMER-NATIONAL-ID  PIC X(11).
           05 CUSTOMER-FIRST-NAME   PIC X(30).
           05 CUSTOMER-LAST-NAME    PIC X(40).
           05 CUSTOMER-BIRTH-DATE   PIC X(8).
           05 CUSTOMER-CREATED-AT.
              10 CUSTOMER-CREATED-DATE PIC X(8).
              10 CUSTOMER-CREATED-TIME PIC X(6).

      * PRINTABLE TRANSACTION SUMMARY - EXACTLY 119 CHARACTERS.
       01  TRANSACTION-OUTPUT.
           05 TO-DATE               PIC X(8).
           05 TO-CURRENCY           PIC X(3).
           05 TO-TXN-COUNT          PIC 9(9).
           05 TO-DEPOSIT-COUNT      PIC 9(9).
           05 TO-DEPOSIT-AMOUNT     PIC +9(13).99.
           05 TO-WITHDRAWAL-COUNT   PIC 9(9).
           05 TO-WITHDRAWAL-AMOUNT  PIC +9(13).99.
           05 TO-INTEREST-COUNT     PIC 9(9).
           05 TO-INTEREST-AMOUNT    PIC +9(13).99.
           05 FILLER                PIC X(21).

      * PRINTABLE CUSTOMER SUMMARY - EXACTLY 119 CHARACTERS.
       01  CUSTOMER-OUTPUT.
           05 CO-DATE               PIC X(8).
           05 CO-SCOPE              PIC X(3).
           05 CO-CUSTOMER-COUNT     PIC 9(9).
           05 CO-ACTIVE-COUNT       PIC 9(9).
           05 CO-INACTIVE-COUNT     PIC 9(9).
           05 CO-NEW-COUNT          PIC 9(9).
           05 FILLER                PIC X(72).

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
                       WS-PROCESS-FAILED
                       WS-SPOOL-FAILED.
           MOVE SPACES TO WS-BROWSE-TYPE
                          REQUEST-RECORD
                          TRANSACTION-RECORD
                          CUSTOMER-RECORD
                          TRANSACTION-OUTPUT
                          CUSTOMER-OUTPUT
                          CA-DATA-RECORD
                          CA-HEADER-RECORD.
           MOVE LOW-VALUES TO WS-TXN-BROWSE-KEY
                              WS-CUST-BROWSE-KEY.
           MOVE ZERO TO CA-DATA-COUNT
                        WS-TXN-COUNT
                        WS-DEPOSIT-COUNT
                        WS-WITHDRAWAL-COUNT
                        WS-INTEREST-COUNT
                        WS-CUSTOMER-COUNT
                        WS-ACTIVE-COUNT
                        WS-INACTIVE-COUNT
                        WS-NEW-CUSTOMER-COUNT
                        WS-DEPOSIT-AMOUNT
                        WS-WITHDRAWAL-AMOUNT
                        WS-INTEREST-AMOUNT
                        WS-RESP
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

           IF CA-OPERATION NOT = 'DAYSTAT'
               MOVE 'BADOPERATION' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-REQUEST-ID = SPACES
               MOVE 'BADREQUEST' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-INPUT-LENGTH NOT = '0011'
               MOVE 'BADLENGTH' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           MOVE CA-INPUT TO REQUEST-RECORD.

           IF REQUEST-BUSINESS-DATE IS NOT NUMERIC
               MOVE 'BADDATE' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-CURRENCY = SPACES
               MOVE 'BADCURRENCY' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           PERFORM START-TXN-BROWSE
               THRU START-TXN-BROWSE-EXIT.

           IF WS-PROCESS-FAILED = 'Y'
               GO TO FINISH-PROGRAM.

           IF WS-END-OF-FILE NOT = 'Y'
               PERFORM READ-TRANSACTIONS
                   THRU READ-TRANSACTIONS-EXIT
                   UNTIL WS-END-OF-FILE = 'Y'.

           IF WS-BROWSE-OPEN = 'Y'
               PERFORM END-CURRENT-BROWSE
                   THRU END-CURRENT-BROWSE-EXIT.

           IF WS-PROCESS-FAILED = 'Y'
               GO TO FINISH-PROGRAM.

           MOVE 'N' TO WS-END-OF-FILE.
           PERFORM START-CUST-BROWSE
               THRU START-CUST-BROWSE-EXIT.

           IF WS-PROCESS-FAILED = 'Y'
               GO TO FINISH-PROGRAM.

           IF WS-END-OF-FILE NOT = 'Y'
               PERFORM READ-CUSTOMERS
                   THRU READ-CUSTOMERS-EXIT
                   UNTIL WS-END-OF-FILE = 'Y'.

           IF WS-BROWSE-OPEN = 'Y'
               PERFORM END-CURRENT-BROWSE
                   THRU END-CURRENT-BROWSE-EXIT.

           IF WS-PROCESS-FAILED = 'Y'
               GO TO FINISH-PROGRAM.

           PERFORM WRITE-TRANSACTION-SUMMARY
               THRU WRITE-TRANSACTION-SUMMARY-EXIT.

           IF WS-SPOOL-FAILED = 'Y'
               GO TO RETURN-PROGRAM.

           PERFORM WRITE-CUSTOMER-SUMMARY
               THRU WRITE-CUSTOMER-SUMMARY-EXIT.

           IF WS-SPOOL-FAILED = 'Y'
               GO TO RETURN-PROGRAM.

           MOVE 'S' TO RH-TYPE.
           MOVE 'C' TO RH-STATUS.
           MOVE 'OK' TO RH-ERROR-CODE.

       FINISH-PROGRAM.

           IF WS-BROWSE-OPEN = 'Y'
               PERFORM END-CURRENT-BROWSE
                   THRU END-CURRENT-BROWSE-EXIT.

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

       START-TXN-BROWSE.

           MOVE 'T' TO WS-BROWSE-TYPE.
           MOVE 13 TO WS-KEY-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS STARTBR
               FILE('TXNID')
               RIDFLD(WS-TXN-BROWSE-KEY)
               KEYLENGTH(WS-KEY-LENGTH)
               GTEQ
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           PERFORM CHECK-START-BROWSE
               THRU CHECK-START-BROWSE-EXIT.

       START-TXN-BROWSE-EXIT.

           EXIT.

       START-CUST-BROWSE.

           MOVE 'C' TO WS-BROWSE-TYPE.
           MOVE 13 TO WS-KEY-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS STARTBR
               FILE('CUSTFILE')
               RIDFLD(WS-CUST-BROWSE-KEY)
               KEYLENGTH(WS-KEY-LENGTH)
               GTEQ
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           PERFORM CHECK-START-BROWSE
               THRU CHECK-START-BROWSE-EXIT.

       START-CUST-BROWSE-EXIT.

           EXIT.

       CHECK-START-BROWSE.

           IF WS-RESP = DFHRESP(NOTFND)
               MOVE 'Y' TO WS-END-OF-FILE
               GO TO CHECK-START-BROWSE-EXIT.

           IF WS-RESP NOT = ZERO
               MOVE 'STRT' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               MOVE 'Y' TO WS-PROCESS-FAILED
               GO TO CHECK-START-BROWSE-EXIT.

           MOVE 'Y' TO WS-BROWSE-OPEN.

       CHECK-START-BROWSE-EXIT.

           EXIT.

       READ-TRANSACTIONS.

           MOVE 119 TO WS-RECORD-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS READNEXT
               FILE('TXNID')
               INTO(TRANSACTION-RECORD)
               LENGTH(WS-RECORD-LENGTH)
               RIDFLD(WS-TXN-BROWSE-KEY)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP = DFHRESP(ENDFILE)
               MOVE 'Y' TO WS-END-OF-FILE
               GO TO READ-TRANSACTIONS-EXIT.

           IF WS-RESP = DFHRESP(NOTFND)
               MOVE 'Y' TO WS-END-OF-FILE
               GO TO READ-TRANSACTIONS-EXIT.

      * A READNEXT THROUGH THE NONUNIQUE TXNID AIX RETURNS DUPKEY
      * WHEN ANOTHER RECORD HAS THE SAME ALTERNATE KEY.  THE CURRENT
      * RECORD HAS STILL BEEN RETURNED AND MUST BE PROCESSED.
           IF WS-RESP = DFHRESP(DUPKEY)
               MOVE ZERO TO WS-RESP WS-RESP2.

           IF WS-RESP NOT = ZERO
               MOVE 'TXRD' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               MOVE 'Y' TO WS-PROCESS-FAILED
                              WS-END-OF-FILE
               GO TO READ-TRANSACTIONS-EXIT.

           IF WS-RECORD-LENGTH NOT = 119
               MOVE 'BADRECLENGTH' TO RH-ERROR-CODE
               MOVE 'Y' TO WS-PROCESS-FAILED
                              WS-END-OF-FILE
               GO TO READ-TRANSACTIONS-EXIT.

           IF TXN-STATUS NOT = 'C'
               GO TO READ-TRANSACTIONS-EXIT.

           IF TXN-CREATED-DATE NOT = REQUEST-BUSINESS-DATE
               GO TO READ-TRANSACTIONS-EXIT.

           IF TXN-CURRENCY NOT = REQUEST-CURRENCY
               GO TO READ-TRANSACTIONS-EXIT.

           IF TXN-TYPE = 'DP'
               ADD 1 TO WS-TXN-COUNT
               ADD 1 TO WS-DEPOSIT-COUNT
               ADD TXN-AMOUNT TO WS-DEPOSIT-AMOUNT
               GO TO READ-TRANSACTIONS-EXIT.

           IF TXN-TYPE = 'WD'
               ADD 1 TO WS-TXN-COUNT
               ADD 1 TO WS-WITHDRAWAL-COUNT
               ADD TXN-AMOUNT TO WS-WITHDRAWAL-AMOUNT
               GO TO READ-TRANSACTIONS-EXIT.

           IF TXN-TYPE = 'IN'
               ADD 1 TO WS-TXN-COUNT
               ADD 1 TO WS-INTEREST-COUNT
               ADD TXN-AMOUNT TO WS-INTEREST-AMOUNT.

       READ-TRANSACTIONS-EXIT.

           EXIT.

       READ-CUSTOMERS.

           MOVE 119 TO WS-RECORD-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS READNEXT
               FILE('CUSTFILE')
               INTO(CUSTOMER-RECORD)
               LENGTH(WS-RECORD-LENGTH)
               RIDFLD(WS-CUST-BROWSE-KEY)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP = DFHRESP(ENDFILE)
               MOVE 'Y' TO WS-END-OF-FILE
               GO TO READ-CUSTOMERS-EXIT.

           IF WS-RESP = DFHRESP(NOTFND)
               MOVE 'Y' TO WS-END-OF-FILE
               GO TO READ-CUSTOMERS-EXIT.

           IF WS-RESP NOT = ZERO
               MOVE 'CURD' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               MOVE 'Y' TO WS-PROCESS-FAILED
                              WS-END-OF-FILE
               GO TO READ-CUSTOMERS-EXIT.

           IF WS-RECORD-LENGTH NOT = 119
               MOVE 'BADRECLENGTH' TO RH-ERROR-CODE
               MOVE 'Y' TO WS-PROCESS-FAILED
                              WS-END-OF-FILE
               GO TO READ-CUSTOMERS-EXIT.

           ADD 1 TO WS-CUSTOMER-COUNT.

           IF CUSTOMER-STATUS = 'A'
               ADD 1 TO WS-ACTIVE-COUNT.

           IF CUSTOMER-STATUS = 'I'
               ADD 1 TO WS-INACTIVE-COUNT.

           IF CUSTOMER-CREATED-DATE = REQUEST-BUSINESS-DATE
               ADD 1 TO WS-NEW-CUSTOMER-COUNT.

       READ-CUSTOMERS-EXIT.

           EXIT.

       END-CURRENT-BROWSE.

           MOVE ZERO TO WS-ENDBR-RESP WS-ENDBR-RESP2.

           IF WS-BROWSE-TYPE = 'T'
               EXEC CICS ENDBR
                   FILE('TXNID')
                   RESP(WS-ENDBR-RESP)
                   RESP2(WS-ENDBR-RESP2)
               END-EXEC.

           IF WS-BROWSE-TYPE = 'C'
               EXEC CICS ENDBR
                   FILE('CUSTFILE')
                   RESP(WS-ENDBR-RESP)
                   RESP2(WS-ENDBR-RESP2)
               END-EXEC.

           MOVE 'N' TO WS-BROWSE-OPEN.

           IF WS-ENDBR-RESP NOT = ZERO
              AND WS-SPOOL-FAILED NOT = 'Y'
               MOVE WS-ENDBR-RESP TO WS-RESP
               MOVE WS-ENDBR-RESP2 TO WS-RESP2
               MOVE 'ENDB' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               MOVE 'Y' TO WS-PROCESS-FAILED.

       END-CURRENT-BROWSE-EXIT.

           EXIT.

       WRITE-TRANSACTION-SUMMARY.

           MOVE SPACES TO TRANSACTION-OUTPUT.
           MOVE REQUEST-BUSINESS-DATE TO TO-DATE.
           MOVE REQUEST-CURRENCY TO TO-CURRENCY.
           MOVE WS-TXN-COUNT TO TO-TXN-COUNT.
           MOVE WS-DEPOSIT-COUNT TO TO-DEPOSIT-COUNT.
           MOVE WS-DEPOSIT-AMOUNT TO TO-DEPOSIT-AMOUNT.
           MOVE WS-WITHDRAWAL-COUNT TO TO-WITHDRAWAL-COUNT.
           MOVE WS-WITHDRAWAL-AMOUNT TO TO-WITHDRAWAL-AMOUNT.
           MOVE WS-INTEREST-COUNT TO TO-INTEREST-COUNT.
           MOVE WS-INTEREST-AMOUNT TO TO-INTEREST-AMOUNT.

           MOVE SPACES TO MBR-RESULT-RECORD.
           MOVE 'MBR' TO MBR-D-PREFIX.
           MOVE ';' TO MBR-D-SEP-0
                       MBR-D-SEP-1
                       MBR-D-SEP-2
                       MBR-D-SEP-3.
           MOVE 'D' TO MBR-D-TYPE.
           MOVE 'DAYTXN' TO MBR-D-ENTITY.
           MOVE CA-REQUEST-ID TO MBR-D-REQUEST-ID.
           MOVE TRANSACTION-OUTPUT TO MBR-D-PAYLOAD.
           MOVE 'M' TO MBR-CALL-CONTROL.
           PERFORM CALL-MBRESULT
               THRU CALL-MBRESULT-EXIT.

           IF MBR-CALL-RETURN-CODE NOT = 'OK'
               MOVE 'E' TO RH-TYPE
               MOVE 'SPLT' TO WS-DIAG-STAGE
               PERFORM SET-SPOOL-DIAGNOSTIC
                   THRU SET-SPOOL-DIAGNOSTIC-EXIT
               MOVE 'Y' TO WS-SPOOL-FAILED.

       WRITE-TRANSACTION-SUMMARY-EXIT.

           EXIT.

       WRITE-CUSTOMER-SUMMARY.

           MOVE SPACES TO CUSTOMER-OUTPUT.
           MOVE REQUEST-BUSINESS-DATE TO CO-DATE.
           MOVE 'ALL' TO CO-SCOPE.
           MOVE WS-CUSTOMER-COUNT TO CO-CUSTOMER-COUNT.
           MOVE WS-ACTIVE-COUNT TO CO-ACTIVE-COUNT.
           MOVE WS-INACTIVE-COUNT TO CO-INACTIVE-COUNT.
           MOVE WS-NEW-CUSTOMER-COUNT TO CO-NEW-COUNT.

           MOVE SPACES TO MBR-RESULT-RECORD.
           MOVE 'MBR' TO MBR-D-PREFIX.
           MOVE ';' TO MBR-D-SEP-0
                       MBR-D-SEP-1
                       MBR-D-SEP-2
                       MBR-D-SEP-3.
           MOVE 'D' TO MBR-D-TYPE.
           MOVE 'DAYCUST' TO MBR-D-ENTITY.
           MOVE CA-REQUEST-ID TO MBR-D-REQUEST-ID.
           MOVE CUSTOMER-OUTPUT TO MBR-D-PAYLOAD.
           MOVE 'M' TO MBR-CALL-CONTROL.
           PERFORM CALL-MBRESULT
               THRU CALL-MBRESULT-EXIT.

           IF MBR-CALL-RETURN-CODE NOT = 'OK'
               MOVE 'E' TO RH-TYPE
               MOVE 'SPLC' TO WS-DIAG-STAGE
               PERFORM SET-SPOOL-DIAGNOSTIC
                   THRU SET-SPOOL-DIAGNOSTIC-EXIT
               MOVE 'Y' TO WS-SPOOL-FAILED.

       WRITE-CUSTOMER-SUMMARY-EXIT.

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
           MOVE 'DAYSTAT' TO RH-OPERATION.
           MOVE CA-REQUEST-ID TO RH-REQUEST-ID.
           MOVE CA-REQUEST-ID TO RH-CUSTOMER-ID.
           MOVE SPACES TO RH-STATUS.
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
