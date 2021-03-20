package com.github.adangel.eclipsejaxb.tests3;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.github.adangel.eclipsejaxb.core3.jaxb.JAXBSample;

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
    @Ignore("the bundle com.sun.xml.bind is not installed")
    public void parseDocWithJaxbImplBundleClassLoader() throws JAXBException {
        String result = JAXBSample.parseDocWithJaxbImplBundleClassLoader();
        Assert.assertEquals("Hello, JAXB World", result);
    }
}
