       IDENTIFICATION DIVISION.
       PROGRAM-ID. GETCUST.

       ENVIRONMENT DIVISION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

       01  WS-RESP                 PIC S9(8) COMP.
       01  WS-RESP2                PIC S9(8) COMP.
       01  WS-RECORD-LENGTH        PIC S9(4) COMP.
       01  WS-KEY-LENGTH           PIC S9(4) COMP.
       01  WS-CUSTOMER-ID          PIC X(13).

       01  WS-LINK-RESP            PIC S9(8) COMP.
       01  WS-LINK-RESP2           PIC S9(8) COMP.
       01  WS-MBR-CALL-LENGTH      PIC S9(4) COMP VALUE +185.

       01  WS-SPOOL-DIAGNOSTIC.
           05 FILLER                PIC X(2) VALUE 'R='.
           05 WS-SPOOL-RESP-OUT     PIC 9(3).
           05 FILLER                PIC X(4) VALUE ' R2='.
           05 WS-SPOOL-RESP2-OUT    PIC 9(5).
           05 FILLER                PIC X(6) VALUE SPACES.

       01  WS-CUSTOMER-RECORD.
           05 WS-STATUS            PIC X.
           05 WS-ID                PIC X(13).
           05 WS-COUNTRY           PIC X(2).
           05 WS-NATIONAL-ID       PIC X(11).
           05 WS-FIRST-NAME        PIC X(30).
           05 WS-LAST-NAME         PIC X(40).
           05 WS-DATE-BIRTH        PIC X(8).
           05 WS-CREATED-AT        PIC X(14).

       01  WS-MBR-CALL-AREA.
           COPY MBRSCA.

           COPY MBRREC.

       LINKAGE SECTION.

       01  DFHCOMMAREA.
           COPY MBGWCA.

       PROCEDURE DIVISION.

       MAIN-PROCESS.

      * DO NOT ACCESS AN ABSENT OR INCORRECT AREA.
           IF EIBCALEN NOT = 855
               EXEC CICS RETURN END-EXEC.

           MOVE ZERO TO CA-DATA-COUNT.
           MOVE SPACES TO CA-DATA-RECORD.
           MOVE SPACES TO CA-HEADER-RECORD.
           MOVE SPACES TO WS-CUSTOMER-RECORD.
           MOVE SPACES TO WS-CUSTOMER-ID.

           MOVE SPACES TO MBR-CALL-CONTROL.
           MOVE SPACES TO MBR-CALL-TOKEN.
           MOVE SPACES TO MBR-CALL-RETURN-CODE.
           MOVE SPACES TO MBR-CALL-RECORD.
           MOVE ZERO TO MBR-CALL-RESP
                        MBR-CALL-RESP2.
           MOVE ZERO TO WS-SPOOL-RESP-OUT
                        WS-SPOOL-RESP2-OUT.
           MOVE SPACES TO MBR-RESULT-RECORD.

           PERFORM PREPARE-HEADER.

           IF CA-VERSION NOT = '01'
               MOVE 'BADVERSION' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-OPERATION NOT = 'GETCUST'
               MOVE 'BADOPERATION' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-REQUEST-ID = SPACES
               MOVE 'BADREQUEST' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-INPUT-LENGTH NOT = '0013'
               MOVE 'BADLENGTH' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

      * ALPHANUMERIC MOVE TAKES THE FIRST 13 INPUT BYTES.
           MOVE CA-INPUT TO WS-CUSTOMER-ID.
           MOVE WS-CUSTOMER-ID TO RH-CUSTOMER-ID.

           IF WS-CUSTOMER-ID = SPACES
               MOVE 'BADID' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           MOVE 119 TO WS-RECORD-LENGTH.
           MOVE 13 TO WS-KEY-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS READ
               FILE('CUSTFILE')
               INTO(WS-CUSTOMER-RECORD)
               LENGTH(WS-RECORD-LENGTH)
               RIDFLD(WS-CUSTOMER-ID)
               KEYLENGTH(WS-KEY-LENGTH)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP = DFHRESP(NOTFND)
               MOVE 'NOTFOUND' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF WS-RESP NOT = ZERO
               MOVE 'READERR' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF WS-RECORD-LENGTH NOT = 119
               MOVE 'BADLENGTH' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           PERFORM PREPARE-DATA.

           MOVE 1 TO CA-DATA-COUNT.
           MOVE 'S' TO RH-TYPE.
           MOVE WS-ID TO RH-CUSTOMER-ID.
           MOVE WS-STATUS TO RH-STATUS.
           MOVE 'OK' TO RH-ERROR-CODE.

      * WRITE D AND KEEP ITS SYSOUT OPEN.
           PERFORM WRITE-DATA-RESULT.

           IF MBR-CALL-RETURN-CODE NOT = 'OK'
               MOVE ZERO TO CA-DATA-COUNT
               MOVE SPACES TO CA-DATA-RECORD
               MOVE 'E' TO RH-TYPE
               MOVE MBR-CALL-RESP TO WS-SPOOL-RESP-OUT
               MOVE MBR-CALL-RESP2 TO WS-SPOOL-RESP2-OUT
               MOVE WS-SPOOL-DIAGNOSTIC TO RH-ERROR-CODE
               GO TO RETURN-PROGRAM.

       FINISH-PROGRAM.

      * WRITE FINAL S OR E AND CLOSE THE SYSOUT.
           PERFORM PREPARE-MBR-HEADER.
           MOVE 'F' TO MBR-CALL-CONTROL.
           PERFORM CALL-MBRESULT THRU CALL-MBRESULT-EXIT.

           IF MBR-CALL-RETURN-CODE NOT = 'OK'
               MOVE 'E' TO RH-TYPE
               MOVE MBR-CALL-RESP TO WS-SPOOL-RESP-OUT
               MOVE MBR-CALL-RESP2 TO WS-SPOOL-RESP2-OUT
               MOVE WS-SPOOL-DIAGNOSTIC TO RH-ERROR-CODE.

       RETURN-PROGRAM.

           EXEC CICS RETURN END-EXEC.

       PREPARE-HEADER.

           MOVE 'MBR' TO RH-PREFIX.
           MOVE ';' TO RH-SEP-0 RH-SEP-1 RH-SEP-2
                       RH-SEP-3 RH-SEP-4 RH-SEP-5.
           MOVE 'E' TO RH-TYPE.
           MOVE 'GETCUST' TO RH-OPERATION.
           MOVE CA-REQUEST-ID TO RH-REQUEST-ID.
           MOVE 'INTERNAL' TO RH-ERROR-CODE.

       PREPARE-DATA.

           MOVE 'MBR' TO RD-PREFIX.
           MOVE ';' TO RD-SEP-0 RD-SEP-1
                       RD-SEP-2 RD-SEP-3.
           MOVE 'D' TO RD-TYPE.
           MOVE 'CUSTOMER' TO RD-ENTITY.
           MOVE CA-REQUEST-ID TO RD-REQUEST-ID.
           MOVE WS-CUSTOMER-RECORD TO RD-PAYLOAD.

       WRITE-DATA-RESULT.

           PERFORM PREPARE-MBR-DATA.
           MOVE 'M' TO MBR-CALL-CONTROL.
           PERFORM CALL-MBRESULT THRU CALL-MBRESULT-EXIT.

       PREPARE-MBR-DATA.

           MOVE SPACES TO MBR-RESULT-RECORD.
           MOVE 'MBR' TO MBR-D-PREFIX.
           MOVE ';' TO MBR-D-SEP-0
                       MBR-D-SEP-1
                       MBR-D-SEP-2
                       MBR-D-SEP-3.
           MOVE 'D' TO MBR-D-TYPE.
           MOVE 'CUSTOMER' TO MBR-D-ENTITY.
           MOVE CA-REQUEST-ID TO MBR-D-REQUEST-ID.
           MOVE WS-CUSTOMER-RECORD TO MBR-D-PAYLOAD.

       PREPARE-MBR-HEADER.

           MOVE SPACES TO MBR-RESULT-RECORD.
           MOVE 'MBR' TO MBR-H-PREFIX.
           MOVE ';' TO MBR-H-SEP-0
                       MBR-H-SEP-1
                       MBR-H-SEP-2
                       MBR-H-SEP-3
                       MBR-H-SEP-4
                       MBR-H-SEP-5.
           MOVE RH-TYPE TO MBR-H-TYPE.
           MOVE RH-OPERATION TO MBR-H-OPERATION.
           MOVE CA-REQUEST-ID TO MBR-H-REQUEST-ID.
           MOVE RH-CUSTOMER-ID TO MBR-H-ENTITY-ID.
           MOVE RH-STATUS TO MBR-H-STATUS.
           MOVE RH-ERROR-CODE TO MBR-H-CODE.

       CALL-MBRESULT.

           MOVE MBR-RESULT-RECORD TO MBR-CALL-RECORD.
           MOVE SPACES TO MBR-CALL-RETURN-CODE.
           MOVE ZERO TO MBR-CALL-RESP
                        MBR-CALL-RESP2
                        WS-LINK-RESP
                        WS-LINK-RESP2.

           EXEC CICS LINK
               PROGRAM('MBRESULT')
               COMMAREA(WS-MBR-CALL-AREA)
               LENGTH(WS-MBR-CALL-LENGTH)
               RESP(WS-LINK-RESP)
               RESP2(WS-LINK-RESP2)
           END-EXEC.

           IF WS-LINK-RESP NOT = ZERO
               MOVE 'LINKERR' TO MBR-CALL-RETURN-CODE.

       CALL-MBRESULT-EXIT.

           EXIT.
