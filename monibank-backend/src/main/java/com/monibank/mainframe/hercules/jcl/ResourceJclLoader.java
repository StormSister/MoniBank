package com.monibank.mainframe.hercules.jcl;

import com.monibank.mainframe.config.MainframeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ResourceJclLoader {

    private final MainframeProperties properties;

    public String load(String jobName) {

        String normalized =
                jobName.toUpperCase();

        if (!normalized.matches("[A-Z0-9]{1,8}")) {
            throw new IllegalArgumentException(
                    "Invalid JCL resource name"
            );
        }

        ClassPathResource resource =
                new ClassPathResource(
                        "jcl/" + normalized + ".jcl"
                );

        if (!resource.exists()) {
            throw new IllegalArgumentException(
                    "JCL resource not found: "
                            + normalized
            );
        }

        try {

            String template =
                    resource.getContentAsString(
                            StandardCharsets.UTF_8
                    );

            return template
                    .replace(
                            "${JOB_USER}",
                            properties.jobUser()
                    )
                    .replace(
                            "${JOB_PASSWORD}",
                            properties.jobPassword()
                    );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Could not read JCL resource: "
                            + normalized,
                    e
            );
        }
    }
}