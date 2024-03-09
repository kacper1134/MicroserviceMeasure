from code.types.microservice import Microservice
from code.types.project import Project


class ADS:
    @staticmethod
    def compute_single(project: Project):
        microservices = project.microservices
        measure_values = {}

        for microservice in microservices.values():
            if microservice.is_common_service:
                continue
            microservices_that_current_services_depends = ADS.__get_microservice_relations(project, microservice)
            measure_values[microservice.name] = len(microservices_that_current_services_depends)

        return measure_values

    @staticmethod
    def __get_microservice_relations(project: Project, microservice: Microservice):
        result = set()
        for m in project.microservices.values():
            for relations in m.microservice_relations.values():
                for relation in relations:
                    if relation.source_microservice == microservice.name:
                        result.add(m.name)
        return result