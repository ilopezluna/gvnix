package org.gvnix.web.typicalsecurity.roo.addon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.gvnix.web.typicalsecurity.roo.addon.utils.TokenReplacementFileCopyUtils;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of commands that are available via the Roo shell.
 * 
 * @since 1.1
 */
@Component
@Service
public class TypicalsecurityOperationsImpl implements TypicalsecurityOperations {
    @Reference
    private FileManager fileManager;
    @Reference
    private PathResolver pathResolver;
    @Reference
    private ProjectOperations projectOperations;
    @Reference
    private Shell shell;

    /*
     * Not available since Roo 1.1.2 (See ROO-2066)
     * 
     * @Reference private ClasspathOperations classpathOperations;
     */

    private static char SEPARATOR = File.separatorChar;

    public boolean isCommandAvailable() {
        /*
         * Not available since Roo 1.1.2 (See ROO-2066) return getPathResolver()
         * != null && classpathOperations.isPersistentClassAvailable();
         */
        return projectOperations.isFocusedProjectAvailable();
    }

    public String setup(String entityPackage, String controllerPackage) {

        injectCaptchaDependency();
        createUserRoleEntities(entityPackage);
        createControllers(entityPackage, controllerPackage);
        injectDatabasebasedSecurity(entityPackage, controllerPackage);

        return "Done";
    }

    private void injectCaptchaDependency() {
        // <dependency>
        // <groupId>net.tanesha.recaptcha4j</groupId>
        // <artifactId>recaptcha4j</artifactId>
        // <version>0.0.7</version>
        // </dependency>
        projectOperations.addDependency(projectOperations
                .getFocusedModuleName(), new Dependency(
                "net.tanesha.recaptcha4j", "recaptcha4j", "0.0.7"));
    }

    /**
     * Create All the entities required for User, Role and User Role
     * 
     * @param entityPackage
     */
    private void createUserRoleEntities(String entityPackage) {

        // -----------------------------------------------------------------------------------
        // Create User entity
        // -----------------------------------------------------------------------------------
        shell.executeCommand("entity jpa --class ".concat(entityPackage)
                .concat(".User --testAutomatically --permitReservedWords"));
        shell.executeCommand("field string --fieldName firstName --sizeMin 1 --notNull");
        shell.executeCommand("field string --fieldName lastName --sizeMin 1 --notNull");
        shell.executeCommand("field string --fieldName emailAddress --sizeMin 1 --notNull --unique");
        shell.executeCommand("field string --fieldName password --sizeMin 1 --notNull");
        shell.executeCommand("field date --fieldName activationDate --type java.util.Date ");
        shell.executeCommand("field string --fieldName activationKey ");
        shell.executeCommand("field boolean --fieldName enabled ");
        shell.executeCommand("field boolean --fieldName locked ");

        // -----------------------------------------------------------------------------------
        // Create Role entity
        // -----------------------------------------------------------------------------------
        shell.executeCommand("entity jpa --class ".concat(entityPackage)
                .concat(".Role --testAutomatically --permitReservedWords"));
        shell.executeCommand("field string --fieldName roleName --sizeMin 1 --notNull --unique");
        shell.executeCommand("field string --fieldName roleDescription --sizeMin --sizeMax 200 --notNull");

        // -----------------------------------------------------------------------------------
        // Create User Role Mapping
        // -----------------------------------------------------------------------------------
        shell.executeCommand("entity jpa --class ".concat(entityPackage)
                .concat(".UserRole --testAutomatically"));
        shell.executeCommand("field reference --fieldName userEntry --type "
                .concat(entityPackage).concat(".User --notNull"));
        shell.executeCommand("field reference --fieldName roleEntry --type "
                .concat(entityPackage).concat(".Role --notNull"));

        // -----------------------------------------------------------------------------------
        // Create Finders for find user by email address and find user role by
        // user
        // -----------------------------------------------------------------------------------
        shell.executeCommand("finder add findUsersByEmailAddress --class "
                .concat(entityPackage).concat(".User"));
        shell.executeCommand("finder add findUsersByActivationKeyAndEmailAddress --class "
                .concat(entityPackage).concat(".User"));
        shell.executeCommand("finder add findUserRolesByUserEntry --class "
                .concat(entityPackage).concat(".UserRole"));

    }

