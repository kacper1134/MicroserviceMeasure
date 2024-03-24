import math
from typing import List, Tuple

from code.types.class_type import Class
from code.types.microservice import Microservice
from code.types.project import Project


class MQM:
    inner_called_methods = set()

    @staticmethod
    def compute_pair(project: Project, alpha):
        microservices = project.microservices
        measure_values = {}
        for microserviceA in microservices.values():
            for microserviceB in microservices.values():
                if microserviceA == microserviceB:
                    continue
                if microserviceA.is_common_service or microserviceB.is_common_service:
                    continue
                relations = MQM.__get_relations_between_microservices(microserviceA, microserviceB)
                if len(relations) == 0:
                    measure_values[microserviceA.name + "->" + microserviceB.name] = 0
                    continue

                count_of_not_common_classes_from_microserviceA = len([cls for cls in microserviceA.classes.values()
                                                                      if not cls.is_class_from_common_package])
                count_of_not_common_classes_from_microserviceB = len([cls for cls in microserviceB.classes.values()
                                                                      if not cls.is_class_from_common_package])

                if count_of_not_common_classes_from_microserviceA == 0 or count_of_not_common_classes_from_microserviceB == 0:
                    measure_values[microserviceA.name + "->" + microserviceB.name] = 0
                    continue

                relations_classes_ratio = len(relations) / (count_of_not_common_classes_from_microserviceA *
                                                            count_of_not_common_classes_from_microserviceB)

                caller_microserviceMethods = {}
                called_microserviceMethods = {}

                for relation in MQM.__get_full_relations_between_microservices(microserviceA, microserviceB):
                    if relation.source_class + "||" + relation.target_class not in caller_microserviceMethods:

                        caller_microserviceMethods[relation.source_class + "||" + relation.target_class] = set()
                        called_microserviceMethods[relation.source_class + "||" + relation.target_class] = set()

                    caller_microserviceMethods[relation.source_class + "||" + relation.target_class].add(relation.caller_method)
                    called_microserviceMethods[relation.source_class + "||" + relation.target_class].add(relation.called_method)

                measure_value = 0

                for relation in relations:
                    caller_methods = caller_microserviceMethods[relation.source_class + "||" + relation.target_class]
                    called_methods = called_microserviceMethods[relation.source_class + "||" + relation.target_class]

                    I_value = MQM.__visit_all_classes(microserviceA, microserviceA.classes.get(relation.source_class),
                                                      True, alpha, caller_methods)
                    D_value = MQM.__visit_all_classes(microserviceB, microserviceB.classes.get(relation.target_class),
                                                      False, alpha, called_methods)

                    measure_value += (I_value + D_value) / 2

                measure_value = relations_classes_ratio * measure_value
                measure_values[microserviceA.name + "->" + microserviceB.name] = measure_value / len(relations)
        return measure_values

    @staticmethod
    def compute_single(project: Project, alpha, afferent: bool):
        microservices = project.microservices
        measure_values = {}

        for microservice in microservices.values():
            if microservice.is_common_service:
                continue
            relations = MQM.__get_microservice_relations(project, microservice, afferent)
            if len(relations) == 0:
                measure_values[microservice.name] = 0
                continue
            count_of_not_common_classes_from_microservice = len([cls for cls in microservice.classes.values()
                                                                 if not cls.is_class_from_common_package])
            count_of_not_common_classes_from_other_than_microservice = len([cls for m in microservices.values()
                                                                            if m != microservice
                                                                            for cls in m.classes.values()
                                                                            if not cls.is_class_from_common_package])

            if count_of_not_common_classes_from_microservice == 0 or count_of_not_common_classes_from_other_than_microservice == 0:
                measure_values[microservice.name] = 0
                continue

            relations_classes_ratio = len(relations) / (count_of_not_common_classes_from_microservice *
                                                        count_of_not_common_classes_from_other_than_microservice)
            caller_microserviceMethods = {}
            called_microserviceMethods = {}

            for r in relations:
                for relation in MQM.__get_full_relations_between_microservices(microservices[r.source_microservice],
                                                                               microservices[r.target_microservice]):
                    key = relation.source_microservice + "||" + relation.source_class + "||" + relation.target_class + \
                          "||" + relation.target_microservice

                    if key not in caller_microserviceMethods:
                        caller_microserviceMethods[key] = set()
                        called_microserviceMethods[key] = set()
                    caller_microserviceMethods[key].add(relation.caller_method)
                    called_microserviceMethods[key].add(relation.called_method)


            measure_value = 0

            for relation in relations:
                key = relation.source_microservice + "||" + relation.source_class + "||" + relation.target_class + \
                      "||" + relation.target_microservice
                caller_methods = caller_microserviceMethods[key]
                called_methods = called_microserviceMethods[key]

                microserviceA = microservices[relation.source_microservice]
                microserviceB = microservices[relation.target_microservice]

                I_value = MQM.__visit_all_classes(microserviceA, microserviceA.classes.get(relation.source_class),
                                                  True, alpha, caller_methods)
                D_value = MQM.__visit_all_classes(microserviceB, microserviceB.classes.get(relation.target_class),
                                                  False, alpha, called_methods)

                measure_value += (I_value + D_value) / 2

            measure_value = relations_classes_ratio * measure_value
            measure_values[microservice.name] = measure_value / len(relations)

        return measure_values

    @staticmethod
    def __get_relations_between_microservices(microserviceA, microservicesB):
        if microserviceA.microservice_relations.get(microservicesB.name) is None:
            return []
        return microserviceA.microservice_relations[microservicesB.name]

    @staticmethod
    def __get_microservice_relations(project: Project, microservice: Microservice, afferent: bool):
        result = []
        for m in project.microservices.values():
            for relations in m.microservice_relations.values():
                for relation in relations:
                    if relation.target_microservice == microservice.name and afferent:
                        result.append(relation)
                    if relation.source_microservice == microservice.name and not afferent:
                        result.append(relation)
        return result

    @staticmethod
    def __get_full_relations_between_microservices(microserviceA, microservicesB):
        if microserviceA.full_microservice_relations.get(microservicesB.name) is None:
            return []
        return microserviceA.full_microservice_relations[microservicesB.name]

    @staticmethod
    def __visit_all_classes(microservice: Microservice, startClass: Class, inverted: bool, alpha: float, depth_zero_methods):
        result = MQM.__visit_depth_zero_methods(startClass, depth_zero_methods)

        visited = set()
        visited.add(startClass.name)
        queue: List[Tuple[Class, int]] = [(startClass, 0)]

        while queue:
            node: Tuple[Class, int] = queue.pop(0)
            class_obj: Class = node[0]
            if class_obj is None:
                continue
            depth: int = node[1]

            tmp_result = []

            tmp_result.extend([MQM.__visit_relations(relations, visited, queue, microservice, depth, inverted, alpha)
                               for relations in (class_obj.inverted_method_relations.values()
                                                 if inverted else class_obj.method_relations.values())])

            tmp_result.extend([MQM.__visit_relations(relations, visited, queue, microservice, depth, inverted, alpha)
                               for relations in (class_obj.inverted_field_relations.values()
                                                 if inverted else class_obj.field_relations.values())])

            for res in tmp_result:
                result["parameters_values"].extend(res["parameters_values"])
                result["calculated_values"].extend(res["calculated_values"])

        return sum(result["calculated_values"]) / sum(result["parameters_values"])

    @staticmethod
    def __visit_depth_zero_methods(clazz, depth_zero_methods):
        used_number_of_lines = 0
        for depth_zero_method in depth_zero_methods:
            method = clazz.methods.get(depth_zero_method)
            if method is None or method in MQM.inner_called_methods:
                continue
            used_number_of_lines += method.number_of_lines
            MQM.inner_called_methods.add(method)
            used_number_of_lines += MQM.__visit_dependencies_of_called_method(clazz, method)
        MQM.inner_called_methods.clear()
        used_number_of_lines += len(clazz.fields)

        return {"parameters_values": [1], "calculated_values": [0 if clazz.number_of_lines == 0
                                                                else used_number_of_lines / clazz.number_of_lines]}

    @staticmethod
    def __visit_relations(relations, visited, queue, microservice, depth, inverted: bool, alpha: float):
        result = {"parameters_values": [], "calculated_values": []}
        for relation in relations:
            next_class = relation.target_class if not inverted else relation.source_class
            if next_class not in visited:
                used_lines_of_code = MQM.__calculate_used_lines_of_code(microservice.classes.get(relation.source_class),
                                                                        microservice.classes.get(relation.target_class))
                visited.add(next_class)
                queue.append((microservice.classes.get(next_class), depth + 1))

                result["parameters_values"].append(math.pow(alpha, depth + 1))
                result["calculated_values"].append(math.pow(alpha, depth + 1) * used_lines_of_code
                                                   / max(microservice.classes.get(relation.target_class).number_of_lines, 1))
        return result

    @staticmethod
    def __calculate_used_lines_of_code(callerClass: Class, calledClass: Class):
        if calledClass is None or callerClass is None:
            return 0
        used_number_of_lines = 0
        for field_relations in callerClass.field_relations.values():
            for relation in field_relations:
                if relation.target_class == calledClass.name:

                    used_number_of_lines += 1

        for method_relations in callerClass.method_relations.values():
            for relation in method_relations:
                if relation.target_class == calledClass.name:
                    if calledClass.methods.get(relation.target_method_signature) is None:
                        continue
                    calledMethod = calledClass.methods[relation.target_method_signature]
                    number_of_lines_of_called_method = calledMethod.number_of_lines
                    used_number_of_lines += number_of_lines_of_called_method

                    MQM.inner_called_methods.add(calledMethod)
                    used_number_of_lines += MQM.__visit_dependencies_of_called_method(calledClass, calledMethod)

        return used_number_of_lines

    @staticmethod
    def __visit_dependencies_of_called_method(calledClass, calledMethod):
        used_number_of_lines = 0

        for called_class_relations in calledClass.method_relations.values():
            for called_class_relation in called_class_relations:
                if called_class_relation.target_class == calledClass.name \
                        and calledMethod.signature == called_class_relation.source_method_signature:

                    method = calledClass.methods[called_class_relation.target_method_signature]

                    if method not in MQM.inner_called_methods:
                        MQM.inner_called_methods.add(method)
                        used_number_of_lines += method.number_of_lines + \
                                                MQM.__visit_dependencies_of_called_method(calledClass, method)
        return used_number_of_lines
