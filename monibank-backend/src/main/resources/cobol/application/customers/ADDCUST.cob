       IDENTIFICATION DIVISION.
       PROGRAM-ID. ADDCUST.

       ENVIRONMENT DIVISION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

       COPY MBACMSD.

      *-----------------------------------------------------------*
      * CICS RESPONSE CODES                                       *
      *-----------------------------------------------------------*

       01  CICS-RESP-NORMAL        PIC S9(4) COMP VALUE +0.
       01  CICS-RESP-DUPREC        PIC S9(4) COMP VALUE +14.
       01  CICS-RESP-DUPKEY        PIC S9(4) COMP VALUE +15.

       01  WS-RESP                 PIC S9(4) COMP VALUE +0.
       01  WS-RESP2                PIC S9(4) COMP VALUE +0.
       01  WS-SEQ-LENGTH           PIC S9(4) COMP VALUE +32.
       01  WS-CUSTOMER-LENGTH      PIC S9(4) COMP VALUE +119.

       01  WS-WRITE-ERROR.
           05 FILLER               PIC X(2) VALUE 'R='.
           05 WS-RESP-DISPLAY      PIC 9(4).
           05 FILLER               PIC X(4) VALUE ';R2='.
           05 WS-RESP2-DISPLAY     PIC 9(4).
           05 FILLER               PIC X(6) VALUE SPACES.

      *-----------------------------------------------------------*
      * CREATE CUSTOMER REQUEST - 113 BYTES                       *
      *-----------------------------------------------------------*

       01  REQUEST-RECORD.
           05 REQUEST-ID           PIC X(8).
           05 REQUEST-COUNTRY      PIC X(2).
           05 REQUEST-NATIONAL-ID  PIC X(11).
           05 REQUEST-FIRST-NAME   PIC X(30).
           05 REQUEST-LAST-NAME    PIC X(40).
           05 REQUEST-DATE-BIRTH   PIC X(8).
           05 REQUEST-CREATED-AT   PIC X(14).

      *-----------------------------------------------------------*
      * MBANK.SEQ RECORD - 32 BYTES                               *
      *-----------------------------------------------------------*

       01  SEQUENCE-KEY            PIC X(12)
                                   VALUE 'CUSTOMER    '.

       01  SEQUENCE-RECORD.
           05 SEQUENCE-NAME        PIC X(12).
           05 SEQUENCE-NUMBER      PIC 9(12).
           05 FILLER               PIC X(8).

       01  GENERATED-CUSTOMER-ID.
           05 GENERATED-PREFIX     PIC X(1).
           05 GENERATED-NUMBER     PIC 9(12).

      *-----------------------------------------------------------*
      * MBANK.CUST RECORD - 119 BYTES                             *
      *-----------------------------------------------------------*

       01  CUSTOMER-RECORD.
           05 CUSTOMER-STATUS      PIC X(1).
           05 CUSTOMER-ID          PIC X(13).
           05 CUSTOMER-COUNTRY     PIC X(2).
           05 CUSTOMER-NATIONAL-ID PIC X(11).
           05 CUSTOMER-FIRST-NAME  PIC X(30).
           05 CUSTOMER-LAST-NAME   PIC X(40).
           05 CUSTOMER-DATE-BIRTH  PIC X(8).
           05 CUSTOMER-CREATED-AT  PIC X(14).

      *-----------------------------------------------------------*
      * MONIBANK RESPONSE - 160 BYTES                             *
      *-----------------------------------------------------------*

       01  RESULT-RECORD.
           05 RR-PREFIX            PIC X(3).
           05 RR-SEP-0             PIC X(1).
           05 RR-TYPE              PIC X(1).
           05 RR-SEP-1             PIC X(1).
           05 RR-OPERATION         PIC X(8).
           05 RR-SEP-2             PIC X(1).
           05 RR-REQUEST-ID        PIC X(8).
           05 RR-SEP-3             PIC X(1).
           05 RR-ENTITY            PIC X(8).
           05 RR-SEP-4             PIC X(1).
           05 RR-ENTITY-ID         PIC X(13).
           05 RR-SEP-5             PIC X(1).
           05 RR-STATUS            PIC X(1).
           05 RR-SEP-6             PIC X(1).
           05 RR-ERROR-CODE        PIC X(20).
           05 FILLER               PIC X(91).

       PROCEDURE DIVISION.

       MAIN-PROCESS.

           PERFORM PREPARE-RESULT.

           MOVE LOW-VALUES TO MBACMAPO.

           EXEC CICS
               SEND MAP('MBACMAP')
               MAPSET('MBACMSD')
               FROM(MBACMAPO)
               ERASE
               RESP(WS-RESP)
           END-EXEC.

           IF WS-RESP NOT = CICS-RESP-NORMAL
               GO TO SEND-MAP-ERROR.

           MOVE SPACES TO REQIDI.
           MOVE SPACES TO CNTRYI.
           MOVE SPACES TO NATIDI.
           MOVE SPACES TO FNAMEI.
           MOVE SPACES TO LNAMEI.
           MOVE SPACES TO DOBI.
           MOVE SPACES TO CRTDATI.

           EXEC CICS
               RECEIVE MAP('MBACMAP')
               MAPSET('MBACMSD')
               INTO(MBACMAPI)
               RESP(WS-RESP)
           END-EXEC.

           IF WS-RESP NOT = CICS-RESP-NORMAL
               GO TO RECEIVE-ERROR.

           IF REQIDL NOT = 8
               MOVE 'BADREQID' TO RR-ERROR-CODE
               GO TO INVALID-INPUT.

           IF CNTRYL NOT = 2
               MOVE 'BADCOUNTRY' TO RR-ERROR-CODE
               GO TO INVALID-INPUT.

           IF NATIDL NOT = 11
               MOVE 'BADNATIONALID' TO RR-ERROR-CODE
               GO TO INVALID-INPUT.

           IF FNAMEL LESS THAN 1
               MOVE 'BADFIRSTNAME' TO RR-ERROR-CODE
               GO TO INVALID-INPUT.

           IF LNAMEL LESS THAN 1
               MOVE 'BADLASTNAME' TO RR-ERROR-CODE
               GO TO INVALID-INPUT.

           IF DOBL NOT = 8
               MOVE 'BADDOB' TO RR-ERROR-CODE
               GO TO INVALID-INPUT.

           IF CRTDATL NOT = 14
               MOVE 'BADCREATEDAT' TO RR-ERROR-CODE
               GO TO INVALID-INPUT.

           MOVE SPACES TO REQUEST-RECORD.
           MOVE REQIDI TO REQUEST-ID.
           MOVE CNTRYI TO REQUEST-COUNTRY.
           MOVE NATIDI TO REQUEST-NATIONAL-ID.
           MOVE FNAMEI TO REQUEST-FIRST-NAME.
           MOVE LNAMEI TO REQUEST-LAST-NAME.
           MOVE DOBI TO REQUEST-DATE-BIRTH.
           MOVE CRTDATI TO REQUEST-CREATED-AT.

       PROCESS-REQUEST.

           MOVE REQUEST-ID TO RR-REQUEST-ID.

      *-----------------------------------------------------------*
      * THE UNIQUE AIX IS THE AUTHORITY FOR COUNTRY + NATIONAL ID. *
      * THE WRITE ATOMICALLY ACCEPTS OR REJECTS THE CUSTOMER.      *
      *-----------------------------------------------------------*

       READ-CUSTOMER-SEQUENCE.

           MOVE 32 TO WS-SEQ-LENGTH.

           EXEC CICS
               READ DATASET('SEQFILE')
               INTO(SEQUENCE-RECORD)
               RIDFLD(SEQUENCE-KEY)
               LENGTH(WS-SEQ-LENGTH)
               UPDATE
               RESP(WS-RESP)
           END-EXEC.

           IF WS-RESP NOT = CICS-RESP-NORMAL
               GO TO SEQUENCE-READ-ERROR.

      *-----------------------------------------------------------*
      * USE CURRENT SEQUENCE VALUE FOR CUSTOMER ID.                *
      *-----------------------------------------------------------*

           MOVE 'C' TO GENERATED-PREFIX.
           MOVE SEQUENCE-NUMBER TO GENERATED-NUMBER.

      *-----------------------------------------------------------*
      * ADVANCE SEQUENCE FOR THE NEXT CUSTOMER.                    *
      *-----------------------------------------------------------*

           ADD 1 TO SEQUENCE-NUMBER.
           MOVE 32 TO WS-SEQ-LENGTH.

           EXEC CICS
               REWRITE DATASET('SEQFILE')
               FROM(SEQUENCE-RECORD)
               LENGTH(WS-SEQ-LENGTH)
               RESP(WS-RESP)
           END-EXEC.

           IF WS-RESP NOT = CICS-RESP-NORMAL
               GO TO SEQUENCE-WRITE-ERROR.

      *-----------------------------------------------------------*
      * BUILD CUSTOMER RECORD                                     *
      *-----------------------------------------------------------*

           MOVE SPACES TO CUSTOMER-RECORD.

           MOVE 'A'
               TO CUSTOMER-STATUS.

           MOVE GENERATED-CUSTOMER-ID
               TO CUSTOMER-ID.

           MOVE REQUEST-COUNTRY
               TO CUSTOMER-COUNTRY.

           MOVE REQUEST-NATIONAL-ID
               TO CUSTOMER-NATIONAL-ID.

           MOVE REQUEST-FIRST-NAME
               TO CUSTOMER-FIRST-NAME.

           MOVE REQUEST-LAST-NAME
               TO CUSTOMER-LAST-NAME.

           MOVE REQUEST-DATE-BIRTH
               TO CUSTOMER-DATE-BIRTH.

           MOVE REQUEST-CREATED-AT
               TO CUSTOMER-CREATED-AT.

      *-----------------------------------------------------------*
      * WRITE BASE RECORD.                                        *
      * UNIQUE AIX WITH UPGRADE IS UPDATED AUTOMATICALLY.          *
      *-----------------------------------------------------------*

           MOVE 119 TO WS-CUSTOMER-LENGTH.
           MOVE 0 TO WS-RESP.
           MOVE 0 TO WS-RESP2.

           EXEC CICS
               WRITE DATASET('CUSTFILE')
               FROM(CUSTOMER-RECORD)
               RIDFLD(CUSTOMER-ID)
               LENGTH(WS-CUSTOMER-LENGTH)
               RESP(WS-RESP)
               RESP2(WS-RESP2)
           END-EXEC.

           IF WS-RESP = CICS-RESP-DUPREC
               GO TO DUPLICATE-CUSTOMER-ID.

           IF WS-RESP = CICS-RESP-DUPKEY
               GO TO DUPLICATE-NATIONAL-WRITE.

           IF WS-RESP NOT = CICS-RESP-NORMAL
               GO TO CUSTOMER-WRITE-ERROR.

      *-----------------------------------------------------------*
      * SUCCESS                                                   *
      *-----------------------------------------------------------*

           MOVE 'S' TO RR-TYPE.
           MOVE CUSTOMER-ID TO RR-ENTITY-ID.
           MOVE CUSTOMER-STATUS TO RR-STATUS.
           MOVE 'OK' TO RR-ERROR-CODE.

           GO TO SEND-RESULT.

       RECEIVE-ERROR.

           MOVE 'E' TO RR-TYPE.
           MOVE 'I' TO RR-STATUS.
           MOVE 'RECEIVEFAIL' TO RR-ERROR-CODE.
           GO TO SEND-RESULT.

       SEND-MAP-ERROR.

           MOVE 'E' TO RR-TYPE.
           MOVE 'I' TO RR-STATUS.
           MOVE 'MAPSENDFAIL' TO RR-ERROR-CODE.
           GO TO SEND-RESULT.

       INVALID-INPUT.

           MOVE 'E' TO RR-TYPE.
           MOVE 'I' TO RR-STATUS.
           GO TO SEND-RESULT.

       SEQUENCE-READ-ERROR.

           MOVE 'E' TO RR-TYPE.
           MOVE 'I' TO RR-STATUS.
           MOVE 'SEQREADFAIL' TO RR-ERROR-CODE.
           GO TO SEND-RESULT.

       SEQUENCE-WRITE-ERROR.

           MOVE 'E' TO RR-TYPE.
           MOVE 'I' TO RR-STATUS.
           MOVE 'SEQREWRITEFAIL' TO RR-ERROR-CODE.
           GO TO SEND-RESULT.

       DUPLICATE-CUSTOMER-ID.

           MOVE 'E' TO RR-TYPE.
           MOVE GENERATED-CUSTOMER-ID TO RR-ENTITY-ID.
           MOVE 'I' TO RR-STATUS.
           MOVE 'DUPCUSTOMERID' TO RR-ERROR-CODE.
           GO TO SEND-RESULT.

       DUPLICATE-NATIONAL-WRITE.

           MOVE 'E' TO RR-TYPE.
           MOVE GENERATED-CUSTOMER-ID TO RR-ENTITY-ID.
           MOVE 'I' TO RR-STATUS.
           MOVE 'DUPNATIONALID' TO RR-ERROR-CODE.
           GO TO SEND-RESULT.

       CUSTOMER-WRITE-ERROR.

           MOVE 'E' TO RR-TYPE.
           MOVE GENERATED-CUSTOMER-ID TO RR-ENTITY-ID.
           MOVE 'I' TO RR-STATUS.
           MOVE WS-RESP TO WS-RESP-DISPLAY.
           MOVE WS-RESP2 TO WS-RESP2-DISPLAY.
           MOVE WS-WRITE-ERROR TO RR-ERROR-CODE.
           GO TO SEND-RESULT.

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

       PREPARE-RESULT.

           MOVE SPACES TO RESULT-RECORD.

           MOVE 'MBR' TO RR-PREFIX.
           MOVE ';' TO RR-SEP-0.
           MOVE ';' TO RR-SEP-1.
           MOVE ';' TO RR-SEP-2.
           MOVE ';' TO RR-SEP-3.
           MOVE ';' TO RR-SEP-4.
           MOVE ';' TO RR-SEP-5.
           MOVE ';' TO RR-SEP-6.

           MOVE 'ADDCUST' TO RR-OPERATION.
           MOVE '00000000' TO RR-REQUEST-ID.
           MOVE 'CUSTOMER' TO RR-ENTITY.