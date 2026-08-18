package com.monibank.mainframe.hercules.jcl;

import com.monibank.mainframe.config.MainframeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RunCobolJclFactory {

    private final MainframeProperties properties;

    public String create(String programName) {

        validateProgramName(programName);

        return """
                //RUNCOB  JOB (TEST),'RUN COBOL',
                //             CLASS=A,
                //             MSGCLASS=A,
                //             MSGLEVEL=(1,1),
                //             USER=%s,
                //             PASSWORD=%s
                //STEP1    EXEC PGM=%s
                //STEPLIB  DD DSN=HERC01.TEST.LOADLIB,DISP=SHR
                //SYSOUT   DD SYSOUT=*
                //
                """.formatted(
                properties.jobUser(),
                properties.jobPassword(),
                programName
        );
    }

    private void validateProgramName(String programName) {
        if (programName == null
                || !programName.matches("[A-Z][A-Z0-9@$#]{0,7}")) {
            throw new IllegalArgumentException(
                    "Invalid COBOL program name: " + programName
            );
        }
    }
}