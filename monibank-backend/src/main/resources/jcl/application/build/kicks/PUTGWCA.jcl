//PUTGWCA JOB (TEST),'PUT GATEWAY COPY',
//             CLASS=A,
//             MSGCLASS=A,
//             MSGLEVEL=(1,1),
//             USER=${JOB_USER},
//             PASSWORD=${JOB_PASSWORD}
//STEP1    EXEC PGM=IEBGENER
//SYSPRINT DD SYSOUT=*
//SYSUT1   DD *
           05 CA-VERSION           PIC X(2).
           05 CA-REQUEST-ID        PIC X(8).
           05 CA-OPERATION         PIC X(8).
           05 CA-INPUT-LENGTH      PIC X(4).
           05 CA-INPUT             PIC X(512).
           05 CA-DATA-COUNT        PIC 9.

           05 CA-DATA-RECORD.
               10 RD-PREFIX        PIC X(3).
               10 RD-SEP-0         PIC X.
               10 RD-TYPE          PIC X.
               10 RD-SEP-1         PIC X.
               10 RD-ENTITY        PIC X(8).
               10 RD-SEP-2         PIC X.
               10 RD-REQUEST-ID    PIC X(8).
               10 RD-SEP-3         PIC X.
               10 RD-PAYLOAD       PIC X(119).
               10 FILLER           PIC X(17).

           05 CA-HEADER-RECORD.
               10 RH-PREFIX        PIC X(3).
               10 RH-SEP-0         PIC X.
               10 RH-TYPE          PIC X.
               10 RH-SEP-1         PIC X.
               10 RH-OPERATION     PIC X(7).
               10 RH-SEP-2         PIC X.
               10 RH-REQUEST-ID    PIC X(8).
               10 RH-SEP-3         PIC X.
               10 RH-CUSTOMER-ID   PIC X(13).
               10 RH-SEP-4         PIC X.
               10 RH-STATUS        PIC X.
               10 RH-SEP-5         PIC X.
               10 RH-ERROR-CODE    PIC X(20).
               10 FILLER           PIC X(101).
/*
//SYSUT2   DD DSN=HERC01.KICKS.V1R5M0.COBCOPY(MBGWCA),
//            DISP=SHR
//SYSIN    DD DUMMY
//