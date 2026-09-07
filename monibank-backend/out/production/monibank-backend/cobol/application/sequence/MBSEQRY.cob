       IDENTIFICATION DIVISION.
       PROGRAM-ID. MBSEQRY.

       ENVIRONMENT DIVISION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

      * MBANK.SEQ RECORD:
      * 12 BYTES KEY
      * 12 BYTES NUMBER
      * 8 BYTES FILLER

       01  SEQUENCE-KEY             PIC X(12)
                                    VALUE 'CUSTOMER    '.

       01  SEQUENCE-RECORD.
           05 SEQUENCE-NAME         PIC X(12).
           05 SEQUENCE-NUMBER       PIC X(12).
           05 FILLER                PIC X(8).

       01  WS-LENGTH                PIC S9(4) COMP
                                    VALUE +32.

       01  WS-RESP                  PIC S9(8) COMP
                                    VALUE +0.

      * STANDARD MONIBANK RESPONSE - 160 BYTES

       01  RESULT-RECORD.
           05 RR-PREFIX             PIC X(3).
           05 RR-SEP-0              PIC X(1).
           05 RR-TYPE               PIC X(1).
           05 RR-SEP-1              PIC X(1).
           05 RR-OPERATION          PIC X(8).
           05 RR-SEP-2              PIC X(1).
           05 RR-REQUEST-ID         PIC X(8).
           05 RR-SEP-3              PIC X(1).
           05 RR-ENTITY             PIC X(8).
           05 RR-SEP-4              PIC X(1).
           05 RR-ENTITY-ID          PIC X(13).
           05 RR-SEP-5              PIC X(1).
           05 RR-STATUS             PIC X(1).
           05 RR-SEP-6              PIC X(1).
           05 RR-ERROR-CODE         PIC X(20).
           05 FILLER                PIC X(91).

       PROCEDURE DIVISION.

       MAIN-PROCESS.

           MOVE SPACES TO RESULT-RECORD.

           MOVE 'MBR'      TO RR-PREFIX.
           MOVE ';'        TO RR-SEP-0.
           MOVE ';'        TO RR-SEP-1.
           MOVE ';'        TO RR-SEP-2.
           MOVE ';'        TO RR-SEP-3.
           MOVE ';'        TO RR-SEP-4.
           MOVE ';'        TO RR-SEP-5.
           MOVE ';'        TO RR-SEP-6.

           MOVE 'MBSEQRY'  TO RR-OPERATION.
           MOVE '00000000' TO RR-REQUEST-ID.
           MOVE 'SEQUENCE' TO RR-ENTITY.

           EXEC CICS
               READ
               DATASET('SEQFILE')
               INTO(SEQUENCE-RECORD)
               RIDFLD(SEQUENCE-KEY)
               LENGTH(WS-LENGTH)
               RESP(WS-RESP)
           END-EXEC.

           IF WS-RESP NOT = 0
               GO TO READ-ERROR.

           MOVE 'S'             TO RR-TYPE.
           MOVE SEQUENCE-NUMBER TO RR-ENTITY-ID.
           MOVE 'A'             TO RR-STATUS.
           MOVE 'OK'            TO RR-ERROR-CODE.

           GO TO SEND-RESULT.

       READ-ERROR.

           MOVE 'E'           TO RR-TYPE.
           MOVE 'CUSTOMER'    TO RR-ENTITY-ID.
           MOVE 'I'           TO RR-STATUS.
           MOVE 'SEQREADFAIL' TO RR-ERROR-CODE.

       SEND-RESULT.

           EXEC CICS
               SEND TEXT
               FROM(RESULT-RECORD)
               ERASE
               FREEKB
           END-EXEC.

           EXEC CICS
               RETURN
           END-EXEC.

           GOBACK.