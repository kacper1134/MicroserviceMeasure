from typing import Dict

from code.types.method_relation import MethodRelation
from code.types.microservice import Microservice


class Project:
    def __init__(self, name: str):
        self.name = name
        self.microservices: Dict[str, Microservice] = {}

    def add_microservice(self, microservice: Microservice):
        self.microservices[microservice.name] = microservice

    def change_microservices_to_common(self, common_microservices):
        for microservice in common_microservices:
            self.microservices[microservice].is_common_service = True

    def add_classes_from_common_microservices(self, common_microservices):
        for common_microservice in common_microservices:
            for microservice in self.microservices.values():
                if microservice.name != common_microservice:
                    microservice.add_classes(self.microservices[common_microservice].classes)

    def delete_microservice_relations_to_common(self):
        for microservice in self.microservices.values():
            new_relations = {}
            for relations in list(microservice.microservice_relations.values()):
                for relation in relations:
                    if not self.microservices[relation.target_microservice].is_common_service:
                        if relation.target_microservice not in new_relations:
                            new_relations[relation.target_microservice] = set()
                        new_relations[relation.target_microservice].add(relation)
            microservice.microservice_relations = new_relations

    def add_common_relations_to_method_relations(self):
        for microservice in self.microservices.values():
            for relations in microservice.full_microservice_relations.values():
                for relation in relations:
                    if self.microservices[relation.target_microservice].is_common_service:
                        methodRelation = MethodRelation(relation.source_class, relation.caller_method, relation.target_class, relation.called_method)
                        microservice.classes[relation.source_class].add_method_relation(methodRelation)

    def add_inverted_relations(self):
        for microservice in self.microservices.values():
            microservice.add_inverted_relations()

    def __str__(self):
        microservices_str = "\n".join([str(ms) for ms in self.microservices.values()])
        return f"Project Name: {self.name}\nMicroservices:\n{microservices_str}"
