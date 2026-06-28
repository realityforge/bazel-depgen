load("@rules_java//java:defs.bzl", _java_binary = "java_binary", _java_library = "java_library", _java_test = "java_test")

_JAVA_JAVACOPTS = [
    "--release",
    "17",
    "-Werror",
    # Bazel's default Java toolchain enables Error Prone; the existing build only enforces javac lint.
    "-XepDisableAllChecks",
    "-Xlint:all,-processing,-serial",
]

_JAVA_TEST_JVM_FLAGS = [
    "-ea",
]

def java_library(name, srcs = [], javacopts = [], **kwargs):
    _java_library(
        name = name,
        srcs = srcs,
        javacopts = _JAVA_JAVACOPTS + javacopts,
        **kwargs
    )

def java_binary(name, srcs = [], javacopts = [], **kwargs):
    _java_binary(
        name = name,
        srcs = srcs,
        javacopts = _JAVA_JAVACOPTS + javacopts,
        **kwargs
    )

def java_test(name, srcs = [], javacopts = [], jvm_flags = [], **kwargs):
    _java_test(
        name = name,
        srcs = srcs,
        javacopts = _JAVA_JAVACOPTS + javacopts,
        jvm_flags = _JAVA_TEST_JVM_FLAGS + jvm_flags,
        **kwargs
    )
