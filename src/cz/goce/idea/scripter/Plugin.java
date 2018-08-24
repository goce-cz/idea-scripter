package cz.goce.idea.scripter;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import jdk.internal.dynalink.beans.StaticClass;
import jdk.nashorn.internal.objects.NativeJava;

import javax.swing.*;
import java.io.PrintWriter;

/**
 * @author goce.cz
 */
public interface Plugin {

    default String getName() {
        return null;
    }

    default String  getDescription() {
        return null;
    }

    default Icon getIcon() {
        return AllIcons.General.Run;
    }

    default boolean isApplicable( PluginContext context ) {
        return true;
    }

    void execute( PluginContext context, ConsoleLogger console );
}