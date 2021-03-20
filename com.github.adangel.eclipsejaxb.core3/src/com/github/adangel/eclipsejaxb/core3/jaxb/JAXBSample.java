package com.github.adangel.eclipsejaxb.core3.jaxb;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class JAXBSample {

    public static String parseDoc() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(RootElement.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        RootElement root = (RootElement) unmarshaller.unmarshal(
                new ByteArrayInputStream("<rootElement><message>Hello, JAXB World</message></rootElement>"
                        .getBytes(StandardCharsets.UTF_8)));
        return root.getMessage();
    }

    public static String parseDocWithThisBundleClassLoader() throws JAXBException {
        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(JAXBSample.class.getClassLoader());
            return parseDoc();
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }
    }

    public static String parseDocWithJaxbImplBundleClassLoader() throws JAXBException {
        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            Bundle thisBundle = FrameworkUtil.getBundle(JAXBSample.class);
            for (Bundle b : thisBundle.getBundleContext().getBundles()) {
                if ("com.sun.xml.bind".equals(b.getSymbolicName())) {
                    Class<?> implClass = b.loadClass("com.sun.xml.bind.v2.JAXBContextFactory");
                    Thread.currentThread().setContextClassLoader(implClass.getClassLoader());
                    return parseDoc();
                }
            }
            throw new RuntimeException("Bundle \"com.sun.xml.bind\" not found");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }
    }
}
