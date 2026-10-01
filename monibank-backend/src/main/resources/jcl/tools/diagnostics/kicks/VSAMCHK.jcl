//VSAMCHK  JOB (MBANK),'VSAM CAPACITY',
//             CLASS=A,
//             MSGCLASS=A,
//             MSGLEVEL=(1,1),
//             USER=${JOB_USER},
//             PASSWORD=${JOB_PASSWORD}
//*
//* ------------------------------------------------------------
//* MONIBANK VSAM CAPACITY CHECK
//* READ-ONLY DIAGNOSTIC JOB
//*
//* Lists all catalog information for MBANK datasets.
//* Used to inspect:
//*   - primary/secondary allocation
//*   - allocated vs used space
//*   - extents
//*   - CI/CA information
//*   - base clusters
//*   - alternate indexes
//*   - paths
//*
//* THIS JOB DOES NOT MODIFY ANY DATASET.
//* ------------------------------------------------------------
//*
//LISTCAT  EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//SYSIN    DD *
  LISTCAT LEVEL(MBANK) ALL
/*
//