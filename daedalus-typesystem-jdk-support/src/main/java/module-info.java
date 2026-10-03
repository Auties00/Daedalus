/**
 * Global mixins that serialize and deserialize the value types of the JDK standard library.
 *
 * <p>The mixin of a type is in the subpackage of {@code com.github.auties00.daedalus.typesystem.jdk.support}
 * that mirrors the package of the type, without its leading {@code java} segment: the
 * mixin of {@code java.time.Instant} is {@code time.InstantMixin}, and the one of
 * {@code javax.crypto.SecretKey} is {@code javax.crypto.SecretKeyMixin}.
 *
 * <p>The mixins that support the types of a module other than {@code java.base} only
 * require it statically, so that an application only needs that module if it uses those
 * types.
 */
module com.github.auties00.daedalus.typesystem.jdk.support {
    requires transitive com.github.auties00.daedalus.typesystem;
    requires static java.logging;
    requires static java.management;
    requires static java.naming;
    requires static java.security.jgss;
    requires static java.sql;
    requires static java.xml;

    exports com.github.auties00.daedalus.typesystem.jdk.support.io;
    exports com.github.auties00.daedalus.typesystem.jdk.support.javax.crypto;
    exports com.github.auties00.daedalus.typesystem.jdk.support.javax.crypto.spec;
    exports com.github.auties00.daedalus.typesystem.jdk.support.javax.management;
    exports com.github.auties00.daedalus.typesystem.jdk.support.javax.management.remote;
    exports com.github.auties00.daedalus.typesystem.jdk.support.javax.naming;
    exports com.github.auties00.daedalus.typesystem.jdk.support.javax.naming.ldap;
    exports com.github.auties00.daedalus.typesystem.jdk.support.javax.security.auth.kerberos;
    exports com.github.auties00.daedalus.typesystem.jdk.support.javax.security.auth.x500;
    exports com.github.auties00.daedalus.typesystem.jdk.support.javax.xml.namespace;
    exports com.github.auties00.daedalus.typesystem.jdk.support.lang;
    exports com.github.auties00.daedalus.typesystem.jdk.support.lang.constant;
    exports com.github.auties00.daedalus.typesystem.jdk.support.lang.module;
    exports com.github.auties00.daedalus.typesystem.jdk.support.math;
    exports com.github.auties00.daedalus.typesystem.jdk.support.net;
    exports com.github.auties00.daedalus.typesystem.jdk.support.nio;
    exports com.github.auties00.daedalus.typesystem.jdk.support.nio.charset;
    exports com.github.auties00.daedalus.typesystem.jdk.support.nio.file;
    exports com.github.auties00.daedalus.typesystem.jdk.support.nio.file.attribute;
    exports com.github.auties00.daedalus.typesystem.jdk.support.org.ietf.jgss;
    exports com.github.auties00.daedalus.typesystem.jdk.support.security;
    exports com.github.auties00.daedalus.typesystem.jdk.support.security.cert;
    exports com.github.auties00.daedalus.typesystem.jdk.support.security.spec;
    exports com.github.auties00.daedalus.typesystem.jdk.support.sql;
    exports com.github.auties00.daedalus.typesystem.jdk.support.time;
    exports com.github.auties00.daedalus.typesystem.jdk.support.util;
    exports com.github.auties00.daedalus.typesystem.jdk.support.util.concurrent;
    exports com.github.auties00.daedalus.typesystem.jdk.support.util.concurrent.atomic;
    exports com.github.auties00.daedalus.typesystem.jdk.support.util.logging;
    exports com.github.auties00.daedalus.typesystem.jdk.support.util.regex;
}
