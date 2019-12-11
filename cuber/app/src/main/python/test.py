import pandas as pd
import numpy as np
from sklearn.decomposition import PCA
from scipy.signal import savgol_filter, find_peaks
import os

# df = pd.DataFrame(columns=['accel_x', 'accel_y', 'accel_z', 'gyro_x','gyro_y','gyro_z'])
df = pd.DataFrame(columns=['accel_x', 'accel_y', 'accel_z'])

def test():
    return os.path.dirname(os.path.realpath(__file__))

def cleardf():
    global df
    df.drop(df.index,inplace=True)

def count_peaks(height):
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

        # acc_squ = np.square(acc_pca)
        # acc_mean = np.mean(acc_squ)

        # if acc_mean > 0.5:
        #     #running
        #     status = 1
        #
        #
        # else:

            

        return str(len(indices)) + "," + str(threshold)




    #
    # global df
    # # df = df.append({'accel_x': ax,'accel_y': ay,'accel_z': az,'gyro_x': gx,'gyro_y': gy,'gyro_z': gz}, ignore_index=True)
    # df = df.append({'accel_x': ax,'accel_y': ay,'accel_z': az}, ignore_index=True)
    # if(df.shape[0] > 10):
    #     x1 = df.values
    #     pca = PCA(n_components=1)
    #     principalComponents = pca.fit_transform(x1)
    #     data_smooth = savgol_filter(principalComponents.flatten(), 1, 0)
    #
    #     indices = find_peaks(data_smooth, height=height)[0]
    #     threshold = 0.1
    #     try:
    #         threshold = np.min(data_smooth[indices]) * 0.3
    #     except Exception:
    #         pass
    #     if height > 0.1:
    #         threshold = min(threshold, height)
    #     threshold = max(threshold, 0.1)
    #     threshold = min(threshold, 0.2)
    #
    #     return str(len(indices)) + "," + str(threshold)
    #
    # return "0,0.1"