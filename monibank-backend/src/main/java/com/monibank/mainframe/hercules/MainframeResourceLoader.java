package com.monibank.mainframe.hercules;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class MainframeResourceLoader {

    private final Map<String, Resource> jclResources;
    private final Map<String, Resource> cobolResources;

    public MainframeResourceLoader(ResourceLoader resourceLoader) {
        ResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver(resourceLoader);

        jclResources = index(resolver, "jcl", ".jcl", "[A-Z0-9]{1,8}");
        cobolResources = index(resolver, "cobol", ".cob", "[A-Z][A-Z0-9@$#]{0,7}");
    }

    public String loadJcl(String jobName) {
        String normalized = normalize(jobName, "[A-Z0-9]{1,8}", "JCL resource name");
        return read(jclResources, normalized, "JCL", StandardCharsets.UTF_8);
    }

    public String loadCobol(String programName) {
        String normalized = normalizeCobolProgramName(programName);
        return read(cobolResources, normalized, "COBOL", StandardCharsets.US_ASCII);
    }

    public static String normalizeCobolProgramName(String programName) {
        return normalize(programName, "[A-Z][A-Z0-9@$#]{0,7}", "COBOL program name");
    }

    private static Map<String, Resource> index(
            ResourcePatternResolver resolver,
            String directory,
            String extension,
            String namePattern
    ) {
        Map<String, Resource> resources = new LinkedHashMap<>();
        String locationPattern = "classpath*:/" + directory + "/**/*";

        try {
            for (Resource resource : resolver.getResources(locationPattern)) {
                String fileName = resource.getFilename();

                // Filter here so .jcl/.JCL and .cob/.COB work on Linux too.
                if (fileName == null
                        || !fileName.toLowerCase(Locale.ROOT).endsWith(extension)
                        || !resource.isReadable()) {
                    continue;
                }

                String name = fileName.substring(0, fileName.length() - extension.length());
                String normalized = name.toUpperCase(Locale.ROOT);

                if (!normalized.matches(namePattern)) {
                    throw new IllegalStateException(
                            "Invalid resource filename: " + resource.getDescription()
                    );
                }

                Resource previous = resources.putIfAbsent(normalized, resource);

                if (previous != null && !previous.equals(resource)) {
                    throw new IllegalStateException(
                            "Duplicate " + extension + " resource name '" + normalized
                                    + "': " + previous.getDescription()
                                    + " and " + resource.getDescription()
                    );
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not scan mainframe resources: " + locationPattern, e
            );
        }

        // Cache resource locations, never rendered JCL containing credentials.
        return Map.copyOf(resources);
    }

    private static String read(
            Map<String, Resource> resources,
            String name,
            String type,
            Charset charset
    ) {
        Resource resource = resources.get(name);

        if (resource == null) {
            throw new IllegalArgumentException(type + " resource not found: " + name);
        }

        // Do not use getFile(): resources may live inside an executable JAR.
        try (InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), charset);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not read " + type + " resource: " + name, e
            );
        }
    }

    private static String normalize(String name, String pattern, String description) {
        if (name == null) {
            throw new IllegalArgumentException(description + " is required");
        }

        String normalized = name.toUpperCase(Locale.ROOT);

        if (!normalized.matches(pattern)) {
            throw new IllegalArgumentException("Invalid " + description + ": " + name);
        }

        return normalized;
    }
}
