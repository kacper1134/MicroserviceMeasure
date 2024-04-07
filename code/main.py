import numpy as np

from code.data_reader.fields_data_reader import store_data_about_class_fields
from code.data_reader.relations_data_reader import store_data_about_method_relations, store_data_about_field_relations, \
    store_data_about_microservice_relations
from code.data_reader.size_data_reader import store_data_about_classes_size, store_data_about_methods_size
from code.data_writer.experiments_data_writer import write_metrics_correlation_data_to_excel, \
    save_correlation_bar_chart, write_metrics_pca_data_to_excel
from code.data_writer.metrics_values_data_writer import write_metrics_data_to_excel
from code.experiment.pca_experiment import PcaExperiment
from code.experiment.spearman_correlation import SpearmanCorrelation
from code.measures.ADS import ADS
from code.measures.AIS import AIS
from code.measures.CA import CA
from code.measures.CE import CE
from code.measures.MCI import MCI
from code.measures.MQM import MQM
from code.types.project import Project
from code.data_reader.data_reader import get_list_of_microservices_for_each_project, get_list_of_common_microservices
from code.data_reader.methods_data_reader import store_data_about_class_method, store_data_about_interface_method
from mlxtend.preprocessing import standardize

import warnings
warnings.simplefilter("ignore")


def print_information_about_project(project: Project) -> None:
    number_of_microservices = len(project.microservices)
    number_of_classes = sum([len(ms.classes) for ms in project.microservices.values()])
    number_of_interfaces = len([cls for ms in project.microservices.values() for cls in ms.classes.values()
                                if cls.is_interface])
    number_of_methods = sum([len(cls.methods) for ms in project.microservices.values() for cls in ms.classes.values()])
    number_of_fields = sum([len(cls.fields) for ms in project.microservices.values() for cls in ms.classes.values()])
    number_of_method_relations = sum(len(rel) for ms in project.microservices.values() for cls in ms.classes.values()
                                     for rel in cls.method_relations.values())
    number_of_field_relations = sum(len(rel) for ms in project.microservices.values() for cls in ms.classes.values()
                                    for rel in cls.field_relations.values())
    number_of_microservice_relations = sum(len(rel) for ms in project.microservices.values()
                                           for rel in ms.microservice_relations.values())
    total_size_of_project = sum([cls.number_of_lines for ms in project.microservices.values() for cls in ms.classes.values()])
    total_size_of_methods = sum([method.number_of_lines for ms in project.microservices.values() for cls in ms.classes.values()
                                 for method in cls.methods.values()])

    print(f"Project: {project.name}")
    print(f"Number of microservices: {number_of_microservices}")
    print(f"Number of classes: {number_of_classes}")
    print(f"Number of interfaces: {number_of_interfaces}")
    print(f"Number of methods: {number_of_methods}")
    print(f"Number of fields: {number_of_fields}")
    print(f"Number of method relations: {number_of_method_relations}")
    print(f"Number of field relations: {number_of_field_relations}")
    print(f"Number of microservice relations: {number_of_microservice_relations}")
    print(f"Total size of methods and fields: {total_size_of_methods + number_of_fields}")
    print(f"Total size of project: {total_size_of_project}")
    print()


def store_data_about_projects():
    microservices = get_list_of_microservices_for_each_project()
    projects = store_data_about_class_method(microservices)

    store_data_about_interface_method(microservices, projects)
    store_data_about_class_fields(microservices, projects)
    store_data_about_method_relations(microservices, projects)
    store_data_about_field_relations(microservices, projects)
    store_data_about_microservice_relations(projects)
    store_data_about_classes_size(microservices, projects)
    store_data_about_methods_size(microservices, projects)

    return projects


def fix_incorrect_classes(projects):
    for project in projects.values():
        for microservice in project.microservices.values():
            microservice.fix_incorrect_classes()


def store_data_about_common_microservices(projects):
    for project in projects.values():
        common_microservices = get_list_of_common_microservices(project.name)
        project.change_microservices_to_common(common_microservices)
        project.add_classes_from_common_microservices(common_microservices)
        project.delete_microservice_relations_to_common()
        project.add_common_relations_to_method_relations()
        project.add_inverted_relations()


