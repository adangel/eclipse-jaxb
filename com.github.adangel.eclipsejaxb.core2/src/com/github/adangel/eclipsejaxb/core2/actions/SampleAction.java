package com.github.adangel.eclipsejaxb.core2.actions;

import java.util.concurrent.Callable;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.github.adangel.eclipsejaxb.core2.jaxb.JAXBSample;

public class SampleAction implements IWorkbenchWindowActionDelegate {
    private IWorkbenchWindow window;

    public void run(IAction action) {
        String message = "Results:\n";
        message += capture("JAXBSample.parseDoc", JAXBSample::parseDoc);
        message += capture("JAXBSample.parseDocWithThisBundleClassLoader",
                JAXBSample::parseDocWithThisBundleClassLoader);
        message += capture("JAXBSample.parseDocWithJaxbImplBundleClassLoader",
                JAXBSample::parseDocWithJaxbImplBundleClassLoader);
        MessageDialog.openInformation(window.getShell(), "eclipse-jaxb Plug-in", message);
    }

    private String capture(String name, Callable<String> r) {
        try {
            return name + ": " + r.call() + "\n";
        } catch (Exception e) {
            return name + ": " + e.toString() + "\n";
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
    }

    public void dispose() {
    }

    public void init(IWorkbenchWindow window) {
        this.window = window;
    }
}