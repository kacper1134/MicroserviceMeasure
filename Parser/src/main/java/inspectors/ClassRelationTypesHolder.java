package inspectors;

import java.util.HashMap;

public class ClassRelationTypesHolder {
    public final HashMap<String, Integer> classToClassRelation = new HashMap<>();
    public final HashMap<String, Integer> classToInterfaceRelation = new HashMap<>();
    public final HashMap<String, Integer> interfaceToClassRelation = new HashMap<>();
    public final HashMap<String, Integer> interfaceToInterfaceRelation = new HashMap<>();
}
