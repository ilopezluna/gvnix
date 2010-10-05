package org.gvnix.service.layer.roo.addon;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Utilities to manage annotations.
 * 
 * @author Mario Martínez Sánchez ( mmartinez at disid dot com ) at <a
 *         href="http://www.disid.com">DiSiD Technologies S.L.</a> made for <a
 *         href="http://www.cit.gva.es">Conselleria d'Infraestructures i
 *         Transport</a>
 */
@Component(immediate = true)
@Service
public class AnnotationsServiceImpl implements AnnotationsService {
    
    @Reference
    private ProjectOperations projectOperations;
    @Reference
    private JavaParserService javaParserService;
    @Reference
    private ClasspathOperations classpathOperations;

    private static Logger logger = Logger.getLogger(AnnotationsService.class
	    .getName());

    /**
     * {@inheritDoc}
     */
    public void addGvNIXAnnotationsDependency() {

	List<Element> databaseDependencies = XmlUtils.findElements(
		"/configuration/gvnix/dependencies/dependency", XmlUtils
			.getConfiguration(this.getClass(),
				"gvnix-annotation-dependencies.xml"));

	for (Element dependencyElement : databaseDependencies) {
	    
	    projectOperations
		    .dependencyUpdate(new Dependency(dependencyElement));
	}
    }

    /**
     * {@inheritDoc}
     */
    public void addJavaTypeAnnotation(JavaType serviceClass, String annotation,
	    List<AnnotationAttributeValue<?>> annotationAttributeValues,
	    boolean forceUpdate) {

	// Load class or interface details.
	// If class not found an exception will be raised.
	ClassOrInterfaceTypeDetails typeDetails = classpathOperations
		.getClassOrInterface(serviceClass);

	// Check and get mutable instance
	Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class,
		typeDetails, "Can't modify " + typeDetails.getName());
	MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) typeDetails;

	// Check annotation defined.
	// The annotation can't be updated.
	if (javaParserService.isAnnotationIntroduced(annotation,
		mutableTypeDetails)) {

	    if (forceUpdate) {
		logger.log(Level.INFO, "The annotation " + annotation
			+ " is already defined in '"
			+ serviceClass.getFullyQualifiedTypeName()
			+ "' and will be updated.");

		mutableTypeDetails
			.removeTypeAnnotation(new JavaType(annotation));
	    } else {
		logger.log(Level.INFO, "The annotation " + annotation
			+ " is already defined in '"
			+ serviceClass.getFullyQualifiedTypeName() + "'.");
		return;

	    }
	}
	    
	// Add annotation
	// If attributes are null, create an empty list to avoid
	// NullPointerException
	if (annotationAttributeValues == null) {

	    annotationAttributeValues = new ArrayList<AnnotationAttributeValue<?>>();
	}

	// Define annotation.
	AnnotationMetadata defaultAnnotationMetadata = new DefaultAnnotationMetadata(
		new JavaType(annotation), annotationAttributeValues);

	// Adds annotation to the entity
	mutableTypeDetails.addTypeAnnotation(defaultAnnotationMetadata);
    }

}
