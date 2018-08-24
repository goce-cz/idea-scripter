package cz.goce.idea.scripter;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * @author goce.cz
 */
public class ConsoleWindowFactory implements ToolWindowFactory {
    private static final Map<Project,ConsoleWindow> instances = new WeakHashMap<Project, ConsoleWindow>();

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        ConsoleWindow consoleWindow = new ConsoleWindow(project, toolWindow);
        synchronized (instances) {
            instances.put(project, consoleWindow);
        }
    }

    public static void prepareWindow(Project project, Consumer<ConsoleWindow> windowConsumer ) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("idea-scripter-console");
        toolWindow.activate(() -> {
            ConsoleWindow consoleWindow = instances.get(project);
            consoleWindow.clear();
            windowConsumer.accept(consoleWindow);
        });
    }
}