    /**
     * Create an Controller for User, Role and UserRole
     * 
     * @param entityPackage
     * @param controllerPackage
     */
    private void createControllers(String entityPackage,
            String controllerPackage) {

        // -----------------------------------------------------------------------------------
        // Controller for User
        // -----------------------------------------------------------------------------------
        shell.executeCommand(String
                .format("web mvc scaffold --class %s.UserController --backingType %s.User",
                        controllerPackage, entityPackage));

        // -----------------------------------------------------------------------------------
        // Controller for Role
        // -----------------------------------------------------------------------------------
        shell.executeCommand(String
                .format("web mvc scaffold --class %s.RoleController --backingType %s.Role",
                        controllerPackage, entityPackage));

        // -----------------------------------------------------------------------------------
        // Controller for User Role
        // -----------------------------------------------------------------------------------
        shell.executeCommand(String
                .format("web mvc scaffold --class %s.UserRoleController --backingType %s.UserRole",
                        controllerPackage, entityPackage));
    }

    /**
     * Inject database based authentication provider in Spring Security
     * 
     * @param entityPackage
     */
    private void injectDatabasebasedSecurity(String entityPackage,
            String controllerPackage) {

        // ----------------------------------------------------------------------
        // Run Security Setup Addon
        // ----------------------------------------------------------------------
        shell.executeCommand("security setup");

        // ----------------------------------------------------------------------
        // Copy DatabaseAuthenticationProvider from template
        // ----------------------------------------------------------------------
        createAuthenticationProvider(entityPackage, controllerPackage);

        // ----------------------------------------------------------------------
        // Inject database based authentication provider into
        // applicationContext-security.xml
        // ----------------------------------------------------------------------
        injectDatabasebasedAuthProviderInXml(entityPackage);

        // ----------------------------------------------------------------------
        // Autowire MessageDigestPasswordEncoder in applicationContext.xml
        // ----------------------------------------------------------------------
        autowireMessageDigestPasswordEncoder(entityPackage);

        createMailSender();
        addForgotPasswordRegisterUserToLoginPage();
        addChangePasswordToFooter();
    }

    private void createMailSender() {
        shell.executeCommand("email sender setup --hostServer smtp.gmail.com --port 587 --protocol SMTP --username rohitsghatoltest@gmail.com --password password4me");
        shell.executeCommand("email template setup --from rohitsghatoltest@gmail.com --subject \"Password Recovery\"");
    }

    /**
     * Inject database based authentication provider into
     * applicationContext-security.xml
     * 
     * @param entityPackage
     */
    private void injectDatabasebasedAuthProviderInXml(String entityPackage) {
        String springSecurity = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_RESOURCES,
                "META-INF/spring/applicationContext-security.xml");

        MutableFile mutableConfigXml = null;
        Document webConfigDoc;
        try {
            if (fileManager.exists(springSecurity)) {
                mutableConfigXml = fileManager.updateFile(springSecurity);
                webConfigDoc = XmlUtils.getDocumentBuilder().parse(
                        mutableConfigXml.getInputStream());
            }
            else {
                throw new IllegalStateException(
                        "Could not acquire ".concat(springSecurity));
            }
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }

        Element firstInterceptUrl = XmlUtils.findFirstElementByName(
                "intercept-url", webConfigDoc.getDocumentElement());
        assert firstInterceptUrl != null : "Could not find intercept-url in "
                + springSecurity;

        firstInterceptUrl.getParentNode().insertBefore(
                new XmlElementBuilder("intercept-url", webConfigDoc)
                        .addAttribute("pattern", "/forgotpassword/**")
                        .addAttribute("access", "permitAll").build(),
                firstInterceptUrl);

        firstInterceptUrl.getParentNode().insertBefore(
                new XmlElementBuilder("intercept-url", webConfigDoc)
                        .addAttribute("pattern", "/signup/**")
                        .addAttribute("access", "permitAll").build(),
                firstInterceptUrl);

        firstInterceptUrl.getParentNode().insertBefore(
                new XmlElementBuilder("intercept-url", webConfigDoc)
                        .addAttribute("pattern", "/")
                        .addAttribute("access", "isAuthenticated()").build(),
                firstInterceptUrl);

        JavaPackage topLevelPackage = projectOperations
                .getFocusedTopLevelPackage();

        String authenticationProviderClass = topLevelPackage
                .getFullyQualifiedPackageName()
                + ".provider.DatabaseAuthenticationProvider";

        Element databaseAuthenticationProviderBean = new XmlElementBuilder(
                "beans:bean", webConfigDoc)
                .addAttribute("id", "databaseAuthenticationProvider")
                .addAttribute("class", authenticationProviderClass)
                .addChild(
                        new XmlElementBuilder("beans:property", webConfigDoc)
                                .addAttribute("name", "adminUser")
                                .addAttribute("value", "admin").build())
                .addChild(
                        new XmlElementBuilder("beans:property", webConfigDoc)
                                .addAttribute("name", "adminPassword")
                                .addAttribute("value",
                                        "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918")
                                .build()).build();

