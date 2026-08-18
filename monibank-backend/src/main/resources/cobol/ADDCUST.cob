       IDENTIFICATION DIVISION.
       PROGRAM-ID. ADDCUST.

       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.

           SELECT INPUT-FILE
               ASSIGN TO UT-S-INPUT.

           SELECT OUTPUT-FILE
               ASSIGN TO UT-S-OUTPUT.

       DATA DIVISION.
       FILE SECTION.

       FD  INPUT-FILE
           LABEL RECORDS ARE OMITTED.
       01  INPUT-CARD              PIC X(80).

       FD  OUTPUT-FILE
           LABEL RECORDS ARE OMITTED.
       01  OUTPUT-RECORD           PIC X(119).

       WORKING-STORAGE SECTION.

       01  CUSTOMER-RECORD.
           05 CUSTOMER-PART-1      PIC X(80).
           05 CUSTOMER-PART-2      PIC X(39).

       01  CUSTOMER-FIELDS REDEFINES CUSTOMER-RECORD.
           05 CUSTOMER-STATUS      PIC X(1).
           05 CUSTOMER-ID          PIC X(13).
           05 COUNTRY-CODE         PIC X(2).
           05 NATIONAL-ID          PIC X(11).
           05 FIRST-NAME           PIC X(30).
           05 LAST-NAME            PIC X(40).
           05 DATE-OF-BIRTH        PIC X(8).
           05 CREATED-AT           PIC X(14).

       PROCEDURE DIVISION.

           OPEN INPUT INPUT-FILE.
           OPEN OUTPUT OUTPUT-FILE.

           READ INPUT-FILE
               AT END
                   DISPLAY 'MBANK|RESULT=ERROR|CODE=NOINPUT1'
                   GO TO END-PROGRAM.

           MOVE INPUT-CARD TO CUSTOMER-PART-1.

           READ INPUT-FILE
               AT END
                   DISPLAY 'MBANK|RESULT=ERROR|CODE=NOINPUT2'
                   GO TO END-PROGRAM.

           MOVE INPUT-CARD TO CUSTOMER-PART-2.

           MOVE CUSTOMER-RECORD TO OUTPUT-RECORD.

           WRITE OUTPUT-RECORD.

           DISPLAY 'MBANK|RESULT=PREPARED'.
           DISPLAY 'CUSTOMER-ID=' CUSTOMER-ID.
           DISPLAY 'STATUS=' CUSTOMER-STATUS.

       END-PROGRAM.

           CLOSE INPUT-FILE.
           CLOSE OUTPUT-FILE.

           STOP RUN.