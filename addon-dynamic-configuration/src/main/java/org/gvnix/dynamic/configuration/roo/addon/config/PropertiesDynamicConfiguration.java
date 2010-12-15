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
package org.gvnix.dynamic.configuration.roo.addon.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.gvnix.dynamic.configuration.roo.addon.entity.DynProperty;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;

/**
 * Dynamic configuration base manager of property files.
 * <p>
 * Extends this class adding the @DynComponent annotation to manage new property
 * files.
 * </p>
 * 
 * @author Mario Martínez Sánchez ( mmartinez at disid dot com ) at <a
 *         href="http://www.disid.com">DiSiD Technologies S.L.</a> made for <a
 *         href="http://www.cit.gva.es">Conselleria d'Infraestructures i
 *         Transport</a>
 */
@Component
@Service
public class PropertiesDynamicConfiguration implements
    DefaultDynamicConfiguration {
  
  @Reference private PathResolver pathResolver;
  @Reference private FileManager fileManager;

  /* (non-Javadoc)
   * @see org.gvnix.dynamic.configuration.roo.addon.DefaultDynamicConfiguration#read()
   */
  public List<DynProperty> read() {

    // Get the properties file path from the annotation
    MutableFile file = getPropertiesFile();

    List<DynProperty> dynProperties = new ArrayList<DynProperty>();

    try {

      Properties props = new Properties();
      props.load(file.getInputStream());
      for (Entry<Object, Object> prop : props.entrySet()) {

        dynProperties.add(new DynProperty(prop.getKey().toString(), prop
            .getValue().toString()));
      }
    }
    catch (IOException ioe) {

      throw new IllegalStateException(ioe);
    }

    return dynProperties;
  }

  /* (non-Javadoc)
   * @see org.gvnix.dynamic.configuration.roo.addon.DefaultDynamicConfiguration#write(java.lang.Object)
   */
  public void write(List<DynProperty> dynProperties) {

    try {

      Properties props = new Properties();
      for (DynProperty dynProperty : dynProperties) {

        props.put(dynProperty.getKey(), dynProperty.getValue());
      }

      // Get the properties file path from the annotation
      MutableFile file = getPropertiesFile();
      props.store(file.getOutputStream(), null);
    }
    catch (IOException ioe) {

      throw new IllegalStateException(ioe);
    }
  }

  /**
   * Get the properties file path from the annotation.
   * 
   * @return Properties file
   */
  private MutableFile getPropertiesFile() {
    
    DynamicConfiguration annotation = this.getClass().getAnnotation(
        DynamicConfiguration.class);
    String path = pathResolver.getIdentifier(
        new Path(annotation.path().name()), annotation.relativePath());

    if (fileManager.exists(path)) {

      return fileManager.updateFile(path);
    }
    else {

      throw new IllegalStateException("Properties file not found");
    }
  }

}