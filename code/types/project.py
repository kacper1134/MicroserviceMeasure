from typing import Dict

from code.types.microservice import Microservice


class Project:
    def __init__(self, name: str):
        self.name = name
        self.microservices: Dict[str, Microservice] = {}

    def add_microservice(self, microservice: Microservice):
        self.microservices[microservice.name] = microservice

    def add_inverted_relations(self):
        for microservice in self.microservices.values():
            microservice.add_inverted_relations()

    def __str__(self):
        microservices_str = "\n".join([str(ms) for ms in self.microservices.values()])
        return f"Project Name: {self.name}\nMicroservices:\n{microservices_str}"
