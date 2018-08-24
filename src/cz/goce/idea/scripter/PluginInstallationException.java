package cz.goce.idea.scripter;

/**
 * @author goce.cz
 */
public class PluginInstallationException extends Exception {
    public PluginInstallationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginInstallationException(String message) {
        super(message);
    }
}
