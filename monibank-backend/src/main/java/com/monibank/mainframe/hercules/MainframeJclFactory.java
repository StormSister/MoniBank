package com.monibank.mainframe.hercules;

import com.monibank.mainframe.config.MainframeProperties;
import com.monibank.mainframe.model.MainframeOperationSpec;
import com.monibank.mainframe.model.MainframeOperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MainframeJclFactory {

    private static final int RESULT_RECORD_LENGTH = 160;

    private final MainframeProperties properties;


    public String create(
            String jobName,
            String resultDataset,
            MainframeOperationSpec spec,
            String inputRecord
    ) {

        validateInput(
                spec,
                inputRecord
        );

        if (spec.type()
                != MainframeOperationType.WRITE) {

            throw new UnsupportedOperationException(
                    "Operation type not supported by create(): "
                            + spec.type()
            );
        }

        String card1 =
                inputRecord.substring(
                        0,
                        Math.min(
                                80,
                                inputRecord.length()
                        )
                );

        String card2 =
                inputRecord.length() > 80
                        ? inputRecord.substring(80)
                        : "";

        int entityLength =
                spec.entityRecordLength();

        return """
            //%s JOB (TEST),'MONIBANK WRITE',
            //             CLASS=A,
            //             MSGCLASS=A,
            //             MSGLEVEL=(1,1),
            //             USER=%s,
            //             PASSWORD=%s
            //WRITE    EXEC PGM=%s
            //STEPLIB  DD DSN=HERC01.TEST.LOADLIB,DISP=SHR
            //INPUT    DD *
            %s
            %s
            /*
            //OUTPUT   DD DSN=&&MBREC,
            //            DISP=(NEW,PASS),
            //            UNIT=SYSDA,
            //            SPACE=(TRK,(1,1)),
            //            DCB=(RECFM=FB,LRECL=%d,BLKSIZE=%d)
            //RESULT   DD DSN=%s,
            //            DISP=(NEW,CATLG,DELETE),
            //            UNIT=SYSDA,
            //            SPACE=(TRK,(1,1)),
            //            DCB=(RECFM=FB,LRECL=%d,BLKSIZE=%d)
            //SYSOUT   DD SYSOUT=A
            //LOADVSAM EXEC PGM=IDCAMS,COND=(0,NE,WRITE)
            //SYSPRINT DD SYSOUT=A
            //INDD     DD DSN=&&MBREC,DISP=(OLD,DELETE)
            //OUTVSAM  DD DSN=%s,DISP=SHR
            //SYSIN    DD *
              REPRO INFILE(INDD) OUTFILE(OUTVSAM)
            /*
            //SENDRES  EXEC PGM=IEBGENER,COND=(0,NE,LOADVSAM)
            //SYSPRINT DD SYSOUT=A
            //SYSUT1   DD DSN=%s,DISP=SHR
            //SYSUT2   DD SYSOUT=Z
            //SYSIN    DD DUMMY
            //
            """.formatted(
                jobName,
                properties.jobUser(),
                properties.jobPassword(),
                spec.programName(),
                card1,
                card2,

                entityLength,
                entityLength * 10,

                resultDataset,
                RESULT_RECORD_LENGTH,
                RESULT_RECORD_LENGTH * 10,

                spec.targetDataset(),

                resultDataset
        );
    }


    public String createReadAll(
            String jobName,
            String resultDataset,
            MainframeOperationSpec spec,
            String inputRecord
    ) {

        validateInput(
                spec,
                inputRecord
        );

        if (spec.type()
                != MainframeOperationType.READ_ALL) {

            throw new UnsupportedOperationException(
                    "Operation type not supported by createReadAll(): "
                            + spec.type()
            );
        }

        return """
            //%s JOB (TEST),'MONIBANK READ ALL',
            //             CLASS=A,
            //             MSGCLASS=A,
            //             MSGLEVEL=(1,1),
            //             USER=%s,
            //             PASSWORD=%s
            //READALL  EXEC PGM=%s
            //STEPLIB  DD DSN=HERC01.TEST.LOADLIB,DISP=SHR
            //INPUT    DD *
            %s
            /*
            //CUSTFILE DD DSN=%s,
            //            DISP=SHR,
            //            AMP=('AMORG','RECFM=F')
            //RESULT   DD DSN=%s,
            //            DISP=(NEW,CATLG,DELETE),
            //            UNIT=SYSDA,
            //            SPACE=(TRK,(5,2)),
            //            DCB=(RECFM=FB,LRECL=%d,BLKSIZE=%d)
            //SYSOUT   DD SYSOUT=A
            //SENDRES  EXEC PGM=IEBGENER,COND=(0,NE,READALL)
            //SYSPRINT DD SYSOUT=A
            //SYSUT1   DD DSN=%s,DISP=SHR
            //SYSUT2   DD SYSOUT=Z
            //SYSIN    DD DUMMY
            //
            """.formatted(
                jobName,
                properties.jobUser(),
                properties.jobPassword(),
                spec.programName(),
                inputRecord,
                spec.targetDataset(),
                resultDataset,
                RESULT_RECORD_LENGTH,
                RESULT_RECORD_LENGTH * 10,
                resultDataset
        );
    }


    public String createUpdate(
            String jobName,
            String resultDataset,
            MainframeOperationSpec spec,
            String inputRecord
    ) {

        validateInput(
                spec,
                inputRecord
        );

        if (spec.type()
                != MainframeOperationType.UPDATE) {

            throw new UnsupportedOperationException(
                    "Operation type not supported by createUpdate(): "
                            + spec.type()
            );
        }

        return """
                //%s JOB (TEST),'MONIBANK UPDATE',
                //             CLASS=A,
                //             MSGCLASS=A,
                //             MSGLEVEL=(1,1),
                //             USER=%s,
                //             PASSWORD=%s
                //UPDATE   EXEC PGM=%s
                //STEPLIB  DD DSN=HERC01.TEST.LOADLIB,DISP=SHR
                //INPUT    DD *
                %s
                /*
                //CUSTFILE DD DSN=%s,
                //            DISP=OLD,
                //            AMP=('AMORG','RECFM=F')
                //RESULT   DD DSN=%s,
                //            DISP=(NEW,CATLG,DELETE),
                //            UNIT=SYSDA,
                //            SPACE=(TRK,(1,1)),
                //            DCB=(RECFM=FB,LRECL=%d,BLKSIZE=%d)
                //SYSOUT   DD SYSOUT=A
                //SENDRES  EXEC PGM=IEBGENER,COND=(0,NE,UPDATE)
                //SYSPRINT DD SYSOUT=A
                //SYSUT1   DD DSN=%s,DISP=SHR
                //SYSUT2   DD SYSOUT=Z
                //SYSIN    DD DUMMY
                //
                """.formatted(
                jobName,
                properties.jobUser(),
                properties.jobPassword(),
                spec.programName(),
                inputRecord,
                spec.targetDataset(),
                resultDataset,
                RESULT_RECORD_LENGTH,
                RESULT_RECORD_LENGTH * 10,
                resultDataset
        );
    }


    public String createResultRead(
            String jobName,
            String datasetName
    ) {

        return """
                //%s JOB (TEST),'READ RESULT',
                //             CLASS=A,
                //             MSGCLASS=A,
                //             MSGLEVEL=(1,1),
                //             USER=%s,
                //             PASSWORD=%s
                //PRINT    EXEC PGM=IEBGENER
                //SYSPRINT DD SYSOUT=*
                //SYSUT1   DD DSN=%s,DISP=SHR
                //SYSUT2   DD SYSOUT=*
                //SYSIN    DD DUMMY
                //
                """.formatted(
                jobName,
                properties.jobUser(),
                properties.jobPassword(),
                datasetName
        );
    }


    public String createResultDelete(
            String jobName,
            String datasetName
    ) {

        return """
                //%s JOB (TEST),'DELETE RESULT',
                //             CLASS=A,
                //             MSGCLASS=A,
                //             MSGLEVEL=(1,1),
                //             USER=%s,
                //             PASSWORD=%s
                //DELETE   EXEC PGM=IDCAMS
                //SYSPRINT DD SYSOUT=*
                //SYSIN    DD *
                  DELETE %s
                /*
                //
                """.formatted(
                jobName,
                properties.jobUser(),
                properties.jobPassword(),
                datasetName
        );
    }

    public String createReadOne(
            String jobName,
            String resultDataset,
            MainframeOperationSpec spec,
            String inputRecord
    ) {

        validateInput(
                spec,
                inputRecord
        );

        if (spec.type()
                != MainframeOperationType.READ_ONE) {

            throw new UnsupportedOperationException(
                    "Operation type not supported by createReadOne(): "
                            + spec.type()
            );
        }

        throw new UnsupportedOperationException(
                "READ_ONE JCL is not implemented yet"
        );
    }



    private void validateInput(
            MainframeOperationSpec spec,
            String inputRecord
    ) {

        if (inputRecord == null) {
            throw new IllegalArgumentException(
                    "Mainframe input record cannot be null"
            );
        }

        if (inputRecord.length()
                != spec.inputRecordLength()) {

            throw new IllegalArgumentException(
                    "Expected input record length "
                            + spec.inputRecordLength()
                            + ", got "
                            + inputRecord.length()
            );
        }
    }
}