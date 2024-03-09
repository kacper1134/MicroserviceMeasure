from code.types.microservice import Microservice
from code.types.project import Project


class AIS:
    @staticmethod
    def compute_single(project: Project):
        microservices = project.microservices
        measure_values = {}

        for microservice in microservices.values():
            if microservice.is_common_service:
                continue
            depended_microservices = AIS.__get_microservice_relations(project, microservice)
            measure_values[microservice.name] = len(depended_microservices)

        return measure_values

    @staticmethod
    def __get_microservice_relations(project: Project, microservice: Microservice):
        result = set()
        for m in project.microservices.values():
            for relations in m.microservice_relations.values():
                for relation in relations:
                    if relation.target_microservice == microservice.name:
                        result.add(m.name)
        return result