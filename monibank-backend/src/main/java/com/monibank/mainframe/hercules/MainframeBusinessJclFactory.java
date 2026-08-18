package com.monibank.mainframe.hercules;

import com.monibank.mainframe.config.MainframeProperties;
import com.monibank.mainframe.model.MainframeOperationSpec;
import com.monibank.mainframe.model.MainframeOperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MainframeBusinessJclFactory {

    private final MainframeProperties properties;

    public String create(
            String jobName,
            MainframeOperationSpec spec,
            String inputRecord
    ) {

        validate(spec, inputRecord);

        if (spec.type() != MainframeOperationType.WRITE) {
            throw new UnsupportedOperationException(
                    "Operation type not supported yet: " + spec.type()
            );
        }

        String card1 = inputRecord.substring(
                0,
                Math.min(80, inputRecord.length())
        );

        String card2 = inputRecord.length() > 80
                ? inputRecord.substring(80)
                : "";

        return """
                //%s JOB (TEST),'MONIBANK BUSINESS',
                //             CLASS=A,
                //             MSGCLASS=A,
                //             MSGLEVEL=(1,1),
                //             USER=%s,
                //             PASSWORD=%s
                //COBSTEP  EXEC PGM=%s
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
                //SYSOUT   DD SYSOUT=*
                //LOADVSAM EXEC PGM=IDCAMS,COND=(0,NE,COBSTEP)
                //SYSPRINT DD SYSOUT=*
                //INDD     DD DSN=&&MBREC,DISP=(OLD,DELETE)
                //OUTVSAM  DD DSN=%s,DISP=SHR
                //SYSIN    DD *
                  REPRO INFILE(INDD) OUTFILE(OUTVSAM)
                /*
                //
                """.formatted(
                jobName,
                properties.jobUser(),
                properties.jobPassword(),
                spec.programName(),
                card1,
                card2,
                spec.recordLength(),
                spec.recordLength(),
                spec.targetDataset()
        );
    }

    public String createReadAll(
            String jobName,
            MainframeOperationSpec spec
    ) {

        return """
            //%s JOB (TEST),'MONIBANK READ ALL',
            //             CLASS=A,
            //             MSGCLASS=A,
            //             MSGLEVEL=(1,1),
            //             USER=%s,
            //             PASSWORD=%s
            //COPY     EXEC PGM=IDCAMS
            //SYSPRINT DD SYSOUT=*
            //OUTSEQ   DD DSN=&&MBREAD,
            //            DISP=(NEW,PASS),
            //            UNIT=SYSDA,
            //            SPACE=(TRK,(5,2)),
            //            DCB=(RECFM=FB,LRECL=%d,BLKSIZE=%d)
            //SYSIN    DD *
              REPRO INDATASET(%s) OUTFILE(OUTSEQ)
            /*
            //PRINT    EXEC PGM=IEBGENER,COND=(0,NE,COPY)
            //SYSPRINT DD SYSOUT=*
            //SYSUT1   DD DSN=&&MBREAD,DISP=(OLD,DELETE)
            //SYSUT2   DD SYSOUT=*
            //SYSIN    DD DUMMY
            //
            """.formatted(
                jobName,
                properties.jobUser(),
                properties.jobPassword(),
                spec.recordLength(),
                spec.recordLength() * 10,
                spec.targetDataset()
        );
    }


    public String createUpdate(
            String jobName,
            String resultDataset,
            MainframeOperationSpec spec,
            String inputRecord
    ) {

        validate(spec, inputRecord);

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
            //            DCB=(RECFM=FB,LRECL=160,BLKSIZE=1600)
            //SYSOUT   DD SYSOUT=*
            //
            """.formatted(
                jobName,
                properties.jobUser(),
                properties.jobPassword(),
                spec.programName(),
                inputRecord,
                spec.targetDataset(),
                resultDataset
        );
    }

    private void validate(
            MainframeOperationSpec spec,
            String inputRecord
    ) {

        if (inputRecord == null) {
            throw new IllegalArgumentException(
                    "Mainframe input record cannot be null"
            );
        }

        if (inputRecord.length() != spec.recordLength()) {
            throw new IllegalArgumentException(
                    "Expected record length "
                            + spec.recordLength()
                            + ", got "
                            + inputRecord.length()
            );
        }
    }
}