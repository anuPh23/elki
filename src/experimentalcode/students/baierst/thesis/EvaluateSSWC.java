package experimentalcode.students.baierst.thesis;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import experimentalcode.students.baierst.measures.NoiseOption;

/**
 * Compute the simplified silhouette of a data set.
 * 
 * @author Stephan Baier
 * 
 * @param <O> Object type
 */
public class EvaluateSSWC<O> implements Evaluator {

  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateSSWC.class);

  /**
   * Option for noise handling.
   */
  private NoiseOption noiseOption = NoiseOption.IGNORE_NOISE_WITH_PENALTY;

  /**
   * Distance function to use.
   */
  private PrimitiveDistanceFunction<? super NumberVector> distanceFunction;

  /**
   * Constructor.
   * 
   * @param distance Distance function
   * @param mergenoise Flag to treat noise as clusters, not singletons
   */
  public EvaluateSSWC(PrimitiveDistanceFunction<? super NumberVector> distance, NoiseOption noiseOpt) {
    super();
    this.distanceFunction = distance;
    this.noiseOption = noiseOpt;
  }

  /**
   * Evaluate a single clustering.
   * 
   * @param db Database
   * @param rel Data relation
   * @param dq Distance query
   * @param c Clustering
   */
  public double evaluateClustering(Database db, Relation<? extends NumberVector> rel, Clustering<?> c) {
    List<? extends Cluster<?>> clusters;

    if(noiseOption.equals(NoiseOption.TREAT_NOISE_AS_SINGLETONS)) {
      clusters = ClusteringUtils.convertNoiseToSingletons(c);
    }
    else {
      clusters = c.getAllClusters();
    }

    int countNoise = 0;

    MeanVariance mssil = new MeanVariance();

    // precompute all centroids
    ArrayList<NumberVector> centroids = new ArrayList<NumberVector>();

    for(Cluster<?> cluster : clusters) {
      if(cluster.isNoise() && (noiseOption.equals(NoiseOption.IGNORE_NOISE) || noiseOption.equals(NoiseOption.IGNORE_NOISE_WITH_PENALTY))) {
        countNoise += cluster.size();
        continue;
      }
      centroids.add(Centroid.make((Relation<? extends NumberVector>) rel, cluster.getIDs()).toVector(rel));
    }

    int i = 0;
    for(Cluster<?> cluster : clusters) {

      if(cluster.isNoise() && (noiseOption.equals(NoiseOption.IGNORE_NOISE) || noiseOption.equals(NoiseOption.IGNORE_NOISE_WITH_PENALTY))) {
        continue;
      }

      ArrayDBIDs ids = DBIDUtil.ensureArray(cluster.getIDs());
      DBIDArrayIter it1 = ids.iter();
      for(it1.seek(0); it1.valid(); it1.advance()) {
        // a: Distance to own centroid
        double a = distanceFunction.distance(centroids.get(i), rel.get(it1));

        // b: Distance to other clusters centroids:
        double min = Double.POSITIVE_INFINITY;
        int j = 0;
        for(Cluster<?> ocluster : clusters) {

          if(ocluster.isNoise() && (noiseOption.equals(NoiseOption.IGNORE_NOISE) || noiseOption.equals(NoiseOption.IGNORE_NOISE_WITH_PENALTY))) {
            continue;
          }
          if(ocluster == cluster) {
            j++;
            continue;
          }

          double b = distanceFunction.distance(centroids.get(j), rel.get(it1));

          if(b < min) {
            min = b;
          }
          j++;
        }

        if(cluster.size() <= 1) {
          // put 0 for singletons
          mssil.put(0);

          continue;
        }

        mssil.put((min - a) / Math.max(min, a));
      }
      i++;
    }

    double sswc = mssil.getMean();

    if(noiseOption.equals(NoiseOption.IGNORE_NOISE_WITH_PENALTY)) {

      double penalty = 1;

      if(countNoise != 0) {
        penalty = ((double) rel.size() - (double) countNoise) / (double) rel.size();
      }

      sswc = penalty * sswc;
    }

    if(LOG.isVerbose()) {
      LOG.verbose("Mean Simplified Silhouette: " + sswc);
    }

    return sswc;      
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    List<Clustering<?>> crs = ResultUtil.getClusteringResults(result);
    if(crs.size() < 1) {
      return;
    }
    Database db = ResultUtil.findDatabase(baseResult);
    Relation<? extends NumberVector> rel = db.getRelation(this.distanceFunction.getInputTypeRestriction());

    for(Clustering<?> c : crs) {
      double sswc = evaluateClustering(db, (Relation<? extends NumberVector>) rel, c);
      
      // Build a primitive result attachment:
      Collection<DoubleVector> col = new ArrayList<>();
      col.add(new DoubleVector(new double[] { sswc }));
      db.getHierarchy().add(c, new CollectionResult<>("Simplified Silhouette coefficient", "sswc", col));      
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Stephan Baier
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * Parameter for choosing the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("simplified-silhouette.distance", "Distance function to use for computing the silhouette.");

    /**
     * Parameter for the option, how noise should be treated.
     */
    public static final OptionID NOISE_OPTION_ID = new OptionID("simplified-silhouette.noiseoption", "option, how noise should be treated.");

    /**
     * Distance function to use.
     */
    private PrimitiveDistanceFunction<NumberVector> distance;

    /**
     * Option, how noise should be treated.
     */
    private NoiseOption noiseOption = NoiseOption.IGNORE_NOISE_WITH_PENALTY;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      ObjectParameter<PrimitiveDistanceFunction<NumberVector>> distanceFunctionP = new ObjectParameter<>(DISTANCE_ID, PrimitiveDistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distance = distanceFunctionP.instantiateClass(config);
      }

      EnumParameter<NoiseOption> noiseP = new EnumParameter<NoiseOption>(NOISE_OPTION_ID, NoiseOption.class, NoiseOption.IGNORE_NOISE_WITH_PENALTY);
      if(config.grab(noiseP)) {
        noiseOption = noiseP.getValue();
      }

    }

    @Override
    protected EvaluateSSWC<? extends NumberVector> makeInstance() {
      return new EvaluateSSWC<>(distance, noiseOption);
    }
  }
}
