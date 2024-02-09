import java.util.*;

public class ProjectManager {
    private static final Map<String, MicroserviceClassesManager> microserviceClassesManagerMap = new HashMap<>();

    private static final Set<String> microserviceNames = new HashSet<>();

    public static ArrayList<String> getMicroserviceNames() {
        return new ArrayList<>(microserviceNames);
    }

    public static MicroserviceClassesManager getMicroserviceClassesManager(String microserviceName) {
        if (!microserviceClassesManagerMap.containsKey(microserviceName)) {
            microserviceClassesManagerMap.put(microserviceName, new MicroserviceClassesManager());
        }
        return microserviceClassesManagerMap.get(microserviceName);
    }

    public static String getMicroserviceNameFromFilePath(String filePath) {
        int targetIndex = filePath.indexOf("\\target\\");
        if (targetIndex != -1) {
            int lastSeparatorIndex = filePath.lastIndexOf("\\", targetIndex - 1);
            if (lastSeparatorIndex != -1) {
                String name = filePath.substring(lastSeparatorIndex + 1, targetIndex);
                microserviceNames.add(name);
                return name;
            }
        }
        return null;
    }
}
