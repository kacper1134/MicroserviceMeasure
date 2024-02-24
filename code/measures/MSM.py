from typing import List, Tuple

from code.types.class_type import Class
from code.types.microservice import Microservice
from code.types.project import Project


class MSM:
    @staticmethod
    def compute(project: Project):
        microservices = project.microservices
        for microserviceA in microservices.values():
            for microserviceB in microservices.values():
                if microserviceA == microserviceB:
                    continue
                relations = MSM.__get_relations_between_microservices(microserviceA, microserviceB)
                if len(relations) == 0:
                    continue

                relations_classes_ratio = len(relations) / (len(microserviceA.classes) * len(microserviceB.classes))
                for relation in relations:
                    MSM.__visit_all_classes(microserviceA, microserviceA.classes.get(relation.source_class), True)
                    MSM.__visit_all_classes(microserviceB, microserviceB.classes.get(relation.target_class), False)

    @staticmethod
    def __get_relations_between_microservices(microserviceA, microservicesB):
        if microserviceA.microservice_relations.get(microservicesB.name) is None:
            return []
        return microserviceA.microservice_relations[microservicesB.name]

    @staticmethod
    def __visit_all_classes(microservice: Microservice, startClass: Class, inverted: bool):
        visited = set()
        visited.add(startClass.name)
        queue: List[Tuple[Class, int]] = [(startClass, 0)]
        while queue:
            node: Tuple[Class, int] = queue.pop(0)
            class_obj: Class = node[0]
            depth: int = node[1]

            [MSM.__visit_relations(relations, visited, queue, microservice, depth, inverted) for relations in
             (class_obj.inverted_method_relations.values() if inverted else class_obj.method_relations.values())]

            [MSM.__visit_relations(relations, visited, queue, microservice, depth, inverted) for relations in
             (class_obj.inverted_field_relations.values() if inverted else class_obj.field_relations.values())]

    @staticmethod
    def __visit_relations(relations, visited, queue, microservice, depth, inverted: bool):
        for relation in relations:
            next_class = relation.target_class if not inverted else relation.source_class
            if next_class not in visited:
                visited.add(next_class)
                queue.append((microservice.classes.get(next_class), depth + 1))
