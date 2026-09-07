       IDENTIFICATION DIVISION.
       PROGRAM-ID. ADDCUSG.

       ENVIRONMENT DIVISION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

       01  WS-RESP                 PIC S9(8) COMP.
       01  WS-RESP2                PIC S9(8) COMP.
       01  WS-UNLOCK-RESP          PIC S9(8) COMP.
       01  WS-SEQ-LENGTH           PIC S9(4) COMP.
       01  WS-CUSTOMER-LENGTH      PIC S9(4) COMP.
       01  WS-SEQ-KEY-LENGTH       PIC S9(4) COMP.
       01  WS-CUST-KEY-LENGTH      PIC S9(4) COMP.
       01  WS-SEQ-LOCKED           PIC X.

       01  WS-LINK-RESP            PIC S9(8) COMP.
       01  WS-LINK-RESP2           PIC S9(8) COMP.
       01  WS-MBR-CALL-LENGTH      PIC S9(4) COMP VALUE +185.

       01  WS-WRITE-ERROR.
           05 FILLER               PIC X(2) VALUE 'R='.
           05 WS-RESP-DISPLAY      PIC +9(4).
           05 FILLER               PIC X(4) VALUE ';R2='.
           05 WS-RESP2-DISPLAY     PIC +9(4).
           05 FILLER               PIC X(4) VALUE SPACES.

       01  WS-SPOOL-DIAGNOSTIC.
           05 FILLER               PIC X(2) VALUE 'R='.
           05 WS-SPOOL-RESP-OUT    PIC 9(3).
           05 FILLER               PIC X(4) VALUE ' R2='.
           05 WS-SPOOL-RESP2-OUT   PIC 9(5).
           05 FILLER               PIC X(6) VALUE SPACES.

      * INPUT IS 105 BYTES. REQUEST ID IS IN MBGWCA.
       01  REQUEST-RECORD.
           05 REQUEST-COUNTRY      PIC X(2).
           05 REQUEST-NATIONAL-ID  PIC X(11).
           05 REQUEST-FIRST-NAME   PIC X(30).
           05 REQUEST-LAST-NAME    PIC X(40).
           05 REQUEST-DATE-BIRTH   PIC X(8).
           05 REQUEST-CREATED-AT   PIC X(14).

       01  SEQUENCE-KEY            PIC X(12)
                                   VALUE 'CUSTOMER    '.

       01  SEQUENCE-RECORD.
           05 SEQUENCE-NAME        PIC X(12).
           05 SEQUENCE-NUMBER      PIC 9(12).
           05 FILLER               PIC X(8).

       01  GENERATED-CUSTOMER-ID.
           05 GENERATED-PREFIX     PIC X.
           05 GENERATED-NUMBER     PIC 9(12).

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

           IF EIBCALEN NOT = 855
               EXEC CICS RETURN END-EXEC.

           MOVE 'N' TO WS-SEQ-LOCKED.
           MOVE ZERO TO CA-DATA-COUNT.
           MOVE SPACES TO CA-DATA-RECORD
                          CA-HEADER-RECORD.
           MOVE SPACES TO REQUEST-RECORD
                          CUSTOMER-RECORD
                          SEQUENCE-RECORD
                          GENERATED-CUSTOMER-ID.
           MOVE ZERO TO WS-RESP
                        WS-RESP2
                        WS-UNLOCK-RESP
                        WS-LINK-RESP
                        WS-LINK-RESP2.
           MOVE 12 TO WS-SEQ-KEY-LENGTH.
           MOVE 13 TO WS-CUST-KEY-LENGTH.

           MOVE SPACES TO MBR-CALL-CONTROL
                          MBR-CALL-TOKEN
                          MBR-CALL-RETURN-CODE
                          MBR-CALL-RECORD
                          MBR-RESULT-RECORD.
           MOVE ZERO TO MBR-CALL-RESP
                        MBR-CALL-RESP2
                        WS-SPOOL-RESP-OUT
                        WS-SPOOL-RESP2-OUT.

           PERFORM PREPARE-HEADER
               THRU PREPARE-HEADER-EXIT.

           IF CA-VERSION NOT = '01'
               MOVE 'BADVERSION' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-OPERATION NOT = 'ADDCUST'
               MOVE 'BADOPERATION' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-REQUEST-ID = SPACES
               MOVE 'BADREQUEST' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-INPUT-LENGTH NOT = '0105'
               MOVE 'BADLENGTH' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           MOVE CA-INPUT TO REQUEST-RECORD.

           IF REQUEST-COUNTRY = SPACES
               MOVE 'BADCOUNTRY' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-NATIONAL-ID = SPACES
               MOVE 'BADNATIONALID' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-FIRST-NAME = SPACES
               MOVE 'BADFIRSTNAME' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-LAST-NAME = SPACES
               MOVE 'BADLASTNAME' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

      * BASIC FORMAT CHECKS, NOT FULL CALENDAR VALIDATION.
           IF REQUEST-DATE-BIRTH IS NOT NUMERIC
               MOVE 'BADDOB' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-CREATED-AT IS NOT NUMERIC
               MOVE 'BADCREATEDAT' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           MOVE 32 TO WS-SEQ-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS READ
               FILE('SEQFILE')
               INTO(SEQUENCE-RECORD)
               RIDFLD(SEQUENCE-KEY)
               KEYLENGTH(WS-SEQ-KEY-LENGTH)
               LENGTH(WS-SEQ-LENGTH)
               UPDATE
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP NOT = ZERO
               MOVE 'SEQREADFAIL' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           MOVE 'Y' TO WS-SEQ-LOCKED.

           IF WS-SEQ-LENGTH NOT = 32
               MOVE 'SEQBADLENGTH' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF SEQUENCE-NAME NOT = SEQUENCE-KEY
               MOVE 'SEQBADNAME' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF SEQUENCE-NUMBER IS NOT NUMERIC
               MOVE 'SEQBADNUMBER' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF SEQUENCE-NUMBER = 999999999999
               MOVE 'SEQEXHAUSTED' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           MOVE 'C' TO GENERATED-PREFIX.
           MOVE SEQUENCE-NUMBER TO GENERATED-NUMBER.

      * RESERVE THE NUMBER BEFORE WRITING THE CUSTOMER.
      * FAILED CUSTOMER WRITES MAY LEAVE A SEQUENCE GAP.
           ADD 1 TO SEQUENCE-NUMBER.
           MOVE 32 TO WS-SEQ-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS REWRITE
               FILE('SEQFILE')
               FROM(SEQUENCE-RECORD)
               LENGTH(WS-SEQ-LENGTH)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP NOT = ZERO
               MOVE 'SEQREWRITEFAIL' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           MOVE 'N' TO WS-SEQ-LOCKED.
           MOVE GENERATED-CUSTOMER-ID TO RH-CUSTOMER-ID.

           MOVE 'A' TO CUSTOMER-STATUS.
           MOVE GENERATED-CUSTOMER-ID TO CUSTOMER-ID.
           MOVE REQUEST-COUNTRY TO CUSTOMER-COUNTRY.
           MOVE REQUEST-NATIONAL-ID TO CUSTOMER-NATIONAL-ID.
           MOVE REQUEST-FIRST-NAME TO CUSTOMER-FIRST-NAME.
           MOVE REQUEST-LAST-NAME TO CUSTOMER-LAST-NAME.
           MOVE REQUEST-DATE-BIRTH TO CUSTOMER-DATE-BIRTH.
           MOVE REQUEST-CREATED-AT TO CUSTOMER-CREATED-AT.

           MOVE 119 TO WS-CUSTOMER-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS WRITE
               FILE('CUSTFILE')
               FROM(CUSTOMER-RECORD)
               RIDFLD(CUSTOMER-ID)
               KEYLENGTH(WS-CUST-KEY-LENGTH)
               LENGTH(WS-CUSTOMER-LENGTH)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP = DFHRESP(DUPREC)
               MOVE 'DUPCUSTOMERID' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

      * DUPKEY MAY ALSO COME FROM THE UNIQUE ALTERNATE INDEX.
           IF WS-RESP = DFHRESP(DUPKEY)
               MOVE 'DUPKEY' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF WS-RESP NOT = ZERO
               MOVE WS-RESP TO WS-RESP-DISPLAY
               MOVE WS-RESP2 TO WS-RESP2-DISPLAY
               MOVE WS-WRITE-ERROR TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           PERFORM PREPARE-DATA
               THRU PREPARE-DATA-EXIT.

           MOVE 1 TO CA-DATA-COUNT.
           MOVE 'S' TO RH-TYPE.
           MOVE CUSTOMER-STATUS TO RH-STATUS.
           MOVE 'OK' TO RH-ERROR-CODE.

      * WRITE D AND KEEP ITS SYSOUT OPEN.
           PERFORM WRITE-DATA-RESULT
               THRU WRITE-DATA-RESULT-EXIT.

           IF MBR-CALL-RETURN-CODE NOT = 'OK'
               MOVE ZERO TO CA-DATA-COUNT
               MOVE SPACES TO CA-DATA-RECORD
               MOVE 'E' TO RH-TYPE
               MOVE MBR-CALL-RESP TO WS-SPOOL-RESP-OUT
               MOVE MBR-CALL-RESP2 TO WS-SPOOL-RESP2-OUT
               MOVE WS-SPOOL-DIAGNOSTIC TO RH-ERROR-CODE
               GO TO RETURN-PROGRAM.

       FINISH-PROGRAM.

           IF WS-SEQ-LOCKED = 'Y'
               PERFORM RELEASE-SEQUENCE
                   THRU RELEASE-SEQ-EXIT.

      * WRITE FINAL S OR E AND CLOSE THE SYSOUT.
           PERFORM PREPARE-MBR-HEADER
               THRU PREPARE-MBR-HEADER-EXIT.
           MOVE 'F' TO MBR-CALL-CONTROL.
           PERFORM CALL-MBRESULT
               THRU CALL-MBRESULT-EXIT.

           IF MBR-CALL-RETURN-CODE NOT = 'OK'
               MOVE 'E' TO RH-TYPE
               MOVE MBR-CALL-RESP TO WS-SPOOL-RESP-OUT
               MOVE MBR-CALL-RESP2 TO WS-SPOOL-RESP2-OUT
               MOVE WS-SPOOL-DIAGNOSTIC TO RH-ERROR-CODE.

       RETURN-PROGRAM.

           EXEC CICS RETURN END-EXEC.

       RELEASE-SEQUENCE.

           MOVE ZERO TO WS-UNLOCK-RESP.

           EXEC CICS UNLOCK
               FILE('SEQFILE')
               RESP(WS-UNLOCK-RESP)
           END-EXEC.

           MOVE 'N' TO WS-SEQ-LOCKED.

           IF WS-UNLOCK-RESP NOT = ZERO
               MOVE 'SEQUNLOCKFAIL' TO RH-ERROR-CODE.

       RELEASE-SEQ-EXIT.

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
           MOVE 'ADDCUST' TO RH-OPERATION.
           MOVE CA-REQUEST-ID TO RH-REQUEST-ID.
           MOVE 'INTERNAL' TO RH-ERROR-CODE.

       PREPARE-HEADER-EXIT.

           EXIT.

       PREPARE-DATA.

           MOVE 'MBR' TO RD-PREFIX.
           MOVE ';' TO RD-SEP-0
                       RD-SEP-1
                       RD-SEP-2
                       RD-SEP-3.
           MOVE 'D' TO RD-TYPE.
           MOVE 'CUSTOMER' TO RD-ENTITY.
           MOVE CA-REQUEST-ID TO RD-REQUEST-ID.
           MOVE CUSTOMER-RECORD TO RD-PAYLOAD.

       PREPARE-DATA-EXIT.

           EXIT.

       WRITE-DATA-RESULT.

           PERFORM PREPARE-MBR-DATA
               THRU PREPARE-MBR-DATA-EXIT.
           MOVE 'M' TO MBR-CALL-CONTROL.
           PERFORM CALL-MBRESULT
               THRU CALL-MBRESULT-EXIT.

       WRITE-DATA-RESULT-EXIT.

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

