package cz.goce.nashorn.commonjava;

import jdk.nashorn.api.scripting.AbstractJSObject;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author goce.cz
 */
public class Module extends AbstractJSObject {
    public static final Set<String> KEYS = Collections.unmodifiableSet(
            new LinkedHashSet<>(
                    Arrays.asList(
                            "id",
                            "exports",
                            "uri"
                    )
            )
    );
    private final CommonJava commonJava;
    private final String id;
    private String uri;
    private final Module main;
    private final Module parent;


    private final Require require = new Require(this);

    private Object exports = new SimpleBindings();

    public Module(CommonJava commonJava, String id, Module parent) {
        this.commonJava = commonJava;
        this.id = id;
        if (parent == null) {
            this.main = this;
            this.parent = null;
        } else {
            this.main = parent.main;
            this.parent = parent;
        }
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
        if ("id".equals(name)) {
            return id;
        } else if ("exports".equals(name)) {
            return exports;
        } else if ("uri".equals(name)) {
            return uri;
        } else {
            return null;
        }
    }

    @Override
    public Collection<Object> values() {
        return Arrays.asList(id, exports);
    }

    public Module getParent() {
        return parent;
    }

    public Module getMain() {
        return main;
    }

    public String getId() {
        return id;
    }

    public Require getRequire() {
        return require;
    }

    public Object getExports() {
        return exports;
    }

    public void setExports(Object exports) {
        this.exports = exports;
    }

    public void inject(Bindings bindings) {
        bindings.put("require", require);
        bindings.put("module", this);
        bindings.put("exports", exports);
    }

    public CommonJava getCommonJava() {
        return commonJava;
    }

    @Override
    public void setMember(String name, Object value) {
        if ("exports".equals(name)) {
            exports = value;
        }
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    private String toForwardSlashes(String path) {
        return path.replace('\\', '/');
    }

    public String normalize(String relativeModuleId) {
        if (relativeModuleId.startsWith(".")) {
            Path basePath = Paths.get(id).getParent();
            if (basePath == null) {
                if (relativeModuleId.startsWith("./")) {
                    return relativeModuleId.substring(2);
                } else {
                    return relativeModuleId;
                }
            } else {
                return toForwardSlashes(basePath.resolve(relativeModuleId).normalize().toString());
            }
        } else {
            return toForwardSlashes(Paths.get(relativeModuleId).normalize().toString());
        }
    }
}
