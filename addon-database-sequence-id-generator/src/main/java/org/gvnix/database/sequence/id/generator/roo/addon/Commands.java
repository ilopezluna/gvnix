package org.gvnix.database.sequence.id.generator.roo.addon;

import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;
import org.springframework.roo.support.util.Assert;

/**
 * Sample of a command class. The command class is registered by the Roo shell following an
 * automatic classpath scan. You can provide simple user presentation-related logic in this
 * class. You can return any objects from each method, or use the logger directly if you'd
 * like to emit messages of different severity (and therefore different colours on 
 * non-Windows systems).
 * 
 * @since 1.1.0-M1
 */
@Component
@Service
public class Commands implements CommandMarker {
	
	private static Logger logger = Logger.getLogger(Commands.class.getName());

	@Reference private Operations operations;
	@Reference private StaticFieldConverter staticFieldConverter;

	protected void activate(ComponentContext context) {
	    staticFieldConverter.add(PropertyName.class);
    }

	protected void deactivate(ComponentContext context) {
		staticFieldConverter.remove(PropertyName.class);
	}
	
	@CliAvailabilityIndicator("welcome property")
	public boolean isPropertyAvailable() {
		return true;  // it's safe to always see the properties we expose
	}
	
	@CliCommand(value="welcome property", help="Obtains a pre-defined system property")
	public String property(@CliOption(key="name", mandatory=false, specifiedDefaultValue="USERNAME", unspecifiedDefaultValue="USERNAME", help="The property name you'd like to display") PropertyName propertyName) {
		return operations.getProperty(propertyName);
	}
	
	@CliAvailabilityIndicator("welcome write hello")
	public boolean isWriteHelloAvailable() {
		return operations.isProjectAvailable() && !operations.isTextFileAvailable("hello");
	}

	@CliCommand(value="welcome write hello", help="Writes hello.txt in the project root directory")
	public void writeHello() {
		operations.writeTextFile("hello");
	}

	@CliAvailabilityIndicator("welcome write hej")
	public boolean isWriteHejAvailable() {
		return operations.isProjectAvailable() && !operations.isTextFileAvailable("hej");
	}

	@CliCommand(value="welcome write hej", help="Writes hej.txt in the project root directory")
	public void writeHej() {
		operations.writeTextFile("hej");
	}
}