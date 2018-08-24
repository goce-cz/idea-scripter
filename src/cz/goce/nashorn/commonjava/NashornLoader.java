package cz.goce.nashorn.commonjava;

import jdk.internal.dynalink.beans.StaticClass;

import java.util.function.Predicate;

/**
 * @author goce.cz
 */
public class NashornLoader implements Loader {
    private final ClassLoader[] classLoaders;
    private Predicate<String> filter;

    public NashornLoader( ClassLoader... classLoaders ) {
        this.classLoaders = classLoaders;
    }

    public Predicate<String> getFilter() {
        return filter;
    }

    public void setFilter(Predicate<String> filter) {
        this.filter = filter;
    }

    @Override
    public boolean load(Module module) {
        if (!module.getId().startsWith( "nashorn/" )) {
            return false;
        }
        if (filter != null && !filter.test(module.getId())) {
            return false;
        }

        String className = module.getId().substring( 8 ).replaceAll( "/", "." );


        for (ClassLoader classLoader : classLoaders) {
            try {
                Class<?> javaClass = classLoader.loadClass( className );

                module.setExports(StaticClass.forClass(javaClass));
                return true;
            } catch (ClassNotFoundException ignored) {
                //continue
            }
        }
        return false;
    }

}