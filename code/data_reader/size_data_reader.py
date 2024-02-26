import os
from typing import Dict, List

import pandas as pd

from code.data_reader.data_reader import get_file_path
from code.types.access_modifier import AccessModifier
from code.types.class_type import Class
from code.types.method import Method
from code.types.microservice import Microservice
from code.types.project import Project


def store_data_about_classes_size(microservices: Dict[str, List[str]], projects: Dict[str, Project],
                                  root_dir='../data') -> None:
    for project_name in os.listdir(root_dir):
        for service in microservices.get(project_name, []):
            file_path = get_file_path(root_dir, project_name, service)
            data = pd.read_excel(file_path, sheet_name="classNumberOfLines", engine="openpyxl")
            microservice = projects[project_name].microservices[service]
            for index, row in data.iterrows():
                process_class_size_data(row, microservice)


def store_data_about_methods_size(microservices: Dict[str, List[str]], projects: Dict[str, Project], root_dir='../data') -> None:
    for project_name in os.listdir(root_dir):
        for service in microservices.get(project_name, []):
            file_path = get_file_path(root_dir, project_name, service)
            data = pd.read_excel(file_path, sheet_name="methodNumberOfLines", engine="openpyxl")
            microservice = projects[project_name].microservices[service]
            for index, row in data.iterrows():
                process_method_size_data(row, microservice)


def process_class_size_data(row: pd.Series, microservice: Microservice) -> None:
    class_name = row.iloc[0]
    number_of_lines = row.iloc[1]

    if class_name not in microservice.classes:
        class_obj = Class(class_name)
        microservice.add_class(class_obj)

    clazz = microservice.classes[class_name]
    clazz.number_of_lines = number_of_lines


def process_method_size_data(row: pd.Series, microservice: Microservice) -> None:
    class_name = row.iloc[0]
    method_signature = row.iloc[1]
    number_of_lines = row.iloc[2]

    clazz = microservice.classes[class_name]

    if method_signature not in clazz.methods:
        clazz.methods[method_signature] = Method(method_signature, "", [], AccessModifier.PUBLIC, "")

    method = clazz.methods[method_signature]
    method.number_of_lines = number_of_lines
