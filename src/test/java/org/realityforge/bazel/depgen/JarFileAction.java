package org.realityforge.bazel.depgen;

import java.io.IOException;
import java.util.jar.JarOutputStream;
import javax.annotation.Nonnull;

public interface JarFileAction
{
  void accept( @Nonnull JarOutputStream outputStream )
    throws IOException;
}
