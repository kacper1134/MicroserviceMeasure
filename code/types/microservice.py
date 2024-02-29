from typing import Dict, Set

from code.types.class_type import Class
from code.types.full_microservice_relation import FullMicroserviceRelation
from code.types.microservice_relation import MicroserviceRelation


class Microservice:
    def __init__(self, name: str):
        self.name = name
        self.classes: Dict[str, Class] = {}
        self.is_common_service = False
        self.microservice_relations: Dict[str, Set[MicroserviceRelation]] = {}
        self.full_microservice_relations: Dict[str, Set[FullMicroserviceRelation]] = {}

    def add_class(self, class_obj: Class):
        self.classes[class_obj.name] = class_obj

    def add_classes(self, classes: Dict[str, Class]):
        self.classes.update(classes)

    def add_microservice_relation(self, relation: MicroserviceRelation):
        if relation.target_microservice not in self.microservice_relations:
            self.microservice_relations[relation.target_microservice] = set()
        self.microservice_relations[relation.target_microservice].add(relation)

    def add_full_microservice_relation(self, relation: FullMicroserviceRelation):
        if relation.target_microservice not in self.full_microservice_relations:
            self.full_microservice_relations[relation.target_microservice] = set()
        self.full_microservice_relations[relation.target_microservice].add(relation)

    def add_inverted_relations(self):
        for clazz in self.classes.values():
            for relations in clazz.method_relations.values():
                for method_relation in relations:
                    target_class = self.classes[method_relation.target_class]

                    if method_relation.source_method_signature not in target_class.inverted_method_relations:
                        target_class.inverted_method_relations[method_relation.source_method_signature] = []
                    target_class.inverted_method_relations[method_relation.source_method_signature].append(method_relation)

            for relations in clazz.field_relations.values():
                for field_relation in relations:
                    target_class = self.classes[field_relation.target_class]

                    if field_relation.source_method not in target_class.inverted_field_relations:
                        target_class.inverted_field_relations[field_relation.source_method] = []
                    target_class.inverted_field_relations[field_relation.source_method].append(field_relation)

    def fix_incorrect_classes(self):
        for class_obj in self.classes.values():
            total_number_of_lines = sum([method.number_of_lines for method in class_obj.methods.values()]) + len(class_obj.fields)
            if total_number_of_lines != class_obj.number_of_lines:
                class_obj.number_of_lines = total_number_of_lines

    def __str__(self):
        classes_str = "\n".join([str(class_obj) for class_obj in self.classes.values()])
        return f"Microservice Name: {self.name}\nClasses:\n{classes_str}"
