from code.types.microservice import Microservice
from code.types.project import Project


class CA:
    @staticmethod
    def compute_single(project: Project):
        microservices = project.microservices
        measure_values = {}

        for microservice in microservices.values():
            if microservice.is_common_service:
                continue
            relations = CA.__get_microservice_relations(project, microservice)
            measure_values[microservice.name] = len(relations)

        return measure_values

    @staticmethod
    def __get_microservice_relations(project: Project, microservice: Microservice):
        result = []
        for m in project.microservices.values():
            for relations in m.microservice_relations.values():
                for relation in relations:
                    if relation.target_microservice == microservice.name:
                        result.append(relation)
        return result
