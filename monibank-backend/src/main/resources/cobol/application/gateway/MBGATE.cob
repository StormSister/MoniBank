       IDENTIFICATION DIVISION.
       PROGRAM-ID. MBGATE.

       ENVIRONMENT DIVISION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

       COPY MBGWMSD.
       COPY DFHAID.

       01  WS-RESP              PIC S9(4) COMP.
       01  WS-LINK-RESP         PIC S9(4) COMP.
       01  WS-LINK-RESP2        PIC S9(4) COMP.
       01  WS-CA-LENGTH         PIC S9(4) COMP VALUE +855.
       01  WS-TARGET-PROGRAM    PIC X(8).

       01  WS-MARKER            PIC X VALUE 'Y'.
       01  WS-MARKER-LENGTH     PIC S9(4) COMP VALUE +1.

       01  WS-OPERATION         PIC X(8).
       01  WS-REQUEST-ID        PIC X(8).
       01  WS-INPUT-LENGTH      PIC X(4).
       01  WS-LENGTH-NUM REDEFINES WS-INPUT-LENGTH
                               PIC 9(4).

       01  WS-STATE             PIC X(8).
       01  WS-MESSAGE           PIC X(70).
       01  WS-HAVE-RESULT       PIC X.

       01  WS-INPUT.
           05 WS-IN01           PIC X(64).
           05 WS-IN02           PIC X(64).
           05 WS-IN03           PIC X(64).
           05 WS-IN04           PIC X(64).
           05 WS-IN05           PIC X(64).
           05 WS-IN06           PIC X(64).
           05 WS-IN07           PIC X(64).
           05 WS-IN08           PIC X(64).

       01  WS-INPUT-BYTES REDEFINES WS-INPUT.
           05 WS-INPUT-CHAR     PIC X OCCURS 512 TIMES.

       01  WS-IDX               PIC S9(4) COMP.

       01  WS-CALL-AREA.
           COPY MBGWCA.

       01  WS-DISPLAY-RECORD.
           05 WS-DISPLAY-1      PIC X(64).
           05 WS-DISPLAY-2      PIC X(64).
           05 WS-DISPLAY-3      PIC X(32).

       01  WS-LINK-MESSAGE.
           05 FILLER            PIC X(10) VALUE 'LINK RESP='.
           05 WS-LINK-CODE      PIC +9(5).
           05 FILLER            PIC X(7) VALUE ' RESP2='.
           05 WS-LINK-CODE2     PIC +9(5).

       LINKAGE SECTION.

       01  DFHCOMMAREA          PIC X.

       PROCEDURE DIVISION.

       MAIN-PROCESS.

           MOVE SPACES TO WS-OPERATION
                          WS-TARGET-PROGRAM
                          WS-REQUEST-ID
                          WS-INPUT-LENGTH
                          WS-STATE
                          WS-MESSAGE
                          WS-INPUT.

           MOVE 'N' TO WS-HAVE-RESULT.
           MOVE 'Y' TO WS-MARKER.
           MOVE +1 TO WS-MARKER-LENGTH.
           MOVE +855 TO WS-CA-LENGTH.

           IF EIBCALEN = ZERO
               GO TO NEW-REQUEST.

           IF EIBAID = DFHCLEAR
               GO TO EXIT-PROGRAM.

           IF EIBCALEN NOT = 1
               GO TO NEW-REQUEST.

      * NEVER RESUBMIT A RESULT SCREEN OR A FAILED LINK.
           IF DFHCOMMAREA = 'R'
               GO TO NEW-REQUEST.

           MOVE LOW-VALUES TO MBGWMAPI.

           EXEC CICS RECEIVE
               MAP('MBGWMAP')
               MAPSET('MBGWMSD')
               INTO(MBGWMAPI)
               RESP(WS-RESP)
           END-EXEC.

           IF WS-RESP NOT = ZERO
               MOVE 'ERROR' TO WS-STATE
               MOVE 'MAP INPUT NOT RECEIVED. TRY AGAIN.'
                   TO WS-MESSAGE
               GO TO SHOW-SCREEN.

           IF OPERL > ZERO
               MOVE OPERI TO WS-OPERATION.

           IF REQIDL > ZERO
               MOVE REQIDI TO WS-REQUEST-ID.

           IF INLENL > ZERO
               MOVE INLENI TO WS-INPUT-LENGTH.

           PERFORM COLLECT-INPUT
               THRU COLLECT-INPUT-EXIT.

           PERFORM NORMALIZE-INPUT
               THRU NORMALIZE-INPUT-EXIT
               VARYING WS-IDX FROM 1 BY 1
               UNTIL WS-IDX > 512.

           IF WS-OPERATION = SPACES
               MOVE 'ERROR' TO WS-STATE
               MOVE 'OPERATION IS REQUIRED.' TO WS-MESSAGE
               GO TO SHOW-SCREEN.

           IF REQIDL NOT = 8
               MOVE 'ERROR' TO WS-STATE
               MOVE 'REQUEST ID MUST CONTAIN 8 CHARACTERS.'
                   TO WS-MESSAGE
               GO TO SHOW-SCREEN.

           IF WS-REQUEST-ID = SPACES
               MOVE 'ERROR' TO WS-STATE
               MOVE 'REQUEST ID MUST NOT BE BLANK.'
                   TO WS-MESSAGE
               GO TO SHOW-SCREEN.

           IF INLENL NOT = 4
               MOVE 'ERROR' TO WS-STATE
               MOVE 'INPUT LENGTH MUST BE FOUR DIGITS.'
                   TO WS-MESSAGE
               GO TO SHOW-SCREEN.

           IF WS-INPUT-LENGTH IS NOT NUMERIC
               MOVE 'ERROR' TO WS-STATE
               MOVE 'INPUT LENGTH MUST BE FOUR DIGITS.'
                   TO WS-MESSAGE
               GO TO SHOW-SCREEN.

           IF WS-LENGTH-NUM > 512
               MOVE 'ERROR' TO WS-STATE
               MOVE 'INPUT LENGTH MUST NOT EXCEED 0512.'
                   TO WS-MESSAGE
               GO TO SHOW-SCREEN.

      * ROUTING ONLY. INPUT LAYOUT BELONGS TO EACH PROGRAM.
           IF WS-OPERATION = 'GETCUST'
               MOVE 'GETCUST' TO WS-TARGET-PROGRAM.

           IF WS-OPERATION = 'ADDCUST'
               MOVE 'ADDCUSG' TO WS-TARGET-PROGRAM.

           IF WS-OPERATION = 'LISTCUST'
               MOVE 'LISTCUST' TO WS-TARGET-PROGRAM.

           IF WS-OPERATION = 'CHGCUST'
               MOVE 'CHGCUST' TO WS-TARGET-PROGRAM.

           IF WS-OPERATION = 'LISTACCT'
               MOVE 'LISTACCT' TO WS-TARGET-PROGRAM.

           IF WS-OPERATION = 'LISTCARD'
               MOVE 'LISTCARD' TO WS-TARGET-PROGRAM.

           IF WS-OPERATION = 'ADDACCT'
               MOVE 'ADDACCT' TO WS-TARGET-PROGRAM.

           IF WS-OPERATION = 'CHGACCT'
               MOVE 'CHGACCT' TO WS-TARGET-PROGRAM.

           IF WS-OPERATION = 'ADDCARD'
               MOVE 'ADDCARD' TO WS-TARGET-PROGRAM.

           IF WS-OPERATION = 'CHGCARD'
               MOVE 'CHGCARD' TO WS-TARGET-PROGRAM.

           IF WS-OPERATION = 'POSTTXN'
               MOVE 'POSTTXN' TO WS-TARGET-PROGRAM.

           IF WS-OPERATION = 'LISTTXN'
               MOVE 'LISTTXN' TO WS-TARGET-PROGRAM.

           IF WS-TARGET-PROGRAM = SPACES
               MOVE 'ERROR' TO WS-STATE
               MOVE 'GETCUST ADDCUST LISTCUST CHGCUST LISTACCT LISTCARD'
                   TO WS-MESSAGE
               GO TO SHOW-SCREEN.

           MOVE SPACES TO WS-CALL-AREA.
           MOVE '01' TO CA-VERSION.
           MOVE WS-REQUEST-ID TO CA-REQUEST-ID.
           MOVE WS-OPERATION TO CA-OPERATION.
           MOVE WS-INPUT-LENGTH TO CA-INPUT-LENGTH.
           MOVE WS-INPUT TO CA-INPUT.
           MOVE ZERO TO CA-DATA-COUNT.
           MOVE ZERO TO WS-LINK-RESP WS-LINK-RESP2.

      * NEVER RETRY A WRITE BY RETURNING TO THIS COMMAREA.
           MOVE 'R' TO WS-MARKER.

           EXEC CICS LINK
               PROGRAM(WS-TARGET-PROGRAM)
               COMMAREA(WS-CALL-AREA)
               LENGTH(WS-CA-LENGTH)
               RESP(WS-LINK-RESP)
               RESP2(WS-LINK-RESP2)
           END-EXEC.

           IF WS-LINK-RESP NOT = ZERO
               MOVE 'ERROR' TO WS-STATE
               MOVE WS-LINK-RESP TO WS-LINK-CODE
               MOVE WS-LINK-RESP2 TO WS-LINK-CODE2
               MOVE WS-LINK-MESSAGE TO WS-MESSAGE
               GO TO SHOW-SCREEN.

           IF RH-PREFIX NOT = 'MBR'
               GO TO BAD-RESPONSE.

           IF RH-REQUEST-ID NOT = WS-REQUEST-ID
               GO TO BAD-RESPONSE.

           IF RH-OPERATION NOT = WS-OPERATION
               GO TO BAD-RESPONSE.

           IF RH-TYPE NOT = 'S'
               IF RH-TYPE NOT = 'E'
                   GO TO BAD-RESPONSE.

      * COMMAREA HOLDS ZERO OR ONE TERMINAL PREVIEW RECORD.
      * LIST OPERATIONS RETURN COMPLETE DATA THROUGH MBRESULT.
           IF CA-DATA-COUNT NOT = ZERO
               IF CA-DATA-COUNT NOT = 1
                   GO TO BAD-RESPONSE.

           IF CA-DATA-COUNT = 1
               IF RD-PREFIX NOT = 'MBR'
                   GO TO BAD-RESPONSE.

           IF CA-DATA-COUNT = 1
               IF RD-TYPE NOT = 'D'
                   GO TO BAD-RESPONSE.

           IF CA-DATA-COUNT = 1
               IF RD-REQUEST-ID NOT = WS-REQUEST-ID
                   GO TO BAD-RESPONSE.

           MOVE 'Y' TO WS-HAVE-RESULT.
           MOVE 'ERROR' TO WS-STATE.

           IF RH-TYPE = 'S'
               MOVE 'SUCCESS' TO WS-STATE.

           PERFORM PREPARE-RESULT-VIEW
               THRU PREPARE-RESULT-EXIT.

           MOVE 'RESULT READY. DATA 1-3 HEADER 4-6. ENTER=NEW.'
               TO WS-MESSAGE.

           GO TO SHOW-SCREEN.

       BAD-RESPONSE.

           MOVE 'ERROR' TO WS-STATE.
           MOVE 'INVALID RESPONSE. DO NOT RETRY WRITES.'
               TO WS-MESSAGE.
           GO TO SHOW-SCREEN.

       NEW-REQUEST.

           MOVE 'READY' TO WS-STATE.
           MOVE 'CUSTOMER OPS; LISTACCT LISTCARD. CLEAR=EXIT.'
               TO WS-MESSAGE.
           GO TO SHOW-SCREEN.

       COLLECT-INPUT.

           IF IN01L > ZERO
               MOVE IN01I TO WS-IN01.

           IF IN02L > ZERO
               MOVE IN02I TO WS-IN02.

           IF IN03L > ZERO
               MOVE IN03I TO WS-IN03.

           IF IN04L > ZERO
               MOVE IN04I TO WS-IN04.

           IF IN05L > ZERO
               MOVE IN05I TO WS-IN05.

           IF IN06L > ZERO
               MOVE IN06I TO WS-IN06.

           IF IN07L > ZERO
               MOVE IN07I TO WS-IN07.

           IF IN08L > ZERO
               MOVE IN08I TO WS-IN08.

       COLLECT-INPUT-EXIT.

           EXIT.

       NORMALIZE-INPUT.

           IF WS-INPUT-CHAR (WS-IDX) = LOW-VALUES
               MOVE SPACE TO WS-INPUT-CHAR (WS-IDX).

       NORMALIZE-INPUT-EXIT.

           EXIT.

       PREPARE-RESULT-VIEW.

           MOVE SPACES TO WS-INPUT.

           IF CA-DATA-COUNT = 1
               MOVE CA-DATA-RECORD TO WS-DISPLAY-RECORD
               MOVE WS-DISPLAY-1 TO WS-IN01
               MOVE WS-DISPLAY-2 TO WS-IN02
               MOVE WS-DISPLAY-3 TO WS-IN03.

           MOVE CA-HEADER-RECORD TO WS-DISPLAY-RECORD.
           MOVE WS-DISPLAY-1 TO WS-IN04.
           MOVE WS-DISPLAY-2 TO WS-IN05.
           MOVE WS-DISPLAY-3 TO WS-IN06.

       PREPARE-RESULT-EXIT.

           EXIT.

       SHOW-SCREEN.

           MOVE LOW-VALUES TO MBGWMAPO.
           MOVE WS-OPERATION TO OPERO.
           MOVE WS-REQUEST-ID TO REQIDO.
           MOVE WS-INPUT-LENGTH TO INLENO.
           MOVE WS-STATE TO STATEO.
           MOVE WS-MESSAGE TO MSGTXTO.
           MOVE SPACES TO ACKIDO.

           IF WS-HAVE-RESULT = 'Y'
               MOVE WS-REQUEST-ID TO ACKIDO.

           MOVE WS-IN01 TO IN01O.
           MOVE WS-IN02 TO IN02O.
           MOVE WS-IN03 TO IN03O.
           MOVE WS-IN04 TO IN04O.
           MOVE WS-IN05 TO IN05O.
           MOVE WS-IN06 TO IN06O.
           MOVE WS-IN07 TO IN07O.
           MOVE WS-IN08 TO IN08O.

           EXEC CICS SEND
               MAP('MBGWMAP')
               MAPSET('MBGWMSD')
               FROM(MBGWMAPO)
               ERASE
               FREEKB
               RESP(WS-RESP)
           END-EXEC.

           IF WS-RESP NOT = ZERO
               GO TO EXIT-PROGRAM.

           EXEC CICS RETURN
               TRANSID('MBGW')
               COMMAREA(WS-MARKER)
               LENGTH(WS-MARKER-LENGTH)
           END-EXEC.

       EXIT-PROGRAM.

           EXEC CICS RETURN END-EXEC.
