package cz.goce.idea.scripter;

import com.intellij.lang.javascript.psi.JSVariable;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import cz.goce.nashorn.commonjava.CommonJava;
import cz.goce.nashorn.commonjava.FileSystemStore;
import cz.goce.nashorn.commonjava.JsLoader;
import cz.goce.nashorn.commonjava.NashornLoader;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.swing.event.HyperlinkEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author goce.cz
 */

@State(
        name = "pluginManager",
        storages = @Storage(
                id = "dir",
                file = StoragePathMacros.PROJECT_CONFIG_DIR + "/ideaScripter.xml",
                scheme = StorageScheme.DIRECTORY_BASED
        )
)
public class PluginManager extends AbstractProjectComponent implements PersistentStateComponent<PluginManager.State> {
    public static final MultiClassLoader MULTI_CLASS_LOADER = new MultiClassLoader( ExecuteScript.class.getClassLoader(), JSVariable.class.getClassLoader() );
    public static final NashornScriptEngineFactory SCRIPT_ENGINE_FACTORY = new NashornScriptEngineFactory();
    private boolean opening = true;

    public PluginManager( Project project ) {
        super( project );
    }

    public static class State {
        public List<String> installedPlugins = new ArrayList<>();
        public boolean compileOnSave;
    }

    private State state = new State();

    private Map<VirtualFile, PluginInstallation> installations = new HashMap<>();

    public void initComponent() {
//        watcher = new DelayedDocumentWatcher(myProject, 500,
//                (Consumer<Set<VirtualFile>>) files -> {
//                    watcher.
//                },
//                installations::containsKey
//        );
        // TODO: insert component initialization logic here
    }

    public void disposeComponent() {
        clearInstallations();
    }

    @NotNull
    public String getComponentName() {
        return "PluginManager";
    }

    @Override
    public void projectOpened() {
        opening = false;
        loadState( state );
    }

    private void clearInstallations() {
        for (PluginInstallation installation : installations.values()) {
            installation.removeFromActions();
        }
        installations.clear();
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState( State state ) {
        this.state = state;

        if (!opening) {
            clearInstallations();

            List<String> missingFiles = new ArrayList<>();

            for (String path : state.installedPlugins) {
                VirtualFile pluginFile = myProject.getBaseDir().findFileByRelativePath( path );
                if (pluginFile == null) {
                    missingFiles.add( path );
                } else {
                    PluginInstallation pluginInstallation = new PluginInstallation( myProject, pluginFile );
                    pluginInstallation.reloadPlugin();
                    installations.put( pluginFile, pluginInstallation );
                }
            }

            if (!missingFiles.isEmpty()) {
                Notifications.Bus.notify( new Notification( "error", "Following IDEA scripter plugins were not found", StringEscapeUtils.escapeHtml( StringUtils.join( missingFiles, "<br/>\n" ) ) + "<br/>\n<a href=\"uninstall\">uninstall missing plugins</a>", NotificationType.ERROR, new NotificationListener.Adapter() {
                    @Override
                    protected void hyperlinkActivated( @NotNull Notification notification, @NotNull HyperlinkEvent e ) {
                        state.installedPlugins.removeAll( missingFiles );
                        notification.hideBalloon();
                    }
                } ) );
            }
        }
    }

    public Object executeScript( VirtualFile file, Map<String, Object> bindings ) throws
            IOException, ScriptException {
        NashornScriptEngine scriptEngine = (NashornScriptEngine) SCRIPT_ENGINE_FACTORY.getScriptEngine( MULTI_CLASS_LOADER );

        CommonJava.inject( scriptEngine, file.getNameWithoutExtension(),
                Arrays.asList(
                        new NashornLoader( PluginManager.class.getClassLoader(), PsiFile.class.getClassLoader(), JSVariable.class.getClassLoader() ),
                        new JsLoader( scriptEngine,
                                Arrays.asList(
                                        new FileSystemStore( Paths.get( file.getParent().getPath() ), StandardCharsets.UTF_8 )
                                ) )
                )
        );

        scriptEngine.put( "javax.script.filename", file.getCanonicalPath() );
        if (bindings != null) {
            Bindings currentBindings = scriptEngine.getBindings( ScriptContext.ENGINE_SCOPE );
            currentBindings.putAll( bindings );
        }
        Document document = FileDocumentManager.getInstance().getDocument( file );
        if (document != null) {
            return scriptEngine.eval( document.getText() );
        } else {
            try (InputStream stream = file.getInputStream()) {
                try (Reader reader = new InputStreamReader( stream, file.getCharset() )) {
                    return scriptEngine.eval( reader );
                }
            }
        }
    }

    private String toRelativePath( VirtualFile file ) {
        String path = file.getPath();
        VirtualFile baseDir = myProject.getBaseDir();
        String projectPath = baseDir == null ? null : baseDir.getPath();
        if (projectPath == null || !path.startsWith( projectPath )) {
            return null;
        }
        return path.substring( projectPath.length()+1 );
    }

    public PluginInstallation installPlugin( VirtualFile file ) {

        PluginInstallation installation = installations.get( file );
        boolean isNew = installation == null;
        if (installation == null) {
            installation = new PluginInstallation( myProject, file );
            installations.put( file, installation );
        }

        boolean wasValid = installation.isValid();
        boolean valid = installation.reloadPlugin();

        if (valid && isNew) {
            String relativePath = toRelativePath( file );
            if (relativePath == null) {
                Notifications.Bus.notify( new Notification( "warn", "IDEA scripter plugin installed for current session only: " + file.getName(), "The source file of the plugin is not within current project directory.", NotificationType.WARNING ) );
                return installation;
            }
            if (!state.installedPlugins.contains( relativePath )) {
                state.installedPlugins.add( relativePath );
            }
        }

        if (valid && !wasValid) {
            Notifications.Bus.notify( new Notification( "ok", "IDEA scripter plugin successfully installed", file.getName(), NotificationType.INFORMATION ) );
        }

        return installation;
    }

    public PluginInstallation getInstalledPlugin( VirtualFile file ) {
        return installations.get( file );
    }

    public boolean uninstallPlugin( VirtualFile file ) {
        PluginInstallation installation = getInstalledPlugin( file );
        boolean wasInstalled = false;
        if (installation != null) {
            installation.removeFromActions();
            installations.remove( file );
            wasInstalled = true;
        }

        String relativePath = toRelativePath( file );
        wasInstalled = state.installedPlugins.remove( relativePath ) || wasInstalled;
        if (wasInstalled) {
            Notifications.Bus.notify( new Notification( "ok", "IDEA scripter plugin successfully uninstalled", file.getName(), NotificationType.INFORMATION ) );
        }
        return wasInstalled;
    }

    public void setCompileOnSave( boolean value ) {
        this.state.compileOnSave = value;
    }

    public boolean isCompileOnSave() {
        return this.state.compileOnSave;
    }
}
