//RPTPDS JOB (TEST),'CREATE REPORT PDS',
//             CLASS=A,
//             MSGCLASS=A,
//             MSGLEVEL=(1,1),
//             USER=${JOB_USER},
//             PASSWORD=${JOB_PASSWORD}
//*
//ALLOC    EXEC PGM=IEFBR14
//REPORTS  DD DSN=HERC01.MBANK.REPORTS,
//            DISP=(NEW,CATLG,DELETE),
//            UNIT=3390,
//            VOL=SER=TSO001,
//            SPACE=(TRK,(5,2,50)),
//            DCB=(DSORG=PO,RECFM=FB,LRECL=160,BLKSIZE=1600)

