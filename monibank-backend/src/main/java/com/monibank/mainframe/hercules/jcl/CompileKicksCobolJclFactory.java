package com.monibank.mainframe.hercules.jcl;

import com.monibank.mainframe.config.MainframeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class CompileKicksCobolJclFactory {

    private final MainframeProperties properties;

    public String create(String programName) {

        String normalizedProgramName =
                normalizeProgramName(programName);

        return """
                //KIKCOMP JOB (TEST),'COMPILE KICKS',
                //             CLASS=A,
                //             MSGCLASS=A,
                //             MSGLEVEL=(1,1),
                //             USER=%s,
                //             PASSWORD=%s
                //JOBPROC  DD DSN=HERC01.KICKSSYS.V1R5M0.PROCLIB,
                //             DISP=SHR
                //COMPILE  EXEC PROC=K2KCOBCL
                //COPY.SYSUT1 DD DSN=HERC01.MBANK.COBOL(%s),
                //             DISP=SHR
                //LKED.SYSIN DD *
                  INCLUDE SKIKLOAD(KIKCOBGL)
                  ENTRY %s
                  NAME %s(R)
                /*
                //
                """.formatted(
                properties.jobUser(),
                properties.jobPassword(),
                normalizedProgramName,
                normalizedProgramName,
                normalizedProgramName
        );
    }

    private String normalizeProgramName(
            String programName
    ) {

        if (programName == null) {
            throw new IllegalArgumentException(
                    "COBOL program name is required"
            );
        }

        String normalized =
                programName.toUpperCase(Locale.ROOT);

        if (!normalized.matches(
                "[A-Z][A-Z0-9@$#]{0,7}"
        )) {
            throw new IllegalArgumentException(
                    "Invalid COBOL program name: "
                            + programName
            );
        }

        return normalized;
    }
}