       IDENTIFICATION DIVISION.
       PROGRAM-ID. LISTCUST.

       ENVIRONMENT DIVISION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

       01  WS-RESP                 PIC S9(8) COMP.
       01  WS-RESP2                PIC S9(8) COMP.
       01  WS-ENDBR-RESP           PIC S9(8) COMP.
       01  WS-ENDBR-RESP2          PIC S9(8) COMP.
       01  WS-RECORD-LENGTH        PIC S9(4) COMP.
       01  WS-KEY-LENGTH           PIC S9(4) COMP.
       01  WS-BROWSE-OPEN          PIC X.
       01  WS-END-OF-FILE          PIC X.
       01  WS-SPOOL-FAILED         PIC X.
       01  WS-PROCESS-FAILED       PIC X.

       01  WS-LINK-RESP            PIC S9(8) COMP.
       01  WS-LINK-RESP2           PIC S9(8) COMP.
       01  WS-MBR-CALL-LENGTH      PIC S9(4) COMP VALUE +185.

      * STAGE + RESP + RESP2 FITS RH-ERROR-CODE X(20).
       01  WS-DIAGNOSTIC.
           05 WS-DIAG-STAGE        PIC X(4).
           05 FILLER               PIC X VALUE SPACE.
           05 FILLER               PIC X(2) VALUE 'R='.
           05 WS-DIAG-RESP         PIC 9(3).
           05 FILLER               PIC X(4) VALUE ' R2='.
           05 WS-DIAG-RESP2        PIC 9(5).
           05 FILLER               PIC X VALUE SPACE.

       01  WS-CUSTOMER-ID          PIC X(13).

       01  CUSTOMER-RECORD.
           05 CUSTOMER-STATUS      PIC X.
           05 CUSTOMER-ID          PIC X(13).
           05 CUSTOMER-COUNTRY     PIC X(2).
           05 CUSTOMER-NATIONAL-ID PIC X(11).
           05 CUSTOMER-FIRST-NAME  PIC X(30).
           05 CUSTOMER-LAST-NAME   PIC X(40).
           05 CUSTOMER-DATE-BIRTH  PIC X(8).
           05 CUSTOMER-CREATED-AT  PIC X(14).

       01  WS-MBR-CALL-AREA.
           COPY MBRSCA.

           COPY MBRREC.

       LINKAGE SECTION.

       01  DFHCOMMAREA.
           COPY MBGWCA.

       PROCEDURE DIVISION.

       MAIN-PROCESS.

      * MBGWCA HAS EXACTLY 855 BYTES.
           IF EIBCALEN NOT = 855
               EXEC CICS RETURN END-EXEC.

      * LIST RESULTS GO TO MBRESULT. THE SCREEN HAS NO DATA SLOT LIST.
           MOVE ZERO TO CA-DATA-COUNT.
           MOVE SPACES TO CA-DATA-RECORD
                          CA-HEADER-RECORD
                          CUSTOMER-RECORD.
           MOVE LOW-VALUES TO WS-CUSTOMER-ID.
           MOVE 'N' TO WS-BROWSE-OPEN
                       WS-END-OF-FILE
                       WS-SPOOL-FAILED
                       WS-PROCESS-FAILED.
           MOVE 119 TO WS-RECORD-LENGTH.
           MOVE 13 TO WS-KEY-LENGTH.
           MOVE ZERO TO WS-RESP
                        WS-RESP2
                        WS-ENDBR-RESP
                        WS-ENDBR-RESP2
                        WS-LINK-RESP
                        WS-LINK-RESP2.

           MOVE SPACES TO MBR-CALL-CONTROL
                          MBR-CALL-TOKEN
                          MBR-CALL-RETURN-CODE
                          MBR-CALL-RECORD
                          MBR-RESULT-RECORD.
           MOVE ZERO TO MBR-CALL-RESP
                        MBR-CALL-RESP2
                        WS-DIAG-RESP
                        WS-DIAG-RESP2.

           PERFORM PREPARE-HEADER
               THRU PREPARE-HEADER-EXIT.

           IF CA-VERSION NOT = '01'
               MOVE 'BADVERSION' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-OPERATION NOT = 'LISTCUST'
               MOVE 'BADOPERATION' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-REQUEST-ID = SPACES
               MOVE 'BADREQUEST' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-INPUT-LENGTH NOT = '0000'
               MOVE 'BADLENGTH' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS STARTBR
               FILE('CUSTFILE')
               RIDFLD(WS-CUSTOMER-ID)
               KEYLENGTH(WS-KEY-LENGTH)
               GTEQ
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

      * AN EMPTY FILE IS A VALID EMPTY LIST.
           IF WS-RESP = DFHRESP(NOTFND)
               MOVE 'S' TO RH-TYPE
               MOVE 'OK' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF WS-RESP NOT = ZERO
               MOVE 'STRT' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               GO TO FINISH-PROGRAM.

           MOVE 'Y' TO WS-BROWSE-OPEN.

           PERFORM READ-CUSTOMERS
               THRU READ-CUSTOMERS-EXIT
               UNTIL WS-END-OF-FILE = 'Y'.

           IF WS-BROWSE-OPEN = 'Y'
               PERFORM END-CUSTOMER-BROWSE
                   THRU END-CUSTOMER-BROWSE-EXIT.

      * A FAILED D FRAME HAS ALREADY CLOSED ITS SPOOL OUTPUT.
           IF WS-SPOOL-FAILED = 'Y'
               GO TO RETURN-PROGRAM.

           IF WS-PROCESS-FAILED = 'Y'
               GO TO FINISH-PROGRAM.

           MOVE 'S' TO RH-TYPE.
           MOVE 'OK' TO RH-ERROR-CODE.

       FINISH-PROGRAM.

           IF WS-BROWSE-OPEN = 'Y'
               PERFORM END-CUSTOMER-BROWSE
                   THRU END-CUSTOMER-BROWSE-EXIT.

      * WRITE FINAL S OR E AND CLOSE THE LOGICAL RESPONSE.
           PERFORM PREPARE-MBR-HEADER
               THRU PREPARE-MBR-HEADER-EXIT.
           MOVE 'F' TO MBR-CALL-CONTROL.
           PERFORM CALL-MBRESULT
               THRU CALL-MBRESULT-EXIT.

           IF MBR-CALL-RETURN-CODE NOT = 'OK'
               MOVE 'E' TO RH-TYPE
               MOVE 'SPLF' TO WS-DIAG-STAGE
               PERFORM SET-SPOOL-DIAGNOSTIC
                   THRU SET-SPOOL-DIAGNOSTIC-EXIT.

       RETURN-PROGRAM.

           EXEC CICS RETURN END-EXEC.

       READ-CUSTOMERS.

           MOVE SPACES TO CUSTOMER-RECORD.
           MOVE 119 TO WS-RECORD-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS READNEXT
               FILE('CUSTFILE')
               INTO(CUSTOMER-RECORD)
               LENGTH(WS-RECORD-LENGTH)
               RIDFLD(WS-CUSTOMER-ID)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP = DFHRESP(ENDFILE)
               MOVE 'Y' TO WS-END-OF-FILE
               GO TO READ-CUSTOMERS-EXIT.

           IF WS-RESP NOT = ZERO
               MOVE 'READ' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               MOVE 'Y' TO WS-PROCESS-FAILED
                              WS-END-OF-FILE
               GO TO READ-CUSTOMERS-EXIT.

           IF WS-RECORD-LENGTH NOT = 119
               MOVE 'BADRECLENGTH' TO RH-ERROR-CODE
               MOVE 'Y' TO WS-PROCESS-FAILED
                              WS-END-OF-FILE
               GO TO READ-CUSTOMERS-EXIT.

           PERFORM PREPARE-MBR-DATA
               THRU PREPARE-MBR-DATA-EXIT.
           MOVE 'M' TO MBR-CALL-CONTROL.
           PERFORM CALL-MBRESULT
               THRU CALL-MBRESULT-EXIT.

           IF MBR-CALL-RETURN-CODE NOT = 'OK'
               MOVE 'E' TO RH-TYPE
               MOVE 'SPLD' TO WS-DIAG-STAGE
               PERFORM SET-SPOOL-DIAGNOSTIC
                   THRU SET-SPOOL-DIAGNOSTIC-EXIT
               MOVE 'Y' TO WS-SPOOL-FAILED
                              WS-END-OF-FILE.

       READ-CUSTOMERS-EXIT.

           EXIT.

       END-CUSTOMER-BROWSE.

           MOVE ZERO TO WS-ENDBR-RESP
                        WS-ENDBR-RESP2.

           EXEC CICS ENDBR
               FILE('CUSTFILE')
               RESP(WS-ENDBR-RESP)
               RESP2(WS-ENDBR-RESP2)
           END-EXEC.

           MOVE 'N' TO WS-BROWSE-OPEN.

           IF WS-ENDBR-RESP NOT = ZERO
              AND WS-SPOOL-FAILED NOT = 'Y'
               MOVE WS-ENDBR-RESP TO WS-RESP
               MOVE WS-ENDBR-RESP2 TO WS-RESP2
               MOVE 'ENDB' TO WS-DIAG-STAGE
               PERFORM SET-CICS-DIAGNOSTIC
                   THRU SET-CICS-DIAGNOSTIC-EXIT
               MOVE 'Y' TO WS-PROCESS-FAILED.

       END-CUSTOMER-BROWSE-EXIT.

           EXIT.

       SET-CICS-DIAGNOSTIC.

           MOVE WS-RESP TO WS-DIAG-RESP.
           MOVE WS-RESP2 TO WS-DIAG-RESP2.
           MOVE WS-DIAGNOSTIC TO RH-ERROR-CODE.

       SET-CICS-DIAGNOSTIC-EXIT.

           EXIT.

       SET-SPOOL-DIAGNOSTIC.

           MOVE MBR-CALL-RESP TO WS-DIAG-RESP.
           MOVE MBR-CALL-RESP2 TO WS-DIAG-RESP2.
           MOVE WS-DIAGNOSTIC TO RH-ERROR-CODE.

       SET-SPOOL-DIAGNOSTIC-EXIT.

           EXIT.

       PREPARE-HEADER.

           MOVE 'MBR' TO RH-PREFIX.
           MOVE ';' TO RH-SEP-0
                       RH-SEP-1
                       RH-SEP-2
                       RH-SEP-3
                       RH-SEP-4
                       RH-SEP-5.
           MOVE 'E' TO RH-TYPE.
           MOVE 'LISTCUST' TO RH-OPERATION.
           MOVE CA-REQUEST-ID TO RH-REQUEST-ID.
           MOVE 'INTERNAL' TO RH-ERROR-CODE.

       PREPARE-HEADER-EXIT.

           EXIT.

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
           MOVE CUSTOMER-RECORD TO MBR-D-PAYLOAD.

       PREPARE-MBR-DATA-EXIT.

           EXIT.

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

       PREPARE-MBR-HEADER-EXIT.

           EXIT.

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
               MOVE 'LINKERR' TO MBR-CALL-RETURN-CODE
               MOVE WS-LINK-RESP TO MBR-CALL-RESP
               MOVE WS-LINK-RESP2 TO MBR-CALL-RESP2.

       CALL-MBRESULT-EXIT.

           EXIT.
