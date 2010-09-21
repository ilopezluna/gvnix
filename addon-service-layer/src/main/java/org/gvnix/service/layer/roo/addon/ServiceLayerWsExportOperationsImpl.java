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

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.*;
import org.gvnix.service.layer.roo.addon.ServiceLayerWsConfigService.CommunicationSense;
import org.gvnix.service.layer.roo.addon.annotations.GvNIXWebService;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.details.*;
import org.springframework.roo.classpath.details.annotations.*;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.*;
import org.springframework.roo.support.util.*;

/**
 * Addon for Handle Service Layer
 * 
 * @author Ricardo García ( rgarcia at disid dot com ) at <a
 *         href="http://www.disid.com">DiSiD Technologies S.L.</a> made for <a
 *         href="http://www.cit.gva.es">Conselleria d'Infraestructures i
 *         Transport</a>
 */
@Component
@Service
public class ServiceLayerWsExportOperationsImpl implements ServiceLayerWsExportOperations {

    private static Logger logger = Logger
	    .getLogger(ServiceLayerWsExportOperations.class.getName());

    @Reference
    private FileManager fileManager;
    @Reference
    private MetadataService metadataService;
    @Reference
    private PathResolver pathResolver;
    @Reference
    private ClasspathOperations classpathOperations;
    @Reference
    private ServiceLayerWsConfigService serviceLayerWsConfigService;
    @Reference
    private JavaParserService javaParserService;
    @Reference
    private AnnotationsService annotationsService;
    @Reference
    private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;
    
    /*
     * (non-Javadoc)
     * 
     * @seeorg.gvnix.service.layer.roo.addon.ServiceLayerWsExportOperations#
     * isProjectAvailable()
     */
    public boolean isProjectAvailable() {

	if (getPathResolver() == null) {

	    return false;
	}

	String webXmlPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP,
		"/WEB-INF/web.xml");
	if (!fileManager.exists(webXmlPath)) {

	    return false;
	}

