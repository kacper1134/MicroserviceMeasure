import math
from typing import List, Tuple

from code.types.class_type import Class
from code.types.microservice import Microservice
from code.types.project import Project


class MSM:
    inner_called_methods = set()

    @staticmethod
    def compute(project: Project, alpha):
        microservices = project.microservices
        measure_values = {}
        for microserviceA in microservices.values():
            for microserviceB in microservices.values():
                if microserviceA == microserviceB:
                    continue
                relations = MSM.__get_relations_between_microservices(microserviceA, microserviceB)
                if len(relations) == 0:
                    continue

                count_of_not_common_classes_from_microserviceA = len([cls for cls in microserviceA.classes.values()
                                                                      if not cls.is_class_from_common_package])
                count_of_not_common_classes_from_microserviceB = len([cls for cls in microserviceB.classes.values()
                                                                      if not cls.is_class_from_common_package])

                relations_classes_ratio = len(relations) / (count_of_not_common_classes_from_microserviceA *
                                                            count_of_not_common_classes_from_microserviceB)

                caller_microserviceMethods = {}
                called_microserviceMethods = {}

                for relation in MSM.__get_full_relations_between_microservices(microserviceA, microserviceB):
                    if relation.source_class + "||" + relation.target_class not in caller_microserviceMethods:
                        caller_microserviceMethods[relation.source_class + "||" + relation.target_class] = []
                        called_microserviceMethods[relation.source_class + "||" + relation.target_class] = []

                    caller_microserviceMethods[relation.source_class + "||" + relation.target_class].append(relation.caller_method)
                    called_microserviceMethods[relation.source_class + "||" + relation.target_class].append(relation.called_method)

                measure_value = 0

                for relation in relations:
                    caller_methods = caller_microserviceMethods[relation.source_class + "||" + relation.target_class]
                    called_methods = called_microserviceMethods[relation.source_class + "||" + relation.target_class]

                    I_value = MSM.__visit_all_classes(microserviceA, microserviceA.classes.get(relation.source_class),
                                                      True, alpha, caller_methods)
                    D_value = MSM.__visit_all_classes(microserviceB, microserviceB.classes.get(relation.target_class),
                                                      False, alpha, called_methods)

                    measure_value += (I_value + D_value) / 2

                measure_value = relations_classes_ratio * measure_value
                measure_values[microserviceA.name + "->" + microserviceB.name] = measure_value
        return measure_values

    @staticmethod
    def __get_relations_between_microservices(microserviceA, microservicesB):
        if microserviceA.microservice_relations.get(microservicesB.name) is None:
            return []
        return microserviceA.microservice_relations[microservicesB.name]

    @staticmethod
    def __get_full_relations_between_microservices(microserviceA, microservicesB):
        if microserviceA.full_microservice_relations.get(microservicesB.name) is None:
            return []
        return microserviceA.full_microservice_relations[microservicesB.name]

    @staticmethod
    def __visit_all_classes(microservice: Microservice, startClass: Class, inverted: bool, alpha: float, depth_zero_methods):
        result = MSM.__visit_depth_zero_methods(startClass, depth_zero_methods)

        visited = set()
        visited.add(startClass.name)
        queue: List[Tuple[Class, int]] = [(startClass, 0)]

        while queue:
            node: Tuple[Class, int] = queue.pop(0)
            class_obj: Class = node[0]
            depth: int = node[1]

            tmp_result = []

            tmp_result.extend([MSM.__visit_relations(relations, visited, queue, microservice, depth, inverted, alpha)
                               for relations in (class_obj.inverted_method_relations.values()
                                                 if inverted else class_obj.method_relations.values())])

            tmp_result.extend([MSM.__visit_relations(relations, visited, queue, microservice, depth, inverted, alpha)
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
            if method is None:
                continue
            used_number_of_lines = method.number_of_lines

        return {"parameters_values": [1], "calculated_values": [used_number_of_lines / clazz.number_of_lines]}

    @staticmethod
    def __visit_relations(relations, visited, queue, microservice, depth, inverted: bool, alpha: float):
        result = {"parameters_values": [], "calculated_values": []}
        for relation in relations:
            next_class = relation.target_class if not inverted else relation.source_class
            if next_class not in visited:
                used_lines_of_code = MSM.__calculate_used_lines_of_code(microservice.classes.get(relation.source_class),
                                                                        microservice.classes.get(relation.target_class))
                visited.add(next_class)
                queue.append((microservice.classes.get(next_class), depth + 1))

                result["parameters_values"].append(math.pow(alpha, depth + 1))
                result["calculated_values"].append(math.pow(alpha, depth + 1) * used_lines_of_code
                                                   / microservice.classes.get(relation.target_class).number_of_lines)
        return result

    @staticmethod
    def __calculate_used_lines_of_code(callerClass: Class, calledClass: Class):
        used_number_of_lines = 0
        for field_relations in callerClass.field_relations.values():
            for relation in field_relations:
                if relation.target_class == calledClass.name:

                    used_number_of_lines += 1

        for method_relations in callerClass.method_relations.values():
            for relation in method_relations:
                if relation.target_class == calledClass.name:
                    calledMethod = calledClass.methods[relation.target_method_signature]
                    number_of_lines_of_called_method = calledMethod.number_of_lines
                    used_number_of_lines += number_of_lines_of_called_method

                    MSM.inner_called_methods.add(calledMethod)
                    used_number_of_lines += MSM.__visit_dependencies_of_called_method(calledClass, calledMethod)

        return used_number_of_lines

    @staticmethod
    def __visit_dependencies_of_called_method(calledClass, calledMethod):
        used_number_of_lines = 0

        for called_class_relations in calledClass.method_relations.values():
            for called_class_relation in called_class_relations:
                if called_class_relation.target_class == calledClass.name \
                        and calledMethod.signature == called_class_relation.source_method_signature:

                    method = calledClass.methods[called_class_relation.target_method_signature]

                    if method not in MSM.inner_called_methods:
                        MSM.inner_called_methods.add(method)
                        used_number_of_lines += method.number_of_lines + \
                                                MSM.__visit_dependencies_of_called_method(calledClass, method)
        return used_number_of_lines
