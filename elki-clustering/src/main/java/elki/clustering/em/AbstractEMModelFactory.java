/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.clustering.em;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.clustering.kmeans.initialization.RandomlyChosen;
import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for initializing EM.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <V> Vector type
 * @param <M> Model type
 */
public abstract class AbstractEMModelFactory<V extends NumberVector, M extends MeanModel> implements EMClusterModelFactory<V, M> {
  /**
   * Class to choose the initial means
   */
  protected KMeansInitialization initializer;

  /**
   * Constructor.
   *
   * @param initializer Class for choosing the initial seeds.
   */
  public AbstractEMModelFactory(KMeansInitialization initializer) {
    super();
    this.initializer = initializer;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <V> vector type
   */
  public abstract static class Par<V extends NumberVector> implements Parameterizer {
    /**
     * Parameter to specify the cluster center initialization.
     */
    public static final OptionID INIT_ID = new OptionID("em.centers", "Method to choose the initial cluster centers.");

    /**
     * Initialization method
     */
    protected KMeansInitialization initializer;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<KMeansInitialization>(INIT_ID, KMeansInitialization.class, RandomlyChosen.class) //
          .grab(config, x -> initializer = x);
    }
  }
}
