package cz.goce.idea.scripter;

import com.intellij.AppTopics;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

/**
 * @author goce.cz
 */
public class SaveListener extends FileDocumentManagerAdapter implements ApplicationComponent {
    private MessageBusConnection connection;

    public SaveListener() {
    }

    public void initComponent() {
        connection = ApplicationManager
                .getApplication()
                .getMessageBus()
                .connect();

        connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, this);
    }

    public void disposeComponent() {
        if (connection != null) {
            connection.disconnect();
        }
    }

    @NotNull
    public String getComponentName() {
        return "SaveListener";
    }

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
            for (Project project : openProjects) {
                PluginManager pluginManager = project.getComponent(PluginManager.class);
                if (pluginManager != null && pluginManager.isCompileOnSave()) {
                    PluginInstallation installation = pluginManager.getInstalledPlugin(file);
                    if (installation != null) {
                        boolean wasValid = installation.isValid();
                        if (installation.reloadPlugin() && !wasValid) {
                            Notifications.Bus.notify(new com.intellij.notification.Notification("ok", "IDEA scripter plugin successfully installed", file.getName(), NotificationType.INFORMATION));
                        }

                    }
                }
            }
        }, ModalityState.any());


    }
}
