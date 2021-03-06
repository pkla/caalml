import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import org.datavec.api.split.NumberedFileInputSplit;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;

public class train extends setup {

    private static final Logger log = LoggerFactory.getLogger(train.class);

    public static void main() throws Exception {

        // ----- Load the training data -----
        SequenceRecordReader trainFeatures = new CSVSequenceRecordReader(0, ",");
        trainFeatures.initialize(new NumberedFileInputSplit(featuresDirTrain.getAbsolutePath() + "/%d.csv", 0, 8002));
        SequenceRecordReader trainLabels = new CSVSequenceRecordReader();
        trainLabels.initialize(new NumberedFileInputSplit(labelsDirTrain.getAbsolutePath() + "/%d.csv", 0, 8002));

        int miniBatchSize = 128;
        int numLabelClasses = 5;
        DataSetIterator trainData = new SequenceRecordReaderDataSetIterator(trainFeatures, trainLabels, miniBatchSize, numLabelClasses,
                false, SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);

        //Normalize the training data
        DataNormalization normalizer = new NormalizerStandardize();
        normalizer.fit(trainData);  //Collect training data statistics
        trainData.reset();

        //Use previously collected statistics to normalize on-the-fly. Each DataSet returned by 'trainData' iterator will be normalized
        trainData.setPreProcessor(normalizer);

        // ----- Load the test data -----
        //Same process as for the training data.
        SequenceRecordReader testFeatures = new CSVSequenceRecordReader(0, ",");
        testFeatures.initialize(new NumberedFileInputSplit(featuresDirTest.getAbsolutePath() + "/%d.csv", 0, 2667));
        SequenceRecordReader testLabels = new CSVSequenceRecordReader();
        testLabels.initialize(new NumberedFileInputSplit(labelsDirTest.getAbsolutePath() + "/%d.csv", 0, 2667));

        DataSetIterator testData = new SequenceRecordReaderDataSetIterator(testFeatures, testLabels, miniBatchSize, numLabelClasses,
                false, SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);

        testData.setPreProcessor(normalizer);   //Note that we are using the exact same normalization process as the training data

        log.info(trainData.next(1).getFeatureMatrix().toString());

        // ----- Configure the network -----
        int lstmLayerSize = 24;
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(123)    //Random number generator seed for improved repeatability. Optional.
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).iterations(1)
                .weightInit(WeightInit.XAVIER)
                .updater(Updater.NESTEROVS)
                .learningRate(0.005)
                .regularization(true)
                .dropOut(0.5)
                .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)  //Not always required, but helps with this data set
                .gradientNormalizationThreshold(0.5)
                .list()
                .layer(0, new GravesLSTM.Builder().activation(Activation.SOFTSIGN).nIn(trainData.inputColumns()).nOut(lstmLayerSize).build())
                .layer( 1, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX).nIn(lstmLayerSize).nOut(numLabelClasses).build())
                .pretrain(false).backprop(true).build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        //net.setListeners(new ScoreIterationListener(10));   //Print the score (loss function value) every 10 iterations

        //Initialize user interface backend
        UIServer uiServer = UIServer.getInstance();

        //Configure storage for network stats
        //File statsFile = new File(userDir + "\\" + new Date().getTime() + "_stats");
        //StatsStorage statsStorage = new FileStatsStorage(statsFile);
        StatsStorage statsStorage = new InMemoryStatsStorage();

        uiServer.attach(statsStorage);
        net.setListeners(new StatsListener(statsStorage));
        //net.setListeners(new PerformanceListener(30));

        // ----- Train the network, evaluating the test set performance at each epoch -----

        int nEpochs = 1000;
        String str = "Test set evaluation at epoch %d: Accuracy = %.2f, F1 = %.2f";
        for (int i = 0; i < nEpochs; i++) {
            net.fit(trainData);

            //Evaluate on the test set:

            Evaluation evaluation = net.evaluate(testData);
            Evaluation evaluation2 = net.evaluate(trainData);
            log.info(evaluation.confusionToString());
            log.info(String.format(str, i, evaluation.accuracy(), evaluation.f1()));
            log.info("TRAIN DATA (DEBUG): " + String.format(str, i, evaluation2.accuracy(), evaluation.f1()));

            testData.reset();
            trainData.reset();
        }

        //Where to save the network. Note: the file is in .zip format - can be opened externally
        File locationToSave = new File(userDir + "\\" + new Date().getTime() + "_RNN.zip");

        //Updater: i.e., the state for Momentum, RMSProp, Adagrad etc. Save this if you want to train your network more in the future
        ModelSerializer.writeModel(net, locationToSave, true);

        log.info("----- Complete -----");
    }
}