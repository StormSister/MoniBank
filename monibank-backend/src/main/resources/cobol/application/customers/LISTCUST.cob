       IDENTIFICATION DIVISION.
       PROGRAM-ID. LISTCUST.

       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.

           SELECT INPUT-FILE
               ASSIGN TO UT-S-INPUT.

           SELECT CUSTOMER-FILE
               ASSIGN TO DA-I-CUSTFILE
               ACCESS IS SEQUENTIAL
               RECORD KEY IS CUSTOMER-ID.

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

       01  RESULT-RECORD           PIC X(160).

       WORKING-STORAGE SECTION.

       01  REQUEST-CARD.
           05 REQUEST-ID           PIC X(8).
           05 FILLER               PIC X(72).

       01  WS-END-OF-FILE          PIC X VALUE 'N'.
       01  WS-CUSTOMER-OPEN        PIC X VALUE 'N'.

       01  RESULT-HEADER.
           05 RH-PREFIX            PIC X(3).
           05 RH-SEP-0             PIC X(1).
           05 RH-TYPE              PIC X(1).
           05 RH-SEP-1             PIC X(1).
           05 RH-OPERATION         PIC X(8).
           05 RH-SEP-2             PIC X(1).
           05 RH-REQUEST-ID        PIC X(8).
           05 RH-SEP-3             PIC X(1).
           05 RH-ENTITY-ID         PIC X(13).
           05 RH-SEP-4             PIC X(1).
           05 RH-STATUS            PIC X(1).
           05 RH-SEP-5             PIC X(1).
           05 RH-ERROR-CODE        PIC X(20).
           05 FILLER               PIC X(100).

       01  RESULT-DATA.
           05 RD-PREFIX            PIC X(3).
           05 RD-SEP-0             PIC X(1).
           05 RD-TYPE              PIC X(1).
           05 RD-SEP-1             PIC X(1).
           05 RD-ENTITY            PIC X(8).
           05 RD-SEP-2             PIC X(1).
           05 RD-REQUEST-ID        PIC X(8).
           05 RD-SEP-3             PIC X(1).
           05 RD-CUSTOMER          PIC X(119).
           05 FILLER               PIC X(17).

       PROCEDURE DIVISION.

           OPEN INPUT INPUT-FILE.
           OPEN OUTPUT RESULT-FILE.

           READ INPUT-FILE
               AT END
                   PERFORM PREPARE-HEADER
                   MOVE 'E'
                       TO RH-TYPE
                   MOVE 'NOINPUT'
                       TO RH-ERROR-CODE
                   PERFORM WRITE-HEADER
                   GO TO END-PROGRAM.

           MOVE INPUT-CARD
               TO REQUEST-CARD.

           OPEN INPUT CUSTOMER-FILE.

           MOVE 'Y'
               TO WS-CUSTOMER-OPEN.

           PERFORM READ-CUSTOMER
               UNTIL WS-END-OF-FILE = 'Y'.

           PERFORM PREPARE-HEADER.

           MOVE 'S'
               TO RH-TYPE.

           MOVE 'OK'
               TO RH-ERROR-CODE.

           PERFORM WRITE-HEADER.

       END-PROGRAM.

           CLOSE INPUT-FILE.

           IF WS-CUSTOMER-OPEN = 'Y'
               CLOSE CUSTOMER-FILE.

           CLOSE RESULT-FILE.

           STOP RUN.

       READ-CUSTOMER.

           READ CUSTOMER-FILE
               AT END
                   MOVE 'Y'
                       TO WS-END-OF-FILE.

           IF WS-END-OF-FILE = 'N'
               PERFORM PREPARE-DATA
               MOVE CUSTOMER-RECORD
                   TO RD-CUSTOMER
               PERFORM WRITE-DATA.

       PREPARE-HEADER.

           MOVE SPACES
               TO RESULT-HEADER.

           MOVE 'MBR'
               TO RH-PREFIX.

           MOVE ';'
               TO RH-SEP-0.

           MOVE ';'
               TO RH-SEP-1.

           MOVE ';'
               TO RH-SEP-2.

           MOVE ';'
               TO RH-SEP-3.

           MOVE ';'
               TO RH-SEP-4.

           MOVE ';'
               TO RH-SEP-5.

           MOVE 'LISTCUST'
               TO RH-OPERATION.

           MOVE REQUEST-ID
               TO RH-REQUEST-ID.

       PREPARE-DATA.

           MOVE SPACES
               TO RESULT-DATA.

           MOVE 'MBR'
               TO RD-PREFIX.

           MOVE ';'
               TO RD-SEP-0.

           MOVE 'D'
               TO RD-TYPE.

           MOVE ';'
               TO RD-SEP-1.

           MOVE 'CUSTOMER'
               TO RD-ENTITY.

           MOVE ';'
               TO RD-SEP-2.

           MOVE REQUEST-ID
               TO RD-REQUEST-ID.

           MOVE ';'
               TO RD-SEP-3.

       WRITE-DATA.

           MOVE RESULT-DATA
               TO RESULT-RECORD.

           WRITE RESULT-RECORD.

       WRITE-HEADER.

           MOVE RESULT-HEADER
               TO RESULT-RECORD.

           WRITE RESULT-RECORD.