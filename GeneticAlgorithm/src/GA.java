import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.*;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.*;
import java.util.ArrayList;
import java.lang.InterruptedException;
import java.util.Random;

// Each MyPolygon has a color and a Polygon object
class MyPolygon {

    Polygon polygon;
    Color color;

    public MyPolygon(Polygon _p, Color _c) {
        polygon = _p;
        color = _c;
    }

    public Color getColor() {
        return color;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public static MyPolygon createRandom(int width, int height, int maxVertices) {
        Random generator = new Random();

        int[] xPoints = new int[maxVertices];
        int[] yPoints = new int[maxVertices];
        for (int i = 0; i < maxVertices; ++i) {
            xPoints[i] = generator.nextInt(width);
            yPoints[i] = generator.nextInt(height);
        }
        Polygon polygon = new Polygon(xPoints, yPoints, maxVertices);
        Color color = new Color(generator.nextInt(256), generator.nextInt(256), generator.nextInt(256));

        return new MyPolygon(polygon, color);
    }
}


// Each GASolution has a list of MyPolygon objects
class GASolution {
    int COLOR_MUTATION_RANGE = 80;
    double POSITION_MUTATION_RATIO = 0.4;
    double POSITION_MUTATION_CHANCE = 0.7;
    double MUTATION_RATE = 0.008;

    ArrayList<MyPolygon> shapes;

    // width and height are for the full resulting image
    int width, height;

    double distance = 0;
    double fitness = 0;

    public static GASolution createRandom(int width, int height, int maxPolygonVertices, int maxPolygons) {
        GASolution result = new GASolution(width, height);

        for (int i = 0; i < maxPolygons; ++i) {
            result.addPolygon(MyPolygon.createRandom(width, height, maxPolygonVertices));
        }
        return result;
    }

    public void calculateDistance(BufferedImage target, int fitness_probes) {
        BufferedImage solutionPicture = getImage();

        Random rand = new Random();

        double totalDistance = 0;
        for (int i = 0; i < fitness_probes; ++i) {
            int x = rand.nextInt(width);
            int y = rand.nextInt(height);
            Color solutionPix = new Color(solutionPicture.getRGB(x, y));
            Color targetPix = new Color(target.getRGB(x, y));

            int redDiff = solutionPix.getRed() - targetPix.getRed();
            int blueDiff = solutionPix.getBlue() - targetPix.getBlue();
            int greenDiff = solutionPix.getGreen() - targetPix.getGreen();

            double colorDistance = Math.sqrt(redDiff*redDiff + blueDiff*blueDiff + greenDiff*greenDiff);
            totalDistance += colorDistance;
        }
        totalDistance /= fitness_probes;
        distance = totalDistance;
    }
    public void calculateFitness(double maxDistance) {
        fitness = maxDistance - distance;
    }

    public double getFitness() {
        return fitness;
    }

    public double getDistance() {
        return distance;
    }

    public GASolution cross(GASolution other) {
        Random random = new Random();
        GASolution child = new GASolution(width, height);

        for (int i = 0; i < shapes.size(); ++i) {
            MyPolygon shape;
            if (i >= shapes.size()/2)
                shape = other.shapes.get(i);
            else
                shape = shapes.get(i);

            int rMutate, gMutate, bMutate;
            rMutate = gMutate = bMutate = 0;
            double chance = random.nextDouble();
            if (chance <= MUTATION_RATE) {
                rMutate += random.nextInt(COLOR_MUTATION_RANGE*2) - COLOR_MUTATION_RANGE;
                gMutate += random.nextInt(COLOR_MUTATION_RANGE*2) - COLOR_MUTATION_RANGE;
                bMutate += random.nextInt(COLOR_MUTATION_RANGE*2) - COLOR_MUTATION_RANGE;
            }

            int red = shape.getColor().getRed() + rMutate;
            int green = shape.getColor().getGreen() + gMutate;
            int blue = shape.getColor().getBlue() + bMutate;
            if (red > 255)
                red = 255;
            if (red < 0)
                red = 0;
            if (green > 255)
                green = 255;
            if (green < 0)
                green = 0;
            if (blue > 255)
                blue = 255;
            if (blue < 0)
                blue = 0;
            Color childColor = new Color(red,green,blue);


            Polygon polygon = shape.getPolygon();

            int[] childX = new int[polygon.npoints];
            int[] childY = new int[polygon.npoints];
            int[] xs = polygon.xpoints;
            int[] ys = polygon.ypoints;

            boolean mutatePosition = random.nextDouble() <= MUTATION_RATE;
            for (int j = 0; j < xs.length; ++j) {
                double mutateX = 0;
                double mutateY = 0;
                if (mutatePosition) {
                    if (random.nextDouble() < POSITION_MUTATION_CHANCE) {
                        mutateX += random.nextDouble() * POSITION_MUTATION_RATIO * 2 - POSITION_MUTATION_RATIO;
                        mutateY += random.nextDouble() * POSITION_MUTATION_RATIO * 2 - POSITION_MUTATION_RATIO;
                    }
                }
                childX[j] = (int)(mutateX*width+(xs[j]));
                if (childX[j] < -width/5)
                    childX[j] = -width/5;
                if (childX[j] > (width*6)/5)
                    childX[j] = (width*6)/5;
                childY[j] = (int)(mutateY*height+(ys[j]));
                if (childY[j] < -height/5)
                    childY[j] = -height/5;
                if (childY[j] > (height*6)/5)
                    childY[j] = (height*6)/5;
            }

            child.addPolygon(new MyPolygon(new Polygon(childX, childY, childX.length), childColor));
        }
        for (int i = shapes.size()/2; i < shapes.size(); ++i) {

        }
        return child;
    }

