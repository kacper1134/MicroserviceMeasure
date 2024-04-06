import os

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
