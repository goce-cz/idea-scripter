package cz.goce.nashorn.commonjava;

import jdk.nashorn.api.scripting.NashornScriptEngine;

import javax.script.Bindings;
import javax.script.ScriptContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author goce.cz
 */
public class CommonJava {

    private final NashornScriptEngine engine;

    private final List<Loader> loaders = new ArrayList<>();

    private final Module main;
    private final Map<String, Module> modules = new HashMap<>();

    private CommonJava(NashornScriptEngine engine, String mainModuleId, List<Loader> loaders) {
        this.engine = engine;
        this.loaders.addAll(loaders);
        this.main = new Module(this, mainModuleId, null);
        this.main.setUri("main:main");
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        main.inject(bindings);
        modules.put(main.getId(), main);
    }

    public static void inject(NashornScriptEngine engine, String mainModuleId, List<Loader> loaders) {
        new CommonJava(engine, mainModuleId, loaders);
    }


    public Object require(String moduleId, Module parent) {

        String absoluteModuleId = parent.normalize(moduleId);

        Module module = modules.get(absoluteModuleId);
        if (module == null) {
            module = new Module(this, absoluteModuleId, parent);
            modules.put(module.getId(), module);
            for (Loader loader : loaders) {
                if (loader.load(module)) {
                    return module.getExports();
                }
            }
            throw new RuntimeException("cannot find dependency: " + absoluteModuleId);
        }

        return module.getExports();
    }

    public NashornScriptEngine getEngine() {
        return engine;
    }
}
