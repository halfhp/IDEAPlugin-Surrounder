package osmedile.intellij.surrounder;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.SimpleCodeInsightAction;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageSurrounders;
import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;

/**
 * @author Olivier Smedile
 * @version $Id: TemplateSurroundEachLineAction.java 13 2008-09-03 15:32:43Z osmedile $
 */
public class TemplateSurroundEachLineAction extends BaseCodeInsightAction {


    public TemplateSurroundEachLineAction() {
        setEnabledInModalContext(true);
    }

    protected CodeInsightActionHandler getHandler() {
        return new TemplateSurroundEachLineHandler(true);
    }
}