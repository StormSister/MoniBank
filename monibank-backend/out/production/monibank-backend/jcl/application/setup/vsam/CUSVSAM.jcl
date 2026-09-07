//CUSVSAM JOB (TEST),'CREATE CUSTOMER VSAM',
//             CLASS=A,
//             MSGCLASS=A,
//             MSGLEVEL=(1,1),
//             USER=${JOB_USER},
//             PASSWORD=${JOB_PASSWORD}
//STEP1    EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//SYSIN    DD *
  DEFINE CLUSTER ( -
         NAME(MBANK.CUST) -
         VOLUMES(TSO001) -
         INDEXED -
         KEYS(13 1) -
         RECORDSIZE(119 119) -
         TRACKS(5 2) -
         SHAREOPTIONS(2 3) -
  ) -
  DATA ( -
       NAME(MBANK.CUST.D) -
  ) -
  INDEX ( -
        NAME(MBANK.CUST.I) -
  ) -
  CATALOG(SYS1.UCAT.TSO)
/*