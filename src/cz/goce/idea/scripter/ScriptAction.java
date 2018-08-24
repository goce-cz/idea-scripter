package cz.goce.idea.scripter;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author goce.cz
 */
public abstract class ScriptAction extends AnAction {

    protected void updatePresentation(Presentation presentation, boolean alreadyInstalled) {
        presentation.setVisible(true);
    }

    @Override
    public void update(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        final VirtualFile[] selectedFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);
        final Presentation presentation = e.getPresentation();

        Project project = e.getProject();
        if (project == null) {
            presentation.setVisible(false);
            return;
        }

        PluginManager pluginManager = project.getComponent(PluginManager.class);

        boolean jsFiles = true;
        boolean allInstalled = true;
        if (selectedFiles == null) {
            jsFiles = false;
        } else {
            for (VirtualFile file : selectedFiles) {
                FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(file.getName());
                if (fileType != StdFileTypes.JS) {
                    jsFiles = false;
                    break;
                }
                if (pluginManager.getInstalledPlugin(file) == null) {
                    allInstalled = false;
                }
            }
        }
        if (jsFiles) {
            updatePresentation(presentation, allInstalled);
        } else {
            presentation.setVisible(false);
        }
    }
}
