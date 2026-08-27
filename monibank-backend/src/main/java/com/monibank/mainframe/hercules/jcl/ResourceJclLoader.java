package com.monibank.mainframe.hercules.jcl;

import com.monibank.mainframe.config.MainframeProperties;
import com.monibank.mainframe.hercules.MainframeResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class ResourceJclLoader {

    private final MainframeProperties properties;
    private final MainframeResourceLoader resources;

    public ResourceJclLoader(
            MainframeProperties properties,
            MainframeResourceLoader resources
    ) {
        this.properties = properties;
        this.resources = resources;
    }

    public String load(String jobName) {

        String template = resources.loadJcl(jobName);

        return template
                .replace(
                        "${JOB_USER}",
                        properties.jobUser()
                )
                .replace(
                        "${JOB_PASSWORD}",
                        properties.jobPassword()
                );
    }
}