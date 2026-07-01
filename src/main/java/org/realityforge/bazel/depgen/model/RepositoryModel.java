package org.realityforge.bazel.depgen.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.realityforge.bazel.depgen.config.ChecksumPolicy;
import org.realityforge.bazel.depgen.config.RepositoryConfig;

public final class RepositoryModel {
    @Nullable
    private final RepositoryConfig _source;

    private final String _name;

    private final String _url;

    public static RepositoryModel parse(final RepositoryConfig source) {
        final String url = source.getUrl();
        if (null == url) {
            throw new InvalidModelException("The repository must specify the 'url' property.", source);
        }
        final String name = source.getName();
        final String actualName = null != name
                ? name
                : url.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+$", "").replaceAll("_+", "_");
        return new RepositoryModel(source, actualName, url);
    }

    public static RepositoryModel create(final String name, final String url) {
        return new RepositoryModel(null, name, url);
    }

    private RepositoryModel(@Nullable final RepositoryConfig source, final String name, final String url) {
        _source = source;
        _name = Objects.requireNonNull(name);
        _url = Objects.requireNonNull(url);
    }

    @Nullable
    public RepositoryConfig getSource() {
        return _source;
    }

    public String getName() {
        return _name;
    }

    public String getUrl() {
        return _url;
    }

    public boolean cacheLookups() {
        final Boolean cacheLookups = null != _source ? _source.getCacheLookups() : null;
        return cacheLookups == null ? true : cacheLookups;
    }

    public boolean searchByDefault() {
        final Boolean searchByDefault = null != _source ? _source.getSearchByDefault() : null;
        return searchByDefault == null ? true : searchByDefault;
    }

    public ChecksumPolicy checksumPolicy() {
        final ChecksumPolicy policy = null != _source ? _source.getChecksumPolicy() : null;
        return policy == null ? ChecksumPolicy.fail : policy;
    }
}
