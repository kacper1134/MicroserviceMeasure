import os


def get_list_of_microservices_for_each_project(root_dir='../data'):
    microservices = {}

    for project_dir in os.listdir(root_dir):
        svc_info_path = os.path.join(root_dir, project_dir, 'svc_info.txt')

        if os.path.isfile(svc_info_path):
            with open(svc_info_path, 'r') as file:
                services = file.read().strip().split(',')
                microservices[project_dir] = services

    return microservices


def get_file_path(root_dir: str, project_name: str, service: str) -> str:
    return f"{root_dir}/{project_name}/{service}_structure.xlsx"


def get_file_path_for_microservice_structure(root_dir: str, project_name: str) -> str:
    return f"{root_dir}/{project_name}/{project_name}_structure.xlsx"
