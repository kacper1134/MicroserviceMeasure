import javassist.CtClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
    private static final Logger LOG = LogManager.getLogger(StructureWriter.class);

    public static void main(String[] args) throws IOException {
        ArrayList<String> projectNames = ProjectManager.getProjectNames("C:\\Users\\Kacper\\Desktop\\data\\");
        for(String projectName : projectNames) {
            MicroserviceClassesManager.clear();
            FeignRelationMicroserviceInspector.reset();
            KafkaRelationMicroserviceInspector.reset();
            ProjectManager.reset();
            LOG.info("Processing project: " + projectName);
            processProject(projectName);
            LOG.info("Project processed: " + projectName);
            LOG.info("\n");
        }
    }

    public static void processProject(String projectName) throws IOException {
        String[] classFilePaths = ClassFinder.findClassFiles("C:\\Users\\Kacper\\Desktop\\data\\" + projectName).toArray(new String[0]);

        for (String classFilePath : classFilePaths) {
            String microserviceName = ProjectManager.getMicroserviceNameFromFilePath(classFilePath);

            MicroserviceClassesManager microserviceClassesManager = ProjectManager.getMicroserviceClassesManager(microserviceName);
            microserviceClassesManager.loadClass(classFilePath);
        }

        StructureWriter.deleteFilesInDirectory("../data/" + projectName);
        StructureWriter.writeMicroserviceNamesToFile(projectName, ProjectManager.getMicroserviceNames());

        for(String microserviceName : ProjectManager.getMicroserviceNames()) {
            LOG.info("Processing microservice: " + microserviceName);
            MicroserviceClassesManager manager = ProjectManager.getMicroserviceClassesManager(microserviceName);
            ArrayList<String> classNames = manager.getClassNames();

            ArrayList<String> classesInfo = new ArrayList<>();
            ArrayList<String> interfacesInfo = new ArrayList<>();
            ArrayList<String> fieldsInfo = new ArrayList<>();

            HashMap<String, Integer> classToClassRelation = new HashMap<>();
            HashMap<String, Integer> classToInterfaceRelation = new HashMap<>();
            HashMap<String, Integer> interfaceToClassRelation = new HashMap<>();
            HashMap<String, Integer> interfaceToInterfaceRelation = new HashMap<>();

            HashMap<String, Integer> fieldClassRelation = new HashMap<>();
            HashMap<String, Integer> fieldInterfaceRelation = new HashMap<>();

            for(String className : classNames) {
                CtClass clazz = manager.getClass(className);
                if(Utility.isApiClass(clazz)) {
                    interfacesInfo.addAll(ClassInspector.getMethodsInfo(clazz, true));
                } else {
                    classesInfo.addAll(ClassInspector.getMethodsInfo(clazz, false));
                }

                FeignRelationMicroserviceInspector.processClass(microserviceName, clazz);
                KafkaRelationMicroserviceInspector.processClass(microserviceName, clazz, manager);

                fieldsInfo.addAll(ClassInspector.getFieldsInfo(clazz));

                ClassRelationTypesHolder holder = ClassInspector.getMethodRelationsInfo(clazz, manager);
                classToClassRelation.putAll(holder.classToClassRelation);
                classToInterfaceRelation.putAll(holder.classToInterfaceRelation);
                interfaceToClassRelation.putAll(holder.interfaceToClassRelation);
                interfaceToInterfaceRelation.putAll(holder.interfaceToInterfaceRelation);

                FieldRelationTypesHolder fieldHolder = ClassInspector.getFieldRelationsInfo(clazz, manager);
                fieldClassRelation.putAll(fieldHolder.fieldClassRelation);
                fieldInterfaceRelation.putAll(fieldHolder.fieldInterfaceRelation);
            }

            StructureWriter.writeAboutClassMethodsInfo(projectName, microserviceName, classesInfo);
            StructureWriter.writeAboutInterfaceMethodsInfo(projectName, microserviceName, interfacesInfo);
            StructureWriter.writeAboutFieldsInfo(projectName, microserviceName, fieldsInfo);
            StructureWriter.writeAboutClassToClassRelations(projectName, microserviceName, classToClassRelation);
            StructureWriter.writeAboutClassToInterfaceRelations(projectName, microserviceName, classToInterfaceRelation);
            StructureWriter.writeAboutInterfaceToClassRelations(projectName, microserviceName, interfaceToClassRelation);
            StructureWriter.writeAboutInterfaceToInterfaceRelations(projectName, microserviceName, interfaceToInterfaceRelation);
            StructureWriter.writeAboutFieldClassRelations(projectName, microserviceName, fieldClassRelation);
            StructureWriter.writeAboutFieldInterfaceRelations(projectName, microserviceName, fieldInterfaceRelation);

            LOG.info("Microservice processed: " + microserviceName);
            LOG.info("\n");
        }

        HashMap<String, Integer> microServiceRelationInfo = new HashMap<>();

        for(String microserviceName : ProjectManager.getMicroserviceNames()) {
            MicroserviceClassesManager manager = ProjectManager.getMicroserviceClassesManager(microserviceName);
            ArrayList<String> classNames = manager.getClassNames();

            for (String className : classNames) {
                CtClass clazz = manager.getClass(className);
                microServiceRelationInfo.putAll(FeignRelationMicroserviceInspector.getMicroserviceRelationsInfo(microserviceName, clazz));
            }
        }

        StructureWriter.writeAboutMicroserviceFeignRelations(projectName, microServiceRelationInfo);
        StructureWriter.writeAboutMicroserviceKafkaRelations(projectName, KafkaRelationMicroserviceInspector.getMicroserviceRelationsInfo());
    }
}
