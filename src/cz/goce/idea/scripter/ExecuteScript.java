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
import jdk.nashorn.api.scripting.JSObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author goce.cz
 */
public class ExecuteScript extends ScriptAction {

    public void actionPerformed( AnActionEvent event ) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }

        DataContext dataContext = event.getDataContext();
        final VirtualFile[] selectedFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData( dataContext );

        if (selectedFiles == null) {
            return;
        }


        Map<String, Object> bindings = new HashMap<>();


        PluginManager pluginManager = project.getComponent( PluginManager.class );

        ConsoleWindowFactory.prepareWindow( project, consoleWindow -> {

            for (VirtualFile file : selectedFiles) {
                consoleWindow.getLogger().getSystem().println( file.getPath() );

                FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName( file.getName() );
                if (fileType == StdFileTypes.JS) {
                    try {
                        Object result = pluginManager.executeScript( file, bindings );
                        if (!(result instanceof JSObject)) {
                            throw new RuntimeException( "script must return a function with a following signature: function( context, log )" );
                        }

                        JSObject jsObject = (JSObject) result;
                        jsObject.call( null, new PluginContext( event, file ), consoleWindow.getLogger() );
                    } catch (Throwable e) {
                        e.printStackTrace( consoleWindow.getLogger().getErr() );
                    }
                }
            }
        } );
    }

    public ExecuteScript() {
        getTemplatePresentation().setIcon( AllIcons.General.Run );
    }

    @Override
    protected void updatePresentation( Presentation presentation, boolean alreadyInstalled ) {
        presentation.setVisible( !alreadyInstalled );
    }
}
