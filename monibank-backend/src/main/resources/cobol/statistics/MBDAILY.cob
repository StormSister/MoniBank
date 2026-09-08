       IDENTIFICATION DIVISION.
       PROGRAM-ID. MBDAILY.

       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.

           SELECT CONTROL-FILE
               ASSIGN TO UT-S-CTLIN.

           SELECT CUSTOMER-FILE
               ASSIGN TO UT-S-CUSTIN.

           SELECT TRANSACTION-FILE
               ASSIGN TO UT-S-TXNIN.

           SELECT REPORT-FILE
               ASSIGN TO UT-S-REPORT.

       DATA DIVISION.
       FILE SECTION.

       FD  CONTROL-FILE
           LABEL RECORDS ARE OMITTED
           RECORDING MODE IS F
           BLOCK CONTAINS 0 RECORDS
           RECORD CONTAINS 80 CHARACTERS.

       01  CONTROL-RECORD.
           05 CONTROL-BUSINESS-DATE PIC X(8).
           05 CONTROL-CURRENCY      PIC X(3).
           05 CONTROL-FILLER        PIC X(69).

       FD  CUSTOMER-FILE
           LABEL RECORDS ARE STANDARD
           RECORDING MODE IS F
           BLOCK CONTAINS 0 RECORDS
           RECORD CONTAINS 119 CHARACTERS.

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

       FD  TRANSACTION-FILE
           LABEL RECORDS ARE STANDARD
           RECORDING MODE IS F
           BLOCK CONTAINS 0 RECORDS
           RECORD CONTAINS 119 CHARACTERS.

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
           05 TXN-FILLER            PIC X.

       FD  REPORT-FILE
           LABEL RECORDS ARE STANDARD
           RECORDING MODE IS F
           BLOCK CONTAINS 0 RECORDS
           RECORD CONTAINS 160 CHARACTERS.

       01  REPORT-OUTPUT            PIC X(160).

       WORKING-STORAGE SECTION.

       01  WS-END-FLAGS.
           05 WS-CUSTOMER-EOF        PIC X VALUE 'N'.
           05 WS-TXN-EOF             PIC X VALUE 'N'.

       01  WS-CONTROL.
           05 WS-BUSINESS-DATE       PIC X(8).
           05 WS-CURRENCY            PIC X(3).

       01  WS-COUNTERS COMP-3.
           05 WS-TXN-COUNT           PIC 9(9) VALUE ZERO.
           05 WS-DEPOSIT-COUNT       PIC 9(9) VALUE ZERO.
           05 WS-WITHDRAWAL-COUNT    PIC 9(9) VALUE ZERO.
           05 WS-INTEREST-COUNT      PIC 9(9) VALUE ZERO.
           05 WS-CUSTOMER-COUNT      PIC 9(9) VALUE ZERO.
           05 WS-ACTIVE-COUNT        PIC 9(9) VALUE ZERO.
           05 WS-INACTIVE-COUNT      PIC 9(9) VALUE ZERO.
           05 WS-NEW-CUSTOMER-COUNT  PIC 9(9) VALUE ZERO.

       01  WS-AMOUNTS COMP-3.
           05 WS-DEPOSIT-AMOUNT      PIC S9(13)V99 VALUE ZERO.
           05 WS-WITHDRAWAL-AMOUNT   PIC S9(13)V99 VALUE ZERO.
           05 WS-INTEREST-AMOUNT     PIC S9(13)V99 VALUE ZERO.

       01  REPORT-RECORD            PIC X(160).

       01  HEADER-LINE REDEFINES REPORT-RECORD.
           05 HL-PREFIX              PIC X(3).
           05 HL-SEP-0               PIC X.
           05 HL-TYPE                PIC X.
           05 HL-SEP-1               PIC X.
           05 HL-DATE                PIC X(8).
           05 HL-SEP-2               PIC X.
           05 HL-CURRENCY            PIC X(3).
           05 HL-SEP-3               PIC X.
           05 HL-STATE               PIC X(6).
           05 HL-FILLER              PIC X(135).

       01  TXN-LINE REDEFINES REPORT-RECORD.
           05 TL-PREFIX              PIC X(3).
           05 TL-SEP-0               PIC X.
           05 TL-TYPE                PIC X.
           05 TL-SEP-1               PIC X.
           05 TL-DATE                PIC X(8).
           05 TL-SEP-2               PIC X.
           05 TL-CURRENCY            PIC X(3).
           05 TL-SEP-3               PIC X.
           05 TL-TXN-COUNT           PIC 9(9).
           05 TL-SEP-4               PIC X.
           05 TL-DEPOSIT-COUNT       PIC 9(9).
           05 TL-SEP-5               PIC X.
           05 TL-DEPOSIT-AMOUNT      PIC +9(13).99.
           05 TL-SEP-6               PIC X.
           05 TL-WITHDRAWAL-COUNT    PIC 9(9).
           05 TL-SEP-7               PIC X.
           05 TL-WITHDRAWAL-AMOUNT   PIC +9(13).99.
           05 TL-SEP-8               PIC X.
           05 TL-INTEREST-COUNT      PIC 9(9).
           05 TL-SEP-9               PIC X.
           05 TL-INTEREST-AMOUNT     PIC +9(13).99.
           05 TL-FILLER              PIC X(48).

       01  CUSTOMER-LINE REDEFINES REPORT-RECORD.
           05 CL-PREFIX              PIC X(3).
           05 CL-SEP-0               PIC X.
           05 CL-TYPE                PIC X.
           05 CL-SEP-1               PIC X.
           05 CL-DATE                PIC X(8).
           05 CL-SEP-2               PIC X.
           05 CL-SCOPE               PIC X(3).
           05 CL-SEP-3               PIC X.
           05 CL-CUSTOMER-COUNT      PIC 9(9).
           05 CL-SEP-4               PIC X.
           05 CL-ACTIVE-COUNT        PIC 9(9).
           05 CL-SEP-5               PIC X.
           05 CL-INACTIVE-COUNT      PIC 9(9).
           05 CL-SEP-6               PIC X.
           05 CL-NEW-COUNT           PIC 9(9).
           05 CL-FILLER              PIC X(102).

       01  END-LINE REDEFINES REPORT-RECORD.
           05 EL-PREFIX              PIC X(3).
           05 EL-SEP-0               PIC X.
           05 EL-TYPE                PIC X.
           05 EL-SEP-1               PIC X.
           05 EL-DATE                PIC X(8).
           05 EL-SEP-2               PIC X.
           05 EL-CURRENCY            PIC X(3).
           05 EL-SEP-3               PIC X.
           05 EL-RESULT              PIC X(2).
           05 EL-FILLER              PIC X(139).

       PROCEDURE DIVISION.

       MAIN-PROCESS.

           OPEN INPUT CONTROL-FILE.

           READ CONTROL-FILE
               AT END MOVE SPACES TO CONTROL-RECORD.

           MOVE CONTROL-BUSINESS-DATE TO WS-BUSINESS-DATE.
           MOVE CONTROL-CURRENCY TO WS-CURRENCY.

           CLOSE CONTROL-FILE.

           IF WS-BUSINESS-DATE = SPACES
               DISPLAY 'MBDAILY BUSINESS DATE IS REQUIRED'
               MOVE 12 TO RETURN-CODE
               STOP RUN.

           IF WS-BUSINESS-DATE IS NOT NUMERIC
               DISPLAY 'MBDAILY BUSINESS DATE MUST BE YYYYMMDD'
               MOVE 12 TO RETURN-CODE
               STOP RUN.

           IF WS-CURRENCY = SPACES
               DISPLAY 'MBDAILY CURRENCY IS REQUIRED'
               MOVE 12 TO RETURN-CODE
               STOP RUN.

           OPEN INPUT CUSTOMER-FILE TRANSACTION-FILE
                OUTPUT REPORT-FILE.

           PERFORM READ-CUSTOMERS THRU READ-CUSTOMERS-EXIT
               UNTIL WS-CUSTOMER-EOF = 'Y'.

           PERFORM READ-TRANSACTIONS THRU READ-TRANSACTIONS-EXIT
               UNTIL WS-TXN-EOF = 'Y'.

           PERFORM WRITE-REPORT THRU WRITE-REPORT-EXIT.

           MOVE ZERO TO RETURN-CODE.

       CLOSE-AND-STOP.

           CLOSE CUSTOMER-FILE TRANSACTION-FILE REPORT-FILE.

           DISPLAY 'MBDAILY CLOSED ' WS-BUSINESS-DATE
               ' ' WS-CURRENCY.

           STOP RUN.

       READ-CUSTOMERS.

           READ CUSTOMER-FILE
               AT END MOVE 'Y' TO WS-CUSTOMER-EOF.

           IF WS-CUSTOMER-EOF = 'Y'
               GO TO READ-CUSTOMERS-EXIT.

           ADD 1 TO WS-CUSTOMER-COUNT.

           IF CUSTOMER-STATUS = 'A'
               ADD 1 TO WS-ACTIVE-COUNT.

           IF CUSTOMER-STATUS = 'I'
               ADD 1 TO WS-INACTIVE-COUNT.

           IF CUSTOMER-CREATED-DATE = WS-BUSINESS-DATE
               ADD 1 TO WS-NEW-CUSTOMER-COUNT.

       READ-CUSTOMERS-EXIT.

           EXIT.

       READ-TRANSACTIONS.

           READ TRANSACTION-FILE
               AT END MOVE 'Y' TO WS-TXN-EOF.

           IF WS-TXN-EOF = 'Y'
               GO TO READ-TRANSACTIONS-EXIT.

           IF TXN-STATUS NOT = 'C'
               GO TO READ-TRANSACTIONS-EXIT.

           IF TXN-CREATED-DATE NOT = WS-BUSINESS-DATE
               GO TO READ-TRANSACTIONS-EXIT.

           IF TXN-CURRENCY NOT = WS-CURRENCY
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

       WRITE-REPORT.

           MOVE SPACES TO REPORT-RECORD.
           MOVE 'MBS' TO HL-PREFIX.
           MOVE ';' TO HL-SEP-0 HL-SEP-1 HL-SEP-2 HL-SEP-3.
           MOVE 'H' TO HL-TYPE.
           MOVE WS-BUSINESS-DATE TO HL-DATE.
           MOVE WS-CURRENCY TO HL-CURRENCY.
           MOVE 'CLOSED' TO HL-STATE.
           MOVE REPORT-RECORD TO REPORT-OUTPUT.
           WRITE REPORT-OUTPUT.
           DISPLAY REPORT-RECORD.

           MOVE SPACES TO REPORT-RECORD.
           MOVE 'MBS' TO TL-PREFIX.
           MOVE ';' TO TL-SEP-0 TL-SEP-1 TL-SEP-2 TL-SEP-3
                       TL-SEP-4 TL-SEP-5 TL-SEP-6 TL-SEP-7
                       TL-SEP-8 TL-SEP-9.
           MOVE 'T' TO TL-TYPE.
           MOVE WS-BUSINESS-DATE TO TL-DATE.
           MOVE WS-CURRENCY TO TL-CURRENCY.
           MOVE WS-TXN-COUNT TO TL-TXN-COUNT.
           MOVE WS-DEPOSIT-COUNT TO TL-DEPOSIT-COUNT.
           MOVE WS-DEPOSIT-AMOUNT TO TL-DEPOSIT-AMOUNT.
           MOVE WS-WITHDRAWAL-COUNT TO TL-WITHDRAWAL-COUNT.
           MOVE WS-WITHDRAWAL-AMOUNT TO TL-WITHDRAWAL-AMOUNT.
           MOVE WS-INTEREST-COUNT TO TL-INTEREST-COUNT.
           MOVE WS-INTEREST-AMOUNT TO TL-INTEREST-AMOUNT.
           MOVE REPORT-RECORD TO REPORT-OUTPUT.
           WRITE REPORT-OUTPUT.
           DISPLAY REPORT-RECORD.

           MOVE SPACES TO REPORT-RECORD.
           MOVE 'MBS' TO CL-PREFIX.
           MOVE ';' TO CL-SEP-0 CL-SEP-1 CL-SEP-2 CL-SEP-3
                       CL-SEP-4 CL-SEP-5 CL-SEP-6.
           MOVE 'C' TO CL-TYPE.
           MOVE WS-BUSINESS-DATE TO CL-DATE.
           MOVE 'ALL' TO CL-SCOPE.
           MOVE WS-CUSTOMER-COUNT TO CL-CUSTOMER-COUNT.
           MOVE WS-ACTIVE-COUNT TO CL-ACTIVE-COUNT.
           MOVE WS-INACTIVE-COUNT TO CL-INACTIVE-COUNT.
           MOVE WS-NEW-CUSTOMER-COUNT TO CL-NEW-COUNT.
           MOVE REPORT-RECORD TO REPORT-OUTPUT.
           WRITE REPORT-OUTPUT.
           DISPLAY REPORT-RECORD.

           MOVE SPACES TO REPORT-RECORD.
           MOVE 'MBS' TO EL-PREFIX.
           MOVE ';' TO EL-SEP-0 EL-SEP-1 EL-SEP-2 EL-SEP-3.
           MOVE 'E' TO EL-TYPE.
           MOVE WS-BUSINESS-DATE TO EL-DATE.
           MOVE WS-CURRENCY TO EL-CURRENCY.
           MOVE 'OK' TO EL-RESULT.
           MOVE REPORT-RECORD TO REPORT-OUTPUT.
           WRITE REPORT-OUTPUT.
           DISPLAY REPORT-RECORD.

       WRITE-REPORT-EXIT.

           EXIT.
