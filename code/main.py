from code.data_reader.fields_data_reader import store_data_about_class_fields
from code.data_reader.relations_data_reader import store_data_about_method_relations, store_data_about_field_relations, \
    store_data_about_microservice_relations
from code.types.project import Project
from code.data_reader.data_reader import get_list_of_microservices_for_each_project
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

    print(f"Project: {project.name}")
    print(f"Number of microservices: {number_of_microservices}")
    print(f"Number of classes: {number_of_classes}")
    print(f"Number of interfaces: {number_of_interfaces}")
    print(f"Number of methods: {number_of_methods}")
    print(f"Number of fields: {number_of_fields}")
    print(f"Number of method relations: {number_of_method_relations}")
    print(f"Number of field relations: {number_of_field_relations}")
    print(f"Number of microservice relations: {number_of_microservice_relations}")
    print()


def main():
    microservices = get_list_of_microservices_for_each_project()
    projects = store_data_about_class_method(microservices)

    store_data_about_interface_method(microservices, projects)
    store_data_about_class_fields(microservices, projects)
    store_data_about_method_relations(microservices, projects)
    store_data_about_field_relations(microservices, projects)
    store_data_about_microservice_relations(projects)

    for project in projects.values():
        print_information_about_project(project)


if __name__ == "__main__":
    main()
