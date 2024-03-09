from code.types.class_type import Class
from code.types.microservice import Microservice
from code.types.project import Project


class MCI:
    @staticmethod
    def compute_pair(project: Project):
        result = {}
        microservices = project.microservices

        for microserviceA in microservices.values():
            for microserviceB in microservices.values():
                if microserviceA == microserviceB or microserviceA.is_common_service or microserviceB.is_common_service:
                    continue

                dependent_entities = MCI.__get_dependent_entities_pair(microserviceA, microserviceB, False)
                dependent_interfaces = MCI.__get_dependent_entities_pair(microserviceA, microserviceB, True)

                number_of_entities = len([entity for entity in microserviceA.classes.values()
                                          if not entity.is_class_from_common_package])
                number_of_interfaces = len([interface for interface in microserviceB.classes.values()
                                            if not interface.is_class_from_common_package and interface.is_interface])

                if len(dependent_interfaces) > 0 and len(dependent_entities) > 0:
                    sum_of_minimal_distances = MCI.__get_total_sum_of_minimal_distances_pair(microserviceA, microserviceB)
                    mci = len(dependent_interfaces) / number_of_interfaces * \
                        ((len(dependent_entities) / number_of_entities) +
                         (len(dependent_entities) / sum_of_minimal_distances))

                    result[f"{microserviceA.name}->{microserviceB.name}"] = mci
                else:
                    result[f"{microserviceA.name}->{microserviceB.name}"] = 0

        return result

    @staticmethod
    def compute_single(project: Project, afferent: bool):
        result = {}
        microservices = project.microservices

        for microservice in microservices.values():
            if microservice.is_common_service:
                continue

            dependent_entities = MCI.__get_dependent_entities(project, microservice, False, afferent)
            dependent_interfaces = MCI.__get_dependent_entities(project, microservice, True, afferent)
            number_of_entities = 0 if afferent else len([entity for entity in microservice.classes.values()
                                                         if not entity.is_class_from_common_package])
            number_of_interfaces = 0 if not afferent else len([interface for interface in microservice.classes.values()
                                                               if not interface.is_class_from_common_package and interface.is_interface])

            for connected_microservice in project.microservices:
                if project.microservices[connected_microservice].is_common_service or connected_microservice == microservice.name:
                    continue

                if afferent:
                    number_of_entities += len([entity for entity in project.microservices[connected_microservice].classes.values()
                                           if not entity.is_class_from_common_package])
                else:
                    number_of_interfaces += len([interface for interface in project.microservices[connected_microservice].classes.values()
                                                if not interface.is_class_from_common_package and interface.is_interface])

            if len(dependent_interfaces) > 0 and len(dependent_entities) > 0:
                sum_of_minimal_distances = MCI.__get_total_sum_of_minimal_distances(project, microservice, afferent)
                mci = len(dependent_interfaces) / number_of_interfaces * \
                    ((len(dependent_entities) / number_of_entities) +
                     (len(dependent_entities) / sum_of_minimal_distances))

                result[microservice.name] = mci
            else:
                result[microservice.name] = 0

        return result

    @staticmethod
    def __get_dependent_entities_pair(microserviceA: Microservice, microserviceB: Microservice, isInterfacesMode: bool):
        dependent_entities = set()
        relations_between_microservices = MCI.__get_full_relations_between_microservices(microserviceA, microserviceB)

        for relation in relations_between_microservices:
            if isInterfacesMode:
                dependent_entities.update(MCI.__get_list_of_connected_entities(microserviceB, microserviceB.classes[relation.target_class], isInterfacesMode))
            else:
                dependent_entities.update(MCI.__get_list_of_connected_entities(microserviceA, microserviceA.classes[relation.source_class], isInterfacesMode))

        return dependent_entities

    @staticmethod
    def __get_dependent_entities(project: Project, microservice: Microservice, isInterfacesMode: bool, afferent: bool):
        dependent_entities = set()
        relations_between_microservices = MCI.__get_full_microservice_relations(project, microservice, afferent)

        for relation in relations_between_microservices:
            microserviceA = project.microservices[relation.source_microservice]
            microserviceB = project.microservices[relation.target_microservice]
            if isInterfacesMode:
                dependent_entities.update(MCI.__get_list_of_connected_entities(microserviceB, microserviceB.classes[relation.target_class], isInterfacesMode))
            else:
                dependent_entities.update(MCI.__get_list_of_connected_entities(microserviceA, microserviceA.classes[relation.source_class], isInterfacesMode))

        return dependent_entities

    @staticmethod
    def __get_full_relations_between_microservices(microserviceA, microservicesB):
        if microserviceA.full_microservice_relations.get(microservicesB.name) is None:
            return []
        return microserviceA.full_microservice_relations[microservicesB.name]

    @staticmethod
    def __get_full_microservice_relations(project: Project, microservice: Microservice, afferent: bool):
        result = []
        for m in project.microservices.values():
            for relations in m.full_microservice_relations.values():
                for relation in relations:
                    target_microservice = project.microservices[relation.target_microservice]
                    if target_microservice.is_common_service:
                        continue

                    if relation.target_microservice == microservice.name and afferent:
                        result.append(relation)
                    if relation.source_microservice == microservice.name and not afferent:
                        result.append(relation)
        return result

    @staticmethod
    def __get_list_of_connected_entities(microservice: Microservice, startClass: Class, isInterfacesMode: bool):
        connected_entities = set()
        result = set()
        queue = [startClass]

        while queue:
            currentClass = queue.pop(0)
            connected_entities.add(currentClass.name)

            if not isInterfacesMode or (isInterfacesMode and currentClass.is_interface):
                result.add(currentClass.name)

            for method_relations in currentClass.method_relations.values() if isInterfacesMode else currentClass.inverted_method_relations.values():
                for relation in method_relations:
                    target_class = microservice.classes[relation.target_class]
                    if target_class.name not in connected_entities and not target_class.is_class_from_common_package:
                        queue.append(target_class)

            for field in currentClass.field_relations.values() if isInterfacesMode else currentClass.inverted_field_relations.values():
                for relation in field:
                    target_class = microservice.classes[relation.target_class]
                    if target_class.name not in connected_entities and not target_class.is_class_from_common_package:
                        queue.append(target_class)

        return result

    @staticmethod
    def __get_total_sum_of_minimal_distances_pair(microserviceA: Microservice, microserviceB: Microservice):
        distances = {}
        relations_between_microservices = MCI.__get_full_relations_between_microservices(microserviceA, microserviceB)

        for relation in relations_between_microservices:
            MCI.__update_minimal_distances_of_depended_entities(microserviceA, microserviceA.classes[relation.source_class], distances)
        return sum(distances.values())

    @staticmethod
    def __get_total_sum_of_minimal_distances(project: Project, microservice: Microservice, afferent: bool):
        distances = {}
        relations_between_microservices = MCI.__get_full_microservice_relations(project, microservice, afferent)

        for relation in relations_between_microservices:
            microserviceA = project.microservices[relation.source_microservice]
            MCI.__update_minimal_distances_of_depended_entities(microserviceA, microserviceA.classes[relation.source_class], distances)

        return sum(distances.values())

    @staticmethod
    def __update_minimal_distances_of_depended_entities(microservice: Microservice, startClass: Class, distances: dict):
        connected_entities = set()
        queue = [{"class": startClass, "distance": 1}]

        while queue:
            node = queue.pop(0)
            currentClass = node["class"]
            connected_entities.add(currentClass.name)

            if distances.get(currentClass.name) is None or distances[currentClass.name] > node["distance"]:
                distances[currentClass.name] = node["distance"]

            for method_relations in currentClass.inverted_method_relations.values():
                for relation in method_relations:
                    target_class = microservice.classes[relation.target_class]
                    if target_class.name not in connected_entities and not target_class.is_class_from_common_package:
                        queue.append({"class": target_class, "distance": node["distance"] + 1})

            for field in currentClass.inverted_field_relations.values():
                for relation in field:
                    target_class = microservice.classes[relation.target_class]
                    if target_class.name not in connected_entities and not target_class.is_class_from_common_package:
                        queue.append({"class": target_class, "distance": node["distance"] + 1})