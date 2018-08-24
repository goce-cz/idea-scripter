package cz.goce.nashorn.commonjava;

import jdk.nashorn.api.scripting.AbstractJSObject;
import jdk.nashorn.api.scripting.ScriptUtils;
import jdk.nashorn.internal.objects.NativeArray;

import java.util.*;

/**
 * @author goce.cz
 */
public class Require extends AbstractJSObject {
    public static final Set<String> KEYS = Collections.unmodifiableSet(
            new LinkedHashSet<>(
                    Arrays.asList(
                            "main",
                            "paths"
                    )
            )
    );
    private final Module module;
    private NativeArray paths;

    public Require(Module module) {
        this.module = module;
    }


    @Override
    public Object call(Object thiz, Object... args) {
        if (args.length != 1) {
            throw new RuntimeException("single string argument is expected");
        }
        String moduleId = (String) ScriptUtils.convert(args[0], String.class);

        return this.module.getCommonJava().require(moduleId, this.module);
    }

    @Override
    public Set<String> keySet() {
        return KEYS;
    }

    @Override
    public boolean hasMember(String name) {
        return KEYS.contains(name);
    }

    @Override
    public Object getMember(String name) {
        if ("main".equals(name)) {
            return module.getMain();
        } else if ("paths".equals(name)) {
            if (paths == null) {
                paths = NativeArray.construct(true, null);
            }
            return paths;
        } else {
            return null;
        }
    }

    @Override
    public Collection<Object> values() {
        return Arrays.asList(module.getMain(), paths);
    }
}
