import pandas as pd
import os

from typing import Dict, List

from code.data_reader.data_reader import get_file_path

from code.types.class_type import Class
from code.types.field import Field
from code.types.project import Project


def store_data_about_class_fields(microservices: Dict[str, List[str]], projects: Dict[str, Project],
                                  root_dir='../data') -> None:
    for project_name in os.listdir(root_dir):
        for service in microservices.get(project_name, []):
            file_path = get_file_path(root_dir, project_name, service)
            data = pd.read_excel(file_path, sheet_name="data2", engine="openpyxl")

            microservice = projects[project_name].microservices[service]

            for index, row in data.iterrows():
                process_field_data(row, microservice)


def process_field_data(row: pd.Series, microservice) -> None:
    class_name = row.iloc[0]
    field_name = row.iloc[1]
    field_modifiers = row.iloc[2]
    field_type = row.iloc[3]

    if class_name not in microservice.classes:
        class_obj = Class(class_name)
        microservice.add_class(class_obj)
    else:
        class_obj = microservice.classes[class_name]

    field = Field(field_name, field_type, field_modifiers)
    class_obj.add_field(field)