def show_metrics_values(projects):
    for project in projects.values():
        print("*" * 140)
        print("Project: ", project.name)
        efferent = MQM.compute_single(project, 0.9, False)
        afferent = MQM.compute_single(project, 0.9, True)

        standard_efferent = CE.compute_single(project)
        standard_afferent = CA.compute_single(project)

        print("{:<20} {:<30} {:<30} {:<30} {:<30}".format("Microservice", "Efferent", "Afferent", "Standard Efferent", "Standard Afferent"))
        print("-" * 140)
        for ms in project.microservices.values():
            if ms.is_common_service:
                continue
            efferent_value = efferent.get(ms.name, "-")
            afferent_value = afferent.get(ms.name, "-")
            standard_efferent_value = standard_efferent.get(ms.name, "-")
            standard_afferent_value = standard_afferent.get(ms.name, "-")

            print("{:<20} {:<30} {:<30} {:<30} {:<30}".format(ms.name, efferent_value, afferent_value, standard_efferent_value, standard_afferent_value))

        print()

        pair_measure = MQM.compute_pair(project, 0.9)
        print("{:<40} {:<30}".format("Pair", "Value"))
        print("-" * 80)
        for pair, value in pair_measure.items():
            microservices = pair.split("->")
            print("{:<40} {:<30}".format(microservices[0] + ", " + microservices[1], value))

        print("*" * 80)
        print()


def calculate_metrics_correlation(projects, alpha_values):
    MQM_result, eMQM_result, aMQM_result = [], [], []
    full_MQM_result, full_eMQM_result, full_aMQM_result = [], [], []

    for alpha in alpha_values:
        MQM_values, MCI_values, CaT_values = [], [], []
        eMQM, eMCI, ce, ads = [], [], [], []
        aMQM, aMCI, ca, ais = [], [], [], []

        for project in projects.values():
            MQM_values.extend(list(MQM.compute_pair(project, alpha).values()))
            MCI_values.extend(list(MCI.compute_pair(project).values()))
            CaT_values.extend(list(CA.compute_pair(project).values()))

            eMQM.extend(list(MQM.compute_single(project, alpha, False).values()))
            eMCI.extend(list(MCI.compute_single(project, False).values()))
            ce.extend(list(CE.compute_single(project).values()))
            ads.extend(list(ADS.compute_single(project).values()))

            aMQM.extend(list(MQM.compute_single(project, alpha, True).values()))
            aMCI.extend(list(MCI.compute_single(project, True).values()))
            ca.extend(list(CA.compute_single(project).values()))
            ais.extend(list(AIS.compute_single(project).values()))

        pair_metrics_values = [MQM_values, MCI_values, CaT_values]
        efferent_metrics_values = [eMQM, eMCI, ce, ads]
        afferent_metrics_values = [aMQM, aMCI, ca, ais]

        pair_correlation, pair_p = SpearmanCorrelation.calculate(pair_metrics_values)
        efferent_correlation, efferent_p = SpearmanCorrelation.calculate(efferent_metrics_values)
        afferent_correlation, afferent_p = SpearmanCorrelation.calculate(afferent_metrics_values)

        MQM_result.append(str(alpha) + "||" + "||".join(list(map(str, pair_correlation))))
        eMQM_result.append(str(alpha) + "||" + "||".join(list(map(str, efferent_correlation))))
        aMQM_result.append(str(alpha) + "||" + "||".join(list(map(str, afferent_correlation))))

        full_MQM_result.append(str(alpha) + "||" + "||".join(list(map(str, pair_correlation))) + "||" + "||".join(list(map(str, pair_p))))
        full_eMQM_result.append(str(alpha) + "||" + "||".join(list(map(str, efferent_correlation))) + "||" + "||".join(list(map(str, efferent_p))))
        full_aMQM_result.append(str(alpha) + "||" + "||".join(list(map(str, afferent_correlation))) + "||" + "||".join(list(map(str, afferent_p))))

    write_metrics_correlation_data_to_excel(full_MQM_result, ["alpha", "MCI", "CaT", "p MCI", "p CaT"], "MQM")
    write_metrics_correlation_data_to_excel(full_eMQM_result, ["alpha", "eMCI", "CE", "ADS", "p eMCI", "p CE", "p ADS"], "eMQM")
    write_metrics_correlation_data_to_excel(full_aMQM_result, ["alpha", "aMCI", "CA", "AIS", "p aMCI", "p CA", "p AIS"], "aMQM")

    save_correlation_bar_chart("MQM", ["MCI", "CaT"], MQM_result)
    save_correlation_bar_chart("eMQM", ["eMCI", "CE", "ADS"], eMQM_result)
    save_correlation_bar_chart("aMQM", ["aMCI", "CA", "AIS"], aMQM_result)
    print("Correlation data saved to excel")


