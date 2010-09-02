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

import java.util.logging.Logger;

import org.apache.felix.scr.annotations.*;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.*;

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
public class ServiceLayerCommands implements CommandMarker {

    private static Logger logger = Logger
	    .getLogger(ServiceLayerCommands.class.getName());

    @Reference
    private ServiceLayerOperations serviceLayerOperations;

    @Reference
    private WebServiceLayerOperations webServiceLayerOperations;

    @CliAvailabilityIndicator("service class")
    public boolean isCreateServiceClassAvailable() {
	return serviceLayerOperations.isProjectAvailable();
    }

    @CliCommand(value = "service class", help = "Creates a Service class int the project.")
    public void createServiceClass(
	    @CliOption(key = "class", mandatory = true, help = "Name of the service class to create") JavaType serviceClass) {
	serviceLayerOperations.createServiceClass(serviceClass);
    }

    @CliAvailabilityIndicator("service export ws")
    public boolean isServiceExportAvailable() {
	return webServiceLayerOperations.isProjectAvailable();
    }

    @CliCommand(value = "service export ws", help = "Exports a Service class to Web Service. If the class doesn't exists the Addon will create it.")
    public void serviceExport(
	    @CliOption(key = "class", mandatory = true, help = "Name of the service class to export or create") JavaType serviceClass) {
	webServiceLayerOperations.exportService(serviceClass);
    }

    @CliAvailabilityIndicator("service export operation")
    public boolean isServiceOperationAvailable() {
	return webServiceLayerOperations.isProjectAvailable()
		&& webServiceLayerOperations.isCxfInstalled();
    }

    @CliCommand(value = "service export operation", help = "Publish a method as Web Service Operation.")
    public String serviceOperation() {
	return "Web Service Operation published.";
    }

}