	return true;
    }

    /**
     * @return the path resolver or null if there is no user project
     */
    private PathResolver getPathResolver() {

	ProjectMetadata projectMetadata = (ProjectMetadata) metadataService
		.get(ProjectMetadata.getProjectIdentifier());
	if (projectMetadata == null) {

	    return null;
	}

	return projectMetadata.getPathResolver();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * If the class to export as web service doesn't exist it will be created
     * automatically in 'src/main/java' directory inside the package defined.
     * </p>
     * 
     */
    public void exportService(JavaType serviceClass, String serviceName,
	    String portTypeName, String targetNamespace, String addressName) {

	// Checks if Cxf is configured in the project and installs it if it's
	// not available.
	serviceLayerWsConfigService.install(CommunicationSense.EXPORT);

	String fileLocation = pathResolver.getIdentifier(Path.SRC_MAIN_JAVA,
		serviceClass.getFullyQualifiedTypeName().replace('.', '/')
			.concat(".java"));

	if (!fileManager.exists(fileLocation)) {
	    logger.log(Level.INFO, "Crea la nueva clase de servicio: "
		    + serviceClass.getSimpleTypeName()
		    + " para publicarla como servicio web.");
	    // Create service class with Service Annotation.
	    javaParserService.createServiceClass(serviceClass);

	}

	// Checks serviceName parameter to publish the web service.
	serviceName = StringUtils.hasText(serviceName) ? serviceName
		: serviceClass.getSimpleTypeName();

	// Checks correct namespace format.
	checkNamespaceFormat(targetNamespace);
	Assert
		.isTrue(
			checkNamespaceFormat(targetNamespace),
			"The namespace for Target Namespace has to start with 'http://'.\ni.e.: http://name.of.namespace/");

	// Namespace for the web service.
	targetNamespace = StringUtils.hasText(targetNamespace) ? targetNamespace
		: serviceLayerWsConfigService
		    .convertPackageToTargetNamespace(serviceClass.getPackage()
			    .toString());

	// Check address name not blank and set service name if not defined.
	addressName = StringUtils.hasText(addressName) ? StringUtils
		.capitalize(addressName) : serviceClass.getSimpleTypeName();

	// Define @GvNIXWebService annotation and attributes.
	// Check port type attribute name format and add attributes to a list.
	List<AnnotationAttributeValue<?>> gvNixAnnotationAttributes = new ArrayList<AnnotationAttributeValue<?>>();
	portTypeName = StringUtils.hasText(portTypeName) ? portTypeName
		: serviceName.concat("PortType");
	gvNixAnnotationAttributes.add(new StringAttributeValue(
		new JavaSymbolName("name"), portTypeName));
	gvNixAnnotationAttributes.add(new StringAttributeValue(
		new JavaSymbolName("targetNamespace"), targetNamespace));
	gvNixAnnotationAttributes.add(new StringAttributeValue(
		new JavaSymbolName("serviceName"), serviceName));
	annotationsService.addJavaTypeAnnotation(serviceClass,
		GvNIXWebService.class.getName(), gvNixAnnotationAttributes);

	// Update CXF XML
	serviceLayerWsConfigService.exportClass(serviceClass, serviceName,
		addressName);

	// Define Jax-WS plugin and creates and execution build for this service
	// to generate the wsdl file to check errors before deploy.
	serviceLayerWsConfigService.jaxwsBuildPlugin(serviceClass, serviceName,
		addressName);

	// Add GvNixAnnotations to the project.
	annotationsService.addGvNIXAnnotationsDependency();
    }

    /**
     * {@inheritDoc}
     * 
     */
    public void exportOperation(JavaType serviceClass,
	    JavaSymbolName methodName, String operationName, String resutlName,
	    String resultNamespace, String responseWrapperName,
	    String responseWrapperNamespace, String requestWrapperName,
	    String requestWrapperNamespace) {

	Assert.notNull(serviceClass, "Java type required");
	Assert.notNull(methodName, "Operation name required");

	// Check if serviceClass is a Web Service. If doesn't exist shows an
	// error.
	if (!isWebServiceClass(serviceClass)) {
	    // Export as a service.
	    exportService(serviceClass, null, null, null, null);
	}

	// Check if method exists in the class.
	Assert.isTrue(isMethodAvailableToExport(serviceClass, methodName),
		"The method: '" + methodName + " doesn't exists in the class '"
			+ serviceClass.getFullyQualifiedTypeName() + "'.");

	// Checks correct namespace format.
	Assert
		.isTrue(
			checkNamespaceFormat(resultNamespace),
			"The namespace for result has to start with 'http://'.\ni.e.: http://name.of.namespace/");
	Assert
		.isTrue(
			checkNamespaceFormat(requestWrapperNamespace),
			"The namespace for Request Wrapper has to start with 'http://'.\ni.e.: http://name.of.namespace/");
	Assert
		.isTrue(
			checkNamespaceFormat(responseWrapperNamespace),
			"The namespace for Response Wrapper has to start with 'http://'.\ni.e.: http://name.of.namespace/");

	// Create annotations to selected Method
	List<AnnotationMetadata> annotationMetadataUpdateList = getAnnotationsToExportOperation(
		serviceClass, methodName, operationName, resutlName,
		resultNamespace, responseWrapperName, responseWrapperNamespace,
		requestWrapperName, requestWrapperNamespace);

	javaParserService.updateMethodAnnotations(serviceClass, methodName,
		annotationMetadataUpdateList);

    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * If the values are not set, define them using WS-i standard names.
     * </p>
     * <p>
     * Annotations to create:
     * </p>
     * <ul>
     * <li>@GvNIXWebMethod()</li>
     * <li>@WebMethod(operationName = "operationName", action = "", exclude =
     * false)</li>
     * <li>@RequestWrapper(localName = "requestWrapperName", targetNamespace =
     * "requestWrapperNamespace", className = "")</li>
     * <li>@ResponseWrapper(localName = "responseWrapperName", targetNamespace =
     * "responseWrapperNamespace", className = "")</li>
     * <li>@WebResult(name = "resutlName", targetNamespace = "resultNamespace",
     * header = false, partName = "parameters")</li>
     * </ul>
     */
    public List<AnnotationMetadata> getAnnotationsToExportOperation(
	    JavaType serviceClass, JavaSymbolName methodName,
	    String operationName, String resutlName, String resultNamespace,
	    String responseWrapperName, String responseWrapperNamespace,
	    String requestWrapperName, String requestWrapperNamespace) {

	List<AnnotationMetadata> annotationMetadataList = new ArrayList<AnnotationMetadata>();
	List<AnnotationAttributeValue<?>> annotationAttributeValueList;

	// org.gvnix.service.layer.roo.addon.annotations.GvNIXWebMethod
	annotationAttributeValueList = new ArrayList<AnnotationAttributeValue<?>>();
	AnnotationMetadata gvNIXWebMethod = new DefaultAnnotationMetadata(
		new JavaType(
			"org.gvnix.service.layer.roo.addon.annotations.GvNIXWebMethod"),
		annotationAttributeValueList);

	annotationMetadataList.add(gvNIXWebMethod);

	// javax.jws.WebMethod
	annotationAttributeValueList = new ArrayList<AnnotationAttributeValue<?>>();
	operationName = StringUtils.hasText(operationName) ? operationName
		: methodName.getSymbolName();

	StringAttributeValue operationNameAttributeValue = new StringAttributeValue(
		new JavaSymbolName("operationName"), operationName);
	annotationAttributeValueList.add(operationNameAttributeValue);

	StringAttributeValue actionAttribuetValue = new StringAttributeValue(
		new JavaSymbolName("action"), "");
	annotationAttributeValueList.add(actionAttribuetValue);

	BooleanAttributeValue excludeAttribuetValue = new BooleanAttributeValue(
		new JavaSymbolName("exclude"), false);
	annotationAttributeValueList.add(excludeAttribuetValue);

	AnnotationMetadata webMethod = new DefaultAnnotationMetadata(
		new JavaType("javax.jws.WebMethod"),
		annotationAttributeValueList);

	annotationMetadataList.add(webMethod);

	// javax.xml.ws.RequestWrapper
	annotationAttributeValueList = new ArrayList<AnnotationAttributeValue<?>>();

	requestWrapperName = StringUtils.hasText(requestWrapperName) ? requestWrapperName
		: operationName;
	StringAttributeValue localNameAttributeValue = new StringAttributeValue(
		new JavaSymbolName("localName"), requestWrapperName);
	annotationAttributeValueList.add(localNameAttributeValue);

	requestWrapperNamespace = StringUtils.hasText(requestWrapperNamespace) ? requestWrapperNamespace
		: serviceLayerWsConfigService
		.convertPackageToTargetNamespace(serviceClass.getPackage()
			.getFullyQualifiedPackageName());

	StringAttributeValue targetNamespaceAttributeValue = new StringAttributeValue(
		new JavaSymbolName("targetNamespace"),
		requestWrapperNamespace);
	annotationAttributeValueList.add(targetNamespaceAttributeValue);

	String className = serviceClass.getPackage()
		.getFullyQualifiedPackageName().concat(".").concat(
			StringUtils.capitalize(requestWrapperName).concat(
				"RequestWrapper"));
	StringAttributeValue classNameAttributeValue = new StringAttributeValue(
		new JavaSymbolName("className"), className);
	annotationAttributeValueList.add(classNameAttributeValue);

	AnnotationMetadata requestWrapper = new DefaultAnnotationMetadata(
		new JavaType("javax.xml.ws.RequestWrapper"),
		annotationAttributeValueList);

	annotationMetadataList.add(requestWrapper);

	// javax.xml.ws.ResponseWrapper
	annotationAttributeValueList = new ArrayList<AnnotationAttributeValue<?>>();

	responseWrapperName = StringUtils.hasText(responseWrapperName) ? responseWrapperName
		: operationName.concat("Response");

	localNameAttributeValue = new StringAttributeValue(new JavaSymbolName(
		"localName"), responseWrapperName);
	annotationAttributeValueList.add(localNameAttributeValue);

	responseWrapperNamespace = StringUtils
		.hasText(responseWrapperNamespace) ? responseWrapperNamespace
		: serviceLayerWsConfigService
			.convertPackageToTargetNamespace(serviceClass
				.getPackage().getFullyQualifiedPackageName());

	targetNamespaceAttributeValue = new StringAttributeValue(
		new JavaSymbolName("targetNamespace"), responseWrapperNamespace);
	annotationAttributeValueList.add(targetNamespaceAttributeValue);

	className = serviceClass.getPackage().getFullyQualifiedPackageName()
		.concat(".")
		.concat(StringUtils.capitalize(responseWrapperName));
	classNameAttributeValue = new StringAttributeValue(new JavaSymbolName(
		"className"), className);
	annotationAttributeValueList.add(classNameAttributeValue);

	AnnotationMetadata responseWrapper = new DefaultAnnotationMetadata(
		new JavaType("javax.xml.ws.ResponseWrapper"),
		annotationAttributeValueList);

	annotationMetadataList.add(responseWrapper);

	// javax.jws.WebResult
	// TODO: Comprobar webResult name.
	annotationAttributeValueList = new ArrayList<AnnotationAttributeValue<?>>();

	localNameAttributeValue = new StringAttributeValue(new JavaSymbolName(
		"name"), resutlName);
	annotationAttributeValueList.add(localNameAttributeValue);

	resultNamespace = StringUtils.hasText(resultNamespace) ? resultNamespace
		: serviceLayerWsConfigService
			.convertPackageToTargetNamespace(serviceClass
				.getPackage().getFullyQualifiedPackageName());

	targetNamespaceAttributeValue = new StringAttributeValue(
		new JavaSymbolName("targetNamespace"), resultNamespace);
	annotationAttributeValueList.add(targetNamespaceAttributeValue);

	BooleanAttributeValue headerAttributeValue = new BooleanAttributeValue(new JavaSymbolName("header"), false);
	annotationAttributeValueList.add(headerAttributeValue);

	StringAttributeValue partNameAttributeValue = new StringAttributeValue(
		new JavaSymbolName("partName"), "parameters");

	annotationAttributeValueList.add(partNameAttributeValue);

	AnnotationMetadata webResult = new DefaultAnnotationMetadata(
		new JavaType("javax.jws.WebResult"),
		annotationAttributeValueList);

	annotationMetadataList.add(webResult);

	return annotationMetadataList;
    }

    /**
     * Checks if the selected class exists and contains
     * {@link ServiceLayerWSExportMetadata}.
     * 
     * @param serviceClass
     *            class to be checked.
     * @return true if the {@link JavaType} contains
     *         {@link ServiceLayerWSExportMetadata}.
     */
    private boolean isWebServiceClass(JavaType serviceClass) {
	String id = physicalTypeMetadataProvider.findIdentifier(serviceClass);

	Assert.notNull(id, "Cannot locate source for '"
		+ serviceClass.getFullyQualifiedTypeName() + "'");

	// Go and get the service layer ws metadata to export selected method.
	JavaType javaType = PhysicalTypeIdentifier.getJavaType(id);
	Path path = PhysicalTypeIdentifier.getPath(id);
	String entityMid = ServiceLayerWSExportMetadata.createIdentifier(
		javaType, path);

	// Get the service layer ws metadata.
	ServiceLayerWSExportMetadata serviceLayerWSExportMetadata = (ServiceLayerWSExportMetadata) metadataService
		.get(entityMid);

	if (serviceLayerWSExportMetadata == null) {
	    return false;
	} else {
	    return true;
	}
    }

    /**
     * Check if the method methodName exists in serviceClass and is not
     * annotated before with @GvNIXWebMethod.
     * <p>
     * TODO: Check if method exists in the class.
     * </p>
     * 
     * @param serviceClass
     * @param methodName
     * @return
     */
    private boolean isMethodAvailableToExport(JavaType serviceClass,
	    JavaSymbolName methodName) {

	// Load class details. If class not found an exception will be raised.
	ClassOrInterfaceTypeDetails tmpServiceDetails = classpathOperations
		.getClassOrInterface(serviceClass);

	// Checks if it's mutable
	Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class,
		tmpServiceDetails, "Can't modify " + tmpServiceDetails.getName());

	MutableClassOrInterfaceTypeDetails serviceDetails = (MutableClassOrInterfaceTypeDetails) tmpServiceDetails;
	
	List<? extends MethodMetadata> methodList = serviceDetails.getDeclaredMethods();
	List<AnnotationMetadata> annotationMethodList;

	for (MethodMetadata methodMetadata : methodList) {
	    if (methodMetadata.getMethodName().equals(methodName)) {

		annotationMethodList = methodMetadata.getAnnotations();
		for (AnnotationMetadata annotationMetadata : annotationMethodList) {
		    Assert
			    .isTrue(
				    annotationMetadata
					    .getAnnotationType()
					    .getFullyQualifiedTypeName()
					    .compareTo(
						    "org.gvnix.service.layer.roo.addon.annotations.GvNIXWebMethod") != 0,
				    "The method '"
					    + methodName
					    + "' has been annotated with @GvNIXWebMethod before, you could update annotation parameters inside its class.");
		}
		return true;
	    }
	}

	return false;
    }

    /**
     * Checks correct namespace URI format. Suffix 'http://'.
     * 
     * <p>
     * If String is blank is also correct.
     * </p>
     * 
     * @param namespace
     *            string to check as correct namespace.
     * 
     * @return true if is blank or if has correct URI format.
     */
    private boolean checkNamespaceFormat(String namespace) {
	if (StringUtils.hasText(namespace)) {
	    return StringUtils.startsWithIgnoreCase(namespace, "http://");
	}
	return true;
    }
    
}