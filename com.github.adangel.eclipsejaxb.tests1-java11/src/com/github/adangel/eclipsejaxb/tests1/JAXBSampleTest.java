package com.github.adangel.eclipsejaxb.tests1;

import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.adangel.eclipsejaxb.core1.jaxb.JAXBSample;

public class JAXBSampleTest {

    @Test
    public void parseDoc() throws JAXBException {
        String result = JAXBSample.parseDoc();
        Assertions.assertEquals("Hello, JAXB World", result);
    }

    @Test
    public void parseDocWithThisBundleClassLoader() throws JAXBException {
        String result = JAXBSample.parseDocWithThisBundleClassLoader();
        Assertions.assertEquals("Hello, JAXB World", result);
    }

    @Test
    @Disabled("com.sun.xml.bind can't be resolved in Java8 due to javax.activation requires Java9+")
    public void parseDocWithJaxbImplBundleClassLoader() throws JAXBException {
        String result = JAXBSample.parseDocWithJaxbImplBundleClassLoader();
        Assertions.assertEquals("Hello, JAXB World", result);
    }
}