    public GASolution(int _width, int _height) {
        shapes = new ArrayList<MyPolygon>();
        width = _width;
        height = _height;
    }

    public void addPolygon(MyPolygon p) {
        shapes.add(p);
    }

    public ArrayList<MyPolygon> getShapes() {
        return shapes;
    }


    public int size() {
        return shapes.size();
    }

    // Create a BufferedImage of this solution
    // Use this to compare an evolved solution with
    // a BufferedImage of the target image
    //
    // This is almost surely NOT the fastest way to do this...
    public BufferedImage getImage() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (MyPolygon p : shapes) {
            Graphics g2 = image.getGraphics();
            g2.setColor(p.getColor());
            Polygon poly = p.getPolygon();
            if (poly.npoints > 0) {
                g2.fillPolygon(poly);
            }
        }
        return image;
    }

    public String toString() {
        return "" + shapes;
    }

    public double getReadableFitness() {
        return 1.0/distance;
    }
}


// A Canvas to draw the highest ranked solution each epoch
class GACanvas extends JComponent{

    int width, height;
    GASolution solution;

    public GACanvas(int WINDOW_WIDTH, int WINDOW_HEIGHT) {
        width = WINDOW_WIDTH;
        height = WINDOW_HEIGHT;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void setImage(GASolution sol) {
        solution = sol;
    }

    public void paintComponent(Graphics g) {
        BufferedImage image = solution.getImage();
        g.drawImage(image, 0, 0, null);
    }
}


public class GA extends JComponent{

    GACanvas canvas;
    int width, height;
    BufferedImage realPicture;
    ArrayList<GASolution> population;

    // Adjust these parameters as necessary for your simulation
    double MUTATION_RATE = 0.01;
    double CROSSOVER_RATE = 0.6;
    int MAX_POLYGON_POINTS = 5;
    int MAX_POLYGONS = 10;
    int FITNESS_PROBES = 100;

    public GA(GACanvas _canvas, BufferedImage _realPicture) {
        canvas = _canvas;
        realPicture = _realPicture;
        width = realPicture.getWidth();
        height = realPicture.getHeight();
        population = new ArrayList<GASolution>();

        // You'll need to define the following functions
        createPopulation(80);	// Make 50 new, random chromosomes
        canvas.setImage(getFittest());
    }

    public void createPopulation(int populationSize) {
        population = new ArrayList<GASolution>();

        for (int i = 0; i < populationSize; ++i) {
            population.add(GASolution.createRandom(width, height, MAX_POLYGON_POINTS, MAX_POLYGONS));
        }
        calculateAllFitnesses();
    }

    public void calculateAllFitnesses() {
        double maxDistance = 0;
        for (GASolution solution : population) {
            solution.calculateDistance(realPicture, FITNESS_PROBES);
            if (solution.getDistance() > maxDistance)
                maxDistance = solution.getDistance();
        }
        for (GASolution solution : population) {
            solution.calculateFitness(maxDistance);
        }
    }

    public GASolution getFitSolution() {
        double totalFitness = 0;
        for (GASolution solution : population) {
            totalFitness += solution.getFitness();
        }

        Random random = new Random();
        double randomFitness = random.nextDouble()*totalFitness;
        int solutionIndex = -1;
        if (randomFitness == 0)
            solutionIndex = 0;
        while (randomFitness > 0) {
            solutionIndex++;
            randomFitness -= population.get(solutionIndex).getFitness();
        }
        return population.get(solutionIndex);
    }

    public GASolution getFittest() {
        double maxFit = -1;
        GASolution selected = null;
        for (GASolution sol : population) {
            if (sol.getFitness() > maxFit) {
                maxFit = sol.getFitness();
                selected = sol;
            }
        }
        return selected;
    }

    public void progressGeneration() {
        ArrayList<GASolution> offspring = new ArrayList<GASolution>();

        int numberOfChildren = (int)(population.size() * CROSSOVER_RATE);
        for (int i = 0; i < numberOfChildren; ++i) {
            offspring.add(getFitSolution().cross(getFitSolution()));
        }

        Random random = new Random();
        for (int i = 0; i < numberOfChildren; ++i) {
            population.remove(random.nextInt(population.size()));
        }
        for (GASolution child : offspring) {
            population.add(child);
        }
        calculateAllFitnesses();
    }


    public void runSimulation() {
        for (int i = 0; i < 100000; ++i) {
            progressGeneration();
            if (i%300 == 0) {
                GASolution fittest = getFittest();
                canvas.setImage(fittest);
                System.out.println("" + i + " " + fittest.getReadableFitness());
                canvas.repaint();
            }
        }
    }

    public static void main(String[] args) throws IOException {

        String realPictureFilename = "test.jpg";

        BufferedImage realPicture = ImageIO.read(new File(realPictureFilename));

        JFrame frame = new JFrame();
        frame.setSize(realPicture.getWidth(), realPicture.getHeight());
        frame.setTitle("GA Simulation of Art");

        GACanvas theCanvas = new GACanvas(realPicture.getWidth(), realPicture.getHeight());
        frame.add(theCanvas);
        frame.setVisible(true);

        GA pt = new GA(theCanvas, realPicture);
        pt.runSimulation();
    }
}



