       IDENTIFICATION DIVISION.
       PROGRAM-ID. ADDACCT.

       ENVIRONMENT DIVISION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

       01  WS-RESP                 PIC S9(8) COMP.
       01  WS-RESP2                PIC S9(8) COMP.
       01  WS-UNLOCK-RESP          PIC S9(8) COMP.
       01  WS-SEQ-LENGTH           PIC S9(4) COMP.
       01  WS-CUST-LENGTH          PIC S9(4) COMP.
       01  WS-ACCOUNT-LENGTH       PIC S9(4) COMP.
       01  WS-SEQ-KEY-LENGTH       PIC S9(4) COMP.
       01  WS-CUST-KEY-LENGTH      PIC S9(4) COMP.
       01  WS-ACCT-KEY-LENGTH      PIC S9(4) COMP.
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

      * INPUT: CUSTOMER X(13), TYPE X(2), LIMIT 9(13)V99,
      * CREATED-AT X(14). TOTAL: 44 BYTES.
       01  REQUEST-RECORD.
           05 REQUEST-CUSTOMER-ID.
              10 REQUEST-CUST-PREFIX PIC X.
              10 REQUEST-CUST-NUMBER PIC X(12).
           05 REQUEST-ACCOUNT-TYPE   PIC X(2).
           05 REQUEST-OVERDRAFT      PIC 9(13)V99.
           05 REQUEST-CREATED-AT     PIC X(14).

       01  SEQUENCE-KEY              PIC X(12)
                                     VALUE 'ACCOUNT     '.

       01  SEQUENCE-RECORD.
           05 SEQUENCE-NAME          PIC X(12).
           05 SEQUENCE-NUMBER        PIC 9(12).
           05 FILLER                 PIC X(8).

       01  GENERATED-ACCOUNT-ID.
           05 GENERATED-ACCT-PREFIX  PIC X.
           05 GENERATED-ACCT-NUMBER  PIC 9(12).

      * FICTIONAL PL BBAN: BANK 99999999 + 0000 + SEQUENCE.
       01  WS-BBAN.
           05 WS-BBAN-BANK           PIC X(8).
           05 WS-BBAN-ZEROES         PIC X(4).
           05 WS-BBAN-NUMBER         PIC 9(12).

      * FOR IBAN MOD-97: BBAN + PL(25,21) + CHECK DIGITS 00.
       01  WS-IBAN-CALCULATION.
           05 WS-IBAN-CALC-BBAN      PIC X(24).
           05 FILLER                 PIC X(6) VALUE '252100'.

       01  WS-IBAN-CALC-REDEF
           REDEFINES WS-IBAN-CALCULATION.
           05 WS-IBAN-DIGIT          PIC 9 OCCURS 30 TIMES.

       01  WS-IBAN-WORK.
           05 WS-IBAN-IDX            PIC S9(4) COMP.
           05 WS-IBAN-DIGIT-NUM      PIC 9.
           05 WS-IBAN-MOD            PIC 9(4) COMP.
           05 WS-IBAN-WORK-NUM       PIC 9(4) COMP.
           05 WS-IBAN-QUOTIENT       PIC 9(4) COMP.
           05 WS-IBAN-CHECK-NUM      PIC 99.

       01  GENERATED-IBAN.
           05 GENERATED-IBAN-COUNTRY PIC X(2).
           05 GENERATED-IBAN-CHECK   PIC 99.
           05 GENERATED-IBAN-BBAN    PIC X(24).
           05 FILLER                 PIC X(6).

       01  CUSTOMER-RECORD.
           05 CUSTOMER-STATUS        PIC X.
           05 CUSTOMER-ID            PIC X(13).
           05 FILLER                 PIC X(105).

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

      * PRINTABLE API PAYLOAD - EXACTLY 119 CHARACTERS.
       01  ACCOUNT-OUTPUT.
           05 AO-STATUS              PIC X.
           05 AO-ID                  PIC X(13).
           05 AO-CUSTOMER-ID         PIC X(13).
           05 AO-IBAN                PIC X(34).
           05 AO-TYPE                PIC X(2).
           05 AO-CURRENCY            PIC X(3).
           05 AO-BALANCE             PIC +9(13).99.
           05 AO-OVERDRAFT           PIC +9(13).99.
           05 AO-BLOCKED             PIC +9(13).99.
           05 FILLER                 PIC X(2).

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
                          CUSTOMER-RECORD
                          ACCOUNT-RECORD
                          ACCOUNT-OUTPUT
                          SEQUENCE-RECORD
                          GENERATED-ACCOUNT-ID
                          GENERATED-IBAN.
           MOVE ZERO TO WS-RESP
                        WS-RESP2
                        WS-UNLOCK-RESP
                        WS-LINK-RESP
                        WS-LINK-RESP2.
           MOVE 12 TO WS-SEQ-KEY-LENGTH.
           MOVE 13 TO WS-CUST-KEY-LENGTH
                      WS-ACCT-KEY-LENGTH.

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

           IF CA-OPERATION NOT = 'ADDACCT'
               MOVE 'BADOPERATION' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-REQUEST-ID = SPACES
               MOVE 'BADREQUEST' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CA-INPUT-LENGTH NOT = '0044'
               MOVE 'BADLENGTH' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           MOVE CA-INPUT TO REQUEST-RECORD.

           IF REQUEST-CUST-PREFIX NOT = 'C'
               MOVE 'BADCUSTOMERID' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-CUST-NUMBER IS NOT NUMERIC
               MOVE 'BADCUSTOMERID' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-ACCOUNT-TYPE NOT = 'ST'
               IF REQUEST-ACCOUNT-TYPE NOT = 'OD'
                   MOVE 'BADTYPE' TO RH-ERROR-CODE
                   GO TO FINISH-PROGRAM.

           IF REQUEST-OVERDRAFT IS NOT NUMERIC
               MOVE 'BADLIMIT' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF REQUEST-ACCOUNT-TYPE = 'ST'
               IF REQUEST-OVERDRAFT NOT = ZERO
                   MOVE 'STLIMITNOTZERO' TO RH-ERROR-CODE
                   GO TO FINISH-PROGRAM.

           IF REQUEST-CREATED-AT IS NOT NUMERIC
               MOVE 'BADCREATEDAT' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

      * VERIFY THAT THE OWNER EXISTS AND IS ACTIVE.
           MOVE 119 TO WS-CUST-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS READ
               FILE('CUSTFILE')
               INTO(CUSTOMER-RECORD)
               RIDFLD(REQUEST-CUSTOMER-ID)
               KEYLENGTH(WS-CUST-KEY-LENGTH)
               LENGTH(WS-CUST-LENGTH)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP = DFHRESP(NOTFND)
               MOVE 'CUSTOMERNOTFOUND' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF WS-RESP NOT = ZERO
               MOVE 'CUSTOMERREADFAIL' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF WS-CUST-LENGTH NOT = 119
               MOVE 'CUSTOMERBADLENGTH' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CUSTOMER-ID NOT = REQUEST-CUSTOMER-ID
               MOVE 'CUSTOMERKEYERROR' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF CUSTOMER-STATUS NOT = 'A'
               MOVE 'CUSTOMERINACTIVE' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

      * LOCK AND RESERVE THE NEXT ACCOUNT SEQUENCE NUMBER.
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

           MOVE 'A' TO GENERATED-ACCT-PREFIX.
           MOVE SEQUENCE-NUMBER TO GENERATED-ACCT-NUMBER.

           PERFORM GENERATE-IBAN
               THRU GENERATE-IBAN-EXIT.

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

           MOVE 'A' TO ACCOUNT-STATUS.
           MOVE GENERATED-ACCOUNT-ID TO ACCOUNT-ID
                                           RH-CUSTOMER-ID.
           MOVE REQUEST-CUSTOMER-ID TO ACCOUNT-CUSTOMER-ID.
           MOVE GENERATED-IBAN TO ACCOUNT-IBAN.
           MOVE REQUEST-ACCOUNT-TYPE TO ACCOUNT-TYPE.
           MOVE 'EUR' TO ACCOUNT-CURRENCY.
           MOVE ZERO TO ACCOUNT-BALANCE
                        ACCOUNT-BLOCKED.
           MOVE REQUEST-OVERDRAFT TO ACCOUNT-OVERDRAFT.
           MOVE REQUEST-CREATED-AT TO ACCOUNT-CREATED-AT
                                        ACCOUNT-UPDATED-AT.

           MOVE 119 TO WS-ACCOUNT-LENGTH.
           MOVE ZERO TO WS-RESP WS-RESP2.

           EXEC CICS WRITE
               FILE('ACCTFILE')
               FROM(ACCOUNT-RECORD)
               RIDFLD(ACCOUNT-ID)
               KEYLENGTH(WS-ACCT-KEY-LENGTH)
               LENGTH(WS-ACCOUNT-LENGTH)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP = DFHRESP(DUPREC)
               MOVE 'DUPACCOUNTID' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

      * DUPKEY CAN ALSO COME FROM THE UNIQUE IBAN AIX.
           IF WS-RESP = DFHRESP(DUPKEY)
               MOVE 'DUPIBAN' TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           IF WS-RESP NOT = ZERO
               MOVE WS-RESP TO WS-RESP-DISPLAY
               MOVE WS-RESP2 TO WS-RESP2-DISPLAY
               MOVE WS-WRITE-ERROR TO RH-ERROR-CODE
               GO TO FINISH-PROGRAM.

           PERFORM PREPARE-ACCOUNT-OUTPUT
               THRU PREPARE-ACCOUNT-OUTPUT-EXIT.
           PERFORM PREPARE-DATA
               THRU PREPARE-DATA-EXIT.

           MOVE 1 TO CA-DATA-COUNT.
           MOVE 'S' TO RH-TYPE.
           MOVE ACCOUNT-STATUS TO RH-STATUS.
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

       GENERATE-IBAN.

           MOVE '99999999' TO WS-BBAN-BANK.
           MOVE '0000' TO WS-BBAN-ZEROES.
           MOVE SEQUENCE-NUMBER TO WS-BBAN-NUMBER.
           MOVE WS-BBAN TO WS-IBAN-CALC-BBAN.
           MOVE ZERO TO WS-IBAN-MOD.

           PERFORM CALCULATE-IBAN-DIGIT
               THRU CALCULATE-IBAN-DIGIT-EXIT
               VARYING WS-IBAN-IDX FROM 1 BY 1
               UNTIL WS-IBAN-IDX > 30.

           COMPUTE WS-IBAN-CHECK-NUM = 98 - WS-IBAN-MOD.
           MOVE 'PL' TO GENERATED-IBAN-COUNTRY.
           MOVE WS-IBAN-CHECK-NUM TO GENERATED-IBAN-CHECK.
           MOVE WS-BBAN TO GENERATED-IBAN-BBAN.

       GENERATE-IBAN-EXIT.

           EXIT.

       CALCULATE-IBAN-DIGIT.

           MOVE WS-IBAN-DIGIT(WS-IBAN-IDX)
               TO WS-IBAN-DIGIT-NUM.
           MULTIPLY 10 BY WS-IBAN-MOD.
           ADD WS-IBAN-DIGIT-NUM TO WS-IBAN-MOD.
           MOVE WS-IBAN-MOD TO WS-IBAN-WORK-NUM.
           DIVIDE 97 INTO WS-IBAN-WORK-NUM
               GIVING WS-IBAN-QUOTIENT
               REMAINDER WS-IBAN-MOD.

       CALCULATE-IBAN-DIGIT-EXIT.

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

       PREPARE-ACCOUNT-OUTPUT.

           MOVE SPACES TO ACCOUNT-OUTPUT.
           MOVE ACCOUNT-STATUS TO AO-STATUS.
           MOVE ACCOUNT-ID TO AO-ID.
           MOVE ACCOUNT-CUSTOMER-ID TO AO-CUSTOMER-ID.
           MOVE ACCOUNT-IBAN TO AO-IBAN.
           MOVE ACCOUNT-TYPE TO AO-TYPE.
           MOVE ACCOUNT-CURRENCY TO AO-CURRENCY.
           MOVE ACCOUNT-BALANCE TO AO-BALANCE.
           MOVE ACCOUNT-OVERDRAFT TO AO-OVERDRAFT.
           MOVE ACCOUNT-BLOCKED TO AO-BLOCKED.

       PREPARE-ACCOUNT-OUTPUT-EXIT.

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
           MOVE 'ADDACCT' TO RH-OPERATION.
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
           MOVE 'ACCOUNT' TO RD-ENTITY.
           MOVE CA-REQUEST-ID TO RD-REQUEST-ID.
           MOVE ACCOUNT-OUTPUT TO RD-PAYLOAD.

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
           MOVE 'ACCOUNT' TO MBR-D-ENTITY.
           MOVE CA-REQUEST-ID TO MBR-D-REQUEST-ID.
           MOVE ACCOUNT-OUTPUT TO MBR-D-PAYLOAD.

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
