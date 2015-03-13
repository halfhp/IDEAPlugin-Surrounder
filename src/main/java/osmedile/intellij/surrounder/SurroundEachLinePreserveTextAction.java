package osmedile.intellij.surrounder;

import com.intellij.codeInsight.CodeInsightActionHandler;

/**
 * @author Olivier Smedile
 * @version $Id: SurroundEachLinePreserveTextAction.java 13 2008-09-03 15:32:43Z osmedile $
 */
public class SurroundEachLinePreserveTextAction extends TemplateSurroundEachLineAction {

    protected CodeInsightActionHandler getHandler() {
        return new TemplateSurroundEachLineHandler(false);
    }
}