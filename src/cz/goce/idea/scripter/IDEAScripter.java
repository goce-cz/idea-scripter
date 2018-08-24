package cz.goce.idea.scripter;

import jdk.internal.dynalink.beans.StaticClass;
import jdk.nashorn.internal.objects.NativeJava;
import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.internal.runtime.linker.JavaAdapterFactory;

import java.lang.invoke.MethodHandles;

/**
 * @author goce.cz
 */
@SuppressWarnings("unused")
public class IDEAScripter {

    public static Plugin implementPlugin( ScriptObject implementation ) {
        StaticClass pluginClass = JavaAdapterFactory.getAdapterClassFor( new Class[]{ Plugin.class}, implementation, MethodHandles.publicLookup() );
        try {
            return (Plugin) pluginClass.getRepresentedClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException( "cannot instantiate plugin", e );
        }
    }
}
