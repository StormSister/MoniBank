package com.monibank.mainframe.hercules.jcl;

import com.monibank.mainframe.config.MainframeProperties;
import com.monibank.mainframe.hercules.MainframeResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class PutCobolJclFactory {

    private final MainframeProperties properties;
    private final MainframeResourceLoader resources;

    public PutCobolJclFactory(
            MainframeProperties properties,
            MainframeResourceLoader resources
    ) {
        this.properties = properties;
        this.resources = resources;
    }

    public String create(String programName) {

        String normalizedProgramName =
                MainframeResourceLoader.normalizeCobolProgramName(
                        programName
                );

        String cobolSource =
                resources.loadCobol(normalizedProgramName);

        return """
                //PUTCOB  JOB (TEST),'PUT COBOL',
                //             CLASS=A,
                //             MSGCLASS=A,
                //             MSGLEVEL=(1,1),
                //             USER=%s,
                //             PASSWORD=%s
                //STEP1    EXEC PGM=IEBGENER
                //SYSPRINT DD SYSOUT=*
                //SYSUT1   DD *
                %s
                /*
                //SYSUT2   DD DSN=HERC01.MBANK.COBOL(%s),DISP=SHR
                //SYSIN    DD DUMMY
                //
                """.formatted(
                properties.jobUser(),
                properties.jobPassword(),
                cobolSource,
                normalizedProgramName
        );
    }
}