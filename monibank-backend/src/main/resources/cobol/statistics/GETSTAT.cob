       IDENTIFICATION DIVISION.
       PROGRAM-ID. GETSTAT.

       ENVIRONMENT DIVISION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

       01  WS-RESP                 PIC S9(8) COMP.
       01  WS-RESP2                PIC S9(8) COMP.
       01  WS-REPORT-LENGTH        PIC S9(4) COMP.
       01  WS-REPORT-KEY-LENGTH    PIC S9(4) COMP.
       01  WS-SPOOL-FAILED         PIC X.

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

      * PHYSICAL MBANK.DAYRPT RECORD - EXACTLY 119 BYTES.
       01  DAILY-REPORT-RECORD.
           05 RPT-STATE             PIC X.
           05 RPT-KEY.
              10 RPT-BUSINESS-DATE  PIC X(8).
              10 RPT-CURRENCY       PIC X(3).
           05 RPT-OPERATION-COUNT   PIC 9(9) COMP-3.
           05 RPT-DEPOSIT-COUNT     PIC 9(9) COMP-3.
           05 RPT-WITHDRAWAL-COUNT  PIC 9(9) COMP-3.
           05 RPT-INTEREST-COUNT    PIC 9(9) COMP-3.
           05 RPT-CUSTOMER-COUNT    PIC 9(9) COMP-3.
           05 RPT-ACTIVE-COUNT      PIC 9(9) COMP-3.
           05 RPT-INACTIVE-COUNT    PIC 9(9) COMP-3.
           05 RPT-NEW-COUNT         PIC 9(9) COMP-3.
           05 RPT-DEPOSIT-AMOUNT    PIC S9(13)V99 COMP-3.
           05 RPT-WITHDRAWAL-AMOUNT PIC S9(13)V99 COMP-3.
           05 RPT-INTEREST-AMOUNT   PIC S9(13)V99 COMP-3.
           05 RPT-REQUEST-ID        PIC X(8).
           05 RPT-CLOSED-AT         PIC X(14).
           05 RPT-RESULT-CODE       PIC X(12).
           05 FILLER                PIC X(9).

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

           MOVE 'N' TO WS-SPOOL-FAILED.
           MOVE SPACES TO REQUEST-RECORD
                          DAILY-REPORT-RECORD
                          TRANSACTION-OUTPUT
                          CUSTOMER-OUTPUT
                          CA-DATA-RECORD
                          CA-HEADER-RECORD.
           MOVE 119 TO WS-REPORT-LENGTH.
           MOVE 11 TO WS-REPORT-KEY-LENGTH.
           MOVE ZERO TO CA-DATA-COUNT
                        WS-RESP
                        WS-RESP2
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

           IF CA-OPERATION NOT = 'GETSTAT'
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

           MOVE REQUEST-BUSINESS-DATE TO RPT-BUSINESS-DATE.
           MOVE REQUEST-CURRENCY TO RPT-CURRENCY.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS READ
               FILE('DAYRPT')
               INTO(DAILY-REPORT-RECORD)
               RIDFLD(RPT-KEY)
               KEYLENGTH(WS-REPORT-KEY-LENGTH)
               LENGTH(WS-REPORT-LENGTH)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP = DFHRESP(NOTFND)
               MOVE 'RPTNOTF' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF WS-RESP NOT = ZERO
               MOVE 'RPRD' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               GO TO FINISH-PROGRAM.

           IF WS-REPORT-LENGTH NOT = 119
               MOVE 'BADRECLENGTH' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF RPT-STATE NOT = 'C'
               MOVE 'BADRPTSTATE' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF RPT-BUSINESS-DATE NOT = REQUEST-BUSINESS-DATE
               MOVE 'BADRPTDATE' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF RPT-CURRENCY NOT = REQUEST-CURRENCY
               MOVE 'BADRPTCURR' TO RH-ERROR-CODE
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
           MOVE RPT-STATE TO RH-STATUS.
           MOVE RPT-RESULT-CODE TO RH-ERROR-CODE.

       FINISH-PROGRAM.

           PERFORM PREPARE-MBR-HEADER
               THRU PREPARE-MBR-HEADER-EXIT.
           MOVE 'F' TO MBR-CALL-CONTROL.
           PERFORM CALL-MBRESULT
               THRU CALL-MBRESULT-EXIT.

       RETURN-PROGRAM.

           EXEC CICS RETURN END-EXEC.

       WRITE-TRANSACTION-SUMMARY.

           MOVE SPACES TO TRANSACTION-OUTPUT.
           MOVE RPT-BUSINESS-DATE TO TO-DATE.
           MOVE RPT-CURRENCY TO TO-CURRENCY.
           MOVE RPT-OPERATION-COUNT TO TO-TXN-COUNT.
           MOVE RPT-DEPOSIT-COUNT TO TO-DEPOSIT-COUNT.
           MOVE RPT-DEPOSIT-AMOUNT TO TO-DEPOSIT-AMOUNT.
           MOVE RPT-WITHDRAWAL-COUNT TO TO-WITHDRAWAL-COUNT.
           MOVE RPT-WITHDRAWAL-AMOUNT TO TO-WITHDRAWAL-AMOUNT.
           MOVE RPT-INTEREST-COUNT TO TO-INTEREST-COUNT.
           MOVE RPT-INTEREST-AMOUNT TO TO-INTEREST-AMOUNT.

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
           MOVE RPT-BUSINESS-DATE TO CO-DATE.
           MOVE 'ALL' TO CO-SCOPE.
           MOVE RPT-CUSTOMER-COUNT TO CO-CUSTOMER-COUNT.
           MOVE RPT-ACTIVE-COUNT TO CO-ACTIVE-COUNT.
           MOVE RPT-INACTIVE-COUNT TO CO-INACTIVE-COUNT.
           MOVE RPT-NEW-COUNT TO CO-NEW-COUNT.

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
           MOVE 'GETSTAT' TO RH-OPERATION.
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
