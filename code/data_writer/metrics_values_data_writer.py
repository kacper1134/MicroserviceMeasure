import os

import pandas as pd

from code.types.project import Project


def write_metrics_data_to_excel(project: Project, data, header, sheet_name):
    file_path = '../result/metrics_values/'+project.name+".xlsx"
    df = pd.DataFrame(data=[row.split("||") for row in data], columns=header)
    if os.path.exists(file_path):
        with pd.ExcelWriter(file_path, mode='a', if_sheet_exists="replace") as writer:
            df.to_excel(writer, sheet_name=sheet_name, index=False)
    else:
        df.to_excel(file_path, sheet_name=sheet_name, index=False)
