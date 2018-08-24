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
public class InstallPlugin extends ScriptAction {

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
            FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(file.getName());
            if (fileType == StdFileTypes.JS) {
                pluginManager.installPlugin(file);
            }
        }
    }

    public InstallPlugin() {
        getTemplatePresentation().setIcon(AllIcons.Actions.Compile);
    }

    @Override
    protected void updatePresentation(Presentation presentation, boolean alreadyInstalled) {
        super.updatePresentation(presentation, alreadyInstalled);
        presentation.setText(alreadyInstalled ? "Compile plugin" : "Install as plugin");
    }
}
