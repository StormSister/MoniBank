       IDENTIFICATION DIVISION.
       PROGRAM-ID. MBHELLO.

       ENVIRONMENT DIVISION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

       01  RESULT-RECORD.
           05 RR-PREFIX        PIC X(3)  VALUE 'MBR'.
           05 RR-SEP-1         PIC X(1)  VALUE ';'.
           05 RR-TYPE          PIC X(1)  VALUE 'S'.
           05 RR-SEP-2         PIC X(1)  VALUE ';'.
           05 RR-OPERATION     PIC X(8)  VALUE 'MBHELLO'.
           05 RR-SEP-3         PIC X(1)  VALUE ';'.
           05 RR-REQUEST-ID    PIC X(8)  VALUE '00000000'.
           05 RR-SEP-4         PIC X(1)  VALUE ';'.
           05 RR-ENTITY        PIC X(8)  VALUE 'SYSTEM'.
           05 RR-SEP-5         PIC X(1)  VALUE ';'.
           05 RR-ENTITY-ID     PIC X(13) VALUE 'KICKS'.
           05 RR-SEP-6         PIC X(1)  VALUE ';'.
           05 RR-STATUS        PIC X(1)  VALUE 'A'.
           05 RR-SEP-7         PIC X(1)  VALUE ';'.
           05 RR-ERROR-CODE    PIC X(20) VALUE 'OK'.
           05 FILLER           PIC X(91) VALUE SPACES.

       PROCEDURE DIVISION.

           EXEC CICS
               SEND TEXT
               FROM(RESULT-RECORD)
               ERASE
               FREEKB
           END-EXEC.

           EXEC CICS
               RETURN
           END-EXEC.