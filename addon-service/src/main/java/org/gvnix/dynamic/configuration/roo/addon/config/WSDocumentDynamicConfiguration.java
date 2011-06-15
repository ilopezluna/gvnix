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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.gvnix.dynamic.configuration.roo.addon.entity.DynProperty;
import org.gvnix.dynamic.configuration.roo.addon.entity.DynPropertyList;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Dynamic configuration manager of document type web services.
 * 
 * @author Mario Martínez Sánchez ( mmartinez at disid dot com ) at <a
 *         href="http://www.disid.com">DiSiD Technologies S.L.</a> made for <a
 *         href="http://www.cit.gva.es">Conselleria d'Infraestructures i
 *         Transport</a>
 */
@Component
@Service
public class WSDocumentDynamicConfiguration extends XmlDynamicConfiguration
        implements DefaultDynamicConfiguration {

    private static final Logger logger = HandlerUtils
            .getLogger(WSDocumentDynamicConfiguration.class);

    /**
     * {@inheritDoc}
     */
    public String getName() {

        return "Document Type Web Services WSDL";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFilePath() {

        return "pom.xml";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DynPropertyList read() {

        DynPropertyList dynProps = new DynPropertyList();

        // Get the XML file path
        MutableFile file = getFile();

        // If managed file not exists, nothing to do
        if (file != null) {

            // Obtain the XML file on DOM document format
            Document doc = getXmlDocument(file);

            List<Element> elems = XmlUtils.findElements(getSourceXpath(),
                    doc.getDocumentElement());
            for (Element elem : elems) {

                Element id = XmlUtils.findFirstElement(getKey(), elem);
                Element wsdl = XmlUtils
                        .findFirstElement(getTargetXpath(), elem);
                if (id == null || wsdl == null) {

                    logger.log(Level.WARNING,
                            "Element identifier or wsdl location not exists");
                    continue;
                }

                dynProps.add(new DynProperty(id.getTextContent(), wsdl
                        .getTextContent()));
            }
        }

        return dynProps;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(DynPropertyList dynProps) {

        // Get the XML file path
        MutableFile file = getFile();
        if (file != null) {

            // Obtain the root element of the XML file
            Document doc = getXmlDocument(file);
            Element root = doc.getDocumentElement();

            // Update the root element property values with dynamic properties
            for (DynProperty dynProp : dynProps) {

                String xpath = getSourceXpath() + XPATH_ARRAY_PREFIX + getKey()
                        + XPATH_EQUALS_SYMBOL + XPATH_STRING_DELIMITER
                        + dynProp.getKey() + XPATH_STRING_DELIMITER
                        + XPATH_ARRAY_SUFIX;
                Element elem = XmlUtils.findFirstElement(xpath, root);
                if (elem == null) {

                    logger.log(Level.WARNING, "Element " + xpath
                            + " to set value not exists on file");
                } else {

                    Element wsdl = XmlUtils.findFirstElement(getTargetXpath(),
                            elem);
                    if (wsdl == null) {

                        logger.log(Level.WARNING, "Element  " + xpath
                                + " to set value not exists on file");
                    } else {

                        wsdl.setTextContent(dynProp.getValue());
                    }
                }
            }

            // Update the XML file
            XmlUtils.writeXml(file.getOutputStream(), doc);

        } else if (!dynProps.isEmpty()) {

            logger.log(Level.WARNING, "File " + getFilePath()
                    + " not exists and there are dynamic properties to set it");
        }
    }

    protected String getSourceXpath() {

        return "/project/build/plugins/plugin"
                + "[groupId='org.apache.cxf' and artifactId='cxf-codegen-plugin']/"
                + "executions/execution";
    }

    protected String getKey() {

        return "id";
    }

    protected String getTargetXpath() {

        return "configuration/wsdlOptions/wsdlOption/wsdl";
    }

}