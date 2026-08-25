package com.monibank.mainframe.hercules;

import com.monibank.mainframe.config.MainframeProperties;
import com.monibank.mainframe.model.MainframeOperationSpec;
import com.monibank.mainframe.model.MainframeOperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

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

        String inputCards =
                createInputCards(
                        inputRecord
                );

        String datasetDds =
                createDatasetDdStatements(
                        spec
                );

        return """
            //%s JOB (TEST),'MONIBANK OPERATION',
            //             CLASS=A,
            //             MSGCLASS=A,
            //             MSGLEVEL=(1,1),
            //             USER=%s,
            //             PASSWORD=%s
            //MAIN     EXEC PGM=%s
            //STEPLIB  DD DSN=HERC01.TEST.LOADLIB,DISP=SHR
            //INPUT    DD *
            %s
            /*
            %s
            //RESULT   DD DSN=%s,
            //            DISP=(NEW,CATLG,DELETE),
            //            UNIT=SYSDA,
            //            SPACE=(TRK,(5,2)),
            //            DCB=(RECFM=FB,LRECL=%d,BLKSIZE=%d)
            //SYSOUT   DD SYSOUT=A
            //SENDRES  EXEC PGM=IEBGENER,COND=(0,NE,MAIN)
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
                inputCards,
                datasetDds,
                resultDataset,
                RESULT_RECORD_LENGTH,
                RESULT_RECORD_LENGTH * 10,
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

    private String createInputCards(
            String inputRecord
    ) {

        StringBuilder result =
                new StringBuilder();

        for (int start = 0;
             start < inputRecord.length();
             start += 80) {

            int end =
                    Math.min(
                            start + 80,
                            inputRecord.length()
                    );

            result.append(
                    inputRecord,
                    start,
                    end
            );

            result.append('\n');
        }

        return result.toString();
    }

    private String createDatasetDdStatements(
            MainframeOperationSpec spec
    ) {

        return spec.datasets()
                .stream()
                .map(dataset -> """
                    //%s DD DSN=%s,DISP=%s
                    """.formatted(
                        dataset.ddName(),
                        dataset.datasetName(),
                        dataset.mode().disposition()
                ))
                .collect(Collectors.joining());
    }
}