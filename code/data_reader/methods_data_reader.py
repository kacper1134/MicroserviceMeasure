import pandas as pd
import os

from typing import Dict, List

from code.data_reader.data_reader import get_file_path
from code.types.class_type import Class
from code.types.method import Method
from code.types.microservice import Microservice
from code.types.project import Project


def store_data_about_class_method(microservices: Dict[str, List[str]], root_dir='../data') -> Dict[str, Project]:
    projects = {}
    for project_name in os.listdir(root_dir):
        project = Project(project_name)

        for service in microservices.get(project_name, []):
            file_path = get_file_path(root_dir, project_name, service)
            data = pd.read_excel(file_path, sheet_name="data1", engine="openpyxl")

            microservice = Microservice(service)

            for index, row in data.iterrows():
                process_method_data(row, microservice)

            project.add_microservice(microservice)
        projects[project_name] = project
    return projects


def store_data_about_interface_method(microservices: Dict[str, List[str]], projects: Dict[str, Project],
                                      root_dir='../data') -> None:
    for project_name in os.listdir(root_dir):
        for service in microservices.get(project_name, []):
            file_path = get_file_path(root_dir, project_name, service)
            data = pd.read_excel(file_path, sheet_name="data5", engine="openpyxl")

            microservice = projects[project_name].microservices[service]

            for index, row in data.iterrows():
                process_method_data(row, microservice)
                microservice.classes[row.iloc[0]].is_interface = True


def process_method_data(row: pd.Series, microservice) -> None:
    class_name = row.iloc[0]
    method_name, parameters = extract_method_signature(row.iloc[1])
    method_modifiers = row.iloc[2]
    return_type = row.iloc[3]

    if class_name not in microservice.classes:
        class_obj = Class(class_name)
        microservice.add_class(class_obj)
    else:
        class_obj = microservice.classes[class_name]

    method = Method(method_name, parameters, method_modifiers, return_type)
    class_obj.add_method(method)


def extract_method_signature(method_signature):
    parts = method_signature.split('(')
    method_name = parts[0]
    parameters_str = parts[1][:-1] if len(parts) > 1 else ''
    parameters = [param.strip() for param in parameters_str.split(',') if param.strip()] if parameters_str else []
    return method_name, parameters
