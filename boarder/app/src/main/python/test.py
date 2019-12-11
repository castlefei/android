import pandas as pd
import numpy as np
from sklearn.decomposition import PCA
from scipy.signal import savgol_filter, find_peaks

import os
def test():
    return os.path.dirname(os.path.realpath(__file__))

def count_peaks(height=0.1):
    filename = "/storage/self/primary/temp.csv"

    with open(filename) as f:
        df = pd.read_csv(filename)

        #dataframe_acc = df.iloc[:, 2:5]
        #dataframe_gyro = df.iloc[:,5:7]

        pca = PCA(n_components=1)
        #acc_pca = pca.fit_transform(dataframe_acc)
        #gyro_pca = pca.fit_transform(dataframe_gyro)
        acc_pca = pca.fit_transform(df.iloc[:, 2:5])

        #acc_smooth = savgol_filter(acc_pca.flatten(), 1, 0)  # window size 51, polynomial order 3
        data_smooth = savgol_filter(acc_pca.flatten(), 1, 0)

        indices = find_peaks(data_smooth, height=height)[0]
        threshold = 0.1
        try:
            threshold = np.min(data_smooth[indices]) * 0.3
        except Exception:
            pass
        if height > 0.1:
            threshold = min(threshold, height)
        threshold = max(threshold, 0.1)
        threshold = min(threshold, 0.2)
        return str(len(indices)) + "," + str(threshold)
