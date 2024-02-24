import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.*;

public class KafkaRelationMicroserviceInspector {

        public static final HashMap<String, ArrayList<KafkaConsumer>> kafkaConsumers = new HashMap<>();

        public static final ArrayList<KafkaProducer> kafkaProducers = new ArrayList<>();

        public static void reset() {
            kafkaProducers.clear();
            kafkaConsumers.clear();
        }

        public static void processClass(String microserviceName, CtClass ctClass, MicroserviceClassesManager manager) {
            searchForMethodsWithKafkaConsumerAnnotation(microserviceName, ctClass);
            if(Utility.isKafkaConsumerClass(ctClass))
                processKafkaListenerClass(microserviceName, ctClass);
            searchForKafkaClassesUsage(microserviceName, ctClass, manager);
        }

        public static HashMap<String, Integer> getMicroserviceRelationsInfo() {
            HashMap<String, Integer> relations = new HashMap<>();

            for (KafkaProducer producer: kafkaProducers) {
                if(kafkaConsumers.containsKey(producer.getTopic())) {
                    for (KafkaConsumer consumer: kafkaConsumers.get(producer.getTopic())) {
                        if(producer.getMicroserviceName().equals(consumer.getMicroserviceName()))
                            continue;

                        String info = producer.getMicroserviceName() + "||" + producer.getClassName() + "||" +
                                consumer.getMicroserviceName() + "||" +
                                consumer.getClassName();
                        if(relations.containsKey(info)) {
                            relations.put(info, relations.get(info) + 1);
                        } else {
                            relations.put(info, 1);
                        }

                    }
                }
            }

            return relations;
        }

        private static void searchForMethodsWithKafkaConsumerAnnotation(String microserviceName, CtClass clazz) {
            Set<CtMethod> methods = new HashSet<>(List.of(clazz.getDeclaredMethods()));
            methods.addAll(List.of(clazz.getMethods()));

            for (CtMethod method : methods) {
                if(Utility.isKafkaConsumerFunction(method)) {
                    StringBuilder methodSignature = new StringBuilder(method.getName() + "(");
                    ClassInspector.getMethodSignature(methodSignature, method.getSignature());

                    KafkaConsumer kafkaConsumer = KafkaConsumer.builder()
                            .microserviceName(microserviceName)
                            .className(clazz.getName())
                            .methodSignature(methodSignature.toString())
                            .topics(Utility.getKafkaTopics(method))
                            .build();

                    processKafkaConsumer(kafkaConsumer);
                }
            }
        }

        public static void processKafkaConsumer(KafkaConsumer kafkaConsumer) {
            for (String topic: kafkaConsumer.getTopics()) {
                if(kafkaConsumers.containsKey(topic)) {
                    kafkaConsumers.get(topic).add(kafkaConsumer);
                } else {
                    kafkaConsumers.put(topic, new ArrayList<>(List.of(kafkaConsumer)));
                }
            }
        }

        private static void processKafkaListenerClass(String microserviceName, CtClass clazz) {
                Set<CtMethod> methods = new HashSet<>(List.of(clazz.getDeclaredMethods()));
                methods.addAll(List.of(clazz.getMethods()));

                for (CtMethod method : methods) {
                    if(Utility.isKafkaHandlerFunction(method)) {
                        StringBuilder methodSignature = new StringBuilder(method.getName() + "(");
                        ClassInspector.getMethodSignature(methodSignature, method.getSignature());

                        KafkaConsumer kafkaConsumer = KafkaConsumer.builder()
                                .microserviceName(microserviceName)
                                .className(clazz.getName())
                                .methodSignature(methodSignature.toString())
                                .topics(Utility.getKafkaTopics(clazz))
                                .build();

                        processKafkaConsumer(kafkaConsumer);
                    }
                }
            }

        private static void searchForKafkaClassesUsage(String microserviceName, CtClass clazz, MicroserviceClassesManager manager) {
            Set<CtMethod> methods = new HashSet<>(List.of(clazz.getDeclaredMethods()));
            methods.addAll(List.of(clazz.getMethods()));

            for (CtMethod method : methods) {
                try {
                    method.instrument(new KafkaRelationBuilder(microserviceName, clazz, method, manager));
                } catch (CannotCompileException e) {
                    throw new RuntimeException(e);
                }
            }
        }
}

@Getter
@AllArgsConstructor
@Builder
@ToString
class KafkaConsumer {
    private final String microserviceName;
    private final String className;
    private final String methodSignature;
    private final ArrayList<String> topics;
}

@Getter
@AllArgsConstructor
@Builder
@ToString
class KafkaProducer {
    private final String microserviceName;
    private final String className;
    private final String methodSignature;
    private final String topic;
}

@AllArgsConstructor
class KafkaRelationBuilder extends ExprEditor {
    private final String microserviceName;
    private final CtClass clazz;
    private final CtMethod method;
    private final MicroserviceClassesManager manager;

    @Override
    public void edit(MethodCall m) {
        StringBuilder calledMethodSignature = new StringBuilder(m.getMethodName() + "(");
        StringBuilder callerMethodSignature = new StringBuilder(method.getName() + "(");
        ClassInspector.getMethodSignature(calledMethodSignature, m.getSignature());
        ClassInspector.getMethodSignature(callerMethodSignature, method.getSignature());

        if(m.getClassName().equals("org.springframework.kafka.core.KafkaTemplate") ||
                m.getClassName().equals("org.apache.kafka.streams.StreamsBuilder")) {
            String filePath = manager.getPathToJavaFile(clazz.getName());
            int lineNumber = m.getLineNumber();

            String instruction = JavaFileReader.extractInstructionFromLine(filePath, lineNumber);
            String topic = Utility.extractKafkaTopicFromKafkaInstruction(filePath, instruction, manager, clazz.getName());

            if (Objects.equals(m.getMethodName(), "send") && topic != null) {
                KafkaProducer producer = KafkaProducer.builder()
                        .microserviceName(microserviceName)
                        .className(clazz.getName())
                        .methodSignature(callerMethodSignature.toString())
                        .topic(topic)
                        .build();

                KafkaRelationMicroserviceInspector.kafkaProducers.add(producer);
            }
            else if(topic != null) {
                KafkaRelationMicroserviceInspector.processKafkaConsumer(KafkaConsumer.builder()
                        .microserviceName(microserviceName)
                        .className(clazz.getName())
                        .methodSignature(callerMethodSignature.toString())
                        .topics(new ArrayList<>(List.of(topic)))
                        .build());
            }
        }
    }
}