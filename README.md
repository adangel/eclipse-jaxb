# eclipse-jaxb

Saturday, March 20, 2021

There are already many articles about how to get JAXB working with Java11 and
a OSGI platform like Eclipse. With Java8, the JAXB (Java XML Binding) API
and implementation was part of the Java platform and "standard". With Java9
it got deprecated for removal and with Java11 it was finally removed.
JAXB API and implementation is supposed to be provided by additional libraries
now.

Here I'll try to summarize the different solutions for bringing in the
libraries into a eclipse plugin accompanied by a tycho sample project.

**In short:** The solution, that is the easiest and is backwards compatible,
is to import the packages `javax.xml.bind` and `javax.xml.bind.annotations`,
but only for Java >= 9. This is done with multi-release bundles as
described in [Java 11, JAXB and OSGI](http://www.descher.at/descher-vu/2019/01/java-11-jaxb-and-osgi/).
Additionally, one must make sure to have the following bundles installed
in the platform when running Java 11:
`jakarta.xml.bind`, `com.sun.xml.bind`, `org.glassfish.hk2.osgi-resource-locator`.
Then the plugin works without changes in a Java 8 environment and in a
Java 11 environment.
The usage of the "OSGI resource locator" was suggested at [github.com/eclipse-ee4j/jaxb-api#99](https://github.com/eclipse-ee4j/jaxb-api/issues/99#issuecomment-742146391).

## Requirements

Let's take a step back: What is the goal?

*   The plugin uses already JAXB. E.g. it just uses the following code to
    unmarshall some data:
    
    ```java
    public static String parseDoc() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(RootElement.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        RootElement root = (RootElement) unmarshaller.unmarshal(
                new ByteArrayInputStream("<rootElement><message>Hello, JAXB World</message></rootElement>"
                        .getBytes(StandardCharsets.UTF_8)));
        return root.getMessage();
    }
    ```

*   The code should not need to be changed. E.g. there should be no need
    to provide a classloader when creating the JAXBContext. It should
    just work.

*   The plugin should work in both environments: In a Java 8 environment,
    where JAXB is provided by the Java platform and in a Java 11 environment,
    where JAXB is provided by additional bundles.

## How does it work?

In Java 8, it is trivial: JAXB is on the system classpath (the system bundle
in OSGI), so it is available in every bundle. And it does not need to be
imported/required.

In Java 11, the plugin uses `Import-Package` in its `MANIFEST.MF` to
wire in the JAXB API packages. Actually in the multi-release manifest.
It compiles with Java 8, so the import package is actually only required
for runtime.

At runtime, the package `javax.xml.bind` is wired to a bundle, that
provides it. This is e.g. `jakarta.xml.bind`. The plugin doesn't depend
on this bundle, so it could be installed, but would fail to resolve when
`jakarta.xml.bind` is missing. In Eclipse 2021-03, this bundle is already
there.

Additionally a implementation for JAXB must be provided. The plugin does
depend on an implementation via `Import-Package` statements in the
multi-release manifest, namely `com.sun.xml.bind.v2`. This again will
make the plugin fail to resolve, if no bundle providing this package is
installed. In Eclipse 2021-03, the bundle `com.sun.xml.bind` is already
there.

However, this is not enough in OSGI environments. JAXBContext might
still not see the implementation classes and would fail with "Implementation of
JAXB-API has not been found on module path or classpath.". That's because
the bundle, that provides the JAXB API (jakarta.xml.bind) doesn't have the
default implementation on the classpath. But our plugin has it on the
classpath (via import package). So we could workaround this by explicitly
changing the context classloader to our plugin's classloader before
executing JAXB. That would work, but would require to change the code
of our plugin. And we might have several places, where we call JAXB.

There comes in another plugin to solve this situation: It's the "OSGI
resource locator". If this bundle (the id is `osgi-resource-locator`) is
installed, then the JAXBContext will find it a utility class from this
bundle and use that to find a matching implementation of JAXB.

Why does JAXBContext find this utility class, which is in another bundle?
Because `jakarta.xml.bind` defines a `DynamicImport-Package` header
in the manifest. See [Dynamic Imports Package](https://docs.osgi.org/specification/osgi.core/7.0.0/framework.module.html#framework.module.dynamicimportpackage).
This is kind of optional, so the API can still be used without this. But
if the bundle is installed, the package is wired and JAXB API sees the
resource locator and can use it.

Since `osgi-resource-locator` is not already there in Eclipse 2021-03,
I've added it as a explicit dependency in the feature for the plugin. This
will make sure that the resource locator is installed and the JAXB implementation
can be found.

By the way, there are two versions available of the `osgi-resource-locator`
bundle, as listed on the page [Orbit Build: R20210223232630](http://download.eclipse.org/tools/orbit/downloads/drops/R20210223232630/):
Version "1.0.3" from 2020-05-09 and "2.5.0" from 2016-11-03. Both are almost
identical. The only difference is the license: 1.0.3 uses EPL 2.0 and GPL2 with
Classpath-Exception, while 2.5.0 uses EPL 1.0 and CDDL.

For a Java 8 environment (the last Eclipse version for that is Eclipse 2020-06)
the resource locator is installed as well, but it doesn't hurt. Since the
feature doesn't mention any JAXB bundle, no JAXB implementation is installed.

**Lessons learned:**

*   Without a multi-release manifest, the import package for `javax.xml.bind`
would be also used in the Java 8 environment. It happens to be, that Eclipse
2020-06 has indeed a bundle, that provides this package. And eclipse
unfortunately wires the plugin to this bundle instead to the system bundle,
so that JAXB doesn't work. It only works, if you force eclipse to rewire
the bundles via `-clean` command line parameter. This problem doesn't occur
with the multi-release manifest.

*   In a [multi-release bundle](https://docs.osgi.org/specification/osgi.core/7.0.0/framework.module.html#framework.module-multireleasejar) only the headers
`Import-Package` and `Require-Capability` can be specified. Anything else
will be ignored. So you can't use here `Require-Bundle`. That's why
I used the feature to ensure that `osgi-resource-locator` is being installed.
That's also the reason, why I need to provide the bundles explicitly for
the tycho tests via `extraRequirements`.
I think, the tycho bug [Tycho does not seem to support multi-release bundles](https://bugs.eclipse.org/bugs/show_bug.cgi?id=542905) is not directly related.

*   The Eclipse project of the plugin uses Java 8 and doesn't use the bundle
    `jakarta.bind.xml` to resolve `javax.xml.bind`. If you get compile
    errors, make sure, you have configured a proper Java 8 runtime under
    Preferences, Java, Installed JREs.

*   Debugging tycho tests can be achieved via the property `debugPort`, e.g.
    `./mvnw clean verify -DdebugPort=8000`...

*   Debugging a full eclipse is possible by starting it like that:
    `./eclipse -data ws -vmargs -agentlib:jdwp=transport=dt_socket,address=localhost:8000,server=y`

*   Executing tycho tests with Java 8 and Java 11 is possible using toolchains
    and different target platform configurations per module.

*   Junit5 needs Java11, that's why the Java 8 tests use Junit 4.

*   Junit5 needs Junit4, at least the bundle imports `junit.runner`. That's
    why the Java 11 tests require `org.junit` as well as `org.junit.jupiter.api`.

## Alternatives

There are a lot of other possibilities:

*   The minimal solution: Just import the package `javax.xml.bind` and hope
    that it is wired to the system bundle under Java 8 and wired to `jakarta.xml.bind`
    otherwise. As described above, in Eclipse 2020-06 this didn't work
    without hickups.
    
    In the sample project, this is shown in modules "core1", "ui1", "tests1",
    "tests1-java11", and "feature1".

*   Depend on the implementation bundle `com.sun.xml.bind` as well.
    This is not backwards compatible, e.g. the plugin won't run in
    a Java 8 environment: the transitive dependency `javax.activation` needs
    Java 9 or later...
    
    Additionally, you need to fiddle with the context classloader to make JAXB
    working.
    
    In the sample project, this is shown in modules "core2", "ui2", "tests2",
    and "feature2".

*   Include the JAXB library directly in the plugin and put it on the
    `Bundle-Classpath`. This is the only solution, that works under
    any circumstances. It has the downside, that you need to ship JAR files
    within your plugin. And if you have multiple plugins (like here
    "core3" and "ui3"), you might need to export the package `javax.xml.bind`
    potentially polluting the osgi environment. I needed to do this in order
    to use the `JAXBException` in the ui module.
    
    It uses maven-dependency-plugin to download the dependencies including
    their transitive dependencies.
    
    In the sample project, this is shown in modules "core3", "ui3", tests3",
    "tests3-java11", and "feature3".

*   In order to make sure, that JAXB implementation und resource-locator
    bundles are installed, these dependencies are declared in the feature4.
    This makes it working immediately in both Java 8 and Java 11 environments.
    However, in Eclipse 2020-06 the Task View seems to be broken afterwards.
    I didn't investigate further, what the cause of this could be.
    
    In the sample project, this is shown in modules "core", "ui" and "feature4".

*   The main reason, why the JAXB API (bundle) doesn't find a implementation is,
    because it doesn't depend on it. Therefore it is not on the bundle's classpath,
    but the API still needs to find it... But it can't declare it as `Required-Bundle`
    because there are different JAXB implementations possible and you only want
    one.
    
    This is the usage of a [Fragment Bundle](https://docs.osgi.org/specification/osgi.core/7.0.0/framework.module.html#framework.module.fragmentbundles) as described in
    [Eclipse RCP, Java 11, JAXB](http://blog.vogella.com/2021/03/08/eclipse-rcp-java-11-jaxb/)
    as variant 2.
    
    The fragment module is called "jaxb-impl-binding" in the sample project
    and it extends the `Import-Package` declaration of the API `jakarta.xml.bind`
    to include the implementation package `com.sun.xml.bind.v2`. This basically
    adds the implementation to the classpath of the API bundle. Suddenly the
    API can find a implementation.
    
    In the sample project, this is shown in modules "core, "ui", "jaxb-impl-binding",
    and "feature5".

## Source code

The sample project is here: <https://github.com/adangel/eclipse-jaxb>.

jakarta.xml.bind is here: <https://github.com/eclipse-ee4j/jaxb-api>.

com.sun.xml.bind is the reference implementation of JAXB and is here:
<https://github.com/eclipse-ee4j/jaxb-ri>.

osgi-resource-locator is here: <https://github.com/eclipse-ee4j/glassfish-hk2-extra/tree/master/osgi-resource-locator>.

## References

Used websites and answers in no specific order:

*   <https://www.eclipse.org/tycho/sitedocs/tycho-surefire-plugin/test-mojo.html>
*   <http://blog.vogella.com/2021/03/08/eclipse-rcp-java-11-jaxb/>
*   <https://liferay.dev/blogs/-/blogs/osgi-module-dependencies>
*   <https://stackoverflow.com/questions/64903717/eclipse-java-11-osgi-and-jaxb>
*   <https://stackoverflow.com/questions/64979229/eclipse-osgi-java-11-jaxb-and-the-classloader>
*   <https://stackoverflow.com/questions/1043109/why-cant-jaxb-find-my-jaxb-index-when-running-inside-apache-felix>
*   <https://stackoverflow.com/questions/54632086/java-11-implementation-of-jaxb-api-has-not-been-found-on-module-path-or-classpa>
*   <https://github.com/eclipse-ee4j/jaxb-api/issues/99#issuecomment-742146391>
*   <https://www.vogella.com/tutorials/OSGi/article.html>
*   <http://www.descher.at/descher-vu/2019/01/java-11-jaxb-and-osgi/>
*   <https://www.eclipse.org/lists/equinox-dev/msg09143.html>
*   <https://docs.osgi.org/specification/osgi.core/7.0.0/framework.module.html#framework.module.fragmentbundles>
*   <https://docs.osgi.org/specification/osgi.core/7.0.0/framework.module.html#framework.module.dynamicimportpackage>
*   <https://docs.osgi.org/specification/osgi.core/7.0.0/framework.module.html#framework.module-multireleasejar>
*   <https://stackoverflow.com/questions/13454654/how-to-get-a-fragment-bundle-into-tycho-test-runtime>
*   <http://download.eclipse.org/tools/orbit/downloads/>
*   <https://git.eclipse.org/c/tycho/org.eclipse.tycho-demo.git/tree/>
*   <https://wiki.eclipse.org/Simultaneous_Release>
*   <https://github.com/takari/maven-wrapper>
*   <https://wiki.eclipse.org/Tycho/How_Tos/JUnit5>
*   <https://wiki.eclipse.org/Tycho/Testing_with_Surefire>
*   <http://maven.apache.org/guides/mini/guide-using-toolchains.html>
*   <https://www.eclipse.org/orbit/documents/RCP_Chapter20.pdf>
