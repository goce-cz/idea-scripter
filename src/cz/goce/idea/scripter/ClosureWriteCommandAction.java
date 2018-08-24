package cz.goce.idea.scripter;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

/**
 * @author goce.cz
 */
public class ClosureWriteCommandAction extends WriteCommandAction.Simple<Object> {
    private Runnable runnable;

    public ClosureWriteCommandAction( Project project, PsiFile[] files, Runnable runnable ) {
        super( project, files );
        this.runnable = runnable;
    }

    @Override
    protected void run() throws Throwable {
        runnable.run();
    }
}
