import javassist.ClassPool;
import javassist.CtClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;

public class MicroserviceClassesManager {
    private final ClassPool pool = new ClassPool(true);

    private final HashSet<String> classNames = new HashSet<>();

    private static final Logger LOG = LogManager.getLogger(MicroserviceClassesManager.class);

    public ArrayList<String> getClassNames() {
        return new ArrayList<>(classNames);
    }

    public boolean isClassDeclared(String className) {
        return classNames.contains(className);
    }

    public CtClass getClass(String className) {
        try {
            return pool.get(className);
        } catch (Exception e) {
            LOG.error("Error getting class: " + e.getMessage());
            return null;
        }
    }

    public void loadClass(String pathToClass) {
        try {
            CtClass clazz = pool.makeClass(Files.newInputStream(Paths.get(pathToClass)));
            classNames.add(clazz.getName());
        } catch (Exception e) {
            LOG.error("Error loading class: " + e.getMessage());
        }
    }

}
