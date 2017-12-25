import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class transform extends setup{
    //This method converts the "one time series per line" format into a suitable
    //CSV sequence format that DataVec (CsvSequenceRecordReader) and DL4J can read.
    public static void main() throws Exception {
        String data = IOUtils.toString(new FileInputStream(new File(rawDataFileName)));
        String[] lines = data.split(";\r\n");

        if (baseTrainDir.exists() && baseTestDir.exists()) {
            clearDirectory(Paths.get(baseTrainDir.getAbsolutePath()));
            clearDirectory(Paths.get(baseTestDir.getAbsolutePath()));
        }

        //Create directories
        baseDir.mkdir();
        baseTrainDir.mkdir();
        featuresDirTrain.mkdir();
        labelsDirTrain.mkdir();
        baseTestDir.mkdir();
        featuresDirTest.mkdir();
        labelsDirTest.mkdir();

        int window = 100;

        int lineCount = 0;
        int segmentLineCount = 0;
        int brokenLineCount = 0;
        int currSegment = 0;

        DataSet dataset = new DataSet();

        List<Integer> aSegmentTemp = new ArrayList<>();
        List<Float> xSegmentTemp = new ArrayList<>();
        List<Float> ySegmentTemp = new ArrayList<>();
        List<Float> zSegmentTemp = new ArrayList<>();

        // build dataframes
        for (String line : lines) {

            String[] s = line.split(",");

            if (line.contains("0,0,0") || s.length > 6 || s.length < 5) {
                brokenLineCount++;
            } else {
                try {
                    xSegmentTemp.add(Float.parseFloat(s[3]));
                    ySegmentTemp.add(Float.parseFloat(s[4]));
                    zSegmentTemp.add(Float.parseFloat(s[5]));
                    aSegmentTemp.add(map.get(s[1]));
                    lineCount++;
                    segmentLineCount++;
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println(lineCount);
                }
            }

            if (segmentLineCount == window) {

                int firstActivity = aSegmentTemp.get(0);
                int lastActivity = aSegmentTemp.get(aSegmentTemp.size() - 1);
                if (firstActivity == lastActivity) {
                    currSegment++;
                    Segment segment = new Segment(xSegmentTemp, ySegmentTemp, zSegmentTemp, firstActivity);
                    dataset.addSegment(DeepCopy(segment));
                }

                segmentLineCount = 0;
                aSegmentTemp.clear();
                xSegmentTemp.clear();
                ySegmentTemp.clear();
                zSegmentTemp.clear();

            }

        }

        List<Segment> segments = dataset.getSegments();

        Collections.shuffle(segments, new Random(12345));

        // outputs segments to files
        int nTrain = (int) (segments.size() * 0.75);
        int nTest = (int) (segments.size() * 0.25);
        int trainCount = 0;
        int testCount = 0;

        for (Segment segment : segments) {
            File outPathFeatures;
            File outPathLabels;
            if (trainCount < nTrain) {
                outPathFeatures = new File(featuresDirTrain, trainCount + ".csv");
                outPathLabels = new File(labelsDirTrain, trainCount + ".csv");
                trainCount++;
            } else {
                outPathFeatures = new File(featuresDirTest, testCount + ".csv");
                outPathLabels = new File(labelsDirTest, testCount + ".csv");
                testCount++;
            }

            StringBuilder featureLine = new StringBuilder();
            StringBuilder labelLine = new StringBuilder();

            labelLine.append(segment.getLabel());

            for (int i = 0; i < segment.getFeatures().get(0).size(); i++) {
                try {
                    featureLine.append(segment.getFeatures().get(0).get(i));
                    featureLine.append(",");
                    featureLine.append(segment.getFeatures().get(1).get(i));
                    featureLine.append(",");
                    featureLine.append(segment.getFeatures().get(2).get(i));
                    featureLine.append(System.getProperty("line.separator"));
                } catch (IndexOutOfBoundsException e) {
                    return;
                }
            }

            FileUtils.writeStringToFile(outPathFeatures, featureLine.toString());
            FileUtils.writeStringToFile(outPathLabels, labelLine.toString());

            featureLine.setLength(0);
            labelLine.setLength(0);
        }

        System.out.println(nTrain + " training examples and " + nTest + " test examples of length " + window);
        System.out.println("End WISDM Transformation");
    }

    static class Segment implements Serializable {

        private List<Float> xSegment;
        private List<Float> ySegment;
        private List<Float> zSegment;
        private int aSegment;

        Segment(List<Float> xSegmentTemp, List<Float> ySegmentTemp, List<Float> zSegmentTemp, int aSegmentTemp) {
            this.aSegment = aSegmentTemp;
            this.xSegment = xSegmentTemp;
            this.ySegment = ySegmentTemp;
            this.zSegment = zSegmentTemp;
        }

        List<List<Float>> getFeatures() {
            List<List<Float>> features = new ArrayList<>();
            features.add(xSegment);
            features.add(ySegment);
            features.add(zSegment);

            return features;
        }

        int getLabel() {
            return aSegment;
        }
    }

    static class DataSet {
        List<Segment> segments = new ArrayList<>();

        void addSegment(Segment segment) {
            this.segments.add(segment);
        }

        List<Segment> getSegments() {
            return segments;
        }
    }

     private static Segment DeepCopy(Segment input) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(input);
        oos.flush();
        oos.close();
        bos.close();
        byte[] byteData = bos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(byteData);
        return((Segment) new ObjectInputStream(bais).readObject());
    }

    static void clearDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
