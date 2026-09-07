       IDENTIFICATION DIVISION.
       PROGRAM-ID. SEEDCARD.

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

       01  CARD-RECORD.
           05 CARD-STATUS             PIC X.
           05 CARD-ID                 PIC X(13).
           05 CARD-ACCOUNT-ID         PIC X(13).
           05 CARD-CUSTOMER-ID        PIC X(13).
           05 CARD-NUMBER             PIC X(16).
           05 CARD-TYPE               PIC X.
           05 CARD-NETWORK            PIC X(2).
           05 CARD-EXPIRY             PIC X(6).
           05 CARD-DAILY-LIMIT        PIC S9(13)V99 COMP-3.
           05 CARD-DAILY-SPENT        PIC S9(13)V99 COMP-3.
           05 CARD-SPENT-DATE         PIC X(8).
           05 CARD-CREATED-AT         PIC X(14).
           05 CARD-UPDATED-AT         PIC X(14).
           05 CARD-FILLER             PIC X(2).

       PROCEDURE DIVISION.

       MAIN-PROCESS.

           OPEN OUTPUT OUTPUT-FILE.

           MOVE 'A' TO CARD-STATUS.
           MOVE 'K000000000000' TO CARD-ID.
           MOVE 'A000000000000' TO CARD-ACCOUNT-ID.
           MOVE 'C000000000006' TO CARD-CUSTOMER-ID.
           MOVE '4111111111111111' TO CARD-NUMBER.
           MOVE 'D' TO CARD-TYPE.
           MOVE 'VS' TO CARD-NETWORK.
           MOVE '203012' TO CARD-EXPIRY.
           MOVE 2000 TO CARD-DAILY-LIMIT.
           MOVE ZERO TO CARD-DAILY-SPENT.
           MOVE '20260904' TO CARD-SPENT-DATE.
           MOVE '20260904000000' TO CARD-CREATED-AT
                                    CARD-UPDATED-AT.
           MOVE SPACES TO CARD-FILLER.

           MOVE CARD-RECORD TO OUTPUT-RECORD.
           WRITE OUTPUT-RECORD.

           CLOSE OUTPUT-FILE.

           DISPLAY 'SEEDCARD CREATED K000000000000'.
           STOP RUN.

