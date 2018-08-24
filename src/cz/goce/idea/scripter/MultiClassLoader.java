package cz.goce.idea.scripter;

/**
 * @author goce.cz
 */
public class MultiClassLoader extends ClassLoader {
    private ClassLoader[] delegates;

    public MultiClassLoader( ClassLoader... delegates ) {
        this.delegates = delegates;
    }

    @Override
    protected Class<?> loadClass( String name, boolean resolve ) throws ClassNotFoundException {
        for (ClassLoader delegate : delegates) {
            try {
                return delegate.loadClass( name );
            } catch (ClassNotFoundException ignored) {
                // try with next
            }
        }

        throw new ClassNotFoundException( name );
    }
}
