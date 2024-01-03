from typing import Dict, List

from code.types.class_type import Class
from code.types.microservice_relation import MicroserviceRelation


class Microservice:
    def __init__(self, name: str):
        self.name = name
        self.classes: Dict[str, Class] = {}
        self.microservice_relations: Dict[str, List[MicroserviceRelation]] = {}

    def add_class(self, class_obj: Class):
        self.classes[class_obj.name] = class_obj

    def add_microservice_relation(self, relation: MicroserviceRelation):
        if relation.source_class not in self.microservice_relations:
            self.microservice_relations[relation.source_class] = []
        self.microservice_relations[relation.source_class].append(relation)

    def __str__(self):
        classes_str = "\n".join([str(class_obj) for class_obj in self.classes.values()])
        return f"Microservice Name: {self.name}\nClasses:\n{classes_str}"
