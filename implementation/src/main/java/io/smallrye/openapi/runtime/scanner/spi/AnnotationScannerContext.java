package io.smallrye.openapi.runtime.scanner.spi;

import java.util.List;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.runtime.scanner.AnnotationScannerExtension;
import io.smallrye.openapi.runtime.scanner.FilteredIndexView;

/**
 * Context for scanners.
 * 
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public class AnnotationScannerContext {
    private final FilteredIndexView index;
    private final List<AnnotationScannerExtension> extensions;
    private final OpenApiConfig config;

    public AnnotationScannerContext(FilteredIndexView index, List<AnnotationScannerExtension> extensions,
            OpenApiConfig config) {
        this.index = index;
        this.extensions = extensions;
        this.config = config;
    }

    public FilteredIndexView getIndex() {
        return index;
    }

    public List<AnnotationScannerExtension> getExtensions() {
        return extensions;
    }

    public OpenApiConfig getConfig() {
        return config;
    }

}
