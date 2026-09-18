package nodebox.graphics;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

public class ColorExtractor {

    public static List<Color> extractPalette(String imagePath, int k, int sampleStep) {
        List<Color> palette = new ArrayList<Color>();
        if (imagePath == null || imagePath.trim().isEmpty() || k <= 0) return palette;

        try {
            File f = new File(imagePath);
            if (!f.exists() || !f.isFile()) return palette;
            BufferedImage img = ImageIO.read(f);
            if (img == null) return palette;

            return extractPalette(img, k, sampleStep);
        } catch (Exception e) {
            return palette;
        }
    }

    public static List<Color> extractPalette(BufferedImage img, int k, int sampleStep) {
        List<Color> palette = new ArrayList<Color>();
        if (img == null || k <= 0) return palette;

        int step = Math.max(1, sampleStep);
        int w = img.getWidth();
        int h = img.getHeight();

        List<double[]> samples = new ArrayList<double[]>();
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                int argb = img.getRGB(x, y);
                int a = (argb >> 24) & 0xff;
                if (a < 64) continue; // skip transparent / semi-transparent
                int r = (argb >> 16) & 0xff;
                int g = (argb >> 8) & 0xff;
                int b = argb & 0xff;
                samples.add(new double[]{r / 255.0, g / 255.0, b / 255.0});
            }
        }

        if (samples.isEmpty()) return palette;

        int numClusters = Math.min(k, samples.size());

        // Initialize centroids with k-means++ or evenly spaced samples
        List<double[]> centroids = new ArrayList<double[]>();
        Random rng = new Random(42);
        centroids.add(samples.get(rng.nextInt(samples.size())));

        while (centroids.size() < numClusters) {
            double[] dists = new double[samples.size()];
            double sum = 0;
            for (int i = 0; i < samples.size(); i++) {
                double minDist = Double.MAX_VALUE;
                for (double[] c : centroids) {
                    double d = distSq(samples.get(i), c);
                    if (d < minDist) minDist = d;
                }
                dists[i] = minDist;
                sum += minDist;
            }
            if (sum <= 0) break;
            double rVal = rng.nextDouble() * sum;
            double running = 0;
            int chosen = 0;
            for (int i = 0; i < samples.size(); i++) {
                running += dists[i];
                if (running >= rVal) {
                    chosen = i;
                    break;
                }
            }
            centroids.add(samples.get(chosen));
        }

        // K-Means iterations (up to 12)
        int[] assignments = new int[samples.size()];
        int[] clusterCounts = new int[centroids.size()];

        for (int iter = 0; iter < 12; iter++) {
            Arrays.fill(clusterCounts, 0);
            for (int i = 0; i < samples.size(); i++) {
                double[] p = samples.get(i);
                double minDist = Double.MAX_VALUE;
                int bestIdx = 0;
                for (int c = 0; c < centroids.size(); c++) {
                    double d = distSq(p, centroids.get(c));
                    if (d < minDist) {
                        minDist = d;
                        bestIdx = c;
                    }
                }
                assignments[i] = bestIdx;
                clusterCounts[bestIdx]++;
            }

            double[][] newCentroids = new double[centroids.size()][3];
            for (int i = 0; i < samples.size(); i++) {
                int c = assignments[i];
                newCentroids[c][0] += samples.get(i)[0];
                newCentroids[c][1] += samples.get(i)[1];
                newCentroids[c][2] += samples.get(i)[2];
            }

            for (int c = 0; c < centroids.size(); c++) {
                if (clusterCounts[c] > 0) {
                    centroids.set(c, new double[]{
                            newCentroids[c][0] / clusterCounts[c],
                            newCentroids[c][1] / clusterCounts[c],
                            newCentroids[c][2] / clusterCounts[c]
                    });
                }
            }
        }

        // Sort centroids by cluster frequency descending (dominant colors first)
        Integer[] order = new Integer[centroids.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Integer.compare(clusterCounts[b], clusterCounts[a]);
            }
        });

        for (int idx : order) {
            double[] c = centroids.get(idx);
            palette.add(new Color(c[0], c[1], c[2], 1.0));
        }

        return palette;
    }

    private static double distSq(double[] a, double[] b) {
        double dr = a[0] - b[0];
        double dg = a[1] - b[1];
        double db = a[2] - b[2];
        return dr * dr + dg * dg + db * db;
    }
}
