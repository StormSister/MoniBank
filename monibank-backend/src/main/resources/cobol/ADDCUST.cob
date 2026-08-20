       IDENTIFICATION DIVISION.
       PROGRAM-ID. ADDCUST.

       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.

           SELECT INPUT-FILE
               ASSIGN TO UT-S-INPUT.

           SELECT OUTPUT-FILE
               ASSIGN TO UT-S-OUTPUT.

           SELECT RESULT-FILE
               ASSIGN TO UT-S-RESULT.

       DATA DIVISION.
       FILE SECTION.

       FD  INPUT-FILE
           LABEL RECORDS ARE OMITTED.

       01  INPUT-CARD              PIC X(80).

       FD  OUTPUT-FILE
           LABEL RECORDS ARE OMITTED.

       01  OUTPUT-RECORD           PIC X(119).

       FD  RESULT-FILE
           LABEL RECORDS ARE OMITTED.

       01  RESULT-RECORD           PIC X(160).

       WORKING-STORAGE SECTION.

      * -------------------------------------------------
      * REQUEST
      *
      * 8   REQUEST-ID
      * 119 CUSTOMER
      * ----------------
      * 127 CHARACTERS
      *
      * JCL reader dostarcza rekordy po 80 znakow,
      * dlatego request skladamy z dwoch kart.
      * -------------------------------------------------

       01  REQUEST-RECORD.
           05 REQUEST-PART-1       PIC X(80).
           05 REQUEST-PART-2       PIC X(47).

       01  REQUEST-FIELDS REDEFINES REQUEST-RECORD.
           05 REQUEST-ID           PIC X(8).
           05 REQUEST-CUSTOMER     PIC X(119).

      * -------------------------------------------------
      * CUSTOMER RECORD
      * Rekord zapisywany do MBANK.CUST.
      * -------------------------------------------------

       01  CUSTOMER-RECORD.
           05 CUSTOMER-STATUS      PIC X(1).
           05 CUSTOMER-ID          PIC X(13).
           05 COUNTRY-CODE         PIC X(2).
           05 NATIONAL-ID          PIC X(11).
           05 FIRST-NAME           PIC X(30).
           05 LAST-NAME            PIC X(40).
           05 DATE-OF-BIRTH        PIC X(8).
           05 CREATED-AT           PIC X(14).

      * -------------------------------------------------
      * TERMINAL RESULT
      *
      * MBR;S;ADDCUST;Rxxxxxxx;CUSTOMER-ID;STATUS;CODE
      * albo
      * MBR;E;ADDCUST;Rxxxxxxx;CUSTOMER-ID;STATUS;CODE
      * -------------------------------------------------

       01  RESULT-HEADER.
           05 RH-PREFIX            PIC X(3).
           05 RH-SEP-0             PIC X(1).
           05 RH-TYPE              PIC X(1).
           05 RH-SEP-1             PIC X(1).
           05 RH-OPERATION         PIC X(7).
           05 RH-SEP-2             PIC X(1).
           05 RH-REQUEST-ID        PIC X(8).
           05 RH-SEP-3             PIC X(1).
           05 RH-CUSTOMER-ID       PIC X(13).
           05 RH-SEP-4             PIC X(1).
           05 RH-STATUS            PIC X(1).
           05 RH-SEP-5             PIC X(1).
           05 RH-ERROR-CODE        PIC X(20).
           05 FILLER               PIC X(101).

      * -------------------------------------------------
      * DATA RESULT
      *
      * MBR;D;CUSTOMER;Rxxxxxxx;<119 CHAR CUSTOMER>
      * -------------------------------------------------

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
           OPEN OUTPUT OUTPUT-FILE.
           OPEN OUTPUT RESULT-FILE.

      * -------------------------------------------------
      * CARD 1
      * -------------------------------------------------

           READ INPUT-FILE
               AT END
                   PERFORM PREPARE-HEADER
                   MOVE 'E'
                       TO RH-TYPE
                   MOVE 'NOINPUT1'
                       TO RH-ERROR-CODE
                   PERFORM WRITE-HEADER
                   GO TO END-PROGRAM.

           MOVE INPUT-CARD
               TO REQUEST-PART-1.

      * -------------------------------------------------
      * CARD 2
      * -------------------------------------------------

           READ INPUT-FILE
               AT END
                   PERFORM PREPARE-HEADER
                   MOVE 'E'
                       TO RH-TYPE
                   MOVE 'NOINPUT2'
                       TO RH-ERROR-CODE
                   PERFORM WRITE-HEADER
                   GO TO END-PROGRAM.

           MOVE INPUT-CARD
               TO REQUEST-PART-2.

      * -------------------------------------------------
      * Wyciagamy CUSTOMER ze 127-znakowego requestu.
      * -------------------------------------------------

           MOVE REQUEST-CUSTOMER
               TO CUSTOMER-RECORD.

      * -------------------------------------------------
      * Rekord przygotowany dla OUTPUT.
      * LOADVSAM w JCL zrobi REPRO do MBANK.CUST.
      * -------------------------------------------------

           MOVE CUSTOMER-RECORD
               TO OUTPUT-RECORD.

           WRITE OUTPUT-RECORD.

      * -------------------------------------------------
      * DATA record.
      *
      * Zwracamy caly CUSTOMER, dzieki czemu Java/front
      * nie musza ponownie robic READ ALL.
      * -------------------------------------------------

           PERFORM PREPARE-DATA.

           MOVE CUSTOMER-RECORD
               TO RD-CUSTOMER.

           PERFORM WRITE-DATA.

      * -------------------------------------------------
      * Terminal SUCCESS zawsze NA KONCU.
      * Listener TCP zakonczy request dopiero po S.
      * -------------------------------------------------

           PERFORM PREPARE-HEADER.

           MOVE 'S'
               TO RH-TYPE.

           MOVE CUSTOMER-ID
               TO RH-CUSTOMER-ID.

           MOVE CUSTOMER-STATUS
               TO RH-STATUS.

           MOVE 'OK'
               TO RH-ERROR-CODE.

           PERFORM WRITE-HEADER.

       END-PROGRAM.

           CLOSE INPUT-FILE.
           CLOSE OUTPUT-FILE.
           CLOSE RESULT-FILE.

           STOP RUN.

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

           MOVE 'ADDCUST'
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