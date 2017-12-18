import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class setup {

    static URL url;
    static String userDir;
    static String dataDir;
    static String downloadedFileName;
    static File downloadedFile;
    static File uncompressedFolder;
    static String labelsFileName;
    static String modifiedLabelsFileName;
    static String rawDataFileName;
    static Map<String, Integer> map = new HashMap<>();

    static File baseDir;
    static File baseTrainDir;
    static File featuresDirTrain;
    static File labelsDirTrain;
    static File baseTestDir;
    static File featuresDirTest;
    static File labelsDirTest;


    public static void main (String args [] ) throws Exception {
        userDir = System.getProperty("user.dir");
        downloadedFileName = userDir + "\\WISDM_ar_latest.tar.gz";
        downloadedFile = new File(downloadedFileName);
        uncompressedFolder = new File(userDir + "\\WISDM_ar_latest");
        dataDir = userDir + "\\WISDM_ar_latest\\WISDM_ar_v1.1";

        //'baseDir': Base directory for the data. Change this if you want to save the data somewhere else
        baseDir = new File(dataDir);
        baseTrainDir = new File(baseDir, "train");
        featuresDirTrain = new File(baseTrainDir, "features");
        labelsDirTrain = new File(baseTrainDir, "labels");
        baseTestDir = new File(baseDir, "test");
        featuresDirTest = new File(baseTestDir, "features");
        labelsDirTest = new File(baseTestDir, "labels");

        map.put("Sitting", 0);
        map.put("Standing", 1);
        map.put("Walking", 2);
        map.put("Jogging", 3);
        map.put("Downstairs", 4);
        map.put("Upstairs", 5);

        //wisdmget.main();
        //transform.main();
        train.main();
    }
}