import random
import numpy as np

class RatingRecord:
    def __init__(self, line):
        arguments = line.split()
        self.userId = int(arguments[0])
        # normalize to count from 0
        self.movieId = int(arguments[1])-1
        self.rating = int(arguments[2])

class MovieModel:
    def __init__(self):
        self.records = []

    def readFromFile(self, fileName):
        with open(fileName) as f:
            lines = f.read().splitlines()
            for line in lines:
                self.records.append(RatingRecord(line))
            self.normalizeData()

    def normalizeData(self):
        user_total_rating = {}
        user_rating_count = {}

        movie_total_rating = {}
        movie_rating_count = {}

        user_id_map = {}
        nextId = 0

        self.max_rating = 0
        self.min_rating = float("inf")

        for record in self.records:
            if record.rating > self.max_rating:
                self.max_rating = record.rating
            if record.rating < self.min_rating:
                self.min_rating = record.rating

            if record.userId not in user_id_map:
                user_id_map[record.userId] = nextId
                record.userId = nextId
                nextId += 1
            else:
                record.userId = user_id_map[record.userId]

            if record.userId in user_total_rating:
                user_total_rating[record.userId] += record.rating
                user_rating_count[record.userId] += 1
            else:
                user_total_rating[record.userId] = record.rating
                user_rating_count[record.userId] = 1

            if record.movieId in movie_total_rating:
                movie_total_rating[record.movieId] += record.rating
                movie_rating_count[record.movieId] += 1
            else:
                movie_total_rating[record.movieId] = record.rating
                movie_rating_count[record.movieId] = 1

        self.dimensions_count = nextId
        self.average_movie_score = {}
        self.average_user_score = {}

        for movieId in movie_total_rating:
            self.average_movie_score[movieId] = float(movie_total_rating[movieId]) / movie_rating_count[movieId]
        for userId in user_total_rating:
            self.average_user_score[userId] = float(user_total_rating[userId]) / user_rating_count[userId]

        self.prepareVectors()

    def prepareVectors(self):
        self.movies = np.empty((len(self.average_movie_score), len(self.average_user_score)))
        self.movies.fill(0)

        for record in self.records:
            self.movies[record.movieId][record.userId] = record.rating

        for movieId in range(len(self.average_movie_score)):
            for userId in range(len(self.average_user_score)):
                if self.movies[movieId][userId] == 0:
                    self.movies[movieId][userId] = self.defaultScore(movieId,userId)

    def defaultScore(self, movieId, userId):
        return (self.average_movie_score[movieId] * 0.5
         + self.average_user_score[userId] * 0.5)

    def distance_squared(self, centroid, movieId):
        total = np.sum((centroid - self.movies[movieId])**2)
        return total

    def cluster(self, cluster_count, iterations = 50):
        centroids = []
        clusters = []
        for i in range(cluster_count):
            clusters.append([])
            centroid = []
            for j in range(self.dimensions_count):
                centroid.append(random.uniform(self.min_rating, self.max_rating))
            centroids.append(centroid)

        for i in range(iterations):
            print i
            # empty the clusters
            for j in range(cluster_count):
                clusters[j] = []

            for movieId in range(len(self.average_movie_score)):
                shortest_distance = float("inf")
                closest_centroid = -1
                for j in range(cluster_count):
                    distance = self.distance_squared(centroids[j], movieId)
                    if distance < shortest_distance:
                        shortest_distance = distance
                        closest_centroid = j
                clusters[closest_centroid].append(movieId)

            centroids = self.calculate_new_centroids(clusters, centroids)

        return clusters, centroids

    def calculate_new_centroids(self, clusters, old_centroids):
        centroids = []

        centroid_index = 0
        for cluster in clusters:

            count = len(cluster)

            if count == 0:
                centroids.append(old_centroids[centroid_index])
            else:
                total = np.empty((self.dimensions_count))
                total.fill(0)
                for movieId in cluster:
                    total += self.movies[movieId]
                total /= count

                centroids.append(total)
        return centroids

def printClusters(clusters, uItemPath):
    dict = {}
    with open(uItemPath) as f:
        lines = f.read().splitlines()
        for line in lines:
            elements = line.split("|")
            dict[int(elements[0])-1] = elements[1]
    for cluster in clusters:
        print("----------------------------------------------------")
        for movie in cluster:
            print(dict[movie])

model = MovieModel()
model.readFromFile("u.data")
clusters, centroids = model.cluster(50, 10)
printClusters(clusters, "u.item")