package managers;

import javassist.ClassPool;
import javassist.CtClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class MicroserviceClassesManager {
    private final ClassPool pool = new ClassPool(true);

    private final HashSet<String> classNames = new HashSet<>();

    private static final HashMap<String, String> javaPaths = new HashMap<>();

    private static final HashMap<String, String> classPaths = new HashMap<>();

    private static final Logger LOG = LogManager.getLogger(MicroserviceClassesManager.class);

    public static void clear() {
        javaPaths.clear();
    }

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

    public String getPathToJavaFile(String className) {
        return javaPaths.get(className);
    }

    public String getPathToClassFile(String className) {
        return classPaths.get(className);
    }

    public void loadClass(String pathToClass) {
        try {
            CtClass clazz = pool.makeClass(Files.newInputStream(Paths.get(pathToClass)));
            classNames.add(clazz.getName());
            classPaths.put(clazz.getName(), pathToClass);
            javaPaths.put(clazz.getName(), pathToClass
                    .replace("\\target\\classes\\", "\\src\\main\\java\\")
                    .replace("\\target\\test-classes\\", "\\src\\test\\java\\")
                    .replace(".class", ".java"));
        } catch (Exception e) {
            LOG.error("Error loading class: " + e.getMessage());
        }
    }

}
