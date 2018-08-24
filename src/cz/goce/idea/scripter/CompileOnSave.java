package cz.goce.idea.scripter;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;

/**
 * @author goce.cz
 */
public class CompileOnSave extends AnAction {

    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }

        PluginManager pluginManager = project.getComponent(PluginManager.class);

        pluginManager.setCompileOnSave(!pluginManager.isCompileOnSave());
    }

    public CompileOnSave() {
        getTemplatePresentation().setIcon(AllIcons.Actions.Compile);
    }

    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();

        Project project = event.getProject();
        if (project == null) {
            presentation.setVisible(false);
            return;
        }

        PluginManager pluginManager = project.getComponent(PluginManager.class);

        presentation.setIcon(pluginManager.isCompileOnSave() ? AllIcons.Actions.Checked : null);
    }
}
