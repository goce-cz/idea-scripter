package cz.goce.idea.scripter;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.script.ScriptException;
import javax.swing.*;
import java.lang.ref.WeakReference;
import java.util.Collections;

/**
 * @author goce.cz
 */
@SuppressWarnings("ComponentNotRegistered")
public class PluginInstallation extends AnAction {
    private final WeakReference<Project> project;
    private Plugin plugin;
    private final VirtualFile file;
    private final String actionId;

    @Override
    public void actionPerformed( AnActionEvent event ) {
        Project actionProject = this.project.get();

        if (actionProject != null && event.getProject() == actionProject) {
            PluginContext context = new PluginContext( event, file );
            ConsoleWindowFactory.prepareWindow( actionProject, consoleWindow -> {
                consoleWindow.getLogger().getSystem().println( file.getPath() );
                try {
                    plugin.execute( context, consoleWindow.getLogger() );
                } catch (Throwable e) {
                    e.printStackTrace( consoleWindow.getLogger().getErr() );
                }
            } );
        }
    }

    @Override
    public void update( AnActionEvent event ) {
        Presentation presentation = event.getPresentation();
        boolean visible = false;
        if (event.getProject() == project.get()) {
            if (plugin == null) {
                presentation.setVisible( true );
                presentation.setEnabled( false );
                return;
            }

            try {
                visible = plugin.isApplicable( new PluginContext( event, file ) );
            } catch (Throwable e) {
                visible = false;
                // TODO Log
            }
        }
        presentation.setVisible( visible );
    }

    public PluginInstallation( Project project, VirtualFile file ) {
        this.project = new WeakReference<>( project );
        this.file = file;
        this.actionId = "idea-scripter-plugin::" + file.getUrl();
        addToActions();
    }

    private void addToActions() {
        ActionManager am = ActionManager.getInstance();
        am.registerAction( actionId, this );
        DefaultActionGroup action = (DefaultActionGroup) am.getAction( "idea-scripter" );
        action.add( this );
    }

    public void removeFromActions() {
        ActionManager am = ActionManager.getInstance();
        am.unregisterAction( actionId );
        DefaultActionGroup action = (DefaultActionGroup) am.getAction( "idea-scripter" );
        action.remove( this );
    }

    public boolean isValid() {
        return plugin != null;
    }

    public boolean reloadPlugin() {
        plugin = null;


        Object pluginCandidate;
        try {
            Project project = this.project.get();
            if (project == null) {
                throw new NullPointerException( "project is no longer available" );
            }
            PluginManager pluginManager = project.getComponent( PluginManager.class );
            pluginCandidate = pluginManager.executeScript( file, null );
        } catch (ScriptException e) {
            Notifications.Bus.notify( new Notification( "error", "Failed to install IDEA scripter plugin " + file.getName(), "error on line " + e.getLineNumber() + ": \n" + e.toString(), NotificationType.ERROR ) );
            return false;
        } catch (Throwable e) {
            Notifications.Bus.notify( new Notification( "error", "Failed to install IDEA scripter plugin " + file.getName(), e.toString(), NotificationType.ERROR ) );
            return false;
        }

        if (!(pluginCandidate instanceof Plugin)) {
            Notifications.Bus.notify( new Notification( "error", "Failed to install IDEA scripter plugin " + file.getName(), "script does not return implementation of Plugin interface", NotificationType.ERROR ) );
            return false;
        }

        plugin = (Plugin) pluginCandidate;

        try {
            String name = plugin.getName();
            Icon icon = plugin.getIcon();
            String description = plugin.getDescription();

            if (name == null) {
                name = file.getName();
            }

            Presentation presentation = getTemplatePresentation();
            presentation.setText( name );
            presentation.setIcon( icon );
            presentation.setDescription( description );


        } catch (Throwable e) {
            plugin = null;
            Notifications.Bus.notify( new Notification( "error", "Failed to install IDEA scripter plugin " + file.getName(), e.toString(), NotificationType.ERROR ) );
            return false;
        }

        return true;
    }
}
