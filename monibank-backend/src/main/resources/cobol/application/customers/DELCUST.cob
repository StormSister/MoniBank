       IDENTIFICATION DIVISION.
       PROGRAM-ID. DELCUST.

       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.

           SELECT INPUT-FILE
               ASSIGN TO UT-S-INPUT.

           SELECT CUSTOMER-FILE
               ASSIGN TO DA-I-CUSTFILE
               ACCESS IS SEQUENTIAL
               RECORD KEY IS CUSTOMER-ID.

           SELECT OUTPUT-FILE
               ASSIGN TO UT-S-OUTPUT.

       DATA DIVISION.
       FILE SECTION.

       FD  INPUT-FILE
           LABEL RECORDS ARE OMITTED.

       01  INPUT-CARD              PIC X(80).

       FD  CUSTOMER-FILE
           LABEL RECORDS ARE STANDARD
           RECORD CONTAINS 119 CHARACTERS
           DATA RECORD IS CUSTOMER-RECORD.

       01  CUSTOMER-RECORD.
           05 CUSTOMER-STATUS      PIC X(1).
           05 CUSTOMER-ID          PIC X(13).
           05 COUNTRY-CODE         PIC X(2).
           05 NATIONAL-ID          PIC X(11).
           05 FIRST-NAME           PIC X(30).
           05 LAST-NAME            PIC X(40).
           05 DATE-OF-BIRTH        PIC X(8).
           05 CREATED-AT           PIC X(14).

       FD  OUTPUT-FILE
           LABEL RECORDS ARE OMITTED.

       01  OUTPUT-RECORD           PIC X(119).

       WORKING-STORAGE SECTION.

       01  REQUEST-CARD.
           05 REQUEST-CUSTOMER-ID  PIC X(13).
           05 FILLER               PIC X(67).

       01  WS-END-OF-FILE          PIC X VALUE 'N'.
       01  WS-FOUND                PIC X VALUE 'N'.

       PROCEDURE DIVISION.

           OPEN INPUT INPUT-FILE.
           OPEN OUTPUT OUTPUT-FILE.

           READ INPUT-FILE
               AT END
                   DISPLAY 'DELCUST: NO INPUT'
                   GO TO END-PROGRAM.

           MOVE INPUT-CARD
               TO REQUEST-CARD.

           DISPLAY 'DELCUST: REQUESTED ID='
               REQUEST-CUSTOMER-ID.

           OPEN INPUT CUSTOMER-FILE.

           PERFORM READ-CUSTOMER
               UNTIL WS-END-OF-FILE = 'Y'.

           IF WS-FOUND = 'Y'
               DISPLAY 'DELCUST: RECORD REMOVED'
           ELSE
               DISPLAY 'DELCUST: RECORD NOT FOUND'.

       END-PROGRAM.

           CLOSE INPUT-FILE.
           CLOSE CUSTOMER-FILE.
           CLOSE OUTPUT-FILE.

           STOP RUN.

       READ-CUSTOMER.

           READ CUSTOMER-FILE
               AT END
                   MOVE 'Y'
                       TO WS-END-OF-FILE.

           IF WS-END-OF-FILE = 'N'
               IF CUSTOMER-ID =
                       REQUEST-CUSTOMER-ID
                   MOVE 'Y'
                       TO WS-FOUND
                   DISPLAY 'DELCUST: SKIPPING RECORD'
                   DISPLAY 'CUSTOMER-ID='
                       CUSTOMER-ID
                   DISPLAY 'STATUS='
                       CUSTOMER-STATUS
               ELSE
                   MOVE CUSTOMER-RECORD
                       TO OUTPUT-RECORD
                   WRITE OUTPUT-RECORD.