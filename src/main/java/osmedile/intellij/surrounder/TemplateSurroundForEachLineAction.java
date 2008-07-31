package osmedile.intellij.surrounder;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;

/**
 * @author Olivier Smedile
 * @version $Id$
 */
public class TemplateSurroundForEachLineAction extends BaseCodeInsightAction {


    public TemplateSurroundForEachLineAction() {
        setEnabledInModalContext(true);
    }

    protected CodeInsightActionHandler getHandler() {
        return new TemplateSurroundForEachLineHandler();
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