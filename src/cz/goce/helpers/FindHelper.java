package cz.goce.helpers;

import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.find.replaceInProject.ReplaceInProjectManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Factory;
import com.intellij.psi.PsiDirectory;
import com.intellij.usages.FindUsagesProcessPresentation;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.usages.UsageSearcher;
import com.intellij.usages.UsageViewManager;
import com.intellij.usages.UsageViewPresentation;
import com.intellij.util.AdapterProcessor;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.RejectedPromise;

/**
 * @author goce.cz
 */
public class FindHelper {

    private final Project project;
    private Promise<Void> lastTask = Promise.DONE;

    public FindHelper( Project project ) {
        this.project = project;
    }

    public static FindHelper getInstance( Project project ) {
        return ServiceManager.getService( project, FindHelper.class );
    }

    public Promise<Void> replaceInPath( FindModel findModel ) {
        final FindManager findManager = FindManager.getInstance( project );
        final PsiDirectory psiDirectory = FindInProjectUtil.getPsiDirectory( findModel, project );

        if (!findModel.isProjectScope() &&
                psiDirectory == null &&
                findModel.getModuleName() == null &&
                findModel.getCustomScope() == null) {
            return new RejectedPromise<>( new RuntimeException( "failed" ) );
        }

        UsageViewManager manager = UsageViewManager.getInstance( project );

        if (manager == null) return new RejectedPromise<>( new RuntimeException( "failed" ) );
        findManager.getFindInProjectModel().copyFrom( findModel );
        final FindModel findModelCopy = findModel.clone();

        final UsageViewPresentation presentation = FindInProjectUtil.setupViewPresentation( true, findModelCopy );
        final FindUsagesProcessPresentation processPresentation = FindInProjectUtil.setupProcessPresentation( project, true, presentation );

        AsyncPromise<Void> promise = new AsyncPromise<>();


        Factory<UsageSearcher> factory = () -> processor -> {
            try {
                FindInProjectUtil.findUsages( findModelCopy, psiDirectory, project,
                        new AdapterProcessor<>( processor, UsageInfo2UsageAdapter.CONVERTER ),
                        processPresentation );
                promise.setResult( null );
            } catch (Throwable e) {
                promise.setError( e );
            }
        };

        ReplaceInProjectManager replaceInProjectManager = ReplaceInProjectManager.getInstance( project );

        lastTask = lastTask.thenAsync( ( Void none ) -> {
            replaceInProjectManager.searchAndShowUsages( manager, factory, findModelCopy, presentation, processPresentation, findManager );
            return promise;
        } );

        return promise;
    }


/*
    public void findInPath( FindModel findModel ) {
        FindInProjectManager findInProjectManager = FindInProjectManager.getInstance( project );

        startWhen(
                () -> findInProjectManager.isEnabled() && !busy,
                () -> findInProjectManager.startFindInProject( findModel )
        );

    }*/
}
