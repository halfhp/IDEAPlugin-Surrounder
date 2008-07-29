package osmedile.intellij.surrounder;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import osmedile.intellij.surrounder.idea.BaseCodeInsightAction;

/**
 * @author Olivier Smedile
 * @version $Id$
 */
public class SurroundForEachLineAction extends BaseCodeInsightAction {


    public SurroundForEachLineAction() {
        setEnabledInModalContext(true);
    }

    protected CodeInsightActionHandler getHandler() {
        return null;
//        return new SurroundForEachLineHandler();
    }

    protected boolean isValidForFile(Project project, Editor editor,
                                     PsiFile psifile) {
        if (psifile instanceof PsiJavaFile) {
            return true;
        } else {
            Language language = psifile.getLanguage();
            return language.getSurroundDescriptors().length > 0;
        }
    }
}