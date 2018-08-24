package cz.goce.idea.scripter;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author goce.cz
 */
public class UninstallPlugin extends ScriptAction {

    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }

        DataContext dataContext = event.getDataContext();
        final VirtualFile[] selectedFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);

        if (selectedFiles == null) {
            return;
        }

        PluginManager pluginManager = project.getComponent(PluginManager.class);

        for (VirtualFile file : selectedFiles) {
            pluginManager.uninstallPlugin(file);
        }
    }

    public UninstallPlugin() {
        getTemplatePresentation().setIcon(AllIcons.Actions.Delete);
    }

    @Override
    protected void updatePresentation(Presentation presentation, boolean alreadyInstalled) {
        presentation.setVisible(alreadyInstalled);
    }
}
