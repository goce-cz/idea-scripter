package cz.goce.nashorn.commonjava;

import jdk.nashorn.api.scripting.NashornScriptEngine;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author goce.cz
 */
public class JsLoader implements Loader {
    private final NashornScriptEngine scriptEngine;
    private List<JsStore> stores = new ArrayList<>();

    public JsLoader(NashornScriptEngine scriptEngine, List<JsStore> stores) {
        this.scriptEngine = scriptEngine;
        this.stores.addAll(stores);
    }

    @Override
    public boolean load(Module module) {
        for (JsStore store : stores) {
            ScriptFile file = store.open(module.getId());
            if (file != null) {
                module.setUri(file.getUri());
                SimpleScriptContext scriptContext = new SimpleScriptContext();
                Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
                module.inject(bindings);

                bindings.put("javax.script.filename", module.getUri());

                try (Reader reader = file.getReader()) {
                    scriptEngine.eval(reader, scriptContext);
                } catch (IOException | ScriptException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
        }
        return false;
    }
}
