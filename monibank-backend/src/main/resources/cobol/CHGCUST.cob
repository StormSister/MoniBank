       IDENTIFICATION DIVISION.
       PROGRAM-ID. CHGCUST.

       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.

           SELECT INPUT-FILE
               ASSIGN TO UT-S-INPUT.

           SELECT CUSTOMER-FILE
               ASSIGN TO DA-I-CUSTFILE
               ACCESS IS RANDOM
               RECORD KEY IS CUSTOMER-ID
               NOMINAL KEY IS WS-CUSTOMER-ID.

           SELECT RESULT-FILE
               ASSIGN TO UT-S-RESULT.

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

       FD  RESULT-FILE
           LABEL RECORDS ARE OMITTED.

       01  RESULT-RECORD.
           05 RESULT-PREFIX        PIC X(4).
           05 RESULT-TYPE          PIC X(1).
           05 RESULT-SEP-1         PIC X(1).
           05 RESULT-OPERATION     PIC X(8).
           05 RESULT-SEP-2         PIC X(1).
           05 RESULT-CUSTOMER-ID   PIC X(13).
           05 RESULT-SEP-3         PIC X(1).
           05 RESULT-STATUS        PIC X(1).
           05 RESULT-SEP-4         PIC X(1).
           05 RESULT-ERROR-CODE    PIC X(20).
           05 FILLER               PIC X(109).

       WORKING-STORAGE SECTION.

       01  REQUEST-CARD.
           05 REQUEST-CUSTOMER-ID  PIC X(13).
           05 REQUEST-STATUS       PIC X(1).
           05 FILLER               PIC X(66).

       01  WS-CUSTOMER-ID          PIC X(13).

       PROCEDURE DIVISION.

           OPEN INPUT INPUT-FILE.
           OPEN OUTPUT RESULT-FILE.

           READ INPUT-FILE
               AT END
                   PERFORM PREPARE-RESULT
                   MOVE 'E' TO RESULT-TYPE
                   MOVE 'NOINPUT' TO RESULT-ERROR-CODE
                   WRITE RESULT-RECORD
                   GO TO END-PROGRAM.

           MOVE INPUT-CARD TO REQUEST-CARD.

           MOVE REQUEST-CUSTOMER-ID TO WS-CUSTOMER-ID.

           OPEN I-O CUSTOMER-FILE.

           READ CUSTOMER-FILE
               INVALID KEY
                   PERFORM PREPARE-RESULT
                   MOVE 'E' TO RESULT-TYPE
                   MOVE REQUEST-CUSTOMER-ID
                       TO RESULT-CUSTOMER-ID
                   MOVE 'NOTFOUND' TO RESULT-ERROR-CODE
                   WRITE RESULT-RECORD
                   GO TO END-PROGRAM.

           MOVE REQUEST-STATUS TO CUSTOMER-STATUS.

           REWRITE CUSTOMER-RECORD
               INVALID KEY
                   PERFORM PREPARE-RESULT
                   MOVE 'E' TO RESULT-TYPE
                   MOVE CUSTOMER-ID
                       TO RESULT-CUSTOMER-ID
                   MOVE CUSTOMER-STATUS
                       TO RESULT-STATUS
                   MOVE 'REWRITE' TO RESULT-ERROR-CODE
                   WRITE RESULT-RECORD
                   GO TO END-PROGRAM.

           PERFORM PREPARE-RESULT.

           MOVE 'S' TO RESULT-TYPE.
           MOVE CUSTOMER-ID TO RESULT-CUSTOMER-ID.
           MOVE CUSTOMER-STATUS TO RESULT-STATUS.
           MOVE 'OK' TO RESULT-ERROR-CODE.

           WRITE RESULT-RECORD.

       END-PROGRAM.

           CLOSE INPUT-FILE.
           CLOSE CUSTOMER-FILE.
           CLOSE RESULT-FILE.

           STOP RUN.

       PREPARE-RESULT.

           MOVE SPACES TO RESULT-RECORD.
           MOVE 'MBR|' TO RESULT-PREFIX.
           MOVE '|' TO RESULT-SEP-1.
           MOVE '|' TO RESULT-SEP-2.
           MOVE '|' TO RESULT-SEP-3.
           MOVE '|' TO RESULT-SEP-4.
           MOVE 'CHGCUST' TO RESULT-OPERATION.