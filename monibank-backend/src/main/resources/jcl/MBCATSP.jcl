//MBCATSP JOB (TEST),'MBANK CAT SPACE',
//             CLASS=A,
//             MSGCLASS=A,
//             MSGLEVEL=(1,1),
//             USER=${JOB_USER},
//             PASSWORD=${JOB_PASSWORD}
//ALIAS    EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//SYSIN    DD *
  DEFINE ALIAS ( -
         NAME(MBANK) -
         RELATE(SYS1.UCAT.TSO) -
  )
/*
//SPACE    EXEC PGM=IDCAMS,REGION=4096K,
//             COND=(0,NE,ALIAS)
//SYSPRINT DD SYSOUT=*
//VOLDD    DD DISP=OLD,
//             UNIT=3390,
//             VOL=SER=TSO001
//SYSIN    DD *
  DEFINE SPACE ( -
         CYLINDERS(300) -
         VOLUMES(TSO001) -
         FILE(VOLDD) -
  ) -
  CATALOG(SYS1.UCAT.TSO)
/*