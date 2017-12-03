import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

public class wisdmget extends setup {

    public static void main() throws Exception {
        try
        {
            url = new URL("http://www.cis.fordham.edu/wisdm/includes/datasets/latest/WISDM_ar_latest.tar.gz");

            saveFile(url, downloadedFileName);
            createLabels();

            rawDataFileName = dataDir + "\\WISDM_ar_v1.1_raw.txt";

        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void createLabels() throws IOException {
        // todo, finish implementing label generation code
        labelsFileName = dataDir + "\\labels.txt";
        modifiedLabelsFileName = dataDir + "\\modified_labels.txt";
        Files.write(Paths.get(labelsFileName),
                Collections.singletonList("User, Activity, Time, X, Y, Z"),
                Charset.forName("UTF-8"));

        Files.write(Paths.get(modifiedLabelsFileName),
                Collections.singletonList("User, Activity, Time, Magnitude"),
                Charset.forName("UTF-8"));

        System.out.println("Created labels\n" + labelsFileName + "\n" + modifiedLabelsFileName);
    }

    public static void saveFile(URL url, String file) throws IOException {
        if (uncompressedFolder.exists()) return; //if data already downloaded and unzipped, don't download again

        System.out.println("opening connection");
        InputStream in = url.openStream();
        FileOutputStream fos = new FileOutputStream(new File(file));

        System.out.println("reading file...");
        int length = -1;
        byte[] buffer = new byte[1024];// buffer for portion of data from
        // connection
        while ((length = in.read(buffer)) > -1) {
            fos.write(buffer, 0, length);
        }

        fos.close();
        in.close();
        System.out.println("file was downloaded");

        uncompressTarGZ(downloadedFile, uncompressedFolder);
    }

    public static void uncompressTarGZ(File tarFile, File dest) throws IOException {
        dest.mkdir();
        TarArchiveInputStream tarIn = null;

        tarIn = new TarArchiveInputStream(
                new GzipCompressorInputStream(
                        new BufferedInputStream(
                                new FileInputStream(
                                        tarFile
                                )
                        )
                )
        );

        TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
        // tarIn is a TarArchiveInputStream
        while (tarEntry != null) { // create a file with the same name as the tarEntry
            File destPath = new File(dest, tarEntry.getName());
            System.out.println("working: " + destPath.getCanonicalPath());
            if (tarEntry.isDirectory()) {
                destPath.mkdirs();
            } else {
                destPath.createNewFile();
                //byte [] btoRead = new byte[(int)tarEntry.getSize()];
                byte [] btoRead = new byte[1024];
                //FileInputStream fin
                //  = new FileInputStream(destPath.getCanonicalPath());
                BufferedOutputStream bout =
                        new BufferedOutputStream(new FileOutputStream(destPath));
                int len = 0;

                while((len = tarIn.read(btoRead)) != -1)
                {
                    bout.write(btoRead,0,len);
                }

                bout.close();
                btoRead = null;

            }
            tarEntry = tarIn.getNextTarEntry();
        }
        tarIn.close();
        System.out.println("file was uncompressed");
    }
}