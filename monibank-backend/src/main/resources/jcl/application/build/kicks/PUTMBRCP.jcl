//PUTMBRCP JOB (TEST),'PUT MBR COPYBOOKS',
//             CLASS=A,
//             MSGCLASS=A,
//             MSGLEVEL=(1,1),
//             USER=${JOB_USER},
//             PASSWORD=${JOB_PASSWORD}
//*
//PUTREC   EXEC PGM=IEBGENER
//SYSPRINT DD SYSOUT=*
//SYSUT1   DD *
       01  MBR-RESULT-RECORD PIC X(160).

       01  MBR-RESULT-HEADER
           REDEFINES MBR-RESULT-RECORD.
           05  MBR-H-PREFIX      PIC X(3).
           05  MBR-H-SEP-0       PIC X.
           05  MBR-H-TYPE        PIC X.
           05  MBR-H-SEP-1       PIC X.
           05  MBR-H-OPERATION   PIC X(8).
           05  MBR-H-SEP-2       PIC X.
           05  MBR-H-REQUEST-ID  PIC X(8).
           05  MBR-H-SEP-3       PIC X.
           05  MBR-H-ENTITY-ID   PIC X(13).
           05  MBR-H-SEP-4       PIC X.
           05  MBR-H-STATUS      PIC X.
           05  MBR-H-SEP-5       PIC X.
           05  MBR-H-CODE        PIC X(20).
           05  FILLER            PIC X(100).

       01  MBR-RESULT-DATA
           REDEFINES MBR-RESULT-RECORD.
           05  MBR-D-PREFIX      PIC X(3).
           05  MBR-D-SEP-0       PIC X.
           05  MBR-D-TYPE        PIC X.
           05  MBR-D-SEP-1       PIC X.
           05  MBR-D-ENTITY      PIC X(8).
           05  MBR-D-SEP-2       PIC X.
           05  MBR-D-REQUEST-ID  PIC X(8).
           05  MBR-D-SEP-3       PIC X.
           05  MBR-D-PAYLOAD     PIC X(136).
/*
//SYSUT2   DD DSN=HERC01.KICKS.V1R5M0.COBCOPY(MBRREC),
//            DISP=SHR
//SYSIN    DD DUMMY
//*
//PUTSCA   EXEC PGM=IEBGENER
//SYSPRINT DD SYSOUT=*
//SYSUT1   DD *
           05  MBR-CALL-CONTROL       PIC X.
               88  MBR-CALL-MORE      VALUE 'M'.
               88  MBR-CALL-FINAL     VALUE 'F'.

           05  MBR-CALL-TOKEN         PIC X(8).
           05  MBR-CALL-RETURN-CODE   PIC X(8).
           05  MBR-CALL-RESP          PIC S9(8) COMP.
           05  MBR-CALL-RESP2         PIC S9(8) COMP.
           05  MBR-CALL-RECORD        PIC X(160).
/*
//SYSUT2   DD DSN=HERC01.KICKS.V1R5M0.COBCOPY(MBRSCA),
//            DISP=SHR
//SYSIN    DD DUMMY
//