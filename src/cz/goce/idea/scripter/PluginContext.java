package cz.goce.idea.scripter;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;

/**
 * @author goce.cz
 */
public class PluginContext {
    private final Project project;
    private final VirtualFile[] selectedFiles;
    private final PsiFile activePsiFile;
    private final Editor activeEditor;
    private final AnActionEvent event;
    private final VirtualFile pluginFile;

    public PluginContext(AnActionEvent event, VirtualFile pluginFile) {
        this.pluginFile = pluginFile;
        this.event = event;
        DataContext dataContext = event.getDataContext();
        selectedFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);

        project = event.getProject();
        if (project != null) {
            activeEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            activePsiFile = activeEditor == null ? null : PsiDocumentManager.getInstance(project).getPsiFile(activeEditor.getDocument());
        } else {
            activeEditor = null;
            activePsiFile = null;
        }
    }

    public Project getProject() {
        return project;
    }

    public VirtualFile[] getSelectedFiles() {
        return selectedFiles;
    }

    public PsiFile getActivePsiFile() {
        return activePsiFile;
    }

    public Editor getActiveEditor() {
        return activeEditor;
    }

    public AnActionEvent getEvent() {
        return event;
    }

    public VirtualFile getPluginFile() {
        return pluginFile;
    }
}
