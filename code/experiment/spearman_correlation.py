import numpy
import numpy as np
from matplotlib import pyplot as plt
from scipy.stats import stats


class SpearmanCorrelation:
    @staticmethod
    def calculate(data):
        data = np.array(data)
        rho, p_value = stats.spearmanr(data, axis=1)

        if type(rho) == numpy.float64:
            table = np.eye(2)
            table[0, 1] = table[1, 0] = rho
        else:
            table = rho

        return table[0, ][1:len(table)]
