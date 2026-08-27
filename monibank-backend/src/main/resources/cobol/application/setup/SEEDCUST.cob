       IDENTIFICATION DIVISION.
       PROGRAM-ID. SEEDCUST.

       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.

           SELECT OUTPUT-FILE
               ASSIGN TO UT-S-OUTPUT.

       DATA DIVISION.
       FILE SECTION.

       FD  OUTPUT-FILE
           LABEL RECORDS ARE OMITTED.

       01  OUTPUT-RECORD           PIC X(119).

       WORKING-STORAGE SECTION.

       01  CUSTOMER-RECORD.
           05 CUSTOMER-STATUS      PIC X(1)
                                   VALUE 'I'.
           05 CUSTOMER-ID          PIC X(13)
                                   VALUE 'C000000000000'.
           05 COUNTRY-CODE         PIC X(2)
                                   VALUE 'ZZ'.
           05 NATIONAL-ID          PIC X(11)
                                   VALUE '00000000000'.
           05 FIRST-NAME           PIC X(30)
                   VALUE 'MONIBANK-AIX-BOOTSTRAP-SYSTEM0'.
           05 LAST-NAME            PIC X(40)
               VALUE 'TECHNICAL-BOOTSTRAP-CUSTOMER-RECORD-0000'.
           05 DATE-OF-BIRTH        PIC X(8)
                                   VALUE '19000101'.
           05 CREATED-AT           PIC X(14)
                                   VALUE '20260826084500'.

       PROCEDURE DIVISION.

       MAIN-PROCESS.

           OPEN OUTPUT OUTPUT-FILE.

           MOVE CUSTOMER-RECORD TO OUTPUT-RECORD.
           WRITE OUTPUT-RECORD.

           CLOSE OUTPUT-FILE.

           STOP RUN.