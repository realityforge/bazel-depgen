package org.realityforge.bazel.depgen;

import java.io.IOException;
import java.util.jar.JarOutputStream;
import org.jspecify.annotations.NonNull;

public interface JarFileAction {
    void accept(@NonNull JarOutputStream outputStream) throws IOException;
}
