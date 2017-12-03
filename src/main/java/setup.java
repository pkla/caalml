import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class setup {

    protected static URL url;
    public static String userDir;
    protected static String dataDir;
    protected static String downloadedFileName;
    protected static File downloadedFile;
    public static File uncompressedFolder;
    protected static String labelsFileName;
    protected static String modifiedLabelsFileName;
    protected static String rawDataFileName;
    public static Map<String, Integer> map = new HashMap<>();


    public static void main (String args [] ) throws Exception {
        userDir = System.getProperty("user.dir");
        downloadedFileName = userDir + "\\WISDM_ar_latest.tar.gz";
        downloadedFile = new File(downloadedFileName);
        uncompressedFolder = new File(userDir + "\\WISDM_ar_latest");
        dataDir = userDir + "\\WISDM_ar_latest\\WISDM_ar_v1.1";

        map.put("Sitting", 1);
        map.put("Standing", 2);
        map.put("Walking", 3);
        map.put("Jogging", 4);
        map.put("Downstairs", 5);
        map.put("Upstairs", 6);

        wisdmget.main();
        wisdmpreprocessor.main();
    }
}
