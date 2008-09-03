package osmedile.intellij.surrounder;

import com.intellij.codeInsight.CodeInsightActionHandler;

/**
 * @author Olivier Smedile
 * @version $Id$
 */
public class SurroundEachLinePreserveTextAction extends TemplateSurroundEachLineAction {

    protected CodeInsightActionHandler getHandler() {
        return new TemplateSurroundEachLineHandler(false);
    }
}