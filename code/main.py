from code.data_reader.fields_data_reader import store_data_about_class_fields
from code.data_reader.relations_data_reader import store_data_about_method_relations, store_data_about_field_relations, \
    store_data_about_microservice_relations
from code.data_reader.size_data_reader import store_data_about_classes_size, store_data_about_methods_size
from code.measures.MSM import MSM
from code.types.project import Project
from code.data_reader.data_reader import get_list_of_microservices_for_each_project, get_list_of_common_microservices
from code.data_reader.methods_data_reader import store_data_about_class_method, store_data_about_interface_method

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


def main():
    projects = store_data_about_projects()
    fix_incorrect_classes(projects)
    store_data_about_common_microservices(projects)

    for project in projects.values():
        print("*" * 80)
        print("Project: ", project.name)
        efferent = MSM.compute_single(project, 0.9, False)
        afferent = MSM.compute_single(project, 0.9, True)

        print("{:<20} {:<30} {:<30}".format("Microservice", "Efferent", "Afferent"))
        print("-" * 80)
        for ms in project.microservices.values():
            if ms.is_common_service:
                continue
            efferent_value = efferent.get(ms.name, "-")
            afferent_value = afferent.get(ms.name, "-")
            print("{:<20} {:<30} {:<30}".format(ms.name, efferent_value, afferent_value))

        print()

        pair_measure = MSM.compute_pair(project, 0.9)
        print("{:<40} {:<30}".format("Pair", "Value"))
        print("-" * 80)
        for pair, value in pair_measure.items():
            microservices = pair.split("->")
            print("{:<40} {:<30}".format(microservices[0] + ", " + microservices[1], value))

        print("*" * 80)
        print()


if __name__ == "__main__":
    main()
