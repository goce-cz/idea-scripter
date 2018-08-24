package cz.goce.idea.scripter;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;

import java.util.function.Function;

/**
 * @author goce.cz
 */
public class PsiIterator extends PsiRecursiveElementWalkingVisitor {
    private final Function<PsiElement, Boolean> callback;

    private PsiIterator(Function<PsiElement, Boolean> callback) {
        this.callback = callback;
    }

    @Override
    public void visitElement(PsiElement element) {
        super.visitElement(element);
        if (Boolean.FALSE.equals( callback.apply( element ) )) {
            stopWalking();
        }
    }

    public static void iterate(PsiElement root, Function<PsiElement, Boolean> callback) {
        new PsiIterator(callback).visitElement(root);
    }
}
