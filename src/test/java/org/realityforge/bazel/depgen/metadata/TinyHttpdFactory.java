package org.realityforge.bazel.depgen.metadata;

import java.io.IOException;
import org.jspecify.annotations.NonNull;

final class TinyHttpdFactory {
    private TinyHttpdFactory() {}

    @NonNull
    static TinyHttpd createServer() throws IOException {
        return new TinyHttpd();
    }
}
