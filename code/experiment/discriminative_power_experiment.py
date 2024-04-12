from code.measures.ADS import ADS
from code.measures.AIS import AIS
from code.measures.CA import CA
from code.measures.CE import CE
from code.measures.MCI import MCI
from code.measures.MQM import MQM
from code.types.project import Project


class DiscriminativePowerExperiment:

    @staticmethod
    def get_discriminative_power_for_pair_metrics(projects):
        MQM_values, MCI_values, CaT_values = [], [], []

        for project in projects.values():
            MQM_values.append(list(MQM.compute_pair(project, 0.9).values()))
            MCI_values.append(list(MCI.compute_pair(project).values()))
            CaT_values.append(list(CA.compute_pair(project).values()))

        MQM_patterns = [len(set(values)) for values in MQM_values]
        MCI_patterns = [len(set(values)) for values in MCI_values]
        CaT_patterns = [len(set(values)) for values in CaT_values]

        possible_connectivity_patterns = [DiscriminativePowerExperiment.
                                          get_number_of_possible_distinct_connectivity_patterns_for_pair_metrics(project)
                                          for project in projects.values()]
        MQM_result = [MQM_patterns[index] / possible_connectivity_patterns[index] for index in range(len(projects))]
        MCI_result = [MCI_patterns[index] / possible_connectivity_patterns[index] for index in range(len(projects))]
        CaT_result = [CaT_patterns[index] / possible_connectivity_patterns[index] for index in range(len(projects))]

        return [MQM_result, MCI_result, CaT_result]

    @staticmethod
    def get_discriminative_power_for_afferent_metrics(projects):
        aMQM_values, aMCI_values, ca_values, ais_values = [], [], [], []

        for project in projects.values():
            aMQM_values.append(list(MQM.compute_single(project, 0.9, True).values()))
            aMCI_values.append(list(MCI.compute_single(project, True).values()))
            ca_values.append(list(CA.compute_single(project).values()))
            ais_values.append(list(AIS.compute_single(project).values()))

        aMQM_patterns = [len(set(values)) for values in aMQM_values]
        aMCI_patterns = [len(set(values)) for values in aMCI_values]
        ca_patterns = [len(set(values)) for values in ca_values]
        ais_patterns = [len(set(values)) for values in ais_values]

        possible_connectivity_patterns = [DiscriminativePowerExperiment.
                                          get_number_of_possible_distinct_connectivity_patterns_for_afferent_metrics(project)
                                          for project in projects.values()]

        aMQM_result = [aMQM_patterns[index] / possible_connectivity_patterns[index] for index in range(len(projects))]
        aMCI_result = [aMCI_patterns[index] / possible_connectivity_patterns[index] for index in range(len(projects))]
        ca_result = [ca_patterns[index] / possible_connectivity_patterns[index] for index in range(len(projects))]
        ais_result = [ais_patterns[index] / possible_connectivity_patterns[index] for index in range(len(projects))]

        return [aMQM_result, aMCI_result, ca_result, ais_result]

    @staticmethod
    def get_discriminative_power_for_efferent_metrics(projects):
        eMQM_values, eMCI_values, ce_values, ads_values = [], [], [], []

        for project in projects.values():
            eMQM_values.append(list(MQM.compute_single(project, 0.9, False).values()))
            eMCI_values.append(list(MCI.compute_single(project, False).values()))
            ce_values.append(list(CE.compute_single(project).values()))
            ads_values.append(list(ADS.compute_single(project).values()))

        eMQM_patterns = [len(set(values)) for values in eMQM_values]
        eMCI_patterns = [len(set(values)) for values in eMCI_values]
        ce_patterns = [len(set(values)) for values in ce_values]
        ads_patterns = [len(set(values)) for values in ads_values]

        possible_connectivity_patterns = [DiscriminativePowerExperiment.
                                          get_number_of_possible_distinct_connectivity_patterns_for_efferent_metrics(project)
                                          for project in projects.values()]

        eMQM_result = [eMQM_patterns[index] / possible_connectivity_patterns[index] for index in range(len(projects))]
        eMCI_result = [eMCI_patterns[index] / possible_connectivity_patterns[index] for index in range(len(projects))]
        ce_result = [ce_patterns[index] / possible_connectivity_patterns[index] for index in range(len(projects))]
        ads_result = [ads_patterns[index] / possible_connectivity_patterns[index] for index in range(len(projects))]

        return [eMQM_result, eMCI_result, ce_result, ads_result]

    @staticmethod
    def get_number_of_possible_distinct_connectivity_patterns_for_pair_metrics(project: Project):
        microservices = project.microservices
        connectivity_patterns = set()

        for microserviceA in microservices.values():
            for microserviceB in microservices.values():
                if microserviceA.is_common_service or microserviceB.is_common_service:
                    continue
                if microserviceA.name != microserviceB.name:
                    if microserviceA.microservice_relations.get(microserviceB.name) is not None:
                        connectivity_patterns.add(str(len(microserviceA.classes)) + "||" + str(len(microserviceB.classes)) +
                                                  "||" + str(len(microserviceA.microservice_relations[microserviceB.name])))
                    else:
                        connectivity_patterns.add(str(len(microserviceA.classes)) + "||" + str(len(microserviceB.classes)) +
                                                  "||" + "0")
        return len(connectivity_patterns)

    @staticmethod
    def get_number_of_possible_distinct_connectivity_patterns_for_afferent_metrics(project: Project):
        microservices = project.microservices
        connectivity_patterns = set()

        for microserviceA in microservices.values():
            if microserviceA.is_common_service:
                continue
            afferent_classes = sum([0 if microservice.microservice_relations.get(microserviceA.name) is None else len(microservice.classes)
                                    for microservice in microservices.values() if microservice.name != microserviceA.name])

            total_CA = sum([0 if microservice.microservice_relations.get(microserviceA.name) is None else len(microservice.microservice_relations[microserviceA.name])
                            for microservice in microservices.values() if microservice.name != microserviceA.name])
            connectivity_patterns.add(str(len(microserviceA.classes)) + "||" + str(afferent_classes) + "||" + str(total_CA))

        return len(connectivity_patterns)

    @staticmethod
    def get_number_of_possible_distinct_connectivity_patterns_for_efferent_metrics(project: Project):
        microservices = project.microservices
        connectivity_patterns = set()

        for microserviceA in microservices.values():
            if microserviceA.is_common_service:
                continue
            efferent_classes = sum([len(project.microservices[m].classes)
                                    for m in microserviceA.microservice_relations.keys()])
            total_CE = sum([len(relations) for relations in microserviceA.microservice_relations.values()])
            connectivity_patterns.add(str(len(microserviceA.classes)) + "||" + str(efferent_classes) + "||" + str(total_CE))

        return len(connectivity_patterns)
