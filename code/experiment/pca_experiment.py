from sklearn.decomposition import PCA
import numpy as np


class PcaExperiment:
    @staticmethod
    def perform_pca(data, metric_names):
        pca = PCA()
        pca.fit(data)

        eigenvalues = pca.explained_variance_
        percentage = pca.explained_variance_ratio_ * 100
        cumulative_percentage = np.cumsum(percentage)
        principal_components = pca.components_

        pc_header = "   ".join([f"PC{i + 1:<10}" for i in range(len(principal_components))])
        print(f"{'':>25}{pc_header}")

        print(f"{'Eigenvalues':<24}", "   ".join([f"{x:<12.2f}" for x in eigenvalues]))
        print(f"{'Percentage':<24}", "   ".join([f"{x:<12.2f}" for x in percentage]))
        print(f"{'Cumulative Percentage':<24}", "   ".join([f"{x:<12.2f}" for x in cumulative_percentage]))
        print()
        index = 0
        for pc in principal_components.T:
            pc_values = "   ".join([f"{x:<12.2f}" for x in pc])
            print(f"{metric_names[index]:<24}", pc_values)
            index += 1
