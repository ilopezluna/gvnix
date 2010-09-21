package org.gvnix.service.layer.roo.addon;

import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.model.JavaType;

/**
 * Utilities to manage annotations.
 * 
 * @author Mario Martínez Sánchez ( mmartinez at disid dot com ) at <a
 *         href="http://www.disid.com">DiSiD Technologies S.L.</a> made for <a
 *         href="http://www.cit.gva.es">Conselleria d'Infraestructures i
 *         Transport</a>
 */
public interface AnnotationsService {

    /**
     * Add GvNIX annotations dependency to the current project.
     */
    public void addGvNIXAnnotationsDependency();

    /**
     * Add an annotation to a JavaType with some attributes.
     * 
     * <p>
     * If annotation already assined on class, message will be raised.
     * </p>
     * 
     * @param serviceClass
     *            Java type to add de annotation
     * @param annotation
     *            Annotation class full name, null if not
     * @param annotationAttributeValues
     *            Attribute list for the annotation
     */
    public void addJavaTypeAnnotation(JavaType serviceClass, String annotation,
	    List<AnnotationAttributeValue<?>> annotationAttributeValues);

}