        Element authenticationManager = XmlUtils.findFirstElementByName(
                "authentication-manager", webConfigDoc.getDocumentElement());

        authenticationManager.getParentNode().insertBefore(
                databaseAuthenticationProviderBean, authenticationManager);

        Element oldAuthProvider = XmlUtils.findFirstElementByName(
                "authentication-provider", webConfigDoc.getDocumentElement());

        // <authentication-provider ref="databaseAuthenticationProvider" />

        Element newAuthProvider = new XmlElementBuilder(
                "authentication-provider", webConfigDoc).addAttribute("ref",
                "databaseAuthenticationProvider").build();

        authenticationManager.replaceChild(newAuthProvider, oldAuthProvider);

        // newAuthProvider.appendChild(
        // new XmlElementBuilder("password-encoder", webConfigDoc)
        // .addAttribute("hash", "sha-256").build());
        OutputStream out = null;
        try {
            out = mutableConfigXml.getOutputStream();
            XmlUtils.writeXml(out, webConfigDoc);
        }
        finally {
            IOUtils.closeQuietly(out);
        }

    }

    /**
     * Inject MessageDigestPasswordEncoder bean in applicationContext.xml
     * 
     * @param entityPackage
     */
    private void autowireMessageDigestPasswordEncoder(String entityPackage) {
        String applicationContextSecurity = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_RESOURCES,
                "META-INF/spring/applicationContext-security.xml");

        MutableFile mutableConfigXml = null;
        Document webConfigDoc;
        try {
            if (fileManager.exists(applicationContextSecurity)) {
                mutableConfigXml = fileManager
                        .updateFile(applicationContextSecurity);
                webConfigDoc = XmlUtils.getDocumentBuilder().parse(
                        mutableConfigXml.getInputStream());
            }
            else {
                throw new IllegalStateException("Could not acquire "
                        + applicationContextSecurity);
            }
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }

        Element messageDigestPasswordEncoder = new XmlElementBuilder(
                "beans:bean", webConfigDoc)
                .addAttribute("id", "messageDigestPasswordEncoder")
                .addAttribute(
                        "class",
                        "org.springframework.security.authentication.encoding.MessageDigestPasswordEncoder")
                .addChild(
                        new XmlElementBuilder("beans:constructor-arg",
                                webConfigDoc).addAttribute("value", "sha-256")
                                .build()).build();

        webConfigDoc.getDocumentElement().appendChild(
                messageDigestPasswordEncoder);

        XmlUtils.writeXml(mutableConfigXml.getOutputStream(), webConfigDoc);

    }

    /**
     * Copy DatabaseAuthenticationProvider from template
     * 
     * @param entityPackage
     */
    private void createAuthenticationProvider(String entityPackage,
            String controllerPackage) {

        JavaPackage topLevelPackage = projectOperations
                .getFocusedTopLevelPackage();

        final String packagePath = topLevelPackage
                .getFullyQualifiedPackageName().replace('.', SEPARATOR);

        final String finalEntityPackage = entityPackage.replace("~",
                topLevelPackage.getFullyQualifiedPackageName());

        final String finalControllerPackage = controllerPackage.replace("~",
                topLevelPackage.getFullyQualifiedPackageName());

        Properties properties = new Properties();
        properties.put("__TOP_LEVEL_PACKAGE__",
                topLevelPackage.getFullyQualifiedPackageName());
        properties.put("__ENTITY_LEVEL_PACKAGE__", finalEntityPackage);
        properties.put("__CONTROLLER_PACKAGE__", finalControllerPackage);

        Map<String, String> map = new HashMap<String, String>();

        final String controllerPath = finalControllerPackage.replace('.',
                SEPARATOR);

        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_JAVA,
                joinPath(controllerPath, "ChangePasswordController.java")),
                "ChangePasswordController.java-template");

        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_JAVA,
                joinPath(controllerPath, "ChangePasswordForm.java")),
                "ChangePasswordForm.java-template");

        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_JAVA,
                joinPath(controllerPath, "ChangePasswordValidator.java")),
                "ChangePasswordValidator.java-template");

        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_JAVA,
                joinPath(controllerPath, "SignUpController.java")),
                "SignUpController.java-template");

        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_JAVA,
                joinPath(controllerPath, "UserRegistrationForm.java")),
                "UserRegistrationForm.java-template");

        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_JAVA,
                joinPath(controllerPath, "SignUpValidator.java")),
                "SignUpValidator.java-template");

        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_JAVA,
                joinPath(controllerPath, "ForgotPasswordController.java")),
                "ForgotPasswordController.java-template");

        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_JAVA,
                joinPath(controllerPath, "ForgotPasswordForm.java")),
                "ForgotPasswordForm.java-template");

        map.put(pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_JAVA,
                joinPath(packagePath, "provider",
                        "DatabaseAuthenticationProvider.java")),
                "DatabaseAuthenticationProvider.java-template");

        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_JAVA,
                joinPath(controllerPath, "UserController.java")),
                "UserController.java-template");

        final String prefix = SEPARATOR + "WEB-INF/views";

        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP,
                joinPath(prefix, "signup", "index.jspx")), "signup/index.jspx");
        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP,
                joinPath(prefix, "signup", "thanks.jspx")),
                "signup/thanks.jspx");
        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP,
                joinPath(prefix, "signup", "error.jspx")), "signup/error.jspx");
        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP,
                joinPath(prefix, "signup", "views.xml")), "signup/views.xml");

        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP,
                joinPath(prefix, "forgotpassword", "index.jspx")),
                "forgotpassword/index.jspx");
        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP,
                joinPath(prefix, "forgotpassword", "thanks.jspx")),
                "forgotpassword/thanks.jspx");
        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP,
                joinPath(prefix, "forgotpassword", "views.xml")),
                "forgotpassword/views.xml");

        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP,
                joinPath(prefix, "changepassword", "index.jspx")),
                "changepassword/index.jspx");
        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP,
                joinPath(prefix, "changepassword", "thanks.jspx")),
                "changepassword/thanks.jspx");
        map.put(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP,
                joinPath(prefix, "changepassword", "views.xml")),
                "changepassword/views.xml");

        for (Entry<String, String> entry : map.entrySet()) {

            MutableFile mutableFile = null;

            final String path = entry.getKey();
            final String file = entry.getValue();
            InputStream ins = null;
            OutputStream outs = null;
            try {
                if (fileManager.exists(path)) {
                    mutableFile = fileManager.updateFile(path);
                }
                else {
                    mutableFile = fileManager.createFile(path);
                }

                ins = FileUtils.getInputStream(getClass(), file);
                outs = mutableFile.getOutputStream();
                TokenReplacementFileCopyUtils.replaceAndCopy(ins, outs,
                        properties);

                insertI18nMessages();
            }
            catch (IOException ioe) {
                throw new IllegalStateException(ioe);
            }
            finally {
                IOUtils.closeQuietly(ins);
                IOUtils.closeQuietly(outs);
            }
        }

    }

    /**
     * Join a list of element with {@link #SEPARATOR}
     * 
     * @param elements
     * @return
     */
    private String joinPath(String... elements) {
        return StringUtils.join(elements, SEPARATOR);
    }

    private void addChangePasswordToFooter() {
        // Look for following in footer.jspx
        // <a href="${logout}">
        // <spring:message code="security_logout"/>
        // </a>
        // and append
        // <a href="changePassword/index">Change Password</a>

        String footerJspx = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/views/footer.jspx");

        MutableFile mutableFooterJspx = null;
        Document footerJspxDoc;
        OutputStream out = null;
        try {
            if (fileManager.exists(footerJspx)) {
                mutableFooterJspx = fileManager.updateFile(footerJspx);
                footerJspxDoc = XmlUtils.getDocumentBuilder().parse(
                        mutableFooterJspx.getInputStream());
                Element logout = XmlUtils.findFirstElement(
                        "//a[@href=\"${logout}\"]",
                        footerJspxDoc.getDocumentElement());
                Validate.notNull(logout,
                        "Could not find <a href=\"${logout}\"> in "
                                .concat(footerJspx));

                logout.getParentNode().appendChild(
                        new XmlElementBuilder("div", footerJspxDoc).addChild(
                                footerJspxDoc.createTextNode("|")).build());
                String contextPath = projectOperations.getFocusedProjectName();
                logout.getParentNode().appendChild(
                        new XmlElementBuilder("a", footerJspxDoc)
                                .addAttribute(
                                        "href",
                                        "/".concat(contextPath).concat(
                                                "/changepassword/index"))
                                .addChild(
                                        footerJspxDoc
                                                .createTextNode("password"))
                                .build());
                out = mutableFooterJspx.getOutputStream();
                XmlUtils.writeXml(out, footerJspxDoc);

            }
            else {
                throw new IllegalStateException(
                        "Could not acquire ".concat(footerJspx));
            }
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
        finally {
            IOUtils.closeQuietly(out);
        }

    }

    private void addForgotPasswordRegisterUserToLoginPage() {
        // <div>
        // <a href ="/TypicalSecurity/forgotpassword/index">Forgot Password</a>
        // | Not a User Yet? <a href ="/TypicalSecurity/signup?form">Sign In</a>
        // </div>

        String loginJspx = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/views/login.jspx");

        MutableFile mutableLoginJspx = null;
        Document loginJspxDoc;
        OutputStream out = null;
        try {
            if (fileManager.exists(loginJspx)) {
                mutableLoginJspx = fileManager.updateFile(loginJspx);
                loginJspxDoc = XmlUtils.getDocumentBuilder().parse(
                        mutableLoginJspx.getInputStream());
                Element form = XmlUtils.findFirstElementByName("form",
                        loginJspxDoc.getDocumentElement());
                Validate.notNull(form,
                        "Could not find form in ".concat(loginJspx));

                String contextPath = projectOperations.getFocusedProjectName();
                form.appendChild(new XmlElementBuilder("div", loginJspxDoc)
                        .addChild(
                                loginJspxDoc
                                        .createTextNode("<br/><a href =\"/"
                                                .concat(contextPath)
                                                .concat("/forgotpassword/index\">Forgot Password</a> | Not a User Yet? <a href =\"/")
                                                .concat(contextPath)
                                                .concat("/signup?form\">Sign Up</a>")))
                        .build());
                out = mutableLoginJspx.getOutputStream();
                XmlUtils.writeXml(out, loginJspxDoc);

            }
            else {
                throw new IllegalStateException("Could not acquire "
                        + loginJspx);
            }
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
        finally {
            IOUtils.closeQuietly(out);
        }

    }

    private void insertI18nMessages() {
        String applicationProperties = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/i18n/application.properties");

        MutableFile mutableApplicationProperties = null;

        BufferedWriter out = null;
        InputStream ins = null;
        try {
            if (fileManager.exists(applicationProperties)) {
                mutableApplicationProperties = fileManager
                        .updateFile(applicationProperties);
                ins = mutableApplicationProperties.getInputStream();
                String originalData = convertStreamToString(ins);

                out = new BufferedWriter(new OutputStreamWriter(
                        mutableApplicationProperties.getOutputStream()));

                out.write(originalData);
                out.write("label_com_training_spring_roo_model_user_id=Id\n");
                out.write("label_com_training_spring_roo_model_user_lastname=Last Name\n");
                out.write("label_com_training_spring_roo_model_user_failedloginattempts=Failed\n");
                out.write("label_com_training_spring_roo_model_user_password=Password\n");
                out.write("label_com_training_spring_roo_model_userstatusmodel_failedloginattempts=Failed Login Attempts\n");
                out.write("label_com_training_spring_roo_model_user_repeat_password=Repeat Password\n");
                out.write("label_com_training_spring_roo_model_user_version=Version\n");
                out.write("label_com_training_spring_roo_model_user_firstname=First Name\n");
                out.write("label_com_training_spring_roo_model_user_plural=Users\n");
                out.write("label_com_training_spring_roo_model_user=User\n");
                out.write("label_com_training_spring_roo_model_user_enabled=Enabled\n");
                out.write("label_com_training_spring_roo_model_user_repeatpassword=Repeat Password\n");
                out.write("label_com_training_spring_roo_model_user_locked=Locked\n");
                out.write("label_com_training_spring_roo_model_user_activationkey=Activation Key\n");
                out.write("label_com_training_spring_roo_model_user_emailaddress=Email Address\n");
                out.write("label_com_training_spring_roo_model_user_activationdate=Activation Date\n");

            }
            else {
                throw new IllegalStateException(
                        "Could not acquire ".concat(applicationProperties));
            }
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
        finally {
            IOUtils.closeQuietly(ins);
            IOUtils.closeQuietly(out);
        }

    }

    private String convertStreamToString(InputStream is) throws IOException {
        /*
         * To convert the InputStream to String we use the Reader.read(char[]
         * buffer) method. We iterate until the Reader return -1 which means
         * there's no more data to read. We use the StringWriter class to
         * produce the string.
         */
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            Reader reader = new BufferedReader(new InputStreamReader(is,
                    "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
            return writer.toString();
        }
        else {
            return "";
        }
    }
}