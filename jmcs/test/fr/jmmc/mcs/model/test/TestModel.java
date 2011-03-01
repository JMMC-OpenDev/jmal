/*******************************************************************************
 * JMMC project
 *
 * "@(#) $Id: TestModel.java,v 1.5 2010-02-19 16:02:52 bourgesl Exp $"
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.4  2010/02/12 15:52:05  bourgesl
 * refactoring due to changed generated classes by xjc
 *
 * Revision 1.3  2010/02/09 16:50:07  bourgesl
 * use rgb color interpolation for better image rendering
 *
 * Revision 1.2  2010/02/03 09:31:29  bourgesl
 * added Amplitude or Phase uv map
 *
 * Revision 1.1  2010/01/29 15:53:16  bourgesl
 * Test code for punct / disk models
 *
 */
package fr.jmmc.mcs.model.test;

import fr.jmmc.mcs.image.ImageViewer;
import fr.jmmc.mcs.model.ModelDefinition;
import fr.jmmc.mcs.model.ModelManager;
import fr.jmmc.mcs.model.targetmodel.Model;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math.complex.Complex;

/**
 *
 * @author bourgesl
 */
public class TestModel {

  private TestModel() {
    // no-op
  }

  /**
   * Return the frequencies in rad-1
   * @param width number of values in the range [-max;max]
   * @param max maximum base line length in meter
   * @return sampled frequencies in rad-1
   */
  private static double[] computeFrequencyRange(final int width, final double max) {
    final double[] freq = new double[width];

    final double min = -1e6D * max;
    final double step = 2e6D * max / width;

    freq[0] = min;
    for (int i = 1; i < width; i++) {
      freq[i] = freq[i - 1] + step;
    }
    freq[width - 1] = 1e6D * max;

    return freq;
  }

  private static List<Model> punctModels() {

    final ModelManager mm = ModelManager.getInstance();

    final List<Model> models = new ArrayList<Model>();

    Model model;

    // punct1 :
    model = mm.createModel(ModelDefinition.MODEL_PUNCT);

    ModelManager.setParameterValue(model, ModelDefinition.PARAM_FLUX_WEIGHT, 1.0);
    ModelManager.setParameterValue(model, ModelDefinition.PARAM_X, 0);
    ModelManager.setParameterValue(model, ModelDefinition.PARAM_Y, 0);

    models.add(model);

    // punct2 :
    model = mm.createModel(ModelDefinition.MODEL_PUNCT);

    ModelManager.setParameterValue(model, ModelDefinition.PARAM_FLUX_WEIGHT, 1);
    ModelManager.setParameterValue(model, ModelDefinition.PARAM_X, 1);
    ModelManager.setParameterValue(model, ModelDefinition.PARAM_Y, 0);

    models.add(model);
/*
    // punct3 :
    model = mm.createModel(ModelDefinition.MODEL_PUNCT);

    ModelManager.setParameterValue(model, ModelDefinition.PARAM_FLUX_WEIGHT, 0.4);
    ModelManager.setParameterValue(model, ModelDefinition.PARAM_X, 0);
    ModelManager.setParameterValue(model, ModelDefinition.PARAM_Y, 1);

    models.add(model);

    // punct4 :
    model = mm.createModel(ModelDefinition.MODEL_PUNCT);

    ModelManager.setParameterValue(model, ModelDefinition.PARAM_FLUX_WEIGHT, 0.2);
    ModelManager.setParameterValue(model, ModelDefinition.PARAM_X, 1);
    ModelManager.setParameterValue(model, ModelDefinition.PARAM_Y, 1);

    models.add(model);
*/
    return models;
  }


  private static List<Model> diskModels() {

    final ModelManager mm = ModelManager.getInstance();

    final List<Model> models = new ArrayList<Model>();

    Model model;

    // disk1 :
    model = mm.createModel(ModelDefinition.MODEL_DISK);

    ModelManager.setParameterValue(model, ModelDefinition.PARAM_FLUX_WEIGHT, 3.0);
    ModelManager.setParameterValue(model, ModelDefinition.PARAM_X, 0);
    ModelManager.setParameterValue(model, ModelDefinition.PARAM_Y, 0);
    ModelManager.setParameterValue(model, ModelDefinition.PARAM_DIAMETER, 30);

    models.add(model);

    // disk2 :
    /*
    model = mm.createModel(ModelDefinition.MODEL_DISK);

    ModelManager.setParameterValue(model, ModelDefinition.PARAM_FLUX_WEIGHT, 1.0);
    ModelManager.setParameterValue(model, ModelDefinition.PARAM_X, 1);
    ModelManager.setParameterValue(model, ModelDefinition.PARAM_Y, 0);
    ModelManager.setParameterValue(model, ModelDefinition.PARAM_DIAMETER, 1);

    models.add(model);
    */

    return models;
  }



  private static void computeImage(final ImageViewer iv, final List<Model> models) {
    // amplitude or phase :
    final boolean doAmp = true;

    // Image :
    final int width = 512;
    final int height = width;

    double[] ufreq;
    double[] vfreq;

    Complex[] vis;

    // Start the computations :
    final long start = System.nanoTime();

    // ASPRO = 200m :

    // LITpro = 10m
    final double maxBL = 10D;

    final double[] freq = computeFrequencyRange(width, maxBL);

    ufreq = new double[width * height];
    vfreq = new double[width * height];

    for (int j = 0; j < height; j++) {
      for (int i = 0, k = 0; i < width; i++) {
        k = width * j + i;
        ufreq[k] = freq[i];
        vfreq[k] = freq[j];
      }
    }

    System.out.println("computeUV : duration = " + 1e-6d * (System.nanoTime() - start) + " ms.");

    vis = ModelManager.getInstance().computeModels(ufreq, vfreq, models);

    System.out.println("computeVIS : duration = " + 1e-6d * (System.nanoTime() - start) + " ms.");

    final float[] img = new float[width * height];

    // Inverse Y axis : 

    float val;
    float min = Float.MAX_VALUE;
    float max = Float.MIN_VALUE;

    if (doAmp) {
      for (int j = 0; j < height; j++) {
        for (int i = 0; i < width; i++) {
          val = (float) vis[width * (height - 1 - j) + i].abs();
          img[width * j + i] = val;
          if (val < min) {
            min = val;
          }
          if (val > max) {
            max = val;
          }
        }
      }
    } else {
      for (int j = 0; j < height; j++) {
        for (int i = 0; i < width; i++) {
          val = (float) vis[width * (height - 1 - j) + i].getArgument();
          img[width * j + i] = val;
          if (val < min) {
            min = val;
          }
          if (val > max) {
            max = val;
          }
        }
      }
    }

    System.out.println("VIS_AMP min = " + min);
    System.out.println("VIS_AMP max = " + max);

    System.out.println("image : duration = " + 1e-6d * (System.nanoTime() - start) + " ms.");

    iv.getImageCanvas().initImage(width, height, img, min, max);

    System.out.println("ImageViewer : duration = " + 1e-6d * (System.nanoTime() - start) + " ms.");
  }

  /**
   * Test code
   * @param args
   */
  public static void main(String[] args) {

    final ImageViewer iv = new ImageViewer();
    iv.getImageCanvas().setAntiAliasing(true);
    iv.getImageCanvas().setDrawTicks(false);

    List<Model> models;
    
    // models = punctModels();

    models = diskModels();

    computeImage(iv, models);

    iv.setPreferredSize(new Dimension(500, 500));
    iv.pack();
    iv.setVisible(true);
  }
}