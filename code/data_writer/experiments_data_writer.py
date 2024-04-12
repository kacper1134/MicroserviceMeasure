import os

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib
matplotlib.use('Agg')


def write_metrics_correlation_data_to_excel(data, header, sheet_name):
    file_path = '../result/experiments/correlation/tables.xlsx'
    df = pd.DataFrame(data=[row.split("||") for row in data], columns=header)
    if os.path.exists(file_path):
        with pd.ExcelWriter(file_path, mode='a', if_sheet_exists="replace") as writer:
            df.to_excel(writer, sheet_name=sheet_name, index=False)
    else:
        df.to_excel(file_path, sheet_name=sheet_name, index=False)


def write_metrics_pca_data_to_excel(pcas, metric_names, metric_name, alphas):
    file_path = '../result/experiments/pca/tables.xlsx'

    for index, pca in enumerate(pcas):
        data = []
        header = [""]
        header.extend([f"PC{i + 1}" for i in range(len(pca.components_))])

        data.append("Eigenvalues" + "||" + "||".join("{:.2f}".format(eigenvalue) for eigenvalue in pca.explained_variance_))
        data.append("Percentage" + "||" + "||".join("{:.2f}".format(percentage) for percentage in pca.explained_variance_ratio_ * 100))
        data.append("Cumulative Percentage" + "||" + "||".join("{:.2f}".format(cumulative_percentage) for cumulative_percentage in np.cumsum(pca.explained_variance_ratio_ * 100)))
        data.extend([metric_names[index] + "||" + "||".join(["{:.2f}".format(x) for x in pc]) for index, pc in enumerate(pca.components_.T)])

        df = pd.DataFrame(data=[row.split("||") for row in data], columns=header)

        if os.path.exists(file_path):
            with pd.ExcelWriter(file_path, mode='a', if_sheet_exists="replace") as writer:
                df.to_excel(writer, sheet_name=metric_name + "| alpha = " + str(alphas[index]), index=False)
        else:
            df.to_excel(file_path, sheet_name=metric_name + "| alpha = " + str(alphas[index]), index=False)


def wrote_metrics_discriminative_power_data_to_excel(power_for_projects, header, projects, sheet_name):
    file_path = '../result/experiments/discriminative_power/tables.xlsx'
    data = []
    for index, _ in enumerate(projects):
        data.append(projects[index].name + "||" + "||".join("{:.2f}".format(metric[index]) for metric in power_for_projects))

    df = pd.DataFrame(data=[row.split("||") for row in data], columns=header)
    if os.path.exists(file_path):
        with pd.ExcelWriter(file_path, mode='a', if_sheet_exists="replace") as writer:
            df.to_excel(writer, sheet_name=sheet_name, index=False)
    else:
        df.to_excel(file_path, sheet_name=sheet_name, index=False)


def save_correlation_bar_chart(metric_name, metrics_names, data_list: list[str]):
    values = [list(map(float, data.split("||"))) for data in data_list]
    df = pd.DataFrame(values, columns=['Alpha'] + metrics_names)
    ax = df.plot(x="Alpha", kind="bar", figsize=(10, 5))

    ax.set_ylabel('Correlation Value')
    ax.set_title(f'Correlation Values for {metric_name}')
    ax.set_ylim(0, 1)
    ax.set_yticks([i / 5 for i in range(6)])

    plt.savefig(f'../result/experiments/correlation/{metric_name}.png')
    plt.close()
