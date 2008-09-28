package osmedile.intellij.surrounder;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.CodeInsightUtil;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.codeInsight.template.impl.TemplateSettings;
import com.intellij.codeInsight.template.impl.Variable;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Olivier Smedile
 * @version $Id$
 */
@SuppressWarnings({"UnresolvedPropertyKey"})
public class TemplateSurroundEachLineHandler implements CodeInsightActionHandler {
    private boolean removedLastSemiColon = true;

    /**
     * True if the semi-colon at the end of the line must be removed
     */
    public boolean getRemoveEndSemicolon() {
        return removedLastSemiColon;
    }

    private class InvokeTemplateAction extends AnAction {
        public void actionPerformed(AnActionEvent anactionevent) {
            //Modified from original SurroundWithTemplateHandler
            //Split each line of text

            String selectedText = editor.getSelectionModel().getSelectedText();

            final String[] selectedLines;
            if (selectedText != null) {
                if (template.isToReformat()) {
                    selectedText = selectedText.trim();
                }
                selectedLines = selectedText.split("\n");
            } else {
                selectedLines = new String[0];
            }

            //create a new template to apply it as a global template to all lines.
            TemplateImpl t = template.copy();
            int nbLines = selectedLines.length;
            int nbVars = t.getVariableCount();

            StringBuilder sb = new StringBuilder();

            List<Variable> variables = new ArrayList<Variable>();
            for (int i = 0; i < nbVars; i++) {
                final String varName = t.getVariableNameAt(i);
                if (!"SELECTION".equals(varName) && !"END".equals(varName)) {
                    variables.add(new Variable(varName, t.getExpressionStringAt(i),
                            t.getDefaultValueStringAt(i), t.isAlwaysStopAt(i)));
                }
            }

            //Remove existant variables (like SELECTION or END);
            t.removeAllParsed();

            //Save template with all $END$ removed
            String template = t.getString().replace("$END$", "");


            for (int line = 0; line < nbLines; line++) {

                //Remove last semicolon?
                String l = selectedLines[line].trim();
                if (getRemoveEndSemicolon() && l.endsWith(";")) {
                    l = l.substring(0, l.length() - 1);
                }

                //Replace SELECTION with current line
                String currentTpl = template.replace("$SELECTION$", l);

                //Replace variable with variable specific for this line
                //For example: on first line, $VAR$ becomes $VAR_1$
                for (Variable var : variables) {
                    final String varName = var.getName();
                    final String newVarName = varName + "_" + line + "_";
                    currentTpl = currentTpl.replace("$" + varName + "$", "$" + newVarName + "$");
                    t.addVariable(newVarName, var.getExpressionString(), var.getDefaultValueString(),
                            var.isAlwaysStopAt());
                }

                sb.append(currentTpl);
                sb.append("\n");
            }
            //Set the new template
            t.setString(sb.toString());

            //Make a copy, otherwise internal variables of template are still set to the first value
            // of the template
            t = t.copy();
            t.parseSegments();

            //Apply template on string "", so current SELECTIOn will be deleted
            TemplateManager.getInstance(project).startTemplate(editor, "", t);


        }

        private TemplateImpl template;
        private Editor editor;
        private Project project;

        public InvokeTemplateAction(TemplateImpl templateimpl, Editor editor,
                                    Project project, Set<Character> set) {
            super((new StringBuilder())
                    .append(TemplateSurroundEachLineHandler.setMnemonic(templateimpl,
                            set)).append(templateimpl.getDescription()).toString());
            template = templateimpl;
            this.project = project;
            this.editor = editor;
        }
    }

    public TemplateSurroundEachLineHandler(boolean removedLastSemiColon) {
        this.removedLastSemiColon = removedLastSemiColon;
    }

    // --------------------- METHODS ---------------------

    private static String setMnemonic(TemplateImpl templateimpl, Set<Character> set) {
        String s = templateimpl.getKey();
        if (StringUtil.isEmpty(s)) {
            return "";
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!set.contains(Character.valueOf(c))) {
                set.add(c);
                return (new StringBuilder()).append(s.substring(0, i))
                        .append('\033').append(s.substring(i)).append(" ").toString();
            }
        }

        return (new StringBuilder()).append(s).append(" ").toString();
    }

    public void invoke(Project project, Editor editor, PsiFile psifile) {
        if (!editor.getSelectionModel().hasSelection()) {
            editor.getSelectionModel().selectLineAtCaret();
            if (!editor.getSelectionModel().hasSelection()) {
                return;
            }
        }
        PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
        int i = editor.getCaretModel().getOffset();
        int j = TemplateManager.getInstance(project).getContextType(psifile, i);
        TemplateImpl atemplateimpl[] = TemplateSettings.getInstance().getTemplates();
        ArrayList<TemplateImpl> arraylist = new ArrayList<TemplateImpl>();
        int k = atemplateimpl.length;
        for (int l = 0; l < k; l++) {
            TemplateImpl templateimpl = atemplateimpl[l];
            if (!templateimpl.isDeactivated() &&
                    templateimpl.getTemplateContext().isInContext(j) && templateimpl.isSelectionTemplate()) {
                arraylist.add(templateimpl);
            }
        }


        if (arraylist.isEmpty()) {
            HintManager.getInstance().showErrorHint(editor,
                    CodeInsightBundle.message("templates.no.defined", new Object[0]));
            return;
        }
        if (!CodeInsightUtil.preparePsiElementForWrite(psifile)) {
            return;
        }
        Collections.sort(arraylist, new Comparator() {
            public int compare(TemplateImpl templateimpl2, TemplateImpl templateimpl3) {
                return templateimpl2.getKey().compareTo(templateimpl3.getKey());
            }

            public int compare(Object obj, Object obj1) {
                return compare((TemplateImpl) obj, (TemplateImpl) obj1);
            }
        }
        );
        HashSet<Character> hashset = new HashSet<Character>();
        DefaultActionGroup defaultactiongroup = new DefaultActionGroup();
        TemplateImpl templateimpl1;
        for (Iterator<TemplateImpl> iterator = arraylist.iterator(); iterator.hasNext();
             defaultactiongroup.add(new InvokeTemplateAction(templateimpl1, editor, project, hashset))) {
            templateimpl1 = iterator.next();
        }

        ListPopup listpopup = JBPopupFactory.getInstance()
                .createActionGroupPopup(CodeInsightBundle.message("templates.select.template.chooser.title",
                        new Object[0]), defaultactiongroup, DataManager.getInstance().getDataContext(
                        editor.getContentComponent()),
                        JBPopupFactory.ActionSelectionAid.MNEMONICS, false);
        listpopup.showInBestPositionFor(editor);
    }

    public boolean startInWriteAction() {
        return false;
    }
}