def calculate_pca_for_metrics(projects, alpha_values):
    pair_result, single_result = [], []

    for alpha in alpha_values:
        MQM_values, MCI_values, CaT_values = [], [], []
        eMQM, eMCI, ce, ads = [], [], [], []
        aMQM, aMCI, ca, ais = [], [], [], []

        for project in projects.values():
            MQM_values.extend(list(MQM.compute_pair(project, alpha).values()))
            MCI_values.extend(list(MCI.compute_pair(project).values()))
            CaT_values.extend(list(CA.compute_pair(project).values()))

            eMQM.extend(list(MQM.compute_single(project, alpha, False).values()))
            eMCI.extend(list(MCI.compute_single(project, False).values()))
            ce.extend(list(CE.compute_single(project).values()))
            ads.extend(list(ADS.compute_single(project).values()))

            aMQM.extend(list(MQM.compute_single(project, alpha, True).values()))
            aMCI.extend(list(MCI.compute_single(project, True).values()))
            ca.extend(list(CA.compute_single(project).values()))
            ais.extend(list(AIS.compute_single(project).values()))

        single_result.append(PcaExperiment.perform_pca(standardize(np.array([eMQM, aMQM, eMCI, aMCI, ce, ca, ads, ais]).transpose())))
        pair_result.append(PcaExperiment.perform_pca(standardize(np.array([MCI_values, MQM_values, CaT_values]).transpose())))

    write_metrics_pca_data_to_excel(single_result, ["eMQM", "aMQM", "eMCI", "aMCI", "CE", "CA", "ADS", "AIS"], "Single", alpha_values)
    write_metrics_pca_data_to_excel(pair_result, ["MCI", "MQM", "CaT"], "Pair", alpha_values)
    print("PCA data saved to excel")


def write_metrics_values_to_file(projects):
    for project in projects.values():
        CA_value = CA.compute_single(project)
        CE_value = CE.compute_single(project)
        AIS_value = AIS.compute_single(project)
        ADS_value = ADS.compute_single(project)

        MQM_value = MQM.compute_pair(project, 0.9)
        MCI_value = MCI.compute_pair(project)

        aMCI_value = MCI.compute_single(project, True)
        aMQM_value = MQM.compute_single(project, 0.9, True)
        eMCI_value = MCI.compute_single(project, False)
        eMQM_value = MQM.compute_single(project, 0.9, False)

        single_microservice_data = []
        for microservice in CA_value.keys():
            single_microservice_data_row = microservice + "||" + str(CA_value[microservice]) + "||" + str(CE_value[microservice]) \
                                           + "||" + str(AIS_value[microservice]) + "||" + str(ADS_value[microservice]) + "||" \
                                           + str(aMCI_value[microservice]) + "||" + str(eMCI_value[microservice]) + "||" + \
                                           str(aMQM_value[microservice]) + "||" + str(eMQM_value[microservice])
            single_microservice_data.append(single_microservice_data_row)

        single_microservice_header = ["Microservice", "CA", "CE", "ADS", "AIS", "aMCI", "eMCI", "aMQM", "eMQM"]
        write_metrics_data_to_excel(project, single_microservice_data, single_microservice_header, "Single Microservice")

        pair_microservices_data = []
        for microservice_pair in MQM_value.keys():
            microservices = microservice_pair.split("->")
            microserviceA = microservices[0]
            microserviceB = microservices[1]

            pair_microservices_data_row = microserviceA + "||" + microserviceB + "||" \
                                          + str(MQM_value[microservice_pair]) + "||" \
                                          + str(MCI_value[microservice_pair])
            pair_microservices_data.append(pair_microservices_data_row)

        pair_microservices_header = ["Source Microservice", "Destination Microservice", "MQM", "MCI"]
        write_metrics_data_to_excel(project, pair_microservices_data, pair_microservices_header, "Pair Microservices")


def main():
    projects = store_data_about_projects()
    fix_incorrect_classes(projects)
    store_data_about_common_microservices(projects)

    #write_metrics_values_to_file(projects)
    calculate_metrics_correlation(projects, [0, 0.2, 0.5, 0.7, 0.9, 1.0])
    calculate_pca_for_metrics(projects, [0, 0.2, 0.5, 0.7, 0.9, 1.0])


if __name__ == "__main__":
    main()
