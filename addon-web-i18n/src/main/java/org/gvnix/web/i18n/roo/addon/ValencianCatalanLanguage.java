package org.gvnix.web.i18n.roo.addon;

import java.io.InputStream;
import java.util.Locale;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.jsp.i18n.AbstractLanguage;
import org.springframework.roo.support.util.TemplateUtils;

/** 
 * Valencian/Catalan language support.
 */
@Component(immediate = true)
@Service
public class ValencianCatalanLanguage extends AbstractLanguage {

	public Locale getLocale() {
		return new Locale("ca");
	}
	
	public String getLanguage() {
		return "Valencian_Catalan";
	}

	public InputStream getFlagGraphic() {
		return TemplateUtils.getTemplate(getClass(), "ca.png");
	}

	public InputStream getMessageBundle() {
		return TemplateUtils.getTemplate(getClass(), "messages_ca.properties");
	}
}
