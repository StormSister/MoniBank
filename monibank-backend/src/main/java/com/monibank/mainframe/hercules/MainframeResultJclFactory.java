package com.monibank.mainframe.hercules;

import com.monibank.mainframe.config.MainframeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MainframeResultJclFactory {

    private final MainframeProperties properties;

    public String createRead(
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

    public String createDelete(
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
}