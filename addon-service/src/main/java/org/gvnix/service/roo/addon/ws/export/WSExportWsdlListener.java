/*
 * gvNIX. Spring Roo based RAD tool for Conselleria d'Infraestructures
 * i Transport - Generalitat Valenciana
 * Copyright (C) 2010 CIT - Generalitat Valenciana
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.gvnix.service.roo.addon.ws.export;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.EnumDeclaration;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.MarkerAnnotationExpr;
import japa.parser.ast.expr.NormalAnnotationExpr;
import japa.parser.ast.expr.SingleMemberAnnotationExpr;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.gvnix.service.roo.addon.ws.export.WSExportWsdlConfigService.GvNIXAnnotationType;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;

/**
 * @author Ricardo García Fernández at <a href="http://www.disid.com">DiSiD
 *         Technologies S.L.</a> made for <a
 *         href="http://www.cit.gva.es">Conselleria d'Infraestructures i
 *         Transport</a>
 */
@Component(immediate = true)
@Service
public class WSExportWsdlListener implements FileEventListener {

    static final String GENERATED_CXF_SOURCES_DIR = "target/generated-sources/cxf/server/";

    protected static Logger logger = Logger
            .getLogger(WSExportWsdlListener.class.getName());

    static final String webService = "javax.jws.WebService";
    static final String webServiceInterface = "WebService";
    static final String soapBinding = "SOAPBinding";
    static final String xmlRootElement = "XmlRootElement";
    static final String xmlAccessorType = "XmlAccessorType";
    static final String xmlType = "XmlType";
    static final String xmlEnum = "XmlEnum";
    static final String webFault = "WebFault";
    static final String PACKAGE_INFO_FILE = "package-info.java";

    /**
     * Use ProjectOperations to install new dependencies, plugins, properties,
     * etc into the project configuration
     */
    @Reference
    private ProjectOperations projectOperations;

    @Reference
    private WSExportWsdlConfigService wSExportWsdlConfigService;

    private String generatedSourcesDirectory;

    protected void activate(ComponentContext context) {
        // Reset generated file lists.
        wSExportWsdlConfigService.resetGeneratedFilesList();
    }

    /** {@inheritDoc} */
    public void onFileEvent(FileEvent fileEvent) {

        File file = fileEvent.getFileDetails().getFile();

        // nothing to do when project hasn't been created
        if (!projectOperations.isProjectAvailable()) {
            return;
        }

        if (file.getAbsolutePath().contains(getGeneratedSourcesDirectory())
                && !file.isDirectory()) {

            if ((fileEvent.getOperation().compareTo(
                    FileOperation.MONITORING_START) == 0)
                    || (fileEvent.getOperation().compareTo(
                            FileOperation.CREATED) == 0)
                    || (fileEvent.getOperation().compareTo(
                            FileOperation.UPDATED) == 0)) {

                if (file.getName().contentEquals(PACKAGE_INFO_FILE)) {
                    wSExportWsdlConfigService.setSchemaPackageInfoFile(file);
                    return;
                }

                // Parse Java file.
                CompilationUnit unit;
                try {
                    unit = JavaParser.parse(file);

                    // Get the first class or interface Java type
                    List<TypeDeclaration> types = unit.getTypes();
                    if (types != null) {
                        TypeDeclaration type = types.get(0);
                        if ((type instanceof ClassOrInterfaceDeclaration)
                                || (type instanceof EnumDeclaration)) {

                            logger.fine(fileEvent.getFileDetails().getFile()
                                    .getAbsolutePath());

                            // Get all annotations.
                            List<AnnotationExpr> annotations = type
                                    .getAnnotations();

                            // Check annotation types.
                            for (AnnotationExpr annotationExpr : annotations) {

                                if (analyzeAnnotations(file, annotationExpr,
                                        type)) {
                                    break;
                                }

                            }
                        }
                    }

                } catch (ParseException e) {
                    throw new IllegalStateException(
                            "Generated web service java file has errors:\n"
                                    + e.getMessage());

                } catch (IOException e) {
                    e.printStackTrace();
                    throw new IllegalStateException(
                            "Generated web service java file has errors:\n"
                                    + e.getMessage());

                }

            }
        }
    }

