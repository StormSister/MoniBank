//MBLIBS  JOB (TEST),'CREATE MBANK LIBS',
//             CLASS=A,
//             MSGCLASS=A,
//             MSGLEVEL=(1,1),
//             USER=${JOB_USER},
//             PASSWORD=${JOB_PASSWORD}
//COBOL    EXEC PGM=IEFBR14
//COBOLDD  DD DSN=HERC01.MBANK.COBOL,
//             DISP=(NEW,CATLG,DELETE),
//             UNIT=3390,
//             VOL=SER=TSO001,
//             SPACE=(TRK,(10,5,20)),
//             DCB=(DSORG=PO,RECFM=FB,LRECL=80,BLKSIZE=3200)
//LOADLIB  EXEC PGM=IEFBR14
//LOADDD   DD DSN=HERC01.MBANK.LOADLIB,
//             DISP=(NEW,CATLG,DELETE),
//             UNIT=3390,
//             VOL=SER=TSO001,
//             SPACE=(TRK,(10,5,20)),
//             DCB=(DSORG=PO,RECFM=U,BLKSIZE=6144)
//