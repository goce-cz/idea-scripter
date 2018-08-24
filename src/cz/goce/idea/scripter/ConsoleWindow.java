package cz.goce.idea.scripter;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import javax.swing.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;

/**
 * @author goce.cz
 */
public class ConsoleWindow {
    private JPanel panWrapper;
    private JTextArea memLog;

    private final ConsoleView consoleView;
    private final Project project;
    private final ToolWindow toolWindow;

    private ConsoleLogger logger = new ConsoleLogger(
            new PrintWriter(new ContentWriter(ConsoleViewContentType.NORMAL_OUTPUT)),
            new PrintWriter(new ContentWriter(ConsoleViewContentType.SYSTEM_OUTPUT)),
            new PrintWriter(new ContentWriter(ConsoleViewContentType.ERROR_OUTPUT))
    );

    private final class ContentWriter extends Writer {
        private final ConsoleViewContentType contentType;

        public ContentWriter(ConsoleViewContentType contentType) {
            this.contentType = contentType;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            char[] buff = Arrays.copyOfRange(cbuf, off, len - off);
            consoleView.print(new String(buff), contentType);
        }

        @Override
        public void flush() throws IOException {

        }

        @Override
        public void close() throws IOException {

        }
    }

    public void clear() {
        consoleView.clear();
    }

    public ConsoleWindow(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;

//        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
//        Content content = contentFactory.createContent(panWrapper, "", false);
//        toolWindow.getContentManager().addContent(content);

        this.consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(consoleView.getComponent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    public ConsoleLogger getLogger() {
        return logger;
    }
}
