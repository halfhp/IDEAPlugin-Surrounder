package osmedile.intellij.surrounder;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.CodeInsightUtil;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.*;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Olivier Smedile
 * @version $Id: TemplateSurroundEachLineHandler.java 28 2008-09-28 18:18:46Z osmedile $
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

    private class InvokeSurroundTemplateAction extends InvokeTemplateAction {
        protected final TemplateImpl template;
        protected final Editor editor;
        protected final Project project;

        public InvokeSurroundTemplateAction(TemplateImpl templateimpl, Editor editor,
                                            Project project, Set<Character> set) {
            super(templateimpl, editor, project, set);
            this.template = templateimpl;
            this.project = project;
            this.editor = editor;
        }

        public void actionPerformed(AnActionEvent anactionevent) {
            //Modified from original SurroundWithTemplateHandler
            //Split each line of text

            final Document document = editor.getDocument();
            final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
            if (file != null) {
                ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(file);
            }

            CommandProcessor.getInstance().executeCommand(project, new Runnable() {
                public void run() {
                    editor.getCaretModel().runForEachCaret(new CaretAction() {
                        public void perform(Caret caret) {
                            // adjust the selection so that it starts with a non-whitespace character (to make sure that the template is inserted
                            // at a meaningful position rather than at indent 0)
                            if (editor.getSelectionModel().hasSelection() && template.isToReformat()) {
                                int offset = editor.getSelectionModel().getSelectionStart();
                                int selectionEnd = editor.getSelectionModel().getSelectionEnd();
                                int lineEnd = document.getLineEndOffset(document.getLineNumber(offset));
                                while (offset < lineEnd && offset < selectionEnd &&
                                        (document.getCharsSequence().charAt(offset) == ' ' || document.getCharsSequence().charAt(offset) == '\t')) {
                                    offset++;
                                }
                                // avoid extra line break after $SELECTION$ in case when selection ends with a complete line
                                if (selectionEnd == document.getLineStartOffset(document.getLineNumber(selectionEnd))) {
                                    selectionEnd--;
                                }
                                if (offset < lineEnd && offset < selectionEnd) {  // found non-WS character in first line of selection
                                    editor.getSelectionModel().setSelection(offset, selectionEnd);
                                }
                            }
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
                    });
                }
            }, "Wrap with template", "Wrap with template " + template.getKey());
        }
    }


    public TemplateSurroundEachLineHandler(boolean removedLastSemiColon) {
        this.removedLastSemiColon = removedLastSemiColon;
    }

    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psifile) {
        if (!editor.getSelectionModel().hasSelection()) {
            editor.getSelectionModel().selectLineAtCaret();
        }
//        int startOffset = editor.getSelectionModel().getSelectionStart();
//        int endOffset = editor.getSelectionModel().getSelectionEnd();

        PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());

        List<TemplateImpl> templateList;
        templateList = TemplateManagerImpl.listApplicableTemplates(psifile, editor.getCaretModel().getOffset(), true);
        //+ listApplicableCustomTemplates ?


        if (templateList.isEmpty()) {
            HintManager.getInstance().showErrorHint(editor,
                    CodeInsightBundle.message("templates.no.defined"));
            return;
        }
        if (!CodeInsightUtil.preparePsiElementsForWrite(psifile)) {
            return;
        }
//        Collections.sort(templateList, new Comparator() {
//            public int compare(TemplateImpl templateimpl2, TemplateImpl templateimpl3) {
//                return templateimpl2.getKey().compareTo(templateimpl3.getKey());
//            }
//
//            public int compare(Object obj, Object obj1) {
//                return compare((TemplateImpl) obj, (TemplateImpl) obj1);
//            }
//        }
//        );
        Set<Character> hashset = new HashSet<Character>();
        DefaultActionGroup defaultactiongroup = new DefaultActionGroup();

        for (TemplateImpl template : templateList) {
            defaultactiongroup.add(new InvokeSurroundTemplateAction(template, editor, project, hashset));
        }


        ListPopup listpopup = JBPopupFactory.getInstance()
                .createActionGroupPopup(CodeInsightBundle.message("templates.select.template.chooser.title"),
                        defaultactiongroup, DataManager.getInstance().getDataContext(
                        editor.getContentComponent()),
                        JBPopupFactory.ActionSelectionAid.MNEMONICS, false);
        listpopup.showInBestPositionFor(editor);
    }

    public boolean startInWriteAction() {
        return true;
    }
}
