//PUTACCP JOB (TEST),'PUT ACCOUNT COPYBOOK',
//             CLASS=A,
//             MSGCLASS=A,
//             MSGLEVEL=(1,1),
//             USER=${JOB_USER},
//             PASSWORD=${JOB_PASSWORD}
//STEP1    EXEC PGM=IEBGENER
//SYSPRINT DD SYSOUT=*
//SYSUT1   DD *
           05 ACCOUNT-STATUS          PIC X.
           05 ACCOUNT-ID              PIC X(13).
           05 ACCOUNT-CUSTOMER-ID     PIC X(13).
           05 ACCOUNT-IBAN            PIC X(34).
           05 ACCOUNT-TYPE            PIC X(2).
           05 ACCOUNT-CURRENCY        PIC X(3).
           05 ACCOUNT-BALANCE         PIC S9(13)V99 COMP-3.
           05 ACCOUNT-OVERDRAFT       PIC S9(13)V99 COMP-3.
           05 ACCOUNT-BLOCKED         PIC S9(13)V99 COMP-3.
           05 ACCOUNT-CREATED-AT      PIC X(14).
           05 ACCOUNT-UPDATED-AT      PIC X(14).
           05 FILLER                  PIC X.
/*
//SYSUT2   DD DSN=HERC01.KICKS.V1R5M0.COBCOPY(MBACCT),
//            DISP=SHR
//SYSIN    DD DUMMY
//