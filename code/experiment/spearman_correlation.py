import numpy
import numpy as np
from scipy.stats import stats


class SpearmanCorrelation:
    @staticmethod
    def calculate(data, metrics_names):
        data = np.array(data)
        rho, p_value = stats.spearmanr(data, axis=1)

        if type(rho) == numpy.float64:
            table = np.eye(2)
            table[0, 1] = table[1, 0] = rho
        else:
            table = rho

        print(" " * 10 + " ".join(f"{metric:<10}" for metric in metrics_names))
        for i, row in enumerate(table):
            print(f"{metrics_names[i]:<10}" + " ".join(f"{val:<10.2f}" for val in row))
