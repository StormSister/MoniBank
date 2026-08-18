package com.monibank.mainframe.hercules.jcl;

import com.monibank.mainframe.config.MainframeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class PutCobolJclFactory {

    private final MainframeProperties properties;

    public String create(String programName) {

        String cobolSource = readCobolSource(programName + ".cob");

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
                programName
        );
    }

    private String readCobolSource(String fileName) {
        try {
            ClassPathResource resource =
                    new ClassPathResource("cobol/" + fileName);

            return resource.getContentAsString(StandardCharsets.US_ASCII);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not read COBOL source: " + fileName,
                    e
            );
        }
    }
}