    /**
     * Check annotations from java generated classes to set file in priority
     * lists.
     * 
     * @param annotationExpr
     *            to check.
     * @param file
     *            file to Add to priority list.
     * @param type
     *            to check if Web Service class is an interface.
     * @return true if has found selected annotation.
     */
    public boolean analyzeAnnotations(File file, AnnotationExpr annotationExpr,
            TypeDeclaration type) {

        ClassOrInterfaceDeclaration classOrInterfaceDeclaration;

        if (annotationExpr instanceof NormalAnnotationExpr) {

            NormalAnnotationExpr normalAnnotationExpr = (NormalAnnotationExpr) annotationExpr;

            if (normalAnnotationExpr.getName().getName()
                    .contains(xmlRootElement)
                    || normalAnnotationExpr.getName().getName()
                            .contains(xmlAccessorType)) {

                wSExportWsdlConfigService.addFileToUpdateAnnotation(file,
                        GvNIXAnnotationType.XML_ELEMENT);
                return true;
            } else if (normalAnnotationExpr.getName().getName()
                    .contains(webFault)) {

                wSExportWsdlConfigService.addFileToUpdateAnnotation(file,
                        GvNIXAnnotationType.WEB_FAULT);
                return true;
            } else if (normalAnnotationExpr.getName().toString()
                    .contentEquals(webService)
                    || normalAnnotationExpr.getName().toString()
                            .contentEquals(webServiceInterface)) {

                classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) type;

                if (!classOrInterfaceDeclaration.isInterface()) {

                    wSExportWsdlConfigService.addFileToUpdateAnnotation(file,
                            GvNIXAnnotationType.WEB_SERVICE);
                } else {
                    wSExportWsdlConfigService.addFileToUpdateAnnotation(file,
                            GvNIXAnnotationType.WEB_SERVICE_INTERFACE);
                }

                return true;
            }

        } else if (annotationExpr instanceof MarkerAnnotationExpr) {

            MarkerAnnotationExpr markerAnnotationExpr = (MarkerAnnotationExpr) annotationExpr;

            if (markerAnnotationExpr.getName().getName()
                    .contains(xmlRootElement)
                    || markerAnnotationExpr.getName().getName()
                            .contains(xmlAccessorType)) {

                wSExportWsdlConfigService.addFileToUpdateAnnotation(file,
                        GvNIXAnnotationType.XML_ELEMENT);
                return true;
            } else if (markerAnnotationExpr.getName().getName()
                    .contains(webFault)) {

                wSExportWsdlConfigService.addFileToUpdateAnnotation(file,
                        GvNIXAnnotationType.WEB_FAULT);
                return true;
            } else if (markerAnnotationExpr.getName().toString()
                    .contains(webService)
                    || markerAnnotationExpr.getName().toString()
                            .contentEquals(webServiceInterface)) {

                classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) type;

                if (!classOrInterfaceDeclaration.isInterface()) {

                    wSExportWsdlConfigService.addFileToUpdateAnnotation(file,
                            GvNIXAnnotationType.WEB_SERVICE);
                } else {
                    wSExportWsdlConfigService.addFileToUpdateAnnotation(file,
                            GvNIXAnnotationType.WEB_SERVICE_INTERFACE);
                }

                return true;
            } else if (markerAnnotationExpr.getName().toString()
                    .contentEquals(xmlEnum)) {

                wSExportWsdlConfigService.addFileToUpdateAnnotation(file,
                        GvNIXAnnotationType.XML_ELEMENT);
                return true;
            }

        } else if (annotationExpr instanceof SingleMemberAnnotationExpr) {

            SingleMemberAnnotationExpr singleMemberAnnotationExpr = (SingleMemberAnnotationExpr) annotationExpr;

            if (singleMemberAnnotationExpr.getName().getName()
                    .contains(xmlRootElement)
                    || singleMemberAnnotationExpr.getName().getName()
                            .contains(xmlAccessorType)) {

                wSExportWsdlConfigService.addFileToUpdateAnnotation(file,
                        GvNIXAnnotationType.XML_ELEMENT);
                return true;
            } else if (singleMemberAnnotationExpr.getName().getName()
                    .contains(webFault)) {

                wSExportWsdlConfigService.addFileToUpdateAnnotation(file,
                        GvNIXAnnotationType.WEB_FAULT);
                return true;
            } else if (singleMemberAnnotationExpr.getName().getName()
                    .contains(webService)
                    || singleMemberAnnotationExpr.getName().getName()
                            .contains(webServiceInterface)) {

                classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) type;

                if (!classOrInterfaceDeclaration.isInterface()) {

                    wSExportWsdlConfigService.addFileToUpdateAnnotation(file,
                            GvNIXAnnotationType.WEB_SERVICE);
                } else {
                    wSExportWsdlConfigService.addFileToUpdateAnnotation(file,
                            GvNIXAnnotationType.WEB_SERVICE_INTERFACE);
                }

                return true;
            }
        }
        return false;
    }

    /**
     * Utility to get {@link #generatedSourcesDirectory}.
     * <p>
     * Use this method to ensure it is initialized.
     * 
     * @return
     */
    private String getGeneratedSourcesDirectory() {
        if (generatedSourcesDirectory != null) {
            return generatedSourcesDirectory;
        }
        generatedSourcesDirectory = projectOperations.getPathResolver()
                .getIdentifier(Path.ROOT, GENERATED_CXF_SOURCES_DIR);

        return generatedSourcesDirectory;
    }
}
