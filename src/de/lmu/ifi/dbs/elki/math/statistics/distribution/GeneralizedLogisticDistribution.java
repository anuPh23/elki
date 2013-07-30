package de.lmu.ifi.dbs.elki.math.statistics.distribution;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import java.util.Random;

/**
 * Generalized logistic distribution. (Type I, skew-logistic distribution)
 * 
 * One of multiple ways of generalizing the logistic distribution.
 * 
 * {@code f(x) = shape * Math.exp(-x) / (1 + Math.exp(-x))**(shape+1)}
 * 
 * Where {@code shape=1} yields the regular logistic distribution.
 * 
 * @author Erich Schubert
 */
public class GeneralizedLogisticDistribution implements DistributionWithRandom {
  /**
   * Parameters: location and scale
   */
  double location, scale;

  /**
   * Shape parameter, for generalized logistic distribution.
   */
  double shape;

  /**
   * Random number generator
   */
  Random random;

  /**
   * Constructor.
   * 
   * @param location Location
   * @param scale Scale
   * @param shape Shape parameter
   */
  public GeneralizedLogisticDistribution(double location, double scale, double shape) {
    this(location, scale, shape, null);
  }

  /**
   * Constructor.
   * 
   * @param location Location
   * @param scale Scale
   * @param shape Shape parameter
   * @param random Random number generator
   */
  public GeneralizedLogisticDistribution(double location, double scale, double shape, Random random) {
    super();
    this.location = location;
    this.scale = scale;
    this.shape = shape;
    this.random = random;
  }

  /**
   * Probability density function.
   * 
   * @param val Value
   * @param loc Location
   * @param scale Scale
   * @param shape Shape
   * @return PDF
   */
  public static double pdf(double val, double loc, double scale, double shape) {
    val = (val - loc) / scale;
    double e = Math.exp(-val);
    double f = 1. + e;
    return shape * e / (scale * Math.pow(f, shape + 1.));
  }

  /**
   * log Probability density function.
   * 
   * TODO: untested.
   * 
   * @param val Value
   * @param loc Location
   * @param scale Scale
   * @param shape Shape
   * @return log PDF
   */
  public static double logpdf(double val, double loc, double scale, double shape) {
    val = (val - loc) / scale;
    double e = Math.exp(-val);
    return -(val + (shape + 1.0) * Math.log1p(e)) + Math.log(shape);
  }

  @Override
  public double pdf(double val) {
    return pdf(val, location, scale, shape);
  }

  /**
   * Cumulative density function.
   * 
   * @param val Value
   * @param loc Location
   * @param scale Scale
   * @param shape Shape
   * @return CDF
   */
  public static double cdf(double val, double loc, double scale, double shape) {
    val = (val - loc) / scale;
    return Math.pow(1. + Math.exp(-val), -shape);
  }

  /**
   * log Cumulative density function.
   * 
   * TODO: untested.
   * 
   * @param val Value
   * @param loc Location
   * @param scale Scale
   * @param shape Shape
   * @return log PDF
   */
  public static double logcdf(double val, double loc, double scale, double shape) {
    val = (val - loc) / scale;
    return Math.log1p(Math.exp(-val)) * -shape;
  }

  @Override
  public double cdf(double val) {
    return cdf(val, location, scale, shape);
  }

  /**
   * Quantile function.
   * 
   * @param val Value
   * @param loc Location
   * @param scale Scale
   * @param shape Shape
   * @return Quantile
   */
  public static double quantile(double val, double loc, double scale, double shape) {
    return loc + scale * -Math.log(Math.pow(val, -1.0 / shape) - 1);
  }

  @Override
  public double quantile(double val) {
    return quantile(val, location, scale, shape);
  }

  @Override
  public double nextRandom() {
    double u = random.nextDouble();
    return location + scale * -Math.log(Math.pow(u, -1.0 / shape) - 1);
  }

  @Override
  public String toString() {
    return "GeneralizedLogisticDistribution(location=" + location + ", scale=" + scale + ", shape=" + shape + ")";
  }
}
