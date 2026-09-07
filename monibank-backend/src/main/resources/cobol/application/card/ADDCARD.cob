       IDENTIFICATION DIVISION.
       PROGRAM-ID. ADDCARD.

       ENVIRONMENT DIVISION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

       01  WS-RESP                 PIC S9(8) COMP.
       01  WS-RESP2                PIC S9(8) COMP.
       01  WS-UNLOCK-RESP          PIC S9(8) COMP.
       01  WS-SEQ-LENGTH           PIC S9(4) COMP.
       01  WS-ACCOUNT-LENGTH       PIC S9(4) COMP.
       01  WS-CARD-LENGTH          PIC S9(4) COMP.
       01  WS-SEQ-KEY-LENGTH       PIC S9(4) COMP.
       01  WS-ACCT-KEY-LENGTH      PIC S9(4) COMP.
       01  WS-CARD-KEY-LENGTH      PIC S9(4) COMP.
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

      * INPUT: ACCOUNT X(13), LIMIT 9(13)V99, EXPIRY X(6),
      * CREATED-AT X(14). TOTAL: 48 BYTES.
       01  REQUEST-RECORD.
           05 REQUEST-ACCOUNT-ID.
              10 REQUEST-ACCT-PREFIX PIC X.
              10 REQUEST-ACCT-NUMBER PIC X(12).
           05 REQUEST-DAILY-LIMIT    PIC 9(13)V99.
           05 REQUEST-EXPIRY         PIC X(6).
           05 REQUEST-CREATED-AT.
              10 REQUEST-CREATED-DATE PIC X(8).
              10 REQUEST-CREATED-TIME PIC X(6).

       01  SEQUENCE-KEY              PIC X(12)
                                     VALUE 'CARD        '.

       01  SEQUENCE-RECORD.
           05 SEQUENCE-NAME          PIC X(12).
           05 SEQUENCE-NUMBER        PIC 9(12).
           05 FILLER                 PIC X(8).

       01  GENERATED-CARD-ID.
           05 GENERATED-CARD-PREFIX  PIC X.
           05 GENERATED-CARD-ID-NUM  PIC 9(12).

      * EDUCATIONAL VISA-LIKE NUMBER: 400 + SEQUENCE + LUHN.
       01  WS-CARD-BASE.
           05 WS-CARD-BASE-PREFIX    PIC X(3).
           05 WS-CARD-BASE-NUMBER    PIC 9(12).

       01  WS-CARD-BASE-REDEF
           REDEFINES WS-CARD-BASE.
           05 WS-CARD-BASE-DIGIT     PIC 9 OCCURS 15 TIMES.

       01  GENERATED-CARD-NUMBER.
           05 GENERATED-CARD-BASE    PIC X(15).
           05 GENERATED-CHECK-DIGIT  PIC 9.

       01  WS-LUHN-WORK.
           05 WS-LUHN-IDX            PIC S9(4) COMP.
           05 WS-LUHN-POSITION       PIC 9(4) COMP.
           05 WS-LUHN-QUOTIENT       PIC 9(4) COMP.
           05 WS-LUHN-REMAINDER      PIC 99 COMP.
           05 WS-LUHN-DIGIT          PIC 99 COMP.
           05 WS-LUHN-SUM            PIC 9(4) COMP.
           05 WS-LUHN-CHECK          PIC 99 COMP.

      * PHYSICAL MBANK.ACCT RECORD - EXACTLY 119 BYTES.
       01  ACCOUNT-RECORD.
           05 ACCOUNT-STATUS         PIC X.
           05 ACCOUNT-ID             PIC X(13).
           05 ACCOUNT-CUSTOMER-ID    PIC X(13).
           05 ACCOUNT-IBAN           PIC X(34).
           05 ACCOUNT-TYPE           PIC X(2).
           05 ACCOUNT-CURRENCY       PIC X(3).
           05 ACCOUNT-BALANCE        PIC S9(13)V99 COMP-3.
           05 ACCOUNT-OVERDRAFT      PIC S9(13)V99 COMP-3.
           05 ACCOUNT-BLOCKED        PIC S9(13)V99 COMP-3.
           05 ACCOUNT-CREATED-AT     PIC X(14).
           05 ACCOUNT-UPDATED-AT     PIC X(14).
           05 FILLER                 PIC X.

      * PHYSICAL MBANK.CARD RECORD - EXACTLY 119 BYTES.
       01  CARD-RECORD.
           05 CARD-STATUS            PIC X.
           05 CARD-ID                PIC X(13).
           05 CARD-ACCOUNT-ID        PIC X(13).
           05 CARD-CUSTOMER-ID       PIC X(13).
           05 CARD-NUMBER            PIC X(16).
           05 CARD-TYPE              PIC X.
           05 CARD-NETWORK           PIC X(2).
           05 CARD-EXPIRY            PIC X(6).
           05 CARD-DAILY-LIMIT       PIC S9(13)V99 COMP-3.
           05 CARD-DAILY-SPENT       PIC S9(13)V99 COMP-3.
           05 CARD-SPENT-DATE        PIC X(8).
           05 CARD-CREATED-AT        PIC X(14).
           05 CARD-UPDATED-AT        PIC X(14).
           05 FILLER                 PIC X(2).

      * PRINTABLE API PAYLOAD - EXACTLY 119 CHARACTERS.
       01  CARD-OUTPUT.
           05 CO-STATUS              PIC X.
           05 CO-ID                  PIC X(13).
           05 CO-ACCOUNT-ID          PIC X(13).
           05 CO-CUSTOMER-ID         PIC X(13).
           05 CO-NUMBER              PIC X(16).
           05 CO-TYPE                PIC X.
           05 CO-NETWORK             PIC X(2).
           05 CO-EXPIRY              PIC X(6).
           05 CO-DAILY-LIMIT         PIC +9(13).99.
           05 CO-DAILY-SPENT         PIC +9(13).99.
           05 CO-SPENT-DATE          PIC X(8).
           05 FILLER                 PIC X(12).

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
                          CA-HEADER-RECORD
                          REQUEST-RECORD
                          ACCOUNT-RECORD
                          CARD-RECORD
                          CARD-OUTPUT
                          SEQUENCE-RECORD
                          GENERATED-CARD-ID
                          GENERATED-CARD-NUMBER.
           MOVE ZERO TO WS-RESP
                        WS-RESP2
                        WS-UNLOCK-RESP
                        WS-LINK-RESP
                        WS-LINK-RESP2.
           MOVE 12 TO WS-SEQ-KEY-LENGTH.
           MOVE 13 TO WS-ACCT-KEY-LENGTH
                      WS-CARD-KEY-LENGTH.

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

           IF CA-OPERATION NOT = 'ADDCARD'
               MOVE 'BADOPERATION' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-REQUEST-ID = SPACES
               MOVE 'BADREQUEST' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-INPUT-LENGTH NOT = '0048'
               MOVE 'BADLENGTH' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           MOVE CA-INPUT TO REQUEST-RECORD.

           IF REQUEST-ACCT-PREFIX NOT = 'A'
               MOVE 'BADACCOUNTID' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-ACCT-NUMBER IS NOT NUMERIC
               MOVE 'BADACCOUNTID' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-DAILY-LIMIT IS NOT NUMERIC
               MOVE 'BADLIMIT' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-DAILY-LIMIT = ZERO
               MOVE 'BADLIMIT' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-EXPIRY IS NOT NUMERIC
               MOVE 'BADEXPIRY' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-CREATED-DATE IS NOT NUMERIC
               MOVE 'BADCREATEDAT' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-CREATED-TIME IS NOT NUMERIC
               MOVE 'BADCREATEDAT' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

      * VERIFY THAT THE LINKED ACCOUNT EXISTS AND IS ACTIVE.
           MOVE 119 TO WS-ACCOUNT-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS READ
               FILE('ACCTFILE')
               INTO(ACCOUNT-RECORD)
               RIDFLD(REQUEST-ACCOUNT-ID)
               KEYLENGTH(WS-ACCT-KEY-LENGTH)
               LENGTH(WS-ACCOUNT-LENGTH)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP = DFHRESP(NOTFND)
               MOVE 'ACCOUNTNOTFOUND' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF WS-RESP NOT = ZERO
               MOVE 'ACCOUNTREADFAIL' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF WS-ACCOUNT-LENGTH NOT = 119
               MOVE 'ACCOUNTBADLENGTH' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF ACCOUNT-ID NOT = REQUEST-ACCOUNT-ID
               MOVE 'ACCOUNTKEYERROR' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF ACCOUNT-STATUS NOT = 'A'
               MOVE 'ACCOUNTINACTIVE' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

      * LOCK AND RESERVE THE NEXT CARD SEQUENCE NUMBER.
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

           MOVE 'K' TO GENERATED-CARD-PREFIX.
           MOVE SEQUENCE-NUMBER TO GENERATED-CARD-ID-NUM.

           PERFORM GENERATE-CARD-NUMBER
               THRU GENERATE-CARD-NUMBER-EXIT.

      * RESERVE BEFORE WRITING; A FAILED WRITE MAY LEAVE A GAP.
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

           MOVE 'A' TO CARD-STATUS.
           MOVE GENERATED-CARD-ID TO CARD-ID
                                        RH-CUSTOMER-ID.
           MOVE ACCOUNT-ID TO CARD-ACCOUNT-ID.
           MOVE ACCOUNT-CUSTOMER-ID TO CARD-CUSTOMER-ID.
           MOVE GENERATED-CARD-NUMBER TO CARD-NUMBER.
           MOVE 'D' TO CARD-TYPE.
           MOVE 'VS' TO CARD-NETWORK.
           MOVE REQUEST-EXPIRY TO CARD-EXPIRY.
           MOVE REQUEST-DAILY-LIMIT TO CARD-DAILY-LIMIT.
           MOVE ZERO TO CARD-DAILY-SPENT.
           MOVE REQUEST-CREATED-DATE TO CARD-SPENT-DATE.
           MOVE REQUEST-CREATED-AT TO CARD-CREATED-AT
                                        CARD-UPDATED-AT.

           MOVE 119 TO WS-CARD-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS WRITE
               FILE('CARDFILE')
               FROM(CARD-RECORD)
               RIDFLD(CARD-ID)
               KEYLENGTH(WS-CARD-KEY-LENGTH)
               LENGTH(WS-CARD-LENGTH)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP = DFHRESP(DUPREC)
               MOVE 'DUPCARDID' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

      * DUPKEY CAN ALSO COME FROM THE UNIQUE CARD NUMBER AIX.
           IF WS-RESP = DFHRESP(DUPKEY)
               MOVE 'DUPCARDNUMBER' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF WS-RESP NOT = ZERO
               MOVE WS-RESP TO WS-RESP-DISPLAY
               MOVE WS-RESP2 TO WS-RESP2-DISPLAY
               MOVE WS-WRITE-ERROR TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           PERFORM PREPARE-CARD-OUTPUT
               THRU PREPARE-CARD-OUTPUT-EXIT.
           PERFORM PREPARE-DATA
               THRU PREPARE-DATA-EXIT.

           MOVE 1 TO CA-DATA-COUNT.
           MOVE 'S' TO RH-TYPE.
           MOVE CARD-STATUS TO RH-STATUS.
           MOVE 'OK' TO RH-ERROR-CODE.

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
                   THRU RELEASE-SEQUENCE-EXIT.

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

       GENERATE-CARD-NUMBER.

           MOVE '400' TO WS-CARD-BASE-PREFIX.
           MOVE SEQUENCE-NUMBER TO WS-CARD-BASE-NUMBER.
           MOVE WS-CARD-BASE TO GENERATED-CARD-BASE.
           MOVE ZERO TO WS-LUHN-SUM.

           PERFORM CALCULATE-LUHN-DIGIT
               THRU CALCULATE-LUHN-DIGIT-EXIT
               VARYING WS-LUHN-IDX FROM 1 BY 1
               UNTIL WS-LUHN-IDX > 15.

           MOVE WS-LUHN-SUM TO WS-LUHN-POSITION.
           DIVIDE 10 INTO WS-LUHN-POSITION
               GIVING WS-LUHN-QUOTIENT
               REMAINDER WS-LUHN-REMAINDER.
           COMPUTE WS-LUHN-CHECK = 10 - WS-LUHN-REMAINDER.

           IF WS-LUHN-CHECK = 10
               MOVE ZERO TO WS-LUHN-CHECK.

           MOVE WS-LUHN-CHECK TO GENERATED-CHECK-DIGIT.

       GENERATE-CARD-NUMBER-EXIT.

           EXIT.

       CALCULATE-LUHN-DIGIT.

           MOVE WS-CARD-BASE-DIGIT(WS-LUHN-IDX)
               TO WS-LUHN-DIGIT.
           MOVE WS-LUHN-IDX TO WS-LUHN-POSITION.
           DIVIDE 2 INTO WS-LUHN-POSITION
               GIVING WS-LUHN-QUOTIENT
               REMAINDER WS-LUHN-REMAINDER.

           IF WS-LUHN-REMAINDER = 1
               MULTIPLY 2 BY WS-LUHN-DIGIT.

           IF WS-LUHN-DIGIT > 9
               SUBTRACT 9 FROM WS-LUHN-DIGIT.

           ADD WS-LUHN-DIGIT TO WS-LUHN-SUM.

       CALCULATE-LUHN-DIGIT-EXIT.

           EXIT.

       RELEASE-SEQUENCE.

           MOVE ZERO TO WS-UNLOCK-RESP.

           EXEC CICS UNLOCK
               FILE('SEQFILE')
               RESP(WS-UNLOCK-RESP)
           END-EXEC.

           MOVE 'N' TO WS-SEQ-LOCKED.

           IF WS-UNLOCK-RESP NOT = ZERO
               MOVE 'SEQUNLOCKFAIL' TO RH-ERROR-CODE.

       RELEASE-SEQUENCE-EXIT.

           EXIT.

       PREPARE-CARD-OUTPUT.

           MOVE SPACES TO CARD-OUTPUT.
           MOVE CARD-STATUS TO CO-STATUS.
           MOVE CARD-ID TO CO-ID.
           MOVE CARD-ACCOUNT-ID TO CO-ACCOUNT-ID.
           MOVE CARD-CUSTOMER-ID TO CO-CUSTOMER-ID.
           MOVE CARD-NUMBER TO CO-NUMBER.
           MOVE CARD-TYPE TO CO-TYPE.
           MOVE CARD-NETWORK TO CO-NETWORK.
           MOVE CARD-EXPIRY TO CO-EXPIRY.
           MOVE CARD-DAILY-LIMIT TO CO-DAILY-LIMIT.
           MOVE CARD-DAILY-SPENT TO CO-DAILY-SPENT.
           MOVE CARD-SPENT-DATE TO CO-SPENT-DATE.

       PREPARE-CARD-OUTPUT-EXIT.

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
           MOVE 'ADDCARD' TO RH-OPERATION.
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
           MOVE 'CARD' TO RD-ENTITY.
           MOVE CA-REQUEST-ID TO RD-REQUEST-ID.
           MOVE CARD-OUTPUT TO RD-PAYLOAD.

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
           MOVE 'CARD' TO MBR-D-ENTITY.
           MOVE CA-REQUEST-ID TO MBR-D-REQUEST-ID.
           MOVE CARD-OUTPUT TO MBR-D-PAYLOAD.

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
