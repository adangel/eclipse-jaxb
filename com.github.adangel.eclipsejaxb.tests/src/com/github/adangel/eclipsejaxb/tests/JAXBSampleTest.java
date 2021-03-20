package com.github.adangel.eclipsejaxb.tests;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.github.adangel.eclipsejaxb.core.jaxb.JAXBSample;

public class JAXBSampleTest {

    @Test
    public void parseDoc() throws JAXBException {
        String result = JAXBSample.parseDoc();
        Assert.assertEquals("Hello, JAXB World", result);
    }

    @Test
    public void parseDocWithThisBundleClassLoader() throws JAXBException {
        String result = JAXBSample.parseDocWithThisBundleClassLoader();
        Assert.assertEquals("Hello, JAXB World", result);
    }

    @Test
    @Ignore("With Java8, there is no bundle com.sun.xml.bind. Only with Java9+")
    public void parseDocWithJaxbImplBundleClassLoader() throws JAXBException {
        String result = JAXBSample.parseDocWithJaxbImplBundleClassLoader();
        Assert.assertEquals("Hello, JAXB World", result);
    }
}
