from sklearn.decomposition import PCA


class PcaExperiment:
    @staticmethod
    def perform_pca(data):
        pca = PCA()
        pca.fit(data)
        return pca
