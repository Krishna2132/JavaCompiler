package rkcc.compiler.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable context passed to an {@link AssemblyBackend} when delegating
 * assembly emission for a parsed type construct.
 *
 * <p>Keeps target configuration decoupled from concrete assembly emitters.</p>
 */
public final class AssemblyGenerationContext {

    private final String targetArchitecture;
    private final String sectionHint;
    private final Map<String, String> attributes;

    /**
     * Creates a context for the given target architecture with no section hint or extra attributes.
     *
     * @param targetArchitecture non-blank target identifier (for example, {@code "x86_64-nasm-proposed"})
     */
    public AssemblyGenerationContext(String targetArchitecture) {
        this(targetArchitecture, null, Map.of());
    }

    /**
     * Creates an assembly generation context with optional section hints and custom attributes.
     *
     * @param targetArchitecture non-blank target identifier
     * @param sectionHint optional target section hint (for example, {@code ".text"} or {@code ".data"})
     * @param attributes non-null map of additional backend attributes
     */
    public AssemblyGenerationContext(
            String targetArchitecture,
            String sectionHint,
            Map<String, String> attributes) {
        if (targetArchitecture == null || targetArchitecture.isBlank()) {
            throw new IllegalArgumentException("targetArchitecture must not be null or blank");
        }
        Objects.requireNonNull(attributes, "attributes must not be null");
        this.targetArchitecture = targetArchitecture.trim();
        this.sectionHint = (sectionHint == null || sectionHint.isBlank()) ? null : sectionHint.trim();
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    /**
     * Returns the configured target architecture identifier.
     */
    public String getTargetArchitecture() {
        return targetArchitecture;
    }

    /**
     * Returns an optional section hint for assembly emission.
     */
    public Optional<String> getSectionHint() {
        return Optional.ofNullable(sectionHint);
    }

    /**
     * Returns an unmodifiable view of contextual attributes.
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }
}
