import matplotlib.pyplot as plt
import numpy as np
import random as random
np.random.seed(43)

def sigmoid(x):
    return 1 / (1 + np.exp(-x))

def dsigmoid(x):
    return x * (1 - x)

# class used to pass the learning rate by reference
class ANNLearningRate:
    def __init__(self, rate):
        self.rate = rate

# a little enum emulation
class ReportingTypeEnum:
    def __init__(self):
        self.none = 1
        self.exact = 2
        self.thresholding = 3
        self.classification = 4
ReportingType = ReportingTypeEnum()

class ANNModel:
    def __init__(self, input_layer_size, output_layer_size):
        self.learning_rate = ANNLearningRate(0.2)
        self.input_layer = ANNLayer(input_layer_size, self.learning_rate, None)
        self.hidden_layers = []
        self.hidden_layers.append(ANNLayer(25, self.learning_rate, self.input_layer))
        #self.hidden_layers.append(ANNLayer(25, self.learning_rate, self.hidden_layers[0]))
        self.output_layer = ANNLayer(output_layer_size, self.learning_rate, self.hidden_layers[0])

        self.input_layer.setNextLayer(self.hidden_layers[0])
        #self.hidden_layers[0].setNextLayer(self.hidden_layers[1])
        self.hidden_layers[0].setNextLayer(self.output_layer)

    def evaluate(self, inputs):
        return self.input_layer.evaluate(inputs)

    def splitData(self, output, input, split_fraction):
        firstSize = round(len(output)*split_fraction)
        return output[:firstSize], input[:firstSize], output[firstSize:], input[firstSize:]

    def train(self, expected_results, input_data, num_epochs = 10, report = ReportingType.none, max_layers = 6, hidden_layer_size = 25):
        training_results, training_inputs, testing_results, testing_inputs = self.splitData(expected_results, input_data, 0.9)

        lastTrainingAccuracy = self.computeModelAccuracy(training_results, training_inputs, report)
        lastTestAccuracy = self.computeModelAccuracy(testing_results, testing_inputs, report)

        for i in range(num_epochs):
            #random order:
            order = list(range(training_results.shape[0]))
            random.shuffle(order)
            for j in order:
                self.input_layer.learn(training_results[j], training_inputs[j])
            if (report != ReportingType.none):
                lastTrainingAccuracy = self.computeModelAccuracy(training_results, training_inputs, report)
                newTestAccuracy = self.computeModelAccuracy(testing_results, testing_inputs, report)

                if (newTestAccuracy >= lastTestAccuracy):
                    self.learning_rate.rate += 0.1
                else:
                    self.learning_rate.rate /= 2

                lastTestAccuracy = newTestAccuracy
                print "Accuracy on training set: ", lastTrainingAccuracy
                print "Accuracy on control set: ", lastTestAccuracy

    def computeModelAccuracy(self, expected_results, inputs, reporting_type):
        results = self.evaluate(inputs)

        if reporting_type == ReportingType.classification:
            maxAccuracy = len(results)
            totalAccuracy = 0
            for i in range(len(results)):
                if results[i].argmax() == expected_results[i].argmax():
                    totalAccuracy += 1
            return float(totalAccuracy)/maxAccuracy
        else:
            maxAccuracy = 1
            for dimension in expected_results.shape:
                maxAccuracy *= dimension

            totalAccuracy = 0

            compared_expected = []
            compared_results = []

            for res in np.nditer(expected_results, order='C'):
                compared_expected.append(res)
            for res in np.nditer(results, order='C'):
                compared_results.append(res)

            for i in range(len(compared_results)):
                if (reporting_type == ReportingType.exact):
                    totalAccuracy += 1 - (compared_expected[i] - compared_results[i])**2
                elif (reporting_type == ReportingType.thresholding):
                    if (compared_expected[i] > 0.5 and compared_results[i] > 0.5):
                        totalAccuracy += 1
                    if (compared_expected[i] <= 0.5 and compared_results[i] <= 0.5):
                        totalAccuracy += 1
            return float(totalAccuracy) / maxAccuracy




class ANNLayer:
    def __init__(self, size, model_learning_rate, prev_layer):
        self.size = size
        self.learning_rate = model_learning_rate
        self.next_layer = None
        self.prev_layer = prev_layer
        self.randomizeWeights()

    def randomizeWeights(self):
        if self.prev_layer == None:
            self.weights = None
        else:
            self.weights = np.random.rand(self.size, self.prev_layer.size)

    def setNextLayer(self, next_layer):
        self.next_layer = next_layer

    def evaluate(self, inputs):
        if self.next_layer == None:
            return inputs
        else:
            return self.next_layer.forwardEvaluate(inputs)

    def calculateActivations(self, X):
        activations = X.dot(self.weights.T)
        return sigmoid(activations)

    def forwardEvaluate(self, X):
        activations = self.calculateActivations(X)
        if (self.next_layer != None):
            return self.next_layer.forwardEvaluate(activations)
        return activations

    def calculateDerivativeErrors(self, result, expected):
        return 2*(expected - result)

    def forwardLearn(self, results, inputs):
        activations = self.calculateActivations(inputs)
        errors = None
        if (self.next_layer != None):
            errors = self.next_layer.forwardLearn(results, activations)
        else:
            errors = self.calculateDerivativeErrors(activations, results)

        errors *=  dsigmoid(activations)
        errors_for_prev = self.weights.T.dot(errors)
        for i in range(len(errors)):
            update = errors[i] * inputs
            self.weights[i] += self.learning_rate.rate * update
        return errors_for_prev

    def learn(self, results, inputs):
        if (self.next_layer != None):
            return self.next_layer.forwardLearn(results, inputs)
        return None

    def calculateDerivError(self, y, pred):
        return 2*(y - pred)

    def calculateError(self, y, pred):
        return (np.sum(np.power((y - pred), 2)))


def loadDataset(filename='breast_cancer.csv'):
    my_data = np.genfromtxt(filename, delimiter=',', skip_header=1)

    # The labels of the cases
    # Raw labels are either 4 (cancer) or 2 (no cancer)
    # Normalize these classes to 0/1
    y = (my_data[:, my_data.shape[1]-1] / 2) - 1

    # Case features
    X = my_data[:, :my_data.shape[1]-1]

    # Normalize the features to (0, 1)
    X_norm = X / X.max(axis=0)

    return X_norm, y

def loadIrisDataset(filename='iris.csv'):
    my_data = np.genfromtxt(filename, delimiter=',')

    y = my_data[:, my_data.shape[1]-3:]

    # Case features
    X = my_data[:, :my_data.shape[1]-3]

    # Normalize the features to (0, 1)
    X_norm = X / X.max(axis=0)

    return X_norm, y




def gradientChecker(model, X, y):
    epsilon = 1E-5

    model.weights[1] += epsilon
    out1 = model.forward(X)
    err1 = model.calculateError(y, out1)

    model.weights[1] -= 2*epsilon
    out2 = model.forward(X)
    err2 = model.calculateError(y, out2)

    numeric = (err2 - err1) / (2*epsilon)
    print numeric

    model.weights[1] += epsilon
    out3 = model.forward(X)
    err3 = model.calculateDerivError(y, out3)
    derivs = model.backward(err3)
    print derivs[1]




if __name__=="__main__":
    # X, y = loadIrisDataset()
    # model = ANNModel(X.shape[1], y.shape[1])
    # model.train(y, X, 50, ReportingType.classification)
    
    X, y = loadDataset()
    model = ANNModel(10, 1)
    model.train(y, X, 100, ReportingType.thresholding)

    for i in range(len(X)):
        print model.evaluate(X[i]), y[i]