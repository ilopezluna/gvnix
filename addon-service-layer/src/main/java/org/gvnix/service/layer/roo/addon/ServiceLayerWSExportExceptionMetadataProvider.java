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
package org.gvnix.service.layer.roo.addon;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.*;
import org.gvnix.service.layer.roo.addon.ServiceLayerWsConfigService.CommunicationSense;
import org.gvnix.service.layer.roo.addon.annotations.GvNIXWebFault;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * @author Ricardo García Fernández ( rgarcia at disid dot com ) at <a
 *         href="http://www.disid.com">DiSiD Technologies S.L.</a> made for <a
 *         href="http://www.cit.gva.es">Conselleria d'Infraestructures i
 *         Transport</a>
 */
@Component(immediate = true)
@Service
public class ServiceLayerWSExportExceptionMetadataProvider extends
        AbstractItdMetadataProvider {

    @Reference
    private ServiceLayerWsConfigService serviceLayerWsConfigService;
    @Reference
    private AnnotationsService annotationsService;

    private static Logger logger = Logger
            .getLogger(ServiceLayerWSExportExceptionMetadataProvider.class
                    .getName());

    protected void activate(ComponentContext context) {
        // Ensure we're notified of all metadata related to physical Java types,
        // in particular their initial creation
        metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier
                .getMetadataIdentiferType(), getProvidesType());
        addMetadataTrigger(new JavaType(GvNIXWebFault.class.getName()));
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.springframework.roo.classpath.itd.AbstractItdMetadataProvider#
     * createLocalIdentifier(org.springframework.roo.model.JavaType,
     * org.springframework.roo.project.Path)
     */
    protected String createLocalIdentifier(JavaType javaType, Path path) {
        return ServiceLayerWSExportExceptionMetadata.createIdentifier(javaType,
                path);
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.springframework.roo.classpath.itd.AbstractItdMetadataProvider#
     * getGovernorPhysicalTypeIdentifier(java.lang.String)
     */
    protected String getGovernorPhysicalTypeIdentifier(
            String metadataIdentificationString) {
        JavaType javaType = ServiceLayerWSExportExceptionMetadata
                .getJavaType(metadataIdentificationString);
        Path path = ServiceLayerWSExportExceptionMetadata
                .getPath(metadataIdentificationString);
        String physicalTypeIdentifier = PhysicalTypeIdentifier
                .createIdentifier(javaType, path);
        return physicalTypeIdentifier;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.roo.classpath.itd.AbstractItdMetadataProvider#getMetadata
     * (java.lang.String, org.springframework.roo.model.JavaType,
     * org.springframework.roo.classpath.PhysicalTypeMetadata, java.lang.String)
     */
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            String metadataIdentificationString, JavaType aspectName,
            PhysicalTypeMetadata governorPhysicalTypeMetadata,
            String itdFilename) {

        ServiceLayerWSExportExceptionMetadata exceptionMetadata = null;

        if (serviceLayerWsConfigService.isProjectAvailable()) {

            // Install configuration to export services if it's not installed.
            serviceLayerWsConfigService.install(CommunicationSense.EXPORT);
            // Installs jax2ws plugin in project.
            serviceLayerWsConfigService.installJaxwsBuildPlugin();
            // Add GvNixAnnotations to the project.
            annotationsService.addGvNIXAnnotationsDependency();

            // Work out the MIDs of the other metadata we depend on
            JavaType javaType = ServiceLayerWSExportExceptionMetadata
                    .getJavaType(metadataIdentificationString);
            Path path = ServiceLayerWSExportExceptionMetadata
                    .getPath(metadataIdentificationString);

            exceptionMetadata = new ServiceLayerWSExportExceptionMetadata(
                    metadataIdentificationString, aspectName,
                    governorPhysicalTypeMetadata);

            if (exceptionMetadata.getItdTypeDetails().getTypeAnnotations()
                    .isEmpty()) {
                logger
                        .log(
                                Level.WARNING,
                                "The annotation @GvNIXWebFault is not declared correctly for '"
                                        + governorPhysicalTypeMetadata
                                                .getPhysicalTypeDetails()
                                                .getName()
                                                .getFullyQualifiedTypeName()
                                        + "'.\nThis will not export the Exception to be used in Web Service.\n@WebParam annotation will be deleted untill the annotation is defined correctly.");
            }
        }

        return exceptionMetadata;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.springframework.roo.classpath.itd.ItdMetadataProvider#
     * getItdUniquenessFilenameSuffix()
     */
    public String getItdUniquenessFilenameSuffix() {
        return "GvNIX_WebFault";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.roo.metadata.MetadataProvider#getProvidesType()
     */
    public String getProvidesType() {
        return ServiceLayerWSExportExceptionMetadata.getMetadataIdentiferType();
    }

}