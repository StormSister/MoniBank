package com.monibank.mainframe.hercules.jcl;

import com.monibank.mainframe.config.MainframeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompileCobolJclFactory {

    private final MainframeProperties properties;

    public String create(String programName) {

        validateProgramName(programName);

        return """
                //CMPCOB  JOB (TEST),'COMPILE COBOL',
                //             CLASS=A,
                //             MSGCLASS=A,
                //             MSGLEVEL=(1,1),
                //             USER=%s,
                //             PASSWORD=%s
                //COMPILE EXEC COBUCL
                //COB.SYSIN DD DSN=HERC01.MBANK.COBOL(%s),DISP=SHR
                //COB.SYSPUNCH DD DUMMY
                //COB.SYSLIB DD DSN=SYS1.COBLIB,DISP=SHR
                //LKED.SYSLMOD DD DSN=HERC01.TEST.LOADLIB(%s),DISP=SHR
                //
                """.formatted(
                properties.jobUser(),
                properties.jobPassword(),
                programName,
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