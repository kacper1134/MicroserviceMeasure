package managers;

import javassist.ClassPool;
import javassist.CtClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class ProjectClassesManager {
    private final ClassPool pool = new ClassPool(true);

    private final HashMap<String, String> classNamesToMicroserviceName = new HashMap<>();

    private static final Logger LOG = LogManager.getLogger(MicroserviceClassesManager.class);

    public String getClassMicroserviceName(String className) {
        try {
            return classNamesToMicroserviceName.get(className);
        } catch (Exception e) {
            LOG.error("Error getting class: " + e.getMessage());
            return null;
        }
    }

    public CtClass getClass(String className) {
        try {
            return pool.get(className);
        } catch (Exception e) {
            LOG.error("Error getting class: " + e.getMessage());
            return null;
        }
    }

    public void loadClass(String pathToClass, String microserviceName) {
        try {
            CtClass clazz = pool.makeClass(Files.newInputStream(Paths.get(pathToClass)));
            classNamesToMicroserviceName.put(clazz.getName(), microserviceName);
        } catch (Exception e) {
            LOG.error("Error loading class: " + e.getMessage());
        }
    }
}
