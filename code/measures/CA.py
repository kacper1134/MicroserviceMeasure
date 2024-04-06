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
    def compute_pair(project: Project):
        microservices = project.microservices
        measure_values = {}

        for microserviceA in microservices.values():
            for microserviceB in microservices.values():
                if microserviceA == microserviceB:
                    continue
                if microserviceA.is_common_service or microserviceB.is_common_service:
                    continue
                measure_values[f"{microserviceA.name}->{microserviceB.name}"] = 0

        for microservice in microservices.values():
            if microservice.is_common_service:
                continue
            relations = CA.__get_microservice_relations(project, microservice)
            for relation in relations:
                microserviceA = project.microservices[relation.source_microservice]
                microserviceB = project.microservices[relation.target_microservice]
                if microserviceA.is_common_service or microserviceB.is_common_service or microserviceA == microserviceB:
                    continue
                measure_values[f"{microserviceA.name}->{microserviceB.name}"] += 1

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
