import matplotlib.pyplot as plt
import numpy as np
from numbers import Number

np.random.seed(42) # Get the same random numbers every time

class regressionResult:
    def __init__(self, weights, bias):
        self.weights = weights
        self.bias = bias

    def evaluate(self, xs):
        independent = xs
        if isinstance(xs, Number):
            independent = [xs]

        return np.dot(xs, self.weights) + self.bias

    def translateRanges(self, oldIndependentMins, oldIndependentMaxs, oldDependentMin, oldDependentMax,
                        newIndependentMins, newIndependentMaxs, newDependentMin, newDependentMax):
        for i in range (len(self.weights)):
            # math behind it is just plugging the range remap formula to a regression result
            self.weights[i] = self.weights[i]*(oldIndependentMaxs[i] - oldIndependentMins[i])*\
                              (newDependentMax - newDependentMin)/((newIndependentMaxs[i]
                                - newIndependentMins[i])*(oldDependentMax - oldDependentMin))
            self.bias -= self.weights[i]*newIndependentMins[i]
        self.bias += newDependentMin
            
            

    def getCost(self, dependent, independent):
        averageCost = 0

        for varIndex in range(len(dependent)):
            independentVars = independent[varIndex]
            dependentVar = dependent[varIndex]

            averageCost += (self.evaluate(independentVars) - dependentVar)**2 / len(dependent)

        return averageCost

class dataModel:
    def __init__(self, dependent, independent):
        if (len(dependent) > 0):
            singleIndependent = isinstance(independent[0], Number)
            if (singleIndependent):
                self.independent = []

                # if dependent is just an array of numbers, pack them into sub-lists
                for variable in independent:
                    self.independent.append([variable])
            else:
                self.independent = independent

            self.dependent = dependent
        else:
            raise IndexError('No data provided')
        self.prepareNormalizedData()


    @staticmethod
    def remapValueRange(value, oldRangeMin, oldRangeMax, newRangeMin, newRangeMax):
        oldRangeSize = oldRangeMax - oldRangeMin
        newRangeSize = newRangeMax - newRangeMin
        valuePositionInRange = 0
        if oldRangeMin == oldRangeMax:
            valuePositionInRange = 0.5
        else:
            valuePositionInRange = (value - oldRangeMin) / oldRangeSize
        return newRangeMin + newRangeSize * valuePositionInRange

    def prepareDataRange(self):
        self.dependentMaxValue = max(self.dependent)
        self.dependentMinValue = min(self.dependent)
        self.independentMaxValues = []
        self.independentMinValues = []

        # Custom min and max search:
        # initialize with extreme values
        independentVarsCount = len(self.independent[0])
        for i in range(independentVarsCount):
            self.independentMinValues.append(float("inf"))
            self.independentMaxValues.append(-float("inf"))

        for independentVarsSet in self.independent:
            for i in range(independentVarsCount):
                if self.independentMinValues[i] > independentVarsSet[i]:
                    self.independentMinValues[i] = independentVarsSet[i]

                if self.independentMaxValues[i] < independentVarsSet[i]:
                    self.independentMaxValues[i] = independentVarsSet[i]

        # if the range size is 0 increase it to 1
        for i in range(independentVarsCount):
            if self.independentMaxValues[i] == self.independentMinValues[i]:
                self.independentMaxValues[i] += 1
        if (self.dependentMinValue == self.dependentMaxValue):
            self.dependentMaxValue += 1

    def remapDataToNewRange(self):
        independentVarsCount = len(self.independent[0])

        self.normalizedDependent = []
        self.normalizedIndependent = []
        for recordIndex in range(len(self.independent)):
            self.normalizedDependent.append(self.remapValueRange(self.dependent[recordIndex], self.dependentMinValue,
                                                                 self.dependentMaxValue, 0, 1))
            record = []
            for i in range(independentVarsCount):
                record.append(self.remapValueRange(self.independent[recordIndex][i], self.independentMinValues[i],
                                                   self.independentMaxValues[i], 0, 1))
            self.normalizedIndependent.append(record)


    def prepareNormalizedData(self):
        self.prepareDataRange()
        self.remapDataToNewRange()

    def gradienDescent(self, approachRate = 0.01, descentCount = 2000):
        # trying to get a function of form y = a1x1 + a2x2 + ... + anxn + b
        weights = []
        for i in range(len(self.normalizedIndependent[0])):
            weights.append(1/len(self.normalizedIndependent[0]))
        bias = 0.5

        for i in range(descentCount):
            weights_batch = []
            for weightIndex in range(len(self.normalizedIndependent[0])):
                weights_batch.append(0)

            bias_batch = 0

            for varIndex in range(len(self.normalizedIndependent)):
                independentVars = self.normalizedIndependent[varIndex]
                dependentVar = self.normalizedDependent[varIndex]

                # precalculating part of the derivatives 2*(a1x1 + a2x2 + .. + anxn + b - y)/n
                precalculations = approachRate * 2 * (bias + np.dot(independentVars, weights) - dependentVar) / len(self.normalizedDependent)

                # partial derivative of the cost function in the direction of b
                gradient = np.multiply(precalculations, independentVars)

                # moving the a and b coefficients away from the gradient
                bias_batch += precalculations
                weights_batch = np.add(weights_batch, gradient)

            weights = np.subtract(weights, weights_batch)
            bias -= bias_batch

        result = regressionResult(weights, bias)


        # translate the result to original range
        independentMaxsNow = []
        independentMinsNow = []
        for i in range(len(self.normalizedIndependent[0])):
            independentMinsNow.append(0)
            independentMaxsNow.append(1)

        result.translateRanges(independentMinsNow, independentMaxsNow, 0, 1, self.independentMinValues,
                               self.independentMaxValues, self.dependentMinValue, self.dependentMaxValue)
        return result

################################################################
### Load the dataset
my_data = np.genfromtxt('djia_temp.csv', delimiter=';', skip_header=1)
index = my_data[:, 1]
temperature = my_data[:, 2:3]

model = dataModel(index, temperature)
approximation = model.gradienDescent()



################################################################
### Graph the dataset along with the line defined by the model
xs = np.arange(min(temperature), max(temperature))
ys = xs * approximation.weights[0] + approximation.bias

print approximation.getCost(temperature, index)

plt.plot(temperature, index, 'r+', xs, ys, 'g-')
plt.show()