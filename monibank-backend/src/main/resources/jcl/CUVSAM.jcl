+---+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
//CUSVSAM JOB (TEST),'CREATE CUSTOMER VSAM',                            00000100
//             CLASS=A,                                                 00000200
//             MSGCLASS=A,                                              00000300
//             MSGLEVEL=(1,1)                                           00000400
//STEP1    EXEC PGM=IDCAMS                                              00000500
//SYSPRINT DD SYSOUT=*                                                  00000600
//SYSIN    DD *                                                         00000700
  DEFINE CLUSTER ( -                                                    00000801
         NAME(MBANK.CUST) -                                             00000903
         VOLUMES(PUB002) -                                              00001000
         INDEXED -                                                      00001100
         KEYS(13 0) -                                                   00001200
         RECORDSIZE(119 119) -                                          00001300
         TRACKS(5 2) -                                                  00001401
 ) -                                                                    00001500
 DATA ( -                                                               00001601
      NAME(MBANK.CUST.D) -                                              00001703
 ) -                                                                    00001800
 INDEX ( -                                                              00001901
       NAME(MBANK.CUST.I) -                                             00002003
 )                                                                      00002100
/*                                                                      00002200