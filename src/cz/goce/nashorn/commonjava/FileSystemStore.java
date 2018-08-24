package cz.goce.nashorn.commonjava;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author goce.cz
 */
public class FileSystemStore implements JsStore {
    private final Path baseDir;
    private final Charset charset;

    public FileSystemStore(Path baseDir, Charset charset) {
        this.baseDir = baseDir;
        this.charset = charset;
    }

    @Override
    public ScriptFile open(String absoluteModuleId) {
        Path filePath = baseDir.resolve(absoluteModuleId + ".js").toAbsolutePath();
        if (!Files.exists(filePath)) {
            return null;
        }

        try {
            return new ScriptFile(
                    Files.newBufferedReader(filePath, charset),
                    filePath.toString()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
