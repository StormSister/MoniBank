//SEQVSAM JOB (TEST),'CREATE SEQUENCE VSAM',
//             CLASS=A,
//             MSGCLASS=A,
//             MSGLEVEL=(1,1),
//             USER=${JOB_USER},
//             PASSWORD=${JOB_PASSWORD}
//STEP1    EXEC PGM=IDCAMS
//STEPCAT  DD DSN=SYS1.UCAT.TSO,DISP=SHR
//SYSPRINT DD SYSOUT=*
//SYSIN    DD *
  DEFINE CLUSTER ( -
         NAME(MBANK.SEQ) -
         VOLUMES(TSO001) -
         INDEXED -
         KEYS(12 0) -
         RECORDSIZE(32 32) -
         TRACKS(1 1) -
         SHAREOPTIONS(2 3) -
  ) -
  DATA ( -
       NAME(MBANK.SEQ.D) -
  ) -
  INDEX ( -
        NAME(MBANK.SEQ.I) -
  )
/*