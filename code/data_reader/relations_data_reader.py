import os
from typing import Dict, List

import pandas as pd

from code.data_reader.data_reader import get_file_path, get_file_path_for_microservice_structure
from code.data_reader.methods_data_reader import extract_method_signature
from code.types.field_relation import FieldRelation
from code.types.method_relation import MethodRelation
from code.types.microservice import Microservice
from code.types.microservice_relation import MicroserviceRelation
from code.types.project import Project


def store_data_about_method_relations(microservices: Dict[str, List[str]], projects: Dict[str, Project],
                                      root_dir='../data') -> None:
    for relation_type in ["classToClassRelations", "classToInterfaceRelations", "interfaceToClassRelations", "interfaceToInterfaceRelations"]:
        for project_name in os.listdir(root_dir):
            for service in microservices.get(project_name, []):
                file_path = get_file_path(root_dir, project_name, service)
                data = pd.read_excel(file_path, sheet_name=relation_type, engine="openpyxl")

                microservice = projects[project_name].microservices[service]

                for index, row in data.iterrows():
                    process_method_relation_data(row, microservice)


def store_data_about_field_relations(microservices: Dict[str, List[str]], projects: Dict[str, Project],
                                     root_dir='../data') -> None:
    for relation_type in ["fieldClassRelations", "fieldInterfaceRelations"]:
        for project_name in os.listdir(root_dir):
            for service in microservices.get(project_name, []):
                file_path = get_file_path(root_dir, project_name, service)
                data = pd.read_excel(file_path, sheet_name=relation_type, engine="openpyxl")

                microservice = projects[project_name].microservices[service]

                for index, row in data.iterrows():
                    process_field_relation_data(row, microservice)


def store_data_about_microservice_relations(projects: Dict[str, Project],
                                            root_dir='../data') -> None:
    for relation_type in ["feignRelations", "kafkaRelations"]:
        for project_name in os.listdir(root_dir):
            file_path = get_file_path_for_microservice_structure(root_dir, project_name)
            data = pd.read_excel(file_path, sheet_name=relation_type, engine="openpyxl")

            for index, row in data.iterrows():
                process_microservice_relation_data(row, projects[project_name].microservices[row.iloc[0]])


def process_method_relation_data(row: pd.Series, microservice: Microservice) -> None:
    source_class = row.iloc[0]
    source_method, _ = extract_method_signature(row.iloc[1])
    target_class = row.iloc[2]
    target_method, _ = extract_method_signature(row.iloc[3])

    relation = MethodRelation(source_class, source_method, target_class, target_method)

    microservice.classes[source_class].add_method_relation(relation)


def process_field_relation_data(row: pd.Series, microservice: Microservice) -> None:
    source_class = row.iloc[0]
    source_method, _ = extract_method_signature(row.iloc[1])
    target_class = row.iloc[2]
    target_field = row.iloc[3]

    relation = FieldRelation(source_class, source_method, target_class, target_field)

    microservice.classes[source_class].add_field_relation(relation)


def process_microservice_relation_data(row: pd.Series, microservice: Microservice) -> None:
    source_microservice = row.iloc[0]
    source_class = row.iloc[1]
    source_method, _ = extract_method_signature(row.iloc[2])
    target_microservice = row.iloc[3]
    target_class = row.iloc[4]
    target_method, _ = extract_method_signature(row.iloc[5])

    relation = MicroserviceRelation(source_microservice, source_class, source_method, target_microservice, target_class,
                                    target_method)

    microservice.add_microservice_relation(relation)
